/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.stage.Window;
import tappas.DataApp.DataType;
import tappas.DataApp.EnumData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgDEAnalysis extends DlgBase {
    TextField txtSigValue, txtFCValue, txtR2Cutoff, txtK;
    RadioButton rbTrans, rbProteins, rbGenes;
    ChoiceBox cbReps, cbMethods;
    CheckBox chkMclust;
    Label lblReplicate, lblR2Cutoff, lblR2Default, lblK, lblFC;
    
    DataApp.ExperimentType expType = project.data.getExperimentType();
    
    public DlgDEAnalysis(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        if(createDialog("DEAParams.fxml", "Differential Expression Analysis Parameters", true, "Help_Dlg_DEA.html")) {
            if(dfltParams == null)
                dfltParams = new Params(project);
            
            // get control objects
            lblReplicate = (Label) scene.lookup("#lblReplicate");
            lblR2Cutoff = (Label) scene.lookup("#lblR2Cutoff");
            lblR2Default = (Label) scene.lookup("#lblR2Default");
            lblFC = (Label) scene.lookup("#lblFC");
            txtR2Cutoff = (TextField) scene.lookup("#txtR2Cutoff");
            txtSigValue = (TextField) scene.lookup("#txtSigValue");
            txtFCValue = (TextField) scene.lookup("#txtFCValue");
            lblK = (Label) scene.lookup("#lblK");
            txtK = (TextField) scene.lookup("#txtK");
            chkMclust = (CheckBox) scene.lookup("#chkMclust");
            rbTrans = (RadioButton) scene.lookup("#rbTrans");
            rbProteins = (RadioButton) scene.lookup("#rbProteins");
            rbGenes = (RadioButton) scene.lookup("#rbGenes");
            cbReps = (ChoiceBox) scene.lookup("#cbReplicates");
            
            // setup dialog
            setProjectName();
            cbMethods = (ChoiceBox) scene.lookup("#cbMethods");
            
            // SHOW EDGER OR NOT IF DATA NORMALIZED
            if(project.data.normalized){
                for(MethodData method : Params.lstMethodsDataNormalized) {
                    if(method.expType.equals(expType))
                        cbMethods.getItems().add(method.name);
                } 
            }else{
                for(MethodData method : Params.lstMethods) {
                    if(method.expType.equals(expType))
                        cbMethods.getItems().add(method.name);
                } 
            }
            
            int maxDegree = Math.max(1, project.data.getTimePoints() - 1);
            if(project.data.isTimeCourseExpType()) {
                lblReplicate.setText("Polynomial Degree:");
                txtR2Cutoff.setVisible(true);
                lblR2Cutoff.setVisible(true);
                lblR2Default.setVisible(true);
                lblK.setVisible(true);
                txtK.setVisible(true);
                chkMclust.setVisible(true);
                cbReps.setPrefWidth(84);
                cbReps.setMaxWidth(84);
                for(int i = 1; i <= maxDegree; i++)
                    cbReps.getItems().add("" + i);
                txtR2Cutoff.setText("" + dfltParams.R2Cutoff);
                txtK.setText("" + dfltParams.k);
                chkMclust.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                    lblK.setText(newValue? "Max K clusters:" : "K clusters:");
                });
                chkMclust.setSelected(dfltParams.mclust);
                if((dfltParams.degree - 1) < maxDegree)
                    cbReps.getSelectionModel().select(dfltParams.degree - 1);
                else
                    cbReps.getSelectionModel().select(maxDegree - 1);
            }
            else {
                txtFCValue.disableProperty().bind(cbMethods.valueProperty().isEqualTo("edgeR"));
                lblFC.disableProperty().bind(cbMethods.valueProperty().isEqualTo("edgeR"));
                cbReps.disableProperty().bind(cbMethods.valueProperty().isEqualTo("edgeR"));
                lblReplicate.disableProperty().bind(cbMethods.valueProperty().isEqualTo("edgeR"));
                
                for(EnumData rep : Params.lstReps)
                    cbReps.getItems().add(rep.name);
                    cbReps.getSelectionModel().select(Params.getRepsListIdx(dfltParams.replicates.name()));
                    
                txtR2Cutoff.setVisible(false);
                lblR2Cutoff.setVisible(false);
                lblR2Default.setVisible(false);
                lblK.setVisible(false);
                txtK.setVisible(false);
                chkMclust.setVisible(false);
                
            }

            if(dfltParams.dataType.equals(DataType.PROTEIN))
                rbProteins.setSelected(true);
            else if(dfltParams.dataType.equals(DataType.TRANS))
                rbTrans.setSelected(true);
            else
                rbGenes.setSelected(true);
            
            if(project.data.normalized){
                cbMethods.getSelectionModel().select(Params.getMethodDataNormalizedCboIdx(dfltParams.method.name(), expType));
            }else{
                cbMethods.getSelectionModel().select(Params.getMethodCboIdx(dfltParams.method.name(), expType));
            }
            
            txtSigValue.setText("" + dfltParams.sigValue);
            
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> params = null;
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    params = validate(dialog);
                return params;
            });
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return new Params(result.get(), project);
        }
        return null;
    }
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        
        // IF DATA NOT RAW WE DONT SHWO EDGER METHOD
        if(project.data.normalized){
            int midx = Params.getMethodDataNormalizedListIdxByName((String) cbMethods.getSelectionModel().getSelectedItem(), expType);
            results.put(Params.METHOD_PARAM, Params.lstMethodsDataNormalized.get(midx).id);
        }else{
            int midx = Params.getMethodListIdxByName((String) cbMethods.getSelectionModel().getSelectedItem(), expType);
            results.put(Params.METHOD_PARAM, Params.lstMethods.get(midx).id);
        }
        
        if(project.data.isTimeCourseExpType()) {
            // check for a valid R2 cutoff value
            String txt = txtR2Cutoff.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= Params.MIN_R2CUTOFF && val <= Params.MAX_R2CUTOFF) {
                        results.put(Params.R2CUTOFF_PARAM, txt);
                    }
                    else
                        errmsg = "Invalid R^2 cutoff value entered (" + Params.MIN_R2CUTOFF + " to " + Params.MAX_R2CUTOFF + " allowed).";
                } catch(Exception e) {
                    txtR2Cutoff.requestFocus();
                    errmsg = "Invalid R^2 cutoff value number entered.";
                }
            }
            else
                errmsg = "You must enter the R^2 cutoff value.";
            
            if(errmsg.isEmpty()) {
                // check for a valid K value
                txt = txtK.getText().trim();
                if(txt.length() > 0) {
                    try {
                        Double val = Double.parseDouble(txt);
                        if(val >= Params.MIN_K && val <= Params.MAX_K) {
                            results.put(Params.K_PARAM, txt);
                        }
                        else
                            errmsg = "Invalid K clusters value entered (" + Params.MIN_K + " to " + Params.MAX_K + " allowed).";
                    } catch(Exception e) {
                        errmsg = "Invalid K clusters value number entered.";
                    }
                }
                else
                    errmsg = "You must enter the K clusters value.";
                
                if(errmsg.isEmpty()) {
                    results.put(Params.MCLUST_PARAM, chkMclust.isSelected()? "true" : "false");
                    results.put(Params.DEGREE_PARAM, (String) cbReps.getSelectionModel().getSelectedItem());
                }
                else
                    txtK.requestFocus();
            }
            else
                txtR2Cutoff.requestFocus();
        }
        else {
            int ridx = cbReps.getSelectionModel().getSelectedIndex();
            results.put(Params.REPS_PARAM, Params.lstReps.get(ridx).id);
        }
        
        if(errmsg.isEmpty()) {
            // get the data type
            if(rbTrans.isSelected())
                results.put(Params.DATATYPE_PARAM, DataType.TRANS.name());
            else if(rbProteins.isSelected())
                results.put(Params.DATATYPE_PARAM, DataType.PROTEIN.name());
            else
                results.put(Params.DATATYPE_PARAM, DataType.GENE.name());

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
            // check for a valid FC value
            txt = txtFCValue.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= Params.MIN_PVAL_THRESHOLD && val <= Params.MAX_PVAL_FC) {
                        results.put(Params.FC_PARAM, txt);
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
        }
        else
            results.put("ERRMSG", errmsg);
        return results;
    }

    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static final int MIN_K = 1;
        public static final int MAX_K = 30;
        public static int MAX_DEAFULT_DEGREE = 3;
        
        // if case-control experiment
        public static final String REPS_PARAM = "replicates";
        // if time course experiment
        public static final String DEGREE_PARAM = "degree";
        public static final String R2CUTOFF_PARAM = "R2cutoff";
        public static final String K_PARAM = "k";
        public static final String MCLUST_PARAM = "mclust";
        // shared by all
        public static final String METHOD_PARAM = "method";
        public static final String DATATYPE_PARAM = "dataType";
        public static final String SIGVAL_PARAM = "sigval";
        public static final String FC_PARAM = "FC";
        
        private static final DataType dfltDataType = DataType.GENE;
        private static final Replicates dfltReplicates = Replicates.BIOLOGICAL;
        private static final double dfltSigValue = 0.05;
        private static final double dfltFCValue = 2;
        private static final double dfltR2Cutoff = 0.7;
        private static final int dfltK = 9;
        private static final boolean dfltMclust = false;
        Method dfltMethod = null;
        
        public static enum Method {
            // case-control
            EDGER, NOISEQ,
            // time course
            MASIGPRO
        }
        
        private static final List<MethodData> lstMethodsDataNormalized = Arrays.asList(
            new MethodData(Method.EDGER.name(), "edgeR", DataApp.ExperimentType.Two_Group_Comparison),
            new MethodData(Method.NOISEQ.name(), "NOISeq", DataApp.ExperimentType.Two_Group_Comparison),
            new MethodData(Method.MASIGPRO.name(), "maSigPro", DataApp.ExperimentType.Time_Course_Multiple),
            new MethodData(Method.MASIGPRO.name(), "maSigPro", DataApp.ExperimentType.Time_Course_Single)
        );
        
        private static final List<MethodData> lstMethods = Arrays.asList(
            new MethodData(Method.NOISEQ.name(), "NOISeq", DataApp.ExperimentType.Two_Group_Comparison),
            new MethodData(Method.MASIGPRO.name(), "maSigPro", DataApp.ExperimentType.Time_Course_Multiple),
            new MethodData(Method.MASIGPRO.name(), "maSigPro", DataApp.ExperimentType.Time_Course_Single)
        );
        
        public static enum Replicates {
            NONE, TECHNICAL, BIOLOGICAL
        }
        private static final List<EnumData> lstReps = Arrays.asList(
            new EnumData(Replicates.BIOLOGICAL.name(), "Biological"),
            new EnumData(Replicates.TECHNICAL.name(), "Technical")
        );
        
        Method method;
        DataType dataType;
        Replicates replicates;
        int comparison, degree, k;
        double sigValue, R2Cutoff, FCValue;
        boolean mclust;
        DataApp.ExperimentType expType;
        public Params(Project project) {
            expType = project.data.getExperimentType();
            dfltMethod = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? DlgDEAnalysis.Params.Method.NOISEQ : DlgDEAnalysis.Params.Method.MASIGPRO;
            this.method = dfltMethod;
            this.dataType = dfltDataType;
            this.replicates = dfltReplicates;
            this.sigValue = dfltSigValue;
            this.FCValue = dfltFCValue;
            this.degree = Math.min(MAX_DEAFULT_DEGREE, Math.max(1, project.data.getTimePoints() - 1));
            this.R2Cutoff = dfltR2Cutoff;
            this.k = dfltK;
            this.mclust = dfltMclust;
        }
        public Params(HashMap<String, String> hmParams, Project project) {
            expType = project.data.getExperimentType();
            int dfltDegree = Math.min(MAX_DEAFULT_DEGREE, Math.max(1, project.data.getTimePoints() - 1));
            dfltMethod = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? DlgDEAnalysis.Params.Method.NOISEQ : DlgDEAnalysis.Params.Method.MASIGPRO;
            this.method = hmParams.containsKey(METHOD_PARAM)? Method.valueOf(hmParams.get(METHOD_PARAM)) : dfltMethod;
            this.dataType = hmParams.containsKey(DATATYPE_PARAM)? DataType.valueOf(hmParams.get(DATATYPE_PARAM)) : dfltDataType;
            this.replicates = hmParams.containsKey(REPS_PARAM)? Replicates.valueOf(hmParams.get(REPS_PARAM)) : dfltReplicates;
            this.sigValue = hmParams.containsKey(SIGVAL_PARAM)? Double.parseDouble(hmParams.get(SIGVAL_PARAM)) : dfltSigValue;
            this.FCValue = hmParams.containsKey(FC_PARAM)? Double.parseDouble(hmParams.get(FC_PARAM)) : dfltFCValue;
            this.degree = hmParams.containsKey(DEGREE_PARAM)? Integer.parseInt(hmParams.get(DEGREE_PARAM)) : dfltDegree;
            this.R2Cutoff = hmParams.containsKey(R2CUTOFF_PARAM)? Double.parseDouble(hmParams.get(R2CUTOFF_PARAM)) : dfltR2Cutoff;
            this.k = hmParams.containsKey(K_PARAM)? Integer.parseInt(hmParams.get(K_PARAM)) : dfltK;
            this.mclust = hmParams.containsKey(MCLUST_PARAM)? Boolean.valueOf(hmParams.get(MCLUST_PARAM).toLowerCase()) : dfltMclust;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(METHOD_PARAM, method.name());
            hm.put(DATATYPE_PARAM, dataType.name());
            hm.put(REPS_PARAM, replicates.name());
            hm.put(SIGVAL_PARAM, "" + sigValue);
            hm.put(FC_PARAM, "" + FCValue);
            hm.put(DEGREE_PARAM, "" + degree);
            hm.put(R2CUTOFF_PARAM, "" + R2Cutoff);
            hm.put(K_PARAM, "" + k);
            hm.put(MCLUST_PARAM, Boolean.toString(mclust));
            return hm;
        }
        
        //
        // Static functions
        //
        public static Params load(String filepath, Project project) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params, project));
        }
        private static int getMethodCboIdx(String id, DataApp.ExperimentType expType) {
            int idx = 0;
            int size = 0;
            for(MethodData ed : lstMethods) {
                if(ed.expType.equals(expType))
                    size++;
            }
            for(MethodData ed : lstMethods) {
                if(ed.expType.equals(expType)) {
                    if(ed.id.equals(id))
                        break;
                    idx++;
                }
            }
            if(idx >= size)
                idx = 0;
            return idx;
        }
        private static int getMethodListIdxByName(String name, DataApp.ExperimentType expType) {
            int idx = 0;
            for(MethodData ed : lstMethods) {
                if(ed.expType.equals(expType)) {
                    if(ed.name.equals(name))
                        break;
                }
                idx++;
            }
            if(idx >= lstMethods.size())
                idx = 0;
            return idx;
        }
        
        private static int getMethodDataNormalizedCboIdx(String id, DataApp.ExperimentType expType) {
            int idx = 0;
            int size = 0;
            for(MethodData ed : lstMethodsDataNormalized) {
                if(ed.expType.equals(expType))
                    size++;
            }
            for(MethodData ed : lstMethodsDataNormalized) {
                if(ed.expType.equals(expType)) {
                    if(ed.id.equals(id))
                        break;
                    idx++;
                }
            }
            if(idx >= size)
                idx = 0;
            return idx;
        }
        private static int getMethodDataNormalizedListIdxByName(String name, DataApp.ExperimentType expType) {
            int idx = 0;
            for(MethodData ed : lstMethodsDataNormalized) {
                if(ed.expType.equals(expType)) {
                    if(ed.name.equals(name))
                        break;
                }
                idx++;
            }
            if(idx >= lstMethodsDataNormalized.size())
                idx = 0;
            return idx;
        }
        
        private static int getRepsListIdx(String id) {
            int idx = 0;
            for(EnumData ed : lstReps) {
                if(ed.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= lstReps.size())
                idx = 0;
            return idx;
        }
    }
    protected static class MethodData implements Comparable<MethodData> {
        public String id;
        public String name;
        public DataApp.ExperimentType expType;
        
        public MethodData(String id, String name, DataApp.ExperimentType expType) {
            this.id = id;
            this.name = name;
            this.expType = expType;
        }
        @Override
        public int compareTo(MethodData entry) {
            return (id.compareToIgnoreCase(entry.id));
        }
    }
    
}
