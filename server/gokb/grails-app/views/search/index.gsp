<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <r:require modules="gokbstyle"/>
    <title>GOKb</title>
  </head>
  <body>

    <div class="container-fluid">
      <div class="row-fluid">
        <div id="sidebar" class="span2">
          <div class="well sidebar-nav">
            <ul class="nav nav-list">
              <li class="nav-header">Search In</li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:components']}">Components</g:link></li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:packages']}">Packages</g:link></li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:orgs']}">Orgs</g:link></li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:platforms']}">Platforms</g:link></li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:titles']}">Titles</g:link></li>
            </ul>
          </div><!--/.well -->
        </div><!--/span-->
        <div id="mainarea" class="span5">
          <div class="well">
            <g:if test="${qbetemplate==null}">
              Please select a search template from the left nav bar
            </g:if>
            <g:else>
              <h1>${qbetemplate.title?:'Search'}</h1>
              <g:render template="qbeform" contextPath="." model="${[formdefn:qbetemplate.qbeConfig?.qbeForm]}"/>
              <g:if test="${recset != null}">
                <g:render template="qberesult" contextPath="." model="${[qbeConfig:qbetemplate.qbeConfig, rows:recset]}"/>
              </g:if>
            </g:else>
          </div>
        </div>
        <div id="mainarea" class="span5">
          <div class="well">
            <g:if test="${activerecord==null}">
             Selected records details will appear here
           </g:if>
           <g:else>
           </g:else>
          </div>
        </div>
      </div>
    </div>
  </body>
</html>
