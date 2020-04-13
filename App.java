/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class App {
    // minimum application requirements
    static final int MIN_CPUS = 4;
    static final double MIN_MEMORY = 1.2; //aprox 6Gb ram
    static final int MIN_DISKSPACE = 20;

    // MAX_ACTIVE_PROJECTS - project numbers have associated CSS defined in style.css
    // Note: Projects can be confusing for users as far as which data belongs to what project - more is not better
    public static final int MAX_ACTIVE_PROJECTS = 3;
    
    // log purge limits
    public static final int MAX_LOG_LINES = 2500;
    public static final int MAX_LOG_CHARS = MAX_LOG_LINES * 100;

    // base file paths
    public static final String macRscriptPath = "/usr/local/bin/Rscript";
    
    // class data
    int logLines = 0;
    public Locale locale;
    private String txtLog = "";
    private String pid = "";
    private Process rsVerProc = null;
    private Process rsGetInstallPackages = null;
    private Process rsDownloadPackages = null;
    private String rsVer = "";
    
    public boolean rsPackages = false;
    public boolean rsDownload = false;
    public String rsDownloadCheck = ""; 
    
    private String rscriptVer = "Rscript version: N/A\nWARNING: You must have Rscript installed and in your environment path.";
    private TaskHandler taskHandler = null;
    private final String appRunPath = System.getProperty("user.dir");
    private final AnnotationFiles annotationFiles;
    // annotation annotation files available in the server
    private ArrayList<AnnotationFiles.AnnotationFileInfo> lstServer = new ArrayList<>();
    // downloads versions of tappAS available in the server
    private ArrayList<String> lstDownloads = new ArrayList<>();
    
    // application objects - must pass 'this' App object if instantiating before App constructor done (value has not been saved yet)
    // WARN: create application logger before creating any other AppObjects
    public final Logger logger = new Logger(this, Paths.get(appRunPath, DataApp.APPLOG_NAME).toString(), Logger.Level.Debug);
    public final Controls ctls = new Controls(this);
    public final UserPrefs userPrefs = new UserPrefs(this);
    public final Resources resources = new Resources(this);
    public final Export export = new Export(this);
    public final DataApp data;
    public final Tabs tabs;
    public final AppController ctlr;
    
    // access functions
    public Locale getLocale() { return locale; }
    public TaskHandler getTaskHandler() { return taskHandler; }
    public ExecutorService getExecutor() { return taskHandler.getExecutor(); }

    static boolean noGOExpansionLimits = false;
    public static boolean isNoGOExpansionLimits() { return noGOExpansionLimits; }
    
    public App() {
        // set locale to US/UK, if neither, otherwise can run into issues with number formats in R packages
        // need to validate this with computer set to a foreign locale to make sure there are no issues
        // will log message if changed later on at initialization
        locale = Locale.getDefault();
        if(!locale.equals(Locale.UK) && !locale.equals(Locale.US)) {
            Locale.setDefault(Locale.US);
            locale = Locale.getDefault();
        }

        // instantiate required app objects
        tabs = new Tabs(this);
        data  = new DataApp(this);
        ctlr = new AppController(this);
        annotationFiles = new AnnotationFiles(this); 
    }
    public void initialize(TabPane tabPaneMain, TabPane tabPaneBottom) {
        // set debug flag if command line option set
        boolean logdbg = false;
        String[] args = Tappas.getArgs();
        if(args != null) {
            for(String arg : args) {
                switch (arg) {
                    case "-a":
                        logdbg = true;
                        System.out.println("Log debug messages enabled.");
                        break;
                    case "-ngel":
                        // temporary for development
                        noGOExpansionLimits = true;
                        System.out.println("GO expansion limits disabled.");
                        break;
                }
            }
        }
        
        // set logger level and log initial messages
        logger.setLevel(logdbg? Logger.Level.Debug : Logger.Level.Info);
        logDebug("Initializing application...");
        if(!locale.equals(Locale.UK) && !locale.equals(Locale.US))
            logDebug("Changed application locale to US/UK.");
        
        // setup application task handler
        taskHandler = new TaskHandler(this);
        taskHandler.initialize(Math.max(2, Runtime.getRuntime().availableProcessors()));

        // check for minimum application resources
        // will just warn user but will allow application to run
        // NOTE: we should not try to solve issues on computers that do not meet the minimum requirements
        String str = checkMinResources();
        if(!str.isEmpty()) {
            ctls.alertWarning("Application Minimum Requirements NOT Met", str);
            logWarning(str.replaceAll("\n\n", "\n"));
        }

        //Check if there is a new version of tappAS uploaded in our website
        getTappASVersionFiles();
        if(checkNewTappASVersion()){
            String msg = "There is a new version of tappAS on the website. Visit \"https://app.tappas.org/downloads\" to download it.";
            ctls.alertWarning("New version of tappAS found.", msg);
        }
        
        // initialize the application UI
        ctlr.initialize();
        if(tabs.initialize(tabPaneMain, tabPaneBottom)) {
            data.initialize();
            Platform.runLater(() -> {
                // get annotation files lists: local, already downloaded, and from server
                runServerList();
                
                // get process id in the background, not essential
                runPIDVersion();
                
                logDebug("Post-initialization setup...");
                ctlr.postInitialize();
                logDebug("Post-initialization setup completed.");

                // check to see if we got the Rscript path
                checkRScriptPath();
                /*if(rsVerProc == null && rsVer.isEmpty())
                {
                    // most mac laptop computers seem to install Rscript in macRscriptPath
                    // but not make it available in the Path for Java to find it
                    if(Utils.isMacOS() && Files.exists(Paths.get(macRscriptPath))) {
                        // should provide a way to clear it or change it via app menu selection
                        logInfo("Setting Rscript file path for Mac OS to " + macRscriptPath);
                        data.writeRscriptFilepath(macRscriptPath);
                        runRscriptVersion();
                    }
                    else {
                        Window wnd = Tappas.getWindow();
                        DlgRscriptFilepath dlg = new DlgRscriptFilepath(null, wnd);
                        HashMap<String, String> results = dlg.showAndWait(new HashMap<>());
                        if(results != null) {
                            String filepath = results.get(DlgRscriptFilepath.FILE_PARAM);
                            data.writeRscriptFilepath(filepath);
                            runRscriptVersion();
                        }
                    }
                }*/
            });
            logDebug("Application initialization completed.");
        }
    }

    public void checkRScriptPath(){
        // check to see if we got the Rscript path
        if(rsVerProc == null && rsVer.isEmpty())
        {
            // most mac laptop computers seem to install Rscript in macRscriptPath
            // but not make it available in the Path for Java to find it
            if(Utils.isMacOS() && Files.exists(Paths.get(macRscriptPath))) {
                // should provide a way to clear it or change it via app menu selection
                logInfo("Setting Rscript file path for Mac OS to " + macRscriptPath);
                data.writeRscriptFilepath(macRscriptPath);
                runRscriptVersion();
            }
            else {
                Window wnd = Tappas.getWindow();
                DlgRscriptFilepath dlg = new DlgRscriptFilepath(null, wnd);
                HashMap<String, String> results = dlg.showAndWait(new HashMap<>());
                if(results != null) {
                    String filepath = results.get(DlgRscriptFilepath.FILE_PARAM);
                    data.writeRscriptFilepath(filepath);
                    runRscriptVersion();
                }
            }
        }
    }

    public boolean shutdown() {
        boolean exit = true;
        if(taskHandler != null && taskHandler.getTasksCount() > 0) {
            String msg = "You have some application task(s) running\nIf you exit, they will not complete properly.\nDo you still want to exit the application?\n\n";
            if(ctls.alertConfirmation("Application Data Analysis Tasks Running", msg, ButtonType.CANCEL)) {
                //allowCancel = false;
                taskHandler.exit();
            }
            else
                exit = false;
        }
        return exit;
    }
    public String getAppInfo() {
        String info = Tappas.APP_NAME + " version: " + Tappas.APP_STRVER + "\n";
        info += "User: " + System.getProperty("user.name") + "\n";
        info += "ProcessID: " + pid + "\n";
        info += "JavaFX: ver " + System.getProperties().getProperty("javafx.runtime.version") + "\n";
        info += "OS: " + System.getProperty("os.name") + " ver " + System.getProperty("os.version") + "\n";
        info += "Architecture: " + System.getProperty("os.arch") + "\n";
        info += "Available processors, cores, for JVM: " + Runtime.getRuntime().availableProcessors() + "\n";
        info += "Available memory, for JVM: " + String.format("%.02f",(Runtime.getRuntime().maxMemory()/((double)(1024*1024*1024)))) + " GB" + "\n";
        info += rscriptVer + "\n";
        return info;
    }
    
    //Create the last version of tappAS the user had to check if re-create goAncestors.obo file
    //true for the same version and false if the usar run an older version of tappAS
    public boolean checkAppLastVersion(){
        return data.checkAppLastVersion();
    }
    
    public ArrayList<AnnotationFiles.AnnotationFileInfo> getAnnotationFilesList() {
        return annotationFiles.getAnnotationFilesList(lstServer);
    }
    
    public boolean checkNewTappASVersion() {
        boolean res = false;
        if(lstDownloads.isEmpty())
            return res;
        else{
            ArrayList<String> files = lstDownloads;

            for(String file : files){
                if(!file.startsWith("tappAS"))
                    continue;
                String[] fields = file.split("\\.");
                //tappAS . 0 . 99 . 01 . zip
                int major = Integer.parseInt(fields[1]);
                int minor = Integer.parseInt(fields[2]);
                int rev = Integer.parseInt(fields[3]);
                if(major > tappas.Tappas.APP_MAJOR_VER)
                    return true;
                else if(major == tappas.Tappas.APP_MAJOR_VER){
                    if(minor > tappas.Tappas.APP_MINOR_VER)
                        return true;
                    else if(minor == tappas.Tappas.APP_MINOR_VER){
                        if(rev > tappas.Tappas.APP_STRREV)
                            return true;
                    }
                }
            }
        }
        return res;
    }
    
    //
    // Project Functions
    //

    public int assignProjectNumber() {
        int projectNum = 1;
        boolean[] active = new boolean[MAX_ACTIVE_PROJECTS];
        for(int i = 0; i < MAX_ACTIVE_PROJECTS; i++)
            active[i] = false;
        ArrayList<TabBase> lstTabBases = tabs.getActiveProjectsTabBase();
        if(lstTabBases.size() < MAX_ACTIVE_PROJECTS) {
            for(TabBase tb : lstTabBases) {
                int num = tb.project.getProjectNumber();
                if(num >= 1 && num <= MAX_ACTIVE_PROJECTS)
                    active[num-1] = true;
            }
            for(int num = 1; num <= MAX_ACTIVE_PROJECTS; num++) {
                if(!active[num-1]) {
                    projectNum = num;
                    break;
                }
            }
        }
        return projectNum;
    }
    public ArrayList<Project.ProjectDef> getProjectsList() {
        ArrayList<Project.ProjectDef> lst = new ArrayList<>();
        File pfolder = new File(data.getAppProjectsFolder());
        FilenameFilter filter = (File dir, String name) -> name.startsWith(DataProject.getProjectNamePrefix()) && name.endsWith(DataProject.getProjectNameExt());        
        File[] files = pfolder.listFiles(filter);
        if(files != null) {
            for (File file : files) {
                HashMap<String, String> hm = new HashMap<>();
                DataProject.loadProjectDef(hm, Paths.get(file.getAbsolutePath()).toString());
                if(!hm.isEmpty()) {
                    // add to list if data format can be handled by this code
                    Project.ProjectDef pd = new Project.ProjectDef(hm);
                    if(DlgOpenProject.Params.isValidData(pd.dataVersion))
                        lst.add(pd);
                }
            }
        }
        Collections.sort(lst);
        return lst;
    }
    public ArrayList<String> getProjectIdsList() {
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<Project.ProjectDef> lst = getProjectsList();
        lst.forEach((def) -> {
            ids.add(def.id);
        });
        return ids;
    }
    public Project.ProjectDef getProjectDef(String id) {
        Project.ProjectDef def = null;
        HashMap<String, String> hm = new HashMap<>();
        DataProject.loadProjectDef(hm, data.getProjectFolder(id));
        if(!hm.isEmpty()){
            def = new Project.ProjectDef(hm);
        }
        return def;
    }
    public Project getProject(Project.ProjectDef def) {
        Project project = null;
        ArrayList<TabBase> lstTB = tabs.getActiveProjectsTabBase();
        for(TabBase tb : lstTB) {
            if(tb.project != null) {
                if(tb.project.getProjectId().equals(def.id)) {
                    project = tb.project;
                    break;
                }
            }
        }
        return project;
    }
    public boolean isProjectNameInUse(String name, String id) {
        boolean inuse = false;
        ArrayList<Project.ProjectDef> lst = getProjectsList();
        for(Project.ProjectDef def : lst) {
            if(!id.equals(def.id)) {
                if(name.toLowerCase().equals(def.name.toLowerCase())) {
                    inuse = true;
                    break;
                }
            }
        }
        return inuse;
    }
    
    //
    // Log Functions
    //
    public void setShowDebugMsg(boolean showdbg) { logger.setLevel(showdbg? Logger.Level.Debug : Logger.Level.Info); }
    public boolean isShowDebugMsg() { return (logger.getLevel() == Logger.Level.Debug); }
    public void logInfo(String msg) {
        logger.logMsg(Logger.Level.Info, msg);
    }
    public void logDebug(String msg) {
        logger.logMsg(Logger.Level.Debug, msg);
    }
    public void logWarning(String msg) {
        logger.logMsg(Logger.Level.Warning, msg);
        ctlr.showSeeAppLog(true);
    }
    public void logError(String msg) {
        logger.logMsg(Logger.Level.Error, msg);
        ctlr.showSeeAppLog(true);
    }
    public void logClear() {
        logger.logClear();
        ctlr.showSeeAppLog(false);
    }

    //
    // Internal application use only - called from Tabs
    //
    public void clearLogDisplay() {
        txtLog = "";
        logLines = 0;
        if(tabs != null)
            tabs.clearLogDisplay();
    }
    public void updateLogDisplay(String msg) {
        txtLog += msg;
        
        // limit how much text in log display
        int nchars = msg.length();
        if(nchars > MAX_LOG_CHARS) {
            if(tabs != null)
                tabs.clearLogDisplay();
            msg = txtLog.substring((int)(MAX_LOG_CHARS * 0.25));
            int pos = msg.indexOf("\n");
            if(pos != -1)
                msg = msg.substring(pos+1);
            txtLog = msg;
        }
        if(tabs != null)
            tabs.updateLogDisplay(msg);
    }    
    public String getAppLogContents() {
        return txtLog;
    }
    
    //
    // Custom Log File, e.g. data analysis log
    // TODO: should pass the level or use a logger directly
    //
    public String getLogFromFile(String filepath, int maxLines) {
        return Logger.getLogFromFile(filepath, maxLines);
    }
    public String getLogMsgLine(String msg, boolean first) {
        return logger.getLogMsgLine(msg, first);
    }    
    public void logMsgToFile(String msg, String filepath) {
        Logger.logMsgToFile(msg, filepath);
    }    

    //
    // Internal functions
    //
    
    // Download tappAS Versions names
    
    private void getTappASVersionFiles(){
    // downloads
        logDebug("Downloads Thread running");
        /* seems to be working properly -- must change to new file names */
        // get server annotation files
        lstDownloads = annotationFiles.getServerDownloadsFilesList(DataApp.refDownloadsUrl);
        if(lstDownloads.isEmpty())
            logWarning("Unable to get server downloads files list.\nWARNING: The application will not be able to check if there are a new version of tappAS available.");
        logDebug("ServerList thread exiting.");    
    }
    
    
    // check for minimum application requirements - return proper message if requirements not met
    private String checkMinResources() {
        String msg = "";
        int cpus = Runtime.getRuntime().availableProcessors();
        logInfo("Available CPUs for JVM: " + cpus);
        if(cpus < MIN_CPUS)
            msg += (msg.isEmpty()? "    " : "\n    ") + "Min CPUs: " + MIN_CPUS + ", available: " + cpus;
        if(!data.getAppDataFolder().isEmpty()) {
            File f = new File(data.getAppDataFolder());
            long fs = f.getFreeSpace() / (1024 * 1024 * 1024);
            if(fs < MIN_DISKSPACE)
                msg += (msg.isEmpty()? "    " : "\n    ") + "Min disk space: " + MIN_DISKSPACE + " GB, available: " + fs + " GB";
        }
        // max memory is determine by -Xmx or the JVM seems to determine the value based on a percentage of the available memory
        // for example: with 32GB available, it returned 6GB, with 8GB it returned 1.78GB (how much memory is free probably impacts the value)
        // Users can customize by passing values in the command line: java -Xmx4gb [-Xms4gb] -jar tappas.jar
        double mem = Runtime.getRuntime().maxMemory()/((double)(1024*1024*1024));
        logInfo("Available memory for JVM: " + String.format("%.02f", mem) + " GB");
        if(mem < MIN_MEMORY)
            msg += (msg.isEmpty()? "    " : "\n    ") + "Min memory for JVM: " + MIN_MEMORY + " GB, available: " + String.format("%.02f", mem) + " GB";
        if(!msg.isEmpty()) {
            msg = "Your computer does not meet the minimum application requirements:\n\n" + msg;
            msg += "\n\nThe application may run but you could experience significant delays and/or errors.";
        }
        if(mem<0.5){
            msg += "\n\nYou should run tappAS from console to get a better performance with following arguments:\n\n"
                    + "java -jar -XmxsM tappas.jar \n";
        }
        return msg;
    }
    
    //
    // Get Rscript Version Thread
    //
    public void runRscriptVersion() {
        // run Rscript to get version
        try {
            data.loadRscriptPath();
            rsVerProc = Runtime.getRuntime().exec(data.getRscriptFilepath() + " --version");
            Thread thread = new RscriptThread();
            thread.start();
        } catch(Exception e) {
            logWarning("Unable to get Rscript version: " + e.getMessage() + "\n    WARNING: You must have Rscript installed and in your environment path.");
        }
    }
    
    public void runGetInstalledPackages(String filepath) {
        // run Rscript to get version
        try {
            data.loadRscriptPath();
            rsGetInstallPackages = Runtime.getRuntime().exec(data.getRscriptFilepath() + " " + filepath);
            Thread thread = new RscriptPackages();
           // thread.start();
            thread.run();
        } catch(Exception e) {
            logWarning("Unable to get R packages: " + e.getMessage());
        }
    }
    
    public boolean runDownloadPackages(String filepath) {
        // run Rscript to get version
        boolean res = false;
        try {
            data.loadRscriptPath();
            rsDownloadPackages = Runtime.getRuntime().exec(data.getRscriptFilepath() + " " + filepath);
            RscriptDownloadPackages down = new RscriptDownloadPackages();
            res = down.run();
           // thread.start();
           // thread.run();
        } catch(Exception e) {
            logWarning("Unable to get R packages: " + e.getMessage());
        }
        return res;
    }
    
    public boolean runDownloadPackagesThread(String filepath) {
        // run Rscript to get version
        boolean res = false;
        try {
            data.loadRscriptPath();
            rsDownloadPackages = Runtime.getRuntime().exec(data.getRscriptFilepath() + " " + filepath);
            Thread thread = new RscriptDownloadPackagesThread();
            thread.run();
           // thread.start();
           // thread.run();
        } catch(Exception e) {
            logWarning("Unable to get R packages: " + e.getMessage());
        }
        return res;
    }
    
    private class RscriptPackages extends Thread {
        @Override
        public void run(){
            logDebug("Rscript checking packages...");
            try {
                String rsver = "";
                String line;
                // note: the version output is sent to the error stream
                BufferedReader input = new BufferedReader(new InputStreamReader(rsGetInstallPackages.getErrorStream()));
                while ((line = input.readLine()) != null)
                    rsver = line;
                try { input.close(); } catch(Exception e) { } 
                rsGetInstallPackages.waitFor();
                int ev = rsGetInstallPackages.exitValue();
                if(ev == 0 && !rsver.isEmpty()) {
                    String[] fields = rsver.split("\n");
                    if(fields.length == 1 && fields[0].equals("TRUE")) {
                        rsPackages = true;
                    }
                }
            } catch(Exception e) {
                logError("Unable to get Rscript version: " + e.getMessage() + "\nWARNING: You must have Rscript installed and in your environment path.");
            }
            logDebug("Rscript check packages thread exiting.");
            rsGetInstallPackages = null;
        }
    }
    
    private class RscriptDownloadPackagesThread extends Thread {
        @Override
        public void run(){
            logDebug("Rscript checking packages...");
            try {
                String rsver = "";
                String line;
                // note: the version output is sent to the error stream
                BufferedReader input = new BufferedReader(new InputStreamReader(rsDownloadPackages.getErrorStream()));
                while ((line = input.readLine()) != null){
                    if(line.contains("Error in ")){
                        rsDownload = false;
                        break;
                    }
                    rsver = line;
                }
                try { input.close(); } catch(Exception e) { } 
                rsDownloadPackages.waitFor();
                int ev = rsDownloadPackages.exitValue();
                if(ev == 0 && !rsver.isEmpty()) {
                    String[] fields = rsver.split("\n");
                    if(fields.length == 1 && fields[0].equals("TRUE")) {
                        rsDownload = true;
                    }
                }
            } catch(Exception e) {
                rsDownloadCheck = "error";
                logError("Unable to download R Packages: " + e.getMessage() + "\nWARNING: You must have Rscript installed and in your environment path.");
            }
            logDebug("Rscript download packages thread exiting.");
            rsDownloadPackages = null;
            rsDownloadCheck = "complete";
        }
    }
    
    private class RscriptDownloadPackages {
        public void RscriptDownloadPackages(){}
        public boolean run(){
            logDebug("Rscript checking packages...");
            try {
                String rsver = "";
                String line;
                // note: the version output is sent to the error stream
                BufferedReader input = new BufferedReader(new InputStreamReader(rsDownloadPackages.getErrorStream()));
                while ((line = input.readLine()) != null){
                    if(line.contains("Error:") || line.contains("Error in")){
                        rsDownload = false;
                        break;
                    }
                    rsver = line;
                }
                try { input.close(); } catch(Exception e) { } 
                rsDownloadPackages.waitFor();
                int ev = rsDownloadPackages.exitValue();
                if(ev == 0 && !rsver.isEmpty()) {
                    String[] fields = rsver.split("\n");
                    if(fields.length == 1 && fields[0].equals("TRUE")) {
                        rsDownload = true;
                    }
                }
            } catch(Exception e) {
                rsDownloadCheck = "error";
                logError("Unable to download R Packages: " + e.getMessage() + "\nWARNING: You must have Rscript installed and in your environment path.");
            }
            logDebug("Rscript download packages thread exiting.");
            rsDownloadPackages = null;
            rsDownloadCheck = "complete";
            return rsDownload;
        }
    }
    
    private class RscriptThread extends Thread {
        @Override
        public void run(){
            logDebug("Rscript Thread running");
            try {
                String rsver = "";
                String line;
                // note: the version output is sent to the error stream
                BufferedReader input = new BufferedReader(new InputStreamReader(rsVerProc.getErrorStream()));
                while ((line = input.readLine()) != null)
                    rsver = line;
                try { input.close(); } catch(Exception e) { } 
                rsVerProc.waitFor();
                int ev = rsVerProc.exitValue();
                if(ev == 0 && !rsver.isEmpty()) {
                    String[] fields = rsver.split("\n");
                    if(fields.length == 1) {
                        rsVer = rsver;
                        rscriptVer = rsver;
                    }
                }
            } catch(Exception e) {
                logError("Unable to get Rscript version: " + e.getMessage() + "\nWARNING: You must have Rscript installed and in your environment path.");
            }
            logDebug("Rscript version thread exiting.");
            rsVerProc = null;
        }
    }
    
    //
    // Get Application PID Thread
    //
    private void runPIDVersion() {
        // run script to get version if not Windows
        try {
            if(System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                pid = "N/A";
            }
            else {
                Thread thread = new PIDThread();
                thread.start();
            }
        } catch(Exception e) {
            logWarning("Unable to get application's PID.");
        }
    }
    private class PIDThread extends Thread {
        @Override
        public void run(){
            logDebug("PID thread running...");
            Path runScriptPath = null;
            try {
                String line;
                String outstr = "";
                // note: the version output is sent to the error stream
                runScriptPath = data.getTmpScriptFileFromResource("getPID.sh");
                Process p = Runtime.getRuntime().exec(runScriptPath.toString());
                if(p != null) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((line = input.readLine()) != null)
                        outstr += line + "\n";
                    try { input.close(); } catch(Exception e) { } 
                    p.waitFor();
                    int ev = p.exitValue();
                    if(ev == 0 && !outstr.isEmpty()) {
                        String[] fields = outstr.split("\n");
                        if(fields.length == 2) {
                            String[] subFields = fields[0].replaceAll("\\s+", " ").split(" ");
                            int idx = 0;
                            int idxPID = -1;
                            int idxPPID = -1;
                            for(String value : subFields) {
                                switch (value) {
                                    case "PID":
                                        idxPID = idx;
                                        break;
                                    case "PPID":
                                        idxPPID = idx;
                                        break;
                                }
                                idx++;
                            }
                            if(idxPID != -1 && idxPPID != -1) {
                                subFields = fields[1].replaceAll("\\s+", " ").split(" ");
                                logDebug("PID: " + subFields[idxPID] + ", PPID: " + subFields[idxPPID] + "\n");
                                pid = subFields[idxPPID];
                            }
                        }
                    }
                }
                else
                    logWarning("Unable to create process to get application's PID");
            } catch(Exception e) {
                logWarning("Unable to get aplication's PID: " + e.getMessage());
            }
            Utils.removeFile(runScriptPath);
            logDebug("PID thread exiting.");
        }
    }
    
    //
    // Get List of Server Annotation Files Thread
    //
    public void runServerList() {
        // run Rscript to get version
        try {
            Thread thread = new ServerListThread();
            thread.start();
        } catch(Exception e) {
            logWarning("Unable to get server annotation files list: " + e.getMessage() + "\n    WARNING: The application will not be able to download annotation files.");
        }
    }
    private class ServerListThread extends Thread {
        @Override
        public void run(){
            logDebug("ServerList Thread running");
            /* seems to be working properly -- must change to new file names */
            // get server annotation files
            lstServer = annotationFiles.getServerFilesList(DataApp.refDataUrl);
            if(lstServer.isEmpty())
                logWarning("Unable to get server annotation files list.\nWARNING: The application will not be able to download annotation files.");

            ArrayList<AnnotationFiles.AnnotationFileInfo> lst = annotationFiles.getAnnotationFilesList(lstServer);
            for(AnnotationFiles.AnnotationFileInfo afi : lst)
                System.out.println(afi.toString() + ", local: " + afi.local);
            logDebug("ServerList thread exiting.");
        }
    }
        
    //
    // Data Classes
    //
    public static class AppResource {
        String resName, fileName;
        AppResource(String resName, String fileName) {
            this.resName = resName;
            this.fileName = fileName;
        }
    }
}
