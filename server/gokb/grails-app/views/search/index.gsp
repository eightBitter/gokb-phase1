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
              <li><g:link controller="search" action="index" params="${[qbe:'g:rules']}">Rules</g:link></li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:projects']}">Projects</g:link></li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:tipps']}">TIPPs</g:link></li>
              <li><g:link controller="search" action="index" params="${[qbe:'g:refdataCategories']}">Refdata</g:link></li>
            </ul>
          </div><!--/.well -->
        </div><!--/span-->

        <div id="mainarea" class="${displayobj != null ? 'span5' : 'span10'}">
          <div class="well">
            <g:if test="${qbetemplate==null}">
              Please select a template from the navigation menu
            </g:if>
            <g:else>
              <div class="navbar">
                <div class="navbar-inner">
                  <div class="brand">${qbetemplate.title?:'Search'}
                    <g:if test="${recset != null}"> : Records ${offset+1} to ${lasthit} of ${reccount}</g:if>
                  </div>
                  <g:if test="${recset != null}">
                    <ul class="nav pull-right">
                      <li><g:link controller="search" action="index" params="${params+[offset:(offset-max),det:null]}">Prev</g:link></li>
                      <li class="divider-vertical"></li>
                      <li><g:link controller="search" action="index" params="${params+[offset:(offset+max),det:null]}">Next</g:link></li>
                    </ul>
                  </g:if>
                </div>
              </div>
              <g:render template="qbeform" contextPath="." model="${[formdefn:qbetemplate.qbeConfig?.qbeForm]}"/>
              <g:if test="${recset != null}">
                <g:render template="qberesult" contextPath="." model="${[qbeConfig:qbetemplate.qbeConfig, rows:recset, offset:offset, det:det]}"/>
              </g:if>
            </g:else>
          </div>
        </div>

        <g:if test="${displayobj != null}">
          <div id="mainarea" class="span5">
            <div class="well">

              <div class="navbar">
                <div class="navbar-inner">
                  <div class="brand">Record ${det} of ${reccount}</div>
                  <ul class="nav pull-right">
                    <li><g:link controller="search" action="index" params="${params+['det':det-1, offset:((int)((det-2) / max))*max]}">Prev</g:link></li>
                    <li class="divider-vertical"></li>
                    <li><g:link controller="search" action="index" params="${params+['det':det+1, offset:((int)(det / max))*max]}">Next</g:link></li>
                  </ul></div></div>
              <g:if test="${displaytemplate != null}">
                <g:if test="${displaytemplate.type=='staticgsp'}">
                  <g:render template="${displaytemplate.rendername}" contextPath="../apptemplates" model="${[d:displayobj]}"/>
                </g:if>
              </g:if>
              <g:else>
                No template currenly available for instances of ${displayobjclassname}
                ${displayobj as grails.converters.JSON}
              </g:else>
            </div>
          </div>
        </g:if>

      </div>
    </div>
  </body>
</html>
