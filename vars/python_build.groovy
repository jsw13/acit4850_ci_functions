def call(dockerRepoName, imageName, portNum) {
    pipeline {
        agent any
    
        parameters {
            booleanParam(defaultValue: false, description: "Deploy the App", name: "DEPLOY")
        }


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
            stage("Package") {
                when {
                    expression { env.GIT_BRANCH == "origin/main" }
                }
                steps {
                    withCredentials([string(credentialsId: "DockerHub", variable: "TOKEN")]) {
                        sh "docker login -u 'j13jha' -p '$TOKEN' docker.io"
                        sh "docker build -t ${dockerRepoName}:latest --tag j13jha/${dockerRepoName}:${imageName} ."
                        sh "docker push j13jha/${dockerRepoName}:${imageName}"
                    }
                }
            }
            stage("Zip Artifacts") {
                steps {
                    sh "zip app.zip *.py"
                    archiveArtifacts artifacts: "app.zip", fingerprint: true, onlyIfSuccessful: true
                }
            }
            stage("Deliver") {
                when {
                    expression { params.DEPLOY }
                }
                steps {
                    sh "docker stop ${dockerRepoName} || true && docker rm ${dockerRepoName} || true"
                    sh "docker run -d -p ${portNum}:${portNum} --name ${dockerRepoName} ${dockerRepoName}:latest"
                }
            }
        }
    }
}
