package org.gokb

import org.gokb.cred.*
import grails.plugins.springsecurity.Secured
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class WorkflowController {

  def genericOIDService
  def springSecurityService

  def actionConfig = [
    'object::statusDeleted':[actionType:'simple'],
    'title::transfer':      [actionType:'workflow', view:'titleTransfer']
  ];

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def action() { 
    log.debug("WorkflowController::action(${params})");
    def result = [:]
    result.ref=request.getHeader('referer')
    def action_config = actionConfig[params.selectedBulkAction];
    if ( action_config ) {

      result.objects_to_action = []

      params.each { p ->
        if ( ( p.key.startsWith('bulk:') ) && ( p.value ) && ( p.value instanceof String ) ) {
          def oid_to_action = p.key.substring(5);
          log.debug("Action oid: ${oid_to_action}");
          result.objects_to_action.add(genericOIDService.resolveOID2(oid_to_action))
        }
      }

      switch ( action_config.actionType ) {
        case 'simple': 
          // Do stuff
          redirect(url: result.ref)
          break;
        case 'workflow':
          render view:action_config.view, model:result
          break;
        default:
          flash.message="Invalid action type information: ${action_config.actionType}";
          break;
      }
    }
    else {
      flash.message="Unable to locate action config for ${params.selectedBulkAction}";
      log.warn("Unable to locate action config for ${params.selectedBulkAction}");
      redirect(url: result.ref)
    }
  }

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def processTitleChange() {
    log.debug("processTitleChange");
    def user = springSecurityService.currentUser
    def result = [:]
    result.titles = []
    result.tipps = []
    result.newtipps = [:]

    def titleTransferData = [:]
    titleTransferData.title_ids = []
    titleTransferData.tipps = [:]

    params.each { p ->
      if ( ( p.key.startsWith('tt:') ) && ( p.value ) && ( p.value instanceof String ) ) {
        def tt = p.key.substring(3);
        log.debug("Title to transfer: \"${tt}\"");
        def title_instance = TitleInstance.get(tt)
        // result.objects_to_action.add(genericOIDService.resolveOID2(oid_to_action))
        // Find all tipps for the title and add to tipps
        if ( title_instance ) {
          result.titles.add(title_instance) 
          titleTransferData.title_ids.add(title_instance.id)
          title_instance.tipps.each { tipp ->
            result.tipps.add(tipp)
            titleTransferData.tipps[tipp.id] = [newtipps:[]]
          }
        }
        else {
          log.error("Unable to locate title with that ID");
        }
      }
    }

    result.newPublisher = genericOIDService.resolveOID2(params.title)
    titleTransferData.newPublisherId = result.newPublisher.id

    def builder = new JsonBuilder()
    builder(titleTransferData)

    def active_status = RefdataCategory.lookupOrCreate('Activity.Status', 'Active').save()
    def transfer_type = RefdataCategory.lookupOrCreate('Activity.Type', 'TitleTransfer').save()


    def new_activity = new Activity(activityData:builder.toString(),
                                    owner:user,
                                    status:active_status, 
                                    type:transfer_type).save()
    
    redirect(action:'editTitleTransfer',id:new_activity.id)
  }

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def editTitleTransfer() {
    log.debug("editTitleTransfer() - ${params}");

    def activity_record = Activity.get(params.id)
    def activity_data = new JsonSlurper().parseText(activity_record.activityData)

    log.debug("Activity record: ${activity_data}");

    if ( params.addTransferTipps ) {
      // Add Transfer tipps
      log.debug("Add transfer tipps");
      if ( ( params.Package != null ) && ( params.Platform != null ) ) {
        def new_tipp_package = genericOIDService.resolveOID2(params.Package);
        def new_tipp_platform = genericOIDService.resolveOID2(params.Platform);
        if ( ( new_tipp_package != null ) && ( new_tipp_platform != null ) ) {
          params.each { p ->
            if ( p.key.startsWith('addto-') ) {
              def tipp_id = p.key.substring(6)
              log.debug("Add new tipp for ${new_tipp_package}, ${new_tipp_platform} to replace ${tipp_id}");
              def old_tipp = KBComponent.get(tipp_id);
              def tipp_info = activity_data.tipps[tipp_id]

              if ( tipp_info != null ) {

                if ( tipp_info.newtipps == null )
                  tipp_info.newtipps = [:]

                tipp_info.newtipps.add([
                                        title_id:old_tipp.title.id, 
                                        package_id:new_tipp_package.id, 
                                        platform_id:new_tipp_platform.id,
                                        start_date:'',
                                        start_volume:'',
                                        start_issue:'',
                                        end_date:'',
                                        end_volume:'',
                                        end_issue:''])
              }
              else {
                log.error("Unable to find key (${tipp_id}) In map: ${activity_data.tipps}");
              }
            }
          }

          // Update the activity data in the database
          def builder = new JsonBuilder()
          builder(activity_data)
          activity_record.activityData = builder.toString();
          activity_record.save()
        }
        else {
          log.error("Add transfer tipps but failed to resolve package(${params.Package}) or platform(${params.Platform})");
        }
      }
      else {
          log.error("Add transfer tipps but package or platform not set");
      }
    }
    else if ( params.process ) {
      log.debug("Process...");
      processTitleTransfer(activity_record, activity_data);
    }

    def result = [:]
    result.titles = []
    result.tipps = []

    activity_data.title_ids.each { tid ->
      result.titles.add(TitleInstance.get(tid))
    }

    activity_data.tipps.each { tipp_info ->
      def tipp_object = TitleInstancePackagePlatform.get(tipp_info.key)
      result.tipps.add([
                        id:tipp_object.id,
                        type:'CURRENT',
                        title:tipp_object.title, 
                        pkg:tipp_object.pkg, 
                        hostPlatform:tipp_object.hostPlatform,
                        startDate:tipp_object.startDate,
                        startVolume:tipp_object.startVolume,
                        startIssue:tipp_object.startIssue,
                        endDate:tipp_object.endDate,
                        endVolume:tipp_object.endVolume,
                        endIssue:tipp_object.endIssue
                        ])
      tipp_info.value.newtipps.each { newtipp_info ->
        result.tipps.add([
                          type:'NEW',
                          title:KBComponent.get(newtipp_info.title_id),
                          pkg:KBComponent.get(newtipp_info.package_id),
                          hostPlatform:KBComponent.get(newtipp_info.platform_id),
                          startDate:newtipp_info.startDate,
                          startVolume:newtipp_info.startVolume,
                          startIssue:newtipp_info.startIssue,
                          endDate:newtipp_info.endDate,
                          endVolume:newtipp_info.endVolume,
                          endIssue:newtipp_info.endIssue
                          ])
      }
    }

    result.newPublisher = Org.get(activity_data.newPublisherId)
    result.id = params.id

    result
  }

  def processTitleTransfer(activity_record, activity_data) {
    log.debug("processTitleTransfer ${params}\n\n ${activity_data}");

    def publisher = Org.get(activity_data.newPublisherId);

    // Step one : Close off existing title publisher links and create new publisher links
    activity_data.title_ids.each { title_id ->
      log.debug("Process title_id ${title_id} and change publisher to ${publisher}");
      def title = TitleInstance.get(title_id);
      title.changePublisher(publisher)
      title.save()
    }

    // Step two : Process TIPP adjustments
  }
}
