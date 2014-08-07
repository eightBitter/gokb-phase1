<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <r:require modules="gokbstyle"/>
    <title>GOKbo : Coreference Service</title>
  </head>
  <body>

   
     <div class="row">
       <form action="index">
         Identifier to lookup Namespace:<input type="text" name="nspart" value="${params.nspart}"/> 
                              Identifier:<input type="text" name="idpart" value="${params.idpart}"/><input type="Submit" value="Go"/><br/>
         <p>
           Enter an identifier above to find the information GOKb holds about that item.<br/>
           Identifiers can take the following forms:
       	 </p>
         <ul>
           <li>a context free identifier, eg "1600-0390" (Without quotes) - Will find any occurrence of this string used as an identifier, and
               could expect to return information about a title with name "ACTA ARCHAEOLOGICA". Other items which share this identifier might also
               be returned. In this case, this is unlikely since the eISSN structure is unique. However if we look at, for example, org identifiers
               from the NCSU orgs database, we see "American Chemical Society" has the ID "2". There will be many items in GOKb who's unqualified 
               identifier is "2" and this query will return many objects.
           </li>
           <li>an identifier with associated namespace, eg 
                     <g:link controller="coreference" action="index" params="${[nspart:'eissn',idpart:'1600-0390']}">Namespace "eissn", Identifier "1600-0390" (Without quotes)</g:link> - 
                     <g:link controller="coreference" action="index" params="${[nspart:'eissn',idpart:'1600-0390',format:'json']}">[json]</g:link> - 
                     <g:link controller="coreference" action="index" params="${[nspart:'eissn',idpart:'1600-0390',format:'xml']}">[xml]</g:link> - 
                     will lookup specific instances of "ACTA ARCHAEOLOGICA". Searching for 
                     <g:link controller="coreference" action="index" params="${[nspart:'ncsu-internal',idpart:'ncsu:2']}">Namespace "ncsu-internal", Identifier "ncsu:2" (Without quotes)</g:link> 
                     <g:link controller="coreference" action="index" params="${[nspart:'ncsu-internal',idpart:'ncsu:2',format:'json']}">[json]</g:link> 
                     <g:link controller="coreference" action="index" params="${[nspart:'ncsu-internal',idpart:'ncsu:2',format:'xml']}">[xml]</g:link> 
                     will lookup specific occurences within the ncsu-internal namespace.
           </li>
         </ul>
       </form>
     </div>
     <g:if test="${identifier}">
       <div class="row">
         Found identifier ${params.identifier} (${identifier.id})... Attached objects follow:
       </div>
       <div class="row">
         <h2>Located ${count} objects for identifier "${params.idpart}" (namespace:${params.nspart?:'None'})</h2>
         <hr/>
         <table class="table">
           <thead>
             <tr>
               <th>GOKb Canonical</th>
               <th>Name/Title</th>
               <th>External Identifiers</th>
             </tr>
           </thead>
           <tbody>
             <g:each in="${records}" var="i">
               <tr><td><g:link controller="resource" action="show" id="${i.getClassName()}:${i.id}">${i.getClassName()}:${i.id}</g:link></td>
                   <td><g:link controller="resource" action="show" id="${i.getClassName()}:${i.id}">${i.name}</g:link></td>
                   <td>
                     <g:each in="${i.ids}" var="sa">
                       <g:link controller="coreference" action="index" params="${[nspart:sa.namespace.value,idpart:sa.value]}">${sa.namespace.value}:${sa.value}</g:link>
                       <br/>
                     </g:each>
                   </td>
             </g:each>
           </tbody>
         </table>
       </div>
     </g:if>  
  </body>
</html>
