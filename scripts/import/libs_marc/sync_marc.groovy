#!groovy

@Grapes([
  @GrabResolver(name='mvnRepository', root='http://central.maven.org/maven2/'),
  @Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.14'),
  @Grab(group='javax.mail', module='mail', version='1.4.7'),
  @Grab(group='net.sourceforge.htmlunit', module='htmlunit', version='2.21'),
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2'),
  @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.5.2'),
  @Grab(group='org.apache.httpcomponents', module='httpmime', version='4.5.2'),
  @Grab(group='commons-net', module='commons-net', version='3.5'),
  @Grab(group='marc4j', module='marc4j', version='2.7.0'),
  @GrabExclude('org.codehaus.groovy:groovy-all')
])


import javax.mail.*
import javax.mail.search.*
import java.util.Properties
import static groovy.json.JsonOutput.*
import groovy.json.JsonSlurper
import java.security.MessageDigest
import com.gargoylesoftware.htmlunit.*
import groovyx.net.http.HTTPBuilder
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import groovyx.net.http.*
import org.apache.http.entity.mime.MultipartEntityBuilder /* we'll use the new builder strategy */
import org.apache.http.entity.mime.content.ByteArrayBody /* this will encapsulate our file uploads */
import org.apache.http.entity.mime.content.StringBody /* this will encapsulate string params */
import org.apache.commons.io.IOUtils
import org.apache.commons.net.ftp.*
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.URIBuilder
import static groovyx.net.http.ContentType.XML
import static groovyx.net.http.Method.GET
import java.io.InputStream
import java.io.FileInputStream

import java.io.InputStream;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.converter.impl.AnselToUnicode;
import org.marc4j.marc.Record;

config = null;
cfg_file = new File('./sync-marc-cfg.json')
if ( cfg_file.exists() ) {
  config = new JsonSlurper().parseText(cfg_file.text);
}
else {
  config=[:]
  config.packageData=[:]
}

println("Using config ${config}");
def httpbuilder = new HTTPBuilder( 'http://localhost:8080' )
httpbuilder.auth.basic config.uploadUser, config.uploadPass

// Convert input file marcxml to output file marc21
InputStream is = new FileInputStream(args[0])
MarcStreamReader reader = new MarcStreamReader(is);

// See example 15 http://projects.freelibrary.info/freelib-marc4j/tutorial.html
// String stylesheetUrl = 'http://www.loc.gov/standards/mods/v3/MARC21slim2MODS3-6.xsl';
File ss_file = new File('./MARC21slim_MODS3-6_XSLT2-0.xsl');
File res = new File('./mods_records')
Source stylesheet = new StreamSource(ss_file);
Result result = new StreamResult(new FileOutputStream(res));

// Output just XML: MarcXmlWriter writer = new MarcXmlWriter(result);
// Output via XSL:  MarcXmlWriter writer = new MarcXmlWriter(result, stylesheet);
MarcXmlWriter writer = new MarcXmlWriter(result);
writer.setConverter(new AnselToUnicode());


println("Reading records...");
while (reader.hasNext()) {
  println("Record");
  Record record = reader.next();
  // println(record.toString() + "\n************\n");
  // addToGoKB(false, httpbuilder, mods_record)
  writer.write(record);
}
println("Done converting...");




def addToGoKB(dryrun, gokb, title_data) {
  
  try {
    if ( dryrun ) {
      println(title_data)
    }
    else {
      gokb.request(Method.POST) { req ->
        uri.path='/gokb/integration/loadCatalogRecord'
        body = title_data
        requestContentType = ContentType.XML

        response.success = { resp, data ->
          println "Success! ${resp.status} ${data.message}"
        }

        response.failure = { resp ->
          println "Request failed with status ${resp.status}"
          println (title_data);
        }
      }
    }
  }
  catch ( Exception e ) {
    println("Fatal error loading ${title_data}");
    e.printStackTrace();
    System.exit(0);
  }

}
