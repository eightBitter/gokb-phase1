<pre>
Acl: ${acl?:'NULL'}
</pre>
<table>
  <g:each in="${acl?.entries}" var="ent">
    <tr>
      <td>${ent.sid}</td>
      <td>${ent.permission}</td>
    </tr>
  </g:each>
</table>


<dl class="dl-horizontal">
  Grant User Permission:
  <g:form controller="ajaxSupport" action="grant" class="form-inline">
    <input type="hidden" name="__context" value="${d.class.name}:${d.id}"/>
    <dt>Perm</dt>
    <dd>
      <select name="perm">
        <option value="VIEW">View</option>
      </select>
    </dd>
    <dt>To</dt><dd><g:simpleReferenceTypedown name="grantee" baseClass="org.gokb.cred.User" /></dd>
    <dt></dt><dd><button type="submit" class="btn btn-primary btn-small">Add</button></dd>
  </g:form>
  Grant Role:
  <g:form controller="ajaxSupport" action="grant" class="form-inline">
    <input type="hidden" name="__context" value="${d.class.name}:${d.id}"/>
    <dd>
      <select name="perm">
        <option value="VIEW">View</option>
      </select>
    </dd>
    <dt>To</dt><dd><g:simpleReferenceTypedown name="grantee" baseClass="org.gokb.cred.Role" /></dd>
    <dt></dt><dd><button type="submit" class="btn btn-primary btn-small">Add</button></dd>
  </g:form>
</dl>

