package org.gokb

import org.gokb.cred.*;
import grails.gorm.*
import grails.converters.*

class CoreferenceController {

  def index() { 
    def result = [:]
    result.count = -1
    log.debug("coreference::index")
    if ( params.idpart ) {

      log.debug("Lookup ${params.nspart}:${params.idpart}.");

      def q = new DetachedCriteria(Identifier).build {
        if ( params.nspart ) {
          ns {
            eq('ns',params.nspart)
          }
        }
        eq('value',params.idpart)
      }

      def int_id = q.get()

      if ( int_id ) {
        log.debug("Recognised identifier.. find all occurrences");
        def q2 = new DetachedCriteria(KBComponent).build {
          ids {
            eq('identifier',int_id)
          }
        }
        result.identifier = int_id
        result.count = q2.count()
        result.records = q2.list()
        log.debug("result: ${result.identifier} ${result.count} ${result.records}");
      }
    }

    def json_response;
    if ( response.format == 'json' ) {
      json_response = ['requestedNS':params.nspart, 
                       'requestedID':params.idpart, 
                       'gokbIdentifier': result.identifier ? "${result.identifier.class.name}:${result.identifier.id}" : "UNKNOWN",
                       'count':result.count ?: 0,
                       'records':[]]
      result.records?.each { r ->
        json_response.records.add(['type':r.class.name,
                                   'id':r.id,
                                   'name':r.name,
                                   'gokbIdentifier':"${r.class.name}:${r.id}"])
      }
    }

    
    withFormat {
      html result
      json { render json_response as JSON }
    }
  }
}
