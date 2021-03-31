/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Tappas extends Application {
    // Single application instance lock - if you find a better method, use it
    // This is just to keep users from accidentally working
    // on the same project with multiple apps (could mess up the project data)
    // I think in the new Java 9 release there might be some relevant functionality
    //
    //
    // The app will check if APP_LOCK_FILE exists:
    //   a) if it does, it will get the port number from the file and try to lock it
    //      (just in case the app aborted and did not delete the file)
    //      If there is no port number or if it's invalid, it will perform step (b)
    //      otherwise, it will just inform the user that the app is already running
    //      WARNING: if the app shutdowns w/o deleting file, 
    //               it is possible that a different app is now using the port
    //               May want to deaTo cite us visit: l with it later if we stick to this locking approach
    //               In unix/linux/mac, can execute script and use netstat for port and then ps to get the process
    //   b) If file not found, will look for a port to lock and create
    //      a lock file to save the number - when exiting, the app will delete the file
    public static final int APP_PORT = 32131;
    public static final int APP_PORT_RANGE = 20;
    private static ServerSocket ss;
    private static boolean ownPortFile = false;

    // App info
    public static final String APP_LONG_NAME = "tappAS";
    public static final String APP_NAME = "tappas";
    public static final int APP_MAJOR_VER = 1;
    public static final int APP_MINOR_VER = 0;
    public static final int APP_STRREV = 7;
    public static final String RES_VER_FILE = "resource_v" + APP_MAJOR_VER + "." + APP_MINOR_VER + "." + APP_STRREV + ".txt";
    public static final String APP_LOCK_FILE = ".tappas.lock.prm";
    public static final String APP_PORT_PARAM = "port";
    public static final String APP_STRVER = APP_MAJOR_VER + "." + String.format("%01d", APP_MINOR_VER) + "." + String.format("%01d", APP_STRREV);
    public static final double APP_VER = APP_MAJOR_VER + (APP_MINOR_VER/100.0);
    public boolean havePackages = false;
    private static final App app = new App();
    private static HostServices host;
    private static String appInstanceId = "";
    private static Scene scene;
    private static Stage stage;
    private static String[] appArgs;
    //Change default .jar icon 
    java.net.URL url = ClassLoader.getSystemResource("/images/logo.png");
    
    @Override
    public void start(Stage startStage) throws Exception {
        stage = startStage;
        host = getHostServices();
        int pos = this.toString().indexOf("@") + 1;
        if(pos != -1)
            appInstanceId = this.toString().substring(pos);
        
        // get app folder paths
        if(app.data.getFolderPaths(stage)) {
            // check if OK to run application
            if(OKToRun()) {
                app.runRscriptVersion();
                app.logInfo(APP_NAME + " " + APP_STRVER + " using Java " + System.getProperties().getProperty("javafx.runtime.version"));

                Parent root = FXMLLoader.load(Tappas.class.getResource("/tappas/AppFXMLDocument.fxml"));
                scene = new Scene(root);
                stage.setTitle(APP_LONG_NAME);
                stage.getIcons().add(new Image(Tappas.class.getResourceAsStream("/tappas/images/logo.png")));
                stage.setMinWidth(1050);
                stage.setMinHeight(600);
                stage.setWidth(1200);
                stage.setHeight(750);
                stage.setScene(scene);
                stage.setOnCloseRequest((event) -> { 
                    app.logInfo("Application close request.");
                });
                
                stage.show();

                // trap the closing request and confirm shutdown if tasks running
                stage.setOnCloseRequest(event -> {
                    if(!app.shutdown())
                        event.consume();
                    else {
                        // remove port lock parameter file
                        if(ownPortFile)
                            Utils.removeFile(Paths.get(app.data.getAppDataBaseFolder(), APP_LOCK_FILE));
                    }
                });
                
            }
            else {
                app.ctls.alertError("tappAS - Application Already Running", "tappAS application is already running.\nYou may only run a single instance\n of the application.\n");
                Platform.exit();
            }
        }
        else {
            Platform.exit();
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        appArgs = args;
        launch(args);
    }
    
    // Application wide functions
    public static String[] getArgs() {
        return appArgs;
    }
    public static HostServices getHost() {
        return host;
    }
    public static Stage getStage() {
        return stage;
    }
    public static Scene getScene() {
        return scene;
    }
    public static Window getWindow() {
        return scene.getWindow();
    }
    public static App getApp() {
        return app;
    }
    public static String getAppInstanceId() {
        return appInstanceId;
    }

    //
    // Internal Functions
    //
    
    private boolean OKToRun() {
        // make sure this is the only instance running
        int port = -1;
        boolean run = false;
        
        // check if we have an existing port parameter file
        if(Files.exists(Paths.get(app.data.getAppDataBaseFolder(), APP_LOCK_FILE))) {
            // get port number and make sure it's still being used
            HashMap<String, String> hmParams = new HashMap<>();
            Utils.loadParams(hmParams, Paths.get(app.data.getAppDataBaseFolder(), APP_LOCK_FILE).toString());
            if(hmParams.containsKey(APP_PORT_PARAM)) {
                try {
                    // check for valid port number
                    int pn = Integer.parseInt(hmParams.get(APP_PORT_PARAM).trim());
                    if(pn >= APP_PORT && pn < (APP_PORT + APP_PORT_RANGE)) {
                        port = pn;
                        System.out.println("Found parameter file set for port " + port + ".");
                        try {
                            // make sure app did not terminate w/o deleting the file
                            ss = new ServerSocket(port);
                            Utils.saveParams(hmParams, Paths.get(app.data.getAppDataBaseFolder(), APP_LOCK_FILE).toString());
                            ownPortFile = true;
                            run = true;
                            app.logInfo("Found port number " + port + " in paramter file but not in use.");
                        }
                        catch(Exception e) { System.out.println("Port " + port + " in use."); }
                    }
                    else {
                        app.logInfo("Invalid port number, " + hmParams.get(APP_PORT_PARAM).trim() + ", parameter value.");
                    }
                } catch(Exception e) { 
                    app.logInfo("Invalid port number, " + hmParams.get(APP_PORT_PARAM).trim() + ", parameter value.");
                }
            }
            else
                app.logInfo("Missing port number parameter in application parameter file.");
        }
        // check if no file or invalid parameter contents
        if(port == -1) {
            // no other version running - find an empty port to use for TAPPAS
            // someone can manually delete the file but it's OK, this is just
            // to keep users from writing to the same project at the same time
            System.out.println("Checking for available port to use...");
            for(port = APP_PORT; port < (APP_PORT + APP_PORT_RANGE); port++) {
                try {
                    ss = new ServerSocket(port);
                    HashMap<String, String> hmParams = new HashMap<>();
                    hmParams.put(APP_PORT_PARAM, "" + port);
                    Utils.saveParams(hmParams, Paths.get(app.data.getAppDataBaseFolder(), APP_LOCK_FILE).toString());
                    ownPortFile = true;
                    run = true;
                    System.out.println("Found port " + port + " to use for lock.");
                    break;
                }
                catch(Exception e) { System.out.println("Port " + port + " in use: " + e.getMessage()); }
            }
        }
        return run;
    }
}
