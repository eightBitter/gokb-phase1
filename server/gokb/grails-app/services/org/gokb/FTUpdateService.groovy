package org.gokb


import grails.transaction.Transactional
import org.gokb.FTControl
import org.hibernate.ScrollMode
import java.nio.charset.Charset
import java.util.GregorianCalendar


@Transactional
class FTUpdateService {

  def executorService
  def ESWrapperService
  def sessionFactory

  public static boolean running = false;

  def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP

  def synchronized updateFTIndexes() {
    log.debug("updateFTIndexes");
    
    if ( running == false ) {
      running = true;
      def future = executorService.submit({
        doFTUpdate()
      } as java.util.concurrent.Callable)
      log.debug("updateFTIndexes returning");
    }
    else {
      log.debug("FTUpdate already running");
    }
  }

  def doFTUpdate() {
    log.debug("doFTUpdate");

    log.debug("Execute IndexUpdateJob starting at ${new Date()}");
    def start_time = System.currentTimeMillis();

    def esclient = ESWrapperService.getClient()

    updateES(esclient, org.gokb.cred.BookInstance.class) { kbc ->

      def result = null

      result = [:]
      result._id = "${kbc.class.name}:${kbc.id}"
      result.name = kbc.name
      result.publisher = kbc.currentPublisher?.name
      result.publisherId = kbc.currentPublisher?.id
      result.altname = []
      kbc.variantNames.each { vn ->
        result.altname.add(vn.variantName)
      }

      result.identifiers = []
      kbc.ids.each { identifier ->
        result.identifiers.add([namespace:identifier.namespace.value, value:identifier.value] );
      }
  
      result.componentType=kbc.class.simpleName

      // log.debug("process ${result}");

      return result
    }


    updateES(esclient, org.gokb.cred.JournalInstance.class) { kbc ->

      def result = null

      result = [:]
      result._id = "${kbc.class.name}:${kbc.id}"
      result.name = kbc.name
      // result.publisher = kbc.currentPublisher?.name
      result.publisherId = kbc.currentPublisher?.id
      result.altname = []
      kbc.variantNames.each { vn ->
        result.altname.add(vn.variantName)
      }

      result.identifiers = []
      kbc.ids.each { identifier ->
        result.identifiers.add([namespace:identifier.namespace.value, value:identifier.value] );
      }

      result.componentType=kbc.class.simpleName

      // log.debug("process ${result}");

      return result
    }

    updateES(esclient, org.gokb.cred.Package.class) { kbc ->
      def result = null
      result = [:]
      result._id = "${kbc.class.name}:${kbc.id}"
      result.name = kbc.name
      return result
    }

    running = false;
  }


  def updateES(esclient, domain, recgen_closure) {

    def count = 0;
    try {
      log.debug("updateES - ${domain.name}");

 
      def latest_ft_record = null;
      def highest_timestamp = 0;
      def highest_id = 0;
      FTControl.withNewTransaction {
        latest_ft_record = FTControl.findByDomainClassNameAndActivity(domain.name,'ESIndex')

        log.debug("result of findByDomain: ${domain} ${latest_ft_record}");
        if ( !latest_ft_record) {
          latest_ft_record=new FTControl(domainClassName:domain.name,activity:'ESIndex',lastTimestamp:0,lastId:0).save(flush:true, failOnError:true)
        }
        else {
          highest_timestamp = latest_ft_record.lastTimestamp
        }
      }

      log.debug("updateES ${domain.name} since ${latest_ft_record.lastTimestamp}");

      def total = 0;
      Date from = new Date(latest_ft_record.lastTimestamp);
      // def qry = domain.findAllByLastUpdatedGreaterThan(from,[sort:'lastUpdated']);

      // def c = domain.createCriteria()
      // c.setReadOnly(true)
      // c.setCacheable(false)
      // c.setFetchSize(Integer.MIN_VALUE);
      // c.setFetchSize(250)

      // c.buildCriteria{
      //     gt('lastUpdated', from)
      //     gt('id', (latest_ft_record.lastId)?:new Long(0))
      //     order("lastUpdated", "asc")
      //     order("id", "asc")
      // }

      // def results = c.scroll(ScrollMode.FORWARD_ONLY)
  
      def countq = domain.executeQuery('select count(o.id) from '+domain.name+' as o where o.lastUpdated > :ts order by o.lastUpdated, o.id',[ts: from], [readonly:true])[0];
      log.debug("Will process ${countq} records");

      def q = domain.executeQuery('select o.id from '+domain.name+' as o where o.lastUpdated > :ts order by o.lastUpdated, o.id',[ts: from], [readonly:true]);
    
      log.debug("Query completed.. processing rows...");

      // while (results.next()) {
      q.each { r_id ->
        Object r = domain.get(r_id)
        log.debug("${r.id} ${domain.name} -- (rects)${r.lastUpdated} > (from)${from}");
        def idx_record = recgen_closure(r)

        if ( idx_record != null ) {
          def recid = idx_record['_id'].toString()
          idx_record.remove('_id');

          def future = esclient.indexAsync {
            index 'gokb'
            type 'component'
            id recid
            source idx_record
          }

          // future.actionGet()
          // log.debug("Index completed -- ${recid}");
        }


        if ( r.lastUpdated?.getTime() > highest_timestamp ) {
          highest_timestamp = r.lastUpdated?.getTime();
        }
        highest_id=r.id

        count++
        total++
        if ( count > 250 ) {
          count = 0;
          log.debug("interim:: processed ${++total} out of ${countq} records (${domain.name}) - updating highest timestamp to ${highest_timestamp} interim flush");
          FTControl.withNewTransaction {
            latest_ft_record = FTControl.get(latest_ft_record.id);
            latest_ft_record.lastTimestamp = highest_timestamp
            latest_ft_record.lastId = highest_id
            latest_ft_record.save(flush:true, failOnError:true);
          }
          cleanUpGorm();
          synchronized(this) {
            Thread.yield()
            Thread.sleep(2000);
          }
        }
      }

      // update timestamp
      FTControl.withNewTransaction {
        latest_ft_record = FTControl.get(latest_ft_record.id);
        latest_ft_record.lastTimestamp = highest_timestamp
        latest_ft_record.lastId = 0
        latest_ft_record.save(flush:true, failOnError:true);
      }
      cleanUpGorm();

      println("final:: Processed ${total} out of ${countq} records for ${domain.name}. Max TS seen ${highest_timestamp} highest id with that TS: ${highest_id}");
    }
    catch ( Exception e ) {
      log.error("Problem with FT index",e);
    }
    finally {
      log.debug("Completed processing on ${domain.name} - saved ${count} records");
    }
  }

  def cleanUpGorm() {
    log.debug("Clean up GORM");
    def session = sessionFactory.currentSession
    session.flush()
    session.clear()
    propertyInstanceMap.get().clear()
  }

  def clearDownAndInitES() {
    FTControl.withTransaction {
      FTControl.executeUpdate("delete FTControl c");
    }

    updateFTIndexes();
  }
 
}
