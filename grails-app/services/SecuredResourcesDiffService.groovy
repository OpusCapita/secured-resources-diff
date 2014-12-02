import groovy.util.slurpersupport.GPathResult
import groovyx.net.http.HTTPBuilder

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * SecuredResourcesDiffService
 *
 * @author Dmitry Divin
 */
class SecuredResourcesDiffService {
    Map configurationProperties

    /**
     * Download application WAR by unique fields (groupId+artefactId+version) from maven repository
     * and load secured resources
     */
    List loadSecuredResources(String groupId, String artefactId, String version) {
        String repositoryUrl = configurationProperties['maven.repository.url']
        String username = configurationProperties['maven.repository.username']
        String password = configurationProperties['maven.repository.password']
        String securedResourcesPath = configurationProperties['securedResources.path']

        URL url = new URL(repositoryUrl)

        HTTPBuilder http = new HTTPBuilder(url.protocol + ":" + (url.port == -1?'':url.port)+ "//" + url.host)

        if (username && password) {
            http.auth.basic(username, password)
        }

        List result = []

        String queryPath = url.path + "/" + groupId.replaceAll('\\.', '/') + "/$artefactId/${version}/${artefactId}-${version}.war"

        InputStream is = null
        try {
            is = http.get([path: queryPath])
        } catch (e) {
            log.error "download war file with errors in maven", e

            return null
        }
        ZipInputStream zipIn = new ZipInputStream(is)
        ZipEntry entry = zipIn.nextEntry
        while(entry != null) {
            if (entry.name == securedResourcesPath) {
                GPathResult xml = new XmlSlurper().parse(zipIn)
                for (item in xml.SecuredResource) {
                    result << [description: item.@description.text(), resourceId: item.ResourceId.text(), resourceType: item.ResourceType.text(), realm: item.Realm.text()]
                }
                break
            }

            entry = zipIn.nextEntry
        }
        zipIn.close()

        return result
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
