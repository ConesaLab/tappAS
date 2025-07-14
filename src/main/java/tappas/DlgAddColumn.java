/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.stage.Window;
import tappas.DataApp.EnumData;

import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgAddColumn extends DlgBase {
    TextField txtColName;
    CheckBox chkAutoName, chkPrefix;
    TreeViewFeatures tvFeatures;
    TreeView tvColumns;

    ArrayList<String> lstColNames;
    ArrayList<String> lstColIDSels = new ArrayList<>();
    ArrayList<String> lstColumns = new ArrayList<>();
    HashMap<String, HashMap<String, Object>> hmFeatureSels = new HashMap<>();

    public DlgAddColumn(Project project, Window window) {
        super(project, window);
    }
    
    public Params showAndWait(ArrayList<String> lstColNames, HashMap<String, String> dfltValues) {
        if(createDialog("AddColumn.fxml", "Add Annotation Features Column(s)", true, "Help_Dlg_AddColumn.html")) {
            this.lstColNames = lstColNames;

            // create parameters object
            Params params = new Params(dfltValues);
            
            // get control objects
            txtColName = (TextField) scene.lookup("#txtColName");
            chkAutoName = (CheckBox) scene.lookup("#chkAutoName");
            chkAutoName.setOnAction((event) -> { if(chkAutoName.isSelected()) annotationFeaturesOnSelectionChanged();  });
            txtColName.disableProperty().bind(chkAutoName.selectedProperty());
            tvColumns = (TreeView) scene.lookup("#tvColumns");
            tvColumns.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
            chkPrefix = (CheckBox) scene.lookup("#chkPrefix");
            tvFeatures = new TreeViewFeatures(project, this);

            // populate dialog
            setDefaultValues(params);
            tvFeatures.initialize((TreeView) scene.lookup("#tvFeatures"), hmFeatureSels, true);
            annotationFeaturesOnSelectionChanged();
            
            // setup dialog event handlers
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> dlgParams = null;
                System.out.println(b.getButtonData().toString());
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    dlgParams = validate(dialog);
                }
                return dlgParams;
            });
            
            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return (new Params(result.get()));
        }
        return null;
    }
    private void setDefaultValues(Params params) {
        // set default values
        txtColName.setText(params.name);
        chkAutoName.setSelected(params.autoName);
        chkPrefix.setSelected(params.prefix);
        // default to add ID if none set
        if(params.lstColumnSels.isEmpty())
            params.lstColumnSels.add(Params.ColType.ID.name());
        initColumns(params);

        hmFeatureSels.clear();
        if(!params.features.isEmpty()) {
            // we don't currently have default values passed for selected features since we don't save the params
            // to support it - just change Params and code to be like in DlgFEAnalysis (also single source features)
            System.out.println("Default features provided - not being currently handled in code!");
        }
    }
    private void initColumns(Params params) {
        TreeItem<String> rootItem = new TreeItem<> ("Columns");
        HashMap<String, Object> hm;
        for(EnumData ed : Params.lstColTypes) {
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (ed.name);
            if(params.lstColumnSels.contains(ed.id))
                item.setSelected(true);
            rootItem.getChildren().add(item);
        }
        rootItem.setExpanded(true);
        tvColumns.setShowRoot(false);
        tvColumns.setRoot(rootItem);
    }
    @Override
    protected void annotationFeaturesOnSelectionChanged() {
        if(chkAutoName.isSelected()) {
            String feature = "";
            TreeItem<String> rootItem = tvFeatures.tv.getRoot();
            if(rootItem != null) {
                ObservableList<TreeItem<String>> lst = rootItem.getChildren();
                for(TreeItem ti : lst) {
                    CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
                    if(item.isSelected() || item.isIndeterminate()) {
                        feature = item.getValue();
                        break;
                    }
                }
            }
            if(feature.isEmpty())
                feature = "<SelectedFeature>";
            String name = feature;
            if(name.length() > Params.MAX_NAME_LENGTH)
                txtColName.setText(name.substring(0, Params.MAX_NAME_LENGTH));
            else
                txtColName.setText(name);
        }
    }
    
    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        String txt;
        System.out.println("Validate dialog");
        
        results.put(Params.AUTONAME_PARAM, chkAutoName.isSelected()? "true" : "false");
        results.put(Params.PREFIX_PARAM, chkPrefix.isSelected()? "true" : "false");
        txt = txtColName.getText().trim();
        if(txt.isEmpty())
            errmsg = "You must specify a column name.";
        if(errmsg.isEmpty()) {
            errmsg = validateColumns(results);
            if(errmsg.isEmpty()) {
                String baseName = txt.toLowerCase();
                boolean dup = false;
                for(String colId : lstColIDSels) {
                    String colIdName = Params.getColName(colId, baseName);
                    for(String colName : lstColNames) {
                        if(colName.toLowerCase().equals(colIdName.toLowerCase())) {
                            dup = true;
                            break;
                        }
                    }
                    if(dup) {
                        errmsg = "Specified column name is already in use.";
                        break;
                    }
                }
                if(errmsg.isEmpty()) {
                    results.put(Params.NAME_PARAM, txt);
                    errmsg = tvFeatures.validate(Params.FEATURE_PARAM, results);
                }
                else
                    txtColName.requestFocus();
            }
        }
        else
            txtColName.requestFocus();

        if(!errmsg.isEmpty())
            results.put("ERRMSG", errmsg);
        return results;
    }
    private String validateColumns(HashMap<String, String> results) {
        String errmsg = "";
        String columns = "";
        ObservableList<TreeItem<String>> lst = tvColumns.getRoot().getChildren();
        String col;
        lstColIDSels.clear();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            if(item.isSelected()) {
                col = item.getValue();
                EnumData ed = Params.getColTypeDef(col);
                lstColIDSels.add(ed.id);
                if(!columns.isEmpty())
                    columns += ";";
                columns += ed.id;
            }
        }
        if(!columns.isEmpty())
            results.put(Params.COLUMNS_PARAM, columns);
        else {
            tvColumns.requestFocus();
            errmsg = "You must select at least one column to add.";
        }
        return errmsg;
    }

    //
    // Data Classes
    //
    public static class Params {
        public static int MAX_NAME_LENGTH = 30;

        // parameters, always specified
        public static final String NAME_PARAM = "name";
        public static final String AUTONAME_PARAM = "autoName";
        public static final String COLUMNS_PARAM = "columns";
        public static final String PREFIX_PARAM = "prefix";
        public static final String FEATURE_PARAM = "feature1";

        static public enum ColType {
            COUNT, ID, DESCRIPTION
        }
        private static final List<EnumData> lstColTypes = Arrays.asList(
            new EnumData(ColType.COUNT.name(), "Number of unique features (\"#ColumnTitle\")"),
            new EnumData(ColType.ID.name(), "Feature ID (\"ColumnTitle ID\")"),
            new EnumData(ColType.DESCRIPTION.name(), "Feature description (\"ColumnTitle Description\")")
        );

        String name, columns, features;
        boolean autoName, prefix;
        HashMap<String, String> hmParams;
        ArrayList<String> lstColumnSels = new ArrayList<>();
        
        public Params(HashMap<String, String> hmParams) {
            this.hmParams = hmParams;
            name = "";
            columns = "";
            features = "";
            autoName = true;
            prefix = false;
            
            // set parameter values
            if(hmParams.containsKey(NAME_PARAM))
                name = hmParams.get(NAME_PARAM);
            if(hmParams.containsKey(AUTONAME_PARAM))
                autoName = Boolean.valueOf(hmParams.get(AUTONAME_PARAM));
            if(hmParams.containsKey(COLUMNS_PARAM))
                columns = hmParams.get(COLUMNS_PARAM);
            if(!columns.isEmpty()) {
                String[] fields = columns.split(";");
                for(String col : fields)
                    lstColumnSels.add(col);
            }
            if(hmParams.containsKey(PREFIX_PARAM))
                prefix = Boolean.valueOf(hmParams.get(PREFIX_PARAM));
            if(hmParams.containsKey(FEATURE_PARAM))
                features = hmParams.get(FEATURE_PARAM);
        }
        public ArrayList<String> getSelectedColNames() {
            ArrayList<String> lstColNames = new ArrayList<>();
            for(String id : lstColumnSels)
                lstColNames.add(getColName(id, name));
            return lstColNames;
        }
        public HashMap<String, HashMap<String, Object>> getFeatures() {
            // NOTE: single source features
            HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
            String[] fields = features.split("\t");
            HashMap<String, Object> hm = new HashMap<>();
            for(int i = 0; i < fields.length; i++) {
                if(i == 0)
                    hmFeatures.put(fields[0], hm);
                else
                    hm.put(fields[i], null);
            }
            return hmFeatures;
        }
        
        //
        // Static Functions
        //
        private static EnumData getColTypeDef(String name) {
            int idx = 0;
            for(EnumData entry : lstColTypes) {
                if(entry.name.equals(name))
                    break;
                idx++;
            }
            if(idx >= lstColTypes.size())
                idx = 0;
            return lstColTypes.get(idx);
        }
        public static String getColName(String colId, String baseName) {
            String colName = baseName;
            if(colId.equals(ColType.COUNT.name()))
                colName = "#" + baseName;
            else if(colId.equals(ColType.ID.name()))
                colName = baseName + " ID";
            else if(colId.equals(ColType.DESCRIPTION.name()))
                colName = baseName + " Description";
            return colName;
        }
    }
}
