<r:require modules="gokbstyle"/>
<r:require modules="editable"/>

<h1>${d.id ? d.getNiceName() + ': ' + (d.name ?: d.id) : 'Create New ' + d.getNiceName()}</h1>


<div id="content">
  <ul id="tabs" class="nav nav-tabs">
    <li class="active"><a href="#rrdets" data-toggle="tab">Review Request Details</a></li>
  </ul>
  <div id="my-tab-content" class="tab-content">
    <div class="tab-pane active" id="rrdets">
      <dl class="dl-horizontal">
	      <dt><g:annotatedLabel owner="${d}" property="id">Internal ID</g:annotatedLabel></dt>
	      <dd>${d.id?:'New record'}&nbsp;</dd>
	      <dt><g:annotatedLabel owner="${d}" property="cause">Cause</g:annotatedLabel></dt>
	      <dd><g:xEditable class="ipe" owner="${d}" field="descriptionOfCause"/></dd>
	      <dt><g:annotatedLabel owner="${d}" property="reviewRequest">Review Request</g:annotatedLabel></dt>
	      <dd><g:xEditable class="ipe" owner="${d}" field="reviewRequest"/></dd>
	      <dt><g:annotatedLabel owner="${d}" property="status">Request Status</g:annotatedLabel></dt>
	      <dd><g:xEditableRefData owner="${d}" field="status" config='ReviewRequest.Status' /></dd>
	      <dt><g:annotatedLabel owner="${d}" property="target">Component</g:annotatedLabel></dt>
	      <dd><g:manyToOneReferenceTypedown owner="${d}" field="componentToReview" baseClass="org.gokb.cred.KBComponent">${d.componentToReview?.name?:''}</g:manyToOneReferenceTypedown>
	      	<g:componentLink object="${d?.componentToReview}" target="_blank" title="View the component (opens in new window)" >view</g:componentLink></dd>

        <g:if test="${d.id != null}">
	        <dt><g:annotatedLabel owner="${d}" property="dateCreated">Request Timestamp</g:annotatedLabel></dt>
	        <dd>${d.dateCreated}&nbsp;</dd>
        </g:if>
        <g:else>
          Additional fields will be available once the record is saved
        </g:else>
    </dl>
  </div>
</div>

