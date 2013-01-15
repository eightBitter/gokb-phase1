package org.gokb

import static java.util.UUID.randomUUID
import grails.converters.JSON
import grails.plugins.springsecurity.Secured
import org.gokb.refine.RefineOperation
import org.gokb.refine.RefineProject
import org.gokb.cred.Org;

/**
 * TODO: Change methods to abide by the RESTful API, and implement GET, POST, PUT and DELETE with proper response codes.
 * 
 * @author Steve Osguthorpe
 */

class ApiController {

  private static final String REQUIRED_EXTENSION_VERSION = "0.5"

  def ingestService
  def grailsApplication

  /**
   * Before interceptor to check the current version of the refine
   * plugin that is being used.
   */

  def beforeInterceptor = [action: this.&versionCheck]

  // defined with private scope, so it's not considered an action
  private versionCheck() {
	def gokbVersion = request.getHeader("GOKb-version")
	if (gokbVersion != REQUIRED_EXTENSION_VERSION) {
	  apiReturn([:], "You are using an out of date version of the GOKb extension. " +
		  "Please download and install the latest version. From http://gokb.k-int.com/extension/latest",
		  "error"
		  )
	  return false
	}
  }

  // Internal API return object that ensures consistent formatting of API return objects
  private def apiReturn = {result, String message = "", String status = "success" ->
	def data = [
		  code    : (status),
		  result    : (result),
		  message    : (message),
		]

	def json = data as JSON
	log.debug (json)
	render json
	//    render (text: "${params.callback}(${json})", contentType: "application/javascript", encoding: "UTF-8")
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
		def file = new File(grailsApplication.config.project_dir + project.file)

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

	
	log.debug(params)
	
	def f = request.getFile('projectFile')

	if (f && !f.empty) {

	  // Get the project.
	  RefineProject project
	  if (params.projectID) {
		project = RefineProject.load(params.projectID)
	  } else {
		// Creating new project.
		project = new RefineProject()
		project.setHash(params.hash ?: null)

		// Set the org too.
		log.debug("Setting provider from submission.");
		Org org = Org.get(params.org)
		if (org) {
		  project.provider = org
		}
	  }

	  if (project) {

		// A quick hack to set the project provider, this should come from refine, but for testing purposes, we set this to Wiley
//		if ( !project.provider ) {
//		  log.debug("Defaulting in provider, this should be set from the refine project initially. #FixMe");
//		  project.provider = Org.findByName('Wiley') ?: new Org(name:'Wiley').save();
//		}

		// Generate a filename...
		def fileName = "project-${randomUUID()}.tar.gz"

		// Save the file.
		f.transferTo(new File(grailsApplication.config.project_dir + fileName))

		// Set the file property.
		project.setFile(fileName)

		// Update other project properties.
		if (params.description) project.setDescription(params.description)
		if (params.name) project.setName(params.name)
		project.setCheckedIn(true)
		project.setCheckedOutBy(null)
		project.setLocalProjectID(null)
		project.setModified(new Date())
		
		// Make sure we null the progress...
		project.setProgress(null)
		if (params.ingest) {
		  // Try and ingest the project too!
		  projectIngest(project)
		}

		// Save and flush.
		project.save(flush: true, failOnError: true)

		// Return the project data.
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

  private def projectIngest (RefineProject project) {
	if (project.getCheckedIn()) {

	  // Do the ingest.
	  log.debug("extract refine project");
	  def parsed_data = ingestService.extractRefineproject(project.file)

	  log.debug("Validate the project");
	  def validationResult = ingestService.validate(parsed_data)
	  project.lastValidationResult = validationResult.messages

	  if ( validationResult.status == true ) {
		ingestService.extractRules(parsed_data, project)
		doIngest(parsed_data, project);
	  }
	  else {
		log.debug("validation failed, not ingesting");
	  }
	} else {
	  log.debug ("Attempted to ingest checked-out project.")
	}
  }
  
  def projectDataValid() {
	def f = request.getFile('projectDataZip')

	def validationResult = [:]
	
	if (f && !f.empty) {
	  def parsed_data = extractRefineDataZip(f)
	  
	  log.debug("Validate the data in the zip");
	  validationResult = ingestService.validate(parsed_data)
	  apiReturn(validationResult)
	} else {
	  
	  log.debug("No file sent to be validated.")
	}
  }

  private def doIngest(parsed_data, project) {
	log.debug("ingesting refine project.. kicking off background task");
	runAsync {
	  ingestService.ingest(parsed_data, project)
	}
  }

  def refdata() {
	def result = [:];

	// Should take a type parameter and do the right thing. Initially only do one type
	switch ( params.type ) {
	  case 'cp' :
		def oq = Org.createCriteria()
		def orgs = oq.listDistinct {
		  tags {
			owner {
			  eq('desc','Org Role');
			}
			eq('value','Content Provider');
		  }
		}
		result.datalist=[:]
		orgs.each { o ->
		  result.datalist["${o.id}"] = o.name
		}
		break;
	  default:
		break;
	}

	apiReturn(result)
  }
}
