<%@ page import="grails.converters.JSON"%>

<g:set var="counter" value="${offset}" />

<g:if test="${ request.isAjax() }">
	<table class="table table-striped table-condensed table-bordered">
		<thead>
			<tr class="nav">
				<g:each in="${qbeConfig.qbeResults}" var="c">
					<th><g:if test="${c.sort}">
							<g:if test="${params.sort==c.sort && params.order=='asc'}">
								<g:link params="${params+['sort':c.sort,order:'desc']}">
									${c.heading}
									<i class="glyphicon glyphicon-sort-up"></i>
								</g:link>
							</g:if>
							<g:else>
								<g:if test="${params.sort==c.sort && params.order=='desc'}">
									<g:link params="${params+['sort':c.sort,order:'asc']}">
										${c.heading}
										<i class="glyphicon glyphicon-sort-down"></i>
									</g:link>
								</g:if>
								<g:else>
									<g:link params="${params+['sort':c.sort,order:'desc']}">
										${c.heading}
										<i class="glyphicon glyphicon-sort"></i>
									</g:link>
								</g:else>
							</g:else>
						</g:if> <g:else>
							${c.heading}
						</g:else></th>
				</g:each>
			</tr>
		</thead>
		<tbody>
			<g:each in="${rows}" var="r">
				<g:set var="r" value="${r}" />
				<tr class="${++counter==det ? 'success':''}">
					<!-- Row ${counter} -->
					<g:each in="${qbeConfig.qbeResults}" var="c">
						<td><g:if test="${c.link != null}">
								<g:link controller="${c.link.controller}"
									action="${c.link.action}"
									id="${c.link.id!=null?groovy.util.Eval.x(pageScope,c.link.id):''}"
									params="${c.link.params!=null?groovy.util.Eval.x(pageScope,c.link.params):[]}">
									${groovy.util.Eval.x(r, 'x.' + c.property) ?: 'Empty'}
								</g:link>
							</g:if> <g:else>
								${groovy.util.Eval.x(r, 'x.' + c.property)}
							</g:else></td>
					</g:each>
				</tr>
			</g:each>
		</tbody>
	</table>
</g:if>
<g:else>
	<g:form controller="workflow" action="action" method="post"
		class='action-form'>
		<table class="table table-striped table-condensed table-bordered">
			<thead>
				<tr>
					<th></th>
					<g:each in="${qbeConfig.qbeResults}" var="c">
						<th><g:if test="${c.sort}">
								<g:if test="${params.sort==c.sort && params.order=='asc'}">
									<g:link params="${params+['sort':c.sort,order:'desc']}">
										${c.heading}
										<i class="glyphicon glyphicon-sort-up"></i>
									</g:link>
								</g:if>
								<g:else>
									<g:if test="${params.sort==c.sort && params.order=='desc'}">
										<g:link params="${params+['sort':c.sort,order:'asc']}">
											${c.heading}
											<i class="glyphicon glyphicon-sort-down"></i>
										</g:link>
									</g:if>
									<g:else>
										<g:link params="${params+['sort':c.sort,order:'desc']}">
											${c.heading}
											<i class="glyphicon glyphicon-sort"></i>
										</g:link>
									</g:else>
								</g:else>
							</g:if> <g:else>
								${c.heading}
							</g:else></th>
					</g:each>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<g:each in="${rows}" var="r">
					<g:if test="${r != null }">
						<g:set var="r" value="${r}" />
						<tr class="${++counter==det ? 'success':''}">
							<!-- Row ${counter} -->
							<td><g:if
									test="${r.isEditable() && r.respondsTo('availableActions')}">
									<g:set var="al"
										value="${new JSON(r.availableActions()).toString().encodeAsHTML()}" />
									<input type="checkbox" name="bulk:${r.class.name}:${r.id}"
										data-actns="${al}" class="obj-action-ck-box" />
								</g:if> <g:else>
									<input type="checkbox"
										title="${ !r.isEditable() ? 'Component is read only' : 'No actions available' }"
										disabled="disabled" readonly="readonly" />
								</g:else></td>
							<g:each in="${qbeConfig.qbeResults}" var="c">
								<td><g:if test="${c.link != null}">
										<g:link controller="${c.link.controller}"
											action="${c.link.action}"
											id="${c.link.id!=null?groovy.util.Eval.x(pageScope,c.link.id):''}"
											params="${c.link.params!=null?groovy.util.Eval.x(pageScope,c.link.params):[]}">
											${groovy.util.Eval.x(r, 'x.' + c.property) ?: 'Empty'}
										</g:link>
									</g:if> <g:else>
										${groovy.util.Eval.x(r, 'x.' + c.property)}
									</g:else></td>
							</g:each>
							<td><g:if
									test="${request.user?.showQuickView?.value=='Yes'}">
									<g:link class="btn btn-default btn-primary pull-right btn-sm" controller="search"
										action="index" params="${params+['det':counter]}">view >></g:link>
								</g:if></td>
						</tr>
					</g:if>
					<g:else>
						<tr>
							<td>Error - Row not found</td>
						</tr>
					</g:else>
				</g:each>
			</tbody>
		</table>
		<div class="pull-right well" id="bulkActionControls">
			<h4>Available actions for selected rows</h4>
			<select id="selectedBulkAction" name="selectedBulkAction"></select>
			<button type="submit" class="btn btn-default btn-primary btn-sm">Submit</button>
		</div>
	</g:form>
</g:else>
