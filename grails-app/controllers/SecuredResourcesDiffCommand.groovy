import grails.validation.Validateable

/**
 * Command definition
 *
 * @author Dmitry Divin
 */
@Validateable
class SecuredResourcesDiffCommand {
    //groupId with arterfactId
    String application
    //version from
    String versionFrom
    //version to
    String versionTo

    static constraints = {
        application(nullable: false, blank: false)
        versionFrom(nullable: false, blank: false)
        versionTo(nullable: false, blank: false)
    }
}
