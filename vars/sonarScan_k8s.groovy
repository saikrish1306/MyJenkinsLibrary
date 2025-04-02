//For running in K8's Node

def call() {
    //def sonartool = tool 'sonar-scanner'
    //def SCANNER_HOME = "${sonartool}/bin/sonar-scanner"
        
    script {
        // Validate if SonarQube Scanner is installed
        def sonarExists = sh(script: "command -v sonar-scanner || echo 'not-found'", returnStdout: true).trim()
        if (sonarExists == 'not-found') {
            error "❌ ERROR: SonarQube Scanner is not installed or not available in PATH!"
        }

        // Validate if pom.xml exists (assuming it's a Maven project)
        if (!fileExists('pom.xml')) {
            error "❌ ERROR: pom.xml not found! Ensure you are in the correct project directory."
        }

        // This block not required since pipline use the "withsonarenv"
        // Validate if SonarQube environment variables are set
        //if (!env.SONAR_HOST_URL || !env.SONAR_TOKEN) {
        //    error "❌ ERROR: Missing SonarQube environment variables (SONAR_HOST_URL or SONAR_TOKEN)."
        //}

        // Run SonarQube scan
        withSonarQubeEnv('sonar') {
          try {
                echo "🔄 Running SonarQube scan..."
                sh """
                    sonar-scanner \
                    -Dsonar.projectKey=${env.REPO_NAME} \
                    -Dsonar.projectName=${env.REPO_NAME} \
                    -Dsonar.sources=. \
                    -Dsonar.java.binaries=. // usually the files will be in the path "target/classes"
                """
                echo "✅ SonarQube scan completed successfully."
            } catch (Exception e) {
                error "❌ ERROR: SonarQube scan failed: ${e.getMessage()}"
            }
        }
    }
}
