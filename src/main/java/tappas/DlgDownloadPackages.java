 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

 import javafx.scene.control.Button;
 import javafx.scene.control.ButtonBar;
 import javafx.scene.control.ButtonType;
 import javafx.stage.Stage;
 import javafx.stage.Window;
 import javafx.util.Callback;

 import java.util.HashMap;
 import java.util.Optional;


/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgDownloadPackages extends DlgBase {
    private boolean res = false;
    Button btDownload;
    Button btCancel;
    
    public DlgDownloadPackages(Project project, Window window) {
        super(project, null);
        this.window = window;
    }
    
    private void closeButtonAction(){
    // get a handle to the stage
        Stage stage = (Stage) btCancel.getScene().getWindow();
    // do what you have to do
        stage.close();
    }
    
    public HashMap<String, String> showAndWait(HashMap<String, String> dfltValues) {
        if(createDialog("DownloadPackages.fxml", "Do you want to download all R Packages?", true, null)) {
            HashMap<String, String> vals = new HashMap<>();
            // setup dialog event handlers
            dialog.setResultConverter(new Callback<ButtonType, HashMap>() {
                @Override
                public HashMap call(ButtonType b) {
                    HashMap<String, String> params = null;
                    System.out.println(b.getButtonData().toString());
                    if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE){
                        params = new HashMap<String, String>();
                        params.put("Download", "true");
                    }else{
                        params = new HashMap<String, String>();
                        params.put("Download", "false");}
                    return params;
                }
            });

            //btDownload.setOnAction((ActionEvent actionEvent) -> {
            //    vals.put("Ok", "true");
            //    closeButtonAction();
            //});
            
            //btDownload.setOnAction((ActionEvent actionEvent) -> {
            //    closeButtonAction();
            //});
            
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
}
