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
import tappas.DataApp.DataType;
import tappas.DataApp.EnumData;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgFEAnalysis extends DlgBase {
    Label lblCompare, lblOverlap, lblVS, lblTG1, lblTG2;
    Label lblItemsList, lblTestSelection;
    Label lblBkgndSelection;
    TextField txtName, txtSigValue, txtSamplingCnt;
    ChoiceBox cbCompare, cbMethod, cbUseWOCat;
    TextField txtTestFile, txtBkgndFile;
    Button btnTestFile, btnBkgndFile;
    RadioButton rbGenes, rbProteins, rbTrans;
    ChoiceBox cbTestLists, cbBkgndLists;
    
    TreeViewFeatures tvFeatures;
    List<IsoType> lstCmp;
    HashMap<String, HashMap<String, String>> hmCmp;
    String paramId = "";
    
    public DlgFEAnalysis(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        if(createDialog("FEAParams.fxml", "Functional Enrichment Analysis Parameters", true, "Help_Dlg_FEA.html")) {
            if(dfltParams == null)
                dfltParams = new Params();

            // get control objects
            txtName = (TextField) scene.lookup("#txtName");
            lblItemsList = (Label) scene.lookup("#lblItemsList");
            lblTestSelection = (Label) scene.lookup("#lblTestSelection");
            lblBkgndSelection = (Label) scene.lookup("#lblBkgndSelection");
            rbGenes = (RadioButton) scene.lookup("#rbGenes");
            rbProteins = (RadioButton) scene.lookup("#rbProteins");
            rbTrans = (RadioButton) scene.lookup("#rbTrans");
            txtSigValue = (TextField) scene.lookup("#txtThreshold");
            txtSamplingCnt = (TextField) scene.lookup("#txtSamplingCnt");
            cbTestLists = (ChoiceBox) scene.lookup("#cbTestLists");
            cbBkgndLists = (ChoiceBox) scene.lookup("#cbBkgndLists");
            txtTestFile = (TextField) scene.lookup("#txtTestFile");
            btnTestFile = (Button) scene.lookup("#btnTestFile");
            txtBkgndFile = (TextField) scene.lookup("#txtBkgndFile");
            btnBkgndFile = (Button) scene.lookup("#btnBkgndFile");
            cbMethod = (ChoiceBox) scene.lookup("#cbMethod");
            cbUseWOCat = (ChoiceBox) scene.lookup("#cbUseWOCat");
            TreeView tvf = (TreeView) scene.lookup("#tvFeatures");
            Label lblSamplingCnt = (Label) scene.lookup("#lblSamplingCnt");
            
            // setup dialog
            setProjectName();
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
            btnTestFile.setOnAction((event) -> { getTestFile(); });
            btnBkgndFile.setOnAction((event) -> { getBkgndFile(); });
            tvFeatures = new TreeViewFeatures(project, this);
            for(EnumData norm : Params.lstMethods)
                cbMethod.getItems().add(norm.name);
            for(EnumData yn : Params.lstYesNo)
                cbUseWOCat.getItems().add(yn.name);
            paramId = dfltParams.paramId;
            txtName.setText(dfltParams.name);
            //false to multiple selections
            tvFeatures.initialize(tvf, dfltParams.hmFeatures, false);
            cbMethod.getSelectionModel().select(Params.getMethodListIdx(dfltParams.method.name()));
            cbUseWOCat.getSelectionModel().select(Params.getYNIndex(dfltParams.useWOCat.name()));
            txtSigValue.setText("" + dfltParams.sigValue);
            txtSamplingCnt.setText("" + dfltParams.sampling);
            if(dfltParams.dataType.equals(DataType.PROTEIN))
                rbProteins.setSelected(true);
            else if(dfltParams.dataType.equals(DataType.TRANS))
                rbTrans.setSelected(true);
            else
                rbGenes.setSelected(true);

            boolean samdis = !dfltParams.method.equals(Params.Method.SAMPLING);
            lblSamplingCnt.setDisable(samdis);
            txtSamplingCnt.setDisable(samdis);
            cbMethod.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                boolean samdis1 = !Params.lstMethods.get(cbMethod.getSelectionModel().getSelectedIndex()).id.equals(Params.Method.SAMPLING.name());
                lblSamplingCnt.setDisable(samdis1);
                txtSamplingCnt.setDisable(samdis1);
            });
            onDataTypeChange();
            
            // set test list
            int idx = Params.getTestListIndexFromID(dfltParams.testList.name());
            for(int i = 0; i < cbTestLists.getItems().size(); i++) {
                if(((String)cbTestLists.getItems().get(i)).startsWith(getCompareNameStart(Params.lstTestLists.get(idx).name)))
                {
                    cbTestLists.getSelectionModel().select(i);
                    break;
                }
            }
            if(dfltParams.testList.equals(Params.TestList.FROMFILE))
                txtTestFile.setText(dfltParams.testFilepath);
            onTestListChanged();
            
            // set background list
            idx = Params.getBkgndListIndexFromID(dfltParams.bkgndList.name());
            for(int i = 0; i < cbBkgndLists.getItems().size(); i++) {
                if(((String)cbBkgndLists.getItems().get(i)).startsWith(getCompareNameStart(Params.lstBkgndLists.get(idx).name)))
                {
                    cbBkgndLists.getSelectionModel().select(i);
                    break;
                }
            }
            if(dfltParams.bkgndList.equals(Params.BkgndList.FROMFILE))
                txtBkgndFile.setText(dfltParams.bkgndFilepath);
            onBkgndListChanged();
            
            // setup 
            rbGenes.getToggleGroup().selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle oldValue, Toggle newValue) -> {
                onDataTypeChange();
            });
            cbTestLists.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                onTestListChanged();
            });
            cbBkgndLists.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                onBkgndListChanged();
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
                return(new Params(result.get()));
        }
        return null;
    }
    
    //
    // Internal Functions
    //
    private void onDataTypeChange() {
        ArrayList<String> lstTest = new ArrayList<>();
        ArrayList<String> lstBkgnd = new ArrayList<>();
        cbTestLists.getSelectionModel().clearSelection();
        cbTestLists.getItems().clear();
        cbBkgndLists.getSelectionModel().clearSelection();
        cbBkgndLists.getItems().clear();
        String lstTestLabel;
        if(rbGenes.isSelected()) {
            lstTestLabel = Params.TEST_LIST_LABEL.replace("@", "Genes");
            // for now users will have to export a list from the proper group in multiple time series ------ for now
            //if(!project.data.isMultipleTimeSeriesExpType()) {
                lstTest.add(Params.lstTestLists.get(Params.TestList.DSTRANS.ordinal()).name);
                lstTest.add(Params.lstTestLists.get(Params.TestList.DSPROT.ordinal()).name);
                lstTest.add(Params.lstTestLists.get(Params.TestList.NOTDSTRANS.ordinal()).name);
                lstTest.add(Params.lstTestLists.get(Params.TestList.NOTDSPROT.ordinal()).name);
                lstTest.add(Params.lstTestLists.get(Params.TestList.DE.ordinal()).name);
                lstTest.add(Params.lstTestLists.get(Params.TestList.NOTDE.ordinal()).name);
            //}
            lstBkgnd.add(Params.lstBkgndLists.get(Params.BkgndList.ALL.ordinal()).name.replace("@", "Genes"));
            lstBkgnd.add(Params.lstBkgndLists.get(Params.BkgndList.MULTIISOFORM.ordinal()).name);
        }
        else if(rbProteins.isSelected()) {
            lstTestLabel = Params.TEST_LIST_LABEL.replace("@", "Proteins");
            // for now users will have to export a list from the proper group in multiple time series
            if(!project.data.isMultipleTimeSeriesExpType()) {
                lstTest.add(Params.lstTestLists.get(Params.TestList.DE.ordinal()).name);
                lstTest.add(Params.lstTestLists.get(Params.TestList.NOTDE.ordinal()).name);
            }
            lstBkgnd.add(Params.lstBkgndLists.get(Params.BkgndList.ALL.ordinal()).name.replace("@", "Proteins"));
        }
        else {
            lstTestLabel = Params.TEST_LIST_LABEL.replace("@", "Transcripts");
            // for now users will have to export a list from the proper group in multiple time series
            if(!project.data.isMultipleTimeSeriesExpType()) {
                lstTest.add(Params.lstTestLists.get(Params.TestList.DE.ordinal()).name);
                lstTest.add(Params.lstTestLists.get(Params.TestList.NOTDE.ordinal()).name);
            }
            lstBkgnd.add(Params.lstBkgndLists.get(Params.BkgndList.ALL.ordinal()).name.replace("@", "Transcripts"));
        }
        lblItemsList.setText(lstTestLabel);
        lstTest.add(Params.lstTestLists.get(Params.TestList.FROMFILE.ordinal()).name);
        for(String name : lstTest)
            cbTestLists.getItems().add(name);
        if(cbTestLists.getItems().size() > 0)
            cbTestLists.getSelectionModel().select(0);
        lstBkgnd.add(Params.lstBkgndLists.get(Params.BkgndList.FROMFILE.ordinal()).name);
        for(String name : lstBkgnd)
            cbBkgndLists.getItems().add(name);
        if(cbBkgndLists.getItems().size() > 0)
            cbBkgndLists.getSelectionModel().select(0);
        onTestListChanged();
    }
    private void onTestListChanged() {
        // must use getSelectedIndex - called from changed listener
        ArrayList<String> lst = new ArrayList<>();
        if(cbTestLists.getSelectionModel().getSelectedIndex() != -1) {
            int idx = Params.getTestListIndex((String)cbTestLists.getItems().get(cbTestLists.getSelectionModel().getSelectedIndex()));
            Params.TestList rl = Params.TestList.valueOf(Params.lstTestLists.get(idx).id);
            switch(rl) {
                case FROMFILE:
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
        onBkgndListChanged();
    }
    private void onBkgndListChanged() {
        // must use getSelectedIndex - called from changed listener
        if(cbBkgndLists.getSelectionModel().getSelectedIndex() != -1) {
            int idx = Params.getBkgndListIndexFromName((String)cbBkgndLists.getItems().get(cbBkgndLists.getSelectionModel().getSelectedIndex()));
            Params.BkgndList rl = Params.BkgndList.valueOf(Params.lstBkgndLists.get(idx).id);
            switch(rl) {
                case FROMFILE:
                    lblBkgndSelection.setDisable(false);
                    txtBkgndFile.setDisable(false);
                    btnBkgndFile.setDisable(false);
                    break;
                default:
                    lblBkgndSelection.setDisable(true);
                    txtBkgndFile.setDisable(true);
                    btnBkgndFile.setDisable(true);
                    break;
            }
        }
        annotationFeaturesOnSelectionChanged();
    }
    private void getTestFile() {
        File f = getFileName();
        if(f != null)
            txtTestFile.setText(f.getPath());
    }
    private void getBkgndFile() {
        File f = getFileName();
        if(f != null)
            txtBkgndFile.setText(f.getPath());
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
                ArrayList<DataApp.EnumData> lstFEAParams = project.data.analysis.getFEAParamsList();
                for(DataApp.EnumData data : lstFEAParams) {
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
        
        // get the data type
        DataType dataType;
        if(rbTrans.isSelected())
            dataType = DataType.TRANS;
        else if(rbProteins.isSelected())
            dataType = DataType.PROTEIN;
        else
            dataType = DataType.GENE;
        results.put(Params.DATATYPE_PARAM, dataType.name());
        
        // get the test list
        int idx = Params.getTestListIndex((String)cbTestLists.getItems().get(cbTestLists.getSelectionModel().getSelectedIndex()));
        String lsitId = Params.lstTestLists.get(idx).id;
        results.put(Params.TESTLIST_PARAM, lsitId);
        if(lsitId.equals(Params.TestList.FROMFILE.name())) {
            String filepath = txtTestFile.getText().trim();
            errmsg = checkTestListFile(filepath);
            if(errmsg.isEmpty())
                results.put(Params.TESTFROMFILE_PARAM, filepath);
            else
                txtTestFile.requestFocus();
        }
        else if(lsitId.equals(Params.TestList.DE.name()) || lsitId.equals(Params.TestList.NOTDE.name())) {
            if(!project.data.analysis.hasDEAData(dataType)) {
                errmsg = "You do not have DEA results for selected test list.";
                cbTestLists.requestFocus();
            }
        }
        else if(lsitId.equals(Params.TestList.DSTRANS.name()) || lsitId.equals(Params.TestList.NOTDSTRANS.name())) {
            if(!project.data.analysis.hasDIUDataTrans()) {
                errmsg = "You do not have transcripts DIU results for selected test list.";
                cbTestLists.requestFocus();
            }
        }
        else if(lsitId.equals(Params.TestList.DSPROT.name()) || lsitId.equals(Params.TestList.NOTDSPROT.name())) {
            if(!project.data.analysis.hasDIUDataProtein()) {
                errmsg = "You do not have protein DIU results for selected test list.";
                cbTestLists.requestFocus();
            }
        }

        if(errmsg.isEmpty()) {
            // get the background list
            idx = Params.getBkgndListIndexFromName((String)cbBkgndLists.getItems().get(cbBkgndLists.getSelectionModel().getSelectedIndex()));
            results.put(Params.BKGNDLIST_PARAM, Params.lstBkgndLists.get(idx).id);
            if(Params.lstBkgndLists.get(idx).id.equals(Params.BkgndList.FROMFILE.name())) {
                String filepath = txtBkgndFile.getText().trim();
                errmsg = checkBkgndListFile(filepath);
                if(errmsg.isEmpty())
                    results.put(Params.BKGNDFROMFILE_PARAM, filepath);
                else
                    txtBkgndFile.requestFocus();
            }
        }
        if(errmsg.isEmpty()) {
            txt = txtSigValue.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= Params.MIN_PVAL_THRESHOLD && val <= Params.MAX_PVAL_THRESHOLD)
                        results.put(Params.SIGVAL_PARAM, txt);
                    else
                        errmsg = "Invalid significance value entered (" + Params.MIN_PVAL_THRESHOLD + " to " + Params.MAX_PVAL_THRESHOLD + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid significance value number entered.";
                }
            }
            else
                errmsg = "You must enter a significance value.";
            if(errmsg.isEmpty())
                errmsg = tvFeatures.validate(Params.FEATURE_PARAM, results);
            else
                txtSigValue.requestFocus();
        }
        if(!errmsg.isEmpty())
            results.put("ERRMSG", errmsg);
        else {
            txt = txtSamplingCnt.getText().trim();
            int midx = cbMethod.getSelectionModel().getSelectedIndex();
            int uidx = cbUseWOCat.getSelectionModel().getSelectedIndex();
            results.put(Params.METHOD_PARAM, Params.lstMethods.get(midx).id);
            results.put(Params.USEWOCAT_PARAM, Params.lstYesNo.get(uidx).id);
            System.out.println("m: " + Params.lstMethods.get(midx).id + ", txt: " + txt);
            if(Params.lstMethods.get(midx).name.equals("Sampling")) {
                if(txt.length() > 0) {
                    try {
                        Integer ival = Integer.parseInt(txt);
                        if(ival >= 1 && ival < 100000)
                            results.put(Params.SAMPLINGCNT_PARAM, txt);
                        else
                            errmsg = "Invalid sampling count value entered (" + 1 + " to " + "99,999" + " allowed).";
                    } catch(Exception e) {
                        errmsg = "Invalid sampling count number entered.";
                    }
                }
                else
                    errmsg = "You must enter a sampling count value.";
            }
            if(!errmsg.isEmpty()) {
                txtSamplingCnt.requestFocus();
                results.put("ERRMSG", errmsg);
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
    private String checkBkgndListFile(String filepath) {
        String errmsg = "";
        if(!Files.exists(Paths.get(filepath)))
            errmsg = "Specified background list file not found.";
        return errmsg;
    }
    private static String getCompareNameStart(String name) {
        int idx = name.indexOf("@");
        if(idx != -1)
            return name.substring(0, idx);
        return name;
    }
    
    //
    // Data classes
    //
    public static class IsoType {
        String name;
        ArrayList<CompareType> lst;
        public IsoType(String name, ArrayList<CompareType> lst) {
            this.name = name;
            this.lst = lst;
        }
    }
    public static class CompareType {
        String id, name, cmpName;
        CheckBoxTreeItem item1, item2;
        public CompareType(String id, String name, String cmpName, CheckBoxTreeItem item1, CheckBoxTreeItem item2) {
            this.id = id;
            this.name = name;
            this.cmpName = cmpName;
            this.item1 = item1;
            this.item1 = item1;
        }
    }
    public static class CompareLabel {
        String name, fullName;
        public CompareLabel(String name, String fullName) {
            this.name = name;
            this.fullName = fullName;
        }
    }
    public static class Params extends DlgParams {
        public static final String NAME_PARAM = "name";
        public static final String DATATYPE_PARAM = "dataType";
        public static final String TESTLIST_PARAM = "testList";
        public static final String TESTFROMFILE_PARAM = "testFile";
        public static final String BKGNDLIST_PARAM = "bkgndList";
        public static final String BKGNDFROMFILE_PARAM = "bkgndFile";

        public static final String METHOD_PARAM = "method";
        public static final String SAMPLINGCNT_PARAM = "samplingcnt";
        public static final String USEWOCAT_PARAM = "usewocat";
        public static final String SIGVAL_PARAM = "sigval";
        public static final String FEATURE_PARAM = "feature1";
        public static int MAX_NAME_LENGTH = 60;
        private static final String TEST_LIST_LABEL = "@ Lists";
        public static final int MAX_FEATURES = 99;

        public static enum Method {
            WALLENIUS, SAMPLING, HYPERGEOMETRIC
        }
        private static final List<EnumData> lstMethods = Arrays.asList(
            new EnumData(Method.WALLENIUS.name(), "Wallenius"),
            new EnumData(Method.SAMPLING.name(), "Sampling"),
            new EnumData(Method.HYPERGEOMETRIC.name(), "Hypergeometric")
        );
        public static enum TestList {
            DSTRANS, NOTDSTRANS, DSPROT, NOTDSPROT, DE, NOTDE, FROMFILE
        }
        private static final List<EnumData> lstTestLists = Arrays.asList(
            new EnumData(TestList.DSTRANS.name(), "Differentially Isoform Usage (Transcripts)"),
            new EnumData(TestList.NOTDSTRANS.name(), "Not Differentially Isoform Usage (Transcripts)"),
            new EnumData(TestList.DSPROT.name(), "Differentially Isoform Usage (Proteins)"),
            new EnumData(TestList.NOTDSPROT.name(), "Not Differentially Isoform Usage (Proteins)"),
            new EnumData(TestList.DE.name(), "Differentially Expressed"),
            new EnumData(TestList.NOTDE.name(), "Not Differentially Expressed"),
            new EnumData(TestList.FROMFILE.name(), "Use list from file...")
        );
        public static enum BkgndList {
            ALL, MULTIISOFORM, FROMFILE
        }
        // Warning: Names must be unique prior to the @ substitution character
        private static final List<EnumData> lstBkgndLists = Arrays.asList(
            new EnumData(BkgndList.ALL.name(), "All @"),
            new EnumData(BkgndList.MULTIISOFORM.name(), "All multiple isoform genes"),
            new EnumData(BkgndList.FROMFILE.name(), "Use list from file...")
        );

        //private boolean dfltAutoName = true;
        private final DataType dfltDataType = DataType.GENE;
        private final TestList dfltTestList = Params.TestList.DSTRANS;
        private final BkgndList dfltBkgndList = BkgndList.MULTIISOFORM;
        private final Method dfltMethod = Method.WALLENIUS;
        private final int dfltSampling = 2000;
        private final YesNo dfltUseWOCat = YesNo.NO;
        private final double dfltSigValue = 0.05;
        
        HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
        public String name = "";
        public boolean autoName;
        public DataType dataType;
        public TestList testList;
        public String testFilepath = "";
        public BkgndList bkgndList;
        public String bkgndFilepath = "";
        public Method method;
        public int sampling;
        public YesNo useWOCat;
        public double sigValue;
        public String paramId = "";
        Integer totalFeatures;
        
        public Params() {
            name = "";
            dataType = dfltDataType;
            testList = dfltTestList;
            testFilepath = "";
            bkgndList = dfltBkgndList;
            bkgndFilepath = "";
            method = dfltMethod;
            sampling = dfltSampling;
            useWOCat = dfltUseWOCat;
            sigValue = dfltSigValue;
            totalFeatures = 0;
        }
        public Params(HashMap<String, String> hmParams) {
            name = hmParams.containsKey(NAME_PARAM)? hmParams.get(NAME_PARAM) : "";
            dataType = hmParams.containsKey(DATATYPE_PARAM)? DataType.valueOf(hmParams.get(DATATYPE_PARAM)) : dfltDataType;
            testList = hmParams.containsKey(TESTLIST_PARAM)? TestList.valueOf(hmParams.get(TESTLIST_PARAM)) : dfltTestList;
            testFilepath = hmParams.containsKey(TESTFROMFILE_PARAM)? hmParams.get(TESTFROMFILE_PARAM) : "";
            bkgndList = hmParams.containsKey(BKGNDLIST_PARAM)? BkgndList.valueOf(hmParams.get(BKGNDLIST_PARAM)) : dfltBkgndList;
            bkgndFilepath = hmParams.containsKey(BKGNDFROMFILE_PARAM)? hmParams.get(BKGNDFROMFILE_PARAM) : "";
            method = hmParams.containsKey(METHOD_PARAM)? Method.valueOf(hmParams.get(METHOD_PARAM)) : dfltMethod;
            sampling = hmParams.containsKey(SAMPLINGCNT_PARAM)? Integer.parseInt(hmParams.get(SAMPLINGCNT_PARAM)) : dfltSampling;
            useWOCat = hmParams.containsKey(USEWOCAT_PARAM)? YesNo.valueOf(hmParams.get(USEWOCAT_PARAM)) : dfltUseWOCat;
            sigValue = hmParams.containsKey(SIGVAL_PARAM)? Double.parseDouble(hmParams.get(SIGVAL_PARAM)) : dfltSigValue;
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
            hm.put(DATATYPE_PARAM, dataType.name());
            hm.put(TESTLIST_PARAM, testList.name());
            if(!testFilepath.isEmpty())
                hm.put(TESTFROMFILE_PARAM, testFilepath);
            hm.put(BKGNDLIST_PARAM, bkgndList.name());
            if(!bkgndFilepath.isEmpty())
                hm.put(BKGNDFROMFILE_PARAM, bkgndFilepath);
            hm.put(METHOD_PARAM, method.name());
            hm.put(SAMPLINGCNT_PARAM, "" + sampling);
            hm.put(USEWOCAT_PARAM, useWOCat.name());
            hm.put(SIGVAL_PARAM, "" + sigValue);
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
        // base class implements boolean save(String filepath)

        //
        // Static functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
        private static List<EnumData> getMethodsList() { return lstMethods; }
        
        private static int getMethodListIdx(String id) {
            int idx = 0;
            for(EnumData ed : lstMethods) {
                if(ed.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= lstMethods.size())
                idx = 0;
            return idx;
        }
        private static int getTestListIndex(String name) {
            int idx = 0;
            for(EnumData entry : lstTestLists) {
                String cmpName = getCompareNameStart(entry.name);
                if(name.startsWith(cmpName))
                    break;
                idx++;
            }
            if(idx >= lstTestLists.size())
                idx = 0;
            return idx;
        }
        private static int getTestListIndexFromID(String id) {
            int idx = 0;
            for(EnumData entry : lstTestLists) {
                if(id.equals(entry.id))
                    break;
                idx++;
            }
            if(idx >= lstTestLists.size())
                idx = 0;
            return idx;
        }
        private static int getBkgndListIndexFromName(String name) {
            int idx = 0;
            for(EnumData entry : lstBkgndLists) {
                String cmpName = getCompareNameStart(entry.name);
                if(name.startsWith(cmpName)) {
                    // "All @" matches "All multiple..." so handle it at end
                    if(idx != 0)
                        break;
                }
                idx++;
            }
            if(idx >= lstBkgndLists.size())
                idx = 0;
            return idx;
        }
        private static int getBkgndListIndexFromID(String id) {
            int idx = 0;
            for(EnumData entry : lstBkgndLists) {
                if(id.equals(entry.id))
                    break;
                idx++;
            }
            if(idx >= lstBkgndLists.size())
                idx = 0;
            return idx;
        }
    }
}
