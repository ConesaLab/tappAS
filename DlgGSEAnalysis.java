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
import tappas.DataApp.DataType;
import tappas.DataApp.EnumData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgGSEAnalysis extends DlgBase {
    Label lblListTest, lblSelection1, lblSelection2, lblSets, lblRank1, lblRank2;
    TextField txtName, txtSigValue, txtRLFile1, txtRLFile2, txtSets;
    Button btnRLFile1, btnRLFile2, btnSets;
    RadioButton rbGenes, rbProteins, rbTrans, rbFeatures, rbSets;
    ChoiceBox cbRankedLists1, cbRankedLists2, cbMethods;
//    Hyperlink lnkClearAll, lnkCheckAll;
    TreeViewFeatures tvFeatures;
    CheckBox chkMulti;
    
    String paramId = "";
    
    public DlgGSEAnalysis(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        if(createDialog("GSEAParams.fxml", "Gene Set Enrichment Analysis Parameters", true, "Help_Dlg_GSEA.html")) {
            if(dfltParams == null)
                dfltParams = new Params();

            // get control objects
            txtName = (TextField) scene.lookup("#txtName");
            lblListTest = (Label) scene.lookup("#lblListTest");
            lblRank1 = (Label) scene.lookup("#lblRank1");
            lblRank2 = (Label) scene.lookup("#lblRank2");
            lblSelection1 = (Label) scene.lookup("#lblSelection1");
            lblSelection2 = (Label) scene.lookup("#lblSelection2");
            cbRankedLists1 = (ChoiceBox) scene.lookup("#cbRankedLists1");
            cbRankedLists2 = (ChoiceBox) scene.lookup("#cbRankedLists2");
            txtRLFile1 = (TextField) scene.lookup("#txtRLFile1");
            txtRLFile2 = (TextField) scene.lookup("#txtRLFile2");
            btnRLFile1 = (Button) scene.lookup("#btnRLFile1");
            btnRLFile2 = (Button) scene.lookup("#btnRLFile2");
            chkMulti = (CheckBox) scene.lookup("#chkMulti");
            
            rbGenes = (RadioButton) scene.lookup("#rbGenes");
            rbProteins = (RadioButton) scene.lookup("#rbProteins");
            rbTrans = (RadioButton) scene.lookup("#rbTrans");
            txtSigValue = (TextField) scene.lookup("#txtThreshold");
            cbMethods = (ChoiceBox) scene.lookup("#cbMethods");
            rbFeatures = (RadioButton) scene.lookup("#rbFeatures");
            rbSets = (RadioButton) scene.lookup("#rbSets");
            lblSets = (Label) scene.lookup("#lblSets");
            txtSets = (TextField) scene.lookup("#txtSets");
            btnSets = (Button) scene.lookup("#btnSets");
            TreeView tvf = (TreeView) scene.lookup("#tvFeatures");

//            lnkClearAll = (Hyperlink) scene.lookup("#lnkClearAll");
//            lnkCheckAll = (Hyperlink) scene.lookup("#lnkCheckAll");
            
            // set default values
            paramId = dfltParams.paramId;
            txtName.setText(dfltParams.name);
            tvFeatures = new TreeViewFeatures(project, this);
            tvFeatures.initialize(tvf, dfltParams.hmFeatures, true);
            txtSigValue.setText("" + dfltParams.sigValue);
            if(dfltParams.dataType.equals(DataType.PROTEIN))
                rbProteins.setSelected(true);
            else if(dfltParams.dataType.equals(DataType.TRANS))
                rbTrans.setSelected(true);
            else
                rbGenes.setSelected(true);
            
            onDataTypeChange(cbRankedLists1, lblSelection1, txtRLFile1, btnRLFile1);
            onDataTypeChange(cbRankedLists2, lblSelection2, txtRLFile2, btnRLFile2);
            
            // custom annotation sets - will probably change once it's used
            if(dfltParams.customFile.isEmpty())
                rbFeatures.setSelected(true);
            else {
                rbSets.setSelected(true);
                txtSets.setText(dfltParams.customFile);
            }
            
            chkMulti.setSelected(false);
            chkMulti.setDisable(true);
            
            cbMethods.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(Params.lstMethods.get(cbMethods.getSelectionModel().getSelectedIndex()).id.equals(Params.Method.GOGLM.name())){
                    chkMulti.setSelected(false);
                    lblRank2.setDisable(true);
                    cbRankedLists2.setDisable(true);
                }
            });
            
            chkMulti.disableProperty().bind(cbMethods.valueProperty().isNotEqualTo("MDGSA"));
            
            lblRank2.setDisable(true);
            txtRLFile2.setDisable(true);
            lblSelection2.setDisable(true);
            cbRankedLists2.setDisable(true);
            btnRLFile2.setDisable(true);
            
            lblRank2.setDisable(true);
            txtRLFile2.setDisable(true);
            lblSelection2.setDisable(true);
            cbRankedLists2.setDisable(true);
            btnRLFile2.setDisable(true);
            
            setProjectName();
            for(MethodData method : Params.lstMethods) {
                cbMethods.getItems().add(method.name);
            }
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
            
            btnRLFile1.setOnAction((event) -> { getRLFile(txtRLFile1); });
            btnRLFile2.setOnAction((event) -> { getRLFile(txtRLFile2); });
            
            //tvf.disableProperty().bindBidirectional(rbSets.selectedProperty());
            
            rbSets.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    tvf.setDisable(false);
                }
            });
            
            lblSets.setDisable(true);
            txtSets.setDisable(true);
            btnSets.setDisable(true);
            
            rbFeatures.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    lblSets.setDisable(true);
                    txtSets.setDisable(true);
                    btnSets.setDisable(true);
                }
            });
            
            btnSets.setOnAction((event) -> { getSetsFile(); });

            // setup
            // setup database categories

            chkMulti.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    lblRank2.setDisable(false);
                    cbRankedLists2.setDisable(false);
                    
                    rbFeatures.setDisable(true);
                    tvf.setDisable(true);
                    rbSets.setDisable(true);
                    lblSets.setDisable(true);
                    txtSets.setDisable(true);
                    btnSets.setDisable(true);
                }else{
                    lblRank2.setDisable(true);
                    cbRankedLists2.setDisable(true);
                    txtRLFile2.setDisable(true);
                    lblSelection2.setDisable(true);
                    btnRLFile2.setDisable(true);
                    
                    rbFeatures.setDisable(false);
                    rbFeatures.setSelected(true);
                    tvf.setDisable(false);
                    rbSets.setDisable(false);
                }
            });
            
            rbFeatures.getToggleGroup().selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle oldValue, Toggle newValue) -> {
                annotationFeaturesOnSelectionChanged();
            });
            
            rbGenes.getToggleGroup().selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle oldValue, Toggle newValue) -> {
                onDataTypeChange(cbRankedLists1, lblSelection1, txtRLFile1, btnRLFile1);
                onDataTypeChange(cbRankedLists2, lblSelection2, txtRLFile2, btnRLFile2);
            });
            
            cbRankedLists1.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                onRankedListChanged(cbRankedLists1, lblSelection1, txtRLFile1, btnRLFile1);
            });
            
            cbRankedLists2.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                onRankedListChanged(cbRankedLists2, lblSelection2, txtRLFile2, btnRLFile2);
            });
            
            cbMethods.getSelectionModel().select(Params.getMethodCboIdx(dfltParams.method.name()));
            // set ranked list
            cbRankedLists1.getSelectionModel().select(Params.getRankedListIndexFromID(dfltParams.rankedList1.name()));
            cbRankedLists2.getSelectionModel().select(Params.getRankedListIndexFromID(dfltParams.rankedList2.name()));
            
            if(dfltParams.method.name().equals(Params.Method.MDGSA.name()) && dfltParams.useMulti.equals("TRUE"))
                chkMulti.setSelected(true);
            
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

