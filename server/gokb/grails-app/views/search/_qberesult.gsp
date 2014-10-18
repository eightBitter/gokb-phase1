<%@ page import="grails.converters.JSON"%>

<g:set var="counter" value="${offset}" />

<g:if test="${ request.isAjax() }">
	<g:render template="pagination" contextPath="." model="${params}" />
	<table class="table table-striped table-condensed table-bordered">
		<thead>
			<tr class="inline-nav">
				<g:each in="${qbeConfig.qbeResults}" var="c">
					<th>
						<g:if test="${c.sort}">
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
						</g:if>
						<g:else>
							${c.heading}
						</g:else>
					</th>
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
	<g:form controller="workflow" action="action" method="post" params="${params}"
		class='action-form' >
		<div class="batch-all-info" style="display:none;"></div>
		<g:render template="pagination" contextPath="." model="${params}" />
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
									<g:link class="btn btn-xs btn-default pull-right desktop-only" controller="search"
										action="index" params="${params+['det':counter]}"><i class="fa fa-eye" ></i></g:link>
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
		<g:render template="pagination" contextPath="." model="${params + [dropup : true]}" />

                <!-- see grails-app/assets/javascripts/gokb/action-forms.js for code relating to bulk actions -->
		<div class="pull-right well col-xs-12 col-sm-9 ${displayobj != null ? 'col-md-6' : 'col-md-3'}" id="bulkActionControls">
			<h4>Available actions for selected rows</h4>
			<div class="input-group" >
				<select id="selectedBulkAction" class="form-control" name="selectedBulkAction"></select>
				<span class="input-group-btn">
					<button type="submit" class="btn btn-default btn-sm">Submit</button>
				</span>
			</div>
		</div>
	</g:form>
</g:else>
