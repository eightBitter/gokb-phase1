package org.gokb

import grails.plugins.springsecurity.Secured
import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.gokb.refine.RefineOperation


class ApiController {
  
  // Internal API return object that ensures consistent formatting of API return objects
  private def apiReturn = {result, String message = "", String status = "success" ->
	  def data = [
		code		: (status),
		result		: (result),
		message		: (message),
      ]
	  
	  def json = data as JSON
	  log.debug (json)
	  render (text: "${params.callback}(${json})", contentType: "application/javascript", encoding: "UTF-8")
  }
  
  private def getFileRepo() {
	File f = new File("./filestore");
    if ( ! f.exists() ) {
      log.debug("Creating upload directory path")
      f.mkdirs();
    }
  }

  def index() { 
  }
  
//  @Secured(["ROLE_USER"])
  def describe() {
	apiReturn(RefineOperation.findAll ())
  }
  
  def saveOperations() {
	  // Get the operations as a list.
	  
	  // The line below looks like it replaces like with like but because the
	  // second parameter is a regex it gets double escaped. 
	  def ops = params.operations.replaceAll("\\\\\"", "\\\\\"")
	  ops = JSON.parse(params.operations)

	  // Save each operation to the database
	  ops.each {
		  try {
			  new RefineOperation(
				  description : it['operation']['description'],
				  operation : new LinkedHashMap(it['operation'])
			  ).save(failOnError : true)
		  } catch (Exception e) {
		  	log.error(e)
		  }
	  }
	  
	  apiReturn( null, "Succesfully saved the operations.")
  }


  @Secured(["ROLE_USER"])
  def ingest() {

    def result = apiReturn (
      [
        [ name:'rule1', blurb:'blurb' ],
        [ name:'rule2', blurb:'blurb' ],
        [ name:'rule3', blurb:'blurb' ],
        [ name:'rule4', blurb:'blurb' ],
      ]
    )

    render result as JSON
	}

	def projectList() {
		def result = apiReturn ([
			[ id: 1, name:'muse_journal_metadata_2012 xls', description:'desc', modified: "2012-11-16T15:15:42Z", locked : true ],
			[ id: 2, name:'Freedom collection 2007 xls', description:'desc', modified: "2012-11-20T16:11:52Z", locked : false ],
		])
	}
	
	def downloadProject() {
		
		if (params.project) {
			// Get the project file. 
			def file = new File(getFileRepo())
			response.setContentType("application/octet-stream")
			response.setHeader("Content-disposition", "attachment;filename=${file.getName()}")
			response.outputStream << file.newInputStream()
			
		} else response.status = 404;
	}
}
