def call() {
    pipeline {
        agent any
        stages {
            stage("Build") {
                steps {
                    sh "pip install -r requirements.txt"
                }
            }
            stage("Python Lint") {
                steps {
                    sh "pylint-fail-under --fail_under 5.0 *.py"
                }
            }
            stage("Test and Coverage") {
                steps {
                    script {
                        def files = findFiles(glob: "*test*/*.xml")
                        for (file in files) {
                            sh "rm ${file.path}"
                        }
                    }
                    script {
                        def files = findFiles(glob: "test*")
                        for (file in files) {
                            sh "coverage run --omit */site-packages/*,*/dist-packages/* ${file.path}"
                        }
                    }
                    sh "coverage report"
                }
                post {
                    always {
                        script {
                            def files = findFiles(glob: "*test*/*.xml")
                            for (file in files) {
                                junit "${file.path}"
                            }
                        }
                    }
                }
            }
            stage("Zip Artifacts") {
                steps {
                    sh "zip app.zip *.py"
                    archiveArtifacts artifacts: "app.zip", fingerprint: true, onlyIfSuccessful: true
                }
            }
        }
    }
}
