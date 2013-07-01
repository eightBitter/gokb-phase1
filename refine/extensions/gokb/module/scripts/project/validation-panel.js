/**
 * Validation panel constructor.
 */	
function ValidationPanel(div, tabHeader) {
  this._div = div;
  this._tabHeader = tabHeader;
  this.update();
}

/**
 * Resize this panel to fit it's contents.
 */
ValidationPanel.prototype.resize = function() {
  var body = this._div.find(".validation-panel-body");
  
  if (body && body.length > 0) {

	  var bodyPaddings = body.outerHeight(true) - body.height();
	  body.height((this._div.height() - bodyPaddings) + "px");
	  body[0].scrollTop = body[0].offsetHeight;
  }
};

/**
 * Update the panel data.
 */
ValidationPanel.prototype.update = function(onDoneFunc) {
  var self = this;
  
  // Set the _data attribute to the data.
  var params = {"project" : theProject.id};
	
  // Post the columns to the service
  GOKb.doCommand (
    "project-validate",
    params,
    null,
    {
    	onDone : function (data) {
    		
    		if ("result" in data && "status" in data.result) {
    			
    			self.data = data.result;
    			
    		  // Then render.
    		  self._render();
    		}
    		
    		if (onDoneFunc) {
    			onDoneFunc();
    		}
    	}
  	}
  );
};

/**
 * Render this panel.
 */
ValidationPanel.prototype._render = function() {
  var self = this;

  // Reload the HTML 
  self._div.empty().unbind().html(DOM.loadHTML("gokb", "scripts/project/validation-panel.html"));

  // Bind the elements.
  var elmts = DOM.bind(this._div);
  
  // Check the data
  var data = self.data;
  if ("status" in data) {
  	
  	// Add the errors and warnings.
		var errorMess = [];
		var warnMess = [];
		if ("messages" in data) {
			
			// hasError.
			var hasError = false;
			
			$.each(data.messages, function() {
				
				// Get the message.
				var message = this;
				
				// The link to display the menu.
				var menuLink = $("<a class='button' href='javascript:{}' ><img src='images/right-arrow.png'></a>")
					.appendTo($("<div class='gokb-message-actions' />"))
					.click(function() {
						ValidationPanel.messages.getActions(message, $(this));
					});
				;
				
				if (message.severity == "error") {
					// Push the data to the error table.
					errorMess.push(["<span class='error' >" + message.text + "</span>", menuLink]);
				} else {
					// Push the data to the error table.
					warnMess.push(["<span class='warning' >" + message.text + "</span>", menuLink]);
				}
				
			});
			
			// Set the header error count to the correct number. 
			self._tabHeader.html('Errors <span class="error count">' + errorMess.length + '</span> / <span class="warning count">' + warnMess.length + '</span>');
			
			// Clear the HTML first.
			elmts.validationContent.html("");
			
			// Append the table to the dialog...
			if (errorMess.length > 0) {
				
				// Error message table.
				var errorMessages = GOKb.toTable (
				  ["<span class='error' >Error messages</span>", ""],
				  errorMess
				).addClass("error");
				
				// Add the table.
				elmts.validationContent.append(errorMessages);
				hasError = true;
			}
			if (warnMess.length > 0) {

				// Warning message table.
				var warnMessages = GOKb.toTable (
	 			  ["<span class='warning' >Warning messages</span>", ""],
				  warnMess
				).addClass("warning");
				
				// Add the table.
				elmts.validationContent.append(warnMessages);
				hasError = true;
			}
		}
  	
  	if (hasError) {
  		$('h1', elmts.panelContent).hide();
  	} else {
  		
			// Set the header error count to the correct number.
  		self._tabHeader.html('Errors <span class="error count">0</span> / <span class="warning count">0</span>');
  		
  		elmts.validationContent
  			.html("<p>The current project has passed all validation rules.</p>")
  			.append(
  			  $("<div>").attr("id", "gokb-ingest-button").append(
			  		$('<button />')
			  			.addClass("button")
			  			.text("Begin ingest process")
			  			.click(function() {
			  				GOKb.handlers.estimateChanges();
			  			})
			  	)
		  	)
  		;
  		$('h1', elmts.panelContent).show();
  	}
  } else {
  	self._tabHeader.html('Errors <span class="count error">0</span>');
		elmts.validationContent.html("<p>Unable to contact the GOKb service for validation information..</p>");
  }

  // Resize the panel.
  this.resize();
};