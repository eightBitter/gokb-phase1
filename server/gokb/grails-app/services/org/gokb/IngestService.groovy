package org.gokb

import org.gokb.refine.*;
import org.gokb.cred.*;
import org.apache.commons.compress.compressors.gzip.*
import org.apache.commons.compress.archivers.tar.*
import org.apache.commons.compress.archivers.*
import grails.converters.JSON

class IngestService {

  // Automatically injected services from grails-app/services
  def grailsApplication
  def titleLookupService
  def sessionFactory
  def propertyInstanceMap = org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin.PROPERTY_INSTANCE_MAP

  /**
   *  Validate a parsed project. 
   *  @param project_data Parsed map of project data
   */
  def validate(project_data) {
    log.debug("Validate");
    // def project_data = extractRefineproject(p.file);

    def result = [:]
    result.status = true
    result.messages = []

    def print_identifier_col = null;
    def online_identifier_col = null;
    def publication_title_col = null;
    def platform_host_name_col = null;
    def platform_host_url_col = null;

    int i=0;
    def col_positions = [:]
    project_data.columnDefinitions.each { cd ->
      log.debug("Assinging col ${cd.name} to position ${i}");
      col_positions[cd.name] = i++;
    }

    if ( col_positions['print_identifier'] == null )
      result.messages.add([text:'Import does not specify a print_identifier column']);

    if ( col_positions['online_identifier'] == null )
      result.messages.add([text:'Import does not specify an online_identifier column']);

    if ( col_positions['publication_title'] == null )
      result.messages.add([text:'Import does not specify a publication_title column']);

    if ( col_positions['platform.host.name'] == null )
      result.messages.add([text:'Import does not specify a platform.host.name column']);

    if ( col_positions['platform.host.url'] == null )
      result.messages.add([text:'Import does not specify a platform.host.url column']);

    if ( result.messages.size() > 0 ) {
      log.error("validation has messages: a failure: ${result.messages}");
      result.status = false;
    }
    else {
      result.messages.add([text:'Checked in file passes GoKB validation step, proceed to ingest']);
    }

    result
  }

  /**
   *  ingest a parsed project. 
   *  @param project_data Parsed map of project data
   */
  def ingest(project_data, project) {
    log.debug("Ingest");
    // def project_data = extractRefineproject(p.file);

    def result = [:]
    result.status = project_data ? true : false


    int i=0;
    def col_positions = [:]
    project_data.columnDefinitions.each { cd ->
      col_positions[cd.name] = i++;
    }

    log.debug("Using col positions: ${col_positions}");

    // int title_index=0
    // int issn_index=1
    // int eissn_index=2
    int ctr = 0
    project_data.rowData.each { datarow ->
      log.debug("Row ${ctr}");
      if ( datarow.cells[col_positions['publication_title']] ) {

        // Title Instance
        log.debug("Looking up title...");
        def title_info = titleLookupService.find(jsonv(datarow.cells[col_positions['publication_title']]),   // jsonv(datarow.cells[title_index]),
                                                 jsonv(datarow.cells[col_positions['print_identifier']]),    // jsonv(datarow.cells[issn_index]) 
                                                 jsonv(datarow.cells[col_positions['online_identifier']]));  // jsonv(datarow.cells[eissn_index]));

        // Platform
        def host_platform_url = jsonv(datarow.cells[col_positions['platform.host.url']])
        def host_platform_name = jsonv(datarow.cells[col_positions['platform.host.name']])
        def host_norm_platform_name = host_platform_name.toLowerCase().trim();
        log.debug("Looking up platform...(${host_platform_url},${host_platform_name},${host_norm_platform_name})");
        def platform_info = Platform.findByPrimaryUrl(host_platform_url) 
        if ( !platform_info ) {
          platform_info = new Platform(primaryUrl:host_platform_url, name:host_platform_name, normname:host_norm_platform_name)
          if (! platform_info.save(flush:true) ) {
            platform_info.errors.each { e ->
              log.error(e);
            }
          }
        }

        // Package

        // TIPP

        // Every 100 records we clear up the gorm object cache - Pretty nasty performance hack, but it stops the VM from filling with
        // instances we've just looked up.
        if ( ctr % 250 == 0 )
          cleanUpGorm()
      }
      else {
        log.debug("Row ${ctr} seems to be a null row. Skipping");
      }
      ctr++
    }

    // finally, rules extraction
    project_data.pastEntryList.each { r ->
      log.debug("Consider rule: ${r}");
    }

    result
  }

  def jsonv(v) {
    def result = null
    if ( v ) {
      if ( !v.equals(null) ) {
        result = v.v
      }
    }
    result
  }


