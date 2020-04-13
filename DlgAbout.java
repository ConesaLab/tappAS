/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgAbout extends DlgBase {
    public DlgAbout(Project project, Window window) {
        super(project, window);
    }
    public HashMap<String, String> showAndWait() {
        if(createDialog("About.fxml", "About tappAS", false, null)) {
            // get control objects
            TextArea txtInfo = (TextArea) scene.lookup("#txtInfo");
            Label txtVersion = (Label) scene.lookup("#lblAboutVersion");
            WebView web = (WebView) scene.lookup("#webCredits");
            WebEngine engine = web.getEngine();;

            // display app info
            String info = app.getAppInfo();
            txtInfo.setText(info);
            txtVersion.setText("Version " + Tappas.APP_STRVER);
            
            // get html file
            String content = app.data.getFileContentFromResource("/tappas/resources/web/about.html");
            if(content.isEmpty())
                content = app.data.getFileContentFromResource("/tappas/resources/web/Help_NA.html");
            
            // display contents
            engine.loadContent(content);

            // setup dialog event handlers
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> params = null;
                return null;
            });

            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return result.get();
        }
        return null;
    }
}
