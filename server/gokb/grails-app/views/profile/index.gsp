<!DOCTYPE html>
<r:require modules="gokbstyle" />
<r:require modules="editable" />
<html>
  <head>
    <meta name="layout" content="main"/>
    <r:require modules="gokbstyle"/>
    <title>GOKbo : About</title>
  </head>
  <body>

   <div class="container-fluid">
     <div class="row-fluid">
       <h1>Application Info</h1>
       <dl class="dl-horizontal">
         <div class="control-group">
           <dt>Show Quick View : </dt>
           <dd><g:xEditableRefData owner="${user}" field="showQuickView" config="YN" /></dd>
         </div>
       </div>
     </div>
   </div>
  
  </body>
</html>
