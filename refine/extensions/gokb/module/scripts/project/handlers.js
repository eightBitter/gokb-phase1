/**
 * Handlers
 */

GOKb.handlers.suggest = function() {
	// Merge the meta-data and columns together.
	var params = {"project" : theProject.id};
	
  // Post the columns to the service
  GOKb.doCommand (
    "rules-suggest",
    params,
    null,
    {
    	onDone : function (data) {
    		
    		// Create and show a dialog with the returned list attached.
    		var dialog = GOKb.createDialog("Suggested Rules", "suggest");
    		
    		if ("result" in data && data.result.length > 0) {
    		
	    		// Create data.
	    		var DTData = [];
	  			$.each(data.result, function () {
	  				DTData.push([this.description]);
	  			});
	  			
	  			// Create the Table.
	  			var table = GOKb.toTable (
	   			  ["Rule"],
	   			  DTData
	   			);
	  			
	  			// Add selection checkboxes
	  			table.selectableRows();
	    		
	    		table.appendTo(dialog.bindings.dialogContent);
	  			
	  			// Create an apply rules button
	  			$("<button>Apply Rules</button>").addClass("button").click(function() {
	  				
	  				// Get the indexes of the selected elements.
	  				var selected = table.selectableRows("getSelected");
	  				
	  				var confirmed = confirm("Are you sure you wish to apply these " + selected.length + " operations to your document?");
	  				
	  				if (confirmed) {
	  					
	  					var ops = [];
	  					
	  					// Get the selected rules from the data.
	  					$.each(selected, function () {
	  						var op = JSON.parse (data.result[Number(this)].ruleJson);
	  						ops.push(op);
	  	  			});
	  					
	  					// Apply the rules through the existing api method.
	  					Refine.postCoreProcess(
	  					  "apply-operations",
	  					  {},
	  					  { operations: JSON.stringify(ops) },
	  					  { everythingChanged: true },
	  					  {
	  					  	onDone: function(o) {
	  					  		if (o.code == "pending") {
	  					  			// Something might have already been done and so it's good to update.
	  					  			Refine.update({ everythingChanged: true });
	  					  		}
	  					  	}
	  					  }
	  					);
	  					
	  					// Close the dialog
	  					DialogSystem.dismissUntil(dialog.level - 1);
	  				}
	  			}).appendTo(
	  			  dialog.bindings.dialogFooter
	  			);
    		} else {
    			// Just output nothing found.
    			dialog.bindings.dialogContent.html("<p>No rule suggestions have been found for the current standing of this document.</p>");
    		}
    		
    		// Show the dialog.
    		GOKb.showDialog(dialog);
    	}
  	}
  );
};

// Display a list of operations applied to this project
GOKb.handlers.history = function() {
	GOKb.doRefineCommand("core/get-operations", {project: theProject.id}, null, function(data){
		var dialog = GOKb.createDialog("Applied Operations");
		if ("entries" in data && data.entries.length > 0) {
			
			// Build a JSON data object to display to the user.
			var DTDdata = [];
			$.each(data.entries, function () {
				if ("operation" in this) {
					
					// Include only operations.
					DTDdata.push([this.description]);
				}
			});
			
			// Create a table from the data.
			var table = GOKb.toTable (
			  ["Operation"],
			  DTDdata
			);
			
			// Append the table
			table.appendTo(dialog.bindings.dialogContent);
			
			// Add a button to send the data up to the GOKb server.
			$("<button>Send Operations</button>").addClass("button").click(function() {
				GOKb.doCommand(
				  "saveOperations",
				  {},
				  {
				  	// Entries.
				  	operations : JSON.stringify(data.entries)
				  },
				  {
				  	onDone : function () {
				  		
				  		// Close the dialog
	  					DialogSystem.dismissUntil(dialog.level - 1);
				    }
				  }
				);
			}).appendTo(
			  // Append to the footer.
			  dialog.bindings.dialogFooter
			);
	  } else {
	  	// Just output nothing found.
	  	dialog.bindings.dialogContent.html("<p>No operations have been applied yet.</p>");
	  }
		GOKb.showDialog(dialog);
	});
};

/**
 * Prompt the user to check project properties and then check in the project.
 */

GOKb.handlers.checkInWithProps = function() {
	// Create the form to collect some basic data about this document.
	var dialog = GOKb.createDialog("Suggested Operations", "form_project_properties");
	
	// Change the location to send the data to project check-in.
	dialog.bindings.form.attr("action", "command/gokb/project-checkin");
	var params = jQuery.extend({update : true}, GOKb.projectDataAsParams(theProject));
	
	// Change the submit button text to be check-in
	dialog.bindings.submit.attr("value", "Save and Check-in");
	
	// Get the refdata from GOKb service.
	GOKb.getRefData ({type: "cp"}, {
		onDone : function (data) {
			
			if ("result" in data && "datalist" in data.result) {
			
				var orgList = $('#org', dialog.bindings.form);
				$.each(data.result.datalist, function (value, display) {
					var opt = $('<option />', {"value" : value})
						.text(display)
					;
					
					// Select the current value...
					if (value == params.org) {
						opt.attr('selected', 'selected');
					}
					
					// Append the arguments...
					orgList.append(
					  opt
					);
				});
				
				// Add the project params as hidden fields to the form.
				GOKb.paramsAsHiddenFields(dialog.bindings.form, params);
			}
		}
	});
	
	// Rename close button to cancel.
	dialog.bindings.closeButton.text("Cancel");
	
	// Show the form.
	return GOKb.showDialog(dialog);
};