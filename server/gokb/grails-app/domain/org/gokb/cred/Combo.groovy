package org.gokb.cred

/**
 * @author sosguthorpe
 *
 */

import grails.util.GrailsNameUtils

class Combo {
  
  public static final String MAPPED_BY = "mappedByCombo"
  public static final String HAS = "hasByCombo"
  public static final String MANY = "manyByCombo"

  RefdataValue status
  RefdataValue type

  // Participant 1 - One of these
  KBComponent fromComponent

  // Participant 2 - One of these
  KBComponent toComponent

  static mapping = {
                id column:'combo_id'
           version column:'combo_version'
            status column:'combo_status_rv_fk'
              type column:'combo_type_rv_fk'
     fromComponent column:'combo_from_fk'
       toComponent column:'combo_to_fk'
  }

  static constraints = {
    status(nullable:true, blank:false)
    type(nullable:true, blank:false)
    fromComponent(nullable:true, blank:false)
    toComponent(nullable:true, blank:false)
  }
}
