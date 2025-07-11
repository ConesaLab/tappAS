/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import tappas.DataApp.DataType;
import tappas.DataDFI.DFISelectionResults;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDFIResults extends SubTabBase {
    TableView tblData;

    String analysisId = "";
    String dfiName = "";
    boolean hasData = false;
    int totalFeatures = 0;
    Path runScriptPath = null;
    String panel;
    DataDFI dfiData;
    //DlgDFIAnalysis.Params dfiParams = new DlgDFIAnalysis.Params();
    DlgDFIAnalysis.Params dfiParams;
    
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("GENE",        "", "Gene",             "Gene", 125, "TOP-CENTER", "Gene name", CompType.TEXT),
        new ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),
        new ColumnDefinition("SOURCE",      "", "Source",           "Source", 100, "TOP-CENTER", "Annotation source", CompType.LIST),
        new ColumnDefinition("FEATURE",     "", "Feature",          "Feature", 100, "TOP-CENTER", "Annotation feature", CompType.LIST),
        new ColumnDefinition("FEATUREID",   "", "Feature Id",       "FeatureId", 100, "TOP-CENTER", "Annotation feature id", CompType.TEXT),
        new ColumnDefinition("FEATUREDESC",    "", "Feature Description", "FeatureDescription", 200, "", "Feature description", CompType.TEXT),
        new ColumnDefinition("POSITION",    "", "Position",         "Position", 100, "", "Annotation feature id position", CompType.TEXT),
        new ColumnDefinition("RESULT",      "", "DFI Result",      "DS", 100, "TOP-CENTER", "DFI Result", CompType.LIST),
        new ColumnDefinition("PVALUE",      "", "P-Value",          "PValue", 100, "TOP-RIGHT", "P-Value", CompType.NUMDBL),
        new ColumnDefinition("QVALUE",      "", "Q-Value",          "QValue", 100, "TOP-RIGHT", "Q-Value", CompType.NUMDBL),
        new ColumnDefinition("FAVORED",     "", "Favored Condition","Favored", 150, "TOP-CENTER", "Favored condition", CompType.LIST),
        new ColumnDefinition("PODIUMCHG",   "", "Major Isoform Switching",        "PodiumChange", 190, "TOP-CENTER", "Podium change", CompType.LIST),
        new ColumnDefinition("PODIUMTIMES",   "", "Switching Times",    "PodiumTimes", 130, "TOP-CENTER", "Podium times", CompType.TEXT),
        new ColumnDefinition("FAVOREDTIMES",   "", "Favored Times",    "FavoredTimes", 120, "TOP-CENTER", "Favored times", CompType.TEXT),
        new ColumnDefinition("CHGVALUE",    "", "ΔFI",         "TotalChange", 100, "TOP-RIGHT", "Total change value", CompType.NUMDBL),
        //new ColumnDefinition("C1L2FC",      "", "C1 L2FC",          "C1L2FC", 100, "TOP-RIGHT", "C1 log2 fold change", CompType.NUMDBL),
        //new ColumnDefinition("C2L2FC",      "", "C2 L2FC",          "C2L2FC", 100, "TOP-RIGHT", "C2 log2 fold change", CompType.NUMDBL),
        //new ColumnDefinition("C3L2FC",      "", "C3 L2FC",          "C3L2FC", 100, "TOP-RIGHT", "C3 log2 fold change", CompType.NUMDBL),
        //new ColumnDefinition("C4L2FC",      "", "C4 L2FC",          "C3L2FC", 100, "TOP-RIGHT", "C4 log2 fold change", CompType.NUMDBL),
        //new ColumnDefinition("C1WITHME",    "", "C1 with",          "C1WithMeanExp", 100, "TOP-RIGHT", "C1 With Feature Id Mean Expression", CompType.NUMDBL),
        //new ColumnDefinition("C2WITHME",    "", "C2 with",          "C2WithMeanExp", 100, "TOP-RIGHT", "C2 With Feature Id Mean Expression", CompType.NUMDBL),
        //new ColumnDefinition("C3WITHME",    "", "C3 with",          "C3WithMeanExp", 100, "TOP-RIGHT", "C3 With Feature Id Mean Expression", CompType.NUMDBL),
        //new ColumnDefinition("C4WITHME",    "", "C4 with",          "C4WithMeanExp", 100, "TOP-RIGHT", "C4 With Feature Id Mean Expression", CompType.NUMDBL),
        //new ColumnDefinition("C1WITHOUTME", "", "C1 without",       "C1WithoutMeanExp", 100, "TOP-RIGHT", "C1 Without Feature Id Mean Expression", CompType.NUMDBL),
        //new ColumnDefinition("C2WITHOUTME", "", "C2 without",       "C2WithoutMeanExp", 100, "TOP-RIGHT", "C2 Without Feature Id Mean Expression", CompType.NUMDBL),
        //new ColumnDefinition("C3WITHOUTME", "", "C3 without",       "C3WithoutMeanExp", 100, "TOP-RIGHT", "C3 Without Feature Id Mean Expression", CompType.NUMDBL),
        //new ColumnDefinition("C4WITHOUTME", "", "C4 without",       "C4WithoutMeanExp", 100, "TOP-RIGHT", "C4 Without Feature Id Mean Expression", CompType.NUMDBL),
        new ColumnDefinition("CHROMO",      "", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
        new ColumnDefinition("STRAND",      "", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
        new ColumnDefinition("ISOCNT",      "", "Isoforms",         "Isoforms", 75, "TOP-RIGHT", "Number of gene isoforms", CompType.NUMINT),
        new ColumnDefinition("CODING",      "", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL)
    );
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabDFIResults(Project project) {
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
            dfiData = new DataDFI(project);
            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.STATSDFI.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.STATSDFI.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");

                // DFI parameter file is required
                result = Files.exists(Paths.get(dfiData.getDFIParamsFilepath(analysisId)));
                if(result) {
                    panel = TabGeneDataViz.Panels.TRANS.name();
                    //params = dfiData.getDFIParams();
                    dfiParams = DlgDFIAnalysis.Params.load(dfiData.getDFIParamsFilepath(analysisId), project);
                    dfiName = dfiParams.name;
                    String shortName = getShortName(dfiParams.name);
                    subTabInfo.title = "DFI: " + shortName;
                    subTabInfo.tooltip = "Differential Feature Inclusion Analysis results for '" + dfiParams.name.trim() + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    
                    hasData = dfiData.hasDFIData(analysisId);
                }
            else
                app.ctls.alertInformation("DFI Gene Association", "Missing DFI parameter file");

            }else{
                logger.logDebug("DFI id not found");
                result = false;
            }
                
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonSignificance() {
        String sigLevel = getSignificanceLevel(dfiParams.sigValue, "DFI Significance Level");
        if(!sigLevel.isEmpty()) {
            dfiParams.sigValue = Double.parseDouble(sigLevel);
            Utils.saveParams(dfiParams.getParams(), dfiData.getDFIParamsFilepath(analysisId));
            ObservableList<DFISelectionResults> data = dfiData.getDFISelectionResults(dfiParams.sigValue, true, analysisId);
            // remove summary file, results affected by significance level
            Utils.removeFile(Paths.get(dfiData.getDFIResultsSummaryFilepath(analysisId)));
            updateDisplayData(data);
            
            // clear any pre-existing search values - worked but left table yellow
            tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
            searchSet(tblData, "", false);
            setTableSearchActive(tblData, false);

            // update summary display if opened
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("updateDFICombinedResults", "DFI");
            tabBase.processRequest(hm);
            
            //if(project.data.isCaseControlExpType()){
                //Remove old results
                Utils.removeFile(project.data.getDFIPlot1Filepath(analysisId));
                Utils.removeFile(project.data.getDFIPlot2Filepath(analysisId));
                Utils.removeFile(project.data.getDFIPlot3Filepath(analysisId));
                Utils.removeFile(project.data.getDFIPlot4Filepath(analysisId));
                Utils.removeFile(project.data.getDFITestFeaturesFilepath(analysisId));
                Utils.removeFile(project.data.getDFITestGenesFilepath(analysisId));
                //Create new data
                String logfilepath = dfiData.getDFILogFilepath(analysisId);
                if(project.data.saveDFIResultAnalysis(tblData, project.data.getMatrixDFIFilepath(analysisId))){
                    appendLogLine("Results save correctly.", logfilepath);
                }
            //}
        }
    }    
    @Override
    protected void onButtonRun() {
        app.ctlr.runDFIAnalysis(analysisId);
    }    
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Genes list (IDs only)", true, "Genes ranked list (IDs and values)");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_DFI_Results_"+dfiName+".tsv";
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
            else if(results.dataType.equals(DlgExportData.Params.DataType.RANKEDLIST.name())) {
                hmColNames.put("Gene", "");
                hmColNames.put("Q-Value", "");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
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
                System.out.println("DFI tblData search called: '" + txt + ", hide: " + hide);
                TableView<DFISelectionResults> tblView = (TableView<DFISelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DFISelectionResults> items = (ObservableList<DFISelectionResults>) info.data;
                if(items != null) {
                    ObservableList<DFISelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DFISelectionResults data : items) {
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
                        for(DFISelectionResults data : items) {
                            if(data.getGene().toLowerCase().contains(txt) || data.getGeneDescription().toLowerCase().contains(txt) || data.getFeatureDescription().toLowerCase().contains(txt) ||
                                    data.getSource().toLowerCase().contains(txt) || data.getFeature().toLowerCase().contains(txt) || data.getFeatureId().toLowerCase().contains(txt)) {
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
        String logdata = app.getLogFromFile(dfiData.getDFILogFilepath(analysisId), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<DFISelectionResults> data) {
        tblData = (TableView) tabNode.lookup("#tblDA_Stats");

        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        
        // create data visualization context menu if just one feature doesn't show the button
        cm = new ContextMenu();
        item = new MenuItem("View Results Summary");
        item.setOnAction((event) -> {
            if(totalFeatures>1) app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYDFI, analysisId);
            else if(project.data.isCaseControlExpType())  app.ctls.alertInformation("DFI Summary Information", "You need to analyze more than 1 feature to see the summary.");
            else app.ctls.alertInformation("DFI Summary Information", "You need to use a case-control experiment to see the summary section for the DFI. (still in development for time series)");
        });
        cm.getItems().add(item);
        subTabInfo.cmDataViz = cm;

        // set table popup context menu
        cm = new ContextMenu();
        item = new MenuItem("Show gene feature id data visualization");
        item.setOnAction((event) -> { showFeatureId(tblData); });
        cm.getItems().add(item);
        addGeneDVItem(cm, tblData, panel);
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);

        // populate table columns list
        String[] names = project.data.getGroupNames();
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // enum DEXSeqValues { QValue, PodiumChange, TotalChange }
            // enum EdgeRValues { PValue, QValue, FavorC2, PodiumChange, TotalChange }
            // don't add columns that are not available for some analysis
// hide position if n/a
            if((cd.id.equals("PVALUE") && 
                    (DlgDFIAnalysis.Params.Method.DEXSEQ.equals(dfiParams.method) || DlgDFIAnalysis.Params.Method.MASIGPRO.equals(dfiParams.method))) ||
               (cd.id.equals("CHGVALUE") && DlgDFIAnalysis.Params.Method.MASIGPRO.equals(dfiParams.method)) ||
               (cd.id.equals("PODIUMTIMES") && !project.data.isSingleTimeSeriesExpType()) ||
               (cd.id.equals("FAVOREDTIMES") && !project.data.isTimeCourseExpType()) ||
               (cd.id.equals("POSITION") && DlgDFIAnalysis.Params.Using.PRESENCE.equals(dfiParams.using)) ||
               //Don't show in Time course because we have FavoredTimes
               (cd.id.equals("FAVORED") && (names.length < 2 || project.data.isTimeCourseExpType())))
                continue;

            if(cd.id.equals("FAVOREDTIMES") && project.data.isMultipleTimeSeriesExpType()) {
                cd.colTitle = "Favored Condition";
            }

            // hide some columns initially
            cd.visible = !(cd.id.equals("GENEDESC") || cd.id.equals("FEATUREDESC") || cd.id.equals("SOURCE") || cd.id.equals("CHROMO") || cd.id.equals("STRAND") || 
                            cd.id.equals("ISOCNT") || cd.id.equals("CODING") || cd.id.equals("C1WITHME") || cd.id.equals("C2WITHME") ||
                            cd.id.equals("C1WITHOUTME") || cd.id.equals("C2WITHOUTME") || cd.id.equals("C1L2FC") || cd.id.equals("C2L2FC"));
            lstTblCols.add(cd);
        }

        // configure table columns
        tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addTableColumns(tblData, lstTblCols, false, false);
        CheckBox chk = addRowNumsSelCols(tblData);
        chk.selectedProperty().addListener((obs, oldData, newData) -> { 
            changeAllRows(newData? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
            tblData.requestFocus();
        });
        updateDisplayData(data);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            // handle special cases - only need to do this once
            switch (cd.id) {
                case "CHROMO":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DFISelectionResults dr : data)
                            hm.put(dr.getChromo(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String chr : hm.keySet())
                            lstSelections.add(chr);
                        Collections.sort(lstSelections, new Chromosome.ChromoComparator());
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                // Note: there could be hundreds or even thousands of feature IDs so not doing a list
                case "FAVORED":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DFISelectionResults dr : data)
                            hm.put(dr.getFavored(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String favored : hm.keySet())
                            lstSelections.add(favored);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                case "PODIUMCHG":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DFISelectionResults dr : data)
                            hm.put(dr.getPodiumChange(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String val : hm.keySet())
                            lstSelections.add(val);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                            break;
                    }                
                case "SOURCE":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DFISelectionResults dr : data)
                            hm.put(dr.getSource(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String db : hm.keySet())
                            lstSelections.add(db);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List)lstSelections;
                        break;
                    }
                case "FEATURE":
                {
                    HashMap<String, Object> hm = new HashMap<>();
                    for(DFISelectionResults dr : data)
                        hm.put(dr.getFeature(), null);
                    ArrayList<String> lstSelections = new ArrayList<>();
                    for(String cat : hm.keySet())
                        lstSelections.add(cat);
                    Collections.sort(lstSelections);
                    cd.lstSelections = (List)lstSelections;
                        break;
                    }
                case "RESULT":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DFISelectionResults dr : data)
                            hm.put(dr.getDS(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String val : hm.keySet())
                            lstSelections.add(val);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
            }
        }

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataType.GENE, true, true, lstTblCols, data, DFISelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    private void showFeatureId(TableView tbl) {
        // get highlighted row's data and show gene data visualization
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            String msg = "You have multiple rows highlighted.\nHighlight a single row with the gene/feature id you want to visualize.";
            app.ctls.alertInformation("Show Gene Feature Id Data Visualization", msg);
        }
        else {
            DFISelectionResults data = (DFISelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            app.ctlr.drillDownDFIFeatureId(data);
        }
    }
    private void updateDisplayData(ObservableList<DFISelectionResults> data) {
        FXCollections.sort(data);
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);
    }    
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            ObservableList<DFISelectionResults> data = dfiData.getDFISelectionResults(dfiParams.sigValue, true, analysisId);
            totalFeatures = dfiParams.totalFeatures;
            showSubTab(data);
        }
        else {
            String name = project.data.isTimeCourseExpType()? "DFI_TimeCourse.R" : "DFI.R";
            runScriptPath = app.data.getTmpScriptFileFromResource(name);
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<DFISelectionResults> data = null;
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "Differential Feature Inclusion Analysis in Progress...");
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
            String logfilepath = dfiData.getDFILogFilepath(analysisId);
            java.lang.Throwable e = getException();
            if(e != null)
                appendLogLine("DFIAnalysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("DFIAnalysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("Differential Feature Inclusion Analysis", "DFI failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                app.logInfo("DFI completed successfully.");
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateDFICombinedResults", "DFI");
                hm.put("featureId", analysisId);
                tabBase.processRequest(hm);
                // analysis succeeded, hide run analysis screen
                // set page data ready flag and show subtab
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data);
                
                //if(project.data.isCaseControlExpType()){
                    //String logfilepath = dfiData.getDFILogFilepath();
                    if(project.data.saveDFIResultAnalysis(tblData, project.data.getMatrixDFIFilepath(analysisId))){}
                //}
            }
            else {
                String msg = "Differential Feature Inclusion Analysis " + (taskAborted? "Aborted by Request" : "Failed");
                app.ctls.alertWarning("Differential Feature Inclusion Analysis", msg);
                setRunAnalysisScreenTitle("Differential Feature Inclusion Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = dfiData.getDFILogFilepath(analysisId);
                    outputFirstLogLine("Differential Feature Inclusion Analysis thread running...", logfilepath);
                    appendLogLine("Generating required input files...", logfilepath);
                    if(dfiData.genDFIInputFiles(dfiParams.hmFeatures, dfiParams, analysisId)) {
                        if(project.data.saveDFIAnnotCount(dfiParams.hmFeatures, project.data.getDFITotalFeaturesFilepath(analysisId))){
                            // setup script arguments and run it
                            //Creating lists
                            String trans = "";
                            String proteins = "";

                            HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();

                            for(String db : hmType.keySet()){
                                HashMap<String, String> hmFeat = hmType.get(db);
                                for(String feat : hmFeat.keySet()){
                                    if(hmFeat.get(feat).equals("T")){
                                        if(trans.equals(""))
                                            trans = feat;
                                        else
                                            trans = trans + "," + feat;
                                    }else if(hmFeat.get(feat).equals("P")){
                                        if(proteins.equals(""))
                                            proteins = feat;
                                        else
                                            proteins = proteins + "," + feat;
                                    }
                                }
                            }

                            appendLogLine("Starting R script...", logfilepath);
                            List<String> lst = new ArrayList<>();
                            lst.add(app.data.getRscriptFilepath());
                            lst.add(runScriptPath.toString());
                            totalFeatures = dfiParams.totalFeatures;
                            String method = dfiParams.method.name();
                            lst.add("-a" + analysisId);
                            lst.add("-m" + method);
                            lst.add("-i" + project.data.getProjectDataFolder());
                            lst.add("-d" + dfiData.getDFIFolder());
                            lst.add("-o" + dfiData.getDFIFolder());
                            String f = "0";
                            String t = "FOLD";
                            if(dfiParams.filter){
                                f = "" + dfiParams.filterFC;
                                t = "" + dfiParams.filteringType;
                            }
                            lst.add("-f" + f);
                            lst.add("-t" + t);
                            if(project.data.isTimeCourseExpType()) {
                                lst.add("-u" + dfiParams.degree);
                                String cmp = "any";
                                String r = "0";
                                if(project.data.isMultipleTimeSeriesExpType()) {
                                    r = dfiParams.strictType.name();
                                    cmp = "group";
                                }
                                lst.add("-k" + cmp);
                                lst.add("-r" + r);
                            }
                            lst.add("-s" + dfiParams.sigValue);
                            lst.add("-c" + project.data.getDFITotalFeaturesFilepath(analysisId));
                            lst.add("-g1" + project.data.getDFITestFeaturesFilepath(analysisId));
                            lst.add("-g2" + project.data.getDFITestGenesFilepath(analysisId));

                            lst.add("-x" + project.data.getMatrixDFIFilepath(analysisId));
                            lst.add("-lt" + trans);
                            lst.add("-lp" + proteins);
                    
                            app.logInfo("Running Differential Feature Inclusion Analysis...");
                            app.logDebug("DFI arguments: " + lst);
                            runScript(taskInfo, lst, "Differential Feature Inclusion Analysis", logfilepath);
                            try {
                                data = dfiData.getDFISelectionResults(dfiParams.sigValue, true, analysisId);
                            }
                            catch (Exception e) {
                                data = null;
                                appendLogLine("Unable to load DFI results.", logfilepath);
                                app.logError("Unable to load DFI results. Code exception: " + e.getMessage());
                            }

                            // remove script file from temp folder
                            Utils.removeFile(runScriptPath);
                            runScriptPath = null;
                            app.logInfo("Differential Feature Inclusion Analysis completed successfully.");
                        }
                    }
                    else {
                        appendLogLine("Unable to create required files for Differential Feature Inclusion Analysis.", logfilepath);
                        app.logError("Unable to create required files for Differential Feature Inclusion Analysis");
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Differential Feature Inclusion Analysis", task);
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
        String subTabs = TabProjectDataViz.Panels.SUMMARYDFI.toString();
        return subTabs;
    }
}
