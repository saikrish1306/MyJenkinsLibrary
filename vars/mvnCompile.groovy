def call() {
    //def mvn_Home = tool 'maven3'
    //def mvn = "${mvn_Home}/bin/mvn"

    //sh "${mvn} clean install"
    sh "mvn clean install"

    script {
        // Validate if pom.xml exists
        if (!fileExists('pom.xml')) {
            error "❌ ERROR: pom.xml not found! Ensure you are in the correct project directory."
        }

        def allowedBranches = ['main', /^dev-.*$/, /^release-.*$/]  // Use regex for pattern matching

        def isAllowed = allowedBranches.any { pattern ->
            if (pattern instanceof String) {
                return env.BRANCH_NAME == pattern
            } else {
                return env.BRANCH_NAME ==~ pattern  // Match regex for wildcard branches
            }
        }

        if (!isAllowed) {
            error "❌ ERROR: Compilation is restricted on branch ${env.BRANCH_NAME}"
        }

        // Run Maven compile
        //sh "${mvn} compile"
        sh "mvn compile"
        echo "✅ Maven compilation completed successfully."
    }
}
