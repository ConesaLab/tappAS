/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TabPane;
import javafx.stage.Window;
import tappas.DataApp.DataType;
import tappas.DataApp.EnumData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class AppController extends AppObject {
    // limits
    static public final int MAX_RECENT_VALUES = 4;
    static public final int MAX_GSEARESULTS = 9;
    static public final int MAX_FEARESULTS = 9;

    // WARNING: AppController always has the active project in curProject but it can be null if there is no active project
    //          that is the case when the focus is on a non-project subtab such as the Overview, App Log, etc. in the AppInfo tab
    // Be aware that loading or selecting a non-project tab, e.g. Tabs.TAB_AI, will cause curProject to be cleared
    private Project curProject;
    public void setCurProject(Project project) { this.curProject = project; }
    
    // FXML controller
    public AppFXMLDocumentController fxdc;
    public void setFXDC(AppFXMLDocumentController fxdc) { this.fxdc = fxdc; }
    
    // class data
    private boolean loadingTabs = false;
    private boolean openingTabs = false;
    public boolean isLoadingTabs() { return loadingTabs; }
    public boolean isOpeningTabs() { return openingTabs; }
    private Tabs tabs;
    private String lastSubTabId = "";
    private String lastTabId = "";
    private String lastTabPaneId = "";
    private long lastTime = 0;

    public AppController(App app) {
        super(app);
    }
    public void initialize() {
        // Note: this is called before the application is fully loaded
    }
    public void postInitialize() {
        // application is loaded and scene and other app objects are available
        Tappas.getScene().focusOwnerProperty().addListener((observable, oldNode, newNode) -> focusChanged(newNode));
        loadInitialTabs();
        fxdc.setupSearch();
    }
    // load initial tabs - even if no project active
    public void loadInitialTabs() {
        loadingTabs = true;
        openingTabs = true;
        tabs = app.tabs;
        tabs.openTab(Tabs.TAB_AI, null, null);
        loadingTabs = false;
        tabs.selectTab(Tabs.TAB_AI);
        fxdc.showStartPage(getProjectsHTML(), Tappas.APP_STRVER);

        //First check if we have Rscript
        // check to see if we got the Rscript path
        app.checkRScriptPath();

        //Check R packages
        Path path = app.data.getTmpScriptFileFromResource("tappas_checkPackages.R");
        app.runGetInstalledPackages(path.toString());
        Utils.removeFile(path);
        
        if(!app.rsPackages){
            fxdc.showAppPaneInstalling(true);
            Window wnd = Tappas.getWindow();
            boolean download  = app.ctls.alertConfirmation("Do you want to download all R Packages?", "Some required R packages are missing. tappAS may not work properly. Would you like to download automatically? \n\n It could take a while.", null);
            
            if(download) {
                //app.ctls.alertInformation("Installing packages...", "Click on OK and wait until all packages will be installed.");
                Window wnd_ins = Tappas.getWindow();
                DlgInstallingPackages dlg_ins = new DlgInstallingPackages(null, wnd_ins);
                boolean installed = dlg_ins.showAndWait(app, fxdc);
                
                fxdc.showAppPaneInstalling(false);
                if(!installed)
                   app.logError("The installation didn't work correctly.");
            }else{
                fxdc.showAppPaneInstalling(false);
            }
        }
        
    }
    
    protected void runScript(TaskHandler.TaskInfo taskInfo, List<String> lst, String name, String logFilepath) {
        // run script
        try {
            ProcessBuilder pb = new ProcessBuilder(lst);
            System.out.println(lst);
            pb.redirectErrorStream(true);
            process = pb.start();
            taskInfo.process = process;
            app.logDebug(name + " process started, process id: " + process.toString());

            // monitor process output
            // could change to have PB send it to a log but still need to update screen
            Writer writer = null;
            try {
                String line, dspline;
                LocalDate date = LocalDate.now();
                LocalTime time = LocalTime.now();
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFilepath, true), "utf-8"));
                dspline = name + " script is running...\n";
                //outputLogLine(dspline);
                writer.write(dspline);
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int totalChars = 0;
                boolean dspinfo = true;
                while ((line = input.readLine()) != null) {
                    time = LocalTime.now();
                    dspline = time.toString() + " " + line + "\n";
                    writer.write(dspline);
                    totalChars += dspline.length();
                    if(totalChars > 100000){
                        if(dspinfo) {
                            //outputLogLine("Log display exceeded limit - no additional information will be displayed...");
                            dspinfo = false;
                        }
                    }
                }
                try { input.close(); } catch(Exception e) { }
                int ev = process.waitFor();
                dspline = time.toString() + " " + name + " script ended. Exit value: " + ev + "\n";
                writer.write(dspline);
                //outputLogLine(dspline);
            } catch(Exception e) {
                app.logError("Unable to capture " + name + " process output: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        } catch(Exception e) {
            app.logError("Unable to run " + name + " script: " + e.getMessage());
        }
    }
    
    public void showSeeAppLog(boolean show) {
        Platform.runLater(() -> {
            fxdc.showSeeAppLog(show);
        });
    }
    
    //
    // Project Functions
    //
    
    // start command request handler - user request originates in start HTML page
    public void processStartCmd(String cmd) {
        if(cmd.equals("new"))
            newProject();
        else if(cmd.equals("open"))
            openProject();
        else if(cmd.startsWith("recent:")) {
            String id = cmd.substring(7);
            Project.ProjectDef def = app.getProjectDef(id);
            if(def != null)
                openRecentProject(def);
        }
    }
    public String getProjectsHTML() {
        String html = "";
        ArrayList<Project.ProjectDef> lstRecents = getRecentProjects();
        int cnt = 0;
        if(!lstRecents.isEmpty()) {
            for(Project.ProjectDef def : lstRecents) {
                String name = def.name.length() > 30? def.name.substring(0, 27) + "..." : def.name;
                html += "<div style=\"padding-bottom:3px;\">";
                html += "<span style=\"color:slategray;\">&bull;&nbsp;</span><span class=\"projectText\" onclick=\"setCmdRequest('recent:" + def.id + "')\">" + name + "</span>";
                html += "</div>";
                if(++cnt > Math.min(MAX_RECENT_VALUES, 4))
                    break;
            }
        }
        
        return html;
    }
    // Note: only call after project base data has been loaded
    private void loadProjectTabs(Project project, boolean newdata) {
        // hide start page
        fxdc.hideStartPage();

        // minimal default tabs
        loadingTabs = false;
        openingTabs = true;
        tabs.openTab(Tabs.TAB_AI, null, null);
        
        // check if new project being created which does not have data yet
        if(project.data.hasInputData()) {
            loadingTabs = false;
            if(newdata) {
                openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYEXPMATRIX);
                openProjectDataSubTab(project, TabProjectData.Panels.EXPMATRIX);
            }
            openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYALL);
            openProjectDataSubTab(project, TabProjectData.Panels.TRANS);
            openingTabs = false;
            tabs.selectTab(Tabs.TAB_PROJECTDATA + project.getDef().id);
        }
        else
            tabs.selectTab(Tabs.TAB_AI);
        loadingTabs = false;
        openingTabs = false;
    }    
    public void newProject() {
        ArrayList<TabBase> lstTabBases = tabs.getActiveProjectsTabBase();
        if(lstTabBases.size() < App.MAX_ACTIVE_PROJECTS) {
            DlgInputData dlg = new DlgInputData(null, Tappas.getWindow());
            try {
                DlgInputData.Params results = dlg.showAndWait(true, null);
                if(results != null)
                    processInputData(null, results);
            }
            catch(IllegalArgumentException e){
                System.out.println(e.toString());
                app.logError("Error Opening a new Project");
            }
        }
        else
            app.ctls.alertInformation("Open Project", "You must close one of the opened projects.\nOnly " + App.MAX_ACTIVE_PROJECTS + " opened projects allowed.");
    }
    public void changeProjectName() {
        DlgRenameProject dlg = new DlgRenameProject(curProject, Tappas.getWindow());
        DlgRenameProject.Params results = dlg.showAndWait();
        if(results != null) {
            if(curProject.changeProjectName(results.name)) {
                setTitle(results.name);
                TabBase tabBase = app.tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                if(tabBase != null) {
                    HashMap<String, Object> hm = new HashMap<>();
                    hm.put("updateProjectName", "");
                    tabBase.processRequest(hm);
                }
                tabBase = app.tabs.getTabBase(Tabs.TAB_PROJECTDV + curProject.getDef().id);
                if(tabBase != null) {
                    HashMap<String, Object> hm = new HashMap<>();
                    hm.put("updateProjectName", "");
                    tabBase.processRequest(hm);
                }
            }
            else
                app.ctls.alertWarning("Change Project Name", "Unable to change project name.");
        }
    }
    public void loadInputData() {
// must check that project has no active tasks!!!
        DlgInputData dlg = new DlgInputData(curProject, Tappas.getWindow());
        DlgInputData.Params params = curProject.data.getParams();
        params.name = curProject.getProjectName();
        params.id = curProject.getProjectId();
        //hmArgs.put(DlgInputData.Params.NAME_PARAM, curProject.def.name);
        //hmArgs.put(DlgInputData.Params.ID_PARAM, curProject.def.id);
        DlgInputData.Params results = dlg.showAndWait(false, params);
        if(results != null) {
            // check for any existing analysis data
            boolean loadflg = true;
            if(curProject.data.analysis.hasAnyAnalysisData()) {
                if(app.ctls.alertConfirmation("Load Input Data", "All previous data and analysis results\nwill be cleared.\nDo you want to Proceeed?\n", null))
                    curProject.data.removeProjectData();
                else
                    loadflg = false;
            }
            if(loadflg)
                processInputData(curProject.getDef(), results);
        }
    }
    private void processInputData(Project.ProjectDef def, DlgInputData.Params results) {
        boolean newProject = (def == null);
        
        // always show log
        Project p = curProject;
        openAppLogSubTab();
        curProject = p;
        
        // check if using any of the application reference files
        if(results.useAppRef) {
            // check if we haven't already downloaded it
            if(!app.data.hasAppReferenceFiles(results.genus, results.species, results.refType.name(), results.refRelease)) {
                // get approval from user
                boolean fdok = false;
                if(app.ctls.alertConfirmation("Download Reference Data Files", "The application files selected need\nto be downloaded from the tappAS server.\nOK to download files?", null)) {
                    String path = app.data.getAppReferenceFileFolder(results.genus, results.species, results.refType.name(), results.refRelease);
                    Utils.removeAllFolderFiles(Paths.get(path), true);
                    DlgFileDownload.Params params = new DlgFileDownload.Params(app.data.getAppReferenceFileUrl(results.refFile), Paths.get(path, results.refFile).toString(), 0L, false);
                    DlgFileDownload fdlg = new DlgFileDownload(curProject, params, Tappas.getWindow());
                    HashMap<String, String> fdresults = fdlg.showAndWait(Paths.get(path, results.refFile).toString(), path);
                    fdok = Boolean.valueOf(fdresults.get(DlgFileDownload.Params.RESULT_PARAM));
                    if(fdok) {
                        if(Files.exists(Paths.get(path, DataApp.ANNOTATION_DB))) {
                            fdok = app.data.createReferencesOKFile(path);
                            if(!fdok)
                                app.logError("Unable to create internal application file.");
                        }
                        else {
                            fdok = false;
                            app.logError("Missing annotation database file from download.");
                        }
                        if(!fdok)
                            app.ctls.alertInformation("File Download Error", "Unable to decompress or process downloaded file.\nSee application log for details.");
                        
                        // always delete the compressed file to save disk space
                        Utils.removeFile(Paths.get(path, results.refFile));
                    }
                    else
                        app.ctls.alertInformation("File Download Error", "Unable to download selected file.\nSee application log for details.");
                }

                if(!fdok)
                    return;
            }
        }

        try {
            // check if this is a new project, create if so
            if(def == null) {
                // create project and open to get vars values
                String name = results.name;
                def = Project.createProject(app, name);
                curProject = _openProject(def);
                addRecentProject(def);
            }
            if(def != null) {
                // save project's input data parameters to include annotation path, must initialize afterwards
                System.out.println("results: " + results.toString());
                app.logInfo("Input Data dialog results: " + results);
                curProject.data.setParams(results);
                curProject.data.initialize();

                // process input data
                DlgProcessInputData pid = new DlgProcessInputData(curProject, results.getParams(), Tappas.getWindow());
                HashMap<String, String> pidResults = pid.showAndWait();
                if(pidResults != null && pidResults.containsKey("result") && pidResults.get("result").equals("OK")) {
                    // close and reopen project for change to take effect
                    closeProject(def);
                    // set new data flag to show expression matrix data and visualization subtabs
                    def.newdata = true;
                    openProject(def);
                }
                else {
                    app.ctls.alertError(newProject? "New Project" : "Load Input Data", newProject? "Unable to create project." : "Unable to load input data.\nProject will be removed.");

                    String folder = app.data.getProjectFolder(def.id);
                    closeProject(def);
                    if(!folder.isEmpty())
                        app.data.removeFolder(Paths.get(folder));

                    // remove from start HTML page menu if page is showing (only shown if no active projects)
                    if(tabs.getActiveProjects().isEmpty())
                        fxdc.showStartPage(getProjectsHTML(), Tappas.APP_STRVER);
                }
            }
        }
        catch(Exception e) {
            app.logError("Project " + (newProject? " creation" : "input data loading code exception: ") + e.getMessage());
        }
    }
    public void viewProjectProperties(Project.ProjectDef def) {
        // project may not be the active project tab
        Project pro = app.getProject(def);
        if(pro != null)
            openProjectDataSubTab(pro, TabProjectData.Panels.PROPS, null);
    }
    public void closeAllProjects() {
// revisit? make sure it works and remove comment (not sure why it is here)!!!
        tabs.closeAllTabs();
        loadInitialTabs();
    }
    public void closeProject(Project.ProjectDef def) {
        // close all project tabs - user can choose to cancel closing if task is running
// test for cancel!!!
        app.logInfo("Closing project '" + def.name + "'\n");
        tabs.closeProjectTabs(def);

        // clear project vars
        curProject = null;
        ArrayList<Project.ProjectDef> pdlst = tabs.getActiveProjects();
        if(pdlst.isEmpty()) {
            fxdc.showStartPage(getProjectsHTML(), Tappas.APP_STRVER);
            tabs.openTab(Tabs.TAB_AI, null, null);
        }
    }
    public void deleteProject() {
        try {
            Window wnd = Tappas.getWindow();
            DlgDeleteProject dlg = new DlgDeleteProject(curProject, wnd);
            DlgDeleteProject.Params results = dlg.showAndWait(new DlgDeleteProject.Params());
            if(results != null) {
                String msg = "Are you sure you want to \ndelete selected project?";
                if(results.hmProjects.size() > 1)   
                    msg = "Are you sure you want to \ndelete all " + results.hmProjects.size() + " selected projects?";
                if(app.ctls.alertConfirmation("Delete Project(s)", msg, null)) {
                    for(String name : results.hmProjects.keySet()) {
                        String path = app.data.getProjectFolder(results.hmProjects.get(name));
                        app.logInfo("Removing project '" + name + "'");
                        app.logInfo("Removing project folder '" + path + "'");
                        app.data.removeFolder(Paths.get(path));
                        // remove from start HTML page menu if page is showing (only shown if no active projects)
                        if(tabs.getActiveProjects().isEmpty())
                            fxdc.showStartPage(getProjectsHTML(), Tappas.APP_STRVER);
                    }
                }
            }
        } catch(Exception e) { logger.logWarning("Delete project - internal program error: " + e.getMessage()); }
    }
    public void openProject() {
        try {
            ArrayList<TabBase> lstTabBases = tabs.getActiveProjectsTabBase();
            if(lstTabBases.size() < App.MAX_ACTIVE_PROJECTS) {
                Window wnd = Tappas.getWindow();
                DlgOpenProject dlg = new DlgOpenProject(curProject, wnd);
                DlgOpenProject.Params results = dlg.showAndWait(new DlgOpenProject.Params());
                if(results != null) {
                    Project.ProjectDef def = new Project.ProjectDef(results.id, results.name);
                    openProject(def);
                }
            }
            else
                app.ctls.alertInformation("Open Project", "You must close one of the opened projects.\nOnly " + App.MAX_ACTIVE_PROJECTS + " opened projects allowed.");
        } catch(Exception e) { logger.logWarning("Open project - internal program error: " + e.getMessage()); }
    }
    public void openRecentProject(Project.ProjectDef def) {
        ArrayList<TabBase> lstTabBases = tabs.getActiveProjectsTabBase();
        if(lstTabBases.size() < App.MAX_ACTIVE_PROJECTS)
            openProject(def);
        else
            app.ctls.alertInformation("Open Recent Project", "You must close one of the opened projects.\nOnly " + App.MAX_ACTIVE_PROJECTS + " opened projects allowed.");
    }
    private void openProject(Project.ProjectDef def) {
        app.logInfo("Open project '" + def.name + "'");

        // see if this project is already opened
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDATA + def.id);
        if(tb == null) {
            runOpenProjectThread(def);
        }
        else {
            // set focus to project tab
            addRecentProject(def);
            tb.onSelect(true);
        }
    }
    public void projectDataLoaded(Project project) {
        fxdc.showAppPane(false);
        Tappas.getScene().setCursor(Cursor.DEFAULT);
        
        // now load project tabs - only time we should do this
        loadProjectTabs(project, project.getDef().newdata);
    }
    public void addRecentProject(Project.ProjectDef def) {
        ArrayList<Project.ProjectDef> lst = new ArrayList<>();
        
        // get existing projects
        UserPrefs prefs = app.userPrefs;
        for(int num = 1; num <= MAX_RECENT_VALUES; num++) {
            String rpid = prefs.getRecentProject(num);
            if(!rpid.trim().isEmpty()) {
                Project.ProjectDef predef = app.getProjectDef(rpid);
                if(predef != null && !predef.id.equals(def.id))
                    lst.add(predef);
            }
            if(lst.size() == (MAX_RECENT_VALUES - 1))
                break;
        }
        
        // add most current one to start of list
        lst.add(0, def);

        // update list
        for(int num = 1; num <= MAX_RECENT_VALUES; num++) {
            String rpid = "";
            if(lst.size() >= num)
                rpid = lst.get(num-1).id;
            prefs.setRecentProject(num, rpid);
        }
    }
    public void clearRecentProjects() {
        UserPrefs prefs = app.userPrefs;
        for(int num = 1; num <= MAX_RECENT_VALUES; num++)
            prefs.setRecentProject(num, "");
        ArrayList<Project.ProjectDef> lst = new ArrayList<>();
    }
    public ArrayList<Project.ProjectDef> getRecentProjects() {
        ArrayList<Project.ProjectDef> lst = new ArrayList<>();
        UserPrefs prefs = app.userPrefs;
        for(int num = 1; num <= MAX_RECENT_VALUES; num++) {
            String id = prefs.getRecentProject(num);
            if(!id.trim().isEmpty()) {
                Project.ProjectDef def = app.getProjectDef(id);
                if(def != null) {
                    if(DlgOpenProject.Params.isValidData(def.dataVersion))
                        lst.add(def);
                }
            }
        }
        return lst;
    }
    
    //
    // Menu Functions
    //
    
    public ArrayList<MenuItem> getStartMenuItems() {
        ArrayList<MenuItem> lstItems = new ArrayList<>();
        ArrayList<Project.ProjectDef> lstProjects = app.getProjectsList();
        ArrayList<Project.ProjectDef> lstRecents = getRecentProjects();
        MenuItem miNew = new MenuItem("New Project...");
        miNew.setOnAction((event) -> { newProject();});
        lstItems.add(miNew);
        lstItems.add(new SeparatorMenuItem());
        MenuItem miOpen = new MenuItem("Open Project...");
        miOpen.setDisable(lstProjects.isEmpty());
        miOpen.setOnAction((event) -> { openProject();});
        lstItems.add(miOpen);
        Menu mr = new Menu("Recent Projects");
        miOpen.setDisable(lstRecents.isEmpty());
        for(Project.ProjectDef pd : lstRecents) {
            MenuItem item = new MenuItem("'" + pd.name + "'");
            item.setOnAction((event) -> { openRecentProject(pd);});
            mr.getItems().add(item);
        }
        lstItems.add(mr);
        return lstItems;
    }
    public ArrayList<MenuItem> getProjectsMenuItems() {
        ArrayList<MenuItem> lstItems = getStartMenuItems();
        lstItems.add(new SeparatorMenuItem());

        ArrayList<Project.ProjectDef> pdlst = tabs.getActiveProjects();
        Menu mProps = new Menu("View Project Properties");
        mProps.setDisable(pdlst.isEmpty());
        for(Project.ProjectDef pd : pdlst) {
            MenuItem item = new MenuItem("'" + pd.name + "'");
            item.setOnAction((event) -> { viewProjectProperties(pd);});
            mProps.getItems().add(item);
        }
        lstItems.add(mProps);

        lstItems.add(new SeparatorMenuItem());
        Menu mClose = new Menu("Close Project");
        mClose.setDisable(pdlst.isEmpty());
        for(Project.ProjectDef pd : pdlst) {
            MenuItem item = new MenuItem("'" + pd.name + "'");
            item.setOnAction((event) -> { closeProject(pd);});
            mClose.getItems().add(item);
        }
        lstItems.add(mClose);
        MenuItem miCloseAll = new MenuItem("Close All Projects");
        miCloseAll.setDisable(pdlst.isEmpty());
        miCloseAll.setOnAction((event) -> { closeAllProjects();});
        lstItems.add(miCloseAll);

        ArrayList<Project.ProjectDef> lstProjects = app.getProjectsList();
        lstItems.add(new SeparatorMenuItem());
        MenuItem miDelete = new MenuItem("Delete Project(s)...");
        miDelete.setDisable(lstProjects.isEmpty());
        miDelete.setOnAction((event) -> { deleteProject();});
        lstItems.add(miDelete);
        return lstItems;
    }
    public DAMenuFlags getDAMenuFlags() {
        DAMenuFlags flags = new DAMenuFlags();
        flags.runDEA = true;
        flags.runDIU = true;
        flags.runDFI = true;
        flags.runDPA = true;
        flags.runUTRL = true;
        flags.hasDEA_Trans = curProject.data.analysis.hasDEAData(DataApp.DataType.TRANS);
        flags.hasDEA_Protein = curProject.data.analysis.hasDEAData(DataApp.DataType.PROTEIN);
        flags.hasDEA_Gene = curProject.data.analysis.hasDEAData(DataApp.DataType.GENE);
        flags.hasDEA = flags.hasDEA_Trans || flags.hasDEA_Protein || flags.hasDEA_Gene;
        flags.hasDIU_Trans = curProject.data.analysis.hasDIUDataTrans();
        flags.hasDIU_Protein = curProject.data.analysis.hasDIUDataProtein();
        flags.hasDIU = flags.hasDIU_Trans || flags.hasDIU_Protein;
        flags.hasDFI = curProject.data.analysis.hasAnyDFIData();
        flags.hasDPA = curProject.data.analysis.hasDPAData();
        flags.hasUTRL = curProject.data.analysis.hasUTRLData();
        flags.hasAny = flags.hasDEA || flags.hasDIU || flags.hasDFI || flags.hasDPA || flags.hasUTRL;
        flags.multipleTimes = curProject.data.isMultipleTimeSeriesExpType();
        return flags;
    }
    public EAMenuFlags getEAMenuFlags() {
        EAMenuFlags flags = new EAMenuFlags();
        flags.runGSEA = true;
        flags.runFEA = true;
        flags.hasFEA = curProject.data.analysis.hasAnyFEAData();
        flags.hasGSEA = curProject.data.analysis.hasAnyGSEAData();
        flags.hasAny = flags.hasFEA || flags.hasGSEA;
        
        flags.lstGSEAViewItems = new ArrayList<>();
        flags.lstGSEAClearItems = new ArrayList<>();
        ArrayList<DataApp.EnumData> lstItems = curProject.data.analysis.getGSEAResultsList();
        for(DataApp.EnumData ed : lstItems) {
            MenuItem mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { openProjectDataSubTabId(curProject, TabProjectData.Panels.STATSGSEA, ed.id);});
            flags.lstGSEAViewItems.add(mi);
            mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { clearGSEA(ed.name, ed.id, true); });
            flags.lstGSEAClearItems.add(mi);
        }

        flags.lstFEAViewItems = new ArrayList<>();
        flags.lstFEAClearItems = new ArrayList<>();
        lstItems = curProject.data.analysis.getFEAResultsList();
        for(DataApp.EnumData ed : lstItems) {
            MenuItem mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { openProjectDataSubTabId(curProject, TabProjectData.Panels.STATSFEA, ed.id);});
            flags.lstFEAViewItems.add(mi);
            mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { clearFEA(ed.name, ed.id, true); });
            flags.lstFEAClearItems.add(mi);
        }

        return flags;
    }
    public MAMenuFlags getMAMenuFlags() {
        MAMenuFlags flags = new MAMenuFlags();
        flags.runDiversity = true;
        flags.hasFDA = curProject.data.analysis.hasAnyFDAData();
        flags.viewDiversity = curProject.data.analysis.hasAnyFDAData();
        flags.hasTwoData = curProject.data.analysis.hasTwoFDAIdData();

        flags.lstFDAViewItems = new ArrayList<>();
        flags.lstFDAClearItems = new ArrayList<>();
        ArrayList<DataApp.EnumData> lstItems = curProject.data.analysis.getFDAResultsList();
        for(DataApp.EnumData ed : lstItems) {
            //results
            MenuItem mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { openProjectDataSubTabId(curProject, TabProjectData.Panels.STATSFDA, ed.id);});
            flags.lstFDAViewItems.add(mi);
            //clear
            mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { clearFDA(ed.name, ed.id, true); });
            flags.lstFDAClearItems.add(mi);
        }
        
        return flags;
    }
    public FAMenuFlags getFAMenuFlags() {
        FAMenuFlags flags = new FAMenuFlags();
        flags.runDFI = true;
        flags.hasDFI = curProject.data.analysis.hasAnyDFIData();
        
        flags.lstDFIViewItems = new ArrayList<>();
        flags.lstDFISummaryItems = new ArrayList<>();
        flags.lstCoDFIViewItems = new ArrayList<>();
        flags.lstDFIClearItems = new ArrayList<>();
        ArrayList<DataApp.EnumData> lstItems = curProject.data.analysis.getDFIResultsList();
        for(DataApp.EnumData ed : lstItems) {
            //results
            MenuItem mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { openProjectDataSubTabId(curProject, TabProjectData.Panels.STATSDFI, ed.id);});
            flags.lstDFIViewItems.add(mi);
            //summary
            mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { openProjectDataSubTabId(curProject, TabProjectData.Panels.FIRESULTSSUMMARY, ed.id);});
            flags.lstDFISummaryItems.add(mi);
            //coDFI
            mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { openProjectDataSubTabId(curProject, TabProjectData.Panels.FIASSOCIATION, ed.id);});
            flags.lstCoDFIViewItems.add(mi);
            //clear
            mi = new MenuItem(ed.name);
            mi.setOnAction((event) -> { clearDFI(ed.id, true); });
            flags.lstDFIClearItems.add(mi);
        }
        
        // DPA
        flags.runDPA = true;
        flags.hasDPA = curProject.data.analysis.hasDPAData();

        // UTR Lengthrning
        flags.runUTRL = true;
        flags.hasUTRL = curProject.data.analysis.hasUTRLData();

        // BOTH
        flags.hasAny = flags.hasDPA || flags.hasDFI || flags.hasUTRL;
        return flags;
    }
    public GraphsMenuFlags getGraphsMenuFlags() {
        GraphsMenuFlags flags = new GraphsMenuFlags();
        if(curProject != null && curProject.data != null) {
            // enable based on data availability
            flags.hasData = curProject.data.hasInputData();
            flags.hasDA = curProject.data.analysis.hasAnyDAData();
            flags.hasEA = curProject.data.analysis.hasAnyEAData();
            // tools are always enabled - change if we come up with a reason not to do so
            flags.toolVennDiag = true;
        }
        return flags;
    }
    // a focus change took place - set the top level menus enable/disable state
    public void updateTopLevelMenus(String tabId, Boolean selTabId) {
        TabBase tabBase = tabs.getTabBase(tabId);
        if(tabBase != null) {
            // update pipe vars and app window title
            String projectName = "";
            if(tabBase.project != null)
                projectName = tabBase.project.getDef().name;
            setCurProject(tabBase.project);
            setTitle(projectName);
            
            // check if project tab, data or graphs, enable toolbar button menus accordingly
            System.out.println("updateTopLevelMenus: " + tabId);
            boolean tabData = tabId.startsWith(Tabs.TAB_PROJECTDATA);
            boolean tabDataViz = tabId.startsWith(Tabs.TAB_PROJECTDV);
            boolean projectTab = tabData || tabDataViz;
            fxdc.disableTopMenu("Project_Data", !projectTab);
            fxdc.disableTopMenu("DA", !projectTab);
            fxdc.disableTopMenu("EA", !projectTab);
            fxdc.disableTopMenu("FDA", !projectTab);
            fxdc.disableTopMenu("FA", !projectTab);
            fxdc.disableTopMenu("Project_DataViz", !projectTab);
            // call tab select function to make sure focus is given to main table, if any, to enable search controls
            // the changing of the focused color for the sub tab, skyblue, is also handled there
            // must call even if selTabId is false otherwise default table/node may not get the focus automatically
            if(!isLoadingTabs()) {
                //System.out.println("updateTopLevelMenus calling onSelect(true) for " + tabBase.tabId);
                tabBase.onSelect(true);
            }
        }
    }

    //
    // Tab and SubTab Functions
    //
    
    // WARNING: Selecting a non-project subtab causes the curProject variable to be cleared
    public void openAppLogSubTab() {
        HashMap<String, Object> hmArgs = new HashMap<>();
        hmArgs.put("panels", TabAppInfo.Panels.LOG.name());
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_AI);
        if(tb == null)
            tabs.openTab(Tabs.TAB_AI, null, hmArgs);
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("panels", hmArgs);
            tb.processRequest(hm);
        }
    }
    public void showOverviewSection(String url) {
        HashMap<String, Object> hmArgs = new HashMap<>();
        hmArgs.put("panels", TabAppInfo.Panels.OVERVIEW.name());
        hmArgs.put("url", url);
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_AI);
        if(tb == null)
            tabs.openTab(Tabs.TAB_AI, null, hmArgs);
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("panels", hmArgs);
            tb.processRequest(hm);
        }
    }
    public void openProjectDataSubTabId(Project project, TabProjectData.Panels panel, String id) {
        openProjectDataSubTabId(project, panel, id, null);
    }
    public void openProjectDataSubTabId(Project project, TabProjectData.Panels panel, String id, HashMap<String, Object> args) {
        if(args == null)
            args = new HashMap<>();
        if(panel != null)
            args.put("panels", panel.name());
        args.put("id", id);
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDATA + project.getDef().id);
        if(tb == null)
            tabs.openTab(Tabs.TAB_PROJECTDATA + project.getDef().id, project, args);
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("panels", args);
            tb.processRequest(hm);
        }
    }
    public void closeProjectDataVizSubTab(Project project, String panelId, boolean startWith) {
        // close requested panel if project data viz tab is opened
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDV + project.getDef().id);
        if(tb != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put(startWith? "closePanelsStartWith" : "closePanels", panelId);
            tb.processRequest(hm);
        }
    }
    public void closeProjectDataSubTab(Project project, String panelId) {
        // close requested panel if project data tab is opened
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDATA + project.getDef().id);
        if(tb != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("closePanels", panelId);
            tb.processRequest(hm);
        }
    }
    public void openProjectDataSubTab(TabProjectData.Panels panel) {
        openProjectDataSubTab(curProject, panel, null);
    }
    public void openProjectDataSubTab(Project project, TabProjectData.Panels panel) {
        openProjectDataSubTab(project, panel, null);
    }
    public void openProjectDataSubTab(Project project, TabProjectData.Panels panel, HashMap<String, Object> args) {
        if(args == null)
            args = new HashMap<>();
        if(panel != null)
            args.put("panels", panel.name());
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDATA + project.getDef().id);
        if(tb == null)
            tabs.openTab(Tabs.TAB_PROJECTDATA + project.getDef().id, project, args);
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("panels", args);
            tb.processRequest(hm);
        }
    }
    public void openProjectDataVizSubTab(TabProjectDataViz.Panels panel) {
        openProjectDataVizSubTab(curProject, panel);
    }
    public void openProjectDataVizSubTab(Project project, TabProjectDataViz.Panels panel) {
        HashMap<String, Object> args = new HashMap<>();
        if(panel != null)
            args.put("panels", panel.name());
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDV + project.getDef().id);
        if(tb == null)
            tabs.openTab(Tabs.TAB_PROJECTDV + project.getDef().id, project, args);
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("panels", args);
            tb.processRequest(hm);
        }
    }
    public void openProjectDataVizSubTab(Project project, TabProjectDataViz.Panels panel, HashMap<String, Object> args) {
        if(panel != null)
            args.put("panels", panel.name());
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDV + project.getDef().id);
        if(tb == null)
            tabs.openTab(Tabs.TAB_PROJECTDV + project.getDef().id, project, args);
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("panels", args);
            tb.processRequest(hm);
        }
    }
    public void openProjectDataVizSubTabId(Project project, TabProjectDataViz.Panels panel, String id) {
        openProjectDataVizSubTabId(project, panel, id, null);
    }
    public void openProjectDataVizSubTabId(Project project, TabProjectDataViz.Panels panel, String id, HashMap<String, Object> args) {
        if(args == null)
            args = new HashMap<>();
        if(panel != null)
            args.put("panels", panel.name());
        args.put("id", id);
        TabBase tb = app.tabs.getTabBase(Tabs.TAB_PROJECTDV + project.getDef().id);
        if(tb == null)
            tabs.openTab(Tabs.TAB_PROJECTDV + project.getDef().id, project, args);
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("panels", args);
            tb.processRequest(hm);
        }
    }
    public void viewFEAClusters(String id) {
        openProjectDataSubTabId(curProject, TabProjectData.Panels.CLUSTERSFEA, id, new HashMap());
    }

    public void viewGSEAClusters(String id) {
        openProjectDataSubTabId(curProject, TabProjectData.Panels.CLUSTERSGSEA, id, new HashMap());
    }
    
    //
    // Analysis Dialog Functions
    //
    
    public void runDEAnalysis(DataType dataType) {
        try {
            DlgDEAnalysis dlg = new DlgDEAnalysis(curProject, Tappas.getWindow());
            DlgDEAnalysis.Params dfltValues = DlgDEAnalysis.Params.load(curProject.data.analysis.getDEAParamsFilepath(dataType == null? DataType.GENE : dataType), curProject);
            DlgDEAnalysis.Params results = dlg.showAndWait(dfltValues);
            if(results != null) {
                results.save(curProject.data.analysis.getDEAParamsFilepath(results.dataType));
                TabProjectData.Panels panel;
                switch(results.dataType) {
                    case PROTEIN:
                        panel = TabProjectData.Panels.STATSDEAPROT;
                        break;
                    case GENE:
                        panel = TabProjectData.Panels.STATSDEAGENE;
                        break;
                    case TRANS:
                    default:
                        panel = TabProjectData.Panels.STATSDEATRANS;
                        break;
                }
                
                // check if analysis already running
                TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                boolean taskRunning = false;
                if(sti != null && sti.subTabBase != null)
                    taskRunning = sti.subTabBase.isServiceTaskRunning();
                if(!taskRunning) {
                    // close data panel
                    closeProjectDataSubTab(curProject, panel.name());
                    // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                    String subTabs = SubTabDEAResults.getAssociatedDVSubTabs(results.dataType);
                    closeProjectDataVizSubTab(curProject, subTabs, true);

                    // clear data and run analysis by opening subtab w/o data
                    curProject.data.analysis.clearDataDEA(results.dataType, false);
                    openProjectDataSubTab(curProject, panel, null);
                }
                else
                    app.ctls.alertInformation("DEAnalysis", "Selected analysis type is already running");
            }
        } catch(Exception e) {
            app.logError("Unable to run DEAnalysis: " + e.getMessage());
        }
    }

    public void runDIUnalysis(DataType dataType) {
        try {
            DlgDIUAnalysis dlg = new DlgDIUAnalysis(curProject, Tappas.getWindow());
            DlgDIUAnalysis.Params dfltValues = DlgDIUAnalysis.Params.load(curProject.data.analysis.getDIUParamsFilepath(dataType == null? DataType.TRANS : dataType), curProject);
            DlgDIUAnalysis.Params results = dlg.showAndWait(dfltValues);
            if(results != null) {
                results.save(curProject.data.analysis.getDIUParamsFilepath(results.dataType), curProject);
                TabProjectData.Panels panel;
                switch(results.dataType) {
                    case PROTEIN:
                        panel = TabProjectData.Panels.STATSDIUPROT;
                        break;
                    case TRANS:
                    default:
                        panel = TabProjectData.Panels.STATSDIUTRANS;
                        break;
                }
                
                // check if analysis already running
                TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                boolean taskRunning = false;
                if(sti != null && sti.subTabBase != null)
                    taskRunning = sti.subTabBase.isServiceTaskRunning();
                if(!taskRunning) {
                    // close data panel
                    closeProjectDataSubTab(curProject, panel.name());

                    // close all associated data visualization subtabs since underlying analysis data will be deleted
                    String subTabs = SubTabDIUResults.getAssociatedDVSubTabs(results.dataType);
                    closeProjectDataVizSubTab(curProject, subTabs, true);

                    // clear data and run analysis by opening subtab w/o data
                    curProject.data.analysis.clearDataDIU(results.dataType, false);
                    openProjectDataSubTab(curProject, panel, null);
                }
                else
                    app.ctls.alertInformation("DIUnalysis", "Selected analysis type is already running");
            }
        } catch(Exception e) {
            app.logError("Unable to run DIUnalysis: " + e.getMessage());
        }
    }
    public DlgExportData.Params getExportDataParams(DlgExportData.Config config, DlgExportData.Params prms, ArrayList<EnumData> lstOtherSelections) {
        DlgExportData.Params results = null;
        try {
            Window wnd = Tappas.getWindow();
            DlgExportData dlg = new DlgExportData(curProject, wnd);
            results = dlg.showAndWait(config, prms, lstOtherSelections);
        } catch(Exception e) {
            app.logError("Unable to get export data parameters: " + e.getMessage());
        }
        return results;
    }
    public void runFDAnalysis(String id) {
        try {
            DlgFDAnalysis dlg = new DlgFDAnalysis(curProject, Tappas.getWindow());
            DlgFDAnalysis.Params dfltParams;
            if(!id.isEmpty())
                dfltParams = DlgFDAnalysis.Params.load(curProject.data.analysis.getFDAParamsFilepath(id));
            else
                dfltParams = new DlgFDAnalysis.Params();
            
            dfltParams.paramId = id;
            DlgFDAnalysis.Params results = dlg.showAndWait(dfltParams);
            
            if(results != null) {
                
                // BY AnalysisID
                ArrayList<String> lst = new ArrayList<>();
                ArrayList<DataApp.EnumData> lstFDAParams = curProject.data.analysis.getFDAParamsList();
                for(DataApp.EnumData data : lstFDAParams)
                    lst.add(data.id);
                if(!results.hmFeatures.isEmpty()) {
                    if(id.isEmpty()) {
                        // make sure we generate a non-existing id
                        Random rand = new Random();
                        LocalDate date = LocalDate.now();
                        LocalTime time = LocalTime.now();
                        do {
                            String idstr = results.name.toLowerCase() + rand.nextInt() + date.toString() + time.toString();
                            id = Utils.getIdHashCode(idstr);
                        } while(lst.contains(id));
                    }
                    // -----
                    TabProjectData.Panels panel = TabProjectData.Panels.STATSFDA;
                    String panelName = TabProjectData.Panels.STATSFDA + id;
                    
                    // check if analysis already running
                    TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                    SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                    boolean taskRunning = false;
                    if(sti != null && sti.subTabBase != null)
                        taskRunning = sti.subTabBase.isServiceTaskRunning();
                    if(!taskRunning) {
                        results.save(curProject.data.analysis.getFDAParamsFilepath(id));
                        closeProjectDataSubTab(curProject, panelName);
                        //curProject.data.analysis.setFDAParams(results.getParams(), id);
                        //closeProjectDataSubTab(curProject, panel.name());

                        // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                        String subTabs = SubTabFDAResults.getAssociatedDVSubTabs(id);
                        //closeProjectDataVizSubTab(curProject, subTabs, true);

                        // clear data and run analysis by opening subtab w/o data
                        curProject.data.analysis.clearDataFDA(id, false);
                        openProjectDataSubTabId(curProject, panel, id, null);
                    }
                    else
                        app.ctls.alertInformation("FDAnalysis", "Selected analysis type is already running");
                }
            }
        } catch(Exception e) {
            app.logError("Unable to run FDAnalysis: " + e.getMessage());
        }
    }

    public void runFDACombinedResults(String id) {
        try {
            DlgFDAnalysisCombinedResults dlg = new DlgFDAnalysisCombinedResults(curProject, Tappas.getWindow());
            DlgFDAnalysisCombinedResults.Params dfltParams;
            if(!id.isEmpty())
                dfltParams = DlgFDAnalysisCombinedResults.Params.load(curProject.data.analysis.getFDAParamsCombinedResultsFilepath());
            else
                dfltParams = new DlgFDAnalysisCombinedResults.Params(curProject);

            DlgFDAnalysisCombinedResults.Params results = dlg.showAndWait(dfltParams);
            if(results != null) {
                // -----
                TabProjectData.Panels panel = TabProjectData.Panels.FAIDCOMBINED;
                String panelName = TabProjectData.Panels.FAIDCOMBINED.name();

                // check if analysis already running
                TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                boolean taskRunning = false;
                if(sti != null && sti.subTabBase != null)
                    taskRunning = sti.subTabBase.isServiceTaskRunning();
                if(!taskRunning) {
                    results.save(curProject.data.analysis.getFDAParamsCombinedResultsFilepath());
                    closeProjectDataSubTab(curProject, panelName);
                    //curProject.data.analysis.setFDAParams(results.getParams(), id);
                    //closeProjectDataSubTab(curProject, panel.name());

                    // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                    String subTabs = SubTabFACombinedResults.getAssociatedDVSubTabs(id);
                    closeProjectDataVizSubTab(curProject, subTabs, true);

                    // clear data and run analysis by opening subtab w/o data
                    //curProject.data.analysis.clearDataFDA(id, false);
                    openProjectDataSubTab(curProject, panel, null);
                }
                else
                    app.ctls.alertInformation("FDAnalysis Combined Results", "Selected analysis type is already running");
            }
        } catch(Exception e) {
            app.logError("Unable to run FDAnalysis: " + e.getMessage());
        }
    }
    
    public void runDFIAnalysis(String id) {
        try {
            DlgDFIAnalysis dlg = new DlgDFIAnalysis(curProject, Tappas.getWindow());
            DlgDFIAnalysis.Params dfltParams;
            
            if(!id.isEmpty())
                dfltParams = DlgDFIAnalysis.Params.load(curProject.data.analysis.getDFIParamsFilepath(id), curProject);
            else
                dfltParams = new DlgDFIAnalysis.Params(curProject);
            
            dfltParams.paramId = id;
            DlgDFIAnalysis.Params results = dlg.showAndWait(dfltParams);
            if(results != null) {
                // BY AnalysisID
                ArrayList<String> lst = new ArrayList<>();
                ArrayList<DataApp.EnumData> lstDFIParams = curProject.data.analysis.getDFIParamsList();
                for(DataApp.EnumData data : lstDFIParams)
                    lst.add(data.id);
                if(!results.hmFeatures.isEmpty()) {
                    if(id.isEmpty()) {
                        // make sure we generate a non-existing id
                        Random rand = new Random();
                        LocalDate date = LocalDate.now();
                        LocalTime time = LocalTime.now();
                        do {
                            String idstr = results.name.toLowerCase() + rand.nextInt() + date.toString() + time.toString();
                            id = Utils.getIdHashCode(idstr);
                        } while(lst.contains(id));
                    }
                    // -----
                    TabProjectData.Panels panel = TabProjectData.Panels.STATSDFI;
                    String panelName = TabProjectData.Panels.STATSDFI + id;
                    String panelNameAssociation = TabProjectData.Panels.FIASSOCIATION + id;
                    String panelNameSummary = TabProjectData.Panels.FIRESULTSSUMMARY + id;

                    // check if analysis already running
                    TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                    SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                    boolean taskRunning = false;
                    if(sti != null && sti.subTabBase != null)
                        taskRunning = sti.subTabBase.isServiceTaskRunning();
                    if(!taskRunning) {
                        results.save(curProject.data.analysis.getDFIParamsFilepath(id), curProject);
                        closeProjectDataSubTab(curProject, panelName);
                        // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                        closeProjectDataSubTab(curProject, panelNameAssociation);
                        closeProjectDataSubTab(curProject, panelNameSummary);

                        //String subTabs = SubTabDFIResults.getAssociatedDVSubTabs();
                        //closeProjectDataVizSubTab(curProject, subTabs, true);

                        // clear data and run analysis by opening subtab w/o data
                        curProject.data.analysis.clearDataDFI(id, false);
                        openProjectDataSubTabId(curProject, panel, id, null);
                    }
                else
                    app.ctls.alertInformation("DFI Analysis", "Selected analysis type is already running");
                }
            }
        } catch(Exception e) {
            app.logError("Unable to run DFI Analysis: " + e.getMessage());
        }
    }
    
    //DPA
    public void runDPAnalysis() {
        try {
            DlgDPAnalysis dlg = new DlgDPAnalysis(curProject, Tappas.getWindow());
            DlgDPAnalysis.Params results = dlg.showAndWait(DlgDPAnalysis.Params.load(curProject.data.analysis.getDPAParamsFilepath(), curProject));
            if(results != null) {
                TabProjectData.Panels panel = TabProjectData.Panels.STATSDPA;

                // check if analysis already running
                TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                boolean taskRunning = false;
                if(sti != null && sti.subTabBase != null)
                    taskRunning = sti.subTabBase.isServiceTaskRunning();
                if(!taskRunning) {
                    results.save(curProject.data.analysis.getDPAParamsFilepath(), curProject);
                    closeProjectDataSubTab(curProject, panel.name());
                    //closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDPAASSOCIATION.name());
                    //closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDPARESULTSSUMMARY.name());

                    // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                    String subTabs = SubTabDPAResults.getAssociatedDVSubTabs();
                    closeProjectDataVizSubTab(curProject, subTabs, true);

                    // clear data and run analysis by opening subtab w/o data
                    curProject.data.analysis.clearDataDPA(false);
                    openProjectDataSubTab(curProject, panel, null);
                }
                else
                    app.ctls.alertInformation("DPAnalysis", "Selected analysis type is already running");
            }
        } catch(Exception e) {
            app.logError("Unable to run DPAnalysis: " + e.getMessage());
        }
    }

    public void runUTRLAnalysis() {
        try {
            DlgUTRLAnalysis dlg = new DlgUTRLAnalysis(curProject, Tappas.getWindow());
            DlgUTRLAnalysis.Params results = dlg.showAndWait(DlgUTRLAnalysis.Params.load(curProject.data.analysis.getUTRLParamsFilepath(), curProject));
            if(results != null) {
                TabProjectData.Panels panel = TabProjectData.Panels.STATSUTRL;

                // check if analysis already running
                TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                boolean taskRunning = false;
                if(sti != null && sti.subTabBase != null)
                    taskRunning = sti.subTabBase.isServiceTaskRunning();
                if(!taskRunning) {
                    results.save(curProject.data.analysis.getUTRLParamsFilepath(), curProject);
                    closeProjectDataSubTab(curProject, panel.name());
                    //closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSUTRLASSOCIATION.name());
                    //closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSUTRLRESULTSSUMMARY.name());

                    // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                    String subTabs = SubTabUTRLResults.getAssociatedDVSubTabs();
                    closeProjectDataVizSubTab(curProject, subTabs, true);

                    // clear data and run analysis by opening subtab w/o data
                    curProject.data.analysis.clearDataUTRL(false);
                    openProjectDataSubTab(curProject, panel, null);
                }
                else
                    app.ctls.alertInformation("UTRLAnalysis", "Selected analysis type is already running");
            }
        } catch(Exception e) {
            app.logError("Unable to run UTRLAnalysis: " + e.getMessage());
        }
    }
    
    public void runFEAClusterAnalysis(String id, HashMap<String, String> dfltParams) {
        try {
            DlgClusterAnalysis dlg = new DlgClusterAnalysis(curProject, Tappas.getWindow());
            DlgClusterAnalysis.Params results = dlg.showAndWait(dfltParams);
            if(results != null) {
                TabProjectData.Panels panel = TabProjectData.Panels.CLUSTERSFEA;
                String panelName = TabProjectData.Panels.CLUSTERSFEA + id;

                // check if analysis already running
                TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                boolean taskRunning = false;
                if(sti != null && sti.subTabBase != null)
                    taskRunning = sti.subTabBase.isServiceTaskRunning();
                if(!taskRunning) {
                    closeProjectDataSubTab(curProject, panelName);
                    curProject.data.analysis.removeFEAClusterFiles(id);
                    curProject.data.analysis.setFEAClusterParams(results.getParams(), id);

                    // close all associated data visualization subtabs since underlying analysis data will be deleted
                    // we change GSEA by FEAResults - i dont know why was GSEA
                    String subTabs = SubTabFEAResults.getAssociatedDVSubTabs(id);
                    closeProjectDataVizSubTab(curProject, subTabs, true);

                    // clear data and run analysis by opening subtab w/o data
                    openProjectDataSubTabId(curProject, panel, id, null);
                }
                else
                    app.ctls.alertInformation("FEA Cluster Analysis", "Selected analysis type is already running");
            }
        } catch(Exception e) {
            app.logError("Unable to run FEA Cluster Analysis: " + e.getMessage());
        }
    }
    // GSEA - Gene Set Enrichment Analysis - rank-based logistic regression using GOglm
    public void runGSEAnalysis(String id) {
        try {
            DlgGSEAnalysis dlg = new DlgGSEAnalysis(curProject, Tappas.getWindow());
            DlgGSEAnalysis.Params dfltParams;
            if(!id.isEmpty())
                dfltParams = DlgGSEAnalysis.Params.load(curProject.data.analysis.getGSEAParamsFilepath(id));
            else
                dfltParams = new DlgGSEAnalysis.Params();
            dfltParams.paramId = id;
            DlgGSEAnalysis.Params results = dlg.showAndWait(dfltParams);
            if(results != null) {
                System.out.println("GSEA dialog results: " + results);
                ArrayList<String> lst = new ArrayList<>();
                ArrayList<DataApp.EnumData> lstGSEAParams = curProject.data.analysis.getGSEAParamsList();
                for(DataApp.EnumData data : lstGSEAParams)
                    lst.add(data.id);
                if(!results.hmFeatures.isEmpty() || !results.customFile.isEmpty()) {
                    if(id.isEmpty()) {
                        // make sure we generate a non-existing id
                        Random rand = new Random();
                        LocalDate date = LocalDate.now();
                        LocalTime time = LocalTime.now();
                        do {
                            String idstr = results.name.toLowerCase() + rand.nextInt() + date.toString() + time.toString();
                            id = Utils.getIdHashCode(idstr);
                        } while(lst.contains(id));
                    }
                    TabProjectData.Panels panel = TabProjectData.Panels.STATSGSEA;
                    String panelName = TabProjectData.Panels.STATSGSEA + id;

                    // check if analysis already running
                    TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                    SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                    boolean taskRunning = false;
                    if(sti != null && sti.subTabBase != null)
                        taskRunning = sti.subTabBase.isServiceTaskRunning();
                    if(!taskRunning) {
                        results.save(curProject.data.analysis.getGSEAParamsFilepath(id));
                        closeProjectDataSubTab(curProject, panelName);                    

                        // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                        String subTabs = SubTabGSEAResults.getAssociatedDVSubTabs(id);
                        closeProjectDataVizSubTab(curProject, subTabs, true);

                        // clear data and run analysis by opening subtab w/o data
                        curProject.data.analysis.clearDataGSEA(id, false);
                        openProjectDataSubTabId(curProject, panel, id, null);
                    }
                    else
                        app.ctls.alertInformation("GSEAnalysis", "Selected analysis type is already running");
                }                
            }
        } catch(Exception e) {
            app.logError("Unable to run GSEAnalysis: " + e.getMessage());
        }
    }

    // MODIFY TO GET THE CHANGE THE VALUES WE WANT TO MODIFY IN QUESITOS GRAPH!!!
    public void runGSEAClusterAnalysis(String id, HashMap<String, String> dfltParams) {
        try {
            //new dialog
            DlgGSEACluster dlg = new DlgGSEACluster(curProject, Tappas.getWindow());
            DlgGSEACluster.Params results = dlg.showAndWait(dfltParams);
            if(results != null) {
                TabProjectData.Panels panel = TabProjectData.Panels.CLUSTERSGSEA;
                String panelName = TabProjectData.Panels.CLUSTERSGSEA + id;

                // check if analysis already running
                TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                boolean taskRunning = false;
                if(sti != null && sti.subTabBase != null)
                    taskRunning = sti.subTabBase.isServiceTaskRunning();
                if(!taskRunning) {
                    closeProjectDataSubTab(curProject, panelName);
                    curProject.data.analysis.removeGSEAClusterFiles(id);
                    curProject.data.analysis.setGSEAClusterParams(results.getParams(), id);

                    // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                    String subTabs = SubTabGSEAResults.getAssociatedDVSubTabs(id);
                    closeProjectDataVizSubTab(curProject, subTabs, true);

                    // clear data and run analysis by opening subtab w/o data
                    openProjectDataSubTabId(curProject, panel, id, null);
                }
                else
                    app.ctls.alertInformation("GSEA Cluster Analysis", "Selected analysis type is already running");
            }
        } catch(Exception e) {
            app.logError("Unable to run GSEA Cluster Analysis: " + e.getMessage());
        }
    }
    // Functional Enrichment Analysis - Fisher's exact test
    public void runFEAnalysis(String id) {
        try {
            DlgFEAnalysis dlg = new DlgFEAnalysis(curProject, Tappas.getWindow());
            DlgFEAnalysis.Params dfltParams;
            if(!id.isEmpty())
                dfltParams = DlgFEAnalysis.Params.load(curProject.data.analysis.getFEAParamsFilepath(id));
            else
                dfltParams = new DlgFEAnalysis.Params();
            dfltParams.paramId = id;
            DlgFEAnalysis.Params results = dlg.showAndWait(dfltParams);
            if(results != null) {
                ArrayList<String> lst = new ArrayList<>();
                ArrayList<DataApp.EnumData> lstFEAParams = curProject.data.analysis.getFEAParamsList();
                for(DataApp.EnumData data : lstFEAParams)
                    lst.add(data.id);
                if(!results.hmFeatures.isEmpty()) {
                    if(id.isEmpty()) {
                        // make sure we generate a non-existing id
                        Random rand = new Random();
                        LocalDate date = LocalDate.now();
                        LocalTime time = LocalTime.now();
                        do {
                            String idstr = results.name.toLowerCase() + rand.nextInt() + date.toString() + time.toString();
                            id = Utils.getIdHashCode(idstr);
                        } while(lst.contains(id));
                    }
                    TabProjectData.Panels panel = TabProjectData.Panels.STATSFEA;
                    String panelName = TabProjectData.Panels.STATSFEA + id;

                    // check if analysis already running
                    TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
                    SubTabBase.SubTabInfo sti = tb.getSubTab(panel.name());
                    boolean taskRunning = false;
                    if(sti != null && sti.subTabBase != null)
                        taskRunning = sti.subTabBase.isServiceTaskRunning();
                    if(!taskRunning) {
                        results.save(curProject.data.analysis.getFEAParamsFilepath(id));
                        closeProjectDataSubTab(curProject, panelName);
                        closeProjectDataSubTab(curProject, TabProjectData.Panels.CLUSTERSFEA.name() + id);

                        // close all associated data visuzalization subtabs since underlying analysis data will be deleted
                        String subTabs = SubTabFEAResults.getAssociatedDVSubTabs(id);
                        closeProjectDataVizSubTab(curProject, subTabs, true);

                        // clear data and run analysis by opening subtab w/o data
                        curProject.data.analysis.clearDataFEA(id, false);
                        openProjectDataSubTabId(curProject, panel, id, null);
                    }
                    else
                        app.ctls.alertInformation("FEAnalysis", "Selected analysis type is already running");
                }
            }
        } catch(Exception e) {
            app.logError("Unable to run FEAnalysis: " + e.getMessage());
        }
    }

    public void viewGraphs(DlgSelectGraph.Params.ItemType itemType) {
        try {
            Window wnd = Tappas.getWindow();
            DlgSelectGraph dlg = new DlgSelectGraph(curProject, wnd);
            DlgSelectGraph.Params dfltValues = new DlgSelectGraph.Params(itemType, "", "");
            DlgSelectGraph.Params results = dlg.showAndWait(dfltValues);
            if(results != null) {
                // check if id provided
                String[] values = results.itemPanel.split("\t");
                if(values.length == 2)
                    openProjectDataVizSubTabId(curProject, TabProjectDataViz.Panels.valueOf(values[0]), values[1]);
                else
                    openProjectDataVizSubTab(curProject, TabProjectDataViz.Panels.valueOf(results.itemPanel));
            }
        } catch(Exception e) {
            app.logError("Unable to view selected graph: " + e.getMessage());
        }
    }

    //
    // DrillDown and Tools Functions
    //
    
    public void toolVennDiag() {
        ToolVennDiag dlg = new ToolVennDiag(curProject, Tappas.getWindow());
        HashMap<String, String> results = dlg.showAndWait();
    }
    
    public void drillDownDEAClusters(DataApp.DataType deaDataType, String deaDataTypeName, String grpName, ArrayList<Integer> lstClusters) {
        DrillDownDEAClusters dlg = new DrillDownDEAClusters(curProject, Tappas.getWindow());
        dlg.showAndWait(deaDataType, deaDataTypeName, grpName, lstClusters);
    }
    public void drillDownGSEACluster(DlgGSEAnalysis.Params params, String analysisId, String cluster, int clusterId, ObservableList<SubTabGSEAClusters.NodesTableData> nodesData) {
        DrillDownGSEACluster dlg = new DrillDownGSEACluster(curProject, Tappas.getWindow());
        dlg.showAndWait(params, analysisId, cluster, clusterId, nodesData);
    }
    public void drillDownGSEAClusterNode(DlgGSEAnalysis.Params params, String analysisId, String cluster, String node) {
        DrillDownGSEAClusterNode dlg = new DrillDownGSEAClusterNode(curProject, Tappas.getWindow());
        dlg.showAndWait(params, analysisId, cluster, node);
    }
    public void drillDownGSEAFeatures(DataApp.DataType dataType, String featureId, String description, String fileFeatures, String fileFeatures2, ArrayList<String> lstTestItems) {
        DrillDownGSEA dlg = new DrillDownGSEA(curProject, Tappas.getWindow());
        dlg.showAndWait(dataType, featureId, description, fileFeatures, fileFeatures2, lstTestItems);
    }
    public void drillDownFEAFeatures(DataApp.DataType dataType, String featureId, String description, String fileFeatures, ArrayList<String> lstTestItems) {
        DrillDownFEA dlg = new DrillDownFEA(curProject, Tappas.getWindow());
        dlg.showAndWait(dataType, featureId, description, fileFeatures, lstTestItems);
    }
    public void drillDownFEACluster(DlgFEAnalysis.Params params, String analysisId, String cluster, int clusterId, ObservableList<SubTabFEAClusters.NodesTableData> nodesData) {
        DrillDownFEACluster dlg = new DrillDownFEACluster(curProject, Tappas.getWindow());
        dlg.showAndWait(params, analysisId, cluster, clusterId, nodesData);
    }
    public void drillDownFEAClusterNode(DlgFEAnalysis.Params params, String analysisId, String cluster, String node) {
        DrillDownFEAClusterNode dlg = new DrillDownFEAClusterNode(curProject, Tappas.getWindow());
        dlg.showAndWait(params, analysisId, cluster, node);
    }
    public void drillDownDFISummaryFeatureId(String analysisId, String dataType, String source, String feature, String featureId) {
        DrillDownDFISummary dlg = new DrillDownDFISummary(curProject, Tappas.getWindow());
        dlg.showAndWait(analysisId, dataType, source, feature, featureId);
    }
    public void drillDownDFIFeatureId(DataDFI.DFISelectionResults data) {
        DrillDownDFI dlg = new DrillDownDFI(curProject, Tappas.getWindow());
        dlg.showAndWait(data);
    }
    public void drillDownDFIAssociation(ArrayList<DataDFI.DFIResultsData> lstResultsData,
                                                DataDFI.DFISelectionResultsAssociation selection) {
        DrillDownDFIAssociation dlg = new DrillDownDFIAssociation(curProject, Tappas.getWindow());
        dlg.showAndWait(lstResultsData, selection);
    }
    public void drillDownDPA(DataDPA.DPASelectionResults data) {
        DrillDownDPA dlg = new DrillDownDPA(curProject, Tappas.getWindow());
        dlg.showAndWait(data);
    }
    public void drillDownDPAData(String gene, HashMap<String,ArrayList<String>> iso) {
        DrillDownDPAData dlg = new DrillDownDPAData(curProject, Tappas.getWindow());
        dlg.showAndWait(gene, iso);
    }
    public void drillDownDPAClusters(String dpaDataTypeName, String grpName, ArrayList<Integer> lstClusters) {
        DrillDownDPAClusters dlg = new DrillDownDPAClusters(curProject, Tappas.getWindow());
        dlg.showAndWait(dpaDataTypeName, grpName, lstClusters);
    }

    public void drillDownUTRL(DataUTRL.UTRLSelectionResults data) {
        DrillDownUTRL dlg = new DrillDownUTRL(curProject, Tappas.getWindow());
        dlg.showAndWait(data);
    }
    public void drillDownUTRLData(String gene, HashMap<String,ArrayList<String>> iso) {
        DrillDownUTRLData dlg = new DrillDownUTRLData(curProject, Tappas.getWindow());
        dlg.showAndWait(gene, iso);
    }

    public void drillDownUTRLClusters(String utrlDataTypeName, String grpName, ArrayList<Integer> lstClusters) {
        DrillDownUTRLClusters dlg = new DrillDownUTRLClusters(curProject, Tappas.getWindow());
        dlg.showAndWait(utrlDataTypeName, grpName, lstClusters);
    }

    public void drillDownFDAData(String gene, HashMap<String,ArrayList<String>> genes) {
        DrillDownFDAData dlg = new DrillDownFDAData(curProject, Tappas.getWindow());
        dlg.showAndWait(gene, genes);
    }
    
    //
    // Data Analysis Menu Functions
    //
    
    public void clearDEA() {
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDEATRANS.name());
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDEAPROT.name());
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDEAGENE.name());
        curProject.data.analysis.clearDataDEA(true);
        updateDACombinedResults();
    }
    public void clearDEA(DataType type, boolean rmvPrm) { 
        switch(type) {
            case TRANS:
                closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDEATRANS.name());
                break;
            case PROTEIN:
                closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDEAPROT.name());
                break;
            case GENE:
                closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDEAGENE.name());
                break;
        }
        curProject.data.analysis.clearDataDEA(type, rmvPrm);
        updateDACombinedResults();
    }
    public void clearDIUAll(boolean rmvPrm) { 
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDIUTRANS.name() + curProject.getDef().id);
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDIUPROT.name() + curProject.getDef().id);
        curProject.data.analysis.clearDataDIU(rmvPrm);
        updateDACombinedResults();
    }
    public void clearDIU(DataType type, boolean rmvPrm) { 
        switch(type) {
            case TRANS:
                closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDIUTRANS.name());
                break;
            case PROTEIN:
                closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDIUPROT.name());
                break;
        }
        curProject.data.analysis.clearDataDIU(type, rmvPrm);
        updateDACombinedResults();
    }
    public void clearFAAll() {
        if(app.ctls.alertConfirmation("Clear All Functional Analysis Results", "Are you sure you want to clear ALL Functional Analysis results?", null)) {
            //closetabs!!!
            curProject.data.analysis.removeAllFAResultFiles(true);
        }
    }
    
    public void clearFADFI() {
        clearDFI(false);
    }
    
    public void clearFADPA() {
        clearDPA(false);
    }
    
    public void clearFAUTRL() {
        clearUTRL(false);
    }
    
    public void clearDFI(String id, boolean rmvPrm) { 
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDFI.name() + curProject.getDef().id);
        curProject.data.analysis.clearDataDFI(id, rmvPrm);
        updateDACombinedResults();
    }
    
    public void clearDFI(boolean rmvPrm) { 
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDFI.name() + curProject.getDef().id);
        curProject.data.analysis.clearAllDataDFI(rmvPrm);
        updateDACombinedResults();
    }
    
    public void clearDPA(boolean rmvPrm) { 
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSDPA.name() + curProject.getDef().id);
        curProject.data.analysis.clearDataDPA(rmvPrm);
        updateDACombinedResults();
    }
    
    public void clearUTRL(boolean rmvPrm) { 
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSUTRL.name() + curProject.getDef().id);
        curProject.data.analysis.clearDataUTRL(rmvPrm);
        updateDACombinedResults();
    }
    
    public void clearFDA(String id, boolean rmvPrm) { 
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSFDA.name() + curProject.getDef().id);
        curProject.data.analysis.clearDataFDA(id, rmvPrm);
        updateDACombinedResults();
    }
    public void clearDAAll() {
        if(curProject.data.analysis.hasAnyDAData()) {
            if(app.ctls.alertConfirmation("Clear All Differential Analysis Results", "Are you sure you want to clear ALL Differential Analysis results?", null)) {
                curProject.data.analysis.clearDataDA();
                updateDACombinedResults();
            }
        }
    }
    private void updateDACombinedResults() {
        TabBase tabBase = app.tabs.getTabBase(Tabs.TAB_PROJECTDATA + curProject.getDef().id);
        if(tabBase != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("updateDEACombinedResults", "");
            System.out.println("Updating DA combined results...");
            tabBase.processRequest(hm);
        }
    }
    // this will need to take into account specific data to clear
    public void clearEAAll() {
        if(app.ctls.alertConfirmation("Clear All Enrichment Analysis Results", "Are you sure you want to clear ALL Enrichment Analysis results?", null)) {
            //closetabs!!!
            curProject.data.analysis.removeAllEAResultFiles(true);
        }
    }
    public void clearGSEA(String name, String id, boolean rmvPrms) {
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSGSEA + id);
        curProject.data.analysis.removeGSEAFiles(id, rmvPrms);
    }
    public void clearFEA(String name, String id, boolean rmvPrms) {
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSFEA + id);
        curProject.data.analysis.removeFEAFiles(id, rmvPrms);
    }
    
    //Clear FDA
    public void clearFDA(String name, String id, boolean rmvPrms) {
        closeProjectDataSubTab(curProject, TabProjectData.Panels.STATSFDA + id);
        curProject.data.analysis.removeFDAFiles(id, rmvPrms);
    }

    // The whole reason for tracking the focus changed is to change the color of the selected subtab menus
    // and to enable/disable the search functionality in the top menu based on subtab table selected
    // Doing this is not ideal and may want to look for a better alternative later - however, it seems to work properly
    public void focusChanged(Node newNode) {
        if(newNode != null) {
            Parent parent = newNode.getParent();
            //System.out.println("focus node: " + newNode);
            TabPane tp = null;
            if(newNode.getClass().getSimpleName().equals("TabPane"))
                tp = (TabPane) newNode;
            while(parent != null) {
                if(parent.getClass().getSimpleName().equals("TabPane"))
                    tp = (TabPane)parent;
                parent = parent.getParent();
            }
            if(tp != null && tp.getSelectionModel().getSelectedItem() != null) {
                String tpId = tp.getId();
                String tabId = tp.getSelectionModel().getSelectedItem().getId();
                String subTabId = "";
                TabBase tabBase = tabs.getTabBase(tabId);
                if(tabBase != null) {
                    SubTabBase.SubTabInfo sti = tabBase.getSelectedSubTab();
                    if(sti != null)
                        subTabId = sti.subTabId;
                }
                //System.out.println("tabPaneId: " + tpId + ", tabId: " + tabId + ", subTabId: " + subTabId);
                //System.out.println("focusOwner: " + Tappas.getScene().getFocusOwner());
                if(Math.abs(lastTime - System.currentTimeMillis()) > 50) {
                    // WARN: only call if subtab changed or will end up causing problems - remember this is called from focusChanged event and
                    //       can get caught in an endless focus changing loop
                    if(!lastTabPaneId.equals(tpId) || !lastTabId.equals(tabId) || !lastSubTabId.equals(subTabId)) {
                        lastTime = System.currentTimeMillis();
                        lastSubTabId = subTabId;
                        lastTabId = tabId;
                        lastTabPaneId = tpId;
                        // Note: do not call this function using RunLater or will end up generating tons of focus changes
                        updateTopLevelMenus(tabId, true);
                    }
                }
                //else
                //    System.out.println(">>>> Focus event less than 50ms apart, last: " + lastTime + ", now: " + System.currentTimeMillis());
            }
            else {
                lastTabId = "";
                lastTabPaneId = "";
            }
        }
    }
    public void setTitle(String projectName) {
        if(projectName.isEmpty())
            Tappas.getStage().setTitle(Tappas.APP_LONG_NAME + " " + Tappas.APP_STRVER);
        else
            Tappas.getStage().setTitle(projectName + " - " + Tappas.APP_LONG_NAME + " " + Tappas.APP_STRVER);
    }
    
    //
    // Project service/task
    //
    TaskHandler.ServiceExt service = null;
    Thread thread = null;
    Process process;
    boolean threadResult = false;
    private boolean runOpenProjectThread(Project.ProjectDef def) {
        boolean result = true;

        // run thread
        service = new OpenProjectService(def);
        service.initialize();
        service.start();
        return result;
    }
    private class OpenProjectService extends TaskHandler.ServiceExt {
        Project.ProjectDef def = null;
        boolean success = false;
        
        public OpenProjectService(Project.ProjectDef def) {
            this.def = def;
        }
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            Tappas.getScene().setCursor(Cursor.WAIT);
            fxdc.showAppPane(true);
        }
        @Override
        protected void onRunning() {
        }
        @Override
        protected void onStopped() {
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("AppController - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("AppController - task aborted.");
            app.ctls.alertWarning("App Controller", "AppController - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(success) {
                // don't enable app unless project data already loaded, not likely
                if(curProject.data.isDataLoaded()) {
                    fxdc.showAppPane(false);
                    Tappas.getScene().setCursor(Cursor.DEFAULT);
                }
            }
            else {
                app.ctls.alertWarning("Open Project", "Unable to open requested project,\nsee application log for details.");
                fxdc.showAppPane(false);
                Tappas.getScene().setCursor(Cursor.DEFAULT);
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // allow the cursor and visual pane filter (to disable all UI controls) to be displayed
                    Thread.sleep(100);
                    Platform.runLater(() -> {
                        try {
                            curProject = _openProject(def);
                            addRecentProject(def);
                            success = true;
                        }
                        catch(Exception e) {
                            app.logError("Open project task code exception: " + e.getMessage());
                        }
                    });
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Open Project", task);
            return task;
        }
    }
    private Project _openProject(Project.ProjectDef def) throws Exception {
        Tappas.getStage().setTitle(Tappas.APP_LONG_NAME + " - " + def.name);
        Project p = new Project();
        p.initialize(def);
        return p;
    }
    
    //
    // Data Classes
    //
    
    // Top Menu - Data
    public static class DataMenuFlags {
        boolean runDEA_Trans;
        boolean runDEA_Protein;
        boolean runDEA_Gene;
        boolean runDIU;
        boolean runDFI;
        boolean runDPA;
        boolean runUTRL;
        boolean viewDEA;
        boolean viewCombined;
        boolean viewDEA_Trans;
        boolean viewDEA_Protein;
        boolean viewDEA_Gene;
        boolean viewDIU;
        boolean viewDFI;
        boolean viewDPA;
        boolean viewUTRL;
        boolean clearDEA;
        boolean clearDEA_Trans;
        boolean clearDEA_Protein;
        boolean clearDEA_Gene;
        boolean clearDIU;
        boolean clearDFI;
        boolean clearDPA;
        boolean clearUTRL;
    }
    // Top Menu - Differential Analysis
    public static class DAMenuFlags {
        boolean runDEA;
        boolean runDIU;
        boolean runDFI;
        boolean runDPA;
        boolean runUTRL;
        boolean hasDEA_Trans;
        boolean hasDEA_Protein;
        boolean hasDEA_Gene;
        boolean hasDEA;
        boolean hasDIU_Trans;
        boolean hasDIU_Protein;
        boolean hasDIU;
        boolean hasDFI;
        boolean hasDPA;
        boolean hasUTRL;
        boolean hasAny;
        boolean multipleTimes;
    }
    // Top Menu - Enrichment Analysis
    public static class EAMenuFlags {
        boolean runGSEA;
        boolean runFEA;
        boolean hasFEA;
        boolean hasGSEA;
        boolean hasAny;
        ArrayList<MenuItem> lstGSEAViewItems = new ArrayList<>();
        ArrayList<MenuItem> lstGSEAClearItems = new ArrayList<>();
        ArrayList<MenuItem> lstFEAViewItems = new ArrayList<>();
        ArrayList<MenuItem> lstFEAClearItems = new ArrayList<>();
    }
    // Top Menu - Miscellaneous Analysis
    public static class MAMenuFlags {
        boolean runDiversity;
        boolean hasFDA;
        boolean hasTwoData;
        
        boolean viewDiversity;
        
        ArrayList<MenuItem> lstFDAViewItems = new ArrayList<>();
        ArrayList<MenuItem> lstFDAClearItems = new ArrayList<>();
    }
    // Top Menu - Features Analysis
    public static class FAMenuFlags {
        boolean runDFI;
        boolean hasDFI;
        boolean runDPA;
        boolean hasDPA;
        boolean runUTRL;
        boolean hasUTRL;
        boolean hasAny;
        
        ArrayList<MenuItem> lstDFIViewItems = new ArrayList<>();
        ArrayList<MenuItem> lstDFISummaryItems = new ArrayList<>();
        ArrayList<MenuItem> lstCoDFIViewItems = new ArrayList<>();
        ArrayList<MenuItem> lstDFIClearItems = new ArrayList<>();
    }
    // Top Menu - Data Graphs/Charts
    public static class GraphsMenuFlags {
        boolean hasData;
        boolean hasDA;
        boolean hasEA;
        boolean toolVennDiag;
        public GraphsMenuFlags() {
            hasData = false;
            hasDA = false;
            hasEA = false;
            toolVennDiag = false;
        }
    }
    public static class DataVizMenuItem {
        String menu, name;
        TabProjectDataViz.Panels panel;
        boolean disabled;
        public DataVizMenuItem(String menu, String name, TabProjectDataViz.Panels panel, boolean disabled) {
            this.menu = menu;
            this.name = name;
            this.panel = panel;
            this.disabled = disabled;
        }
    }
}
