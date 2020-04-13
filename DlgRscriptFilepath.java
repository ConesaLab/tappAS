 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

 import javafx.scene.control.*;
 import javafx.stage.FileChooser;
 import javafx.stage.Window;

 import java.io.File;
 import java.nio.file.Files;
 import java.nio.file.Paths;
 import java.util.HashMap;
 import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgRscriptFilepath extends DlgBase {
    public static final String FILE_PARAM = "file";
    
    TextField txtFile;
    Button btnFile;
    
    public DlgRscriptFilepath(Project project, Window window) {
        super(project, window);
    }
    public HashMap<String, String> showAndWait(HashMap<String, String> dfltValues) {
        if(createDialog("RscriptFilepath.fxml", "Rscript Full File Path and Name", true, null)) {
            // setup default values
            HashMap<String, String> vals = new HashMap<>();
            vals.put(FILE_PARAM, "");
            if(dfltValues != null) {
                if(dfltValues.containsKey(FILE_PARAM))
                    vals.put(FILE_PARAM, dfltValues.get(FILE_PARAM));
            }
            
            // get control objects
            txtFile = (TextField) scene.lookup("#txtFile");
            btnFile = (Button) scene.lookup("#btnFile");

            // populate default values
            txtFile.setText(vals.get(FILE_PARAM));

            // setup button actions
            btnFile.setOnAction((event) -> { getFile(); });

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
                return result.get();
        }
        return null;
    }
    
    //
    // Internal Functions
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();

        // check file paths
        String inpFile = txtFile.getText().trim();
        if(!inpFile.isEmpty()) {
            if(Files.exists(Paths.get(inpFile)) && !Files.isDirectory(Paths.get(inpFile)))
                results.put(FILE_PARAM, inpFile);
            else {
                results.put("ERRMSG", "Specified file not found.");
                txtFile.requestFocus();
            }            
        }
        else {
            results.put("ERRMSG", "You must specify the file path and name");
            txtFile.requestFocus();
        }            

        return results;
    }
    private void getFile() {
        File f = getFileName();
        if(f != null)
            txtFile.setText(f.getPath());
    }
    private File getFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Rscript File");
        File f = new File(System.getProperty("user.home"));
        if(f.exists())
            fileChooser.setInitialDirectory(f);
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        return selectedFile;
    }
}
