/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

/**
 * Application objects base class
 * <p>
 * All application classes should extend this class
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class AppObject {
    App app;
    Project project;
    Logger logger;
    String analysisId;

    /**
     * Instantiates an AppObject - constructor
     * <p>
     * Base class for all application classes
     * Requires main App object to have been created
     * 
     * @param   project project object if applicable, may be null
     * @param   logger  logger object if applicable, may be null - defaults to app.logger
     */
    public AppObject(Project project, Logger logger) {
        // check for App not set for message but just let exception happen anyway
        if(Tappas.getApp() == null)
            System.out.println("AppObjects can not be created before the App object is created.\nUse AppObject(App app) to create objects from App constructor or class declaration.");
        this.app = Tappas.getApp();
        this.project = project;
        this.logger = (logger == null)? app.logger : logger;
    }
    /**
     * Instantiates an AppObject - constructor
     * <p>
     * Base class for all application classes
     * Requires main App object to have been created
     * 
     * @param   project project object if applicable, may be null
     * @param   logger  logger object if applicable, may be null - defaults to app.logger
     * @param   analysisId  id for specific analysis (used by DFI plots)
     */
    public AppObject(Project project, Logger logger, String analysisId) {
        // check for App not set for message but just let exception happen anyway
        if(Tappas.getApp() == null)
            System.out.println("AppObjects can not be created before the App object is created.\nUse AppObject(App app) to create objects from App constructor or class declaration.");
        this.app = Tappas.getApp();
        this.project = project;
        this.logger = (logger == null)? app.logger : logger;
        this.analysisId = analysisId;
    }
    /**
     * Instantiates an AppObject - constructor
     * <p>
     * ONLY use from App in cases where the app object is not available yet
     * 
     * @param   app     application object
     */
    public AppObject(App app) {
        this.app = app;
        this.project = null;
        this.logger = app.logger;
    }
}
