config = [
    agentLabel: '',
    maven: 'Maven 3',
    jdk: 'Zulu 17',
    extraMavenArguments: '-U -Ddkpro.core.testCachePath="${WORKSPACE}/cache/dkpro-core-datasets" -Dmaven.artifact.threads=15  -T 4',
    wipeWorkspaceBeforeBuild: true,
    wipeWorkspaceAfterBuild: true
  ]

pipeline {
  parameters {
    string(
      name: 'extraMavenArguments',
      defaultValue: config.extraMavenArguments,
      description: "Extra arguments to be passed to Maven (for testing; overrides only current build)")
    string(
      name: 'agentLabel',
      defaultValue: config.agentLabel,
      description: "Eligible agents (in case a build keeps running on a broken agent; overrides only current build)")
    booleanParam(
      name: 'wipeWorkspaceBeforeBuild',
      defaultValue: config.wipeWorkspaceBeforeBuild,
      description: "Wipe workspace before build (for testing; next build only)")
    booleanParam(
      name: 'wipeWorkspaceAfterBuild',
      defaultValue: config.wipeWorkspaceAfterBuild,
      description: "Wipe workspace after build (for testing; next build only)")
  }

  agent {
    label params.agentLabel
  }
  
  tools {
    maven config.maven
    jdk config.jdk
  }

  options {
    buildDiscarder(logRotator(
      numToKeepStr: '25',
      artifactNumToKeepStr: '5'
    ))
    skipDefaultCheckout()
  }

  stages {
    stage("Checkout code") {
      steps {
        script {
          if (params.wipeWorkspaceBeforeBuild) {
            echo "Wiping workspace..."
            cleanWs(cleanWhenNotBuilt: true,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true)
          }
        }

        dir('checkout') {
          checkout scm
        }
      }
    }
    
    // Display information about the build environment. This can be useful for debugging
    // build issues.
    stage("Info") {
      steps {
        echo '=== Environment variables ==='
        script {
          if (isUnix()) {
            sh 'printenv'
          }
          else {
            bat 'set'
          }
        }
      }
    }
        
    // Perform a merge request build. This is a conditional stage executed with the GitLab
    // sources plugin triggers a build for a merge request. To avoid conflicts with other
    // builds, this stage should not deploy artifacts to the Maven repository server and
    // also not install them locally.
    stage("PR build") {
      when { branch 'PR-*' }
    
      steps {
        script {
          currentBuild.description = 'Triggered by: <a href="' + CHANGE_URL + '">' + BRANCH_NAME +
            ': ' + env.CHANGE_BRANCH + '</a> (' +  env.CHANGE_AUTHOR_DISPLAY_NAME + ')'
        }

        dir('checkout') {
          withMaven(maven: config.maven, jdk: config.jdk, mavenLocalRepo: "$WORKSPACE/.repository") {
            script {
              def mavenCommand = 'mvn ' +
                  params.extraMavenArguments +
                  ' -B -Dmaven.test.failure.ignore=true -T 4 -Pjacoco clean verify javadoc:javadoc';
                  
              if (isUnix()) {
                sh script: mavenCommand
              }
              else {
                bat script: mavenCommand
              }
            }
          }
          
          script {
            def mavenConsoleIssues = scanForIssues tool: mavenConsole()
            def javaIssues = scanForIssues tool: java()
            def javaDocIssues = scanForIssues tool: javaDoc()
            publishIssues id: "analysis", issues: [mavenConsoleIssues, javaIssues, javaDocIssues]
          }
        }
      }
    }
    
    // Perform a SNAPSHOT build of a main branch. This stage is typically executed after a
    // merge request has been merged. On success, it deploys the generated artifacts to the
    // Maven repository server.
    stage("SNAPSHOT build") {
      when { branch pattern: "main|release/.*", comparator: "REGEXP" }
      
      steps {
        dir('checkout') {
          withMaven(maven: config.maven, jdk: config.jdk, mavenLocalRepo: "$WORKSPACE/.repository") {
            script {
              def mavenCommand = 'mvn ' +
                params.extraMavenArguments +
                ' -B -Dmaven.test.failure.ignore=true -Pjacoco clean verify javadoc:javadoc'
                
              if (isUnix()) {
                sh script: mavenCommand
              }
              else {
                bat script: mavenCommand
              }
            }
          }
          
          script {
            def mavenConsoleIssues = scanForIssues tool: mavenConsole()
            def javaIssues = scanForIssues tool: java()
            def javaDocIssues = scanForIssues tool: javaDoc()
            def spotBugsIssues = scanForIssues tool: spotBugs()
            def taskScannerIssues = scanForIssues tool: taskScanner()
            publishIssues id: "analysis", issues: [mavenConsoleIssues, javaIssues, javaDocIssues, spotBugsIssues, taskScannerIssues]
          }
        }
      }
    }
  }
  
  post {
    always {
      script {
        if (params.wipeWorkspaceAfterBuild) {
          echo "Wiping workspace..."
          cleanWs(cleanWhenNotBuilt: false,
                  deleteDirs: true,
                  disableDeferredWipeout: true,
                  notFailBuild: true)
        }
      }
    }
  }
}