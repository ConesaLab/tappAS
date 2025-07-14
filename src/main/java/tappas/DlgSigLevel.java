/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgSigLevel extends DlgBase {
    TextField txtSigValue;
    TextField txtFCValue;
    
    public DlgSigLevel(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        if(createDialog("SigLevel.fxml", "Significance Level", true, null)) {
            // create default parameters if not given
            if(dfltParams == null)
                dfltParams = new Params();

            // get control objects
            txtSigValue = (TextField) scene.lookup("#txtSigValue");
 
            // populate dialog
            txtSigValue.setText("" + dfltParams.sigValue);

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
    
    public ParamsDEA showAndWaitDEA(ParamsDEA dfltParams) {
        if(createDialog("SigLevelDEA.fxml", "Significance Level", true, null)) {
            // create default parameters if not given
            if(dfltParams == null)
                dfltParams = new ParamsDEA();

            // get control objects
            txtSigValue = (TextField) scene.lookup("#txtSigValue");
            txtFCValue = (TextField) scene.lookup("#txtFCValue");
 
            // populate dialog
            txtSigValue.setText("" + dfltParams.sigValue);
            txtFCValue.setText("" + dfltParams.FCValue);

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
                    params = validateDEA(dialog);
                return params;
            });

            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return(new ParamsDEA(result.get()));
        }
        return null;
    }
    
    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        
        // check for a valid threshold value
        String txt = txtSigValue.getText().trim();
        if(txt.length() > 0) {
            try {
                Double val = Double.parseDouble(txt);
                if(val >= Params.MIN_PVAL_THRESHOLD && val <= Params.MAX_PVAL_THRESHOLD) {
                    results.put(Params.SIGVAL_PARAM, txt);
                }
                else
                    errmsg = "Invalid significance value entered (" + Params.MIN_PVAL_THRESHOLD + " to " + Params.MAX_PVAL_THRESHOLD + " allowed).";
            } catch(Exception e) {
                errmsg = "Invalid significance value number entered.";
            }
        }
        else
            errmsg = "You must enter a significance value.";
        if(!errmsg.isEmpty()) {
            txtSigValue.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        return results;
    }
    
    private HashMap<String, String> validateDEA(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        
        // check for a valid threshold value
        String txt = txtSigValue.getText().trim();
        if(txt.length() > 0) {
            try {
                Double val = Double.parseDouble(txt);
                if(val >= Params.MIN_PVAL_THRESHOLD && val <= Params.MAX_PVAL_THRESHOLD) {
                    results.put(Params.SIGVAL_PARAM, txt);
                }
                else
                    errmsg = "Invalid significance value entered (" + Params.MIN_PVAL_THRESHOLD + " to " + Params.MAX_PVAL_THRESHOLD + " allowed).";
            } catch(Exception e) {
                errmsg = "Invalid significance value number entered.";
            }
        }
        else
            errmsg = "You must enter a significance value.";
        if(!errmsg.isEmpty()) {
            txtSigValue.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        
        // check for a valid threshold value
        txt = txtFCValue.getText().trim();
        if(txt.length() > 0) {
            try {
                Double val = Double.parseDouble(txt);
                if(val >= Params.MIN_PVAL_THRESHOLD && val <= Params.MAX_PVAL_FC) {
                    results.put(ParamsDEA.FC_PARAM, txt);
                }
                else
                    errmsg = "Invalid fold change value entered (" + Params.MIN_PVAL_THRESHOLD + " to " + Params.MAX_PVAL_FC + " allowed).";
            } catch(Exception e) {
                errmsg = "Invalid fold change value number entered.";
            }
        }
        else
            errmsg = "You must enter a fold change value.";
        if(!errmsg.isEmpty()) {
            txtSigValue.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        return results;
    }

    //
    // Data Classes
    //
    public static class ParamsDEA extends DlgParams {
        private static final String TITLE_PARAM = "title";
        private static final String SIGVAL_PARAM = "sigval";
        private static final String FC_PARAM = "FC";
        private final double dfltSigValue = 0.05;
        private final double dfltFCValue = 2.0;
        private final String dfltTitle = "Analysis Significance Level";
        
        String title;
        double sigValue;
        double FCValue;
        public ParamsDEA() {
            this.title = dfltTitle;
            this.sigValue = dfltSigValue;
            this.FCValue = dfltFCValue;
        }
        public ParamsDEA(HashMap<String, String> hmParams) {
            if(hmParams.containsKey(TITLE_PARAM))
                this.title = hmParams.get(TITLE_PARAM);
            else
                this.title = "Analysis Significance Level";
            if(hmParams.containsKey(SIGVAL_PARAM))
                this.sigValue = Double.parseDouble(hmParams.get(SIGVAL_PARAM));
            else
                this.sigValue = dfltSigValue;
            if(hmParams.containsKey(FC_PARAM))
                this.FCValue = Double.parseDouble(hmParams.get(FC_PARAM));
            else
                this.FCValue = dfltFCValue;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(TITLE_PARAM, title);
            hm.put(SIGVAL_PARAM, "" + sigValue);
            hm.put(FC_PARAM, "" + FCValue);
            return hm;
        }
    }
    
    public static class Params extends DlgParams {
        private static final String TITLE_PARAM = "title";
        private static final String SIGVAL_PARAM = "sigval";
        private final double dfltSigValue = 0.05;
        private final String dfltTitle = "Analysis Significance Level";
        
        String title;
        double sigValue;
        public Params() {
            this.title = dfltTitle;
            this.sigValue = dfltSigValue;
        }
        public Params(HashMap<String, String> hmParams) {
            if(hmParams.containsKey(TITLE_PARAM))
                this.title = hmParams.get(TITLE_PARAM);
            else
                this.title = "Analysis Significance Level";
            if(hmParams.containsKey(SIGVAL_PARAM))
                this.sigValue = Double.parseDouble(hmParams.get(SIGVAL_PARAM));
            else
                this.sigValue = dfltSigValue;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(TITLE_PARAM, title);
            hm.put(SIGVAL_PARAM, "" + sigValue);
            return hm;
        }
    }
}
