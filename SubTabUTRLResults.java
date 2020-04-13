/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.util.Callback;
import tappas.DataApp.DataType;
import tappas.DataUTRL.UTRLSelectionResults;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class SubTabUTRLResults extends SubTabBase {
    TableView tblData;

    public static final String UTRL_INFO = "utrl_info.tsv";
    public static final String UTRL_CAT = "utrl_cat.tsv";
    public String getUTRLFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_UTRL).toString(); }
    public String getUTRLInfoFilepath() { return Paths.get(getUTRLFolder(), UTRL_INFO).toString(); }
    public String getUTRLCatFilepath() { return Paths.get(getUTRLFolder(), UTRL_CAT).toString(); }

    boolean hasData = false;
    Path runScriptPath = null;
    String panel;
    DataUTRL utrlData;
    DataType utrlType;
    String utrlTypeID;
    String utrlTypeName, utrlColTitle;
    TabProjectDataViz.Panels utrlPanel;
    TabProjectDataViz.Panels utrlClusterPanel;
    TabProjectDataViz.Panels utrlVennDiagPanel;
    DlgUTRLAnalysis.Params utrlParams;

    private final List<ColumnDefinition> lstColDefs = new LinkedList<>(Arrays.asList(
        //                   id,            colGroup, colTitle,             propName,           prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("GENE",            "",     "Gene",              "Gene",             125, "TOP-CENTER", "Gene name", CompType.TEXT),

        //Prop. between each group - case control just 2 - do it dynamically

        new ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),

        new ColumnDefinition("CHROMO",      "", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
        new ColumnDefinition("STRAND",      "", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
        new ColumnDefinition("ISOCNT",      "", "Isoforms",         "Isoforms", 75, "TOP-CENTER", "Number of gene isoforms", CompType.NUMINT),
        new ColumnDefinition("CODING",      "", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL)
    ));

    private final List<ColumnDefinition> lstColDefsTimes = new LinkedList<>(Arrays.asList(
            //                   id,            colGroup, colTitle,             propName,           prefWidth, alignment, selection, Params.CompType
            new ColumnDefinition("GENE",            "",     "Gene",              "Gene",             125, "TOP-CENTER", "Gene name", CompType.TEXT),

            new ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),

            new ColumnDefinition("CHROMO",      "", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
            new ColumnDefinition("STRAND",      "", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
            new ColumnDefinition("ISOCNT",      "", "Isoforms",         "Isoforms", 75, "TOP-CENTER", "Number of gene isoforms", CompType.NUMINT),
            new ColumnDefinition("CODING",      "", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL),

            new ColumnDefinition("PVALUTR3",      "UTR3", "P.Value UTR3",           "PVALUTR3", 125, "TOP-CENTER", "UTR3 PValue", CompType.NUMDBL),
            new ColumnDefinition("ADJPVALUTR3",      "UTR3", "Adj. P.Value UTR3",           "ADJPVALUTR3", 125, "TOP-CENTER", "UTR3 Adj. PValue", CompType.NUMDBL),
            new ColumnDefinition("PVALUTR5",      "UTR5", "P.Value UTR5",           "PVALUTR5", 125, "TOP-CENTER", "UTR5 PValue", CompType.NUMDBL),
            new ColumnDefinition("ADJPVALUTR5",      "UTR5", "Adj. P.Value UTR5",           "ADJPVALUTR5", 125, "TOP-CENTER", "UTR5 Adj. PValue", CompType.NUMDBL)


    ));


    ObservableList<MenuItem> lstTableMenus = null;

    public SubTabUTRLResults(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        subTabInfo.btnSignificance = true;
        subTabInfo.btnRun = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            utrlPanel = TabProjectDataViz.Panels.SUMMARYUTRL;
            utrlClusterPanel = TabProjectDataViz.Panels.CLUSTERSUTRL;
            //utrlVennDiagPanel = TabProjectDataViz.Panels.VENNDIAGUTRL;
            utrlData = new DataUTRL(project);
            result = Files.exists(Paths.get(utrlData.getUTRLParamsFilepath()));
            if(result) {
                panel = TabGeneDataViz.Panels.TRANS.name();
                utrlParams = DlgUTRLAnalysis.Params.load(utrlData.getUTRLParamsFilepath(), project);
                hasData = utrlData.hasUTRLData();
            }
            else
                app.ctls.alertInformation("UTRL Gene Association", "Missing UTRL parameter file");

            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonSignificance() {
        String sigLevel = getSignificanceLevel(utrlParams.sigValue, "UTRL Significance Level");
        if(!sigLevel.isEmpty()) {
            utrlParams.sigValue = Double.parseDouble(sigLevel);
            Utils.saveParams(utrlParams.getParams(), utrlData.getUTRLParamsFilepath());
            ObservableList<UTRLSelectionResults> data = utrlData.getUTRLSelectionResults(true);
            // remove summary file, results affected by significance level
            Utils.removeFile(Paths.get(utrlData.getUTRLResultsSummaryFilepath()));
            updateDisplayData(data);

            // clear any pre-existing search values - worked but left table yellow
            tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
            searchSet(tblData, "", false);
            setTableSearchActive(tblData, false);

            // update summary display if opened
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("updateUTRLCombinedResults", "UTRL");
            tabBase.processRequest(hm);
        }
    }
    @Override
    protected void onButtonRun() {
        app.ctlr.runUTRLAnalysis();
    }
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Genes list (IDs only)", true, "Genes ranked list (IDs and values)");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_UTRL_Genes.tsv";
            HashMap<String, String> hmColNames = new HashMap<>();
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put("Gene", "");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
/*            else if(results.dataType.equals(DlgExportData.Params.DataType.RANKEDLIST.name())) {
                hmColNames.put("Gene", "");
                hmColNames.put("Q-Value", "");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }*/
        }
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("setTextLog")) {
            String txt = (String) hm.get("setTextLog");
            TextArea txtLog = (TextArea) nodeLog.lookup("#txtA_Log");
            if(txtLog != null)
                txtLog.setText(txt);
        }
        else if(hm.containsKey("appendTextLog")) {
            String txt = (String) hm.get("appendTextLog");
            TextArea txtLog = (TextArea) nodeLog.lookup("#txtA_Log");
            if(txtLog != null)
                txtLog.appendText(txt);
        }
        return obj;
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        ignoreHideEvent = true;
        if(obj != null) {
            if(obj.equals(tblData)) {
                if(txt.isEmpty() && !hide)
                    setTableSearchActive(tblData, false);
                else
                    setTableSearchActive(tblData, true);
                System.out.println("UTRL tblData search called: '" + txt + ", hide: " + hide);
                TableView<UTRLSelectionResults> tblView = (TableView<UTRLSelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<UTRLSelectionResults> items = (ObservableList<UTRLSelectionResults>) info.data;
                if(items != null) {
                    ObservableList<UTRLSelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(UTRLSelectionResults data : items) {
                                if(data.isSelected())
                                    matchItems.add(data);
                            }
                        }
                        else {
                            // restore original dataset
                            matchItems = items;
                        }
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(UTRLSelectionResults data : items) {
                            if(data.getGene().toLowerCase().contains(txt) || data.getGeneDescription().toLowerCase().contains(txt)) {
                                if(!hide || data.isSelected())
                                    matchItems.add(data);
                            }
                        }
                    }
                    tblView.setItems(matchItems);
                }
            }
        }
        ignoreHideEvent = false;
    }
    public void viewLog() {
        String logdata = app.getLogFromFile(utrlData.getUTRLLogFilepath(), App.MAX_LOG_LINES);
        viewLog(logdata);
    }

    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<UTRLSelectionResults> data) {
        tblData = (TableView) tabNode.lookup("#tblDA_Stats");
        //String[] resultNames = project.data.getResultNames();
        String[] resultNames = project.data.getGroupNames();

        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // cluster options
        cm = new ContextMenu();
        if(project.data.isCaseControlExpType()) {
            // case-control will ignore the group - there is no time series data
            // single time series only has a single group
            String group = resultNames[0];
            item = new MenuItem("View Results Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, utrlPanel); });
            cm.getItems().add(item);
        }

        String[] lstUTR = new String[]{"_UTR3","_UTR5"};

        if(project.data.isTimeCourseExpType()) {
            if(project.data.isMultipleTimeSeriesExpType()) {
                Menu menu = new Menu("View Expression Profile Clusters\'");
                for(String group : resultNames) {
                    for(String utr : lstUTR){
                        item = new MenuItem("View Expression Profile Clusters " + group + " - " + utr);
                        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, utrlClusterPanel, group+utr); });
                        menu.getItems().add(item);
                    }
                    //item = new MenuItem(group);
                    //item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, utrlClusterPanel, group); });
                    //menu.getItems().add(item);
                }
                cm.getItems().add(menu);
            }
            else {
                HashMap<String, Object> hmArgs = new HashMap<>();
                String group = resultNames[0];
                for(String utr : lstUTR){
                    item = new MenuItem("View Expression Profile Clusters " + group + " - " + utr);
                    item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, utrlClusterPanel, group+utr); });
                    cm.getItems().add(item);
                }
            }
        }

        subTabInfo.cmDataViz = cm;

