import grails.util.Metadata
import org.apache.commons.lang.StringUtils

/**
 * Script deploys generated documentation to the pre configured folder.
 *
 * @author Eugene Viktorovich
 */

includeTargets << grailsScript("_GrailsPackage")

String docsDeployPath = grailsSettings.config.grails.project.docs.deploy.path ?: ''

target(default: "Deploys generated documentation to the pre configured folder.") {
    runDeployGeneratedDocs()
}

target(runDeployGeneratedDocs: "Run to deploy generated grails documentation.") {
    depends(createConfig)

    event "StatusUpdate", ["Deploying documentation..."]

    String appName = ''
    String groupId = ''
    String version = ''

    if (grailsSettings.isPluginProject()) {
        appName = pluginSettings.getPluginInfo(basedir).getName()
        groupId = pluginSettings.getPluginInfo(basedir).attributes.groupId ?: 'org.grails.plugins'
        version = pluginSettings.getPluginInfo(basedir).getVersion()
    } else {
        Metadata metadata = Metadata.current
        appName = metadata.getApplicationName() ?: baseDir.name
        if (config.grails.project.groupId) {
            groupId = config.grails.project.groupId
        }
        version = metadata.getApplicationVersion()
    }

    Boolean isSuccessful = true
    [appName: appName, groupId: groupId, version: version].each { key, value ->
        if (StringUtils.isBlank(value)) {
            isSuccessful = false
            event "StatusError", ["'${key}' is not found."]
        }
    }

    if (isSuccessful) {
        grailsConsole.info """
**********************************************
    appName = ${appName}
    groupId = ${groupId}
    version = ${version}
**********************************************
"""

        if (new File(grailsSettings.docsOutputDir as String).exists()) {
            if (StringUtils.isNotBlank(docsDeployPath)) {
                File docsDeployFile = new File(docsDeployPath)
                if (docsDeployFile.exists()) {
                    File deployingFile = new File(docsDeployPath, "${groupId}/${appName}/${version}")

                    ant.delete(dir: deployingFile.path, failonerror: false)

                    ant.copydir(src: grailsSettings.docsOutputDir, dest: deployingFile.path)

                    event "StatusFinal", ["Documentation deployed from '$grailsSettings.docsOutputDir' to '$deployingFile.path' successfully."]
                } else {
                    event "StatusError", ["Deploy directory '$docsDeployFile.path' doesn't exists."]
                }
            } else {
                event "StatusError", ["Documentation deploy directory is not configured. Please configure 'grails.project.docs.deploy.path'."]
            }
        } else {
            event "StatusError", ["Generated documentation is not found by path '$grailsSettings.docsOutputDir'."]
        }
    }
}
