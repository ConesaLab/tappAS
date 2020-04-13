/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.stage.Window;
import tappas.DataApp.EnumData;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgDFIAnalysis extends DlgBase {
    TextField txtSigValue, txtFiltValue, txtName;
    ChoiceBox cbMethods, cbUsing, cbDegree, cbType, cbFiltering;
    TreeView tvFeaturesPos, tvFeaturesPresence;
    TreeView tv;
    Hyperlink lnkClearAll, lnkCheckAll;
    CheckBox chkFilt;
    Label lblDegree, lblFilter, lblFilterBy, lblFilterDefault, lblType;
    String paramId = "";
    
    ArrayList<String> lstTerms;
    DataApp.ExperimentType expType = project.data.getExperimentType();
    
    public DlgDFIAnalysis(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        String fxml = project.data.isTimeCourseExpType()? "DFIParams_TimeCourse.fxml" : "DFIParams.fxml";
        if(createDialog(fxml, "Differential Feature Inclusion Analysis Parameters", true, "Help_Dlg_DFI.html")) {
            if(dfltParams == null)
                dfltParams = new Params(project);

            // get control objects
            txtName = (TextField) scene.lookup("#txtName");
            txtSigValue = (TextField) scene.lookup("#txtThreshold");
            cbMethods = (ChoiceBox) scene.lookup("#cbMethods");
            lnkClearAll = (Hyperlink) scene.lookup("#lnkClearAll");
            lnkCheckAll = (Hyperlink) scene.lookup("#lnkCheckAll");
            tvFeaturesPos = (TreeView) scene.lookup("#tvFeaturesPos");
            tvFeaturesPresence = (TreeView) scene.lookup("#tvFeaturesPresence");
            
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

            //Set analysis name
            UnaryOperator<TextFormatter.Change> filter = (TextFormatter.Change change) -> {
                if (change.getControlNewText().length() > Params.MAX_NAME_LENGTH) {
                    showDlgMsg("Name may not exceed " + Params.MAX_NAME_LENGTH + " characters");
                    return null;
                } else {
                    showDlgMsg("");
                    return change;
                }
            };   
            txtName.setTextFormatter(new TextFormatter(filter));
            txtName.setText(dfltParams.name);
            paramId = dfltParams.paramId;
            
            // setup dialog
            setProjectName();
            for(MethodData method : Params.lstMethods){
                if(method.expType.equals(expType))
                    cbMethods.getItems().add(method.name);
            }
            
            cbUsing = (ChoiceBox) scene.lookup("#cbUsing");
            for(EnumData use : Params.lstUsing) {
                cbUsing.getItems().add(use.name);
            }
            cbMethods.getSelectionModel().select(Params.getMethodCboIdx(dfltParams.method.name(), expType));
            cbUsing.getSelectionModel().select(Params.getUsingListIdx(dfltParams.using.name()));
            txtSigValue.setText("" + dfltParams.sigValue);
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
            
            if(dfltParams.filteringType.name().equals("PROP"))
                lblFilterDefault.setText("(default: 0.2)");

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
                cbType.getSelectionModel().select(Params.getTypeListIdx(dfltParams.strictType.name()));
            }
            
            // setup database categories
            lnkClearAll.setOnAction((event) -> {
                if(tvFeaturesPos.isDisable()) 
                    clearAllFeatures(tvFeaturesPresence);
                else
                    clearAllFeatures(tvFeaturesPos);
            });
            
            lnkCheckAll.setOnAction((event) -> {
                if(tvFeaturesPos.isDisable())
                    checkAllFeatures(tvFeaturesPresence);
                else
                    checkAllFeatures(tvFeaturesPos); 
            });
            
            // we have to load just positional annotation
            populateAnnotationFeaturesPositional(tvFeaturesPos, dfltParams.hmFeatures);
            populateAnnotationFeaturesPresence(tvFeaturesPresence, dfltParams.hmFeatures);

            //By deafult Genomic Position
            tvFeaturesPresence.disableProperty();
            tvFeaturesPresence.setVisible(false);
            
            if(dfltParams.hmFeatures.isEmpty()){
                checkAllFeatures(tvFeaturesPos);
                checkAllFeatures(tvFeaturesPresence);
            }
            
            int midx1 = cbUsing.getSelectionModel().getSelectedIndex();
                String selected1 = Params.lstUsing.get(midx1).id;
                if(selected1.equals(Params.lstUsing.get(0).id)){
                    loadPositionalFeatures();
                }else{
                    loadPresenceFeatures();
            }
                
            cbUsing.setOnAction((event) -> {
                int midx = cbUsing.getSelectionModel().getSelectedIndex();
                String selected = Params.lstUsing.get(midx).id;
                if(selected.equals(Params.lstUsing.get(0).id)){
                    loadPositionalFeatures();
                }else{
                    loadPresenceFeatures();
                }
            });
            
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
    
    //
    // Internal Functions
    //
    private void loadPositionalFeatures(){
        tvFeaturesPos.setDisable(false);
        tvFeaturesPresence.setVisible(false);
        tvFeaturesPresence.setDisable(true);
        tvFeaturesPos.setVisible(true);
    }
    
    private void loadPresenceFeatures(){
        tvFeaturesPresence.setDisable(false);
        tvFeaturesPos.setVisible(false);
        tvFeaturesPos.setDisable(true);
        tvFeaturesPresence.setVisible(true);
    }
    
    private void clearAllFeatures(TreeView aux) {
        TreeItem<String> rootItem = aux.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            ObservableList<TreeItem<String>> sublst = item.getChildren();
            for(TreeItem subti : sublst) {
                CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                subitem.setSelected(false);
            }
            item.setSelected(false);
        }
    }
    
    private void checkAllFeatures(TreeView aux) {
        TreeItem<String> rootItem = aux.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            ObservableList<TreeItem<String>> sublst = item.getChildren();
            for(TreeItem subti : sublst) {
                CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                subitem.setSelected(true);
            }
            item.setSelected(true);
        }
    }
    
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        
        // there is no way to screw up the choice boxes - must select a value
        // so all we need to do is check for a valid threshold value
        int tidx;
        int idx = cbUsing.getSelectionModel().getSelectedIndex();
        results.put(Params.USING_PARAM, Params.lstUsing.get(idx).id);
        idx = Params.getMethodListIdxByName((String) cbMethods.getSelectionModel().getSelectedItem(), expType);
        results.put(Params.METHOD_PARAM, Params.lstMethods.get(idx).id);
        
        // get analysis name
        String txt = txtName.getText().trim();
        if(!txt.isEmpty()) {
            // check if not an existing parameter id
            if(paramId.isEmpty()) {
                // check that name is not already used
                ArrayList<DataApp.EnumData> lstDFIParams = project.data.analysis.getDFIParamsList();
                for(DataApp.EnumData data : lstDFIParams) {
                    if(txt.toLowerCase().equals(data.name.toLowerCase())) {
                        errmsg = "The name specified is already in use.";
                        break;
                    }
                }
            }
            if(errmsg.isEmpty())
                results.put(Params.NAME_PARAM, txt);
        }
        else
            errmsg = "You must provide a name for the analysis.";
        if(!errmsg.isEmpty()) {
            txtName.requestFocus();
            results.put("ERRMSG", errmsg);
            return results;
        }
        
        // Course time
        if(project.data.isTimeCourseExpType()) {
            results.put(Params.FILTER_PARAM, chkFilt.isSelected()? Boolean.TRUE.toString() : Boolean.FALSE.toString());
            if(chkFilt.isSelected()) {
                txt = txtFiltValue.getText().trim();
                if(txt.length() > 0) {
                    try {
                        Double val = Double.parseDouble(txt);
                        if(val >= Params.MIN_FILTER && val <= Params.MAX_FILTER) {
                            results.put(Params.FILTERFC_PARAM, txt);
                        }
                        else
                            errmsg = "Invalid filter value entered (" + Params.MIN_FILTER + " to " + Params.MAX_FILTER + " allowed).";
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
            }
            if(errmsg.isEmpty()){
                results.put(Params.DEGREE_PARAM, (String) cbDegree.getSelectionModel().getSelectedItem());
                if(project.data.isMultipleTimeSeriesExpType()){
                    tidx = Params.getTypeListByName((String) cbType.getSelectionModel().getSelectedItem());
                    results.put(Params.STRICTTYPE_PARAM, Params.lstType.get(tidx).id);
                }
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
        if(errmsg.isEmpty()) {
            if(tvFeaturesPos.isDisable()){
                tv = tvFeaturesPresence;
            }else{
                tv = tvFeaturesPos;
            }
            String term;
            ArrayList<String> dbcats = new ArrayList<>();
            TreeItem<String> rootItem = tv.getRoot();
            ObservableList<TreeItem<String>> lst = rootItem.getChildren();
            for(TreeItem ti : lst) {
                term = "";
                CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
                if(item.isSelected() && !item.isIndeterminate()) {
                    term = item.getValue();
                    dbcats.add(term);
                    System.out.println("Source: " + item.getValue());
                }
                else {
                    ObservableList<TreeItem<String>> sublst = item.getChildren();
                    for(TreeItem subti : sublst) {
                        CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                        if(subitem.isSelected()) {
                            if(term.isEmpty())
                                term = item.getValue();
                            term += "\t" + subitem.getValue();
                        }
                    }
                    if(!term.isEmpty())
                        dbcats.add(term);
                }
            }
            if(dbcats.isEmpty())
                errmsg = "You must select at least one annotation feature.";
            if(errmsg.isEmpty()) {
                    int termnum = 1;
                    for(String ieterm : dbcats)
                        results.put(Params.FEATURE_PARAM + termnum++, ieterm);
            }
            else {
                tv.requestFocus();
                results.put("ERRMSG", errmsg);
            }
        }
        else {
            txtSigValue.requestFocus();
            results.put("ERRMSG", errmsg);
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
        
        public static enum FilteringType {
            FOLD, PROP
        }
        
        public static enum StrictType {
            // time course
            LESSSTRICT, MORESTRICT
        }
        
        private static final List<EnumData> lstType = Arrays.asList(
            new EnumData(StrictType.LESSSTRICT.name(), "Less strict"),
            new EnumData(StrictType.MORESTRICT.name(), "More strict")
        );
        
        private static final List<EnumData> lstFiltering = Arrays.asList(
            new EnumData(FilteringType.FOLD.name(), "Fold filtering"),
            new EnumData(FilteringType.PROP.name(), "Proportion filtering")
        );
        
        public static enum Using {
            NEW_GENPOS, PRESENCE//, GENPOS
        }
        private static final List<EnumData> lstUsing = Arrays.asList(
                
            new EnumData(Using.NEW_GENPOS.name(), "Feature genomic position"),
            new EnumData(Using.PRESENCE.name(), "Feature presence")
            //new EnumData(Using.GENPOS.name(), "Feature genomic position overlap")
        );

        
         // if time course experiment
        private static final String DEGREE_PARAM = "polynomial";
        public static int MAX_DEAFULT_DEGREE = 3;
        // if multiple time course experiment
        private static final String STRICTTYPE_PARAM = "strictType";
        // shared by all
        public static int MAX_NAME_LENGTH = 60;
        FilteringType dfltFilteringType = FilteringType.FOLD;
        private static final String FILTERINGTYPE_PARAM = "filteringType";
        private static final String FILTER_PARAM = "filter";
        private static final String FILTERFC_PARAM = "filter_FC";
        private static final String  TOTAL_FEATURES_PARAM = "totalFeatures";
        public static double MIN_FILTER = 1.0;
        public static double MAX_FILTER = 99.0;
        public static double MIN_FILTER_PROP = 0.01;
        public static double MAX_FILTER_PROP = 0.99;
        private final double dfltFiltFCValue = 2.0;
        
        public static final String NAME_PARAM = "name";
        private static final String METHOD_PARAM = "method";
        public static final String USING_PARAM = "using";
        private static final String SIGVAL_PARAM = "sigval";
        public static final String FEATURE_PARAM = "feature";
        
        public static final int MAX_FEATURES = 99;
        private Method dfltMethod = Method.DEXSEQ;
        private Using dfltUsing = Using.NEW_GENPOS;
        private double dfltSigValue = 0.05;
        
        public String name = "";
        public String paramId = "";
        Method method;
        Using using;
        StrictType strictType;
        double sigValue;
        int degree, totalFeatures;
        boolean filter;
        double filterFC;
        FilteringType filteringType;
        DataApp.ExperimentType expType;
        HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
        
        public Params(Project project) {
            this.expType = project.data.getExperimentType();
            boolean dfltFilter = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? false : true;            Method dfltMethod = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? DlgDFIAnalysis.Params.Method.DEXSEQ : DlgDFIAnalysis.Params.Method.MASIGPRO;
            StrictType dfltStrictType = expType.equals(DataApp.ExperimentType.Time_Course_Multiple)? StrictType.LESSSTRICT : null;
            int dfltDegree = Math.min(MAX_DEAFULT_DEGREE, Math.max(1, project.data.getTimePoints() - 1));
            FilteringType dfltFilteringType = DlgDFIAnalysis.Params.FilteringType.FOLD;
            
            name = "";
            this.method = dfltMethod;
            this.using = dfltUsing;
            this.sigValue = dfltSigValue;
            
            this.filter = dfltFilter;
            this.filterFC = dfltFiltFCValue;
            this.filteringType = dfltFilteringType;
            this.strictType = dfltStrictType;
            
            this.degree = dfltDegree;
            this.strictType = dfltStrictType;
            
            this.totalFeatures = 0;
        }
        public Params(HashMap<String, String> hmParams, Project project) {
            this.expType = project.data.getExperimentType();
            boolean dfltFilter = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? false : true;
            Method dfltMethod = expType.equals(DataApp.ExperimentType.Two_Group_Comparison)? DlgDFIAnalysis.Params.Method.DEXSEQ : DlgDFIAnalysis.Params.Method.MASIGPRO;
            StrictType dfltStrictType = expType.equals(DataApp.ExperimentType.Time_Course_Multiple)? StrictType.LESSSTRICT : null;
            int dfltDegree = Math.min(MAX_DEAFULT_DEGREE, Math.max(1, project.data.getTimePoints() - 1));
            FilteringType dfltFilteringType = DlgDFIAnalysis.Params.FilteringType.FOLD;
            
            if(hmParams.isEmpty()) {
                this.name = "";
                this.filter = dfltFilter;
                this.filterFC = dfltFiltFCValue;
                this.filteringType = dfltFilteringType;
                this.degree = dfltDegree;
                this.strictType = dfltStrictType;
                this.method = dfltMethod;
                this.using = dfltUsing;
                this.sigValue = dfltSigValue;
                this.hmFeatures = new HashMap<>();
            }
            else {
                name = hmParams.containsKey(NAME_PARAM)? hmParams.get(NAME_PARAM) : "";
                this.filter = hmParams.containsKey(FILTER_PARAM)? Boolean.valueOf(hmParams.get(FILTER_PARAM)) : dfltFilter;
                this.filterFC = hmParams.containsKey(FILTERFC_PARAM)? Double.parseDouble(hmParams.get(FILTERFC_PARAM)) : dfltFiltFCValue;
                this.filteringType = hmParams.containsKey(FILTERINGTYPE_PARAM)? FilteringType.valueOf(hmParams.get(FILTERINGTYPE_PARAM)) : dfltFilteringType;
                this.degree = hmParams.containsKey(DEGREE_PARAM)? Integer.parseInt(hmParams.get(DEGREE_PARAM)) : dfltDegree;
                this.strictType = hmParams.containsKey(STRICTTYPE_PARAM)? StrictType.valueOf(hmParams.get(STRICTTYPE_PARAM)) : null;
                this.using = hmParams.containsKey(USING_PARAM)? Using.valueOf(hmParams.get(USING_PARAM)) : dfltUsing;
                this.hmFeatures = new HashMap<>();
                this.totalFeatures = 0;
                for(int t = 1; t < MAX_FEATURES; t++) {
                    if(hmParams.containsKey(FEATURE_PARAM + t)) {
                        String[] fields = hmParams.get(FEATURE_PARAM + t).trim().split("\t");
                        if(fields.length > 0) {
                            this.totalFeatures = t;
                            HashMap<String, Object> hm = new HashMap<>();
                            hmFeatures.put(fields[0].trim(), hm);
                            for(int i = 1; i < fields.length; i++)
                                hm.put(fields[i].trim(), null);
                        }
                    }
                    else
                        break;
                }
                // these parameters must be provided by all if HashMap has values - exception otherwise
                this.method = Method.valueOf(hmParams.get(METHOD_PARAM));
                this.sigValue = Double.parseDouble(hmParams.get(SIGVAL_PARAM));
            }
        }

        public HashMap<String, String> getParams(Project project) {
            HashMap<String, String> hm = new HashMap<>();
            // get shared parameters
            hm.put(NAME_PARAM, name);
            hm.put(METHOD_PARAM, method.name());
            hm.put(USING_PARAM, using.name());
            hm.put(SIGVAL_PARAM, "" + sigValue);
            hm.put(FILTER_PARAM, "" + Boolean.toString(filter));
            hm.put(TOTAL_FEATURES_PARAM, "" + totalFeatures);
            if(filter)
                hm.put(FILTERFC_PARAM, "" + filterFC);
            if(filteringType != null)
                hm.put(FILTERINGTYPE_PARAM, "" + filteringType.name());
            int t = 1;
            for(String db : hmFeatures.keySet()) {
                String features = db;
                HashMap<String, Object> hmCats = hmFeatures.get(db);
                for(String cat : hmCats.keySet())
                    features += "\t" + cat;
                hm.put(FEATURE_PARAM + t, features);
                if(++t > MAX_FEATURES)
                    break;
            }
            // only get parameters if relevant
            if(project.data.isTimeCourseExpType()) {
                hm.put(FILTER_PARAM, "" + Boolean.toString(filter));
                if(filter)
                    hm.put(FILTERFC_PARAM, "" + filterFC);
                hm.put(DEGREE_PARAM, "" + degree);
                if(project.data.isMultipleTimeSeriesExpType()) {
                    if(strictType != null)
                        hm.put(STRICTTYPE_PARAM, "" + strictType.name());
                }
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
                
        private static int getTypeListIdx(String id) {
            int idx = 0;
            for(EnumData ed : lstType) {
                if(ed.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= lstType.size())
                idx = 0;
            return idx;
        }
        private static int getTypeListByName(String id) {
            int idx = 0;
            for(EnumData ed : lstType) {
                if(ed.name.equals(id))
                    break;
                idx++;
            }
            if(idx >= lstType.size())
                idx = 0;
            return idx;
        }
        private static int getUsingListIdx(String id) {
            int idx = 0;
            for(EnumData ed : lstUsing) {
                if(ed.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= lstUsing.size())
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
