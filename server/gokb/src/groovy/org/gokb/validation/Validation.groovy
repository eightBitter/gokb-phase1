package org.gokb.validation

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.gokb.validation.types.I_ColumnValidationRule
import org.gokb.validation.types.I_DeferredRowValidationRule
import org.gokb.validation.types.A_ValidationRule
import org.gokb.validation.types.I_RowValidationRule

class Validation {

  private static Map<String, List<List>> validationRules = [:]
  
  private static final String CONTEXT_COLUMN = "context-column"
  private static final String CONTEXT_ROW = "context-row"
  private static final Log log = LogFactory.getLog(this)
  
  public static addRule (Class <? extends A_ValidationRule> ruleClass, Object... args) {
	
	// The rules.
	List<List> rules
	String context
	if (I_ColumnValidationRule.class.isAssignableFrom(ruleClass) ) {
	  
	  // Set the context.
	  context = CONTEXT_COLUMN
	  
	} else {
	
	  // Row context.
	  context = CONTEXT_ROW
	}
	
	// Read in the rules already added.
	rules = validationRules[context]
	
	// None added yet.
	if (rules == null) {
	  
	  // Add the context list and default to empty list.
	  rules = []
	}
	
	// Add the rule and instantiation args to the list.
	rules.add ([ruleClass, args])
	
	// Ensure the map is updated.
	validationRules[context] = rules
  }
  
  public static def doValidate (final def project_data) {
	Validation v = new Validation ()
	v.validate(project_data)
  }
  
  private def validate (final def project_data) {
	
	// Define the object to contain the results of the validation routine.
	def result = [
	  messages : [],
	  status : true
	]
	
	// Check processing complete
	checkProcessingComplete (result, project_data)
	
	// Create map containing the column positions.
	def col_positions = [:]
	project_data.columnDefinitions?.each { cd ->
	  log.debug("Assigning col ${cd.name} to position ${cd.cellIndex}");
	  col_positions[cd.name?.toLowerCase()] = cd.cellIndex;
	}
	
	// Check the rules for the column context.
	checkColumnRules (result, col_positions)
	
	// Row-level checks.
	checkRowRules (result, col_positions, project_data)
	
	// Return the result.
	result
  }

  private void checkRowRules (final result, final col_positions, final project_data) {
	
	// The rules.
	List<List> valRules = validationRules [CONTEXT_ROW]
	
	// The lists for the rules.
	List<A_ValidationRule> deferredRules = []
	List<A_ValidationRule> rules = []
	
	// Execute each rule, passing in the column positions for each.
	valRules.each {List ruleDef ->
	  
	  // Instantiate the rule.
	  A_ValidationRule rule = (ruleDef[0] as Class).newInstance(ruleDef[1])
	  
	  // Add to the deferred list too for deferring the call to valid(). 
	  if (rule instanceof I_DeferredRowValidationRule) {
		
		// Add to the deferred List
		deferredRules += rule
		
	  }
	  
	  // Just add to the normal list of rules.
	  rules += rule
	}
	
	// Do the row-level checks.
	// Go through the data and see whether each row is valid.
	def rowNum = 1

	// Go through each row.
	project_data.rowData.each { datarow ->
	  
	  // Execute each rule on the row in turn.
	  rules.each { A_ValidationRule rule ->

		// Go through each rule and execute.
		if (rule instanceof I_DeferredRowValidationRule) {
		  
		  // Call the process method.
		  (rule as I_DeferredRowValidationRule).process(col_positions, rowNum, datarow)
		} else {
		
		  // Call the validate method.
		  (rule as I_RowValidationRule).validate(result, col_positions, rowNum, datarow)
		}
	  }
	  
	  // Increment the row number.
	  rowNum ++
	}
	
	// We now just need to go through the deferred validation rules and call the valid method to
	// get the validation result.
	deferredRules.each { I_DeferredRowValidationRule rule -> 
	  rule.validate(result)
	}
  }
  
  private void checkColumnRules (final result, final col_positions) {
	
	// The rules.
	List<List> valRules = validationRules [CONTEXT_COLUMN]
	
	// Execute each rule, passing in the column positions for each.
	valRules.each {List ruleDef ->
	  
	  // Instantiate the rule.
	  I_ColumnValidationRule rule = (ruleDef[0] as Class).newInstance(ruleDef[1])
	  
	  // Execute the rule.
	  rule.validate(result, col_positions)
	}
  }
  
  private void checkProcessingComplete(result, project_data) {
	if ( project_data?.processingCompleted ) {
	  log.debug("Processing of ingest file completed ok, validating")
	}
	else {
	  log.debug("Processing of ingest file failed, unable to vlidate.")
	  result.messages.add([text:'Unable to process ingest file at this time'])
	  result.status = false
	}
  }
}
