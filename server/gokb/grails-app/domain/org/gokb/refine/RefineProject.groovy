package org.gokb.refine

import org.gokb.cred.KBComponent
import org.gokb.cred.Org
import javax.persistence.Transient

class RefineProject extends KBComponent {

   String name
   String description
     Date modified
   String file
  Boolean checkedIn
   String checkedOutBy
     Long localProjectID
   String hash
      Org provider
   String lastValidationResult
     Long progress
   String possibleRulesString
   String notes

  @Transient
  def lastValidationResultAsMap = null
  @Transient
  def possibleRulesResultAsList = null

  static mapping = {
             description column: 'rp_desc'
                modified column: 'rp_modified'
                    file column: 'rp_file'
               checkedIn column: 'rp_checked_in'
            checkedOutBy column: 'rp_checked_out_by'
          localProjectID column: 'rp_local_project_id'
                    hash column: 'rp_hash'
                provider column: 'rp_prov_fk'
    lastValidationResult column: 'rp_last_validation_result', type: 'text'
                progress column: 'rp_progress'
     possibleRulesString column: 'rp_matching_rules', type: 'text'
     			   notes column: 'rp_notes'
  }

  
  static constraints = {
                    hash nullable: true
            checkedOutBy nullable: true
             description nullable: true
          localProjectID nullable: true
    lastValidationResult nullable: true
                provider nullable: true
                progress nullable: true
     possibleRulesString nullable: true
     			   notes nullable: true
  }

  @Transient
  lastValidationAsMap() {
    if ( lastValidationResult && ( lastValidationResultAsMap == null ) ) {
      lastValidationResultAsMap = grails.converters.JSON.parse(lastValidationResult)
    }
    lastValidationResultAsMap
  }

  @Transient
  possibleRulesAsList() {
    if ( possibleRulesString && ( possibleRulesResultAsList == null ) ) {
      possibleRulesResultAsList = grails.converters.JSON.parse(possibleRulesString)
    }
    possibleRulesResultAsList
  }

}
