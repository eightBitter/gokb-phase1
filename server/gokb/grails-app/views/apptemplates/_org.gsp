<r:require modules="gokbstyle"/>
<r:require modules="editable"/>

<g:if test="${d.id != null}">
	<dl class="dl-horizontal">
		<div class="control-group">
			<dt>Roles</dt>
			<dd>
				<ul>
					<g:each in="${d.roles?.sort({"${it.value}"})}" var="t">
						<li>
							${t.value}
						</li>
					</g:each>
				</ul>
				<br />
				Add role:
				<g:simpleReferenceTypedown name="roleRefdataValue"
					baseClass="org.gokb.cred.RefdataValue" filter1="Org.Role" />


				<%--      <g:select name="orgRoles" --%>
				<%--                from="${org.gokb.cred.RefdataValue.list()}"--%>
				<%--                value="${d.roles*.id}"--%>
				<%--                optionKey="id"--%>
				<%--                optionValue="value"--%>
				<%--                multiple="true"/>--%>
			</dd>
		</div>

		<g:if test="${d.parent != null}">
			<div class="control-group">
				<dt>Parent</dt>
				<dd>
					<g:link controller="resource" action="show"
						id="${d.parent.getClassName()+':'+d.parent.id}">
						${d.parent.name}
					</g:link>
				</dd>
			</div>
		</g:if>

		<g:if test="${d.children?.size() > 0}">
			<dt>Children</dt>
			<dd>
				<ul>
					<g:each in="${d.children}" var="c">
						<li><g:link controller="resource" action="show"
								id="${c.getClassName()+':'+c.id}">
								${c.name}
							</g:link></li>
					</g:each>
				</ul>
			</dd>
		</g:if>
	</dl>
</g:if>

<script language="JavaScript">
	$(document).ready(function() {

		$.fn.editable.defaults.mode = 'inline';
		$('.ipe').editable();
	});
</script>
