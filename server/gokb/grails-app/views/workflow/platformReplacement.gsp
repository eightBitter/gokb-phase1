<!DOCTYPE html>
<html>
  <head>
    <meta name="layout" content="main"/>
    <r:require modules="gokbstyle"/>
    <r:require modules="editable"/>
    <title>GOKb: Platform Replacement</title>
  </head>
  <body>
  
  	<g:form controller="workflow" action="processPackageReplacement" method="get">
			<h1 class="page-header">Replace Platform</h1>
			<div id="mainarea" class="panel panel-default">
				<div class="panel-body">
					<h3>Update TIPP records and replace the platform on the following package(s)</h3>
					<table class="table table-striped table-bordered no-select-all">
	       		<thead>
	       			<tr>
	       				<th></th>
	       				<th>Package(s)</th>
	       			</tr>
	       		</thead>
	       		<tbody>
	       			<g:each in="${objects_to_action}" var="o">
	       				<tr>
	       					<td>
	              		<input type="checkbox" name="tt:${o.id}" checked="checked" />
	              	</td>
	              	<td>
	              		${o.name}
								</td>
	            </g:each>
	       		</tbody>
	       	</table>
					<dl class="dl-horizontal clearfix">
						<dt>With Platform:</dt>
						<dd>
							<div class="input-group">
								<g:simpleReferenceTypedown class="form-control" name="newplatform" baseClass="org.gokb.cred.Platform"/>
								<div class="input-group-btn" >
									<button type="submit" class="btn btn-default btn-sm">Update</button>
								</div>
							</div>
						</dd>
					</dl>
				</div>
			</div>
		</g:form>
  </body>
</html>

