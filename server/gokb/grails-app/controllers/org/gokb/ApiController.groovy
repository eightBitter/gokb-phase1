package org.gokb

import static java.util.UUID.randomUUID
import grails.converters.JSON
import grails.plugins.springsecurity.Secured

import org.gokb.refine.RefineOperation
import org.gokb.refine.RefineProject
/**
 * TODO: Change methods to abide by the RESTful API, and implement GET, POST, PUT and DELETE with proper response codes.
 * 
 * @author Steve Osguthorpe
 */

class ApiController {
	
	
	/**
	 * TODO: The below versionCheck and before interceptor code checks for a custom request header.
	 * Cross-domain AJAX calls using JSONP strip out custom headers and so the code fails when it shouldn't.
	 * The code will work once all ajax requests are proxied through the custom refine code as the custom headers,
	 * are not stripped from the request.  
	 */
	
//	def beforeInterceptor = [action: this.&versionCheck]
//	
//	// defined with private scope, so it's not considered an action 
//	private versionCheck() {
//		def gokbVersion = request.getHeader("GOKb-version")
//		if (gokbVersion != 0.3) {
//		  apiReturn("", "You are using an out of date version of the GOKb extension. " +
//			  "Please download and install the latest version. From http://gokb.k-int.com/extension/latest",
//			  "error"
//		  )		  
//		  return false
//		}
//	}

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

		String fs = "./project-files/"
		File f = new File(fs)
		if ( ! f.exists() ) {
			log.debug("Creating upload directory path")
			f.mkdirs()
		}

		// Return the filestore
		fs
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
		apiReturn ( RefineProject.findAll() )
	}

	def projectCheckout() {
		
		log.debug(params)
		if (params.projectID) {
			
			// Get the project.
			def project = RefineProject.load(params.projectID)
			
			if (project) { 
			
				// Get the file and send the file to the client.
				def file = new File(getFileRepo() + project.file)
				
				
				// Send the file.
				response.setContentType("application/x-gzip")
				response.setHeader("Content-disposition", "attachment;filename=${file.getName()}")
				response.outputStream << file.newInputStream()
				
				// Set the checkout details.
				def chOut = (params.checkOutName ?: "No Name Given") +
					" (" + (params.checkOutEmail ?: "No Email Given") + ")"
				project.setCheckedOutBy(chOut)
				project.setCheckedIn(false)
				project.setLocalProjectID(params.long("localProjectID"))
				return
			}
		}
		
		// Send 404 if not found.
		response.status = 404;
	}
	
	def projectCheckin() {
		
		def f = request.getFile('projectFile')
		
		log.debug(params)
		if (f && !f.empty) {
			
			// Get the project.
			def project
			if (params.projectID) {
				project = RefineProject.load(params.projectID)
			} else {
			
				// Creating new project.
				project = new RefineProject()
				project.setHash(params.hash ?: null)
			}
			
			if (project) {
				
				// Generate a filename...
				def fileName = "project-${randomUUID()}.tar.gz"
				
				// Save the file.
				f.transferTo(new File(getFileRepo() + fileName))
				
				// Set the file property.
				project.setFile(fileName)
				
				// Update other project properties.
				if (params.description) project.setDescription(params.description)
				if (params.name) project.setName(params.name)
				project.setCheckedIn(true)
				project.setCheckedOutBy(null)
				project.setLocalProjectID(null)
				project.setModified(new Date())
				
				// Save and flush.
				project.save(flush: true, failOnError: true)
				
				apiReturn(project)
				return
			}
		} else if (params.projectID) {
		
			// Check in with no changes. (In effect we are just removing the lock)
			def project = RefineProject.load(params.projectID)
			if (project) {
				
				// Remove lock properties and return the project state.
				project.setCheckedIn(true)
				project.setCheckedOutBy(null)
				project.setLocalProjectID(0)
				project.save(flush: true, failOnError: true)
				apiReturn(project)
				return
			}
		}
		
		// Send 404 if not found.
		response.status = 404;
	}
}