/*        // set table popup context menu
        cm = new ContextMenu();
        item = new MenuItem("Show gene feature id data visualization");
        item.setOnAction((event) -> { showFeatureId(tblData); });
        cm.getItems().add(item);
        addGeneDVItem(cm, tblData, panel);*/

/*        // add drill down data
        MenuItem item_drill = new MenuItem("Drill down data");
        cm.getItems().add(item_drill);
        item_drill.setOnAction((event) -> { try {
            drillDownData(tblData);
            } catch (IOException ex) {
                Logger.getLogger(SubTabUTRLResults.class.getName()).log(Level.SEVERE, null, ex);
            }
        });*/

        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);

        // populate table columns list
        //String[] names = project.data.getGroupNames();
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        if(project.data.isCaseControlExpType()) {
            for (ColumnDefinition cd : lstColDefs) {
                // enum DEXSeqValues { QValue, PodiumChange, TotalChange }
                // enum EdgeRValues { PValue, QValue, FavorC2, PodiumChange, TotalChange }
                // don't add columns that are not available for some analysis

                // hide some columns initially
                cd.visible = !(cd.id.equals("GENEDESC") || cd.id.equals("SOURCE") || cd.id.equals("CHROMO") || cd.id.equals("STRAND") ||
                        cd.id.equals("ISOCNT") || cd.id.equals("CODING") || cd.id.equals("DISTAL_EXPRESSION") || cd.id.equals("PROXIMAL_EXPRESSION"));

                // special case for multiple time series
                if (project.data.isMultipleTimeSeriesExpType()) {
                    lstTblCols.add(cd);
                    if (cd.id.equals("CLUSTER")) {
                        cd.colGroup = resultNames[0];
                        for (int i = 1; i < resultNames.length; i++) {
                            // create duplicate for other groups and add
                            ColumnDefinition cdm = new ColumnDefinition(cd.id + "Cmp" + i, cd.colGroup, cd.colTitle + " Cmp" + i, cd.propName + "Cmp" + i, 125, cd.alignment, cd.selection + " for Cmp" + i, cd.compType);
                            cdm.colGroup = resultNames[i];
                            lstTblCols.add(cdm);
                        }
                    }
                } else
                    lstTblCols.add(cd);
            }
        }else{
            for (ColumnDefinition cd : lstColDefsTimes) {
                // enum DEXSeqValues { QValue, PodiumChange, TotalChange }
                // enum EdgeRValues { PValue, QValue, FavorC2, PodiumChange, TotalChange }
                // don't add columns that are not available for some analysis

                // hide some columns initially
                cd.visible = !(cd.id.equals("GENEDESC") || cd.id.equals("SOURCE") || cd.id.equals("CHROMO") || cd.id.equals("STRAND") ||
                        cd.id.equals("ISOCNT") || cd.id.equals("CODING") || cd.id.equals("DISTAL_EXPRESSION") || cd.id.equals("PROXIMAL_EXPRESSION"));

                // special case for multiple time series
                if (project.data.isMultipleTimeSeriesExpType()) {
                    lstTblCols.add(cd);
                    if (cd.id.equals("CLUSTER")) {
                        cd.colGroup = resultNames[0];
                        for (int i = 1; i < resultNames.length; i++) {
                            // create duplicate for other groups and add
                            ColumnDefinition cdm = new ColumnDefinition(cd.id + "Cmp" + i, cd.colGroup, cd.colTitle + " Cmp" + i, cd.propName + "Cmp" + i, 125, cd.alignment, cd.selection + " for Cmp" + i, cd.compType);
                            cdm.colGroup = resultNames[i];
                            lstTblCols.add(cdm);
                        }
                    }
                } else
                    lstTblCols.add(cd);
            }
        }
        // ADDED NEW COLUMNS DYNAMICALLY - CASE-CONTROL
        if(project.data.isCaseControlExpType()) {
            String[] GroupNames = project.data.getGroupNames();
            String[] UTRnames = {"Expression-weighted UTR3", "Expression-weighted UTR5"};
            String[] colID = {"GRPA3", "GRPB3", "GRPA5", "GRPB5"};
            ColumnDefinition aux = null;
            for (int utr = 0; utr < 2; utr++) {
                for (int i = 0; i < project.data.getGroupNames().length; i++) {
                    if (utr > 0)
                        aux = new ColumnDefinition(colID[utr + i + 1], UTRnames[utr], GroupNames[i], colID[utr + i + 1], 100, "TOP-CENTER", UTRnames[utr] + " length", CompType.NUMDBL);
                    else
                        aux = new ColumnDefinition(colID[utr + i], UTRnames[utr], GroupNames[i], colID[utr + i], 100, "TOP-CENTER", UTRnames[utr] + " length", CompType.NUMDBL);

                    lstTblCols.add(aux);
                }
            }
        }
        // configure table columns
        tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addTableColumns(tblData, lstTblCols, false, false);
        CheckBox chk = addRowNumsSelCols(tblData);
        chk.selectedProperty().addListener((obs, oldData, newData) -> {
            changeAllRows(newData? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
            tblData.requestFocus();
        });

        //updateDisplayData(data);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            // ADDED NEW COLUMNS DYNAMICALLY - DOESNT WORK
            /*for(int utr = 0; utr < 2; utr++) {
                for (int i = 0; i < project.data.getGroupNames().length; i++) {
                    if(cd.id.equals(project.data.getGroupNames()[i]+"_"+UTRnames[utr])){
                        HashMap<String, Double> hm = new HashMap<String,Double>();
                        for(UTRLSelectionResults dr : data){
                            if(utr>0)
                                hm.put(dr.gene.getValue(), dr.expr[utr+i+1]);
                            else
                                hm.put(dr.gene.getValue(), dr.expr[utr+i]);
                        }
                        ArrayList<Double> lstSelections = new ArrayList<Double>();
                        for(String gene : hm.keySet())
                            lstSelections.add(hm.get(gene));
                        cd.lstSelections = (List) lstSelections;
                    }
                }
            }*/
            // handle special cases - only need to do this once
            switch (cd.id) {
                case "CHROMO":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(UTRLSelectionResults dr : data)
                            hm.put(dr.getChromo(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String chr : hm.keySet())
                            lstSelections.add(chr);
                        //Collections.sort(lstSelections, new Chromosome.ChromoComparator());
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
            }
        }


        updateDisplayData(data);

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataType.GENE, true, true, lstTblCols, data, UTRLSelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    private void showFeatureId(TableView tbl) {
        // get highlighted row's data and show gene data visualization
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            String msg = "You have multiple rows highlighted.\nHighlight a single row with the gene id you want to visualize.";
            app.ctls.alertInformation("Show Gene Data Drill Down", msg);
        }
        else {
            UTRLSelectionResults data = (UTRLSelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            app.ctlr.drillDownUTRL(data);
        }
    }

    //DrillDown Data ISO
    private void drillDownData(TableView tbl) throws IOException {
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            app.ctls.alertInformation("UTRL Drill Down Data", "You have multiple rows highlighted.\nHighlight a single row with the feature you are interested in.");
        }
        else {
            UTRLSelectionResults rdata = (UTRLSelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            String gene = rdata.getGene().trim();
            HashMap<String,ArrayList<String>> isoforms = getUTRLIsoforms(gene);
            app.ctlr.drillDownUTRLData(gene, isoforms);
        }
    }

    public HashMap<String, ArrayList<String>> getUTRLIsoforms(String gene) throws IOException {
        HashMap<String, ArrayList<String>> hmData = new HashMap<>();
        ArrayList<String> shortIso = new ArrayList<String>();
        ArrayList<String> longIso = new ArrayList<String>();
        boolean find = false;
        boolean last = false;
        try {
            if(Files.exists(Paths.get(getUTRLCatFilepath()))) {
                    List<String> lines = Files.readAllLines(Paths.get(getUTRLCatFilepath()));
                    // get cluster info
                    String[] fields;
                    int length = 4; //gene and cluster
                    int lnum = 1; // 1st line not count
                    for(String line : lines) {
                        if(last==false && find ==true)
                            break;
                        if(!line.isEmpty() && lnum>1) {
                            fields = line.split("\t");
                            if(fields.length == length) {
                                if(fields[0].equals(gene)){
                                    find = true;
                                    last = true;
                                    if(fields[3].equals("S"))
                                        shortIso.add(fields[1]);
                                    if(fields[3].equals("L"))
                                        longIso.add(fields[1]);
                                }else{
                                    last = false;
                                }
                            }
                            else {
                                logger.logError("Invalid number of columns in line " + lnum + ", " + fields.length + ", in UTRL cluster file.");
                                break;
                            }
                        }
                        lnum++;
                    }
                hmData.put("L", longIso);
                hmData.put("S", shortIso);
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load UTRL info file: " + e.getMessage());
        }
        return hmData;
    }

    private void updateDisplayData(ObservableList<UTRLSelectionResults> data) {
        FXCollections.sort(data);

        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);
    }

    //
    // Data Load
    //

    public static <S, T> Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> createArrayValueFactory(Function<S, T[]> arrayExtractor, final int index) {
        if (index < 0) {
            return cd -> null;
        }
        return cd -> {
            T[] array = arrayExtractor.apply(cd.getValue());
            return array == null || array.length <= index ? null : new SimpleObjectProperty<>(array[index]);
        };
    }

    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            ObservableList<UTRLSelectionResults> data = utrlData.getUTRLSelectionResults(true);
            showSubTab(data);
        }
        else {
            String name = project.data.isTimeCourseExpType()? "UTRL_TimeCourse.R" : "UTRL.R";
            runScriptPath = app.data.getTmpScriptFileFromResource(name);
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<UTRLSelectionResults> data = null;

        // do all service FX thread initialization
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "UTR Lengthening Analysis in Progress...");
        }
        @Override
        protected void onRunning() {
            showProgress();
            setDisableAbortTaskButton(false);
        }
        @Override
        protected void onStopped() {
            hideProgress();
            setDisableAbortTaskButton(true);
        }
        @Override
        protected void onFailed() {
            String logfilepath = utrlData.getUTRLLogFilepath();
            Throwable e = getException();
            if(e != null)
                appendLogLine("UTRLAnalysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("UTRLAnalysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("UTR Lengthening Analysis", "UTRL failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateDEACombinedResults", "UTRL");
                tabBase.processRequest(hm);
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data);
            }
            else {
                String msg = "UTR Lengthening Analysis " + (taskAborted? "Aborted by Request" : "Failed");
                app.ctls.alertWarning("UTR Lengthening Analysis", msg);
                setRunAnalysisScreenTitle("UTR Lengthening Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
            }
        }

        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = utrlData.getUTRLLogFilepath();
                    outputFirstLogLine("UTR Lengthening Analysis thread running...", logfilepath);
                    appendLogLine("Generating required input files...", logfilepath);
                    if(utrlData.genUTRLInputFiles(utrlParams)) { 
                        // setup script arguments and run it
                        appendLogLine("Starting R script...", logfilepath);
                        List<String> lst = new ArrayList<>();
                        lst.add(app.data.getRscriptFilepath());
                        lst.add(runScriptPath.toString());

                        String method = utrlParams.method.name();
                        lst.add("-m" + method);
                        lst.add("-i" + project.data.getProjectDataFolder());
                        lst.add("-o" + utrlData.getUTRLFolder());
                        lst.add("-c" + project.data.getUTRPlotFilepath());
                        String names = "";
                        for(int i = 0; i < project.data.getGroupsCount(); i++){
                            if(i==project.data.getGroupsCount()-1)
                                names = names + project.data.getGroupNames()[i];
                            else
                                names = names + project.data.getGroupNames()[i] + ";";
                        }
                        
                        lst.add("-n" + names);
                        String f = "0";
                        String t = "FOLD";
                        if(utrlParams.filter){
                            f = "" + utrlParams.filterFC;
                            t = "" + utrlParams.filteringType;
                        }
                        lst.add("-f" + f);
                        lst.add("-t" + t);
                        lst.add("-l" + utrlParams.lengthValue);
                        if(project.data.isTimeCourseExpType()) {
                            lst.add("-s" + utrlParams.sigValue);
                            lst.add("-u" + utrlParams.degree);
                            lst.add("-k" + utrlParams.k);
                            lst.add("-c" + (utrlParams.mclust? "1" : "0"));
                            String cmp = "any";
                            String r = "0";
                            if(project.data.isMultipleTimeSeriesExpType()) {
                                r = utrlParams.strictType.name();
                                cmp = "group";
                            }
                            lst.add("-g" + cmp);
                            lst.add("-r" + r);
                        }
                        app.logInfo("Running UTR Lengthening Analysis...");
                        app.logDebug("UTRL arguments: " + lst);
                        runScript(taskInfo, lst, "UTR Lengthening Analysis", logfilepath);
                        try {
                            data = utrlData.getUTRLSelectionResults(true);
                        }
                        catch (Exception e) {
                            data = null;
                            appendLogLine("Unable to load UTRL results.", logfilepath);
                            app.logError("Unable to load UTRL results. Code exception: " + e.getMessage());
                        }

                        // remove script file from temp folder
                        Utils.removeFile(runScriptPath);
                        runScriptPath = null;
                        app.logInfo("UTR Lengthening Analysis completed successfully.");
                    }
                    else {
                        appendLogLine("Unable to create required files for UTR Lengthening Analysis.", logfilepath);
                        app.logError("Unable to create required files for UTR Lengthening Analysis");
                    }
                    return null;
                    }
            };
            taskInfo = new TaskHandler.TaskInfo("UTR Lengthening Analysis", task);
            return task;
        }
    }

    //
    // Static Functions
    //
    
    // Just add all possible sub tabs - no need to bother with experiment type, etc.
    // List is used to close all subtabs that start with given panel names
    // Used to make sure we close all associated subtabs when running the analysis - data will be deleted
    public static String getAssociatedDVSubTabs() {
        String subTabs = TabProjectDataViz.Panels.SUMMARYUTRL.toString();
        return subTabs;
    }
}
