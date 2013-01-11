#!/usr/bin/groovy

// @GrabResolver(name='es', root='https://oss.sonatype.org/content/repositories/releases')
@Grapes([
  @Grab(group='net.sf.opencsv', module='opencsv', version='2.0'),
  @Grab(group='org.apache.httpcomponents', module='httpmime', version='4.1.2'),
  @Grab(group='org.apache.httpcomponents', module='httpclient', version='4.0'),
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.0'),
  @Grab(group='org.apache.httpcomponents', module='httpmime', version='4.1.2')
])


import groovy.util.slurpersupport.GPathResult
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.*
import org.apache.http.entity.mime.*
import org.apache.http.entity.mime.content.*
import java.nio.charset.Charset
import org.apache.http.*
import org.apache.http.protocol.*
import org.apache.log4j.*
import au.com.bytecode.opencsv.CSVReader
import java.text.SimpleDateFormat


// Load the fam reconcilliation data
// def target_service = new RESTClient('http://metadata.ukfederation.org.uk')

// try {
//   target_service.request(GET, ContentType.XML) { request ->
//     uri.path='/ukfederation-metadata.xml'
//     response.success = { resp, data ->
//       // data is the xml document
//       ukfam = data;
//     }
//     response.failure = { resp ->
//       println("Error - ${resp.status}");
//       System.out << resp
//     }
//   }
// }
// catch ( Exception e ) {
//   e.printStackTrace();
// }

// To clear down the gaz: curl -XDELETE 'http://localhost:9200/gaz'
// CSVReader r = new CSVReader( new InputStreamReader(getClass().classLoader.getResourceAsStream("./IEEE_IEEEIEL_2012_2012.csv")))
println("Processing ${args[0]}");
CSVReader r = new CSVReader( new InputStreamReader(new FileInputStream(args[0]),java.nio.charset.Charset.forName('UTF-8')) )

String [] nl;

int rownum = 0;

// Read column headings
nl = r.readNext()
println("Column heads: ${nl}");

while ((nl = r.readNext()) != null) {
  // println("Process line ${nl}");
  // Internal ID,Parent Org. ID,Authorized Name,Organization Name,Provider,Vendor,Publisher,Licensor
  def org_assert = [    
    name:nl[3],
    description:nl[3],
    customIdentifers:[
      [identifierType:'ncsu-internal',identifierValue:"ncsu:nl[0]"]
    ],
    combos:[],
    flags:[]
  ]

  if ( nl[0] != nl [1] ) {
    // Add a combo that links to the parent org
    org_assert.combos.add([linkTo:[identifierType:'ncsu-internal',identifierValue:"ncsu:${nl[1]}"], linkType:'ParentOrg'])
  }

  if ( nl[4] == 'Y' )
    org_assert.flags.add([flagType:'Org Role',flagValue:'Content Provider'])

  if ( nl[5] == 'Y' )
    org_assert.flags.add([flagType:'Org Role',flagValue:'Vendor'])

  if ( nl[6] == 'Y' )
    org_assert.flags.add([flagType:'Org Role',flagValue:'Publisher'])

  if ( nl[7] == 'Y' )
    org_assert.flags.add([flagType:'Org Role',flagValue:'Licensor'])

  org_assert.flags.add([flagType:'Authorized',flagValue:nl[2]])


  println("assert that : ${org_assert}");
}
