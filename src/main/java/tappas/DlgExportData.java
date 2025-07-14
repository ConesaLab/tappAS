 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

 import javafx.scene.control.*;
 import javafx.stage.Window;
 import tappas.DataApp.EnumData;

 import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgExportData extends DlgBase {
    ComboBox cbData;
    RadioButton rbAll, rbSelected;
    private ArrayList<EnumData> lstDataTypes = new ArrayList<>();
    
    public DlgExportData(Project project, Window window) {
        super(project, window);
    }
    // currently initial params values are ignored - change if needed
    public Params showAndWait(Config config, Params params, ArrayList<EnumData> lstOtherSelections) {
        if(createDialog("ExportData.fxml", "Export Data", true, "Help_Dlg_ExportData.html")) {
            // get control objects
            cbData = (ComboBox) scene.lookup("#cbData");
            rbAll = (RadioButton) scene.lookup("#rbAll");
            rbSelected = (RadioButton) scene.lookup("#rbSelected");

            // generate data selection list
            for(EnumData data : Params.lstMasterDataTypes) {
                if(data.id.equals(Params.DataType.LIST.name()) && config.lstflg) {
                    EnumData newdata = new EnumData(data.id, data.name);
                    lstDataTypes.add(newdata);
                    if(!config.lstName.isEmpty())
                        newdata.name = config.lstName;
                }
                else if(data.id.equals(Params.DataType.RANKEDLIST.name()) && config.rlstflg) {
                    EnumData newdata = new EnumData(data.id, data.name);
                    lstDataTypes.add(newdata);
                    if(!config.lstName.isEmpty())
                        newdata.name = config.rlstName;
                }
                else if(data.id.equals(Params.DataType.TABLEROWS.name()) && config.rowflg) {
                    EnumData newdata = new EnumData(data.id, data.name);
                    lstDataTypes.add(newdata);
                    if(!config.rowName.isEmpty())
                        newdata.name = config.rowName;
                }
            }
            // check if requested to add other selections
            if(lstOtherSelections != null) {
                for(EnumData data : lstOtherSelections)
                    lstDataTypes.add(data);
            }
            
            // populate dialog
            for(EnumData data : lstDataTypes)
                cbData.getItems().add(data.name);
            if(lstDataTypes.size() == 1)
                cbData.getSelectionModel().select(0);

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
                return(new Params(result.get()));
        }
        return null;
    }
    
    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        
        // check project selection
        String errmsg = "";
        int idx = cbData.getSelectionModel().getSelectedIndex();
        if(idx != -1) {
            EnumData dt = lstDataTypes.get(idx);
            String dataType = dt.id;
            results.put(Params.DATATYPE_PARAM, dataType);
        }
        else {
            errmsg = "You must select the data type to export";
            cbData.requestFocus();
        }
        if(errmsg.isEmpty()) {
            if(rbSelected.isSelected())
                results.put(Params.DATASEL_PARAM, Params.DataSelection.SELECTEDROWS.name());
            else
                results.put(Params.DATASEL_PARAM, Params.DataSelection.ALL.name());
        }        
        if(!errmsg.isEmpty())
            results.put("ERRMSG", errmsg);
        return results;
    }
    
    //
    // Data Classes
    //
    public static class Config {
        boolean rowflg;
        String rowName;
        boolean lstflg;
        String lstName;
        boolean rlstflg;
        String rlstName;
        public Config(boolean lstflg, String lstName, boolean rlstflg, String rlstName) {
            this.lstflg = lstflg;
            this.lstName = lstName;
            this.rlstflg = rlstflg;
            this.rlstName = rlstName;
            
            // default values
            this.rowflg = true;
            this.rowName = "";
        }
    }
    public static class Params extends DlgParams {
        public static final String DATATYPE_PARAM = "dataType";
        public static final String DATASEL_PARAM = "dataSelection";

        public static enum DataType {
            TABLEROWS, LIST, RANKEDLIST
        }
        public static enum DataSelection {
            ALL, SELECTEDROWS
        }
        // these are the fixed values - there can be additional ones applicable to some forms
        private static final List<EnumData> lstMasterDataTypes = Arrays.asList(
            new EnumData(DataType.TABLEROWS.name(), "Table rows"),
            new EnumData(DataType.LIST.name(), "Plain list (IDs only)"),
            new EnumData(DataType.RANKEDLIST.name(), "Ranked list (IDs and values)")
        );
        private final ArrayList<EnumData> lstDataTypes = new ArrayList<>();
        private final String dfltDataType = DataType.TABLEROWS.name();
        private final DataSelection dfltDataSelection = DataSelection.ALL;
        
        public String dataType;
        public DataSelection dataSelection; 
        
        public Params() {
            dataType = dfltDataType;
            dataSelection = dfltDataSelection;
        }
        public Params(HashMap<String, String> hmParams) {
            this.dataType = hmParams.containsKey(DATATYPE_PARAM)? hmParams.get(DATATYPE_PARAM) : dfltDataType;
            this.dataSelection = hmParams.containsKey(DATASEL_PARAM)? DataSelection.valueOf(hmParams.get(DATASEL_PARAM)) : dfltDataSelection;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(DATATYPE_PARAM, dataType);
            hm.put(DATASEL_PARAM, dataSelection.name());
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
