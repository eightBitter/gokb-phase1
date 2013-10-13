package org.gokb

import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH

import org.gokb.cred.*

/**
 * Look at uploaded files to see if there is any post-processing we can do on them
 */

class UploadAnalysisService {

  // Consider org.apache.tika.Tika for detection
  def grailsApplication

  def analyse(uploaded_file, datafile) {
    log.debug('UploadAnalysisService::analyse');

    def t = new org.apache.tika.Tika()
    def content_type = t.detect(uploaded_file)

    if ( content_type.equalsIgnoreCase('application/xml') ||
         content_type.equalsIgnoreCase('text/xml') ) {
      analyseXML(uploaded_file, datafile)
    }
    else {
      log.debug("Unhandled file type in analysis: ${datafile.mimetype}");
    }
  }

  def analyseXML(uploaded_file, datafile) {
    log.debug("analyseXML");

    try {
      // Get schema

      // Open the new file so that we can parse the xml
      def xml = new XmlSlurper().parse(new FileInputStream(uploaded_file))

      def root_element_namespace = xml.namespaceURI();
      def root_element_name = xml.name();

      // Root node information....
      log.debug( "Root element namespace: ${root_element_namespace} root element: ${root_element_name}")

      if ( root_element_namespace != null && root_element_name != null ) {
        datafile.doctype="${root_element_namespace}:${root_element_name}"
        datafile.save(flush:true);
        // Select any local handlers based on the root namespace/element
        if ( root_element_namespace?.equalsIgnoreCase('http://www.editeur.org/onix-pl') && 
             root_element_name?.equalsIgnoreCase('PublicationsLicenseExpression') ) {
          processOnixLicense(xml, uploaded_file, datafile);
        }
      }
    }
    catch ( Exception e ) {
      log.error("Problem analysing XML document upload",e);
    }
  }

  def processOnixLicense(parsedXml, uploaded_file, datafile) {
    // Create a license relating to this file
    def license = new License()

    // Extract high level details
    license.name = parsedXml.ExpressionDetail.Description.text()
    // license.fileAttachments.add(datafile);

    def baos = new ByteArrayOutputStream()
    def xslt = grailsApplication.mainContext.getResource('/WEB-INF/resources/onixToSummary.xsl').inputStream

    if ( xslt != null ) {
      // Run transform against document and store output in license.summaryStatement
      def factory = TransformerFactory.newInstance()
      def transformer = factory.newTransformer(new StreamSource(xslt))
      transformer.transform(new StreamSource(new FileReader(uploaded_file)), new StreamResult(baos))

      license.summaryStatement = baos.toString()
    }
    else {
      log.error("Unable to get handle to /onixToSummary.xsl XSL");
    }

    license.save(flush:true)

    // Create the combo
    license.fileAttachments.add(datafile);
    license.save(flush:true);
  }
}
