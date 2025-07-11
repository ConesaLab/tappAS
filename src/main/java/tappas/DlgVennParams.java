/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgVennParams extends DlgBase {
    Pane paneSets, paneEditor;
    TextArea taMembers;
    TextField txtName, txtFile;
    Label lblInfo, lblEditor;
    RadioButton rbFile, rbMembers;
    Button btnAdd, btnRemove, btnSave, btnEditCancel, btnFile;
    
    ListView lvSets;
    boolean modified = false;
    ArrayList<Params.DataSetDef> lstDefs = new ArrayList<>();
    
    public DlgVennParams(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        if(createDialog("VennParams.fxml", "Venn Diagram Parameters", true, "HelpVennParamsDlg.html")) {
            // create default parameters if not given
            if(dfltParams == null)
                dfltParams = new Params();

            // get control objects
            paneSets = (Pane) scene.lookup("#paneSets");
            paneEditor = (Pane) scene.lookup("#paneEditor");
            txtName = (TextField) scene.lookup("#txtName");
            rbFile = (RadioButton) scene.lookup("#rbFile");
            rbMembers = (RadioButton) scene.lookup("#rbMembers");
            taMembers = (TextArea) scene.lookup("#taMembers");
            lblInfo = (Label) scene.lookup("#lblInfo");
            lblEditor = (Label) scene.lookup("#lblEditor");
            btnAdd = (Button) scene.lookup("#btnAdd");
            btnRemove = (Button) scene.lookup("#btnRemove");
            btnSave = (Button) scene.lookup("#btnSave");
            btnEditCancel = (Button) scene.lookup("#btnCancel");
            btnFile = (Button) scene.lookup("#btnFile");
            txtFile = (TextField) scene.lookup("#txtFile");
            lvSets = (ListView) scene.lookup("#lvSets");

            // restrict and format name
            txtName.textProperty().addListener((observable, oldValue, newValue) -> { onModified(); });            
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
            
            // setup listeners and bindings
            ToggleGroup tg = rbFile.getToggleGroup();
            tg.selectedToggleProperty().addListener((observable, oldValue, newValue) -> { onModified(); });
            taMembers.textProperty().addListener((observable, oldValue, newValue) -> { onModified(); });            
            txtFile.textProperty().addListener((observable, oldValue, newValue) -> { onModified(); });            
            taMembers.disableProperty().bind(rbFile.selectedProperty());
            txtFile.disableProperty().bind(rbMembers.selectedProperty());
            btnFile.disableProperty().bind(rbMembers.selectedProperty());

            // setup button actions
            btnAdd.setOnAction(e -> { onAdd(); });
            btnRemove.setOnAction(e -> { onRemove(); });
            btnSave.setOnAction(e -> { onSave(); });
            btnEditCancel.setOnAction(e -> { onCancel(); });
            btnFile.setOnAction(e -> { onFile(); });

            // populate dialog
            lstDefs = dfltParams.lstDefs;
            for(Params.DataSetDef dsd : lstDefs)
                lvSets.getItems().add(dsd.name);
            lvSets.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> { onSetSelected(); } );

            // setup dialog event handlers
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
                else {
                    if(modified) {
                        if(!app.ctls.alertConfirmation("Data Set Editor", "You have unsaved changes in the editor.\nDo you want to Proceeed?\n", null))
                            event.consume();
                    }
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> params = null;
                System.out.println(b.getButtonData().toString());
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    params = validate(dialog);
                return params;
            });
            dialog.setOnShown(e -> { txtName.requestFocus(); } );
            
            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return new Params(result.get());
        }
        return null;
    }
    
    //
    // Event Handling Functions
    //
    private void onAdd() {
        // clear list selection will cause Add mode by default
        lvSets.getSelectionModel().clearSelection();
    }
    private void onRemove() {
        // remove selected item
        if(lvSets.getSelectionModel().getSelectedIndex() != -1) {
            String name = (String) lvSets.getItems().get(lvSets.getSelectionModel().getSelectedIndex());
            for(Params.DataSetDef dsd : lstDefs) {
                if(name.equals(dsd.name)) {
                    lstDefs.remove(dsd);
                    lvSets.getItems().remove(name);
                    lvSets.requestFocus();
                    break;
                }
            }
        }
        updateControls();
    }
    private void onSave() {
        // validate current definition in editor
        boolean saved = false;
        String name = txtName.getText().trim();
        boolean members = rbMembers.isSelected();
        String value = (members)? taMembers.getText().trim() : txtFile.getText().trim();
        // check that name was provided and some value was provided for file or members
        if(name.isEmpty()) {
            app.ctls.alertInformation("List Definition", "You must specify a list name.");
            txtName.requestFocus();
        }
        else if(value.isEmpty()) {
            app.ctls.alertInformation("List Definition", members? "You must specify list members." : "You must specify list members file.");
            if(members)
                taMembers.requestFocus();
            else
                txtFile.requestFocus();
        }
        else { 
            // check if name already used
            int selIdx = lvSets.getSelectionModel().getSelectedIndex();
            int idx = 0;
            boolean dup = false;
            for(Params.DataSetDef dsd : lstDefs) {
                if(name.equals(dsd.name) && selIdx != idx++) {
                    dup = true;
                    break;
                }
                idx++;
            }
            if(dup) {
                app.ctls.alertInformation("List Definition", "The specified list name is already in use.");
                txtName.requestFocus();
            }
            else {
                // check if new set definition
                if(selIdx == -1) {
                    saved = true;
                    lvSets.getItems().add(name);
                    lstDefs.add(new Params.DataSetDef(name, members, value));
                    // may have already been cleared so need to call clearEditor()
                    if(lvSets.getItems().size() < Params.MAX_SETS) {
                        lvSets.getSelectionModel().clearSelection();
                        clearEditor();
                    }
                    else
                        lvSets.getSelectionModel().select(name);
                }
                else {
                    // existing set definition
                    String oldname = (String) lvSets.getItems().get(lvSets.getSelectionModel().getSelectedIndex());
                    boolean fnd = false;
                    for(Params.DataSetDef dsd : lstDefs) {
                        if(oldname.equals(dsd.name)) {
                            lstDefs.remove(dsd);
                            fnd = true;
                            break;
                        }
                    }
                    if(fnd) {
                        saved = true;
                        lvSets.getItems().set(lvSets.getSelectionModel().getSelectedIndex(), name);
                        lstDefs.add(new Params.DataSetDef(name, members, value));
                        if(lvSets.getItems().size() < Params.MAX_SETS) {
                            // may have already been cleared so need to call clearEditor()
                            lvSets.getSelectionModel().clearSelection();
                            clearEditor();
                        }
                        else
                            lvSets.getSelectionModel().select(name);
                    }
                }
            }
        }
        if(saved)
            modified = false;
        updateControls();
    }
    private void onCancel() {
        clearEditor();
        modified = false;
        updateControls();
    }
    private void onFile() {
        File f = getFileName();
        if(f != null)
            txtFile.setText(f.getPath());
    }
    private void onSetSelected() {
        // show set definition
        if(lvSets.getSelectionModel().getSelectedIndex() == -1) {
            clearEditor();
            lblInfo.setText("Define new list then press Add List button");
            btnSave.setText("Add List");
            txtName.requestFocus();
        }
        else {
            String name = (String) lvSets.getItems().get(lvSets.getSelectionModel().getSelectedIndex());
            for(Params.DataSetDef dsd : lstDefs) {
                if(name.equals(dsd.name)) {
                    lblInfo.setText("Edit selected list then press Update button");
                    btnSave.setText("Update");
                    setEditor(dsd);
                    break;
                }
            }
        }
        
        // need to make sure it's always cleared
        modified = false;
        updateControls();
    }
    private void onModified() {
        modified = true;
        updateControls();
    }
    
    //
    // Internal Functions
    //
    
    private File getFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Select List Members File"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("DList members files", "*.txt", "*.tsv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        return selectedFile;
    }
    private void updateControls() {
        // update editor controls
        btnSave.setDisable(!modified);
        btnEditCancel.setDisable(!modified);

        // update list controls
        boolean sel = lvSets.getSelectionModel().getSelectedIndex() != -1;
        boolean maxed = lvSets.getItems().size() >= Params.MAX_SETS;
        btnAdd.setDisable(maxed | !sel);
        btnRemove.setDisable(lvSets.getItems().isEmpty() | modified | !sel);
        lblEditor.setText(sel? "List Definition Editor - Edit Selected List" : "List Definition Editor - Define New List");
        paneSets.setDisable(modified);
    }
    private void clearEditor() {
        // clear values
        txtFile.setText("");
        taMembers.setText("");
        txtName.setText("");
        txtName.requestFocus();

        // reset to known state
        modified = false;
        updateControls();
    }
    private void setEditor(Params.DataSetDef dsd) {
        // set values
        rbMembers.setSelected(dsd.members);
        rbFile.setSelected(!dsd.members);
        txtFile.setText(dsd.members? "" : dsd.value);
        taMembers.setText(dsd.members? dsd.value : "");
        txtName.setText(dsd.name);
        txtName.requestFocus();
        
        // reset to known state
        modified = false;
        updateControls();
    }

    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        if(lstDefs.size() >= 2)
        {
            // set results
            Params p = new Params(lstDefs);
            results = p.getParams();
        }
        else
            errmsg = "You must specify at least two lists";
        
        if(!errmsg.isEmpty())
        {        
            lvSets.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        return results;
    }

    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static final int MAX_SETS = 5;
        public static final int MAX_NAME_LENGTH = 20;
        public static final String NAME_PARAM = "name";
        public static final String SOURCE_PARAM = "source";
        public static final String VALUE_PARAM = "value";
        
        public static enum Source {
            FILE, MEMBERS
        }
        
        ArrayList<DataSetDef> lstDefs;
        public Params() {
            lstDefs = new ArrayList<>();
        }
        public Params(ArrayList<DataSetDef> lstDefs) {
            this.lstDefs = lstDefs;
        }
        public Params(HashMap<String, String> hmParams) {
            lstDefs = new ArrayList<>();
            for(int i = 1; i <= MAX_SETS; i++) {
                if(hmParams.containsKey(NAME_PARAM + i)) {
                    // must have all values or generate exception
                    String name = hmParams.get(NAME_PARAM + i);
                    boolean members = Source.valueOf(hmParams.get(SOURCE_PARAM + i)).equals(Source.MEMBERS);
                    String value = hmParams.get(VALUE_PARAM + i);
                    lstDefs.add(new DataSetDef(name, members, value));
                }
                else
                    break;
            }
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            for(int i = 1; i <= lstDefs.size(); i++) {
                DataSetDef dsd = lstDefs.get(i-1);
                hm.put(NAME_PARAM + i, dsd.name);
                hm.put(SOURCE_PARAM + i, dsd.members? Source.MEMBERS.name() : Source.FILE.name());
                hm.put(VALUE_PARAM + i, dsd.value);
            }
            return hm;
        }
        
        //
        // Static functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
        public static class DataSetDef {
            public String name;
            public boolean members;
            public String value;
            public DataSetDef(String name, boolean members, String value) {
                this.name = name;
                this.members = members;
                this.value = value;
            }
        }
    }
}
