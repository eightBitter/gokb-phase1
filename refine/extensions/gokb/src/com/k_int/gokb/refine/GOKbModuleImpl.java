package com.k_int.gokb.refine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import javax.servlet.ServletConfig;

import org.apache.commons.collections.ExtendedProperties;

import com.k_int.gokb.refine.commands.GerericProxiedCommand;

import com.google.refine.RefineServlet;
import com.google.refine.importing.ImportingManager;

import edu.mit.simile.butterfly.ButterflyModule;
import edu.mit.simile.butterfly.ButterflyModuleImpl;


public class GOKbModuleImpl extends ButterflyModuleImpl {
    
    public static GOKbModuleImpl singleton;
    public static ExtendedProperties properties;
    
    public static final String VERSION = "0.5";
    
    @Override
    public void init(ServletConfig config) throws Exception {
        
        // Run default init method.
        super.init(config);
        
        // Perform our extended initialisation...
        extendModuleProperties();
        swapImportControllers();
        
        // Add our proxied Commands from the config file.
        addProxiedCommands();
        
        // Set the singleton.
        singleton = this;
        
        // Set the properties
        properties = singleton.getProperties();
    }
    
    private void addProxiedCommands() {
        @SuppressWarnings("unchecked")
        List<String> commands = getProperties().getList("proxyCommands");
        
        // Register each command from the list.
        for (String command : commands) {
            RefineServlet.registerCommand(this, command, new GerericProxiedCommand(command));
        }
    }
    
    private void extendModuleProperties() {
        // The module path
        File f = getPath();
        
        // Load our custom properties.
        File modFile = new File(f,"MOD-INF");
        if (modFile.exists()) {
            try {
                // Get the existing module properties and overload with our extras.
                File propFile = new File(modFile,"gokb.properties");
                if (propFile.exists()) {
                    ExtendedProperties p = new ExtendedProperties();
                    _logger.info("Loading GOKb properties ({})", propFile);
                    BufferedInputStream stream = null;
                    try {
                        stream = new BufferedInputStream(new FileInputStream(propFile));
                        p.load(stream);
                    } finally {
                        // Close the stream.
                        if (stream != null) stream.close();
                    }
                    
                    // Add to module properties.
                    getProperties().combine(p);
                }
            } catch (Exception e) {
                _logger.error("Error loading GOKb properties", e);
            }
        }
    }
    
    private void swapImportControllers() {
        // Get the core module.
        ButterflyModule coreMod = getModule("core");
        String controllerName = "default-importing-controller";

        // Remove default controller.
        ImportingManager.controllers.remove(
          coreMod.getName() + "/" + controllerName
        );

        // Now register our controller at the default key.
        ImportingManager.registerController(
          coreMod,
          controllerName,
          new GOKbImportingController()
        );        
    }
}