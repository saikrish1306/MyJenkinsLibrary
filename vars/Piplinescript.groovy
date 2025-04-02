def call(){
  
 pipeline {
    agent any
    
    tools {
        maven 'Maven'
        jdk 'JDK'
    }

    environment {
        SCANNER_HOME= tool 'Sonar-Scanner'
    }
    
    stages {     
        stage('Git checkout') {
            steps {
               git branch: 'main', credentialsId: 'Git-cred', url: 'https://github.com/saikrish1306/MyJavaProject.git'
            }
        }
        
        stage('Compile') {
            steps {
            dir('MyJavaProject')
            {
                sh "mvn compile"}
            }
        }
        
        stage('Test') {
            steps {
                 dir('MyJavaProject') 
                  {
                sh "mvn test"
                  }
                }
        }
        
        stage('SonarQube Analsyis') {
            steps {
                withSonarQubeEnv('Sonar') {
                    sh ''' $SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=BoardGame -Dsonar.projectKey=BoardGame \
                            -Dsonar.java.binaries=. '''
                }
            }
        }
        
        stage('Build') {
            steps {
                 dir('MyJavaProject')
                 {
                sh "mvn package"}
            }
        }
        
        stage('Publish To Nexus') {
            steps {
               withMaven(globalMavenSettingsConfig: 'global-settings', jdk: 'JDK', maven: 'Maven', mavenSettingsConfig: '', traceability: true) {
            dir('MyJavaProject')
                 {
                    sh "mvn deploy"
                 }
                 }
                 }
            }
    }
}
}
