<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <r:require modules="gokbstyle"/>
    <r:require modules="editable"/>
    <title>GOKb::Package - Register Webhook </title>
  </head>
  <body>
    <div class="container">
      <g:form controller="workflow" action="processCreateWebHook" method="get">
        <input type="hidden" name="from" value="${request.getHeader('referer')}"/>

        <div class="row">
          <div class="col-md-12 hero well">
            Register Webhook
          </div>
        </div>
        <div class="row">
  
          <div class="col-md-12">
            <img class="pull-right" src="${resource(dir: 'images', file: 'WebHook.png')}"/>
            
            Register WebHook callbacks for:<br/>
            <g:each in="${objects_to_action}" var="o">
              <input type="checkbox" name="tt:${o.id}" checked="true"/> ${o.name}<br/>
            </g:each>
            <hr>

            <h3>Link to Existing hook</h3>
            <dl class="dl-horizontal">
              <dt>Url</dt>
              <dd><g:simpleReferenceTypedown class="input-xxlarge" style="width:350px;" name="existingHook" baseClass="org.gokb.cred.WebHookEndpoint" filter1="${request.user?.id}"/></dd>
              <dt></td><dd><button type="submit">Create Web Hook</button></dd>
           
            </dl>

            <hr>

            <h3>Link to New hook</h3>
            <dl class="dl-horizontal">
              <dt>Hook Name</dt> <dd><input type="text" name="newHookName"/></dd>
              <dt>Url</dt> <dd><input type="text" name="newHookUrl"/></dd>
              <dt>Auth</dt> <dd><select name="newHookAuth">
                                  <option value="0">Anonymous (No Auth)</option>
                                  <option value="1">HTTP(s) Basic</option>
                                  <option value="2">Signed HTTP Requests</option>
                                </select></dd>
              <dt>Principal</dt> <dd><input type="text" name="newHookPrin"/></dd>
              <dt>Credentials</dt> <dd><input type="text" name="newHookCred"/></dd>
              <dt></td><dd><button type="submit">Create Web Hook</button></dd>
            </dl>

          </div>
        </div>
      </g:form>
    </div>
  </body>
</html>

