<r:require modules="gokbstyle"/>
<r:require modules="editable"/>

<h1>${d.id ? d.getNiceName() + ': ' + (d.name ?: d.id) : 'Create New ' + d.getNiceName()}</h1>

<g:render template="kbcomponent" contextPath="../apptemplates" model="${[d:displayobj, rd:refdata_properties, dtype:'KBComponent']}" />

<g:if test="${d.id != null}">
  <dl class="dl-horizontal">
	  <dt><g:annotatedLabel owner="${d}" property="url">URL</g:annotatedLabel></dt>
	  <dd><g:xEditable class="ipe" owner="${d}" field="url"/></dd>
  </dl>
</g:if>
<script type="text/javascript">
  $(document).ready(function() {

    $.fn.editable.defaults.mode = 'inline';
    $('.ipe').editable();
  });
</script>
