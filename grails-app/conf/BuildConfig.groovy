grails.servlet.version = "3.0"
grails.work.dir = "target"
grails.project.target.level = 1.8
grails.project.source.level = 1.8

grails.project.sourceUrl = "http://buildserver.jcatalog.com/hg-devops/secured-resources-dif"

grails.project.dependency.resolution = {
    inherits "global"
    log "warn"
    checksums true

    dependencies {
        runtime 'org.codehaus.groovy.modules.http-builder:http-builder:0.7'
    }

    plugins {
        // build ('com.jcatalog.grailsplugins:build-process:7.18.GA.7') {
        //   excludes "google-oauth-client-jetty"
        // }

        compile ":twitter-bootstrap:3.3.1"
        compile ":asset-pipeline:1.9.9"
        runtime ":jquery:1.11.1"

        build ':release:3.0.1'
        build ":tomcat:7.0.70"
   }
}
