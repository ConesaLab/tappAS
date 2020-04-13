/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.stage.Window;
import tappas.DataApp.EnumData;

import java.util.*;

import static tappas.DataAnnotation.POLYA_FEATURE;
import static tappas.DataAnnotation.STRUCTURAL_SOURCE;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class DlgDPAnalysis extends DlgBase {
    TextField txtSigValue, txtFiltValue, txtLength, txtK;
    ChoiceBox cbMethods, cbDegree, cbType, cbFiltering;
    CheckBox chkFilt, chkMclust;
    Label lblDegree, lblFilter, lblFilterBy, lblFilterDefault, lblType, lblK;
    
    ArrayList<String> lstTerms;
    DataApp.ExperimentType expType = project.data.getExperimentType();
    
    public DlgDPAnalysis(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        String fxml = project.data.isTimeCourseExpType()? "DPAParams_TimeCourse.fxml" : "DPAParams.fxml";
        if(createDialog(fxml, "Differential PolyAdenylation Analysis Parameters", true, "Help_Dlg_DPA.html")) {
            if(dfltParams == null)
                dfltParams = new Params(project);

            // get control objects
            txtSigValue = (TextField) scene.lookup("#txtThreshold");
            txtLength = (TextField) scene.lookup("#txtLength");
            cbMethods = (ChoiceBox) scene.lookup("#cbMethods");
            
            cbDegree = (ChoiceBox) scene.lookup("#cbDegree");
            cbType = (ChoiceBox) scene.lookup("#cbType");
            lblDegree = (Label) scene.lookup("#lblDegree");
            lblType = (Label) scene.lookup("#lblType");
            
            txtFiltValue = (TextField) scene.lookup("#txtFiltValue");
            lblFilter = (Label) scene.lookup("#lblFilter");
            lblFilterBy = (Label) scene.lookup("#lblFilterBy");
            lblFilterDefault = (Label) scene.lookup("#lblFilterDefault");
            chkFilt = (CheckBox) scene.lookup("#chkFilt");
            cbFiltering = (ChoiceBox) scene.lookup("#cbFiltering");

            lblK = (Label) scene.lookup("#lblK");
            txtK = (TextField) scene.lookup("#txtK");
            chkMclust = (CheckBox) scene.lookup("#chkMclust");
            
            // setup dialog
            setProjectName();
            for(MethodData method : Params.lstMethods){
                if(method.expType.equals(expType))
                    cbMethods.getItems().add(method.name);
            }

            cbMethods.getSelectionModel().select(Params.getMethodCboIdx(dfltParams.method.name(), expType));
            txtSigValue.setText("" + dfltParams.sigValue);
            txtLength.setText("" + dfltParams.lengthValue);
            int maxDegree = Math.max(1, project.data.getTimePoints() - 1);
            
            //Visible options - CaseControl is another fxml
            //all
            if(dfltParams.filter) {
                    chkFilt.setSelected(true);
            }else{
                    chkFilt.setSelected(false);
            }
            txtFiltValue.setText("" + dfltParams.filterFC);
            txtFiltValue.setVisible(true);
            lblFilter.setVisible(true);
            lblFilterBy.setVisible(true);
            lblFilterDefault.setVisible(true);
            cbFiltering.setVisible(true);

            txtFiltValue.disableProperty().bind(chkFilt.selectedProperty().not());
            lblFilterBy.disableProperty().bind(chkFilt.selectedProperty().not());
            cbFiltering.disableProperty().bind(chkFilt.selectedProperty().not());

            for(EnumData tp : Params.lstFiltering)
                cbFiltering.getItems().add(tp.name);
            cbFiltering.getSelectionModel().select(Params.getFilterCboIdx(dfltParams.filteringType.name()));
            lblFilterBy.setText(dfltParams.filteringType.name().equals("FOLD")? "Fold expression difference:" : "Prop. expression difference:");
            
            cbFiltering.valueProperty().addListener((obs, oldValue, newValue) -> {
                System.out.println(newValue);
                lblFilterBy.setText(newValue.equals("Fold filtering")? "Fold expression difference:" : "Prop. expression difference:");
                lblFilterDefault.setText(newValue.equals("Fold filtering")? "(default: 2)" : "(default: 0.2)");
                txtFiltValue.promptTextProperty().set(newValue.equals("Fold filtering")? "1.0 to 99.0" : "0.01 to 0.99");
                txtFiltValue.setText(newValue.equals("Fold filtering")? "2.0" : "0.2");
            });
            
            if(project.data.isTimeCourseExpType()) {
                
                cbDegree.setVisible(true);
                lblDegree.setVisible(true);
                lblK.setVisible(true);
                txtK.setVisible(true);
                chkMclust.setVisible(true);
                
                txtK.setText("" + dfltParams.k);
                chkMclust.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                    lblK.setText(newValue? "Max K clusters:" : "K clusters:");
                });
                
                for(int i = 1; i <= maxDegree; i++)
                    cbDegree.getItems().add("" + i);
                if((dfltParams.degree - 1) < maxDegree)
                    cbDegree.getSelectionModel().select(dfltParams.degree - 1);
                else
                    cbDegree.getSelectionModel().select(maxDegree - 1);
            }
            if(project.data.isSingleTimeSeriesExpType()) {
                cbType.setVisible(false);
                lblType.setVisible(false);
            }else if(project.data.isMultipleTimeSeriesExpType()){
                cbType.setVisible(true);
                lblType.setVisible(true);
                for(EnumData tp : Params.lstType)
                    cbType.getItems().add(tp.name);
                cbType.getSelectionModel().select(Params.getTypeListIdx(dfltParams.strictType.name(), Params.lstType));
            }

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
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return(new Params(result.get(), project));
        }
        return null;
    }
    
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        int tidx;
        // there is no way to screw up the choice boxes - must select a value
        // so all we need to do is check for a valid threshold value
        int idx = Params.getMethodListIdxByName((String) cbMethods.getSelectionModel().getSelectedItem(), expType);
        results.put(Params.METHOD_PARAM, Params.lstMethods.get(idx).id);
        String txt = "";
        // Course time
        if(project.data.isTimeCourseExpType()) {
            if(errmsg.isEmpty()){
                results.put(Params.DEGREE_PARAM, (String) cbDegree.getSelectionModel().getSelectedItem());
                if(project.data.isMultipleTimeSeriesExpType()){
                    tidx = Params.getTypeListByName((String) cbType.getSelectionModel().getSelectedItem(), Params.lstType);
                    results.put(Params.STRICTTYPE_PARAM, Params.lstType.get(tidx).id);
                }
            }
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
                }
                else
                    txtK.requestFocus();
            }
        }
        //All
        //filter
        results.put(Params.FILTER_PARAM, chkFilt.isSelected()? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        if(chkFilt.isSelected()) {
            txt = txtFiltValue.getText().trim();
            if(txt.length() > 0) {
                try {
                    tidx = Params.getTypeListByName((String) cbFiltering.getSelectionModel().getSelectedItem(), Params.lstFiltering);
                    results.put(Params.FILTERINGTYPE_PARAM, Params.lstFiltering.get(tidx).id);
                    Double val = Double.parseDouble(txt);
                    if(Params.lstFiltering.get(tidx).id.equals("FOLD")){
                        if(val >= Params.MIN_FILTER && val <= Params.MAX_FILTER) {
                            results.put(Params.FILTERFC_PARAM, txt);
                        }
                        else
                            errmsg = "Invalid filter value entered (" + Params.MIN_FILTER + " to " + Params.MAX_FILTER + " allowed).";
                    }else if(val >= Params.MIN_FILTER_PROP && val <= Params.MAX_FILTER_PROP){
                        results.put(Params.FILTERFC_PARAM, txt);
                    }else
                        errmsg = "Invalid filter value entered (" + Params.MIN_FILTER_PROP + " to " + Params.MAX_FILTER_PROP + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid filter value number entered.";
                }
            }
            else
                errmsg = "You must enter a filter value.";
            if(!errmsg.isEmpty()) {
                txtFiltValue.requestFocus();
                results.put("ERRMSG", errmsg);
            }
        }else{
            results.put(Params.FILTERFC_PARAM, "0");
        }
        //sigValue
        txt = txtSigValue.getText().trim();
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
        if(!errmsg.isEmpty()) {txtSigValue.requestFocus();
            results.put("ERRMSG", errmsg);
        }else{
            txt = txtLength.getText().trim();
            if(txt.length() > 0) {
                try {
                    int val = Integer.parseInt(txt);
                    if(val >= Params.MIN_PVAL_LENGTH && val <= Params.MAX_PVAL_LENGTH) {
                        results.put(Params.LENGTH_PARAM, txt);
                    }
                    else
                        errmsg = "Invalid length value entered (" + Params.MIN_PVAL_LENGTH + " to " + Params.MAX_PVAL_LENGTH + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid length value number entered.";
                }
            }
        else
            errmsg = "You must enter a length value.";
        }
        return results;
    }

    public static class Params extends DlgParams {
        public static enum Method {
            // case-control
            DEXSEQ, //EDGER, 
            // time course
            MASIGPRO
        }
        private static final List<MethodData> lstMethods = Arrays.asList(
            //new MethodData(Method.EDGER.name(), "edgeR", DataApp.ExperimentType.Two_Group_Comparison),
            new MethodData(Method.DEXSEQ.name(), "DEXSeq", DataApp.ExperimentType.Two_Group_Comparison),
            new MethodData(Method.MASIGPRO.name(), "maSigPro", DataApp.ExperimentType.Time_Course_Multiple),
            new MethodData(Method.MASIGPRO.name(), "maSigPro", DataApp.ExperimentType.Time_Course_Single)
        );
        
        public static enum StrictType {
            // time course
            LESSSTRICT, MORESTRICT
        }
        
        public static enum FilteringType {
            FOLD, PROP
        }
        
        private static final List<EnumData> lstType = Arrays.asList(
            new EnumData(StrictType.LESSSTRICT.name(), "Less strict"),
            new EnumData(StrictType.MORESTRICT.name(), "More strict")
        );
        
        private static final List<EnumData> lstFiltering = Arrays.asList(
            new EnumData(FilteringType.FOLD.name(), "Fold filtering"),
            new EnumData(FilteringType.PROP.name(), "Proportion filtering")
        );

        // if time course experiment
        public static final int MIN_K = 1;
        public static final int MAX_K = 30;
        public static final String K_PARAM = "k";
        public static final String MCLUST_PARAM = "mclust";
        private static final int dfltK = 9;
        private static final boolean dfltMclust = false;
        
        public static int MAX_DEAFULT_DEGREE = 3;
        private static final String DEGREE_PARAM = "polynomial";
        
        FilteringType dfltFilteringType = FilteringType.FOLD;
        private static final String FILTERINGTYPE_PARAM = "filteringType";
        private static final String FILTER_PARAM = "filter";
        private static final String FILTERFC_PARAM = "filter_FC";
        public static double MIN_FILTER = 1.0;
        public static double MAX_FILTER = 99.0;
        public static double MIN_FILTER_PROP = 0.01;
        public static double MAX_FILTER_PROP = 0.99;
        private final double dfltFiltFCValue = 2.0;
        // if multiple time course experiment
        private static final String STRICTTYPE_PARAM = "strictType";
        
        // shared by all
        private static final String METHOD_PARAM = "method";
        public static final String USING_PARAM = "using";
        private static final String SIGVAL_PARAM = "sigval";
        private static final String LENGTH_PARAM = "length";
        public static final String FEATURE_PARAM = "feature";
        
        //SourceFeature
        public static final String source = STRUCTURAL_SOURCE;
        public static final String feature = POLYA_FEATURE;
        
        public String getSource(){return this.source;}
        public String getFeature(){return this.feature;}
        
        final static int MIN_PVAL_LENGTH = 1;
        final static int MAX_PVAL_LENGTH = 160;
        
        public static final int MAX_FEATURES = 99;
        private Method dfltMethod = Method.DEXSEQ;
        private double dfltSigValue = 0.05;
        private int dfltLengthValue = 60;
        
        Method method;
        StrictType strictType;
        double sigValue;
        int lengthValue, degree, k;
        boolean filter, mclust;
        double filterFC;
        FilteringType filteringType;
        DataApp.ExperimentType expType;
        
        public Params(Project project) {            
            this.expType = project.data.getExperimentType();
            boolean dfltFilter = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? false : true;
            Method dfltMethod = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? DlgDPAnalysis.Params.Method.DEXSEQ : DlgDPAnalysis.Params.Method.MASIGPRO;
            StrictType dfltStrictType = expType.equals(DataApp.ExperimentType.Time_Course_Multiple)? StrictType.LESSSTRICT : null;
            int dfltDegree = Math.min(MAX_DEAFULT_DEGREE, Math.max(1, project.data.getTimePoints() - 1));
            FilteringType dfltFilteringType = DlgDPAnalysis.Params.FilteringType.FOLD;
            
            this.method = dfltMethod;
            this.sigValue = dfltSigValue;
            this.lengthValue = dfltLengthValue;
            this.degree = dfltDegree;
            
            this.filter = dfltFilter;
            this.filterFC = dfltFiltFCValue;
            this.filteringType = dfltFilteringType;
            this.strictType = dfltStrictType;
            
            this.k = dfltK;
            this.mclust = dfltMclust;
        }
        public Params(HashMap<String, String> hmParams, Project project) {
            this.expType = project.data.getExperimentType();
            boolean dfltFilter = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? false : true;
            Method dfltMethod = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? DlgDPAnalysis.Params.Method.DEXSEQ : DlgDPAnalysis.Params.Method.MASIGPRO;
            StrictType dfltStrictType = expType.equals(DataApp.ExperimentType.Time_Course_Multiple)? StrictType.LESSSTRICT : null;
            int dfltDegree = Math.min(MAX_DEAFULT_DEGREE, Math.max(1, project.data.getTimePoints() - 1));
            FilteringType dfltFilteringType = DlgDPAnalysis.Params.FilteringType.FOLD;
            
            if(hmParams.isEmpty()) {
                this.filter = dfltFilter;
                this.filterFC = dfltFiltFCValue;
                this.filteringType = dfltFilteringType;
                this.degree = dfltDegree;
                this.strictType = dfltStrictType;
                this.method = dfltMethod;
                this.sigValue = dfltSigValue;
                this.lengthValue = dfltLengthValue;
                this.k = dfltK;
                this.mclust = dfltMclust;
            }
            else {
                this.filter = hmParams.containsKey(FILTER_PARAM)? Boolean.valueOf(hmParams.get(FILTER_PARAM)) : dfltFilter;
                this.filterFC = hmParams.containsKey(FILTERFC_PARAM)? Double.parseDouble(hmParams.get(FILTERFC_PARAM)) : dfltFiltFCValue;
                this.filteringType = hmParams.containsKey(FILTERINGTYPE_PARAM)? FilteringType.valueOf(hmParams.get(FILTERINGTYPE_PARAM)) : dfltFilteringType;
                this.degree = hmParams.containsKey(DEGREE_PARAM)? Integer.parseInt(hmParams.get(DEGREE_PARAM)) : dfltDegree;
                this.strictType = hmParams.containsKey(STRICTTYPE_PARAM)? StrictType.valueOf(hmParams.get(STRICTTYPE_PARAM)) : null;
                // these parameters must be provided by all if HashMap has values - exception otherwise
                this.method = Method.valueOf(hmParams.get(METHOD_PARAM));
                this.lengthValue = hmParams.containsKey(LENGTH_PARAM)? Integer.parseInt(hmParams.get(LENGTH_PARAM)) : dfltLengthValue;
                this.sigValue = Double.parseDouble(hmParams.get(SIGVAL_PARAM));
                this.k = hmParams.containsKey(K_PARAM)? Integer.parseInt(hmParams.get(K_PARAM)) : dfltK;
                this.mclust = hmParams.containsKey(MCLUST_PARAM)? Boolean.valueOf(hmParams.get(MCLUST_PARAM).toLowerCase()) : dfltMclust;
        }
        }

        public HashMap<String, String> getParams(Project project) {
            HashMap<String, String> hm = new HashMap<>();
            // get shared parameters
            hm.put(METHOD_PARAM, method.name());
            hm.put(SIGVAL_PARAM, "" + sigValue);
            hm.put(LENGTH_PARAM, "" + lengthValue);
            hm.put(FILTER_PARAM, "" + Boolean.toString(filter));
            if(filter)
                hm.put(FILTERFC_PARAM, "" + filterFC);
            if(filteringType != null)
                hm.put(FILTERINGTYPE_PARAM, "" + filteringType.name());
            // only get parameters if relevant
            if(project.data.isTimeCourseExpType()) {
                hm.put(DEGREE_PARAM, "" + degree);
                if(project.data.isMultipleTimeSeriesExpType()) {
                    if(strictType != null)
                        hm.put(STRICTTYPE_PARAM, "" + strictType.name());
                }
            hm.put(K_PARAM, "" + k);
            hm.put(MCLUST_PARAM, Boolean.toString(mclust));
            }
            return hm;
        }
        
        // add save function to only save relevant experiment type parameters
        public boolean save(String filepath, Project project) {
            return Utils.saveParams(getParams(project), filepath);
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
        
        private static int getFilterCboIdx(String id) {
            int idx = 0;
            for(EnumData ed : lstFiltering) {
                if(ed.id.equals(id)){
                    break;
                }
                idx++;
            }
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
        
        private static int getTypeListByName(String id, List<EnumData> lst) {
            int idx = 0;
            for(EnumData ed : lst) {
                if(ed.name.equals(id))
                    break;
                idx++;
            }
            if(idx >= lst.size())
                idx = 0;
            return idx;
        }
        
        private static int getTypeListIdx(String id, List<EnumData> lst) {
            int idx = 0;
            for(EnumData ed : lst) {
                if(ed.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= lst.size())
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
