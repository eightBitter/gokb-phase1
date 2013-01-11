<h1>Title: ${d.name}</h1>

<dl>
  <dt>Internal ID</dt>
  <dd>${d.id}</dd>
  <dt>Title</dt>
  <dd>${d.name}</dd>
  <dt>Tags</dt>
  <dd>
    <ul>
      <g:each in="${d.tags}" var="t">
        <li>${t.value}</li>
      </g:each>
    </ul>
  </dd>
  <dt>Appears in packages</dt>
  <dd>
    <table class="table table-striped">
     <caption>Search results</caption>
      <thead>
        <tr>
          <th>Package</th>
          <th>Platform</th>
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
        <g:each in="${d.tipps}" var="tipp">
          <tr>
            <td><g:link controller="resource" action="show" id="${tipp.pkg.class.name+':'+tipp.pkg.id}">${tipp.pkg.name}</g:link></td>
            <td><g:link controller="resource" action="show" id="${tipp.platform.class.name+':'+tipp.platform.id}">${tipp.platform.name}</g:link></td>
            <td><g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.startDate}"/></td>
            <td>${tipp.startVolume}</td>
            <td>${tipp.startIssue}</td>
            <td><g:formatDate format="${session.sessionPreferences?.globalDateFormat}" date="${tipp.endDate}"/></td>
            <td>${tipp.endVolume}</td>
            <td>${tipp.endIssue}</td>
            <td>${tipp.embargo}</td>
          </tr>
        </g:each>
      </tbody>
    </table>
  </dd>
</dl>

