/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tappas.DataApp.EnumData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;

import static tappas.DataAnnotation.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgFDAnalysis extends DlgBase {
    Hyperlink lnkClearAll, lnkCheckAll;
    TreeView tvFeaturesPresence, tvFeaturesPos;
    TreeView tv;
    ArrayList<String> lstFeatures;
    ChoiceBox cbTestList, cbUsing;
    TextField txtTestFile, txtName;
    Button btnTestFile;
    Label lblTestSelection;
    RadioButton rbCat, rbId;
    String paramId = "";
    
    public DlgFDAnalysis(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        if(createDialog("FDAParams.fxml", "Functional Diversity Analysis Parameters", true, "Help_Dlg_FDA.html")) {
            if(dfltParams == null)
                dfltParams = new Params();

            // get control objects
            txtName = (TextField) scene.lookup("#txtName");
            tvFeaturesPos = (TreeView) scene.lookup("#tvFeaturesPos");
            tvFeaturesPresence = (TreeView) scene.lookup("#tvFeaturesPresence");
            lnkClearAll = (Hyperlink) scene.lookup("#lnkClearAll");
            lnkCheckAll = (Hyperlink) scene.lookup("#lnkCheckAll");
            cbUsing = (ChoiceBox) scene.lookup("#cbUsing");
            rbId = (RadioButton) scene.lookup("#rbId");
            rbCat = (RadioButton) scene.lookup("#rbCat");
            // select file with header, cause tappAS export tables with #header
            txtTestFile = (TextField) scene.lookup("#txtTestFile");
            btnTestFile = (Button) scene.lookup("#btnTestFile");
            cbTestList = (ChoiceBox) scene.lookup("#cbTestLists");
            lblTestSelection = (Label) scene.lookup("#lblTestSelection");

            // setup listeners and bindings
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
            btnTestFile.setOnAction((event) -> { getTestFile();});
            rbId.setOnAction((event) -> {setIdView(); clearAllFeatures(tvFeaturesPos); clearAllFeatures(tvFeaturesPresence);});
            rbCat.setOnAction((event) -> {setCatView();});
            
            onDataTypeChange();
            
            // set test list
            cbTestList.getSelectionModel().select(Params.getIdxByName(dfltParams.testList.name(), Params.lstTestList));
            
            int idx = 0;
            for(int i = 0; i < cbTestList.getItems().size(); i++) {
                if(((String)cbTestList.getItems().get(i)).startsWith(getCompareNameStart(Params.lstTestList.get(idx).name))){
                    cbTestList.getSelectionModel().select(i);
                    break;
                }
            }
            onTestListChanged();

            cbTestList.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                onTestListChanged();
            });
            
            // set using list
            for(EnumData use : Params.lstUsing) {
                cbUsing.getItems().add(use.name);
            }
            
            cbUsing.getSelectionModel().select(Params.getIdxById(dfltParams.using.name(), Params.lstUsing));

            // populate annotation features
            populateAnnotationFeaturesPresence(tvFeaturesPresence, dfltParams.hmFeatures);
            populateAnnotationFeaturesPositional(tvFeaturesPos, dfltParams.hmFeatures);
            
            //By deafult Genomic Position
            tvFeaturesPresence.disableProperty();
            tvFeaturesPresence.setVisible(false);
            
            if(dfltParams.hmFeatures.isEmpty()){
                checkAllFeatures(tvFeaturesPos);
                checkAllFeatures(tvFeaturesPresence);
            }
            
            //set radio button
            if(dfltParams.method.equals(Params.Method.CATEGORY)){
                rbCat.setSelected(true);
                rbId.setSelected(false);
            }else{
                rbId.setSelected(true);
                rbCat.setSelected(false);
            }
            
            // setup dialog event handlers
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            
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
    
    private void onDataTypeChange() {
        ArrayList<String> lstTest = new ArrayList<>();
        cbTestList.getSelectionModel().clearSelection();
        cbTestList.getItems().clear();
        
        lstTest.add(Params.lstTestList.get(Params.TestList.ALL.ordinal()).name);
        lstTest.add(Params.lstTestList.get(Params.TestList.FROMFILE.ordinal()).name);
        for(String name : lstTest)
            cbTestList.getItems().add(name);
        if(cbTestList.getItems().size() > 0)
            cbTestList.getSelectionModel().select(0);
    }
    
    private void setIdView(){
        //Edit tree to acept just one source
        rbCat.setSelected(false);
    }
    
    private void setCatView(){
        rbId.setSelected(false);
    }
    
    private void onTestListChanged() {
        // must use getSelectedIndex - called from changed listener
        ArrayList<String> lst = new ArrayList<>();
        if(cbTestList.getSelectionModel().getSelectedIndex() != -1) {
            int idx = cbTestList.getSelectionModel().getSelectedIndex();
            String rl = Params.TestList.valueOf(Params.lstTestList.get(idx).id).name();
            switch(rl) {
                case "FROMFILE":
                    lblTestSelection.setDisable(false);
                    txtTestFile.setDisable(false);
                    btnTestFile.setDisable(false);
                    break;
                default:
                    lblTestSelection.setDisable(true);
                    txtTestFile.setDisable(true);
                    btnTestFile.setDisable(true);
                    break;
            }
        }
    }
    
    private void getTestFile() {
        File f = getFileName();
        if(f != null)
            txtTestFile.setText(f.getPath());
    }
    
    private File getFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Select List File"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("List files", "*.txt", "*.tsv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        String expFolder = app.userPrefs.getImportRankedListFEAFolder();
        if(!expFolder.isEmpty()) {
            File f = new File(expFolder);
            if(f.exists() && f.isDirectory())
                fileChooser.setInitialDirectory(f);
            else
                app.userPrefs.setImportRankedListFEAFolder("");
        }
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        if(selectedFile!=null)    
            app.userPrefs.setImportRankedListFEAFolder(selectedFile.getParent());
        return selectedFile;
    }
    
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        System.out.println("Validate dialog");
        
        // get analysis name
        String txt = txtName.getText().trim();
        if(!txt.isEmpty()) {
            // check if not an existing parameter id
            if(paramId.isEmpty()) {
                // check that name is not already used
                ArrayList<DataApp.EnumData> lstFDAParams = project.data.analysis.getFDAParamsList();
                for(DataApp.EnumData data : lstFDAParams) {
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
        
        // get the test list
        int idx = cbTestList.getSelectionModel().getSelectedIndex();
        String lsitId = Params.lstTestList.get(idx).id;
        results.put(Params.TESTLIST_PARAM, lsitId);
        if(lsitId.equals(Params.TestList.FROMFILE.name())) {
            String filepath = txtTestFile.getText().trim();
            errmsg = checkTestListFile(filepath);
            if(errmsg.isEmpty())
                results.put(Params.TESTFROMFILE_PARAM, filepath);
            else
                txtTestFile.requestFocus();
        }
        
        if(errmsg.isEmpty()){
            int midx = cbUsing.getSelectionModel().getSelectedIndex();
            results.put(Params.USING_PARAM, Params.lstUsing.get(midx).id);
        }
        boolean cat = rbCat.isSelected();
        if(errmsg.isEmpty()){
            if(cat)
                results.put(Params.METHOD_PARAM, Params.Method.CATEGORY.name());
            else
                results.put(Params.METHOD_PARAM, Params.Method.ID.name());
        }

        if(cat){
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
        }else{
            //just save one
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
                    if(item.isSelected() || item.isIndeterminate()){
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
                    errmsg = "You must select one annotation source.";
                if(dbcats.size()>1)
                    errmsg = "You must select just one annotation source for ID analysis.";
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
        }
        return results;
    }
    
    private String checkTestListFile(String filepath) {
        String errmsg = "";
        if(!Files.exists(Paths.get(filepath)))
            errmsg = "Specified test list file not found.";
        return errmsg;
    }
    
    private static String getCompareNameStart(String name) {
        int idx = name.indexOf("@");
        if(idx != -1)
            return name.substring(0, idx);
        return name;
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
    
    //
    // Data Classes
    //
    public static class Params extends DlgParams {

        public static enum Using {PRESENCE, GENPOS}
        public static enum Method {CATEGORY, ID}
        public static enum TestList {ALL, FROMFILE}

        private static final List<EnumData> lstTestList = Arrays.asList(
            new EnumData(TestList.ALL.name(), "All genes"),
            new EnumData(TestList.FROMFILE.name(), "Use list of genes from file...")
        );

        private static final List<EnumData> lstMethod = Arrays.asList(
            new EnumData(Method.CATEGORY.name(), "Category"),
            new EnumData(Method.ID.name(), "Id")
        );

        private static final List<EnumData> lstUsing = Arrays.asList(
            new EnumData(Using.GENPOS.name(), "Feature genomic position"),
            new EnumData(Using.PRESENCE.name(), "Feature presence")
        );

        // special features - no spaces allowed, used for table column ID 
        public static final String FEATURE_CDS_LENGTH = CDS_FEATURE;
        public static final String FEATURE_3UTR_LENGTH = UTRLENGTH3_FEATURE;
        public static final String FEATURE_5UTR_LENGTH = UTRLENGTH5_FEATURE;
        public static final String FEATURE_PAS_POSITION = POLYA_FEATURE;
        
        public static final String NAME_PARAM = "name";
        public static final String METHOD_PARAM = "method";
        public static final String USING_PARAM = "using";
        public static final String TESTLIST_PARAM = "testList";
        public static final String TESTFROMFILE_PARAM = "testFromFile";
        public static final String FEATURE_PARAM = "feature";
        public static final int MAX_FEATURES = 99;
        public static int MAX_NAME_LENGTH = 60;
        
        private final Method dfltMethod = Method.CATEGORY;
        private final Using dfltUsing = Using.GENPOS;
        private final TestList dfltTestList = TestList.ALL;

        public String name = "";
        public String paramId = "";
        TestList testList;
        Integer totalFeatures;
        public String testListFilepath = "";
        Method method;
        Using using;
        HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
        
        public Params() {
            name = "";
            this.testList = dfltTestList;
            this.testListFilepath = "";
            this.method = dfltMethod;
            this.using = dfltUsing;
            this.totalFeatures = 0;
        }
        
        public Params(HashMap<String, String> hmParams){
            name = hmParams.containsKey(NAME_PARAM)? hmParams.get(NAME_PARAM) : "";
            this.testList = hmParams.containsKey(TESTLIST_PARAM)? TestList.valueOf(hmParams.get(TESTLIST_PARAM)) : dfltTestList;
            this.testListFilepath = hmParams.containsKey(TESTFROMFILE_PARAM)? hmParams.get(TESTFROMFILE_PARAM) : "";
            this.method = hmParams.containsKey(METHOD_PARAM)? Method.valueOf(hmParams.get(METHOD_PARAM)) : dfltMethod;
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
            }
        }
        
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(NAME_PARAM, name);
            hm.put(TESTLIST_PARAM, testList.name());
            hm.put(TESTFROMFILE_PARAM, testListFilepath.toString());
            hm.put(METHOD_PARAM, method.name());
            hm.put(USING_PARAM, using.name());
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
            return hm;
        }
        
        public String getFeaturesName() {
            String source = "";
            String features = "";
            // NOTE: only single source allowed
            for(String src : hmFeatures.keySet()) {
                source = src;
                HashMap<String, Object> hmSF = hmFeatures.get(src);
                for(String feature : hmSF.keySet())
                    features += (features.isEmpty()? " [" : ", ") + feature;
                features += features.isEmpty()? "" : "]";
                break;
            }
            return source + features;
        }
        //
        // Static functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
        
        private static int getIdxById(String id, List<EnumData> lst) {
            int idx = 0;
            for(EnumData ed : lst) {
                if(id.equals(ed.id))
                    break;
                idx++;
            }
            if(idx >= lst.size())
                idx = 0;
            return idx;
        }
        
        private static int getIdxByName(String name, List<EnumData> lst) {
            int idx = 0;
            for(EnumData entry : lst) {
                String cmpName = getCompareNameStart(entry.name);
                if(name.startsWith(cmpName)) {
                    // "All @" matches "All multiple..." so handle it at end
                    if(idx != 0)
                        break;
                }
                idx++;
            }
            if(idx >= lst.size())
                idx = 0;
            return idx;
        }
    }   
}