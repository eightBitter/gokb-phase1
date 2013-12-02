<!DOCTYPE html>
<html>
<head>
<meta name="layout" content="main" />
<r:require modules="gokbstyle" />
<title>GOKb</title>
</head>
<body class="">
	<div class="container-fluid well">
		<table class="table table-bordered">
			<tr>
        <th>Status</th>
				<th>Role</th>
			</tr>
			<g:each in="${ currentRoles }" var="roleName, status">
				<tr>
          <td class="group-status" >
            <g:if test="${ status }" >
              <a href="#" title="remove role"><i class="group-member icon-ok-sign" ></i></a>
            </g:if>
            <g:else>
              <a href="#" title="add role"><i class="group-none-member icon-remove-sign" ></i></a>
            </g:else>
          </td>
					<td>
						${ roleName }
					</td>
				</tr>
			</g:each>
		</table>
	</div>
</body>
</html>