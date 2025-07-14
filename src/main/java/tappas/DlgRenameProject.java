 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

 import javafx.scene.control.*;
 import javafx.stage.Window;

 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgRenameProject extends DlgBase {
    TextField txtName;
    
    public DlgRenameProject(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait() {
        if(createDialog("RenameProject.fxml", "Rename Project", true, null)) {
            setProjectName();

            // get control objects
            txtName = (TextField) scene.lookup("#txtName");
            txtName.setText(project.getProjectName());

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
        String name = txtName.getText().trim();
        if(!name.isEmpty()) {
            String namelc = name.toLowerCase();
            ArrayList<Project.ProjectDef> lstProjects = app.getProjectsList();
            if(name.equals(project.getProjectName()))
                errmsg = "Specified project's name is the same as current name.";
            else {
                // allow case insensitive match only for current name
                if(!namelc.equals(project.getProjectName().toLowerCase())) {
                    for(Project.ProjectDef def : lstProjects) {
                        // do not allow same name, case insensitive
                        if(namelc.equals(project.getProjectName().toLowerCase())) {
                            errmsg = "Specified project's name is already in use.";
                            break;
                        }
                    }
                }
            }
        }
        else
            errmsg = "You must enter the new project name.";
        
        if(errmsg.isEmpty())
            results.put(Params.NAME_PARAM, name);
        else {
            txtName.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        return results;
    }
    
    //
    // Data Classes
    //
    
    public static class Params extends DlgParams {
        public static final String NAME_PARAM = "name";
        
        String name = "";
        public Params() {
            this.name = "";
        }
        public Params(String id, String name) {
            this.name = name;
        }
        public Params(HashMap<String, String> hmParams) {
            this.name = hmParams.containsKey(NAME_PARAM)? hmParams.get(NAME_PARAM) : "";
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(NAME_PARAM, name);
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
