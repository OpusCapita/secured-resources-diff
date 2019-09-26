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
    static transactional = false
    Map configurationProperties

    List getListOfVersions(String groupId, String artefactId) {
        if (configurationProperties['maven.repository.server'] == 'nexus') {
            List result = getLisOfVersionsFromNexus(groupId, artefactId)
            if (artefactId == 'proc') {
                return getLisOfVersionsFromNexus('com.jcatalog.procurement', 'procurement') + result
            } else {
                return result
            }
        } else {
            return getLisOfVersionsFromArtifactory(groupId, artefactId)
        }
    }

    private List getLisOfVersionsFromNexus(String groupId, String artefactId) {
        String repositoryUrl = configurationProperties['maven.repository.nexus.metadata.url']
        String username = configurationProperties['maven.repository.username']
        String password = configurationProperties['maven.repository.password']

        URL url = new URL(repositoryUrl + "/${groupId.replace('.', '/')}/${artefactId}/maven-metadata.xml")

        HTTPBuilder http = new HTTPBuilder(url.protocol + ":" + (url.port == -1?'':url.port)+ "//" + url.host)

        if (username && password) {
            http.auth.basic(username, password)
        }

        def xml = http.get([path: url.path])

        List result = []
        for (version in xml.versioning.versions.version) {
            result << version.text()
        }

        return result
    }

    private List getLisOfVersionsFromArtifactory(String groupId, String artefactId) {
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
        if (configurationProperties['maven.repository.server'] == 'nexus') {
            return loadSecuredResourcesFromNexus(groupId, artefactId, version)
        }
        return loadSecuredResourcesFromArtifactory(groupId, artefactId, version)
    }

    List loadSecuredResourcesFromArtifactory(String groupId, String artefactId, String version) {
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

    List loadSecuredResourcesFromNexus(String groupId, String artefactId, String version) {
        String repositoryUrl = configurationProperties['maven.repository.nexus.url']
        String repositoryName = configurationProperties['maven.repository.nexus.name']
        String username = configurationProperties['maven.repository.username']
        String password = configurationProperties['maven.repository.password']
        String securedResourcesPath = configurationProperties['securedResources.path']

        URL url = new URL(repositoryUrl)

        HTTPBuilder http = new HTTPBuilder(url.protocol + ":" + (url.port == -1?'':url.port)+ "//" + url.host)

        if (username && password) {
            http.auth.basic(username, password)
        }


        InputStream is = null
        try {
            is = http.get([path: url.path, query: [r: repositoryName, g: groupId, a: artefactId, v: version, e: 'war']])
        } catch (e1) {
            // for Procurement we probably need to use 'old coordinates
            // group id 'com.jcatalog.procurement' and artefact id 'procurement'
            if (artefactId?.equalsIgnoreCase('proc')) {
                try {
                    is = http.get([path: url.path, query: [r: repositoryName, g: 'com.jcatalog.procurement', a: 'procurement', v: version, e: 'war']])
                } catch (e2) {
                    log.error "download war file with errors in maven", e2
                    return null
                }
            } else {
                log.error "download war file with errors in maven", e1
                return null
            }
        }
        def result = []
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