//            lnkClearAll.setOnAction((event) -> {
//                if(tvf.isDisable())
//                    clearAllFeatures(tvf);
//                else
//                    clearAllFeatures(tvf);
//            });
//
//            lnkCheckAll.setOnAction((event) -> {
//                if(tvf.isDisable())
//                    checkAllFeatures(tvf);
//                else
//                    checkAllFeatures(tvf);
//            });
            
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return(new Params(result.get()));
        }
        return null;
    }

    //hyperlink funtions
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

    private void onDataTypeChange(ChoiceBox cb, Label lbl, TextField txt, Button btn) {
        ArrayList<String> lst = new ArrayList<>();
        cb.getSelectionModel().clearSelection();
        cb.getItems().clear();
        String lstLabel;
        if(rbGenes.isSelected()) {
            lstLabel = Params.LIST_LABEL.replace("@", "Genes");
            // for now users will have no way to get a ranked list for time series - no values available from maSigPro
            //if(!project.data.isTimeCourseExpType()) {
                lst.add(Params.lstRankedLists.get(Params.RankedList.DIUTRANS.ordinal()).name);
                lst.add(Params.lstRankedLists.get(Params.RankedList.DIUPROT.ordinal()).name);
                lst.add(Params.lstRankedLists.get(Params.RankedList.DIUTRANSEXT.ordinal()).name);
                lst.add(Params.lstRankedLists.get(Params.RankedList.DIUPROTEXT.ordinal()).name);
                lst.add(Params.lstRankedLists.get(Params.RankedList.DEA.ordinal()).name);
            //}
        }
        else if(rbProteins.isSelected()) {
            lstLabel = Params.LIST_LABEL.replace("@", "Proteins");
            // for now users will have no way to get a ranked list for time series - no values available from maSigPro
            //if(!project.data.isTimeCourseExpType()) {
                lst.add(Params.lstRankedLists.get(Params.RankedList.DEA.ordinal()).name); // + " for Proteins");
            //}
        }
        else {
            lstLabel = Params.LIST_LABEL.replace("@", "Transcripts");
            // for now users will have no way to get a ranked list for time series - no values available from maSigPro
            //if(!project.data.isTimeCourseExpType()) {
                lst.add(Params.lstRankedLists.get(Params.RankedList.DEA.ordinal()).name); // + " for Transcripts");
            //}
        }
        lblListTest.setText(lstLabel);
        lst.add(Params.lstRankedLists.get(Params.RankedList.FROMFILE.ordinal()).name);
        for(String name : lst)
            cb.getItems().add(name);
        if(cb.getItems().size() > 0)
            cb.getSelectionModel().select(0);
        onRankedListChanged(cb, lbl, txt, btn);
    }
    
    private void onRankedListChanged(ChoiceBox cb, Label lbl, TextField txt, Button btn) {
        // must use getSelectedIndex - called from changed listener
        ArrayList<String> lst = new ArrayList<>();
        if(cb.getSelectionModel().getSelectedIndex() != -1) {
            int idx = Params.getRankedListIndexFromName((String)cb.getItems().get(cb.getSelectionModel().getSelectedIndex()));
            Params.RankedList rl = Params.RankedList.valueOf(Params.lstRankedLists.get(idx).id);
            switch(rl) {
                case FROMFILE:
                    lbl.setDisable(false);
                    txt.setDisable(false);
                    btn.setDisable(false);
                    break;
                default:
                    lbl.setDisable(true);
                    txt.setDisable(true);
                    btn.setDisable(true);
                    break;
            }
        }
        annotationFeaturesOnSelectionChanged();
    }
    private void getRLFile(TextField txt) {
        File f = getFileName();
        if(f != null)
            txt.setText(f.getPath());
    }
    private void getSetsFile() {
        File f = getSetsFileName();
        if(f != null)
            txtSets.setText(f.getPath());
    }
    private File getFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Select Ranked list File"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ranked list files", "*.txt", "*.tsv"),
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
    private File getSetsFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Select Annotation Feature Sets File in GMT format (*.gmt)"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Annotation feature sets files", "*.gmt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        return selectedFile;
    }
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        System.out.println("Validate dialog");

        int idx = Params.getMethodListIdxByName((String) cbMethods.getSelectionModel().getSelectedItem());
        results.put(Params.METHOD_PARAM, Params.lstMethods.get(idx).id);
        
        // get analysis name
        String txt = txtName.getText().trim();
        if(!txt.isEmpty()) {
            // check if not an existing parameter id
            if(paramId.isEmpty()) {
                // check that name is not already used
                ArrayList<DataApp.EnumData> lstGSEAParams = project.data.analysis.getGSEAParamsList();
                for(DataApp.EnumData data : lstGSEAParams) {
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
        
        // get the ranked list
        idx = Params.getRankedListIndexFromName((String)cbRankedLists1.getItems().get(cbRankedLists1.getSelectionModel().getSelectedIndex()));
        String lsitId = Params.lstRankedLists.get(idx).id;
        results.put(Params.RANKEDLIST1_PARAM, lsitId);
        if(lsitId.equals(Params.RankedList.FROMFILE.name())) {
            String filepath = txtRLFile1.getText().trim();
            errmsg = checkRankedListFile(filepath, results);
            if(errmsg.isEmpty())
                results.put(Params.FROMFILE1_PARAM, filepath);
            else
                txtRLFile1.requestFocus();
        }
        else if(lsitId.equals(Params.RankedList.DEA.name())) {
            if(!project.data.analysis.hasDEAData(dataType)) {
                errmsg = "You do not have DEA results for selected ranked list.";
                cbRankedLists1.requestFocus();
            }
        }
        else if(lsitId.equals(Params.RankedList.DIUTRANS.name()) || lsitId.equals(Params.RankedList.DIUTRANSEXT.name())) {
            if(!project.data.analysis.hasDIUDataTrans()) {
                errmsg = "You do not have transcripts DIU results for selected test list.";
                cbRankedLists1.requestFocus();
            }
        }
        else if(lsitId.equals(Params.RankedList.DIUPROT.name()) || lsitId.equals(Params.RankedList.DIUPROTEXT.name())) {
            if(!project.data.analysis.hasDIUDataProtein()) {
                errmsg = "You do not have protein DIU results for selected test list.";
                cbRankedLists1.requestFocus();
            }
        }
        
        if(chkMulti.isSelected()){
            results.put(Params.USEMULTI_PARAM, "TRUE");
            // get the ranked list
            idx = Params.getRankedListIndexFromName((String)cbRankedLists2.getItems().get(cbRankedLists2.getSelectionModel().getSelectedIndex()));
            String lsitId2 = Params.lstRankedLists.get(idx).id;
            if(lsitId.equals(lsitId2))
                errmsg = "You can not have selected the same ranked list for a multidimensional mdgsa analysis.";
            if(errmsg.isEmpty()){
                results.put(Params.RANKEDLIST2_PARAM, lsitId2);
                if(lsitId2.equals(Params.RankedList.FROMFILE.name())) {
                    String filepath = txtRLFile2.getText().trim();
                    errmsg = checkRankedListFile(filepath, results);
                    if(errmsg.isEmpty())
                        results.put(Params.FROMFILE2_PARAM, filepath);
                    else
                        txtRLFile1.requestFocus();
                }
                else if(lsitId2.equals(Params.RankedList.DEA.name())) {
                    if(!project.data.analysis.hasDEAData(dataType)) {
                        errmsg = "You do not have DEA results for selected ranked list.";
                        cbRankedLists2.requestFocus();
                    }
                }
                else if(lsitId2.equals(Params.RankedList.DIUTRANS.name()) || lsitId2.equals(Params.RankedList.DIUTRANSEXT.name())) {
                    if(!project.data.analysis.hasDIUDataTrans()) {
                        errmsg = "You do not have transcripts DIU results for selected test list.";
                        cbRankedLists2.requestFocus();
                    }
                }
                else if(lsitId2.equals(Params.RankedList.DIUPROT.name()) || lsitId2.equals(Params.RankedList.DIUPROTEXT.name())) {
                    if(!project.data.analysis.hasDIUDataProtein()) {
                        errmsg = "You do not have protein DIU results for selected test list.";
                        cbRankedLists2.requestFocus();
                    }
                }
            }
        }
        
        if(errmsg.isEmpty()) {
            txt = txtSigValue.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= DlgBase.DlgParams.MIN_PVAL_THRESHOLD && val <= DlgBase.DlgParams.MAX_PVAL_THRESHOLD)
                        results.put(Params.SIGVAL_PARAM, txt);
                    else
                        errmsg = "Invalid significance value entered (" + DlgBase.DlgParams.MIN_PVAL_THRESHOLD + " to " + DlgBase.DlgParams.MAX_PVAL_THRESHOLD + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid significance value number entered.";
                }
            }
            else
                errmsg = "You must enter a significance value.";
            if(errmsg.isEmpty()) {
                if(chkMulti.isSelected()){
                    //for multivariant just use GeneOntology
                    results.put(Params.FEATURE_PARAM, "GeneOntology");
                }else{
                    // check if using annotation file features
                    if(rbFeatures.isSelected())
                        errmsg = tvFeatures.validate(Params.FEATURE_PARAM, results);
                    else {
                        // load annotation sets file if provided
                        String filepath = txtSets.getText().trim();
                        if(filepath.length() > 0) {
                            ArrayList<Utils.Set> lst = Utils.loadSetsFile(filepath);
                            if(!lst.isEmpty()) {
                                // let's prevent out of controls sets - need to find out what value is reasonable later!!!
                                if(lst.size() <= Params.MAX_NUM_SETS) {
                                    int cnt = 0;
                                    ArrayList<String> nflst = new ArrayList<>();
                                    for(Utils.Set set : lst) {
                                        for(String member : set.lstMembers) {
                                            switch(dataType) {
                                                case GENE:
                                                    if(project.data.hasGene(member))
                                                        cnt++;
                                                    else
                                                        nflst.add(member);
                                                    break;
                                            }
                                        }
                                    }

                                    // check if any genes found
                                    if(cnt > 0) {
                                        // check if missing genes
                                        if(!nflst.isEmpty()) {
                                            int badcnt = nflst.size();
                                            int dspcnt = Math.min(25, badcnt);
                                            String msg = "Non-matching " + app.data.getDataTypePlural(dataType.name()) + " found in custom sets:\n    ";
                                            for(int i = 0; i < dspcnt; i++) {
                                                msg += nflst.get(i);
                                                if(i < (dspcnt-2))
                                                    msg += ", ";
                                            }
                                            if(dspcnt < badcnt)
                                                msg += "\nOnly showing the first " + dspcnt + app.data.getDataTypePlural(dataType.name()) + ".";
                                            app.logInfo(msg);
                                            String alertmsg = "Some of the " + app.data.getDataTypePlural(dataType.name()) + " specified in custom sets\nare not part of the project data\nand will be removed.\nDo you want to Proceeed?\n";
                                            if(!app.ctls.alertConfirmation("Custom Annotation Sets", alertmsg, null))
                                                errmsg = "Non-matching " + app.data.getDataTypePlural(dataType.name()) + " found in custom sets.";
                                        }
                                    }
                                    else
                                        errmsg = "No matching " + app.data.getDataTypePlural(dataType.name()) + " found in custom sets.";
                                }
                                else
                                    errmsg = "Custom sets file exceeds limit of " + Params.MAX_NUM_SETS + " sets (" + lst.size() + ")";
                            }
                            else
                                errmsg = "Unable to load annotation sets data.";

                            if(errmsg.isEmpty())
                                results.put(Params.CUSTOMFILE_PARAM, filepath);
                        }
                        else
                            errmsg = "You must enter the annotation sets file path and name.";
                    }
                }
            }
            else
                txtSigValue.requestFocus();
        }
        if(!errmsg.isEmpty())
            results.put("ERRMSG", errmsg);
        return results;
    }
    
    // We want to provide meaningful messages to the user - so there is a lot of checking taking place
    private String checkRankedListFile(String path, HashMap<String, String> results) {
        String errmsg = "";
        app.logInfo("Checking specified ranked list file: " + path);
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
                                    errmsg = "Unable to read ranked list file data.";
                            }                        
                        }
                        else
                            errmsg = "Ranked list file does not have read access. Check file permissions.";
                    }
                    else {
                        if(size > Params.MAX_RL_FILE_SIZE)
                            errmsg = "Ranked list file exceeds maximum size allowed of " + (Params.MAX_RL_FILE_SIZE/1000000) + "MB";
                        else
                            errmsg = "Ranked list file does not have sufficient data (" + size + "bytes).";
                    }
                }
                else
                    errmsg = "Unable to find specified Ranked list file.";
            } catch(Exception e) {
                errmsg = "Unable to open specified Ranked list file.";
            }
        }
        else
            errmsg = "You must specify the Ranked list file's location.";
        if(!errmsg.isEmpty())
            app.logInfo("Ranked list file check failed: " + errmsg);
        else
            app.logInfo("Ranked list file passed initial check.");
        return errmsg;
    }
    
    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static final String NAME_PARAM = "name";
        public static final String DATATYPE_PARAM = "dataType";
        public static final String RANKEDLIST1_PARAM = "rankedList1";
        public static final String FROMFILE1_PARAM = "file1";
        public static final String RANKEDLIST2_PARAM = "rankedList2";
        public static final String FROMFILE2_PARAM = "file2";
        public static final String SIGVAL_PARAM = "sigval";
        public static final String CUSTOMFILE_PARAM = "customFile";
        public static final String FEATURE_PARAM = "feature1";
        private static final String METHOD_PARAM = "method";
        private static final String USEMULTI_PARAM = "multi";
        public static int MAX_NAME_LENGTH = 60;
        public static final int MIN_RL_FILE_SIZE = 10;
        public static final int MAX_RL_FILE_SIZE = 100000000;
        public static final int MAX_NUM_SETS = 1000;
        
        public static enum Method {
            // all cases
            MDGSA, 
            GOGLM
        }
        
        private static final List<MethodData> lstMethods = Arrays.asList(
            new MethodData(Method.MDGSA.name(), "MDGSA"),
            new MethodData(Method.GOGLM.name(), "GOglm")
        );

        private static final String LIST_LABEL = "@ Ranked List";
        
        public static enum RankedList {
            DIUTRANS, DIUTRANSEXT, DIUPROT, DIUPROTEXT, DEA, FROMFILE
        }
        
        private static final List<EnumData> lstRankedLists = Arrays.asList(
            new EnumData(RankedList.DIUTRANS.name(), "Differential Isoform Usage Results (Transcripts)"),
            new EnumData(RankedList.DIUTRANSEXT.name(), "DIU Analysis Results (Transcripts - including single isoform genes)"),
            new EnumData(RankedList.DIUPROT.name(), "Differential Isoform Usage Results (Proteins)"),
            new EnumData(RankedList.DIUPROTEXT.name(), "DIU Analysis Results (Proteins - including single isoform genes)"),
            new EnumData(RankedList.DEA.name(), "Differential Expression Analysis Results"),
            new EnumData(RankedList.FROMFILE.name(), "Use ranked list from file...")
        );

        private final DataApp.DataType dfltDataType = DataApp.DataType.GENE;
        private final RankedList dfltRankedList1 = Params.RankedList.DIUTRANS;
        private final RankedList dfltRankedList2 = Params.RankedList.DEA;
        private final double dfltSigValue = 0.05;
        private final String dfltUseMulti = "FALSE";
        HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
        
        Method method;
        public String name = "";
        public DataApp.DataType dataType;
        public RankedList rankedList1;
        public RankedList rankedList2;
        public String fromFile1 = "";
        public String fromFile2 = "";
        public double sigValue;
        public String customFile = "";
        public String paramId = "";
        public String useMulti = "";
        
        public Params() {
            //Method dfltMethod = DlgGSEAnalysis.Params.Method.MDGSA;
            Method dfltMethod = DlgGSEAnalysis.Params.Method.GOGLM;
            
            method = dfltMethod;
            name = "";
            dataType = dfltDataType;
            rankedList1 = dfltRankedList1;
            rankedList2 = dfltRankedList2;
            fromFile1 = "";
            fromFile2 = "";
            sigValue = dfltSigValue;
            customFile = "";
            useMulti = dfltUseMulti;
        }
        public Params(HashMap<String, String> hmParams) {
            //Method dfltMethod = DlgGSEAnalysis.Params.Method.MDGSA;
            Method dfltMethod = DlgGSEAnalysis.Params.Method.GOGLM;
            
            method = hmParams.containsKey(METHOD_PARAM)? Method.valueOf(hmParams.get(METHOD_PARAM)) : dfltMethod;
            name = hmParams.containsKey(NAME_PARAM)? hmParams.get(NAME_PARAM) : "";
            useMulti = hmParams.containsKey(USEMULTI_PARAM)? hmParams.get(USEMULTI_PARAM) : dfltUseMulti;
            dataType = hmParams.containsKey(DATATYPE_PARAM)? DataApp.DataType.valueOf(hmParams.get(DATATYPE_PARAM)) : dfltDataType;
            rankedList1 = hmParams.containsKey(RANKEDLIST1_PARAM)? RankedList.valueOf(hmParams.get(RANKEDLIST1_PARAM)) : dfltRankedList1;
            rankedList2 = hmParams.containsKey(RANKEDLIST2_PARAM)? RankedList.valueOf(hmParams.get(RANKEDLIST2_PARAM)) : dfltRankedList2;
            fromFile2 = hmParams.containsKey(FROMFILE2_PARAM)? hmParams.get(FROMFILE2_PARAM) : "";
            fromFile1 = hmParams.containsKey(FROMFILE1_PARAM)? hmParams.get(FROMFILE1_PARAM) : "";
            sigValue = hmParams.containsKey(SIGVAL_PARAM)? Double.parseDouble(hmParams.get(SIGVAL_PARAM)) : dfltSigValue;
            customFile = hmParams.containsKey(CUSTOMFILE_PARAM)? hmParams.get(CUSTOMFILE_PARAM) : "";
            this.hmFeatures = new HashMap<>();
            if(hmParams.containsKey(FEATURE_PARAM)) {
                String[] fields = hmParams.get(FEATURE_PARAM).trim().split("\t");
                if(fields.length > 0) {
                    HashMap<String, Object> hm = new HashMap<>();
                    hmFeatures.put(fields[0].trim(), hm);
                    for(int i = 1; i < fields.length; i++)
                        hm.put(fields[i].trim(), null);
                }
            }
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(METHOD_PARAM, method.name());
            hm.put(NAME_PARAM, name);
            hm.put(USEMULTI_PARAM, useMulti);
            hm.put(DATATYPE_PARAM, dataType.name());
            hm.put(RANKEDLIST1_PARAM, rankedList1.name());
            if(!fromFile1.isEmpty())
                hm.put(FROMFILE1_PARAM, fromFile1);

            hm.put(RANKEDLIST2_PARAM, rankedList2.name());
            if(!fromFile2.isEmpty())
                hm.put(FROMFILE2_PARAM, fromFile2);
           
            if(!customFile.isEmpty())
                hm.put(CUSTOMFILE_PARAM, customFile);
            
            hm.put(SIGVAL_PARAM, "" + sigValue);
            for(String db : hmFeatures.keySet()) {
                String features = db;
                HashMap<String, Object> hmCats = hmFeatures.get(db);
                for(String cat : hmCats.keySet())
                    features += "\t" + cat;
                hm.put(FEATURE_PARAM, features);
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
        
        private static int getMethodCboIdx(String id) {
            int idx = 0;
            for(MethodData ed : lstMethods) {
                if(ed.id.equals(id))
                        break;
                idx++;
            }
            if(idx >= lstMethods.size())
                idx = 0;
                System.out.println("We can't run the selected method. Default method is selected.");
            return idx;
        }
        private static int getMethodListIdxByName(String name) {
            int idx = 0;
            for(MethodData ed : lstMethods) {
                if(ed.name.equals(name))
                    break;
                idx++;
            }
            if(idx >= lstMethods.size()){
               idx = 0;
               System.out.println("We can't run the selected method. Default method is selected.");
            }
            return idx;
        }
        
        private static int getRankedListIndexFromID(String id) {
            int idx = 0;
            for(EnumData ed : Params.lstRankedLists) {
                if(ed.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= Params.lstRankedLists.size())
                idx = 0;
            return idx;
        }
        private static int getRankedListIndexFromName(String name) {
            int idx = 0;
            for(EnumData ed : Params.lstRankedLists) {
                if(ed.name.equals(name))
                    break;
                idx++;
            }
            if(idx >= Params.lstRankedLists.size())
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
        
        public MethodData(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public int compareTo(MethodData entry) {
            return (id.compareToIgnoreCase(entry.id));
        }
    }
    
}
