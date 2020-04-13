 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

 import javafx.beans.binding.Bindings;
 import javafx.beans.value.ObservableValue;
 import javafx.collections.ObservableList;
 import javafx.event.ActionEvent;
 import javafx.scene.control.*;
 import javafx.scene.layout.Pane;
 import javafx.stage.FileChooser;
 import javafx.stage.Window;
 import tappas.AnnotationFiles.AnnotationFileInfo;

 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileReader;
 import java.nio.charset.StandardCharsets;
 import java.nio.file.Files;
 import java.nio.file.Paths;
 import java.util.*;
 import java.util.function.UnaryOperator;

/* TODO
Exception in thread "JavaFX Application Thread" java.lang.NullPointerException
	at tappas.DlgInputData.lambda$showAndWait$9(DlgInputData.java:178)
I think it happens when converting from old format to new format w/ demo
*/

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgInputData extends DlgBase {
    final String msgDemo = "<Demo includes its own expression matrix file>";
    
    Label lblFilterValues, lblFilterCOV;
    Pane panelExpMatrix;
    CheckBox chkFilter, chkNorm;
    TextField txtMatrix, txtDesign, txtList, txtAnnot, txtProject, txtFilterValues, txtFilterCOV;
    Button btnMatrix, btnDesign, btnList, btnAnnot;
    RadioButton rbAppFile, rbUserFile;
    ComboBox cboSpecies, cboFiles, cboExperiment;
    ChoiceBox cboFilters;
    
    ArrayList<AnnotationFileInfo> lstAnnotationFiles = new ArrayList<>();
    ArrayList<String> lstSpecies = new ArrayList<>();
    //HashMap<String, String> vals = new HashMap<>();
    Params params;
    boolean initializing = true;
    boolean newProject = false;
    String projectId = "";

    public DlgInputData(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(boolean newProject, Params dfltParams) throws IllegalArgumentException {
        this.params = dfltParams == null? new Params(null) : dfltParams;
        this.newProject = newProject;
        if(createDialog("InputData.fxml", newProject? "New Project" : "Load Input Data for Current Project", true, "Help_Dlg_NewProject.html")) {
            /// get control objects
            txtProject = (TextField) scene.lookup("#txtProject");
            panelExpMatrix = (Pane) scene.lookup("#panelExpMatrix");
            txtDesign = (TextField) scene.lookup("#txtDesign");
            txtMatrix = (TextField) scene.lookup("#txtMatrix");
            txtAnnot = (TextField) scene.lookup("#txtAnnot");
            rbUserFile = (RadioButton) scene.lookup("#rbUserFile");
            rbAppFile = (RadioButton) scene.lookup("#rbAppFile");
            btnDesign = (Button) scene.lookup("#btnDesign");
            btnMatrix = (Button) scene.lookup("#btnMatrix");
            btnAnnot = (Button) scene.lookup("#btnAnnot");
            chkFilter = (CheckBox) scene.lookup("#chkFilter");
            chkNorm = (CheckBox) scene.lookup("#chkNorm");
            txtFilterValues = (TextField) scene.lookup("#txtFilterValues");
            txtFilterCOV = (TextField) scene.lookup("#txtFilterCOV");
            lblFilterValues = (Label) scene.lookup("#lblFilterValues");
            lblFilterCOV = (Label) scene.lookup("#lblFilterCOV");
            cboExperiment = (ComboBox) scene.lookup("#cboExperiment");
            cboSpecies = (ComboBox) scene.lookup("#cboSpecies");
            cboFiles = (ComboBox) scene.lookup("#cboFiles");
            cboFilters = (ChoiceBox) scene.lookup("#cboFilters");
            txtList = (TextField) scene.lookup("#txtList");
            btnList = (Button) scene.lookup("#btnList");

            // setup dialog
            txtProject.setDisable(!newProject);
            UnaryOperator<TextFormatter.Change> filter = (TextFormatter.Change change) -> {
                if (change.getControlNewText().length() > Params.MAX_PROJECTNAME_LENGTH) {
                    showDlgMsg("Project name may not exceed " + Params.MAX_PROJECTNAME_LENGTH + " characters.");
                    return null;
                } else {
                    showDlgMsg("");
                    return change;
                }
            };            
            UnaryOperator<TextFormatter.Change> filterValue = (TextFormatter.Change change) -> {
                if (change.getControlNewText().length() > Params.MAX_VALDIGS)
                    return null;
                else
                    return change;
            };            
            UnaryOperator<TextFormatter.Change> filterCOV = (TextFormatter.Change change) -> {
                if (change.getControlNewText().length() > Params.MAX_COVDIGS)
                    return null;
                else
                    return change;
            };

            // get annotation files list also used for species
            lstAnnotationFiles = app.getAnnotationFilesList();
            lstSpecies = new ArrayList<>();
            for(AnnotationFileInfo afi : lstAnnotationFiles) {
                String name = afi.genus + " " + afi.species;
                if(!lstSpecies.contains(name))
                    lstSpecies.add(name);
            }
            // add one for other - make sure to keep 2 word format
            lstSpecies.add("Other species");
                
            // setup controls
            txtProject.setTextFormatter(new TextFormatter(filter));
            btnDesign.setOnAction((event) -> { getDesignFile(); });
            btnMatrix.setOnAction((event) -> { getMatrixFile(); });
            btnAnnot.setOnAction((event) -> { getAnnotFile(); });
            rbAppFile.setOnAction((event) -> { onAnnotSelection(event); });
            rbUserFile.setOnAction((event) -> { onAnnotSelection(event); });
            txtAnnot.disableProperty().bind(rbAppFile.selectedProperty());
            btnAnnot.disableProperty().bind(rbAppFile.selectedProperty());
            txtFilterValues.disableProperty().bind(chkFilter.selectedProperty().not());
            txtFilterValues.setTextFormatter(new TextFormatter(filterValue));
            txtFilterCOV.disableProperty().bind(chkFilter.selectedProperty().not());
            txtFilterCOV.setTextFormatter(new TextFormatter(filterCOV));
            lblFilterValues.disableProperty().bind(chkFilter.selectedProperty().not());
            lblFilterCOV.disableProperty().bind(chkFilter.selectedProperty().not());
            for(DataApp.EnumData rep : DataApp.lstExperiments)
                cboExperiment.getItems().add(rep.name);
            cboExperiment.getSelectionModel().clearSelection();
            for(String name : lstSpecies)
                cboSpecies.getItems().add(name);
            cboSpecies.getSelectionModel().clearSelection();
            cboFiles.disableProperty().bind(Bindings.or(rbAppFile.selectedProperty().not(), rbAppFile.disabledProperty()));
            btnList.setOnAction((event) -> { getListFile(); });
            cboSpecies.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(newValue != null && (int) newValue != -1) {
                    // only enable, panes are disabled in initial form - no way to go back to disabled
                    panelExpMatrix.setDisable(false);
                    
                    rbUserFile.setDisable(false);
                    if(cboFiles.getItems() != null)
                        cboFiles.getItems().clear();
                    rbAppFile.setDisable(false);
                    rbAppFile.setSelected(true);
                    String species = lstSpecies.get((int) newValue);
                    for(AnnotationFileInfo afi : lstAnnotationFiles) {
                        String name = afi.genus + " " + afi.species;
                        if(name.equals(species))
                            cboFiles.getItems().add(afi.genus + " " + afi.species + " " + afi.reference + " " + afi.release);

                    }
                    if(cboFiles.getItems().isEmpty()) {
                        rbUserFile.setSelected(true);
                        rbAppFile.setDisable(true);
                    }
                    else {
                        if(initializing) {
                            // check if this already has data from previous processing
                            if(params.refType != null && !params.refRelease.isEmpty()) {
                                String selection = params.genus + " " + params.species + " " + params.refType.name() + " " + params.refRelease;
                                for(String sel : (ObservableList<String>) cboFiles.getItems()) {
                                    if(selection.equals(sel)) {
                                        cboFiles.getSelectionModel().select(sel);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                
            });
            cboFiles.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(newValue != null) {
                    boolean demo = false;
                    if((int) newValue != -1) {
                        int idx = getRefFileIndexFromId((String) cboFiles.getItems().get((int) newValue), lstAnnotationFiles);
                        AnnotationFileInfo afi = lstAnnotationFiles.get(idx);
                        if(afi != null && afi.reference.equals(DataApp.RefType.Demo.name()))
                            demo = true;
                    }
                    if(demo) {
                        txtDesign.setText(msgDemo);
                        txtMatrix.setText(msgDemo);
                        // currently only one demo is provided - change if needed
                        cboExperiment.getSelectionModel().select(getExpTypeIndexFromId(DataApp.ExperimentType.Two_Group_Comparison.name(), DataApp.lstExperiments));
                    }
                    else {
                        if(txtDesign.getText().equals(msgDemo))
                            txtDesign.setText("");
                        if(txtMatrix.getText().equals(msgDemo))
                            txtMatrix.setText("");
                    }
                    txtDesign.setDisable(demo);
                    btnDesign.setDisable(demo);
                    txtMatrix.setDisable(demo);
                    btnMatrix.setDisable(demo);
                    cboExperiment.setDisable(demo);
                }
            });
            // initial change of species will set the reference file type and overwrite expMatrix file name if demo
            if(!params.genus.isEmpty() && !params.species.isEmpty())
                cboSpecies.getSelectionModel().select(getSpeciesIndex(params.genus, params.species));
            if(params.experimentType != null)
                cboExperiment.getSelectionModel().select(getExpTypeIndexFromId(params.experimentType.name(), DataApp.lstExperiments));
            txtProject.setText(params.name);
            txtDesign.setText(params.edFilepath);
            txtMatrix.setText(params.emFilepath);
            txtAnnot.setText(params.afFilepath);
            if(params.useAppRef)
                rbAppFile.setSelected(true);
            else
                rbUserFile.setSelected(true);
            ToggleGroup tg = rbAppFile.getToggleGroup();
            tg.selectedToggleProperty().addListener((observable, oldValue, newValue) -> { onAnnotSelection(null); });
            onAnnotSelection(null);
            if(params.filter) {
                chkFilter.setSelected(true);
                txtFilterValues.setText("" + params.filterValue);
                txtFilterCOV.setText("" + params.filterCOV);
            }
            else
                chkFilter.setSelected(false);
            
            if(params.norm)
                chkNorm.setSelected(true);
            else
                chkNorm.setSelected(false);
            
            // set filter list
            for(DataApp.EnumData ed : Params.lstFilterLists)
                cboFilters.getItems().add(ed.name);
            cboFilters.getSelectionModel().select(Params.getFilterListIndexById(params.filterList.name()));
            txtList.setText(params.lstFilepath);
            cboFilters.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                onFiltersChanged();
            });
            onFiltersChanged();

            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> prm = null;
                System.out.println(b.getButtonData().toString());
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    prm = validate(dialog);
                return prm;
            });
            initializing = false;
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return (new Params(result.get()));
        }
        return null;
    }
    private void onAnnotSelection(ActionEvent event) {
        boolean flg = !rbUserFile.isSelected();
        boolean demo = false;
        if(flg) {
            if(cboFiles.getSelectionModel().getSelectedIndex() != -1) {
                int idx = getRefFileIndexFromId((String) cboFiles.getSelectionModel().getSelectedItem(), lstAnnotationFiles);
                AnnotationFileInfo afi = lstAnnotationFiles.get(idx);
                if(afi != null && afi.reference.equals(DataApp.RefType.Demo.name()))
                    demo = true;
            }
        }
        if(demo) {
            txtDesign.setText(msgDemo);
            txtMatrix.setText(msgDemo);
        }
        else {
            if(txtDesign.getText().equals(msgDemo))
                txtDesign.setText("");
            if(txtMatrix.getText().equals(msgDemo))
                txtMatrix.setText("");
        }
        txtDesign.setDisable(demo);
        btnDesign.setDisable(demo);
        txtMatrix.setDisable(demo);
        btnMatrix.setDisable(demo);
        cboExperiment.setDisable(demo);
    }
    private void onFiltersChanged() {
        // must use getSelectedIndex - called from changed listener
        ArrayList<String> lst = new ArrayList<>();
        if(cboFilters.getSelectionModel().getSelectedIndex() != -1) {
            int idx = Params.getFilterListIndexByName((String)cboFilters.getItems().get(cboFilters.getSelectionModel().getSelectedIndex()));
            Params.FilterList fl = Params.FilterList.valueOf(Params.lstFilterLists.get(idx).id);
            txtList.setDisable(fl.equals(Params.FilterList.NONE));
            btnList.setDisable(fl.equals(Params.FilterList.NONE));
        }
    }
    
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();

        // pass project id back if given
        if(!newProject && !projectId.isEmpty())
            results.put(DlgOpenProject.Params.ID_PARAM, projectId);
            
        // check project selection
        String errmsg = "";
        int sidx = cboSpecies.getSelectionModel().getSelectedIndex();
        if(sidx != -1) {
            results.put(Params.VER_PARAM, "" + Params.PARAM_VER);

            // save species name
            String species = lstSpecies.get(sidx).trim();
            String[] fields = species.split(" ");
            results.put(Params.GENUS_PARAM, fields[0]);
            results.put(Params.SPECIES_PARAM, fields[1]);
            String txt = txtProject.getText().trim();
            if(!txt.isEmpty()) {
                if(newProject) {
                    String name = txt.toLowerCase();
                    ArrayList<Project.ProjectDef> lstProjects = app.getProjectsList();
                    for(Project.ProjectDef def : lstProjects) {
                        if(name.equals(def.name.toLowerCase())) {
                            errmsg = "Specified project's name is already in use.";
                            break;
                        }
                    }
                }
                results.put(DlgOpenProject.Params.NAME_PARAM, txt);
            }
            else {
                if(newProject) {
                    errmsg = "You must enter the name for the project.";
                    txtProject.requestFocus();
                }
            }

            if(errmsg.isEmpty()) {
                int didx = cboExperiment.getSelectionModel().getSelectedIndex();
                if(didx != -1) {
                    // lstExperiments MUST be in same order as display
                    DataApp.EnumData ed = DataApp.lstExperiments.get(didx);
                    results.put(Params.EXPTYPE_PARAM, ed.id);
                    DataApp.ExperimentType et = DataApp.ExperimentType.valueOf(ed.id);

                    // check file paths
                    String inpAnnot;
                    boolean chkMatrix = true;
                    String expDesign = txtDesign.getText().trim();
                    String inpMatrix = txtMatrix.getText().trim();
                    if(rbUserFile.isSelected()) {
                        results.put(Params.USEAPPREF_PARAM, "false");
                        inpAnnot = txtAnnot.getText().trim();
                        errmsg = checkAnnotFile(inpAnnot);
                        if(!errmsg.isEmpty())
                            txtAnnot.requestFocus();
                    }
                    else {
                        // need to get cboFiles setting and process accordingly - may require a download
                        int cboidx = cboFiles.getSelectionModel().getSelectedIndex();
                        int idx = getRefFileIndexFromId((String) cboFiles.getItems().get(cboidx), lstAnnotationFiles);
                        AnnotationFileInfo afi = lstAnnotationFiles.get(idx);
                        results.put(Params.USEAPPREF_PARAM, "true");
                        results.put(Params.REFTYPE_PARAM, afi.reference);
                        results.put(Params.REFFILE_PARAM, afi.toString());
                        results.put(Params.REFREL_PARAM, afi.release);
                        inpAnnot = "";
                        if(afi.reference.equals(DataApp.RefType.Demo.name())) {
                            expDesign = "";
                            inpMatrix = "";
                            chkMatrix = false;
                        }
                    }
                    if(errmsg.isEmpty() && chkMatrix) {
                        errmsg = checkExperimentData(et, expDesign, inpMatrix, results, logger);
                        if(!errmsg.isEmpty())
                            txtMatrix.requestFocus();
                    }

                    if(errmsg.isEmpty()) {
                        results.put(Params.EDFILE_PARAM, expDesign);
                        results.put(Params.EMFILE_PARAM, inpMatrix);
                        results.put(Params.AFILE_PARAM, inpAnnot);

                        if(errmsg.isEmpty()) {
                            if(chkFilter.isSelected()) {
                                results.put(Params.FILTER_PARAM, Boolean.TRUE.toString());
                                txt = txtFilterValues.getText().trim();
                                if(txt.length() > 0) {
                                    try {
                                        Double val = Double.parseDouble(txt);
                                        if(val >= Params.MIN_EMFVAL && val <= Params.MAX_EMFVAL) {
                                            results.put(Params.EMFVAL_PARAM, txt);
                                        }
                                        else {
                                            errmsg = "Invalid level cutoff value entered (" + Params.MIN_EMFVAL + " to " + Params.MAX_EMFVAL + " allowed).";
                                            txtFilterValues.requestFocus();
                                        }
                                    } catch(Exception e) {
                                        errmsg = "Invalid level cutoff value number entered.";
                                        txtFilterValues.requestFocus();
                                    }
                                }
                                else {
                                    errmsg = "You must enter a level cutoff value.";
                                    txtFilterValues.requestFocus();
                                }
                            }
                            else
                                results.put(Params.FILTER_PARAM, Boolean.FALSE.toString());
                            if(chkNorm.isSelected())
                                results.put(Params.NORM_PARAM, Boolean.TRUE.toString());
                            else
                                results.put(Params.NORM_PARAM, Boolean.FALSE.toString());
                        }
                        if(errmsg.isEmpty()) {
                            if(chkFilter.isSelected()) {
                                txt = txtFilterCOV.getText().trim();
                                if(txt.length() > 0) {
                                    try {
                                        Double val = Double.parseDouble(txt);
                                        if(val >= Params.MIN_EMFCOV && val <= Params.MAX_EMFCOV) {
                                            results.put(Params.EMFCOV_PARAM, txt);
                                        }
                                        else {
                                            errmsg = "Invalid COV cutoff value entered (" + Params.MIN_EMFCOV + " to " + Params.MAX_EMFCOV + " allowed).";
                                            txtFilterCOV.requestFocus();
                                        }
                                    } catch(Exception e) {
                                        errmsg = "Invalid COV cutoff value number entered.";
                                        txtFilterCOV.requestFocus();
                                    }
                                }
                                else {
                                    errmsg = "You must enter a COV cutoff value.";
                                    txtFilterCOV.requestFocus();
                                }
                            }
                        }
                        if(!errmsg.isEmpty())
                            results.put("ERRMSG", errmsg);
                    }
                    else
                        results.put("ERRMSG", errmsg);
                }
                else {
                    errmsg = "You must select an experiment type.";
                    cboExperiment.requestFocus();
                    results.put("ERRMSG", errmsg);
                }
            }            
            else
                results.put("ERRMSG", errmsg);

            if(errmsg.isEmpty()) {
                // get filter type
                if(cboFilters.getSelectionModel().getSelectedIndex() != -1) {
                    int idx = Params.getFilterListIndexByName((String)cboFilters.getSelectionModel().getSelectedItem());
                    Params.FilterList fl = Params.FilterList.valueOf(Params.lstFilterLists.get(idx).id);
                    results.put(Params.FILTERLIST_PARAM, fl.name());
                    if(!fl.equals(Params.FilterList.NONE)) {
                        String filepath = txtList.getText().trim();
                        errmsg = checkListFile(filepath, results);
                        if(errmsg.isEmpty())
                            results.put(Params.LISTFILE_PARAM, filepath);
                        else
                            txtList.requestFocus();
                    }
                }
                else {
                    errmsg = "You must select a transcript filtering option.";
                    cboFilters.requestFocus();
                }
                if(!errmsg.isEmpty())
                    results.put("ERRMSG", errmsg);
            }
        }
        else {
            cboSpecies.requestFocus();
            errmsg = "You must select a species.";
            results.put("ERRMSG", errmsg);
        }
        return results;
    }
    // We want to provide meaningful messages to the user - so there is a lot of checking taking place
    public String checkExperimentData(DataApp.ExperimentType experiment, String designPath, String matrixPath, HashMap<String, String> hmResults, Logger logger) {
        DesignResults dr = checkDesignFile(experiment, designPath, hmResults, logger);
        String errmsg = dr.errmsg;
        if(errmsg.isEmpty())
            errmsg = checkExpMatrixFile(experiment, dr.lstCols, matrixPath, hmResults, logger);
        if(!errmsg.isEmpty())
            app.logInfo("Experiment design/matrix file check failed: " + errmsg);
        return errmsg;
    }
    // NOTE: No comment lines allowed in design files
    public DesignResults checkDesignFile(DataApp.ExperimentType experimentType, String filepath, HashMap<String, String> hmResults, Logger logger) {
        DesignResults results = new DesignResults();
        String errmsg = "";
        HashMap<String, Object> hmColNames = new HashMap<>();
        ArrayList<String> lstCols = new ArrayList<>();
        ArrayList<Params.ExpMatrixGroup> lstGroups = new ArrayList<>();
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmGroups = new HashMap<>();
        logger.logInfo("Checking design file: " + filepath);
        DlgInputData.Params.ExperimentLimits lims = new DlgInputData.Params.ExperimentLimits(experimentType);
        try {
            if(!filepath.isEmpty()) {
                File f = new File(filepath);
                long size = f.length();
                double curTime = Double.MIN_VALUE;
                String curGroup = "";
                if(size >= Params.MIN_DESIGN_FILE_SIZE && size <= Params.MAX_DESIGN_FILE_SIZE) {
                    if(f.canRead()) {
                        List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                        int lnum = 1;
                        int colcnt = experimentType.equals(DataApp.ExperimentType.Two_Group_Comparison)? 2 : 3;
                        for(String line : lines) {
                            // process if past header line
                            if(lnum++ > 1) {
                                if(!line.trim().isEmpty()) {
                                    // break column name into sample name and experimental group - will return null if not a valid column name
                                    Params.ColumnInfo ci = Params.ColumnInfo.fromName(line, experimentType);
                                    if(ci != null) {
                                        // dashes are not allowed in column names, R will convert to periods
                                        if(!ci.sample.contains("-")) {
                                            // all columns must have unique names to avoid issues
                                            if(!hmColNames.containsKey(ci.sample)) {
                                                // check if this is a new group
                                                if(!ci.group.equals(curGroup)) {
                                                    // check that we have not processed it before
                                                    // all entries for a group must be continous
                                                    if(hmGroups.containsKey(ci.group)) {
                                                        errmsg = "Samples in group, '" + ci.group + "', not continous, see Help.";
                                                        break;
                                                    }
                                                    curGroup = ci.group;
                                                    curTime = Integer.valueOf(ci.time);
                                                }
                                                else {
                                                    double time = Double.parseDouble(ci.time);
                                                    if(time < curTime) {
                                                        errmsg = "Sample times in group, '" + ci.group + "', not in chronological order, see Help.";
                                                        break;
                                                    }
                                                    curTime = time;
                                                }
                                                lstCols.add(ci.sample);
                                                hmColNames.put(ci.sample, null);
                                                if(hmGroups.containsKey(ci.group)) {
                                                    HashMap<String, HashMap<String, Object>> hmTimes = hmGroups.get(ci.group);
                                                    if(hmTimes.containsKey(ci.time)) {
                                                        HashMap<String, Object> hm = hmTimes.get(ci.time);
                                                        if(!hm.containsKey(ci.sample)) {
                                                            hm.put(ci.sample, null);
                                                            for(Params.ExpMatrixGroup ct : lstGroups) {
                                                                if(ct.name.equals(ci.group)) {
                                                                    for(Params.ExpMatrixTime ts : ct.lstTimes) {
                                                                        if(ts.name.equals(ci.time)) {
                                                                            ts.addSample(ci.sample);
                                                                            break;
                                                                        }
                                                                    }
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        else {
                                                            errmsg = "Duplicate sample name, '" + ci.sample + "', within the same group (" + ci.group + ").";
                                                            break;
                                                        }
                                                    }
                                                    else {
                                                        // add new time with sample
                                                        HashMap<String, Object> hmSamples = new HashMap<>();
                                                        hmSamples.put(ci.sample, null);
                                                        hmTimes.put(ci.time, hmSamples);
                                                        Params.ExpMatrixTime ts = new Params.ExpMatrixTime(ci.time);
                                                        ts.addSample(ci.sample);
                                                        for(Params.ExpMatrixGroup ct : lstGroups) {
                                                            if(ct.name.equals(ci.group)) {
                                                                ct.addTime(ts);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                }
                                                else {
                                                    // add new condition with time and sample
                                                    HashMap<String, Object> hmSamples = new HashMap<>();
                                                    hmSamples.put(ci.sample, null);
                                                    HashMap<String, HashMap<String, Object>> hmTimes = new HashMap<>();
                                                    hmTimes.put(ci.time, hmSamples);
                                                    hmGroups.put(ci.group, hmTimes);
                                                    Params.ExpMatrixTime ts = new Params.ExpMatrixTime(ci.time);
                                                    ts.addSample(ci.sample);
                                                    Params.ExpMatrixGroup ct = new Params.ExpMatrixGroup(ci.group);
                                                    ct.addTime(ts);
                                                    lstGroups.add(ct);
                                                }
                                            }
                                            else {
                                                errmsg = "Duplicate column names, " + ci.sample + ", are not allowed.";
                                                break;
                                            }
                                        }
                                        else {
                                            errmsg = "Sample column names, " + ci.sample + ", can not contain dashes, see Help.";
                                            break;
                                        }
                                    }
                                    else {
                                        errmsg = "Invalid line contents: '" + line + "'";
                                        break;
                                    }
                                }
                            }
                            else {
                                // just check for the right number of columns
                                String[] fields = line.split("\t");
                                if(fields.length != colcnt) {
                                    errmsg = "Design file header must contain " + colcnt + " columns.";
                                    break;
                                }
                            }
                        }

                        // check contents if no errors so far
                        if(errmsg.isEmpty()) {
                            // check for allowed number of conditions
                            int cnt = hmGroups.size();
                            if(cnt < lims.minGroups || cnt > lims.maxGroups) {
                                if(lims.minGroups == lims.maxGroups)
                                    errmsg = "Invalid number of experimental groups, " + cnt + ". Must contain " + lims.minGroups + " experimental groups.";
                                else
                                    errmsg = "Invalid number of experimental groups, " + cnt + ". Valid range is from " + lims.minGroups + " to " + lims.maxGroups + " experimental groups.";
                            }
                            else {
                                // check for min samples per time slot, total time slots, and get total samples count
                                int totalSamples = 0;
                                HashMap<String, Integer> hmTimeCounts = new HashMap<>();
                                for(String group : hmGroups.keySet()) {
                                    HashMap<String, HashMap<String, Object>> hmTimes = hmGroups.get(group);
                                    for(String time : hmTimes.keySet()) {
                                        if(hmTimeCounts.containsKey(time))
                                            hmTimeCounts.put(time, hmTimeCounts.get(time) + hmTimes.size());
                                        else
                                            hmTimeCounts.put(time, hmTimes.size());
                                        int tcnt = hmTimes.get(time).size();
                                        if(tcnt < lims.minTimeSamples) {
                                            if(experimentType.equals(DataApp.ExperimentType.Two_Group_Comparison))
                                                errmsg = "Invalid number of samples, " + tcnt + ", per group. Must have at least " + lims.minTimeSamples + " per group.";
                                            else
                                                errmsg = "Invalid number of samples, " + tcnt + ", per time slot. Must have at least " + lims.minTimeSamples + " per time slot.";
                                            break;
                                        }
                                        totalSamples += tcnt;
                                    }
                                    if(!errmsg.isEmpty())
                                        break;
                                }
                                if(errmsg.isEmpty()) {
                                    cnt = hmTimeCounts.size();
                                    if(cnt < lims.minTimes || cnt > lims.maxTimes) {
                                        if(lims.minTimes == lims.maxTimes)
                                            errmsg = "Invalid number of time slots, " + cnt + ". Must contain " + lims.minTimes + " time slot(s).";
                                        else
                                            errmsg = "Invalid number of time slots, " + cnt + ". Valid range is from " + lims.minTimes + " to " + lims.maxTimes + " time slots.";
                                    }
                                    else {
                                        // ony check the max total limit exceeded - min is checked per time slot
                                        cnt = totalSamples;
                                        if(cnt > lims.maxTotalSamples)
                                            errmsg = "Invalid number of total samples, " + cnt + ". Up to " + lims.maxTotalSamples + " total samples allowed.";
                                        else {
                                            // check that we have at least some shared counts
                                            boolean shared = false;
                                            for(int tc : hmTimeCounts.values()) {
                                                if(tc > 1) {
                                                    shared = true;
                                                    break;
                                                }
                                            }
                                            if(!shared)
                                                errmsg = "There are no shared time slots across samples.";
                                        }
                                    }
                                }

                                if(errmsg.isEmpty()) {
                                    // all checking is done, make sure we have at least one shared time slot
                                    for(String cond : hmGroups.keySet()) {
                                        HashMap<String, HashMap<String, Object>> hmTimes = hmGroups.get(cond);
                                        int times = hmTimes.size();
                                        if(times < lims.minTimes || times > lims.maxTimes) {
                                            errmsg = "Experimental group '" + cond + "' contains invalid number of time slots. Valid range is from " + lims.minTimes + " to " + lims.maxTimes + " time slots.";
                                            break;
                                        }
                                    }
                                }

                                // save results if no errors
                                if(errmsg.isEmpty()) {
                                    int c = 1;
                                    for(Params.ExpMatrixGroup ct : lstGroups) {
                                        String cond = ct.name;
                                        hmResults.put(DlgInputData.Params.GROUP_PARAM + c, cond);
                                        int t = 1;
                                        for(Params.ExpMatrixTime ts : ct.lstTimes) {
                                            String time = ts.name;
                                            hmResults.put(DlgInputData.Params.CTIMES_PREFIX + c + DlgInputData.Params.CTIME_PARAM + t, time);
                                            String samples = "";
                                            for(String sample : ts.lstSampleNames)
                                                samples += (samples.isEmpty()? "" : "\t") + sample;
                                            hmResults.put(DlgInputData.Params.CTSAMPLES_PREFIX1 + c + DlgInputData.Params.CTSAMPLES_PREFIX2 + t + DlgInputData.Params.CTSAMPLES_PARAM, samples);
                                            t++;
                                        }
                                        c++;
                                    }
                                }
                            }
                        }
                    }
                    else
                        errmsg = "Matrix file does not have read access. Check file permissions.";
                }
                else {
                    if(size > Params.MAX_DESIGN_FILE_SIZE)
                        errmsg = "Design file exceeds maximum size allowed of " + (Params.MAX_DESIGN_FILE_SIZE/1000) + "KB";
                    else
                        errmsg = "Design file does not have sufficient data (" + size + "bytes).";
                }
            }
            else
                errmsg = "You must specify the design file's location.";
        } catch(Exception e) {
            errmsg = "Unable to process specified design file: " + e.getMessage();
        }
        
        results.errmsg = errmsg;
        if(errmsg.isEmpty()) {
            results.lstCols = lstCols;
            results.lstGroups = lstGroups;
            results.hmGroups = hmGroups;
            app.logInfo("Design file passed initial check.");
        }
        return results;
    }
    // this is checking the expression matrix provided by the user
    // it is possible that we are only using a subset of the groups and/or samples
    // so we just need to make sure that all the specified samples are present
    public String checkExpMatrixFile(DataApp.ExperimentType experiment, ArrayList<String> lstCols,
                                     String path, HashMap<String, String> hmResults, Logger logger) {
        String errmsg = "";
        logger.logInfo("Checking specified matrix file (" + experiment.name() + "): " + path);
        DlgInputData.Params.ExperimentLimits lims = new DlgInputData.Params.ExperimentLimits(experiment);
        if(!path.isEmpty()) {
            try {
                File f = new File(path);
                long size = f.length();
                if(size >= Params.MIN_MATRIX_FILE_SIZE && size <= Params.MAX_MATRIX_FILE_SIZE) {
                    if(f.canRead()) {
                        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                            String line = br.readLine();
                            if(line != null) {
                                // allow having a # at start of line
                                if(line.startsWith("#"))
                                    line = line.substring(1);
                                line = line.trim();
                                
                                // parse into column fields
                                String[] fields = line.split("\t");
                                int fldcnt = fields.length;
                                if(fldcnt > 0) {
                                    // check that rows match header - may not have the row id column header
                                    int cnt1 = br.readLine().split("\t").length;
                                    int cnt2 = br.readLine().split("\t").length;
                                    if(cnt1 == cnt2 && (cnt1 == fldcnt || cnt1 == (fldcnt + 1))) {
                                        // create a list of columns in expression matrix
                                        int stridx = (cnt1 == fldcnt)? 1 : 0;
                                        ArrayList<String> lstMatrix = new ArrayList<>();
                                        for(int i = stridx; i < fldcnt; i++)
                                            lstMatrix.add(fields[i]);
                                        
                                        // make sure all the columns specified in design file are present
                                        if(lstMatrix.size() >= lstCols.size()) {
                                            for(int i = 0; i < lstCols.size(); i++) {
                                                if(lstMatrix.indexOf(lstCols.get(i)) == -1) {
                                                    errmsg = "Column name specified in design file, " + lstCols.get(i) + ", not found in the matrix file.";
                                                    break;
                                                }
                                            }
                                        }
                                        else
                                            errmsg = "The number of columns specified in the design file, " + lstCols.size() + ", is greater than the number of columns in the matrix file, " + lstMatrix.size() + ".";
                                    }
                                    else
                                        errmsg = "Number of columns in matrix file header and data rows do not match.";
                                }
                                else
                                    errmsg = "Expression matrix must contain some data columns.";
                            }
                            else
                                errmsg = "Unable to read expression matrix file data.";
                        }                        
                    }
                    else
                        errmsg = "Matrix file does not have read access. Check file permissions.";
                }
                else {
                    if(size > Params.MAX_MATRIX_FILE_SIZE)
                        errmsg = "Matrix file exceeds maximum size allowed of " + (Params.MAX_MATRIX_FILE_SIZE/1000000) + "MB";
                    else
                        errmsg = "Matrix file does not have sufficient data (" + size + "bytes).";
                }
            } catch(Exception e) {
                errmsg = "Unable to open specified expression matrix file.";
            }
        }
        else
            errmsg = "You must specify the expression matrix file's location.";
        if(!errmsg.isEmpty())
            app.logInfo("Matrix file check failed: " + errmsg);
        else
            app.logInfo("Matrix file passed initial check.");
        return errmsg;
    }
    private String checkAnnotFile(String path) {
        String errmsg = "";
        app.logInfo("Checking specified annotation file: " + path);
        if(path.length() > 0) {
            try {
                int x = 2;
            } catch(Exception e) {
                errmsg = "Unable to open specified annotation file.";
            }
        }
        else
            errmsg = "You must specify the annotation file's location or, if provided, use one of the application files.";
        return errmsg;
    }
    // We want to provide meaningful messages to the user - so there is a lot of checking taking place
    private String checkListFile(String path, HashMap<String, String> results) {
        String errmsg = "";
        if(path.length() > 0) {
            try {
                File f = new File(path);
                if(f.exists()) {
                    long size = f.length();
                    if(size >= Params.MIN_LIST_FILE_SIZE && size <= Params.MAX_LIST_FILE_SIZE) {
                        if(f.canRead()) {
                            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                                String line = br.readLine();
                                if(line == null)
                                    errmsg = "Unable to read list file data.";
                            }                        
                        }
                        else
                            errmsg = "List file does not have read access. Check file permissions.";
                    }
                    else {
                        if(size > Params.MAX_LIST_FILE_SIZE)
                            errmsg = "List file exceeds maximum size allowed of " + (Params.MAX_LIST_FILE_SIZE/1000000) + "MB";
                        else
                            errmsg = "List file does not have sufficient data (" + size + "bytes).";
                    }
                }
                else
                    errmsg = "Unable to find specified list file.";
            } catch(Exception e) {
                errmsg = "Unable to open specified list file.";
            }
        }
        else
            errmsg = "You must specify the list file's location.";
        return errmsg;
    }
    private void getListFile() {
        File f = getListName("Select Transcripts Filter List File", new FileChooser.ExtensionFilter("List files", "*.txt", "*.tsv"));
        if(f != null){
            app.userPrefs.setImportListFolder(f.getParent());
            txtList.setText(f.getPath());
        }
    }
    private void getDesignFile() {
        File f = getDataName("Select Experiment Design File", new FileChooser.ExtensionFilter("Experiment design files", "*.txt", "*.tsv"));
        if(f != null){
            app.userPrefs.setImportDataFolder(f.getParent());
            txtDesign.setText(f.getPath());
        }
    }
    private void getMatrixFile() {
        File f = getDataName("Select Expression Matrix File", new FileChooser.ExtensionFilter("Expression Matrix files", "*.txt", "*.tsv"));
        if(f != null){
            app.userPrefs.setImportDataFolder(f.getParent());
            txtMatrix.setText(f.getPath());
        }
    }
    private void getAnnotFile() {
        File f = getAnnotName("Select Annotation Data File", new FileChooser.ExtensionFilter("Annotation files", "*.txt", "*.tsv", "*.gff", "*.gff3"));
        if(f != null){
            app.userPrefs.setImportAnnotFolder(f.getParent());
            txtAnnot.setText(f.getPath());
        }
    }
    
    private File getListName(String title, FileChooser.ExtensionFilter ef) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        File f = new File(app.userPrefs.getImportListFolder());
        if(f.exists()) {
            fileChooser.setInitialDirectory(f);
        }
        fileChooser.getExtensionFilters().addAll(ef, new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        return selectedFile;
    }

    private File getDataName(String title, FileChooser.ExtensionFilter ef) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        File f = new File(app.userPrefs.getImportDataFolder());
        if(f.exists()) {
            fileChooser.setInitialDirectory(f);
        }
        fileChooser.getExtensionFilters().addAll(ef, new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        return selectedFile;
    }    
    
    private File getAnnotName(String title, FileChooser.ExtensionFilter ef) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        File f = new File(app.userPrefs.getImportAnnotFolder());
        if(f.exists()) {
            fileChooser.setInitialDirectory(f);
        }
        fileChooser.getExtensionFilters().addAll(ef, new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        return selectedFile;
    }

    private int getSpeciesIndex(String genus, String species) {
        int idx = 0;
        String fullname = genus + " " + species;
        for(String name : lstSpecies) {
            if(name.equals(fullname))
                break;
            idx++;
        }
        if(idx >= lstSpecies.size())
            idx = 0;
        return idx;
    }
    private static int getRefFileIndexFromId(String id, ArrayList<AnnotationFileInfo> files) {
        int idx = 0;
        if(id != null && !id.isEmpty()) {
            String[] fields = id.split(" ");
            if(fields.length == 4) {
                AnnotationFileInfo afid = new AnnotationFileInfo(fields[0], fields[1], fields[2], fields[3], "", false);
                for(AnnotationFileInfo afi : files) {
                    if(afi.compare(afid))
                        break;
                    idx++;
                }
                if(idx >= files.size())
                    idx = 0;
            }
        }
        return idx;
    }
    private static int getExpTypeIndexFromId(String id, List<DataApp.EnumData> lstExperiments) {
        int idx = 0;
        for(DataApp.EnumData ed : lstExperiments) {
            if(ed.id.equals(id))
                break;
            idx++;
        }
        if(idx >= lstExperiments.size())
            idx = 0;
        return idx;
    }
    
    //
    // Data Classes
    //
    public static class DesignResults {
        String errmsg;
        ArrayList<String> lstCols;
        ArrayList<Params.ExpMatrixGroup> lstGroups;
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmGroups;
        public DesignResults() {
            errmsg = "";
            lstCols = new ArrayList<>();
            lstGroups = new ArrayList<>();
            hmGroups = new HashMap<>();
        }
        public DesignResults(String errmsg, ArrayList<String> lstCols, ArrayList<Params.ExpMatrixGroup> lstGroups, HashMap<String, HashMap<String, HashMap<String, Object>>> hmGroups) {
            this.errmsg = errmsg;
            this.lstCols = lstCols;
            this.lstGroups = lstGroups;
            this.hmGroups = hmGroups;
        }
    }

    /*
    Note: A convertion was added for this file mostly to make sure we could handle future changes.
          Keep in mind that refFile only has a valid file name if we are downloading a file from
          the server as a result of the user's selection. Otherwise, it just has something like
          "Mus_musculus_Demo_1.0" which can not be used for accessing any real file.
    */
    public static class Params extends DlgParams {
        public static final int MAX_GROUPS = 4;
        public static final int MAX_TIMESLOTS = 16;
        public static final int MAX_SAMPLES = 1000;
        public static final int PARAM_VER = 1;

        public static int MAX_PROJECTNAME_LENGTH = 50;
        public static long MIN_MATRIX_FILE_SIZE = 10;
        public static long MAX_MATRIX_FILE_SIZE = 500000000;
        public static double MIN_EMFVAL = 0;
        public static double MAX_EMFVAL = 100000;
        public static double MAX_VALDIGS = 6;
        public static double MIN_EMFCOV = 1;
        public static double MAX_EMFCOV = 1000000;
        public static double MAX_COVDIGS = 7;
        
        public static final String VER_PARAM = "version";
        public static final String NAME_PARAM = "name";
        public static final String ID_PARAM = "id";
        public static final String GENUS_PARAM = "genus";
        public static final String SPECIES_PARAM = "species";
        public static final String USEAPPREF_PARAM = "useAppRef";
        public static final String REFTYPE_PARAM = "refType";
        public static final String REFFILE_PARAM = "refFile";
        public static final String REFREL_PARAM = "refRelease";
        public static final String EXPTYPE_PARAM = "experimentType";
        public static final String EDFILE_PARAM = "expDesignFile";
        public static final String EMFILE_PARAM = "expMatrixFile";
        public static final String AFILE_PARAM = "annotationFile";
        public static final String FILTER_PARAM = "filter";
        public static final String NORM_PARAM = "norm";
        public static final String EMFVAL_PARAM = "expFilterValue";
        public static final String EMFCOV_PARAM = "expFilterCOV";
        public static final String GROUP_PARAM = "group";
        public static final String CTIMES_PREFIX = "c";
        public static final String CTIME_PARAM = "_time";
        public static final String CTSAMPLES_PARAM = "_samples";
        public static final String CTSAMPLES_PREFIX1 = "c";
        public static final String CTSAMPLES_PREFIX2 = "_t";
        /* sample entry:
           condition1   PSC
           c1_times1   0H
           c1_t1_samples    XRG10   YGR10   GFR10
           c1_times2   3H
           c1_t2_samples    XRG30   YGR30   GFR30
            ...
        */
        public static final String FILTERLIST_PARAM = "filterList";
        public static final String LISTFILE_PARAM = "listFile";
        public static final int MIN_LIST_FILE_SIZE = 10;
        public static final int MAX_LIST_FILE_SIZE = 500000000;
        public static final int MIN_DESIGN_FILE_SIZE = 10;
        public static final int MAX_DESIGN_FILE_SIZE = 100000;
        public static enum FilterList {
            NONE, INCLUSION, EXCLUSION
        }
        public static final List<DataApp.EnumData> lstFilterLists = Arrays.asList(
            new DataApp.EnumData(FilterList.NONE.name(), "Do not filter transcripts"),
            new DataApp.EnumData(FilterList.INCLUSION.name(), "Inclusion list - only include transcripts contained in inclusion list specified below"),
            new DataApp.EnumData(FilterList.EXCLUSION.name(), "Exclusion list - exclude transcripts contained in exclusion list specified below")
        );

        private String dfltGenus = ""; //Mus";
        private String dfltSpecies = ""; //musculus";
        private DataApp.ExperimentType dfltExperimentType = null; //DataApp.ExperimentType.Two_Group_Comparison;
        private double dfltFilterValue = 1.0;
        private double dfltFilterCOV = 100.0;
        private boolean dfltUseRefAnnot = true;
        private boolean dfltFilter = true;
        private boolean dfltNorm = true;
        private DataApp.RefType dfltRefType = null;
        
        int version;
        String name, id;
        String genus, species;
        DataApp.ExperimentType experimentType;
        boolean useAppRef, filter, norm;
        DataApp.RefType refType;
        String refFile, refRelease;
        FilterList filterList;
        ArrayList<ExpMatrixGroup> lstGroups;
        String afFilepath, edFilepath, emFilepath, lstFilepath;
        double filterValue, filterCOV;

        public String paramId = "";
        public Params(HashMap<String, String> hmParams) throws IllegalArgumentException {
            // convert paramater file if needed
            if(hmParams != null && !hmParams.isEmpty())
                hmParams = convert(hmParams);
            else
                hmParams = new HashMap<>();
            
            version = hmParams.containsKey(VER_PARAM)? Integer.parseInt(hmParams.get(VER_PARAM)) : PARAM_VER;
            name = hmParams.containsKey(NAME_PARAM)? hmParams.get(NAME_PARAM) : "";
            id = hmParams.containsKey(ID_PARAM)? hmParams.get(ID_PARAM) : "";
            genus = hmParams.containsKey(GENUS_PARAM)? hmParams.get(GENUS_PARAM): dfltGenus;
            species = hmParams.containsKey(SPECIES_PARAM)? hmParams.get(SPECIES_PARAM): dfltSpecies;
            experimentType = hmParams.containsKey(EXPTYPE_PARAM)? DataApp.ExperimentType.valueOf(hmParams.get(EXPTYPE_PARAM)) : dfltExperimentType;
            lstGroups = new ArrayList<>();
            for(int c = 1; c <= MAX_GROUPS; c++) {
                if(hmParams.containsKey(Params.GROUP_PARAM + c)) {
                    ExpMatrixGroup ct = new ExpMatrixGroup(hmParams.get(Params.GROUP_PARAM + c));
                    lstGroups.add(ct);
                    for(int t = 1; t < MAX_TIMESLOTS; t++) {
                        if(hmParams.containsKey(CTIMES_PREFIX + c + CTIME_PARAM + t)) {
                            ExpMatrixTime ts = new ExpMatrixTime(hmParams.get(CTIMES_PREFIX + c + CTIME_PARAM + t).trim());
                            ct.addTime(ts);
                            if(hmParams.containsKey(CTSAMPLES_PREFIX1 + c + CTSAMPLES_PREFIX2 + t + CTSAMPLES_PARAM)) {
                                String names[] = hmParams.get(CTSAMPLES_PREFIX1 + c + CTSAMPLES_PREFIX2 + t + CTSAMPLES_PARAM).trim().split("\t");
                                for(String sname : names)
                                    ts.addSample(sname.trim());
                            }
                            else
                                break;
                        }
                        else
                            break;
                    }
                }
                else
                    break;
            }
            useAppRef = hmParams.containsKey(USEAPPREF_PARAM)? Boolean.valueOf(hmParams.get(USEAPPREF_PARAM)) : dfltUseRefAnnot;
            refType = hmParams.containsKey(REFTYPE_PARAM)? DataApp.RefType.valueOf(hmParams.get(REFTYPE_PARAM)) : dfltRefType;
            refFile = hmParams.containsKey(REFFILE_PARAM)? hmParams.get(REFFILE_PARAM) : "";
            refRelease = hmParams.containsKey(REFREL_PARAM)? hmParams.get(REFREL_PARAM) : "";
            filter = hmParams.containsKey(FILTER_PARAM)? Boolean.valueOf(hmParams.get(FILTER_PARAM)) : dfltFilter;
            norm = hmParams.containsKey(NORM_PARAM)? Boolean.valueOf(hmParams.get(NORM_PARAM)) : dfltNorm;
            filterList = hmParams.containsKey(FILTERLIST_PARAM)? FilterList.valueOf(hmParams.get(FILTERLIST_PARAM)) : FilterList.NONE;
            filterValue = hmParams.containsKey(EMFVAL_PARAM)? Double.parseDouble(hmParams.get(EMFVAL_PARAM)) : dfltFilterValue;
            filterCOV = hmParams.containsKey(EMFCOV_PARAM)? Double.parseDouble(hmParams.get(EMFCOV_PARAM)) : dfltFilterCOV;
            afFilepath = hmParams.containsKey(AFILE_PARAM)? hmParams.get(AFILE_PARAM) : "";
            edFilepath = hmParams.containsKey(EDFILE_PARAM)? hmParams.get(EDFILE_PARAM) : "";
            emFilepath = hmParams.containsKey(EMFILE_PARAM)? hmParams.get(EMFILE_PARAM) : "";
            lstFilepath = hmParams.containsKey(LISTFILE_PARAM)? hmParams.get(LISTFILE_PARAM) : "";
        }
        @Override
        // WARNING: Function assumes parameters have been validated - could get exception if not
        public HashMap<String, String> getParams() {
            HashMap<String, String> hmParams = new HashMap<>();
            hmParams.put(VER_PARAM, "" + version);
            hmParams.put(NAME_PARAM, name);
            if(!id.isEmpty())
                hmParams.put(ID_PARAM, id);
            hmParams.put(GENUS_PARAM, genus);
            hmParams.put(SPECIES_PARAM, species);
            if(experimentType != null)
                hmParams.put(EXPTYPE_PARAM, experimentType.name());
            int c = 1;
            for(ExpMatrixGroup emg : lstGroups) {
                hmParams.put(Params.GROUP_PARAM + c, emg.name);
                int t = 1;
                for(ExpMatrixTime emt : emg.lstTimes) {
                    hmParams.put(CTIMES_PREFIX + c + CTIME_PARAM + t, emt.name);
                    String snames = "";
                    for(String sname : emt.lstSampleNames)
                        snames += (snames.isEmpty()? "" : "\t") + sname;
                    hmParams.put(CTSAMPLES_PREFIX1 + c + CTSAMPLES_PREFIX2 + t + CTSAMPLES_PARAM, snames);
                    t++;
                }
                c++;
            }
            hmParams.put(USEAPPREF_PARAM, Boolean.toString(useAppRef));
            if(refType != null)
                hmParams.put(REFTYPE_PARAM, refType.name());
            hmParams.put(REFFILE_PARAM, refFile);
            hmParams.put(REFREL_PARAM, refRelease);
            hmParams.put(FILTER_PARAM, Boolean.toString(filter));
            hmParams.put(NORM_PARAM, Boolean.toString(norm));
            hmParams.put(FILTERLIST_PARAM, filterList.name());
            hmParams.put(EMFVAL_PARAM, "" + filterValue);
            hmParams.put(EMFCOV_PARAM, "" + filterCOV);
            if(!afFilepath.isEmpty())
                hmParams.put(AFILE_PARAM, afFilepath);
            if(!edFilepath.isEmpty())
                hmParams.put(EDFILE_PARAM, edFilepath);
            if(!emFilepath.isEmpty())
                hmParams.put(EMFILE_PARAM, emFilepath);
            if(!lstFilepath.isEmpty())
                hmParams.put(LISTFILE_PARAM, lstFilepath);
            return hmParams;
        }
        // base class implements boolean save(String filepath)

        //
        // Internal Functions
        //
        
        // convertions should be avoided whenever possible
        // this one was done mainly to come up with a method to deal with them
        private HashMap<String, String> convert(HashMap<String, String> hmParams) throws IllegalArgumentException {
            // get version number
            int ver = hmParams.containsKey(VER_PARAM)? Integer.parseInt(hmParams.get(VER_PARAM)) : 0;
            if(ver < PARAM_VER) {
                int verLast = ver;

                // perform convertions, incrementally version by version
                while(ver < PARAM_VER) {
                    switch(ver) {
                        case 0:
                            hmParams = convert_0to1(hmParams);
                            break;
                    }
                    ver = hmParams.containsKey(VER_PARAM)? Integer.parseInt(hmParams.get(VER_PARAM)) : 0;

                    // make sure we don't get stuck in an endless loop
                    if(ver <= verLast) {
                        // unable to perform convertion - throw exception
                        throw new IllegalArgumentException("Unable to convert old project parameters");
                    }
                }
            }
            else {
                if(ver > PARAM_VER) {
                    // unable to perform convertion - throw exception
                    throw new IllegalArgumentException("Unable to use project parameters with older version of tappAS");
                }
            }
            return hmParams;
        }
        private HashMap<String, String> convert_0to1(HashMap<String, String> hmParams) {
            HashMap<String, String> hm = new HashMap<>();
            String[] values;
            for(String param : hmParams.keySet()) {
                switch(param) {
                    case "useAnnot":
                        hm.put(USEAPPREF_PARAM, hmParams.get(param));
                        break;
                    case "refname":
                        hm.put(REFFILE_PARAM, hmParams.get(param));
                        break;
                    case "species":
                        values = hmParams.get(param).split("_");
                        hm.put(GENUS_PARAM, values[0]);
                        hm.put(SPECIES_PARAM, (values.length == 2)? values[1] : "");
                        break;
                    case "refver":
                        hm.put(REFREL_PARAM, hmParams.get(param));
                        break;
                    case "reftype":
                        hm.put(REFTYPE_PARAM, hmParams.get(param));
                        break;
                    default:
                        // all other parameters are saved as is
                        hm.put(param, hmParams.get(param));
                }
            }

            hm.put(VER_PARAM, "1");
            
            return hm;
        }
        
        //
        // Static functions
        //
        public static Params load(String filepath) throws IllegalArgumentException {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
            public static String getExperimentTypeName(DataApp.ExperimentType et) {
                String type = "";
                for(DataApp.EnumData ed : DataApp.lstExperiments) {
                    if(ed.id.equals(et.name())) {
                        type = ed.name;
                        break;
                    }
                }
                return type;
            }
        public static int getFilterListIndexById(String id) {
            int idx = 0;
            for(DataApp.EnumData ed : lstFilterLists) {
                if(ed.id.equals(id))
                    break;
                idx++;
            }
            if(idx >= lstFilterLists.size())
                idx = 0;
            return idx;
        }
        public static int getFilterListIndexByName(String name) {
            int idx = 0;
            for(DataApp.EnumData ed : lstFilterLists) {
                if(ed.name.equals(name))
                    break;
                idx++;
            }
            if(idx >= lstFilterLists.size())
                idx = 0;
            return idx;
        }
                
        //
        // Data Classes
        //
        public static class ExperimentLimits {
            // this class provides some limits to make sure we don't get an unreasonable amount of data
            // these limits are not meant to be written in stone - may need to be adjusted - but it will at least
            // bring to our attention if users are trying to use this for huge datasets
            // one concern is with the table display, paricularly the expression matrix table, 
            // not sure how well Java will deal with hundreds of columns in a table
            int minGroups, maxGroups;
            int minTimes, maxTimes;
            int minTimeSamples;
            int maxTotalSamples;
            public ExperimentLimits(DataApp.ExperimentType et) {
                switch(et) {
                    case Two_Group_Comparison:
                        minGroups = maxGroups = 2;
                        minTimes = maxTimes = 1;
                        minTimeSamples = 2;
                        maxTotalSamples = MAX_SAMPLES;
                        break;
                    case Time_Course_Single:
                        minGroups = maxGroups = 1;
                        minTimes = 2;
                        maxTimes = MAX_TIMESLOTS;
                        minTimeSamples = 2;
                        maxTotalSamples = MAX_SAMPLES;
                        break;
                    case Time_Course_Multiple:
                        minGroups = 1;
                        maxGroups = MAX_GROUPS;
                        minTimes = 2;
                        maxTimes = MAX_TIMESLOTS / 2;
                        minTimeSamples = 2;
                        maxTotalSamples = MAX_SAMPLES;
                        break;
                }
            }
        }
        public static class ExpMatrixGroup {
            // used for condition name and ExpMatrixTime list
            String name;
            ArrayList<ExpMatrixTime> lstTimes;
            public ExpMatrixGroup(String name) {
                this.name = name;
                lstTimes = new ArrayList<>();
            }
            public void addTime(ExpMatrixTime ts) { lstTimes.add(ts); }
            public int getTotalSamplesCount() {
                int cnt = 0;
                for(ExpMatrixTime ts : lstTimes)
                    cnt += ts.lstSampleNames.size();
                return cnt; 
            }
        }
        public static class ExpMatrixTime {
            // used for time name and sample names list
            String name;
            ArrayList<String> lstSampleNames;
            public ExpMatrixTime(String name) {
                this.name = name;
                lstSampleNames = new ArrayList<>();
            }
            public void addSample(String sample) { lstSampleNames.add(sample); }
        }
        public static class ColumnInfo {
            String sample, group, time;
            public ColumnInfo(String sample, double time, String group) {
                this.sample = sample;
                this.time = "" + time;
                // most often time will be in integers, no need to be showing the trailing ".0"
                if(this.time.endsWith(".0"))
                    this.time = this.time.substring(0, this.time.length() - 2);
                this.group = group;
            }
            public static ColumnInfo fromName(String name, DataApp.ExperimentType et) {
                ColumnInfo ci = null;
                String fields[] = name.trim().split("\t");
                switch(et) {
                    case Two_Group_Comparison:
                        if(fields.length == 2)
                            ci = new ColumnInfo(fields[0], 0, fields[1]);
                        break;
                    case Time_Course_Single:
                    case Time_Course_Multiple:
                        if(fields.length == 3)
                            ci = new ColumnInfo(fields[0], Double.parseDouble(fields[1].trim()), fields[2]);
                        break;
                }
                return ci;
            }
        }
    }
}
