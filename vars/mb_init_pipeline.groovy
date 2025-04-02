def call() {
    
pipeline {                
    agent {
    kubernetes {
    yaml """
    apiVersion: v1
    kind: Pod
    spec:
      dnsPolicy: "None"
      dnsConfig:
        nameservers:
          - 8.8.8.8
          - 1.1.1.1
      containers:
        - name: maven
          image: maven:3.9.9-eclipse-temurin-17
          command:
            - cat
          tty: true
          volumeMounts:
            - mountPath: /workspace
              name: shared-volume
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "1"
              memory: "1Gi"
              
        - name: sonar-scanner
          image: sonarsource/sonar-scanner-cli:latest
          command:
            - cat
          tty: true
          volumeMounts:
            - mountPath: /workspace
              name: shared-volume
          resources:
            requests:
              cpu: "250m"
              memory: "500Mi"
            limits:
              cpu: "1"
              memory: "2Gi"
              
        - name: git
          image: alpine/git:latest
          command: ["cat"]
          tty: true
          volumeMounts:
            - mountPath: /workspace
              name: shared-volume
          resources:
            requests:
              cpu: "250m"
              memory: "100Mi"
            limits:
              cpu: "500m"
              memory: "500Mi"     
    
        - name: kaniko
          image: gcr.io/kaniko-project/executor:debug
          imagePullPolicy: Always
          command:
            - sleep
          args:
            - "9999999"
          volumeMounts:
            - mountPath: /workspace
              name: shared-volume
            - mountPath: /kaniko/.docker
              name: docker-config
            - mountPath: /kaniko-secret
              name: gcp-credentials
          resources:
            requests:
              cpu: "250m"
              memory: "500Mi"
            limits:
              cpu: "1"
              memory: "2Gi"
      volumes:
        - name: shared-volume
          persistentVolumeClaim:
            claimName: shared-pvc
        - name: docker-config
          emptyDir: {}
        - name: gcp-credentials
          secret:
            secretName: gcp-gar-cred
                    """
            }
        }

    stages {
        stage('Checkout') {
            steps {
                container('git') {
                    git branch: 'main', credentialsId: 'git-cred', url: 'https://github.com/rajalearn90/MyJavaProject.git'
                    sh '''
                    echo "Current working directory:"
                    pwd
                    echo "Listing files in current directory:"
                    ls -l
                    echo "Listing files in /workspace from Kaniko container:"
                    ls -l /workspace
                    '''
                }
            }
        }
        
        stage('Compile') {
            steps {
                container('maven') 
                {
                  mvnCompile()
                }
            }
       }
       
       stage('Test') {
            steps {
                container('maven') 
                {
                   sh '''
                      mvn clean test
                   '''
                }
            }
       }
       
        stage('SonarQube Analsyis') {
            steps {
                container('sonar-scanner')
                    {
                        withCredentials([string(credentialsId: 'sonar-cred', variable: 'SONAR_TOKEN')]) {
                          sh '''
                            sonar-scanner \
                            -Dsonar.projectName=BoardGame \
                            -Dsonar.projectKey=BoardGame \
                            -Dsonar.host.url=http://34.131.163.91:9000 \
                            -Dsonar.login=$SONAR_TOKEN \
                            -Dsonar.java.binaries=.
                            '''
                        }                         
                }
            }
        }
        
        stage('Kaniko Build') {
            steps {
                container(name: 'kaniko', shell: '/busybox/sh') {
                    sh '''
                        #!/bin/sh
                        export GOOGLE_APPLICATION_CREDENTIALS="/kaniko-secret/key.json"
                        
                        JOB_ONLY=$(echo "$JOB_NAME" | cut -d'/' -f1)
                        JOBNAME_BRANCHNAME="${JOB_ONLY}_${BRANCH_NAME}"
                        
                        /kaniko/executor \
                        --context=/home/jenkins/agent/workspace/${JOBNAME_BRANCHNAME} \
                        --dockerfile=/home/jenkins/agent/workspace/${JOBNAME_BRANCHNAME}/Dockerfile \
                        --destination=us-central1-docker.pkg.dev/rare-palace-450610-v7/jenkins-project/boardgame:${BUILD_NUMBER} \
                        --cache=true \
                        --verbosity=debug
                    '''
                }
            }
        }
    }
}
}
