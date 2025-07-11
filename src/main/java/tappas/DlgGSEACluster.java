/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Optional;

/** 
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgGSEACluster extends DlgBase {
    Button btnListFile;
    RadioButton rbtnPval, rbtnList;
    TextField txtPval, txtListFile;
    Label lblPvalInfo, lblPval, lblListFile;

    public DlgGSEACluster(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(HashMap<String, String> dfltValues) {
        if(createDialog("ClusterParamsGSEA.fxml", "GSEA Multidimensional Graph Parameters", true, "Help_Dlg_GSEACluster.html")) {
            // setup default values
            HashMap<String, String> vals = new HashMap<>();
            vals.put(Params.PVAL_PARAM, String.valueOf(Params.dfltPval));
            vals.put(Params.LIST_PARAM, Params.dfltList);
            if(dfltValues != null) {
                if(dfltValues.containsKey(Params.PVAL_PARAM))
                    vals.put(Params.PVAL_PARAM, dfltValues.get(Params.PVAL_PARAM));
                if(dfltValues.containsKey(Params.LIST_PARAM))
                    vals.put(Params.LIST_PARAM, dfltValues.get(Params.LIST_PARAM));
            }

            // get control objects
            btnListFile = (Button) scene.lookup("#btnListFile");
            rbtnPval = (RadioButton) scene.lookup("#rbtnPval");
            rbtnList = (RadioButton) scene.lookup("#rbtnList");
            txtPval = (TextField) scene.lookup("#txtPval");
            txtListFile = (TextField) scene.lookup("#txtListFile");
            lblPvalInfo = (Label) scene.lookup("#lblPvalInfo");
            lblPval = (Label) scene.lookup("#lblPval");
            lblListFile = (Label) scene.lookup("#lblListFile");

            // populate dialog

            lblListFile.setDisable(true);
            btnListFile.setDisable(true);
            txtListFile.setDisable(true);
            rbtnList.setSelected(false);

            rbtnPval.setSelected(true);
            rbtnList.setSelected(false);
            txtPval.setText(vals.get(Params.PVAL_PARAM));

            btnListFile.setOnAction((event) -> { getRLFile(txtListFile); });

            rbtnPval.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    lblListFile.setDisable(true);
                    btnListFile.setDisable(true);
                    txtListFile.setDisable(true);
                    rbtnList.setSelected(false);
                }else{
                    lblListFile.setDisable(false);
                    btnListFile.setDisable(false);
                    txtListFile.setDisable(false);
                }
            });

            rbtnList.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    lblPval.setDisable(true);
                    lblPvalInfo.setDisable(true);
                    txtPval.setDisable(true);
                    rbtnPval.setSelected(false);
                }else{
                    lblPval.setDisable(false);
                    lblPvalInfo.setDisable(false);
                    txtPval.setDisable(false);
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

            // setup dialog event handlers
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return new Params(result.get());
        }
        return null;
    }
    
    //
    // Dialog Validation
    //
    private void getRLFile(TextField txt) {
        File f = getFileName();
        if(f != null)
            txt.setText(f.getPath());
    }

    private File getFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Select Feature list File"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("List of features file", "*.txt", "*.tsv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        String expFolder = app.userPrefs.getImportRankedListGSEAFolder();
        if(!expFolder.isEmpty()) {
            File f = new File(expFolder);
            if(f.exists() && f.isDirectory())
                fileChooser.setInitialDirectory(f);
            else
                app.userPrefs.setImportRankedListGSEAFolder("");
        }
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        if(selectedFile!=null)
            app.userPrefs.setImportRankedListGSEAFolder(selectedFile.getParent());
        return selectedFile;
    }

    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        System.out.println("Validate dialog");
        
        // check pval
        if(rbtnPval.isSelected()){
            String txt = txtPval.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= Params.MIN_PVAL && val <= Params.MAX_PVAL) {
                        results.put(Params.PVAL_PARAM, txt);
                    }
                    else
                        errmsg = "Invalid p-value entered (" + Params.MIN_PVAL + " to " + Params.MAX_PVAL + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid p-value entered.";
                }
            }
            else
                errmsg = "You must enter a p-value.";
            if(!errmsg.isEmpty()) {
                txtPval.requestFocus();
                results.put("ERRMSG", errmsg);
            }
        // check list
        }else {
            String txt = txtListFile.getText().trim();
            if(txt.length() > 0) {
                try {
                    String filepath = txtListFile.getText().trim();
                    errmsg = checkFeatureListFile(filepath, results);
                    if(errmsg.isEmpty())
                        results.put(Params.LIST_PARAM, filepath);
                    else
                        txtListFile.requestFocus();
                } catch(Exception e) {
                    errmsg = "Invalid Feature list file entered.";
                }
            }
            else
                errmsg = "You must enter a feature list.";
        }
        if(!errmsg.isEmpty()) {
            txtListFile.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        return results;
    }

    // We want to provide meaningful messages to the user - so there is a lot of checking taking place
    private String checkFeatureListFile(String path, HashMap<String, String> results) {
        String errmsg = "";
        app.logInfo("Checking specified feature list file: " + path);
        if(path.length() > 0) {
            try {
                File f = new File(path);
                if(f.exists()) {
                    long size = f.length();
                    if(size >= Params.MIN_RL_FILE_SIZE && size <= Params.MAX_RL_FILE_SIZE) {
                        if(f.canRead()) {
                            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                                String line = br.readLine();
                                if(line != null) {
                                    if(!line.trim().isEmpty() && !line.startsWith("#")) {
                                        String[] fields = line.split("\t");
                                        if(fields.length >= 1) {

                                        }
                                    }
                                }
                                else
                                    errmsg = "Unable to read feature list file data.";
                            }
                        }
                        else
                            errmsg = "Feature list file does not have read access. Check file permissions.";
                    }
                    else {
                        if(size > Params.MAX_RL_FILE_SIZE)
                            errmsg = "Feature list file exceeds maximum size allowed of " + (Params.MAX_RL_FILE_SIZE/1000000) + "MB";
                        else
                            errmsg = "Feature list file does not have sufficient data (" + size + "bytes).";
                    }
                }
                else
                    errmsg = "Unable to find specified Feature list file.";
            } catch(Exception e) {
                errmsg = "Unable to open specified Feature list file.";
            }
        }
        else
            errmsg = "You must specify the Feature list file's location.";
        if(!errmsg.isEmpty())
            app.logInfo("Feature list file check failed: " + errmsg);
        else
            app.logInfo("Feature list file passed initial check.");
        return errmsg;
    }

    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static final String PVAL_PARAM = "pval";
        public static final String LIST_PARAM = "custom_list";
        
        final static double MIN_PVAL = 0.0;
        final static double MAX_PVAL = 1.0;
        public static final int MIN_RL_FILE_SIZE = 10;
        public static final int MAX_RL_FILE_SIZE = 100000000;

        final static double dfltPval = 0.01;
        final static String dfltList = "";

        public double pVal;
        public String list;
        public Params() {
            this.pVal = dfltPval;
            this.list = dfltList;
        }
        public Params(HashMap<String, String> hmParams) {
            this.pVal = hmParams.containsKey(PVAL_PARAM)? Double.parseDouble(hmParams.get(PVAL_PARAM)) : dfltPval;
            this.list = hmParams.containsKey(LIST_PARAM)? hmParams.get(LIST_PARAM) : dfltList;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(PVAL_PARAM, "" + pVal);
            hm.put(LIST_PARAM, "" + list);
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
