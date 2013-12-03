<r:require modules="gokbstyle" />
<r:require modules="editable" />


<h3>
  <g:if test="${d.id != null}">
    ${d.getNiceName()} : ${d.name ?: d.id} -
    <g:if test="${d.title != null}">
      <g:link controller="resource" action="show"
        id="${d.title.class.name+':'+d.title.id}">
        ${d.title.name}
      </g:link> in
    </g:if>
    <g:if test="${d.pkg != null}">
      <g:link controller="resource" action="show"
        id="${d.pkg.class.name+':'+d.pkg.id}">
        ${d.pkg.name}
      </g:link> via
    </g:if>
    <g:if test="${d.hostPlatform != null}">
      <g:link controller="resource" action="show"
        id="${d.hostPlatform.class.name+':'+d.hostPlatform.id}">
        ${d.hostPlatform.name}
      </g:link>
    </g:if>
  </g:if>
  <g:else>
  Create New ${d.getNiceName()}
  </g:else>
</h3>

<div id="content">

  <dl class="dl-horizontal">
    <g:if test="${d.title != null}">
      <dt><g:annotatedLabel owner="${d}" property="title">Title</g:annotatedLabel></dt>
      <dd>
        <g:link controller="resource" action="show"
          id="${d.title.class.name+':'+d.title?.id}">
          ${d.title.name}
        </g:link>
      </dd>
    </g:if>

    <g:if test="${d.pkg != null}">
      <dt><g:annotatedLabel owner="${d}" property="package">Package</g:annotatedLabel></dt>
      <dd>
        <g:link controller="resource" action="show"
          id="${d.pkg.class.name+':'+d.pkg?.id}">
          ${d.pkg.name}
        </g:link>
      </dd>
    </g:if>

    <g:if test="${d.hostPlatform != null}">
      <dt><g:annotatedLabel owner="${d}" property="platform">Platform</g:annotatedLabel></dt>
      <dd>
        <g:link controller="resource" action="show"
          id="${d.hostPlatform.class.name+':'+d.hostPlatform.id}">
          ${d.hostPlatform.name}
        </g:link>
      </dd>
    </g:if>

    <dt><g:annotatedLabel owner="${d}" property="status">Status</g:annotatedLabel></dt>
    <dd>
      <g:xEditableRefData owner="${d}" field="status"
        config="KBComponent.Status" />
    </dd>
    
    <dt><g:annotatedLabel owner="${d}" property="reference">Reference</g:annotatedLabel></dt>
    <dd>
      <g:xEditable class="ipe" owner="${d}" field="reference" />
    </dd>

    <dt><g:annotatedLabel owner="${d}" property="shortCode">Short Code</g:annotatedLabel></dt>
    <dd>
      <g:xEditable class="ipe" owner="${d}" field="shortcode" />
    </dd>
  <dt><g:annotatedLabel owner="${d}" property="editStatus">Edit Status</g:annotatedLabel></dt>
  <dd><g:xEditableRefData owner="${d}" field="editStatus" config='KBComponent.EditStatus' /></dd>

  </dl>

  <ul id="tabs" class="nav nav-tabs">
    <li class="active"><a href="#tippdetails" data-toggle="tab">TIPP
        Details</a></li>
    <li><a href="#tippcoverage" data-toggle="tab">Coverage</a></li>
    <li><a href="#tippopenaccess" data-toggle="tab">Open Access</a></li>
    <li><a href="#tipplists" data-toggle="tab">Lists</a></li>
    <li><a href="#addprops" data-toggle="tab">Additional
        Properties <span class="badge badge-warning">
          ${d.additionalProperties?.size()}
      </span>
    </a></li>
    <li><a href="#review" data-toggle="tab">Review Tasks <span
        class="badge badge-warning">
          ${d.reviewRequests?.size()}
      </span></a></li>
  </ul>


  <div id="my-tab-content" class="tab-content">

    <div class="tab-pane" id="tippcoverage">
      <dl class="dl-horizontal">
        <dt><g:annotatedLabel owner="${d}" property="coverage">Coverage</g:annotatedLabel></dt>
        <dd>
          <table class="table table-striped">
            <thead>
              <tr>
                <th>Start Date</th>
                <th>Start Volume</th>
                <th>Start Issue</th>
                <th>End Date</th>
                <th>End Volume</th>
                <th>End Issue</th>
                <th>Embargo</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td><g:xEditable class="ipe" owner="${d}" type="date"
                    field="startDate" /></td>
                <td><g:xEditable class="ipe" owner="${d}"
                    field="startVolume" /></td>
                <td><g:xEditable class="ipe" owner="${d}"
                    field="startIssue" /></td>
                <td><g:xEditable class="ipe" owner="${d}" type="date"
                    field="endDate" /></td>
                <td><g:xEditable class="ipe" owner="${d}"
                    field="endVolume" /></td>
                <td><g:xEditable class="ipe" owner="${d}" field="endIssue" /></td>
                <td><g:xEditable class="ipe" owner="${d}" field="embargo" /></td>
              </tr>
            </tbody>
          </table>
        </dd>
        <dt><g:annotatedLabel owner="${d}" property="covergaeNote">Coverage Note</g:annotatedLabel></dt>
        <dd>
          <g:xEditable class="ipe" owner="${d}" field="coverageNote" />
        </dd>
      </dl>
    </div>

    <div class="tab-pane" id="tippopenaccess">
      <dl class="dl-horizontal">
        <dt><g:annotatedLabel owner="${d}" property="delayedOA">Delayed OA ?</g:annotatedLabel></dt>
        <dd>
          <g:xEditableRefData owner="${d}" field="delayedOA"
            config="TitleInstancePackagePlatform.DelayedOA" />
        </dd>
        
        <dt><g:annotatedLabel owner="${d}" property="delayedOAEmbargo">Delayed OA Embargo</g:annotatedLabel></dt>
        <dd>
          <g:xEditable class="ipe" owner="${d}" field="delayedOAEmbargo" />
        </dd>
  
        <dt><g:annotatedLabel owner="${d}" property="hybridOA">Hybrid OA ?</g:annotatedLabel></dt>
        <dd>
          <g:xEditableRefData owner="${d}" field="hybridOA"
            config="TitleInstancePackagePlatform.HybridOA" />
        </dd>
  
        <dt><g:annotatedLabel owner="${d}" property="hybridOAUrl">Hybrid OA URL</g:annotatedLabel></dt>
        <dd>
          <g:xEditable class="ipe" owner="${d}" field="hybridOAUrl" />
        </dd>
      </dl>
    </div>

    <div class="tab-pane" id="tipplists"></div>

    <div class="tab-pane" id="addprops">
      <g:render template="addprops" contextPath="../apptemplates"
        model="${[d:d]}" />
    </div>


    <div class="tab-pane active" id="tippdetails">

      <g:if test="${d.id != null}">

        <dl class="dl-horizontal">
          <dt><g:annotatedLabel owner="${d}" property="url">Host Platform URL</g:annotatedLabel></dt>
          <dd>
            <g:xEditable class="ipe" owner="${d}" field="url" />
          </dd>
          <dt><g:annotatedLabel owner="${d}" property="format">Format</g:annotatedLabel></dt>
          <dd>
            <g:xEditableRefData owner="${d}" field="format"
              config="TitleInstancePackagePlatform.Format" />
          </dd>
          <dt><g:annotatedLabel owner="${d}" property="paymentType">Payment Type</g:annotatedLabel></dt>
          <dd>
            <g:xEditableRefData owner="${d}" field="paymentType"
              config="TitleInstancePackagePlatform.PaymentType" />
          </dd>
        </dl>
      </g:if>
    </div>

    <div class="tab-pane" id="review">
      <g:render template="revreqtab" contextPath="../apptemplates"
        model="${[d:d]}" />
    </div>
  </div>
  <g:render template="componentStatus" contextPath="../apptemplates" model="${[d:displayobj, rd:refdata_properties, dtype:'KBComponent']}" />
</div>


<script type="text/javascript">
  $(document).ready(function() {

    $.fn.editable.defaults.mode = 'inline';
    $('.ipe').editable();
  });
</script>
