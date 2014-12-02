grails.servlet.version = "3.0"
grails.work.dir = "target"
grails.project.target.level = 1.7
grails.project.source.level = 1.7

grails.project.sourceUrl = "http://buildserver.jcatalog.com/hg-devops/secured-resources-dif"

// deployment configuration
grails.project.repos.default = grailsSettings.config.jcatalog.repos.default
grails.project.repos.mavenDeploy.url = "${grailsSettings.config.jcatalog.repos.mavenDeploy.url}${appVersion && appVersion.endsWith('SNAPSHOT')?'/snapshots':'/releases'}"
grails.project.repos.mavenDeploy.username = grailsSettings.config.jcatalog.repos.mavenDeploy.username
grails.project.repos.mavenDeploy.password = grailsSettings.config.jcatalog.repos.mavenDeploy.password


grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    run: false,
    // configure settings for the run-war JVM
    war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

// dependencies
grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    inherits("global") {
    }
    log "warn"
    checksums true

    repositories {
        if (grailsSettings.config.jcatalog.mavenLocal) {
            mavenLocal (grailsSettings.config.jcatalog.mavenLocal)
        } else {
            mavenLocal ()
        }
        if (grailsSettings.config.jcatalog.mavenCentral) {
            mavenRepo (grailsSettings.config.jcatalog.mavenCentral) {
                updatePolicy 'always'
            }
        }
    }

    dependencies {
        runtime 'org.codehaus.groovy.modules.http-builder:http-builder:0.7'
    }

    plugins {
        build 'com.jcatalog.grailsplugins:build-process:7.14.GA'

        compile ":twitter-bootstrap:3.3.1"
        compile ":asset-pipeline:1.9.9"
        runtime ":jquery:1.11.1"

        runtime ":hibernate:3.6.10.8"
        build ':release:3.0.1'
        build ":tomcat:7.0.54"
   }
}
