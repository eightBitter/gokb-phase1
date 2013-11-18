package org.gokb

import grails.plugins.springsecurity.Secured
import grails.util.GrailsNameUtils;

import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.gokb.cred.*
import org.springframework.security.acls.model.NotFoundException

class ResourceController {

  def genericOIDService
  def classExaminationService
  def springSecurityService
  def gokbAclService

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def index() {
  }

  @Secured(['ROLE_USER', 'IS_AUTHENTICATED_FULLY'])
  def show() {
    User user = springSecurityService.currentUser

    def result = [:]

    if ( params.id ) {
      log.debug("Attempt to retrieve ${params.id} and find a global template for it");
      result.displayobj = genericOIDService.resolveOID(params.id)

      if ( result.displayobj ) {

        def new_history_entry = new History(controller:params.controller,
        action:params.action,
        actionid:params.id,
        owner:user,
        title:"View ${result.displayobj.toString()}").save()

        result.displayobjclassname = result.displayobj.class.name
        result.__oid = "${result.displayobjclassname}:${result.displayobj.id}"
        result.displaytemplate = grailsApplication.config.globalDisplayTemplates[result.displayobjclassname]

        // Add any refdata property names for this class to the result.
        result.refdata_properties = classExaminationService.getRefdataPropertyNames(result.displayobjclassname)
        result.displayobjclassname_short = result.displayobj.class.simpleName
        result.isComponent = (result.displayobj instanceof KBComponent)
        
        result.acl = gokbAclService.readAclSilently(result.displayobj)

      }
      else {
        log.debug("unable to resolve object");
      }
    }

    result
  }
}
