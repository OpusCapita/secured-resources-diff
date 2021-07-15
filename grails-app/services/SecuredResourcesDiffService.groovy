import com.azure.identity.UsernamePasswordCredential
import com.azure.identity.UsernamePasswordCredentialBuilder
import com.microsoft.graph.authentication.TokenCredentialAuthProvider
import com.microsoft.graph.models.DriveItemSearchParameterSet
import com.microsoft.graph.requests.DriveItemCollectionPage
import com.microsoft.graph.requests.GraphServiceClient
import groovy.util.slurpersupport.GPathResult
import okhttp3.Request

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

    List getListOfVersions(String groupId, String artefactId, boolean onlyReleases = true) {
        String ownerId = "9fbc943c-74d6-4352-aac3-561fca65cfdb"

        DriveItemCollectionPage itemCollectionPage = graphClient.users(ownerId)
            .drive().root().itemWithPath("applications_storage/$artefactId").children().buildRequest().get()
        ArrayList<String> files = itemCollectionPage.currentPage.name.findAll {it.contains('.war')}

        def result = []

        if (!files.empty) {
            result.addAll(files.collect { it.substring(0, it.lastIndexOf(".")).minus("${artefactId.toLowerCase()}-") })

            if (onlyReleases) {
                return result.findAll { it.indexOf("SNAPSHOT") == -1 }
            }
        }

        return result
    }

    /**
     * Download application WAR by unique fields (groupId+artefactId+version) from maven repository
     * and load secured resources
     */
    List loadSecuredResources(String artefactId, String version) {
        String securedResourcesPath = configurationProperties['securedResources.path']
        def search = graphClient.users("9fbc943c-74d6-4352-aac3-561fca65cfdb")
            .drive()
            .root()
            .search(DriveItemSearchParameterSet
                .newBuilder()
                .withQ("${artefactId}-${version}.war")
                .build())
            .buildRequest()
            .get().currentPage.findAll {it.name.contains("${artefactId}-${version}.war")}

        List result = []

        InputStream is = null
        try {
            is = graphClient.users("9fbc943c-74d6-4352-aac3-561fca65cfdb").drive().items(search.get(0).id).content().buildRequest().get();
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

    private GraphServiceClient<Request> getGraphClient() {
        String appId = "76bbe1e4-3c3f-4e51-97d2-2ce409f28e09"
        String username = configurationProperties['onedrive.repository.username']
        String password = configurationProperties['onedrive.repository.password']
        List<String> appScopes = Arrays.asList("User.Read Files.Read.All Files.ReadWrite.All Sites.Read.All Sites.ReadWrite.All")

        UsernamePasswordCredential usernamePasswordCredential = new UsernamePasswordCredentialBuilder()
            .clientId(appId)
            .username(username)
            .password(password)
            .build();
        TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(appScopes, usernamePasswordCredential);

        GraphServiceClient graphClient = GraphServiceClient
            .builder()
            .authenticationProvider(tokenCredentialAuthProvider)
            .buildClient()
        graphClient
    }
}
