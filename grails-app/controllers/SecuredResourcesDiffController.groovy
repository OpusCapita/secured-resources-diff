import net.sf.json.JSON

/**
 * SecuredResourcesDiffController
 *
 * @author Dmitry Divin
 */
class SecuredResourcesDiffController {
    def securedResourcesDiffService

    /**
     * Show diff form
     */
    def index() {
        SecuredResourcesDiffCommand command = new SecuredResourcesDiffCommand()
        bindData(command, params)

        if (request.method == 'POST') {
            return diff(command)
        }
        List applications = securedResourcesDiffService.applications
        if (!command.application) {
            Map application = applications[0]
            command.application = "${application.groupId}:${application.artefactId}"
        }
        command.versionFrom = ""
        command.versionTo = ""

        String[] app = command.application.split(':')
        List versions = securedResourcesDiffService.getListOfVersions(app[0], app[1], command.showOnlyReleases)

        render view: 'diff', model: [command: command, applications: applications, versions: versions]
    }

    /**
     * Take diff
     */
    def diff(SecuredResourcesDiffCommand command) {
        Map model = [command: command, applications: securedResourcesDiffService.applications]
        if (command.validate()) {
            String[] app = command.application.split(':')

            List oldResources = securedResourcesDiffService.loadSecuredResources(app[1], command.versionFrom)

            if (oldResources == null) {
                command.errors.rejectValue('versionFrom', 'versionFrom.invalid', [command.versionFrom, app[1]] as Object[], 'Application version "{0}" doesn\'\'t exist')
            } else {
                List newResources = securedResourcesDiffService.loadSecuredResources( app[1], command.versionTo)
                if (newResources == null) {
                    command.errors.rejectValue('versionTo', 'versionTo.invalid', [command.versionTo, app[1]] as Object[], 'Application  version "{0}" doesn\'\'t exist')
                } else {
                    Closure equalsResource = {left, right->
                        return left.resourceId == right.resourceId && left.resourceType == right.resourceType && left.realm == right.realm
                    }

                    model.addedItems = newResources.findAll {!oldResources.any(equalsResource.curry(it))}
                    model.deletedItems = oldResources.findAll {!newResources.any(equalsResource.curry(it))}

                    model.checkedItems = true
                }
            }
        }
        if (!command.application) {
            Map application = model.applications[0]
            command.application = "${application.groupId}:${application.artefactId}"
        }
        String[] app = command.application.split(':')
        model.versions = securedResourcesDiffService.getListOfVersions(app[0], app[1], command.showOnlyReleases)

        render view: 'diff', model: model
    }
}
