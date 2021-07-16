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
    private static final String USER_ID = "9fbc943c-74d6-4352-aac3-561fca65cfdb"
    // UserId of owner of the applications_storage folder
    private static final String APP_ID = "b4380407-7dbc-44d6-8a38-f81a1846a87f"
    // Client_id of azure app which we used to connect for download wars

    List getListOfVersions(String artefactId, boolean onlyReleases = true) {

        DriveItemCollectionPage itemCollectionPage = graphClient.users(USER_ID)
            .drive().root().itemWithPath("applications_storage/$artefactId").children().buildRequest().get()
        ArrayList<String> files = itemCollectionPage.currentPage.name.findAll { it.contains('.war') }

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
        def search = graphClient.users(USER_ID)
            .drive()
            .root()
            .search(DriveItemSearchParameterSet
                .newBuilder()
                .withQ("${artefactId}-${version}.war")
                .build())
            .buildRequest()
            .get().currentPage.findAll { it.name.contains("${artefactId}-${version}.war") }

        List result = []

        InputStream is = null
        try {
            is = graphClient.users(USER_ID).drive().items(search.get(0).id).content().buildRequest().get();
        } catch (e) {
            log.error "download war file with errors", e

            return null
        }
        ZipInputStream zipIn = new ZipInputStream(is)
        ZipEntry entry = zipIn.nextEntry
        while (entry != null) {
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
        List apps = configurationProperties['applications'].split(',').collect { it.trim() }

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
        String username = configurationProperties['onedrive.repository.username']
        String password = configurationProperties['onedrive.repository.password']
        List<String> appScopes = ["User.Read", "Files.Read.All", "Files.ReadWrite.All", "Sites.Read.All", "Sites.ReadWrite.All"]
        // used the User/Pass Auth which is not recommended, need to use one of other ways, but Subscriptions administrator rights are needed https://docs.microsoft.com/en-us/graph/sdks/choose-authentication-providers?tabs=Java
        UsernamePasswordCredential usernamePasswordCredential = new UsernamePasswordCredentialBuilder()
            .clientId(APP_ID)
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
