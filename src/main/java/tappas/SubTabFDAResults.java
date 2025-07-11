/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tappas.DataAnnotation.AnnotationType;
import static tappas.DataFDA.DataSelectionResults;
import static tappas.DlgSelectRows.ColumnDefinition;
import static tappas.Utils.saveFDAIDResultAnalysis;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFDAResults extends SubTabBase {
    TableView tblData;

    String analysisId = "";
    int totalFeatures = 0;
    boolean hasData = false;
    boolean methodCat = false;
    boolean usingGenPos = false;
    Path runScriptPath = null;
    DataFDA fdaData;
    TaskHandler.TaskAbortFlag taf = new  TaskHandler.TaskAbortFlag(false);
    HashMap<String, String> params = new HashMap<>();
    DlgFDAnalysis.Params fdaParams;
    
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new DlgSelectRows.ColumnDefinition("GENE",        "", "Gene",             "Gene", 125, "", "Gene name", DlgSelectRows.Params.CompType.TEXT),
        new DlgSelectRows.ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", DlgSelectRows.Params.CompType.TEXT)
    );
    
    private final List<ColumnDefinition> lstColDefsID = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new DlgSelectRows.ColumnDefinition("GENE",        "", "Gene",             "Gene", 125, "", "Gene name", DlgSelectRows.Params.CompType.TEXT),
        new DlgSelectRows.ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", DlgSelectRows.Params.CompType.TEXT),
        new DlgSelectRows.ColumnDefinition("PVALUE",    "", "P-Value", "pValue", 90, "center", "P-Value", DlgSelectRows.Params.CompType.NUMDBL),
        new DlgSelectRows.ColumnDefinition("ADJPVALUE",    "", "AdjP-Value", "AdjPValue", 90, "center", "AdjP-Value", DlgSelectRows.Params.CompType.NUMDBL)
    );
    
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabFDAResults(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        subTabInfo.btnRun = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            // parameter data file is required
            fdaData = new DataFDA(project);
            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.STATSFDA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.STATSFDA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");

                // FDA parameter file is required
                result = Files.exists(Paths.get(fdaData.getFDAParamsFilepath(analysisId)));
                if(result) {
                    //params = fdaData.getFDAParams();
                    fdaParams = DlgFDAnalysis.Params.load(fdaData.getFDAParamsFilepath(analysisId));

                    String shortName = getShortName(fdaParams.name);
                    subTabInfo.title = "FDA: " + shortName;
                    subTabInfo.tooltip = "Functional Diversity Analysis results for '" + fdaParams.name.trim() + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    
                    hasData = fdaData.hasFDAData(analysisId);
                    methodCat = fdaParams.method.equals(fdaParams.method.CATEGORY);
                    if(!methodCat){
                        subTabInfo.btnDataViz = false;
                        super.initialize(tabBase, subTabInfo, args);
                    }
                    usingGenPos = fdaParams.using.equals(fdaParams.using.GENPOS);
                }
                else
                    app.ctls.alertInformation("FDA Results", "Missing FDA parameter file");
            }else{
                logger.logDebug("FDA id not found");
                result = false;
            }
            
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonRun() {
        app.ctlr.runFDAnalysis(analysisId);
    }    
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Genes list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String using = usingGenPos? "genomic" : "presence";
            String filename = "tappAS_FDA_"+fdaParams.name.trim()+"_Genes_" + using + ".tsv";
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
                System.out.println("FDA tblData search called: '" + txt + ", hide: " + hide);
                TableView<DataSelectionResults> tblView = (TableView<DataSelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DataSelectionResults> items = (ObservableList<DataSelectionResults>) info.data;
                if(items != null) {
                    ObservableList<DataSelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DataSelectionResults data : items) {
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
                        for(DataSelectionResults data : items) {
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
        String logdata = app.getLogFromFile(fdaData.getFDALogFilepath(analysisId), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(DataFDA.DiversityResults fdaResults, String method) {
        tblData = (TableView) tabNode.lookup("#tblMA_Stats");

        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // set data visualization menu
        if(methodCat){
            cm = new ContextMenu();
            item = new MenuItem("View Results Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYFDA, analysisId); });
            cm.getItems().add(item);
            subTabInfo.cmDataViz = cm;
        }
        
        // set table popup context menu and include export option
        cm = new ContextMenu();
        if(methodCat){
            addGeneDVItem(cm, tblData, TabGeneDataViz.Panels.TERMS.name());
            app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
            tblData.setContextMenu(cm);
            app.export.addCopyToClipboardHandler(tblData);
        }else{
            MenuItem item_drill = new MenuItem("Drill down data");
            cm.getItems().add(item_drill);
            item_drill.setOnAction((event) -> { 
                try {
                    drillDownData(tblData);
                } catch (IOException ex) {
                    Logger.getLogger(SubTabFDAResults.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
            tblData.setContextMenu(cm);
            app.export.addCopyToClipboardHandler(tblData);
        }
        
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        ObservableList<DataSelectionResults> data = FXCollections.observableArrayList();
        
        if(methodCat){
            // populate table columns list
            for(ColumnDefinition cd : lstColDefs) {
                // hide some columns initially
                cd.visible = !(cd.id.equals("GENEDESC"));
                lstTblCols.add(cd);
            }

            // configure table columns
            tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            addTableColumns(tblData, lstTblCols, false, false);
            ArrayList<AnnotationTypeSource> lstATSources = addSourceColumns(tblData, lstTblCols, fdaResults, method);
            CheckBox chk = addRowNumsSelCols(tblData);
            chk.selectedProperty().addListener((obs, oldData, newData) -> { 
                changeAllRows(newData? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
                tblData.requestFocus();
            });

            // get table data
            if(!lstATSources.isEmpty()) {
                int srccnt = lstATSources.size();
                for(String gene : fdaResults.hmGeneResults.keySet()) {
                    String[] strSources = new String[srccnt];
                    int idx = 0;
                    DataFDA.DiversityGeneResults dgr = fdaResults.hmGeneResults.get(gene);
                    for(AnnotationTypeSource atp : lstATSources) {
                        String str = "";
                        if(dgr.hmResults.containsKey(atp.type)) {
                            HashMap<String, Boolean> hmResults = dgr.hmResults.get(atp.type);
                            if(hmResults.containsKey(atp.source))
                                str = hmResults.get(atp.source)? "Varying" : "Not Varying";
                        }
                        strSources[idx++] = str;
                    }
                    DataSelectionResults dsr = new DataSelectionResults(project, false, gene, project.data.getGeneDescription(gene), strSources);
                    data.add(dsr);
                }
            }
            
            // set table data and select first row
            tblData.setItems(data);
            if(data.size() > 0)
                tblData.getSelectionModel().select(0);

            // handle special columns, once data is available
            for(DlgSelectRows.ColumnDefinition cd : lstTblCols) {
                // handle special cases - only need to do this once
                if(cd.id.startsWith("SOURCE")) {
                    String sourceIdx = cd.id.substring("SOURCE".length());
                    HashMap<String, Object> hm = new HashMap<>();
                    for(DataSelectionResults dr : data)
                        hm.put(dr.getSource(sourceIdx), null);
                    ArrayList<String> lstSelections = new ArrayList<>();
                    for(String val : hm.keySet())
                        lstSelections.add(val);
                    Collections.sort(lstSelections);
                    cd.lstSelections = (List) lstSelections;
                }
            }
        }else{
            //ID
            // populate table columns list
            for(ColumnDefinition cd : lstColDefsID) {
                // hide some columns initially
                //cd.visible = !(cd.id.equals("GENEDESC"));
                lstTblCols.add(cd);
                if(cd.id.equals("GENE"))
                    cd.colTitle = "Feature ID";
                if(cd.id.equals("GENEDESC"))
                    cd.colTitle = "Feature Description";
            }

            // configure table columns
            tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            addTableColumns(tblData, lstTblCols, false, false);
            ArrayList<AnnotationTypeSource> lstATSources = addSourceColumns(tblData, lstTblCols, fdaResults, method);
            CheckBox chk = addRowNumsSelCols(tblData);
            chk.selectedProperty().addListener((obs, oldData, newData) -> { 
                changeAllRows(newData? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
                tblData.requestFocus();
            });

            // get table data
            if(!lstATSources.isEmpty()) {
                int srccnt = lstATSources.size();
                for(String id : fdaResults.hmIDResults.keySet()) {
                    Integer[] intSources = new Integer[srccnt];
                    int idx = 0;
                    DataFDA.DiversityIDResults dgr = fdaResults.hmIDResults.get(id);
                    for(AnnotationTypeSource atp : lstATSources) {
                        int quant = 0;
                        if(dgr.hmResults.containsKey(atp.type)) {
                            HashMap<String, Integer> hmResults = dgr.hmResults.get(atp.type);
                            if(hmResults.containsKey(atp.source))
                                quant = hmResults.get(atp.source);
                        }
                        intSources[idx++] = quant;
                    }
                    
                    String s, f, i;
                    //UniprotKB/SwissProt case
                    if(id.split("/").length>3){
                        s = id.split("/")[0] + "/" + id.split("/")[1];
                        f = id.split("/")[2];
                        i = id.split("/")[3];
                    }else{
                        s = id.split("/")[0];
                        f = id.split("/")[1];
                        i = id.split("/")[2];
                    }
                    DataSelectionResults dsr = new DataSelectionResults(project, false, id, project.data.getFeatureDescription(s,f,i), intSources);
                    data.add(dsr);
                }
            }
            // get Fisher test and its FDR correction
            int totalV = 0, totalN = 0, idV = 0, idN = 0;
            for(DataSelectionResults dsr : data){
                totalV += dsr.intSources[0];
                totalN += dsr.intSources[1];
            }
            
            //ObservableList<DataSelectionResults> aux_data = data;
            //data.clear();
            ArrayList<Double> pValue = new ArrayList<Double>();
            
            for(DataSelectionResults dsr : data){
                idV = dsr.intSources[0];
                idN = dsr.intSources[1];
                int tV = totalV - idV;
                int tN = totalN - idN;
                
                FisherExact fTest = new FisherExact(idV + idN + tV + tN);
                double pVal = Math.round(fTest.getRightTailedP(idV, idN, tV, tN) * 10000.0) / 10000.0;
                dsr.pValue = pVal;
                pValue.add(pVal);
                //System.out.println(dsr.pValue);
                //data.add(dsr);
            }
            
            //AdjPValue
            BenjaminiHochbergFDR BHadjPValue = new BenjaminiHochbergFDR(pValue);
            BHadjPValue.calculate();
            
            double[] orderAdjPValue = BHadjPValue.getAdjustedPvalues();
            Integer[] indexes = BHadjPValue.getIndexes();
            
            for(int i = 0; i < orderAdjPValue.length; i++){
                data.get(indexes[i]).adjPValue = Math.round(orderAdjPValue[i] * 10000.0) / 10000.0;
            }
            
            // set table data and select first row
            tblData.setItems(data);
            if(data.size() > 0)
                tblData.getSelectionModel().select(0);

            // handle special columns, once data is available
            for(DlgSelectRows.ColumnDefinition cd : lstTblCols) {
                // handle special cases - only need to do this once
                if(cd.id.startsWith("SOURCE")) {
                    String sourceIdx = cd.id.substring("SOURCE".length());
                    HashMap<Integer, Object> hm = new HashMap<>();
                    for(DataSelectionResults dr : data)
                        hm.put(dr.getIntSource(sourceIdx), null);
                    ArrayList<Integer> lstSelections = new ArrayList<>();
                    for(Integer val : hm.keySet())
                        lstSelections.add(val);
                    Collections.sort(lstSelections);
                    cd.lstSelections = (List) lstSelections;
                }
                if(cd.id.startsWith("PVALUE")) {
                    HashMap<Double, Object> hm = new HashMap<>();
                    for(DataSelectionResults dr : data)
                        hm.put(dr.pValue, null);
                    ArrayList<Double> lstSelections = new ArrayList<>();
                    for(Double val : hm.keySet())
                        lstSelections.add(val);
                    Collections.sort(lstSelections);
                    cd.lstSelections = (List) lstSelections;
                }
            }
        }

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataApp.DataType.GENE, true, true, lstTblCols, data, DataSelectionResults.class.getName());
        setFocusNode(tblData, true);
        
        //export if ID
        if(!methodCat)
            saveFDAIDResultAnalysis(tblData, fdaData.getFDAResultsFilepath(analysisId));
    }
    protected ArrayList<AnnotationTypeSource> addSourceColumns(TableView tbl, ArrayList<ColumnDefinition> lstTblCols, DataFDA.DiversityResults fdaResults, String method) {
        ArrayList<AnnotationTypeSource> lstATSources = new ArrayList<>();
        
        int sourceIdx = 0;
        String[] types = { AnnotationType.TRANS.name(), AnnotationType.PROTEIN.name(), AnnotationType.GENOMIC.name() };
        String[] titles = { "Transcript Annotation", "Protein Annotation", "Genomic Annotation" };
        String[] groups = { "groupA", "groupB", "groupC" };
        ArrayList<String> lstSources = new ArrayList<>();
        ObservableList<TableColumn> cols = tbl.getColumns();
        boolean flag = false;
        int tidx = 0;
        int tidxmax = groups.length;
        for(String type : types) {
            if(fdaResults.hmSources.containsKey(type)) {
                lstSources.clear();
                lstSources.addAll(fdaResults.hmSources.get(type).keySet());
                if(method.equals(fdaParams.method.CATEGORY.name()))
                    Collections.sort(lstSources);
                int cnt = lstSources.size();
                int subcolWidth = cnt > 1? 80 : 150;
                TableColumn col = new TableColumn<>(titles[tidx]);
                col.setPrefWidth(cnt * subcolWidth);
                col.setEditable(false);
                col.getStyleClass().add(groups[tidx++]);
                if(method.equals(fdaParams.method.CATEGORY.name())){
                    for (String key : lstSources) {
                        String fields[] = key.split(":");
                        String source = fields[1];
                        lstATSources.add(new AnnotationTypeSource(type, key));
                        TableColumn subcol = new TableColumn<>(source);
                        subcol.setMinWidth(25);
                        subcol.setPrefWidth(subcolWidth);
                        subcol.setEditable(false);
                        subcol.setCellValueFactory(getSourceDataCallback(sourceIdx));
                        subcol.setStyle("-fx-alignment:CENTER;");
                        col.getColumns().add(subcol);
                        ColumnDefinition cd = new ColumnDefinition("SOURCE" + sourceIdx, col.getText(), subcol.getText(), RowSelection.ARG_PREFIX + "Source\t" + sourceIdx,
                                                                    100, " CENTER", col.getText() + ": " + subcol.getText(), DlgSelectRows.Params.CompType.LISTIDVALS);
                        cd.lstIdValuesSelections = DlgSelectRows.Params.lstIdValuesVarying;
                        cd.visible = false;
                        lstTblCols.add(cd);
                        sourceIdx++;
                    }
                }else{
                //ID
                    for (String key : lstSources) {
                        String source = key;
                        lstATSources.add(new AnnotationTypeSource(type, key));
                        TableColumn subcol = new TableColumn<>(source);
                        subcol.setMinWidth(25);
                        subcol.setPrefWidth(subcolWidth);
                        subcol.setEditable(false);
                        subcol.setCellValueFactory(getIntSourceDataCallback(sourceIdx));
                        subcol.setStyle("-fx-alignment:CENTER;");
                        col.getColumns().add(subcol);
                        ColumnDefinition cd = new ColumnDefinition("SOURCE" + sourceIdx, col.getText(), subcol.getText(), RowSelection.ARG_PREFIX + "Source\t" + sourceIdx,
                                                                    100, " CENTER", col.getText() + ": " + subcol.getText(), DlgSelectRows.Params.CompType.NUMINT);
                        cd.lstIdValuesSelections = DlgSelectRows.Params.lstIdValuesVarying;
                        cd.visible = false;
                        lstTblCols.add(cd);
                        sourceIdx++;
                    }
                }
                cols.add(col);
                flag = true;
            }
            if(tidx >= tidxmax)
                tidx = 0;
            else if(!flag)
                tidx++;
        }
        return lstATSources;
    }
    
    //Cat method
    private Callback getSourceDataCallback(int idx) {
        return (new Callback<TableColumn.CellDataFeatures<DataSelectionResults, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<DataSelectionResults, String> p) {
                return p.getValue().sources[idx];
            }
        });
    }
    
    //ID method
    private Callback getIntSourceDataCallback(int idx) {
        return (new Callback<TableColumn.CellDataFeatures<DataSelectionResults, String>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(TableColumn.CellDataFeatures<DataSelectionResults, String> p) {
                return p.getValue().intsources[idx].asObject();
            }
        });
    }

    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            if(fdaParams.method.name().equals(fdaParams.method.CATEGORY.name())){
                methodCat = true;
                DataFDA.DiversityResults data = fdaData.getFDAResults(fdaParams.method.name(), analysisId);
                totalFeatures = fdaParams.totalFeatures;
                showSubTab(data, fdaParams.method.name());
            }else{
                //use ID form
                methodCat = false;
                DataFDA.DiversityResults data = fdaData.getFDAResults(fdaParams.method.name(), analysisId);
                totalFeatures = fdaParams.totalFeatures;
                showSubTab(data, fdaParams.method.name());
            }
        }
        else {
            //String name = "FDA.R";
            //runScriptPath = app.data.getTmpScriptFileFromResource(name);
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        DataFDA.DiversityResults data = null;
        
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "Functional Diversity Analysis in Progress...");
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
            String logfilepath = fdaData.getFDALogFilepath(analysisId);
            java.lang.Throwable e = getException();
            if(e != null)
                appendLogLine("FDAnalysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("FDAnalysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("Funtional Diversity Analysis", "FDA failed - task aborted");
            Utils.removeFolderFilesContaining(Paths.get(fdaData.getFDAFolder()), "." + analysisId + ".");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                app.logInfo("FDA completed successfully.");
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data, fdaParams.method.name());
                
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateFDACombinedResults", "FDA");
                hm.put("featureId", analysisId);
                tabBase.processRequest(hm);
            }
            else {
                app.logInfo("FDA " + (taskAborted? "aborted by request." : "failed."));
                app.ctls.alertWarning("Feature Diversity Analysis", "FDA " + (taskAborted? "aborted by request." : "failed."));
                setRunAnalysisScreenTitle("Feature Diversity Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = fdaData.getFDALogFilepath(analysisId);
                    outputFirstLogLine("Functional Diversity Analysis thread running...", logfilepath);

                    // build a struct using params or just pass params to annot data func? 
                    appendLogLine("Building annotation features list....", logfilepath);
                    
                    HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
                    for(String s : fdaParams.hmFeatures.keySet()) {
                            hmFeatures.put(s, fdaParams.hmFeatures.get(s));
                    }
                    if(fdaData.genDiversityData(hmFeatures, taf, fdaData.getFDAParams(analysisId), fdaParams, analysisId))
                        data = fdaData.getFDAResults(fdaParams.method.name(), analysisId);
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Differential Isoform Usage Analysis", task);
            return task;
        }
    }
    @Override
    protected void abortServiceTask(TaskHandler.ServiceExt svc) {
        // there is no additional process to kill here - change if runscript is used
        // cancel svc/task does not seem to break the code out of the loop it's in
        // so we pass abort flag inside object to check it in loop function
        if(svc != null && svc.taskInfo != null) {
            String name = svc.taskInfo.name;
            app.logInfo("Aborting '" + name + "' by user request.");
            taf.abortFlag = true;
            svc.taskAborted = true;
        }
    }
    
    //
    // Static Functions
    //
    
    // Just add all possible sub tabs - no need to bother with experiment type, etc.
    // List is used to close all subtabs that start with given panel names
    // Used to make sure we close all associated subtabs when running the analysis - data will be deleted
    public static String getAssociatedDVSubTabs(String id) {
        String subTabs = TabProjectDataViz.Panels.SUMMARYFDA.toString() + id + ";";
        return subTabs;
    }

    //DrillDown Data ISO
    private void drillDownData(TableView tbl) throws IOException {
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            app.ctls.alertInformation("FDA Drill Down Data", "You have multiple rows highlighted.\nHighlight a single row with the feature you are interested in.");
        }
        else {
            DataSelectionResults rdata = (DataSelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            String id = rdata.getGene().trim();
            HashMap<String,ArrayList<String>> genes = getFDAGenes(id); 
            app.ctlr.drillDownFDAData(id, genes);
        }
    }
    
    public HashMap<String, ArrayList<String>> getFDAGenes(String id) throws IOException {
        HashMap<String, ArrayList<String>> hmData = new HashMap<>();
        ArrayList<String> varying = new ArrayList<String>();
        ArrayList<String> notVarying = new ArrayList<String>();
        boolean find = false;
        boolean last = false;
        try {
            if(Files.exists(Paths.get(fdaData.getFDAResultsGeneDetailsFilepath(analysisId)))) {
                    List<String> lines = Files.readAllLines(Paths.get(fdaData.getFDAResultsGeneDetailsFilepath(analysisId)));
                    // get cluster info 
                    String[] fields;
                    int length = 3; //gene and cluster
                    int lnum = 1; // 1st line not count
                    for(String line : lines) {
                        if(last==false && find ==true)
                            break;
                        if(!line.isEmpty() && lnum>1) {
                            fields = line.split("\t");
                            if(fields.length == length) {
                                if(fields[0].equals(id)){
                                    find = true;
                                    last = true;
                                    if(fields[2].equals("V"))
                                        varying.add(fields[1]);
                                    if(fields[2].equals("S"))
                                        notVarying.add(fields[1]);
                                }else{
                                    last = false;
                                }
                            }
                            else {
                                logger.logError("Invalid number of columns in line " + lnum + ", " + fields.length + ", in DPA cluster file.");
                                break;
                            }
                        }
                        lnum++;
                    }
                hmData.put("S", notVarying);
                hmData.put("V", varying);
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DPA info file: " + e.getMessage());
        }
        return hmData;
    }
    
    //
    //Data Classes
    //
    public static class AnnotationTypeSource {
        String type;
        String source;
        public AnnotationTypeSource(String type, String source) {
            this.type = type;
            this.source = source;
        }
    }
}
