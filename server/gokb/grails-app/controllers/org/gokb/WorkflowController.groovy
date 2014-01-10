package org.gokb

import org.gokb.cred.*
import grails.plugins.springsecurity.Secured
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

class WorkflowController {

  def genericOIDService
  def springSecurityService

  def actionConfig = [
    'method::deleteSoft':[actionType:'simple'],
    'title::transfer':      [actionType:'workflow', view:'titleTransfer'],
    'platform::replacewith':[actionType:'workflow', view:'platformReplacement'],
    'general::registerWebhook':[actionType:'workflow', view:'registerWebhook']
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
          
          def method_config = params.selectedBulkAction.split(/\:\:/) as List
          
          switch (method_config[0]) {
            
            case "method" : 
          
              // Everything after the first 2 "parts" are args for the method.
              def method_params = []
              if (method_config.size() > 2) {
                method_params.addAll(method_config.subList(2, method_config.size()))
              }
              
              // We should just call the method on the targets.
              result.objects_to_action.each {def target ->
                
                log.debug ("Attempting to fire method ${method_config[1]} (${method_params})")
                
                // Wrap in a transaction.
                KBComponent.withNewTransaction {def trans_status ->
                  try {
                    
                    // Just try and fire the method.
                    target.invokeMethod("${method_config[1]}", method_params ? method_params as Object[] : null)
                    
                    // Save the object.
                    target.save(failOnError:true)
                  } catch (Throwable t) {
                  
                    // Rollback and log error.
                    trans_status.setRollbackOnly()
                    t.printStackTrace()
                    log.error(t)
                  }
                }
              }
              break
          }
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

    def sw = new StringWriter()

    boolean first = true
    params.each { p ->
      if ( ( p.key.startsWith('tt:') ) && ( p.value ) && ( p.value instanceof String ) ) {
        def tt = p.key.substring(3);
        log.debug("Title to transfer: \"${tt}\"");
        def title_instance = TitleInstance.get(tt)
        // result.objects_to_action.add(genericOIDService.resolveOID2(oid_to_action))
        // Find all tipps for the title and add to tipps
        if ( title_instance ) {
          if ( first == true ) {
            first=false
          }
          else {
            sw.write(", ");
          }

          sw.write(title_instance.name);

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


    def new_activity = new Activity(
                                    activityName:"Title transfer ${sw.toString()} to ${result.newPublisher.name}",
                                    activityData:builder.toString(),
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
                                        start_date:old_tipp.start_date,
                                        start_volume:old_tipp.start_volume,
                                        start_issue:old_tipp.start_issue,
                                        end_date:old_tipp.end_date,
                                        end_volume:old_tipp.end_volume,
                                        end_issue:old_tipp.end_issue])
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
      redirect(controller:'home', action:'index');
    }
    else if ( params.abandon ) {
      log.debug("**ABANDON**...");
      activity_record.status = RefdataCategory.lookupOrCreate('Activity.Status', 'Abandoned')
      activity_record.save()
      redirect(controller:'home', action:'index');
    }

    def result = [:]
    result.titles = []
    result.tipps = []
    result.d = activity_record

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
    activity_data.tipps.each { tipp_map_entry ->
      def current_tipp = TitleInstancePackagePlatform.get(tipp_map_entry.key)
      log.debug("Processing current tipp : ${current_tipp.id}");

      tipp_map_entry.value.newtipps.each { newtipp ->
        log.debug("Process new tipp : ${newtipp}");

        def new_package = Package.get(newtipp.package_id)
        def new_platform = Platform.get(newtipp.platform_id)
 
        def new_tipp = new TitleInstancePackagePlatform(
                                   pkg:new_package,
                                   hostPlatform:new_platform,
                                   title:current_tipp.title,
                                   startDate:current_tipp.startDate,
                                   startVolume:current_tipp.startVolume,
                                   startIssue:current_tipp.startIssue,
                                   endDate:current_tipp.endDate,
                                   endVolume:current_tipp.endVolume,
                                   endIssue:current_tipp.endIssue).save()
      }

      current_tipp.status = RefdataCategory.lookupOrCreate(KBComponent.RD_STATUS, KBComponent.STATUS_RETIRED)
      current_tipp.save()
    }

    activity_record.status = RefdataCategory.lookupOrCreate('Activity.Status', 'Complete')
    activity_record.save()
  }

  def processPackageReplacement() {
    def deleted_status = RefdataCategory.lookupOrCreate('KBComponent.Status', 'Deleted')
    params.each { p ->
      log.debug("Testing ${p.key}");
      if ( ( p.key.startsWith('tt') ) && ( p.value ) && ( p.value instanceof String ) ) {
         def tt = p.key.substring(3);
         log.debug("Platform to replace: \"${tt}\"");
         def old_platform = Platform.get(tt)
         def new_platform = genericOIDService.resolveOID2(params.newplatform)

         log.debug("old: ${old_platform} new: ${new_platform}");
         try {
           Combo.executeUpdate("update Combo combo set combo.fromComponent = ? where combo.fromComponent = ?",[new_platform,old_platform]);

           old_platform.status = deleted_status
           old_platform.save(flush:true)
         }
         catch ( Exception e ) {
           log.debug("Problem executing update");
         }
      }
    }
    render view:'platformReplacementResult'
  }

  def download() {
    log.debug("Download ${params}");

    DataFile df = DataFile.findByGuid(params.id)
    if ( df != null ) {
      response.setContentType(df.uploadMimeType)
      response.addHeader("content-disposition", "attachment; filename=\"${df.uploadName}\"")
      def outs = response.outputStream

      def baseUploadDir = grailsApplication.config.baseUploadDir ?: '.'

      log.debug("copyUploadedFile...");
      def deposit_token = df.guid
      def sub1 = deposit_token.substring(0,2);
      def sub2 = deposit_token.substring(2,4);
      def temp_file_name = "${baseUploadDir}/${sub1}/${sub2}/${deposit_token}";
      def temp_file = new File(temp_file_name);

      org.apache.commons.io.IOUtils.copy(new FileReader(temp_file), outs);

      outs.flush()
      outs.close()
    }
  }

  def authorizeVariant() {
    log.debug(params);
    def result = [:]
    result.ref=request.getHeader('referer')
    def variant = KBComponentVariantName.get(params.id)

    if ( variant != null ) {
      // Does the current owner.name exist in a variant? If not, we should create one so we don't loose the info
      def current_name_as_variant = variant.owner.variantNames.find { it.variantName == variant.owner.name }

      if ( current_name_as_variant == null ) {
        def new_variant = new KBComponentVariantName(owner:variant.owner,variantName:variant.owner.name).save(flush:true);
      }

      variant.owner.name = variant.variantName
      variant.owner.save(flush:true);
    }

    redirect(url: result.ref)
  }

  def deleteVariant() {
    log.debug(params);
    def result = [:]
    result.ref=request.getHeader('referer')
    def variant = KBComponentVariantName.get(params.id)
    if (variant != null ) {
      variant.delete()
    }
    redirect(url: result.ref)
  }

  def processCreateWebHook() {

    log.debug("processCreateWebHook ${params}");

    def result = [:]

    result.ref=params.from

    try {

      def webook_endpoint = null
      if ( ( params.existingHook != null ) && ( params.existingHook.length() > 0 ) ) {
        log.debug("From existing hook");
      }
      else {
        webook_endpoint = new WebHookEndpoint(name:params.newHookName, 
                                              url:params.newHookUrl,
                                              authmethod:Long.parseLong(params.newHookAuth),
                                              principal:params.newHookPrin,
                                              credentials:params.newHookCred,
                                              owner:request.user)
        if ( webook_endpoint.save(flush:true) ) {
        }
        else {
          log.error("Problem saving new webhook endpoint : ${webook_endpoint.errors}");
        }
      }


      params.each { p ->
        if ( ( p.key.startsWith('tt:') ) && ( p.value ) && ( p.value instanceof String ) ) {
          def tt = p.key.substring(3);
          def wh = new WebHook( oid:tt, endpoint:webook_endpoint)
          if ( wh.save(flush:true) ) {
          }
          else {
            log.error(wh.errors);
          }
        }
      }
    }
    catch ( Exception e ) {
      log.error("Problem",e);
    }

    redirect(url: result.ref)
  }
}
