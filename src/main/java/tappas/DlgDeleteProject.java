 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

 import javafx.collections.ObservableList;
 import javafx.scene.control.*;
 import javafx.scene.input.MouseButton;
 import javafx.scene.input.MouseEvent;
 import javafx.stage.Window;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgDeleteProject extends DlgBase {
    ListView lvProjects;
    ArrayList<Project.ProjectDef> lstProjects = new ArrayList<>();
    ArrayList<Project.ProjectDef> lstActiveProjects = new ArrayList<>();
    
    public DlgDeleteProject(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dlftParams) {
        if(createDialog("DeleteProject.fxml", "Delete Project(s)", true, null)) {
            // get control objects
            lvProjects = (ListView) scene.lookup("#lstProjects");

            // populate projects list
            lvProjects.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            lstProjects = app.getProjectsList();
            lstActiveProjects = app.tabs.getActiveProjects();
            for(Project.ProjectDef def : lstProjects) {
                boolean found = false;
                for(Project.ProjectDef actdef : lstActiveProjects) {
                    if(def.id.equals(actdef.id)) {
                        found = true;
                        break;
                    }
                }
                if(!found)
                    lvProjects.getItems().add(def.name);
            }

            // setup listeners and bindings
            lvProjects.setOnMouseClicked((MouseEvent event) -> {
                if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1){
                    ObservableList<ButtonType> lst = dialog.getDialogPane().getButtonTypes();
                    for(ButtonType bt : lst) {
                        if(bt.getText().equals("OK")) {
                            if(dialog.getDialogPane().lookupButton(bt) != null)
                                ((Button) dialog.getDialogPane().lookupButton(bt)).fire();
                        }
                    }
                }
            });

            // setup dialog event handlers
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> params = null;
                System.out.println(b.getButtonData().toString());
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    params = validate(dialog);
                return params;
            });

            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return(new Params(result.get()));
        }
        return null;
    }
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        
        // check project selection
        String errmsg = "";
        int pidx = lvProjects.getSelectionModel().getSelectedIndex();
        if(pidx != -1) {
            ObservableList<String> ol = lvProjects.getSelectionModel().getSelectedItems();
            int p = 1;
            for(String name : ol) {
                for(Project.ProjectDef def : lstProjects) {
                    if(name.equals(def.name)) {
                        results.put(Params.NAME_PARAM + p, name);
                        results.put(Params.ID_PARAM + p, def.id);
                        p++;
                    }
                }
            }
            if(results.isEmpty()) {
                errmsg = "Internal error: Unable to get project's folder name.";
                lvProjects.requestFocus();
            }
        }
        else {
            errmsg = "You must select one or more projects from list.";
            lvProjects.requestFocus();
        }
        
        if(!errmsg.isEmpty())
            results.put("ERRMSG", errmsg);
        return results;
    }
    
    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static final String NAME_PARAM = "name";
        public static final String ID_PARAM = "id";
        public static final int MAX_PROJECTS = 99;
        
        HashMap<String, String> hmProjects = new HashMap<>();
        public Params() {
            hmProjects = new HashMap<>();
        }
        public Params(HashMap<String, String> hmParams) {
            hmProjects = new HashMap<>();
            for(int p = 1; p < MAX_PROJECTS; p++) {
                if(hmParams.containsKey(NAME_PARAM + p))
                    hmProjects.put(hmParams.get(NAME_PARAM + p).trim(), hmParams.get(ID_PARAM + p).trim());
                else
                    break;
            }
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            int p = 1;
            for(String project : hmProjects.keySet()) {
                for(String name : hmProjects.keySet()) {
                    hm.put(NAME_PARAM + p, name);
                    hm.put(ID_PARAM + p, hmProjects.get(name));
                }
                if(++p > MAX_PROJECTS)
                    break;
            }
            return hm;
        }
        // base class implements boolean save(String filepath)

        //
        // Static functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
    }
}
