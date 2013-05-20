package org.gokb.cred

/**
 * @author sosguthorpe
 *
 */

import java.util.Date;

import grails.util.GrailsNameUtils

class Combo {
  
  static final String RD_STATUS = "Combo.Status"
  static final String RD_TYPE = "Combo.Type"
  static final String STATUS_ACTIVE = "Active"
  static final String STATUS_SUPERSEDED = "Superseded"

  public static final String MAPPED_BY = "mappedByCombo"
  public static final String HAS = "hasByCombo"
  public static final String MANY = "manyByCombo"

  RefdataValue status
  RefdataValue type
  
  // All Combos should have a start date.
  Date startDate
  
  // The Combos without an end date are the "current" values.
  Date endDate

  // Participant 1 - One of these
  KBComponent fromComponent

  // Participant 2 - One of these
  KBComponent toComponent

  static mapping = {
                id column:'combo_id'            , index:'combo_id_idx'
           version column:'combo_version'
            status column:'combo_status_rv_fk'  , index:'combo_status_rv_idx'
              type column:'combo_type_rv_fk'    , index:'combo_type_rv_idx'
     fromComponent column:'combo_from_fk'       , index:'combo_from_idx'
       toComponent column:'combo_to_fk'         , index:'combo_to_idx'
	       endDate column:'combo_end_date'
	     startDate column:'combo_start_date'
  }

  static constraints = {
    status(nullable:true, blank:false)
    type(nullable:true, blank:false)
    fromComponent(nullable:true, blank:false)
    toComponent(nullable:true, blank:false)
	endDate(nullable:true, blank:false)
	startDate(nullable:false, blank:false)
  }
  
  public Date expire (Date endDate = null) {
	
	if (endDate == null) endDate = new Date ()
	
	// Expire this combo...
	setStatus (RefdataCategory.lookupOrCreate(Combo.RD_STATUS, Combo.STATUS_SUPERSEDED))
	setEndDate(endDate)
	endDate
  }
}
