#!groovy

@Grapes([
  @GrabResolver(name='mvnRepository', root='http://central.maven.org/maven2/'),
  @Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.14'),
  @Grab(group='javax.mail', module='mail', version='1.4.7'),
  @Grab(group='net.sourceforge.htmlunit', module='htmlunit', version='2.21'),
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.2');
  @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.3.5');
  @Grab(group='org.apache.httpcomponents', module='httpmime', version='4.3.5');
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
import static groovyx.net.http.ContentType.URLENC
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import groovyx.net.http.*

config = null;
cfg_file = new File('./handler-cfg.json')
if ( cfg_file.exists() ) {
  config = new JsonSlurper().parseText(cfg_file.text);
}
else {
  config=[:]
  config.packageData=[:]
}

println("Using config ${config}");

println("Pulling latest messages");
pullLatest(config,'http://holdings.sciencedirect.com/ehr/manageProductReports.url');
println("All done");

println("Updating config");
cfg_file << toJson(config);



def pullLatest(config, url) {
  def result = false;

  def http = new HTTPBuilder( 'http://localhost:8080' )
  http.auth.basic 'admin', 'admin'


  println("Get URL ${url}");
  client = new WebClient()
  client.getOptions().setThrowExceptionOnScriptError(false);
  client.getOptions().setJavaScriptEnabled(true);
  client.getOptions().setRedirectEnabled(true);
  client.getOptions().setCssEnabled(false);
  client.getOptions().setTimeout(600000);
  client.waitForBackgroundJavaScript(8000);
  client.setAjaxController(new NicelyResynchronizingAjaxController());
  client.getCookieManager().setCookiesEnabled(true);

  // Added as HtmlUnit had problems with the JavaScript
  // client.javaScriptEnabled = true
  html = client.getPage(url);
  
  boolean next_page = true;
  int page_count = 0;
  int package_count = 0;

  while(next_page) {
    page_count++
    // List<?> links = page.getByXPath("//div[@class='generate']/@href");
    List<?> links = html.getByXPath("//a/@href");
    println("Processing ${links.size()} links");
    links.each { link ->
      if ( link.value.startsWith('../holdings/productReport.url') ) {
        def package_name = link.getOwnerElement().getParentNode().getByXPath('../td[@class="report"]/text()');
        processFile(package_name[0],link.value, config, http);
        package_count++;
      }
    }
  
    def next_page_links = html.getByXPath("//a[text()='Next >']")
    if ( next_page_links.size() > 0 ) {
      html = next_page_links[0].click();
    }
    else {
      next_page = false;
    }
  }
  
  println("Done ${page_count} pages");
  println("Done ${package_count} packages");
}

def processFile(official_package_name, link, config, http) {
  def url_to_fecth = "http://holdings.sciencedirect.com/"+link.substring(3,link.length())
  println("fetching ${official_package_name} - ${url_to_fecth}");
  def package_data = new URL(url_to_fecth).getText()


  MessageDigest md5_digest = MessageDigest.getInstance("MD5");
  InputStream md5_is = new ByteArrayInputStream(package_data.getBytes());

  int filesize = 0;
  byte[] md5_buffer = new byte[8192];
  int md5_read = 0;
  while( (md5_read = md5_is.read(md5_buffer)) >= 0) {
    md5_digest.update(md5_buffer, 0, md5_read);
    filesize += md5_read
  }
  md5_is.close();
  byte[] md5sum = md5_digest.digest();
  def md5sumHex = new BigInteger(1, md5sum).toString(16);

  // println("Hash for ${link} is ${md5sumHex}");

  if ( config.packageData[official_package_name] == null ) {
    config.packageData[official_package_name] = [ cksum:0 ];
  }

  if ( md5sumHex == config.packageData[official_package_name].cksum ) {
    println("Checksum not changed - Skipping");
  }
  else {
    println("Checksum changed - process file");
    pushToGokb(official_package_name, package_data, http);
    config.packageData[official_package_name].cksum = md5sumHex
    config.packageData[official_package_name].lastProcessed = System.currentTimeMillis()
  }

}

def pushToGokb(name, data, http) {
  // curl -v --user admin:admin -X POST \
  //   $GOKB_HOST/gokb/packages/deposit

  http.request(Method.POST) { req ->
    uri.path="/gokb/packages/deposit"

    MultipartEntityBuilder multiPartContent = new MultipartEntityBuilder()
    // Adding Multi-part file parameter "imageFile"
    multiPartContent.addPart("content", new InputStreamBody( new ByteArrayInputStream(data.getBytes()), 
                                        "application/tsv",
                                        name))

    // Adding another string parameter "city"
    multiPartContent.addPart("source", new StringBody("ELSEVIER"))
    multiPartContent.addPart("fmt", new StringBody("elsevier"))
    multiPartContent.addPart("pkg", new StringBody(name))
    multiPartContent.addPart("platformUrl", "http://www.sciencedirect.com/science");
    multiPartContent.addPart("format", "JSON");
    multiPartContent.addPart("providerName", "elsevier");
    multiPartContent.addPart("providerIdentifierNamespace", "ELSEVIER");
    multiPartContent.addPart("reprocess", "Y");
    multiPartContent.addPart("synchronous", "Y");
    multiPartContent.addPart("flags", "+ReviewNewTitles,+ReviewVariantTitles,+ReviewNewOrgs");
    
    req.entity = multiPartContent.build()

    response.success = { resp, data ->
      if (resp.statusLine.statusCode == 200) {
        // response handling
        println("OK");
      }
    }
  }
}

