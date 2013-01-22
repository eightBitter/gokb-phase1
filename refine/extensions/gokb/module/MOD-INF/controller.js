var html = "text/html";
var encoding = "UTF-8";
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

/*
 * Register our custom commands.
 */
function registerCommands() {
  Packages.java.lang.System.out.print("\tRegistering commands...");
  var RS = Packages.com.google.refine.RefineServlet;
  RS.registerCommand(module, "project-checkout", new Packages.com.k_int.gokb.refine.commands.CheckOutProject());
  RS.registerCommand(module, "project-checkin", new Packages.com.k_int.gokb.refine.commands.CheckInProject());
  RS.registerCommand(module, "project-validate", new Packages.com.k_int.gokb.refine.commands.ValidateData());
  RS.registerCommand(module, "rules-suggest", new Packages.com.k_int.gokb.refine.commands.SuggestRules());
  Packages.java.lang.System.out.println("done");
}

/*
 * Function invoked to initialize the extension.
 */
function init() {
  Packages.java.lang.System.out.println("Initializing GOKb...");
  Packages.java.lang.System.out.println(module.getMountPoint());
  registerCommands();
  
  ClientSideResourceManager.addPaths(
		 "index/scripts",
		 module,
		 [
	     "scripts/plugins/jquery.uniform.min.js",
	     "scripts/common.js",
	     "scripts/forms.js",
	     "scripts/index.js",
	     "scripts/index/ui-open-project.js",
		 ]
	);
  
  // Style files to inject into /project page
  ClientSideResourceManager.addPaths(
    "index/styles",
    module,
    [
     "styles/uniform.default.css",
     "styles/uniform.aristo.css",
     "styles/common.less",
     "scripts/forms.js",
     "styles/index.less",
    ]
  );

  // Script files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/scripts",
    module,
    [
     "scripts/plugins/jquery.plugin.selectablerows.js",
     "scripts/plugins/jquery.uniform.min.js",
     "scripts/common.js",
     "scripts/project/validation-panel.js",
     "scripts/project.js",
     "scripts/project/handlers.js",
     "scripts/project/menu.js",
    ]
  );

  // Style files to inject into /project page
  ClientSideResourceManager.addPaths(
    "project/styles",
    module,
    [
      "styles/uniform.default.css",
      "styles/uniform.aristo.css",
      "styles/common.less",
    ]
  );
}