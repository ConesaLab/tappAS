 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

 import javafx.scene.Cursor;
 import javafx.scene.control.ProgressIndicator;
 import javafx.stage.Stage;
 import javafx.stage.Window;

 import java.nio.file.Path;


/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgInstallingPackages extends DlgBase {
    private boolean res = false;
    ProgressIndicator pg;
    
    public DlgInstallingPackages(Project project, Window window) {
        super(project, null);
        this.window = window;
    }
    
    public void closeAction(){
    // get a handle to the stage
        Stage stage = (Stage) pg.getScene().getWindow();
    // do what you have to do
        stage.close();
    }
    
    public boolean showAndWait(App app, AppFXMLDocumentController fxdc) {
        if(createDialog("InstallingPackages.fxml", "Installing all packages you need...", false, false, null)) {
            try {
                Path path = app.data.getTmpScriptFileFromResource("tappas_downloadPackages.R");
                res = app.runDownloadPackages(path.toString());
                Utils.removeFile(path);
            
            }catch(Exception e) {
                app.logWarning("Unable to install R packages: " + e.getMessage());
            }
            Tappas.getScene().setCursor(Cursor.DEFAULT);
            
            if(res)
                showDlgMsg("All packages installed.");
            else
                showDlgMsg("There seems to have been an error installing the packages. We recommend you install them manually by following our manual which you will find on: http://app.tappas.org/downloads/");
            return res;
        }
        return false;
    }
    
    //
    // Internal Functions
    //
}
