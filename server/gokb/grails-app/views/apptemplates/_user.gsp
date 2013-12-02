<r:require modules="gokbstyle"/>
<r:require modules="editable"/>

<g:if test="${d.id != null}">
  <dl class="dl-horizontal">
	  <dt><g:annotatedLabel owner="${d}" property="username">User Name</g:annotatedLabel></dt>
	  <dd>${d.username}</dd>
	
	  <dt><g:annotatedLabel owner="${d}" property="displayName">Display Name</g:annotatedLabel></dt>
	  <dd><g:xEditable class="ipe" owner="${d}" field="displayName"/></dd>
	
	  <dt><g:annotatedLabel owner="${d}" property="email">Email</g:annotatedLabel></dt>
	  <dd><g:xEditable class="ipe" owner="${d}" field="email"/></dd>
  </dl>
  <div id="content">
	  <ul id="tabs" class="nav nav-tabs">
	    <li class="active"><a href="#roles" data-toggle="tab">Roles</a></li>
	  </ul>
	  <div id="my-tab-content" class="tab-content">
	    <div class="tab-pane active" id="roles">
	      ... Coming Soon!
	    </div>
	  </div>
	</div>
</g:if>

<script type="text/javascript">
  $(document).ready(function() {

    $.fn.editable.defaults.mode = 'inline';
    $('.ipe').editable();
  });
</script>
