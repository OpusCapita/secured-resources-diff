<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Secured Resources Diff</title>
    <meta name="layout" content="main"></head>

    <asset:script>
        function onApplicationChange(e) {
            var application = e.target.value;
            document.location.href = "${g.createLink(controller: "securedResourcesDiff", action: "index")}?application=" + application
        }
    </asset:script>
<body>
  <div class="container">
    <h2>
        Secured Resources Diff
        <div class="pull-right">
            <small>Application versions</small>
            <div class="btn-group btn-group-sm">
                <g:link controller="securedResourcesDiff" action="index" params="[showOnlyReleases: false]" class="btn btn-default ${command.showOnlyReleases ? '': 'active'}">All</g:link>
                <g:link controller="securedResourcesDiff" action="index" params="[showOnlyReleases: true]" class="btn btn-default ${command.showOnlyReleases ? 'active': ''}">Only Releases</g:link>
            </div>
        </div>
    </h2>

    <g:if test="${request.method == 'POST'}">
      <g:if test="${!command.hasErrors() && !addedItems && !deletedItems}">
        <div class="bs-callout bs-callout-info">
          Secured Resources are not changed
        </div>
      </g:if>
    </g:if>

    <g:form role="form" class="form-horizontal">
        <g:hiddenField name="showOnlyReleases" value="${command.showOnlyReleases}"/>
        <div class="form-group ${g.hasErrors([bean: command, field: 'application'], 'has-error')}">
            <label for="application" class="control-label col-sm-3">Application</label>
            <div class="col-sm-9">
                <g:select name="application" onchange="onApplicationChange(event)" class="form-control" from="${applications}" optionKey="${{"${it.groupId}:${it.artefactId}"}}" optionValue="title" value="${command.application}"/>
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
                <g:select name="versionFrom" noSelection="${['': '-- Select One --']}"
                          from="${versions}" class="form-control" value="${command.versionFrom}"/>
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
                <g:select name="versionTo" noSelection="${['': '-- Select One --']}"
                          from="${versions}" class="form-control" value="${command.versionTo}"/>
            </div>
            <g:hasErrors bean="${command}" field="versionTo">
                <div class="col-sm-9 col-sm-offset-3">
                    <span class="label label-danger">
                        <g:fieldError bean="${command}" field="versionTo"/>
                    </span>
                </div>
            </g:hasErrors>
        </div>

        <div class="form-submit text-right">
            <div class="form-inline">
                <button type="submit" class="btn btn-primary">Generate</button>
            </div>
        </div>
    </g:form>

    <g:if test="${addedItems || deletedItems}">
        <h2>Wiki markup</h2>
        <textarea id="wiki_markup" style="width: 100%" rows="10">
<g:if test="${addedItems}">h2. New Resources
||Type||Realm||Resource Id||Description||
<g:each in="${addedItems}" var="item">|${item.resourceType.encodeAsHTML()}|${item.realm.encodeAsHTML()}|${item.resourceId.encodeAsHTML()}|${item.description.encodeAsHTML()}|
</g:each>
</g:if>
<g:if test="${deletedItems}">h2. Deleted Resources
||Type||Realm||Resource Id||Description||
<g:each in="${deletedItems}" var="item">|${item.resourceType.encodeAsHTML()}|${item.realm.encodeAsHTML()}|${item.resourceId.encodeAsHTML()}|${item.description.encodeAsHTML()}|
</g:each>
            </g:if>
        </textarea>
        <div class="text-right">
            <button type="button" class="btn btn-default" data-clipboard-target="wiki_markup">Copy to clipboard</button>
        </div>
    </g:if>

    <g:if test="${addedItems}">
        <div>
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
      <div>
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
