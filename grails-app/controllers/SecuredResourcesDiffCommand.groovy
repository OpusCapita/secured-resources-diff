import grails.validation.Validateable

/**
 * Command definition
 *
 * @author Dmitry Divin
 */
@Validateable
class SecuredResourcesDiffCommand {
    //arterfactId
    String application
    //version from
    String versionFrom
    //version to
    String versionTo
    //show only releases
    boolean showOnlyReleases = true

    static constraints = {
        application(nullable: false, blank: false)
        versionFrom(nullable: false, blank: false)
        versionTo(nullable: false, blank: false)
    }
}
