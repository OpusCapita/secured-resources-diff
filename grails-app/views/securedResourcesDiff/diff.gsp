<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Secured Resources diff</title>
    <meta name="layout" content="main"></head>

<body>

    <g:if test="${!(addedItems && deletedItems) && checkedItems}">
        <div class="alert alert-info">Nothing changes in Secured Resources</div>
    </g:if>

    <g:form action="diff" role="form">
        <div class="form-horizontal">
            <div class="row">
                <div class="col-md-offset-3 col-md-6">
                    <fieldset>
                        <legend>Secured Resources diff</legend>
                        <div class="form-group ${g.hasErrors([bean: command, field: 'application'], 'has-error')}">
                            <label for="application" class="control-label col-sm-3">Application</label>
                            <div class="col-sm-9">
                                <g:select name="application" class="form-control" from="${applications}" optionKey="${{"${it.groupId}:${it.artefactId}"}}" optionValue="title" value="${command.application}"/>
                            </div>
                            <g:hasErrors bean="${command}" field="application">
                                <div class="col-sm-9 col-sm-offset-3">
                                    <span class="label label-danger">
                                        <g:fieldError bean="${command}" field="application"/>
                                    </span>
                                </div>
                            </g:hasErrors>
                        </div>

                        <div class="form-group ${g.hasErrors([bean: command, field: 'versionFrom'], 'has-error')}">
                            <label for="versionFrom" class="control-label col-sm-3">Version from</label>
                            <div class="col-sm-9">
                                <g:textField name="versionFrom" class="form-control" value="${command.versionFrom}"/>
                            </div>
                            <g:hasErrors bean="${command}" field="versionFrom">
                                <div class="col-sm-9 col-sm-offset-3">
                                    <span class="label label-danger">
                                        <g:fieldError bean="${command}" field="versionFrom"/>
                                    </span>
                                </div>
                            </g:hasErrors>
                        </div>

                        <div class="form-group ${g.hasErrors([bean: command, field: 'versionTo'], 'has-error')}">
                            <label for="versionTo" class="control-label col-sm-3">Version to</label>
                            <div class="col-sm-9">
                                <g:textField name="versionTo" class="form-control" value="${command.versionTo}"/>
                            </div>
                            <g:hasErrors bean="${command}" field="versionTo">
                                <div class="col-sm-9 col-sm-offset-3">
                                    <span class="label label-danger">
                                        <g:fieldError bean="${command}" field="versionTo"/>
                                    </span>
                                </div>
                            </g:hasErrors>
                        </div>

                        <div class="pull-right">
                            <button type="submit" class="btn btn-primary">Generate</button>
                        <div>
                    </fieldset>
                </div>
            </div>
        </div>
    </g:form>

    <g:if test="${addedItems}">
        <h2>New Resources</h2>
        <table class="table">
            <tr>
                <th>Type</th>
                <th>Realm</th>
                <th>Resource Id</th>
                <th>Description</th>
            </tr>
            <g:each in="${addedItems}" var="item">
                <tr>
                    <td>${item.resourceType.encodeAsHTML()}</td>
                    <td>${item.realm.encodeAsHTML()}</td>
                    <td>${item.resourceId.encodeAsHTML()}</td>
                    <td>${item.description.encodeAsHTML()}</td>
                </tr>
            </g:each>
        </table>
    </g:if>

    <g:if test="${deletedItems}">
        <h2>Deleted Resources</h2>
        <table class="table">
            <tr>
                <th>Type</th>
                <th>Realm</th>
                <th>Resource Id</th>
                <th>Description</th>
            </tr>
            <g:each in="${deletedItems}" var="item">
                <tr>
                    <td>${item.resourceType.encodeAsHTML()}</td>
                    <td>${item.realm.encodeAsHTML()}</td>
                    <td>${item.resourceId.encodeAsHTML()}</td>
                    <td>${item.description.encodeAsHTML()}</td>
                </tr>
            </g:each>
        </table>
    </g:if>

</body>
</html>