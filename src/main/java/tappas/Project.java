/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Project {
    App app;
    public final DataProject data;
    
    private ProjectDef def;
    private int pnum;

    // file paths
    public String getProjectFolder() { return Paths.get(app.data.getAppProjectsFolder(), DataProject.getProjectFolderName(def.id)).toString(); }
    
    // project access functions
    public String getProjectName() { return def.name; }
    public String getProjectId() { return def.id; }
    public int getProjectNumber() { return pnum; }
    public ProjectDef getProjectDef() { return def; }
    
    public Project() {
        app = Tappas.getApp();
        data = new DataProject(this);
    }
    public void initialize(Project.ProjectDef def) throws Exception {
        app.logDebug("Initializing project...");
        this.def = def;
        this.pnum = app.assignProjectNumber();
        data.initialize();
        app.logDebug("Project initialization completed (id:" + def.id + ", num:" + this.pnum + ").");
    }
    public ProjectDef getDef() { return def; }
    
    public static ProjectDef createProject(App app, String name) {
        ProjectDef def = null;
        
        // make sure we generate a non-existing id
        ArrayList<String> lst = app.getProjectIdsList();
        Random rand = new Random();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        String id = "";
        do {
            String idstr = name.toLowerCase() + rand.nextInt() + date.toString() + time.toString();
            id = Utils.getIdHashCode(idstr);
        } while(lst.contains(id));
        
        // create project folder
        String folderpath = app.data.getProjectFolder(id);
        if(Utils.createFolder(folderpath)) {
            // save project definition to file
            Project.ProjectDef projdef = new Project.ProjectDef(id, name);
            if(DataProject.saveProjectDef(projdef.getParams(), folderpath))
                def = projdef;
            else
                app.ctls.alertError("Project Creation Error", "Unable to save project definition to file.");
        }
        else
            app.ctls.alertError("Project Creation Error", "Unable to create project folders.");
        return def;
    }
    public boolean changeProjectName(String name) {
        boolean result = false;
        String folderpath = app.data.getProjectFolder(def.id);
        Project.ProjectDef projdef = new Project.ProjectDef(def.id, name);
        if(DataProject.saveProjectDef(projdef.getParams(), folderpath)) {
            def = projdef;
            result = true;
        }
        return result;
    }
    
    //
    // Data Classes
    //
    
    public static class ProjectDef implements Comparable<ProjectDef> {
        String id, name;
        int dataVersion;
        
        // internal use flag - not part of parameters
        boolean newdata = false;
        public ProjectDef(String id, String name) {
            this.id = id;
            this.name = name;
            this.dataVersion = DlgOpenProject.Params.CUR_DATA_VER;
        }
        public ProjectDef(HashMap<String, String> hmParams) {
            this.id = hmParams.containsKey(DlgOpenProject.Params.ID_PARAM)? hmParams.get(DlgOpenProject.Params.ID_PARAM) : "";
            this.name = hmParams.containsKey(DlgOpenProject.Params.NAME_PARAM)? hmParams.get(DlgOpenProject.Params.NAME_PARAM) : "";
            this.dataVersion = hmParams.containsKey(DlgOpenProject.Params.DATAVER_PARAM)? Integer.parseInt(hmParams.get(DlgOpenProject.Params.DATAVER_PARAM)) : 0;
        }
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(DlgOpenProject.Params.ID_PARAM, id);
            hm.put(DlgOpenProject.Params.NAME_PARAM, name);
            hm.put(DlgOpenProject.Params.DATAVER_PARAM, "" + dataVersion);
            return hm;
        }
        @Override
        public int compareTo(ProjectDef td) {
            return (name.compareToIgnoreCase(td.name));
        }
    }
}
