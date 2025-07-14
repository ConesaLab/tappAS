/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tappas.DataApp.EnumData;

import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgSelectRows extends DlgBase {
    public static int MAX_VALUES_LENGTH = 50000;
    
    Label lblValueFile, lblListStatus;
    Pane paneSelections;
    ComboBox cbFields, cbComparison;
    ChoiceBox cbAction;
    CheckBox chkNewSelect;
    TextArea txtValue;
    Button btnValueFile;
    TreeView tvSelections;
    Hyperlink lnkClearAll;
    Label lblComparison, lblValue;
    
    ArrayList<String> lstDBs = new ArrayList<>();
    ArrayList<String> lstStructCats = new ArrayList<>();
    ArrayList<String> lstStructAttrs = new ArrayList<>();
    ArrayList<String> lstSelections = new ArrayList<>();
    ArrayList<CompChoiceDef> lstComparison = new ArrayList<>();
    List<ColumnDefinition> lstFields;
    ColumnDefinition fdSelected = null;

    public DlgSelectRows(Project project, Window window) {
        super(project, window);
    }
    
    public Params showAndWait(ArrayList<ColumnDefinition> lstFields, HashMap<String, String> dfltValues, int cntSelected) {
        if(createDialog("SelectRows.fxml", "Row Selection Editor", true, "Help_Dlg_SelectRows.html")) {
            this.lstFields = lstFields;

            // get control objects
            lblListStatus = (Label) scene.lookup("#lblListStatus");
            lblValueFile = (Label) scene.lookup("#lblValueFile");
            btnValueFile = (Button) scene.lookup("#btnValueFile");
            txtValue = (TextArea) scene.lookup("#txtValue");
            lblComparison = (Label) scene.lookup("#lblComparison");
            lblValue = (Label) scene.lookup("#lblValue");
            cbAction = (ChoiceBox) scene.lookup("#cbAction");
            chkNewSelect = (CheckBox) scene.lookup("#chkNewSelect");
            cbComparison = (ComboBox) scene.lookup("#cbComparison");
            cbFields = (ComboBox) scene.lookup("#cbFields");
            paneSelections = (Pane) scene.lookup("#paneSelections");
            tvSelections = (TreeView) scene.lookup("#tvSelections");
            lnkClearAll = (Hyperlink) scene.lookup("#lnkClearAll");

            // setup special input filter - could add \n\t removal here but must take into acct change type, range, etc.
            UnaryOperator<TextFormatter.Change> filter = (TextFormatter.Change change) -> {
                if (change.getControlNewText().length() > MAX_VALUES_LENGTH) {
                    showDlgMsg("Values may not exceed " + NumberFormat.getInstance().format(MAX_VALUES_LENGTH) + " characters");
                    return null;
                } else {
                    showDlgMsg("");
                    return change;
                }
            };            
            
            // setup dialog
            lblListStatus.setText(cntSelected == 0? "You currently have an empty selection list." : "You currently have " + cntSelected + " row(s) selected. Choose add/remove action accordingly.");
            lblListStatus.setStyle(cntSelected == 0? "-fx-text-fill: lightgray;" : "-fx-text-fill: darkorange;");
            btnValueFile.setOnAction((event) -> { loadValuesFromFile(); });
            txtValue.setTextFormatter(new TextFormatter(filter));
            for(EnumData ed : Params.lstActions)
                cbAction.getItems().add(ed.name);
            cbAction.getSelectionModel().select(0);
            for(ColumnDefinition cd : lstFields) {
                if(cd.visible)
                    cbFields.getItems().add(cd.selection);
            }
            tvSelections.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
            lnkClearAll.setOnAction((event) -> { clearAllSelections(); });
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

            cbAction.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(newValue != null) {
                    int idx = (Integer) newValue;
                    chkNewSelect.setText(idx==0? "Start new selection (all unselected)" : "Start new selection (all selected)");
                }
            });            
            cbFields.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(newValue != null) {
                    String selection = (String) cbFields.getSelectionModel().getSelectedItem();
                    int idx = 0;
                    for(ColumnDefinition cd : lstFields) {
                        if(cd.selection.equals(selection)) {
                            onFieldSelected(lstFields.get(idx));
                            break;
                        }
                        idx++;
                    }
                }
            });            
            cbComparison.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(newValue != null) {
                    if(txtValue.isVisible()) {
                        String selection = (String) cbComparison.getSelectionModel().getSelectedItem();
                        if(selection != null) {
                            CompChoiceDef def1 = Params.getCompChoiceDef(Params.CompChoice.MATCHES, Params.lstCompChoices);
                            CompChoiceDef def2 = Params.getCompChoiceDef(Params.CompChoice.MATCHESNOT, Params.lstCompChoices);
                            boolean show = false;
                            if((def1 != null && selection.equals(def1.selection)) || (def2 != null && selection.equals(def2.selection)))
                                show = true;
                            btnValueFile.setVisible(show);
                            btnValueFile.setDisable(!show);
                            lblValueFile.setVisible(show);
                            txtValue.setDisable(false);
                        }
                    }
                }
            });
            
            // check if default values provided and set if so
            setDefaultValues(dfltValues);
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return (new Params(lstFields, result.get()));
        }
        return null;
    }
    private void setDefaultValues(HashMap<String, String> dfltValues) {
        if(dfltValues != null && !dfltValues.isEmpty()) {
            if(dfltValues.containsKey(Params.ACTION_PARAM))
                cbAction.getSelectionModel().select(Params.getActionIdx(dfltValues.get(Params.ACTION_PARAM)));
            if(dfltValues.containsKey(Params.NEWSEL_PARAM))
                chkNewSelect.setSelected(Boolean.valueOf(dfltValues.get(Params.NEWSEL_PARAM)));
            if(dfltValues.containsKey(Params.FIELD_PARAM)) {
                String fieldName = dfltValues.get(Params.FIELD_PARAM);
                ColumnDefinition fieldDef = null;
                if(cbFields.getItems() != null) {
                    int idx = 0;
                    for(ColumnDefinition cd : lstFields) {
                        if(cd.id.equals(fieldName)) {
                            fieldDef = cd;
                            cbFields.getSelectionModel().select(cd.selection);
                            break;
                        }
                        idx++;
                    }
                }
                if(fieldDef != null) {
                    if(dfltValues.containsKey(Params.COMPARISON_PARAM)) {
                        boolean selected = false;
                        String compName = dfltValues.get(Params.COMPARISON_PARAM);
                        if(cbComparison.getItems() != null) {
                            int idx = 0;
                            for(CompChoiceDef ccd : lstComparison) {
                                if(ccd.comp.name().equals(compName)) {
                                    selected = true;
                                    cbComparison.getSelectionModel().select(idx);
                                    break;
                                }
                                idx++;
                            }
                        }
                        
                        if(selected) {
                            if(dfltValues.containsKey(Params.VALUE_PARAM)) {
                                String[] fields;
                                String value = dfltValues.get(Params.VALUE_PARAM);
                                switch(fieldDef.compType) {
                                    case NUMINT:
                                    case NUMDBL:
                                    case NUMDBLPN:
                                    case TEXT:
                                        txtValue.setText(value);
                                        break;
                                    case STRAND:
                                    case BOOL:
                                        // no need to do anything
                                        break;
                                    case LIST:
                                        if(dfltValues.containsKey(Params.SELTYPE_PARAM)) {
                                            fields = value.split(";");
                                            for(String selection : fields) {
                                                int idx = lstSelections.indexOf(selection);
                                                if(idx != -1)
                                                    ((CheckBoxTreeItem)tvSelections.getRoot().getChildren().get(idx)).setSelected(true);
                                            }
                                        }
                                        break;
                                    case LISTIDVALS:
                                        // no text input allowed
                                        fields = value.split(";");
                                        for(String selection : fields) {
                                            int idx = 0;
                                            boolean found = false;
                                            for(EnumData ed : fdSelected.lstIdValuesSelections) {
                                                if(selection.equals(ed.id))
                                                    break;
                                                idx++;
                                            }
                                            if(found)
                                                ((CheckBoxTreeItem)tvSelections.getRoot().getChildren().get(idx)).setSelected(true);
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void loadValuesFromFile() {
        File f = getFileName(false);
        if(f != null) {
            if(f.exists()) {
                long max = 1000000;
                long size = f.length();
                if(size > 1 && size <= max) {
                    String values = Utils.loadValuesFromFile(f.getPath());
                    if(values.isEmpty())
                        app.ctls.alertWarning("Load Values from File", "Unable to load values from file.");
                    else
                        txtValue.setText(values);
                }
                else {
                    String msg;
                    if(size > max)
                        msg = "Ranked list file size (" + size + " bytes) exceeds limit of 1 MB";
                    else
                        msg = "Ranked list file does not have sufficient data (" + size + " bytes).";
                    app.ctls.alertWarning("Load Values from File", msg);
                }
            }
        }
    }
    private File getFileName(boolean matrix) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Select List File"));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("List files", "*.txt", "*.tsv"),
            new FileChooser.ExtensionFilter("All Files", "*.*"));
        String expFolder = app.userPrefs.getImportRankedListSelectionFolder();
        if(!expFolder.isEmpty()) {
            File f = new File(expFolder);
            if(f.exists() && f.isDirectory())
                fileChooser.setInitialDirectory(f);
            else
                app.userPrefs.setImportRankedListSelectionFolder("");
        }
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        if(selectedFile!=null)
            app.userPrefs.setImportRankedListSelectionFolder(selectedFile.getParent());
        return selectedFile;
    }
    private void initSelections(List<String> lstSelections) {
        TreeItem<String> root = new TreeItem<>("");
        CheckBoxTreeItem<String> ti;
        for(String chr : lstSelections) {
            ti = new CheckBoxTreeItem<>(chr);
            root.getChildren().add(ti);
        }
        root.setExpanded(true);
        tvSelections.setShowRoot(false);
        tvSelections.setRoot(root);
    }
    private void initIdValuesSelections(List<EnumData> lstSelections) {
        TreeItem<String> root = new TreeItem<>("");
        CheckBoxTreeItem<String> ti;
        for(EnumData ed : lstSelections) {
            ti = new CheckBoxTreeItem<>(ed.name);
            root.getChildren().add(ti);
        }
        root.setExpanded(true);
        tvSelections.setShowRoot(false);
        tvSelections.setRoot(root);
    }
    private void clearAllSelections() {
        TreeItem<String> rootItem = tvSelections.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            item.setSelected(false);
        }
    }
    private void onFieldSelected(ColumnDefinition cd) {
        Params.CompType ct = cd.compType;
        fdSelected = cd;
        boolean value = false;
        boolean valuelbl = false;
        boolean showPaneSelections = false;
        int selIdx = -1;
        lstComparison.clear();
        switch(ct) {
            case NUMINT:
            case NUMDBL:
            case NUMDBLPN:
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.EQ));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.NEQ));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.GT));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.GTE));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.LT));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.LTE));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.RANGE));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.RANGENOT));
                value = true;
                break;
            case BOOL:
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.TRUE));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.FALSE));
                break;
            case STRAND:
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.NEGATIVE));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.POSITIVE));
                break;
            case TEXT:
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.CONTAINS));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.MATCHES));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.STARTSWITH));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.ENDSWITH));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.CONTAINSNOT));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.MATCHESNOT));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.STARTSWITHNOT));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.ENDSWITHNOT));
                value = true;
                break;
            case LIST:
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.MATCHESANY));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.MATCHESANYNOT));
                selIdx = 0;
                initSelections(cd.lstSelections);
                valuelbl = true;
                showPaneSelections = true;
                break;
            case LISTIDVALS:
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.MATCHESANY));
                lstComparison.add(Params.getCompChoiceDef(Params.CompChoice.MATCHESANYNOT));
                selIdx = 0;
                initIdValuesSelections(cd.lstIdValuesSelections);
                valuelbl = true;
                showPaneSelections = true;
                break;
        }
        lblComparison.setDisable(lstComparison.isEmpty());
        cbComparison.setDisable(lstComparison.isEmpty());
        cbComparison.getItems().clear();
        if(!lstComparison.isEmpty()) {
            for(CompChoiceDef ccd : lstComparison)
                cbComparison.getItems().add(ccd.selection);
            if(lstComparison.size() == 1)
                cbComparison.getSelectionModel().select(0);
            else if(selIdx != -1)
                cbComparison.getSelectionModel().select(selIdx);
        }
        lblValue.setVisible(value || valuelbl);
        txtValue.setVisible(value);
        txtValue.setDisable(true);
        txtValue.setText("");
        btnValueFile.setVisible(false);
        btnValueFile.setDisable(true);
        lblValueFile.setVisible(false);
        paneSelections.setVisible(showPaneSelections);
    }

    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        String txt;
        int cfidx = cbFields.getSelectionModel().getSelectedIndex();
        if(cfidx >= 0) {
            EnumData ed = Params.getActionDef((String) cbAction.getSelectionModel().getSelectedItem());
            results.put(Params.ACTION_PARAM, ed.id);
            results.put(Params.NEWSEL_PARAM, chkNewSelect.isSelected()? "true" : "false");
            ColumnDefinition cd = getColDef((String) cbFields.getSelectionModel().getSelectedItem());
            results.put(Params.FIELD_PARAM, cd.id);
            results.put(Params.COMPTYPE_PARAM, cd.compType.name());
            int cidx = cbComparison.getSelectionModel().getSelectedIndex();
            if(cidx >= 0) {
                results.put(Params.COMPARISON_PARAM, lstComparison.get(cidx).comp.name());
                switch(cd.compType) {
                    case NUMINT:
                        validateNumInt(lstComparison.get(cidx), results);
                        break;
                    case NUMDBL:
                        validateNumDbl(lstComparison.get(cidx), results, false); //cd.field.equals(Field.DEAL2FOLDCHG));
                        break;
                    case NUMDBLPN:
                        validateNumDbl(lstComparison.get(cidx), results, true); //cd.field.equals(Field.DEAL2FOLDCHG));
                        break;
                    case BOOL:
                    case STRAND:
                        // don't need to do anything, all done
                        break;
                    case TEXT:
                        txt = txtValue.getText().trim().replaceAll("[\n\t]", "");
                        if(!txt.isEmpty())
                            results.put(Params.VALUE_PARAM, txt);
                        else {
                            txtValue.requestFocus();
                            errmsg = "You must enter a value.";
                            results.put("ERRMSG", errmsg);
                        }
                        break;
                    case LIST:
                        results.put(Params.SELTYPE_PARAM, Params.SelectionType.LIST.name());
                        validateSelections(results);
                        break;
                    case LISTIDVALS:
                        results.put(Params.SELTYPE_PARAM, Params.SelectionType.LIST.name());
                        validateIdValuesSelections(results);
                        break;
                    default:
                        errmsg = "Internal program error: Unhandled field type.";
                        results.put("ERRMSG", errmsg);
                }
            }
            else {
                cbComparison.requestFocus();
                errmsg = "You must select a comparison.";
                results.put("ERRMSG", errmsg);
            }
        }
        else {
            cbFields.requestFocus();
            errmsg = "You must select a criteria field.";
            results.put("ERRMSG", errmsg);
        }
        return results;
    }
    private String validateNumInt(CompChoiceDef ccd, HashMap<String, String> results) {
        String errmsg = "";
        String value = "";
        String txt = txtValue.getText().trim().replaceAll("[\n\t]", "");
        if(!txt.isEmpty()) {
            String[] nums = txt.split(";");
            if(nums.length > 1) {
                // check if it does not makes sense to have multiple OR conditions for a number
                if(!ccd.comp.equals(Params.CompChoice.RANGE) && !ccd.comp.equals(Params.CompChoice.RANGENOT) &&
                   !ccd.comp.equals(Params.CompChoice.EQ) && !ccd.comp.equals(Params.CompChoice.NEQ)) {
                    txtValue.requestFocus();
                    errmsg = "Multiple OR conditions are not needed for comparison type selected.";
                    results.put("ERRMSG", errmsg);
                }
            }
            if(errmsg.isEmpty()) {
                for(String num : nums) {
                    // check if we need a range
                    if(ccd.comp.equals(Params.CompChoice.RANGE) || ccd.comp.equals(Params.CompChoice.RANGENOT)) {
                        String fields[] = num.split(",");
                        if(fields.length == 2 && isValidNumInt(fields[0].trim()) && isValidNumInt(fields[1].trim())) {
                            int a = Integer.parseUnsignedInt(fields[0].trim());
                            int b = Integer.parseUnsignedInt(fields[1].trim());
                            value += (value.isEmpty()? "" : ";") + (a < b? a : b) + "," + (a < b? b : a);
                        }
                        else {
                            txtValue.requestFocus();
                            errmsg = "Invalid numeric range value entered.";
                            results.put("ERRMSG", errmsg);
                            break;
                        }
                    }
                    else {
                        // must be a plain positive number
                        num = num.trim();
                        if(isValidNumInt(num))
                            value += (value.isEmpty()? "" : ";") + num;
                        else {
                            txtValue.requestFocus();
                            errmsg = "Invalid number value entered.";
                            results.put("ERRMSG", errmsg);
                            break;
                        }
                    }
                }
            }
        }
        else {
            txtValue.requestFocus();
            errmsg = "You must enter a value.";
            results.put("ERRMSG", errmsg);
        }
        if(errmsg.isEmpty())
            results.put(Params.VALUE_PARAM, value);
        return errmsg;
    }
    private String validateNumDbl(CompChoiceDef ccd, HashMap<String, String> results, boolean allowNeg) {
        String errmsg = "";
        String value = "";
        String txt = txtValue.getText().trim().replaceAll("[\n\t]", "");
        if(!txt.isEmpty()) {
            String[] nums = txt.split(";");
            if(nums.length > 1) {
                // check if it does not makes sense to have multiple OR conditions for a number
                if(!ccd.comp.equals(Params.CompChoice.RANGE) && !ccd.comp.equals(Params.CompChoice.RANGENOT) &&
                   !ccd.comp.equals(Params.CompChoice.EQ) && !ccd.comp.equals(Params.CompChoice.NEQ)) {
                    txtValue.requestFocus();
                    errmsg = "Multiple OR conditions are not needed for comparison type selected.";
                    results.put("ERRMSG", errmsg);
                }
            }
            if(errmsg.isEmpty()) {
                for(String num : nums) {
                    // check if we need a range: it's a problem if we allow range with negative values!
                    if(ccd.comp.equals(Params.CompChoice.RANGE) || ccd.comp.equals(Params.CompChoice.RANGENOT)) {
                        String fields[] = num.split(",");
                        if(fields.length == 2 && isValidNumDbl(fields[0].trim()) && isValidNumDbl(fields[1].trim())) {
                            double a = Double.parseDouble(fields[0].trim());
                            double b = Double.parseDouble(fields[1].trim());
                            if(!allowNeg && (a < 0 || b < 0)) {
                                txtValue.requestFocus();
                                errmsg = "Invalid range value entered, only positive values allowed.";
                                results.put("ERRMSG", errmsg);
                                break;
                            }
                            value += (value.isEmpty()? "" : ";") + (a < b? a : b) + "," + (a < b? b : a);
                        }
                        else {
                            txtValue.requestFocus();
                            errmsg = "Invalid numeric range value entered.";
                            results.put("ERRMSG", errmsg);
                            break;
                        }
                    }
                    else {
                        // get value
                        num = num.trim();
                        if(isValidNumDbl(num)) {
                            double a = Double.parseDouble(num);
                            if(!allowNeg && a < 0) {
                                txtValue.requestFocus();
                                errmsg = "Invalid value entered, only positive values allowed.";
                                results.put("ERRMSG", errmsg);
                                break;
                            }
                            else
                                value += (value.isEmpty()? "" : ";") + num;
                        }
                        else {
                            txtValue.requestFocus();
                            errmsg = "Invalid number value entered.";
                            results.put("ERRMSG", errmsg);
                            break;
                        }
                    }
                }
            }
        }
        else {
            txtValue.requestFocus();
            errmsg = "You must enter a value.";
            results.put("ERRMSG", errmsg);
        }
        if(errmsg.isEmpty())
            results.put(Params.VALUE_PARAM, value);
        return errmsg;
    }
    private void validateSelections(HashMap<String, String> results) {
        String errmsg = "";
        String chrs = "";
        ObservableList<TreeItem<String>> lst = tvSelections.getRoot().getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            if(item.isSelected()) {
                if(!chrs.isEmpty())
                    chrs += ";";
                chrs += item.getValue();
            }
        }
        if(!chrs.isEmpty())
            results.put(Params.VALUE_PARAM, chrs);
        else {
            tvSelections.requestFocus();
            errmsg = "You must select one or more items.";
            results.put("ERRMSG", errmsg);
        }
    }
    private void validateIdValuesSelections(HashMap<String, String> results) {
        String errmsg = "";
        String chrs = "";
        boolean selection = false;
        ObservableList<TreeItem<String>> lst = tvSelections.getRoot().getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            if(item.isSelected()) {
                String val = item.getValue();
                // blank entries must always be the last on the list or will not work for multiple selections
                for(EnumData ed : fdSelected.lstIdValuesSelections) {
                    if(val.equals(ed.name)) {
                        selection = true;
                        if(!chrs.isEmpty())
                            chrs += ";";
                        chrs += ed.id;
                        break;
                    }
                }
            }
        }
        if(selection)
            results.put(Params.VALUE_PARAM, chrs);
        else {
            tvSelections.requestFocus();
            errmsg = "You must select one or more items.";
            results.put("ERRMSG", errmsg);
        }
    }
    private ColumnDefinition getColDef(String selection) {
        ColumnDefinition def = null;
        for(ColumnDefinition cd : lstFields) {
            if(cd.selection.equals(selection)) {
                def = cd;
                break;
            }
        }
        return def;
    }
    
    //
    // Data Classes
    //
    public static class CompChoiceDef implements Comparable<CompChoiceDef>{
        public Params.CompChoice comp;
        public String selection, display;
        CompChoiceDef(Params.CompChoice comp, String selection, String display) {
            this.comp = comp;
            this.selection = selection;
            this.display = display;
        }
        @Override
        public int compareTo(CompChoiceDef td) {
            return (selection.compareTo(td.selection));
        }
    }
    public static class ListSelection implements Comparable<ListSelection> {
        public String selection, display;
        ListSelection(String selection, String display) {
            this.selection = selection;
            this.display = display;
        }
        @Override
        public int compareTo(ListSelection td) {
            return (selection.compareTo(td.selection));
        }
    }
    static class StructAttributeUI {
        RadioButton rbNA,rbWith,rbWithout;
        String attribute;
    }
    
    //
    // Data Classes
    //
    public static class StructAttrsFilter {
        String category;
        ArrayList<String> lstAttrsWith;
        ArrayList<String> lstAttrsWithout;
        public StructAttrsFilter(String category) {
            this.category = category;
            lstAttrsWith = new ArrayList<>();
            lstAttrsWithout = new ArrayList<>();
        }
        // determines if the given item meets filter crteria
        public boolean isCriteriaMet(String category, String attrs) {
            boolean meetCriteria = false;
            if(!category.trim().isEmpty()) {
                String[] fields = null;
                if(!attrs.trim().isEmpty()) {
                    fields = attrs.split(",");
                    if(fields != null) {
                        for(int idx = 0; idx < fields.length; idx++)
                            fields[idx] = fields[idx].trim();
                    }
                }
                if(category.equals(this.category)) {
                    if(fields != null) {
                        meetCriteria = true;
                        int withcnt = 0;
                        for(String attr : fields) {
                            if(!lstAttrsWith.isEmpty()) {
                                if(lstAttrsWith.contains(attr))
                                    withcnt++;
                            }
                            if(!lstAttrsWithout.isEmpty()) {
                                if(lstAttrsWithout.contains(attr)) {
                                    meetCriteria = false;
                                    break;
                                }
                            }
                        }
                        if(withcnt != lstAttrsWith.size())
                            meetCriteria = false;
                    }
                }
            }
            return(meetCriteria);
        }
    }
    public static class ColumnDefinition {
        String id, colGroup, colTitle, propName, alignment, selection;
        int minWidth, prefWidth, maxWidth;
        Params.CompType compType;
        boolean visible;
        List<String> lstSelections;
        List<EnumData> lstIdValuesSelections;
        HashMap<String, ArrayList<String>> hmTreeSelections;
        
        public ColumnDefinition(String id, String colGroup, String colTitle, String propName,int prefWidth,
                                String alignment, String selection, Params.CompType compType) {
            this.id = id;
            this.propName = propName;
            this.colGroup = colGroup;
            this.colTitle = colTitle;
            this.minWidth = 10;             // keep column from disappearing, hard for user to find
            this.prefWidth = prefWidth;
            this.maxWidth = 0;              // 0 == don't set a max value
            this.alignment = alignment;
            this.selection = selection;
            this.compType = compType;
            this.visible = true;
            this.lstSelections = new ArrayList<>();
            this.lstIdValuesSelections = new ArrayList<>();
            this.hmTreeSelections = new HashMap<>();
        }
        
        public static ColumnDefinition getColDefFromColGroupTitle(String group, String title, List<ColumnDefinition> lstFields) {
            ColumnDefinition def = null;
            for(ColumnDefinition fd : lstFields) {
                if(fd.colGroup.equals(group) && fd.colTitle.equals(title)) {
                    def = fd;
                    break;
                }
            }
            return def;
        }
        public static ColumnDefinition getColDefFromColTitle(String title, List<ColumnDefinition> lstFields) {
            ColumnDefinition def = null;
            for(ColumnDefinition fd : lstFields) {
                if(fd.colTitle.equals(title)) {
                    def = fd;
                    break;
                }
            }
            return def;
        }
        public static ColumnDefinition getColDefFromId(String id, List<ColumnDefinition> lstFields) {
            ColumnDefinition def = null;
            for(ColumnDefinition fd : lstFields) {
                if(fd.id.equals(id)) {
                    def = fd;
                    break;
                }
            }
            return def;
        }
    }
    public static class Params {
        public static final String ACTION_PARAM = "action";
        public static final String NEWSEL_PARAM = "new";
        public static final String FIELD_PARAM = "field";
        public static final String COMPTYPE_PARAM = "comparisonType";
        public static final String COMPARISON_PARAM = "comparison";
        public static final String VALUE_PARAM = "value";
        // parameters for some special fields
        public static final String SELTYPE_PARAM = "selection";

        static public enum SelectAction {
            ADD, REMOVE
        }
        
        // these lists allow matching blank cells - Neither DS/NotDS or neither UP/DOWN
        public static final List<EnumData> lstIdValuesDS = Arrays.asList(
            new EnumData("DS", "DS - Differentially Spliced"),
            new EnumData("Not DS", "Not DS - Not Differentially Spliced"),
            new EnumData("", "<blank> - not tested")
        );
        public static final List<EnumData> lstIdValuesDIU = Arrays.asList(
            new EnumData("DIU", "DIU - Differentially Isoform Usage"),
            new EnumData("Not DIU", "Not DIU - Not Differentially Isoform Usage"),
            new EnumData("", "<blank> - not tested")
        );
        public static final List<DataApp.EnumData> lstIdValuesDERegulated = Arrays.asList(
            new EnumData("UP", "UP - Up Regulated"),
            new EnumData("DOWN", "DOWN - Down Regulated"),
            new EnumData("", "<blank> - not regulated")
        );
        public static final List<EnumData> lstIdValuesVarying = Arrays.asList(
            new EnumData("Varying", "Varying - Varying feature annotation"),
            new EnumData("Not Varying", "Not Varying - Not Varying feature annotation"),
            new EnumData("", "<blank> -  - does not contain feature annotation")
        );
        
        private static final List<DataApp.EnumData> lstActions = Arrays.asList(
            new EnumData(SelectAction.ADD.name(), "Add to existing selections"),
            new EnumData(SelectAction.REMOVE.name(), "Remove from existing selections")
        );
        static public enum CompType { //BOOLSTRING, 
            NUMINT, NUMDBL, NUMDBLPN, BOOL, TEXT, LIST, LISTIDVALS, STRAND
        }
        static public enum CompChoice {
            TRUE, FALSE, NEGATIVE, POSITIVE, 
            EQ, NEQ, GT, GTE, LT, LTE, RANGE, RANGENOT,
            STARTSWITH, ENDSWITH, MATCHES, MATCHESANY, MATCHESALL, CONTAINS, CONTAINSANY, CONTAINSALL,
            STARTSWITHNOT, ENDSWITHNOT, MATCHESNOT, MATCHESANYNOT, MATCHESALLNOT, CONTAINSNOT, CONTAINSANYNOT, CONTAINSALLNOT
        }
        private static final List<CompChoiceDef> lstCompChoices = Arrays.asList(
            new CompChoiceDef(Params.CompChoice.NEGATIVE, "Is negative (\"-\")", "Negative"),
            new CompChoiceDef(Params.CompChoice.POSITIVE, "Is positive (\"+\")", "Positive"),
            new CompChoiceDef(Params.CompChoice.TRUE, "Is true (\"YES\")", "True"),
            new CompChoiceDef(Params.CompChoice.FALSE, "Is false (\"NO\")", "False"),
            new CompChoiceDef(Params.CompChoice.EQ, "== (equal)", "=="),
            new CompChoiceDef(Params.CompChoice.NEQ, "!= (not equal)", "!="),
            new CompChoiceDef(Params.CompChoice.GT, "> (greater than)", ">"),
            new CompChoiceDef(Params.CompChoice.GTE, ">= (greater than or equal to)", ">="),
            new CompChoiceDef(Params.CompChoice.LT, "< (less than)", "<"),
            new CompChoiceDef(Params.CompChoice.LTE, "<= (less than or equal to)", "<="),
            new CompChoiceDef(Params.CompChoice.RANGE, "Within range, inclusive, e.g. 100,500", "Within range"),
            new CompChoiceDef(Params.CompChoice.RANGENOT, "Outside range, NOT inclusive, e.g. 100,500", "!Within range"),
            new CompChoiceDef(Params.CompChoice.STARTSWITH, "Starts with", "Starts with"),
            new CompChoiceDef(Params.CompChoice.STARTSWITHNOT, "Does NOT start with", "!Starts with"),
            new CompChoiceDef(Params.CompChoice.ENDSWITH, "Ends with", "Ends with"),
            new CompChoiceDef(Params.CompChoice.ENDSWITHNOT, "Does NOT end with", "!Ends with"),
            new CompChoiceDef(Params.CompChoice.MATCHES, "Exact match (can load values from file)", "Exact Match"),
            new CompChoiceDef(Params.CompChoice.MATCHESNOT, "Does NOT match exactly (can load values from file)", "!Exact Match"),
            new CompChoiceDef(Params.CompChoice.MATCHESALL, "Matches ALL selected values", "Matches ALL"),
            new CompChoiceDef(Params.CompChoice.MATCHESALLNOT, "Does NOT match ALL selected values", "!Matches ALL"),
            new CompChoiceDef(Params.CompChoice.MATCHESANY, "Matches ANY of the selected values", "Matches ANY"),
            new CompChoiceDef(Params.CompChoice.MATCHESANYNOT, "Does NOT match ANY of the selected values", "!Matches ANY"),
            new CompChoiceDef(Params.CompChoice.CONTAINS, "Contains", "Contains"),
            new CompChoiceDef(Params.CompChoice.CONTAINSNOT, "Does NOT contain", "Contains NOT"),
            new CompChoiceDef(Params.CompChoice.CONTAINSANY, "Contains ANY, at least one, of the selected values", "Contains ANY"),
            new CompChoiceDef(Params.CompChoice.CONTAINSANYNOT, "Does NOT contain ANY of the selected values", "!Contains ANY"),
            new CompChoiceDef(Params.CompChoice.CONTAINSALL, "Contains ALL of the selected values", "Contains ALL"),
            new CompChoiceDef(Params.CompChoice.CONTAINSALLNOT, "Does NOT contains ALL of the selected values", "!Contains ALL")
        );
        static public enum SelectionType {
            LIST, TEXT
        }

        String field;
        SelectAction action;
        boolean select, newSelect;
        CompType compType;
        CompChoice compChoice;
        String value;
        boolean boolValue = false;
        ArrayList<ColumnDefinition> lstFields;
        HashMap<String, String> hmParams;
        ArrayList<String> lstValues = new ArrayList<>();
        ArrayList<Integer> lstIntValues = new ArrayList<>();
        ArrayList<IntRange> lstIntRangeValues = new ArrayList<>();
        ArrayList<Double> lstDblValues = new ArrayList<>();
        ArrayList<DblRange> lstDblRangeValues = new ArrayList<>();
        
        // most values are required, if N/A then will generate exception
        public Params(ArrayList<ColumnDefinition> lstFields, HashMap<String, String> hmParams) {
            this.lstFields = lstFields;
            this.hmParams = hmParams;
            
            // required parameters
            this.field = hmParams.get(FIELD_PARAM);
            ColumnDefinition fcd = null; 
            for(ColumnDefinition cd : lstFields) {
                if(cd.id.equals(field)) {
                    fcd = cd;
                    break;
                }
            }
            this.action = SelectAction.valueOf(hmParams.get(ACTION_PARAM));
            this.select = action.equals(SelectAction.ADD);
            this.newSelect = Boolean.valueOf(hmParams.get(NEWSEL_PARAM));
            this.compType = CompType.valueOf(hmParams.get(COMPTYPE_PARAM));
            this.compChoice = CompChoice.valueOf(hmParams.get(COMPARISON_PARAM));
            if(hmParams.containsKey(VALUE_PARAM))
                this.value = hmParams.get(VALUE_PARAM).trim();
            else
                this.value = "";
            
            // we allow multiple values to be specified separated by ";"
            String[] fields = value.split(";");
            for(String val : fields)
                lstValues.add(val.trim());
            // this is an odd issue to allow seleting empty fields
            if(this.compType.equals(CompType.LISTIDVALS)) {
                if(value.endsWith(";"))
                    lstValues.add("");
            }
            
            // setup data based on comparison
            switch(compType) {
                case NUMINT:
                    try {
                        for(String val : lstValues) {
                            if(compChoice.equals(CompChoice.RANGE) || compChoice.equals(CompChoice.RANGENOT)) {
                                fields = val.split(",");
                                lstIntRangeValues.add(new IntRange(Integer.parseInt(fields[0].trim()), Integer.parseInt(fields[1].trim())));
                            }
                            else
                                lstIntValues.add(Integer.parseInt(val));
                        }
                    } catch(Exception e) { System.out.println("DlgSelectRows Params INT exception: " + e.getMessage()); }
                    break;
                case NUMDBL:
                case NUMDBLPN:
                    try {
                        for(String val : lstValues) {
                            if(compChoice.equals(CompChoice.RANGE) || compChoice.equals(CompChoice.RANGENOT)) {
                                fields = val.split(",");
                                lstDblRangeValues.add(new DblRange(Double.parseDouble(fields[0].trim()), Double.parseDouble(fields[1].trim())));
                            }
                            else
                                lstDblValues.add(Double.parseDouble(val));
                        }
                    } catch(Exception e) { System.out.println("DlgSelectRows Params NUMDBL exception: " + e.getMessage()); }
                    break;
                case BOOL:
                    try {
                        boolValue = Boolean.valueOf(compChoice.name().toLowerCase());
                    } catch(Exception e) { System.out.println("DlgSelectRows Params BOOL exception: " + e.getMessage()); }
                    break;
                case STRAND:
                    try {
                        String cmpval = hmParams.get(COMPARISON_PARAM);
                        boolValue = CompChoice.POSITIVE.name().equals(cmpval);
                    } catch(Exception e) { System.out.println("DlgSelectRows Params STRAND exception: " + e.getMessage()); }
                    break;
            }                    
        }

        public boolean isCriteriaMet(String data) {
            boolean result = false;
            int cnt = 0;
            for(String val : lstValues) {
                switch(compChoice) {
                    case CONTAINS:
                        result = data.toLowerCase().contains(val.toLowerCase());
                        break;
                    case CONTAINSNOT:
                        if(!data.toLowerCase().contains(val.toLowerCase()))
                            cnt++;
                        break;
                    case STARTSWITH:
                        result = data.toLowerCase().startsWith(val.toLowerCase());
                        break;
                    case STARTSWITHNOT:
                        if(!data.toLowerCase().startsWith(val.toLowerCase()))
                            cnt++;
                        break;
                    case ENDSWITH:
                        result = data.toLowerCase().endsWith(val.toLowerCase());
                        break;
                    case ENDSWITHNOT:
                        if(!data.toLowerCase().endsWith(val.toLowerCase()))
                            cnt++;
                        break;
                    case MATCHES:
                        result = data.toLowerCase().equals(val.toLowerCase());
                        break;
                    case MATCHESNOT:
                        if(!data.toLowerCase().equals(val.toLowerCase()))
                            cnt++;
                        break;
                }
                
                // check if criteria met
                if(result)
                    break;
            }
            
            // if we are doing a "NOT" condition then results are treated like AND - all must be true to return true
            if(!result && cnt > 0 && cnt == lstValues.size())
                result = true;
            return result;
        }
        public boolean isCriteriaMet(double dblValue) {
            boolean result = false;
            int cnt = 0;
            if(compChoice.equals(CompChoice.RANGE) || compChoice.equals(CompChoice.RANGENOT)) {
                for(DblRange range : lstDblRangeValues) {
                    switch(compChoice) {
                        case RANGE:
                            result = dblValue >= Math.min(range.start, range.end) && dblValue <= Math.max(range.start, range.end);
                            break;
                        case RANGENOT:
                            if(!(dblValue >= Math.min(range.start, range.end) && dblValue <= Math.max(range.start, range.end)))
                                cnt++;
                            break;
                    }

                    // check if criteria met
                    if(result)
                        break;
                }
            }
            else {
                for(double dblVal : lstDblValues) {
                    switch(compChoice) {
                        case EQ:
                            result = dblValue == dblVal;
                            break;
                        case NEQ:
                            if(dblValue != dblVal)
                                cnt++;
                            break;
                        // none of the conditions below are allowed multiple values
                        case GT:
                            result = dblValue > dblVal;
                            break;
                        case GTE:
                            result = dblValue >= dblVal;
                            break;
                        case LT:
                            result = dblValue < dblVal;
                            break;
                        case LTE:
                            result = dblValue <= dblVal;
                            break;
                    }

                    // check if criteria met
                    if(result)
                        break;
                }
            }
            
            // if we are doing a "NOT" condition then results are treated like AND - all must be true to return true
            if(!result && cnt > 0 && cnt == lstValues.size())
                result = true;
            return result;
        }
        public boolean isCriteriaMet(int intValue) {
            boolean result = false;
            int cnt = 0;
            if(compChoice.equals(CompChoice.RANGE) || compChoice.equals(CompChoice.RANGENOT)) {
                for(IntRange range : lstIntRangeValues) {
                    switch(compChoice) {
                        case RANGE:
                            result = intValue >= Math.min(range.start, range.end) && intValue <= Math.max(range.start, range.end);
                            break;
                        case RANGENOT:
                            if(!(intValue >= Math.min(range.start, range.end) && intValue <= Math.max(range.start, range.end)))
                                cnt++;
                            break;
                    }

                    // check if criteria met
                    if(result)
                        break;
                }
            }
            else {
                for(int intVal : lstIntValues) {
                    switch(compChoice) {
                        case EQ:
                            result = intValue == intVal;
                            break;
                        case NEQ:
                            if(intValue != intVal)
                                cnt++;
                            break;
                        // none of the conditions below are allowed multiple values
                        case GT:
                            result = intValue > intVal;
                            break;
                        case GTE:
                            result = intValue >= intVal;
                            break;
                        case LT:
                            result = intValue < intVal;
                            break;
                        case LTE:
                            result = intValue <= intVal;
                            break;
                    }

                    // check if criteria met
                    if(result)
                        break;
                }
            }
            
            // if we are doing a "NOT" condition then results are treated like AND - all must be true to return true
            if(!result && cnt > 0 && cnt == lstValues.size())
                result = true;
            return result;
        }
        public boolean isCriteriaMet(boolean boolValue) {
            boolean result = (boolValue == this.boolValue);
            return result;
        }
        public boolean isCriteriaMet(ArrayList<String> lst) {
            boolean result = false;
            switch(compChoice) {
                case MATCHESANY:
                case MATCHESANYNOT:
                    for(String selection : lst) {
                        int idx = lstValues.indexOf(selection);
                        if(idx != -1) {
                            result = true;
                            break;
                        }
                    }
                    if(compChoice.equals(CompChoice.MATCHESANYNOT))
                            result = !result;
                    break;
            }
            return result;
        }
        
        //
        // Static Functions
        //
        
        private static int getActionIdx(String id) {
            int idx = 0;
            for(EnumData entry : lstActions) {
                if(entry.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= lstActions.size())
                idx = 0;
            return idx;
        }
        public static CompChoiceDef getCompChoiceDef(Params.CompChoice cc) {
            CompChoiceDef def = null;
            for(CompChoiceDef ccd : lstCompChoices) {
                if(ccd.comp.equals(cc)) {
                    def = ccd;
                    break;
                }
            }
            return def;
        }
        public static CompChoiceDef getCompChoiceDef(Params.CompChoice cc, List<CompChoiceDef> lstCCDefs) {
            CompChoiceDef def = null;
            for(CompChoiceDef ccd : lstCCDefs) {
                if(ccd.comp.equals(cc)) {
                    def = ccd;
                    break;
                }
            }
            return def;
        }
        private static EnumData getActionDef(String name) {
            int idx = 0;
            for(EnumData entry : lstActions) {
                if(entry.name.equals(name))
                    break;
                idx++;
            }
            if(idx >= lstActions.size())
                idx = 0;
            return lstActions.get(idx);
        }

        //
        // Internal Static Functions
        //
        private static class IntRange {
            int start, end;
            public IntRange(int start, int end) {
                this.start = start;
                this.end = end;
            }
        }
        private static class DblRange {
            double start, end;
            public DblRange(double start, double end) {
                this.start = start;
                this.end = end;
            }
        }
    }
    public static class TTVSelections implements Comparable<TTVSelections> {
        public SimpleBooleanProperty selected;
        public final SimpleStringProperty cat;
        public final SimpleStringProperty id;
        
        public TTVSelections(String cat, String id, boolean selected) {
            this.selected = new SimpleBooleanProperty(selected);
            this.cat = new SimpleStringProperty(cat);
            this.id = new SimpleStringProperty(id);
        }
        public SimpleBooleanProperty SelectedProperty() { return selected; }        
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean val) { selected.set(val); }
        public String getCategory() { return cat.get(); }
        public String getId() { return id.get(); }
        
        @Override
        public int compareTo(TTVSelections td) {
            return (cat.get().compareTo(td.cat.get()));
        }
    }
}
