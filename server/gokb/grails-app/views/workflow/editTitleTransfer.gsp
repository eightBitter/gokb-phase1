<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <r:require modules="gokbstyle"/>
    <r:require modules="editable"/>
    <title>GOKb::Title Transfer</title>
  </head>
  <body>
    <div class="container-fluid">
      <g:form controller="workflow" action="editTitleTransfer" id="${params.id}">
        <div class="row-fluid">
          <div class="span12 hero well">
            Title Transfer (2/2)
          </div>
        </div>
        <div class="row-fluid">
  
          <div class="span12">

            The following titles:
            <ul>
              <g:each in="${titles}" var="title">
                <li>${title.name}</li>
              </g:each>
            </ul>

            Will be transferred from their current publisher to ${newPublisher.name}. <span style="background-color:#FF4D4D;">Current tipps shown with a red background</span> will be deprecated. <span style="background-color:#11bb11;">New tipps with a green background</span> will be created by this transfer.

            <table class="table">
              <thead>
                <tr>
                  <th>Select</th>
                  <th>Type</th>
                  <th>Current Title</th>
                  <th>Current Package</th>
                  <th>Current Platform</th>
                  <th>Start Date</th>
                  <th>Start Volume</th>
                  <th>Start Issue</th>
                  <th>End Date</th>
                  <th>End Volume</th>
                  <th>End Issue</th>
                  <th>Close</th>
                  <th>Review</th>
                </tr>
              </thead>
              <tbody>
                <g:each in="${tipps}" var="tipp">
                  <tr style="background-color: #FF4D4D;">
                    <td><input name="addto-${tipp.id}" type="checkbox" checked="true"/></td>
                    <td>CURRENT</td>
                    <td>${tipp.title.name}</td><td>${tipp.pkg.name}</td><td>${tipp.hostPlatform.name}</td>
                    <td><g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.startDate}"/></td>
                    <td>${tipp.startVolume}</td>
                    <td>${tipp.startIssue}</td>
                    <td><g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.endDate}"/></td>
                    <td>${tipp.endVolume}</td>
                    <td>${tipp.endIssue}</td>
                    <td><input name="close-${tipp.id}" type="checkbox" checked="true"/></td>
                    <td><input name="review-${tipp.id}" type="checkbox" checked="true"/></td>
                  </tr>
                  <g:each in="${newtipps[tipp.id]}" var="newtipp">
                    <tr>
                      <td><input type="checkbox" checked="true"/></td>
                      <td>NEW</td>
                      <td>${newtipp.title.name}</td><td>${newtipp.pkg.name}</td><td>${newtipp.hostPlatform.name}</td>
                      <td></td>
                      <td></td>
                      <td></td>
                      <td></td>
                      <td></td>
                      <td></td>
                      <td></td>
                      <td><input type="checkbox" checked="true"/></td>
                    </tr>
                  </g:each>
                </g:each>
              </tbody>
            </table>

            Use the following form to indicate the package and platform for new TIPPs. Select/Deselect TIPPS above to indicate

            <dl class="dl-horizontal">
              <div class="control-group">
                <dt>New Package</dt>
                <dd><g:simpleReferenceTypedown class="input-xxlarge" style="width:350px;" name="Package" baseClass="org.gokb.cred.Package"/></dd>
              </div>

              <div class="control-group">
                <dt>New Platform</dt>
                <dd><g:simpleReferenceTypedown class="input-xxlarge" style="width:350px;" name="Platform" baseClass="org.gokb.cred.Platform"/></dd>
              </div>
              <div class="control-group">
                <dt></dt>
                <dd><button type="submit" class="btn btn-primary" name="addTransferTipps" value="AddTipps">Add transfer tipps</button></dd>
              </div>
            </dl>
 
            <br/>
                <button type="submit" class="btn btn-primary" name="process" value="process">Process Transfer</button>
          </div>

 
        </div>
      </g:form>
    </div>
  </body>
</html>

