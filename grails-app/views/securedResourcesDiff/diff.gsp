<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Secured Resources Diff</title>
    <meta name="layout" content="main"></head>
<body>
  <div class="container">
    <h2>Secured Resources Diff</h2>

    <g:if test="${request.method == 'POST'}">
      <g:if test="${!addedItems && !deletedItems}">
        <div class="bs-callout bs-callout-info">
          Secured Resources are not changed
        </div>
      </g:if>
    </g:if>

    <g:form action="diff" role="form" class="form-horizontal">
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
    </g:form>

    <g:if test="${addedItems || deletedItems}">
        <h2>Wiki markup</h2>
        <textarea id="wiki_markup" style="width: 100%" rows="10">
            <g:if test="${addedItems}">
h2. New Resources
||Type||Realm||Resource Id||Description||
<g:each in="${addedItems}" var="item">|${item.resourceType.encodeAsHTML()}|${item.realm.encodeAsHTML()}|${item.resourceId.encodeAsHTML()}|${item.description.encodeAsHTML()}|
</g:each>
            </g:if>
            <g:if test="${deletedItems}">
h2. Deleted Resources
||Type||Realm||Resource Id||Description||
<g:each in="${deletedItems}" var="item">|${item.resourceType.encodeAsHTML()}|${item.realm.encodeAsHTML()}|${item.resourceId.encodeAsHTML()}|${item.description.encodeAsHTML()}|
</g:each>
            </g:if>
        </textarea>
        <div class="row pull-right">
            <button type="button" class="btn btn-default" data-clipboard-target="wiki_markup">Copy to clipboard</button>
        </div>
    </g:if>

    <g:if test="${addedItems}">
        <div class="row">
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
        </div>
    </g:if>

    <g:if test="${deletedItems}">
      <div class="row">
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
      </div>
    </g:if>
  </div>
</body>
</html>
