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
public class DlgOpenProject extends DlgBase {
    ListView lvProjects;
    ArrayList<Project.ProjectDef> lstProjects = new ArrayList<>();
    
    public DlgOpenProject(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dlftParams) {
        if(createDialog("OpenProject.fxml", "Open Project", true, null)) {
            // get control objects
            lvProjects = (ListView) scene.lookup("#lstProjects");

            // populate projects list
            lstProjects = app.getProjectsList();
            for(Project.ProjectDef def : lstProjects)
                lvProjects.getItems().add(def.name);
            if(!lvProjects.getItems().isEmpty())
                lvProjects.getSelectionModel().select(0);
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
    
    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        
        // check project selection
        String errmsg = "";
        int pidx = lvProjects.getSelectionModel().getSelectedIndex();
        if(pidx != -1) {
            String id = "";
            String name = (String) lvProjects.getSelectionModel().getSelectedItem();
            results.put(Params.NAME_PARAM, name);
            for(Project.ProjectDef def : lstProjects) {
                if(name.equals(def.name)) {
                    id = def.id;
                    results.put(Params.ID_PARAM, id);
                    break;
                }
            }
            if(id.isEmpty()) {
                errmsg = "Internal error: Unable to get project's folder name.";
                lvProjects.requestFocus();
            }
        }
        else {
            errmsg = "You must select a project from list.";
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
        // minimum data version with which this code will work and current data version
        // code will never work with a data version greater than the current data version
        // the only reason to change the data version is because it is no longer compatible
        public static final int MIN_DATA_VER = 1;
        public static final int CUR_DATA_VER = 1;
        
        public static final String NAME_PARAM = "name";
        public static final String ID_PARAM = "id";
        public static final String DATAVER_PARAM = "dataver";
        
        String name = "";
        String id = "";
        int dataVersion;
        public Params() {
            this.name = "";
            this.id = "";
            this.dataVersion = CUR_DATA_VER;
        }
        public Params(HashMap<String, String> hmParams) {
            this.name = hmParams.containsKey(NAME_PARAM)? hmParams.get(NAME_PARAM) : "";
            this.id = hmParams.containsKey(ID_PARAM)? hmParams.get(ID_PARAM) : "";
            this.dataVersion = hmParams.containsKey(DATAVER_PARAM)? Integer.parseInt(hmParams.get(DATAVER_PARAM)) : 0;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(NAME_PARAM, name);
            hm.put(ID_PARAM, id);
            hm.put(DATAVER_PARAM, "" + dataVersion);
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
        public static boolean isValidData(int dv) {
            boolean result = false;
            if(dv >= MIN_DATA_VER && dv <= CUR_DATA_VER)
                result = true;
            return result;
        }
    }
}
