<r:require modules="gokbstyle"/>
<r:require modules="editable"/>

<h3>${d.id ? d.getNiceName() + ': ' + (d.name ?: d.id) : 'Create New ' + d.getNiceName()}</h3>

<div id="content">
  <ul id="tabs" class="nav nav-tabs">
    <li class="active"><a href="#platformdetails" data-toggle="tab">Title Details</a></li>
    <li><a href="#header" data-toggle="tab">Header</a></li>
    <li><a href="#status" data-toggle="tab">Status</a></li>
  </ul>
  <div id="my-tab-content" class="tab-content">
    <div class="tab-pane active" id="platformdetails">

      <dl class="dl-horizontal">
        <div class="control-group">
          <dt>Primary URL</dt>
          <dd>
            <g:xEditable class="ipe" owner="${d}" field="primaryUrl">${d.primaryUrl}</g:xEditable>
          </dd>
        </div>

        <div class="control-group">
          <dt>Software</dt>
          <dd><g:xEditableRefData owner="${d}" field="software" config='Platform.Software' /></dd>
        </div>

        <div class="control-group">
          <dt>Service</dt>
          <dd><g:xEditableRefData owner="${d}" field="service" config='Platform.Service' /></dd>
        </div>

        <div class="control-group">
          <dt>Authentication</dt>
          <dd><g:xEditableRefData owner="${d}" field="authentication" config='Platform.AuthMethod' /></dd>
        </div>
      </dl>
    </div>

    <div class="tab-pane" id="header">
      <g:render template="kbcomponent" contextPath="../apptemplates" model="${[d:displayobj, rd:refdata_properties, dtype:'KBComponent']}" />
    </div>

    <div class="tab-pane" id="status">
      <g:render template="componentStatus" contextPath="../apptemplates" model="${[d:displayobj, rd:refdata_properties, dtype:'KBComponent']}" />
    </div>


  </div>
</div>

<script language="JavaScript">
  $(document).ready(function() {
    $.fn.editable.defaults.mode = 'inline';
    $('.ipe').editable();
  });
</script>
