def call(){
  
  pipeline {
      agent any
      
      tools {
          jdk 'jdk17'
          maven 'maven3'
      }
  
      environment {
          SCANNER_HOME= tool 'sonar-scanner'
          CLOUDSDK_CORE_PROJECT = 'rare-palace-450610-v7'
      }
  
      stages {
         stage('Git Checkout') {
              steps {
                 git branch: 'main', credentialsId: 'git-cred', url: 'https://github.com/rajalearn90/DevopsJavaProject.git'     
              }
         }
          
          stage('Compile') {
              steps {
                  sh "mvn compile"
              }
          }
          
          stage('Test') {
              steps {
                  sh "mvn test"
              }
          }
  
        //install trivy in the jenkins server
          stage('File System Scan') {
              steps {
                  sh "trivy fs --format table -o trivy-fs-report.html ."
              }
          }
  
           // add the webhook in the sonarqube server http://34.47.215.174:8080/sonarqube-webhook
           //Create token in the Sonarqube server
           //add server in the manage jenkins
           //add environement for SONAR_HOME
  
          stage('SonarQube Analsyis') {
              steps {
                  withSonarQubeEnv('sonar') {
                      sh ''' $SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=BoardGame -Dsonar.projectKey=BoardGame -Dsonar.java.binaries=. '''
                  }
              }
          }
                   
          stage('Quality Gate') {
              steps {
                  script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'sonar-cred' 
                  }
              }
          }
          
          stage('Build') {
              steps {
                 sh "mvn package"
              }
          }
          
          stage('Publish To Nexus') {
              steps {
                 withMaven(globalMavenSettingsConfig: 'global-settings', jdk: 'jdk17', maven: 'maven3', mavenSettingsConfig: '', traceability: true) {
                      sh "mvn deploy"
                 }
              }
          }
           
      }
    }
}
