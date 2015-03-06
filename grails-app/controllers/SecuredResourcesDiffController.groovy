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
        render view: 'diff', model: [command: new SecuredResourcesDiffCommand(), applications: securedResourcesDiffService.applications]
    }

    /**
     * Take diff
     */
    def diff(SecuredResourcesDiffCommand command) {
        Map model = [command: command, applications: securedResourcesDiffService.applications]
        if (command.validate()) {
            String[] app = command.application.split(':')

            List oldResources = securedResourcesDiffService.loadSecuredResources(app[0], app[1], command.versionFrom)

            if (oldResources == null) {
                command.errors.rejectValue('versionFrom', 'versionFrom.invalid', [command.versionFrom, app[1]] as Object[], 'Application "{1}" with version "{0}" doesn\'\'t exist')
            } else {
                List newResources = securedResourcesDiffService.loadSecuredResources(app[0], app[1], command.versionTo)
                if (newResources == null) {
                    command.errors.rejectValue('versionTo', 'versionTo.invalid', [command.versionTo, app[1]] as Object[], 'Application "{1}" with version "{0}" doesn\'\'t exist')
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
        render view: 'diff', model: model
    }
}
