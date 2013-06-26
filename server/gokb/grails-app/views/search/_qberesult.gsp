<%@ page import="grails.converters.JSON" %>

<g:set var="counter" value="${offset}" />

<g:form controller="workflow" action="action" method="get">
  <table class="table table-striped">
   <caption>Search results</caption>
    <thead>
      <tr>
        <th></th>
        <g:each in="${qbeConfig.qbeResults}" var="c">
          <th>${c.heading}</th>
        </g:each>
        <th></th>
      </tr>
    </thead>
    <tbody>
      <g:each in="${rows}" var="r">
        <tr class="${++counter==det ? 'success':''}">
          <!-- Row ${counter} -->
          <td>
            <g:if test="${r.respondsTo('availableActions')}">
              <g:set var="al" value="${new JSON(r.availableActions()).toString().encodeAsHTML()}"/> 
              <input type="checkbox" name="bulk:${r.class.name}:${r.id}" data-actns="${al}" class="obj-action-ck-box" onChange="javascript:updateAvailableActions();"/>
            </g:if>
          </td>
          <g:each in="${qbeConfig.qbeResults}" var="c">
            <td>${groovy.util.Eval.x(r, 'x.' + c.property)}</td>
          </g:each>
          <td>
            <g:link class="btn" controller="resource" action="show" id="${r.class.name+':'+r.id}">Show</g:link>
            <g:link class="btn" controller="search" action="index" params="${params+['det':counter]}">Preview -></g:link>
          </td>
        </tr>
      </g:each>
    </tbody>
  </table>
  <div class="pull-right well">
    <h4>Available actions for selected rows</h4>
    <select id="selectedBulkAction" name="selectedBulkAction">
    </select>
    <button type="submit" class="btn">Action</button>
  </div>
</g:form>

<script language="javascript">
  function updateAvailableActions() {
    // alert("update actions");

    var allActionsAvailable = []; 
			
		// Step through each checked box.
		$('input.obj-action-ck-box:checked').each(function(i) {
			var elem = $(this);
			if (i == 0) {
				var json = elem.attr('data-actns');
				
				// Set all actions available to this objects actions.
				allActionsAvailable = $.parseJSON(json);
			} else {
				var json = elem.attr('data-actns');
				
				var elementActions = $.parseJSON(json);

				// Filter the array using a callback that checks that this element actions contains
				// the object.
				allActionsAvailable = $.grep(allActionsAvailable, function(action, index) {
					var match = $.grep(elementActions, function (el, i) {
						return el.code == action.code && el.label == action.label;
					});
					return match.length > 0
				});
			}
		});

		var opts = $('select#selectedBulkAction').prop("options");
		opts.length = 0;
		
		// Add the options to the dropdown.
		$.each(allActionsAvailable, function (index, action) {
			opts[index] = new Option(action.label, action.code);
		});
    
  }
</script>