  /**
   *  Read an uploaded refine .tar.gz file, uncompress and create a map containing all the data. This is in memory,
   *  but our package files should never be large enough to cause a problem.
   *  @param zipFilename .tar.gz file to extract.
   *  @return Map containing parsed project data
   */
  def extractRefineproject(String zipFilename) {
    def result = null;


    try {
      def full_filename = grailsApplication.config.project_dir + zipFilename

      log.debug("Extract ${full_filename}");

      FileInputStream fin = new FileInputStream(full_filename);
      GzipCompressorInputStream gzIn = new GzipCompressorInputStream(fin);
      TarArchiveInputStream tin = new TarArchiveInputStream(gzIn)
      TarArchiveEntry ae = tin.getNextTarEntry()
      while ( ae ) {
        log.debug("Processing archive entry: ${ae} ${ae.name} isFile:${ae.isFile()}");
        switch ( ae.name ) {
          case 'metadata.json':
            log.debug("Handle metadata");
            break;
          case 'data.zip':
            def temp_data_zipfile
            try {
              log.debug("Handle Data.. create zipfile. need to copy ${ae.getSize()} bytes from tin to a buffer and re-read as a zip file");

              // Copy bytes from tar stream into temp zip file
              temp_data_zipfile = File.createTempFile('gokb_','_refinedata.zip',null)
              FileOutputStream fos = new FileOutputStream(temp_data_zipfile);
              int bytes_to_read = ae.getSize()
              byte[] buffer = new byte[4096]
              while (bytes_to_read) {
                int bytes_read = tin.read(buffer,0,4096)
                log.debug("Copying ${bytes_read} bytes to temp file");
                fos.write(buffer, 0, bytes_read)
                bytes_to_read -= bytes_read
              }
              fos.flush()
              fos.close();
  
              // Open temp zip file as a zip object
              if ( temp_data_zipfile ) {
                java.util.zip.ZipFile zf = new java.util.zip.ZipFile(temp_data_zipfile)
                log.debug("Getting data.txt");
                java.util.zip.ZipEntry ze = zf.getEntry('data.txt');
                if ( ze ) {
                    log.debug("Got data.txt");
                  result=[:]
                  processData(result, zf.getInputStream(ze));
                }
                else {
                  log.error("Problem getting data.txt");
                }
              }
              else {
                log.debug("zip file is null");
              }
            }
            finally {
              if ( temp_data_zipfile ) {
                try {
                  temp_data_zipfile.delete();
                }
                catch ( Throwable t ) {
                }
              }
            }
            break;
          default:
            break;
        }
        
        ae = tin.getNextTarEntry()
      }
      tin.close();
      gzIn.close();
      fin.close();
    }
    catch ( Exception e ) {
      log.error("Unexpected error trying to extrat refine data.",e);
      e.printStackTrace();
    }
    
    result
  }

  def processData(result, is) {
    log.debug("processing refine data.txt");
    def bis = new BufferedReader(new InputStreamReader(is));

    // First line is the refine version
    result.refineVersion = bis.readLine()

    // 2.5

    // Header info
    result.columnModel=valuePart(bis.readLine())
    result.maxCellIndex=valuePart(bis.readLine())
    result.keyColumnIndex=valuePart(bis.readLine())
    result.columnCount=Integer.decode(valuePart(bis.readLine()))

    result.columnDefinitions = []
    for ( int i=0; i<result.columnCount; i++ ) {
      log.debug("Reading column ${i}");
      result.columnDefinitions.add(JSON.parse(bis.readLine()));
    }

    result.columnGroupCount = Integer.decode(valuePart(bis.readLine()))
    for (int i=0; i<result.columnGroupCount; i++ ) {
      // Skipping column group info
      def row = bis.readLine()
    }

    if ( bis.readLine() != '/e/' ) {
      log.error("Unexpected row!");
    }
  
    result.history = valuePart(bis.readLine())

    result.pastEntryCount = Integer.decode(valuePart(bis.readLine()))
    result.pastEntryList = []
    for (int i=0; i<result.pastEntryCount; i++ ) {
      // Skipping past entry
      result.pastEntryList.add(JSON.parse(bis.readLine()));
    }

    result.futureEntryCount = Integer.decode(valuePart(bis.readLine()))
    for (int i=0; i<result.futureEntryCount; i++ ) {
      // Skipping future entry
      def row = bis.readLine()
    }

    if ( bis.readLine() != '/e/' ) {
      log.error("Unexpected row!");
    }

    result.overlayModel = valuePart(bis.readLine())
    result.rowCount = Integer.decode(valuePart(bis.readLine()))
    result.rowData = []

    for (int i=0; i<result.rowCount; i++ ) {
      def row = bis.readLine()
      // log.debug("Row ${i}");
      result.rowData.add(JSON.parse(row))
      // Skipping row
    }

    bis.close();
  }

  def valuePart(s) {
    int equalsPos = s.indexOf('=');
    def result = s.substring(equalsPos+1, s.length());
    log.debug("valuePart(${s})=${result}");
    result;
  }


  def cleanUpGorm() {
    log.debug("Clean up GORM");
    def session = sessionFactory.currentSession
    session.flush()
    session.clear()
    propertyInstanceMap.get().clear()
  }

}
