class BuildProcessGrailsPlugin {
    // the plugin version
    def version = "7.14.GA"
    def groupId = "com.jcatalog.grailsplugins"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.6 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]

    // TODO Fill in these fields
    def author = "Eugeny Pozharsky"
    def authorEmail = "pozheg@scand.com"
    def title = "Helpers to build grails plugins and app"
    def description = "Contain tools that helps to do some works at the grails project's build phase."

    def documentation = "http://buildserver.jcatalog.com/generated-docs/${groupId}/build-process/${version}/"
}
