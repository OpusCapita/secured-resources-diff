import groovy.util.slurpersupport.GPathResult

/**
 * SecuredResourcesDiffService
 *
 * @author Dmitry Divin
 */
class SecuredResourcesDiffService {
    static transactional = false
    Map configurationProperties

    List getListOfVersions(String groupId, String artefactId) {
        String repositoryUrl = configurationProperties['maven.repository.artifactory.url']
        String username = configurationProperties['maven.repository.username']
        String password = configurationProperties['maven.repository.password']

        URL url = new URL ("${repositoryUrl}/${groupId.replace('.', '/')}/${artefactId}/maven-metadata.xml")
        def xmlContent = url.getText(['requestProperties': ["Authorization": "Basic " + "${username}:${password}".bytes.encodeBase64().toString()]])
        def result = []
        GPathResult xml = new XmlSlurper().parseText(xmlContent)
        for (version in xml.versioning.versions.version) {
            result << version.text()
        }
        return result
    }

    /**
     * Download application WAR by unique fields (groupId+artefactId+version) from maven repository
     * and load secured resources
     */
    List loadSecuredResources(String groupId, String artefactId, String version) {
        String repositoryUrl = configurationProperties['maven.repository.artifactory.url']
        String username = configurationProperties['maven.repository.username']
        String password = configurationProperties['maven.repository.password']
        String securedResourcesPath = configurationProperties['securedResources.path']

        URL url = new URL ("${repositoryUrl}/${groupId.replace('.', '/')}/${artefactId}/${version}/${artefactId}-${version}.war!/${securedResourcesPath}")
        try {
            def xmlContent = url.getText(['requestProperties': ["Authorization": "Basic " + "${username}:${password}".bytes.encodeBase64().toString()]])
            // println "${xmlContent}"
            try {
                def result = []
                GPathResult xml = new XmlSlurper().parseText(xmlContent)
                for (item in xml.SecuredResource) {
                    result << [description: item.@description.text(), resourceId: item.ResourceId.text(), resourceType: item.ResourceType.text(), realm: item.Realm.text()]
                }
                return result
            } catch (e) {
                log.error "download parsing secured resources xml file", e
            }
        } catch (e) {
            log.error "download war file with errors in maven", e
        }

    }

    /**
     * Get applications from configuration properties
     */
    List getApplications() {
        List apps = configurationProperties['applications'].split(',').collect {it.trim()}

        List result = []
        for (String app in apps) {
            String artefactId = configurationProperties["applications.${app}.artefactId"]
            String groupId = configurationProperties["applications.${app}.groupId"]


            if (artefactId && groupId) {
                String title = configurationProperties["applications.${app}.title"] ?: artefactId.toUpperCase()

                result << [artefactId: artefactId, groupId: groupId, title: title]
            }
        }

        return result
    }
}
