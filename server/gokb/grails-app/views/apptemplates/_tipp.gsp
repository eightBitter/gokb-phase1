<h1>TIPP ${d.id}</h1>
<p>occurrence of ${d.title.name} in Package ${d.pkg.name} under platform ${d.hostPlatform.name}<p>

<dl>
  <dt>Internal ID</dt>
  <dd>${d.id}</dd>

  <dt>Title</dt>
  <dd><g:link controller="resource" action="show" id="${d.title?.class.name+':'+d.title?.id}">${d.title?.name}</g:link></dd>

  <dt>Package</dt>
  <dd><g:link controller="resource" action="show" id="${d.pkg?.class.name+':'+d.pkg?.id}">${d.pkg?.name}</g:link></dd>

  <dt>Platform</dt>
  <dd><g:link controller="resource" action="show" id="${d.hostPlatform?.class.name+':'+d.hostPlatform?.id}">${d.hostPlatform?.name}</g:link></dd>


  <dt>Incoming Combos</dt>
  <dd>
    <ul>
      <g:each in="${d.getOtherIncomingCombos()}" var="c">
      	
        <li><g:link controller="resource" action="show" id="${c.fromComponent.class.name+':'+c.fromComponent.id}">${c.fromComponent.name}</g:link> has ${c.type?.value} This Org</li>
      </g:each>
    </ul>
  </dd>
  <dt>Outgoing Combos</dt>
  <dd>
    <ul>
      <g:each in="${d.getOtherOutgoingCombos()}" var="c">
        <li>This Org has ${c.type?.value} <g:link controller="resource" action="show" id="${c.toComponent.class.name+':'+c.toComponent.id}">${c.toComponent.name}</g:link></li>
      </g:each>
    </ul>
  </dd>
</dl>
