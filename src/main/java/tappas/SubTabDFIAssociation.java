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
import tappas.DataDFI.DFISelectionResultsAssociation;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDFIAssociation extends SubTabBase {
    // class data
    TableView tblData;
    
    String analysisId = "";
    String analysisName = "";
    boolean hasData;
    String panel;
    DataDFI dfiData;
    //DlgDFIAnalysis.Params dfiParams = new DlgDFIAnalysis.Params();
    DlgDFIAnalysis.Params dfiParams;
    
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("SOURCE1",      "", "Source 1",        "Source1", 100, "", "Annotation source 1", CompType.LIST),
        new ColumnDefinition("FEATURE1",     "", "Feature 1",       "Feature1", 100, "", "Annotation feature 1", CompType.LIST),
        new ColumnDefinition("FEATUREID1",   "", "Feature ID 1",      "FeatureId1", 100, "", "Feature ID 1", CompType.TEXT),
        new ColumnDefinition("SOURCE2",      "", "Source 2",        "Source2", 100, "", "Annotation source 2", CompType.LIST),
        new ColumnDefinition("FEATURE2",     "", "Feature 2",       "Feature2", 100, "", "Annotation feature 2", CompType.LIST),
        new ColumnDefinition("FEATUREID2",  "", "Feature ID 2",      "FeatureId2", 100, "", "Feature ID 2", CompType.TEXT),
        //new ColumnDefinition("PVALUE",    "", "P-Value", "pValue", 90, "center", "P-Value", DlgSelectRows.Params.CompType.NUMDBL),
        //new ColumnDefinition("ADJPVALUE",    "", "AdjP-Value", "AdjPValue", 90, "center", "AdjP-Value", DlgSelectRows.Params.CompType.NUMDBL),
        new ColumnDefinition("DSGENES",     "", "Genes with Both DFI Feature IDs",  "GeneCount", 190, "TOP-RIGHT", "Count of genes with both DFI feature IDs", CompType.NUMINT),
        new ColumnDefinition("SAMEFAVCNT",  "", "Co-Inclusion",     "SameFavored", 100, "TOP-RIGHT", "Same condition favored count", CompType.NUMINT),
        new ColumnDefinition("DIFFFAVCNT",  "", "Mutual exclusion", "OppositeFavored", 110, "TOP-RIGHT", "Opposite condition favored count", CompType.NUMINT)
    );
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabDFIAssociation(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            // DFI data is required
            dfiData = new DataDFI(project);
            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.FIASSOCIATION.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.FIASSOCIATION.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");

                // DFI parameter file is required
                result = Files.exists(Paths.get(dfiData.getDFIParamsFilepath(analysisId)));
                if(result) {
                    //params = dfiData.getDFIParams();
                    dfiParams = DlgDFIAnalysis.Params.load(dfiData.getDFIParamsFilepath(analysisId), project);
                    analysisName = (String) dfiParams.name;
                    String shortName = getShortName(dfiParams.name);
                    subTabInfo.title = "DFI Association: " + shortName;
                    subTabInfo.tooltip = "Differential Feature Inclusion Association results for '" + dfiParams.name.trim() + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    
                    hasData = dfiData.hasDFIData(analysisId);
                }
            else
                app.ctls.alertInformation("DFI Association", "Missing DFI parameter file");

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
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(false, "", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_DFI_FeatureAssociations_"+ analysisName +".tsv";
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
        }
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
                TableView<DFISelectionResultsAssociation> tblView = (TableView<DFISelectionResultsAssociation>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DFISelectionResultsAssociation> items = (ObservableList<DFISelectionResultsAssociation>) info.data;
                if(items != null) {
                    ObservableList<DFISelectionResultsAssociation> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DFISelectionResultsAssociation data : items) {
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
                        for(DFISelectionResultsAssociation data : items) {
                            if(data.getSource1().toLowerCase().contains(txt) || data.getFeature1().toLowerCase().contains(txt) || data.getFeatureId1().toLowerCase().contains(txt) ||
                                    data.getSource2().toLowerCase().contains(txt) || data.getFeature2().toLowerCase().contains(txt) || data.getFeatureId2().toLowerCase().contains(txt)) {
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

    
    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<DFISelectionResultsAssociation> data, ArrayList<DataDFI.DFIResultsData> lstResultsData) {
        tblData = (TableView) tabNode.lookup("#tblDA_Stats");

        // set table popup context menu and include gene data viz and export options
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Drill down data");
        item.setOnAction((event) -> { drillDownData(tblData, lstResultsData); });
        cm.getItems().add(item);
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);

        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // hide some columns initially
            cd.visible = !(cd.id.equals("SOURCE1") || cd.id.equals("SOURCE2") || cd.id.equals("FEATURE1") || cd.id.equals("FEATURE2"));
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

        //DFI results
        ObservableList<DFISelectionResults> dataDFI = dfiData.getDFISelectionResults(dfiParams.sigValue, true, analysisId);
        HashMap<String,HashMap<String, Boolean>> hmDFI = new HashMap<>();
        //list feature1 and feature2
        for(DFISelectionResultsAssociation dsr : data){
            if(hmDFI.containsKey(dsr.featureId1.getValue()) && hmDFI.containsKey(dsr.featureId2.getValue())){
                continue;
            }else if(!hmDFI.containsKey(dsr.featureId1.getValue()) && !hmDFI.containsKey(dsr.featureId2.getValue())){
                HashMap<String, Boolean> hmGenes1 = new HashMap<>();
                HashMap<String, Boolean> hmGenes2 = new HashMap<>();
                for(DFISelectionResults aux : dataDFI){
                    if(!(hmGenes1.containsKey(aux.getGene()) && hmGenes1.get(aux.getGene()))){
                        if(aux.featureId.getValue().equals(dsr.featureId1.getValue()))
                            hmGenes1.put(aux.getGene(), aux.getDS().equals("DFI"));
                    }
                    if(hmGenes2.containsKey(aux.getGene()) && hmGenes2.get(aux.getGene()))
                        continue;
                    if(aux.featureId.getValue().equals(dsr.featureId2.getValue()))
                        hmGenes2.put(aux.getGene(), aux.getDS().equals("DFI"));
                }
                hmDFI.put(dsr.featureId1.getValue(), hmGenes1);
                hmDFI.put(dsr.featureId2.getValue(), hmGenes2);
                
            }else if(!hmDFI.containsKey(dsr.featureId1.getValue())){
                HashMap<String, Boolean> hmGenes = new HashMap<>();
                for(DFISelectionResults aux : dataDFI){
                    if(hmGenes.containsKey(aux.getGene()) && hmGenes.get(aux.getGene()))
                        continue;
                    if(aux.featureId.getValue().equals(dsr.featureId1.getValue()))
                        hmGenes.put(aux.getGene(), aux.getDS().equals("DFI"));
                }
                hmDFI.put(dsr.featureId1.getValue(), hmGenes);
                
            }else if(!hmDFI.containsKey(dsr.featureId2.getValue())){
                HashMap<String, Boolean> hmGenes = new HashMap<>();
                for(DFISelectionResults aux : dataDFI){
                    if(hmGenes.containsKey(aux.getGene()) && hmGenes.get(aux.getGene()))
                        continue;
                    if(aux.featureId.getValue().equals(dsr.featureId2.getValue()))
                        hmGenes.put(aux.getGene(), aux.getDS().equals("DFI"));
                }
                hmDFI.put(dsr.featureId2.getValue(), hmGenes);
            }
        }

        // get Fisher test and its FDR correction
//        int totalV = 0, totalN = 0, idV = 0, idN = 0;
//        for(DFISelectionResultsAssociation dsr : data){
//            totalV += dsr.sameFavored.getValue();
//            totalN += dsr.oppositeFavored.getValue();
//        }

        int totalV = 0, totalN = 0, idVV = 0, idVN = 0, idNV = 0, idNN = 0;
        ArrayList<String> ft1V = new ArrayList<>();
        ArrayList<String> ft1N = new ArrayList<>();
        ArrayList<String> ft2V = new ArrayList<>();
        ArrayList<String> ft2N = new ArrayList<>();
        ArrayList<Double> pValue = new ArrayList<Double>();
        
        //create each list with Diverse, notDiverse  for each feature
        /*for(DFISelectionResultsAssociation dsr : data){
            for(String gene : hmDFI.get(dsr.featureId1.getValue()).keySet()){
                if(hmDFI.get(dsr.featureId1.getValue()).get(gene)){
                    totalV += 1;
                    ft1V.add(gene);
                }else{
                    totalN += 1;
                    ft1N.add(gene);
                }
            }
            for(String gene : hmDFI.get(dsr.featureId2.getValue()).keySet()){
                if(hmDFI.get(dsr.featureId2.getValue()).get(gene)){
                    totalV += 1;
                    ft2V.add(gene);
                }else{
                    totalN += 1;
                    ft2N.add(gene);
                }
            }
            
            //calculate PVal
            for(String aux : ft1V){
                if(ft2V.contains(aux))
                    idVV += 1;
                if(ft2N.contains(aux))
                    idVN += 1;
            }
            
            for(String aux : ft1N){
                if(ft2V.contains(aux))
                    idNV += 1;
                if(ft2N.contains(aux))
                    idNN += 1;
            }
            
            for(String aux : ft2V){
                if(ft1V.contains(aux))
                    continue; //already counted
                else
                    idVN += 1;
            }
            
            for(String aux : ft2N){
                if(ft1N.contains(aux))
                    continue; //already counted
                else
                    idNN += 1;
            }

            FisherExact fTest = new FisherExact(idVV + idVN + idNV + idNN);
            double pVal = Math.round(fTest.getRightTailedP(idVV, idVN, idNV, idNN) * 10000.0) / 10000.0;
            dsr.pValue = pVal;
            pValue.add(pVal);
        }
        
        //calculate Adj
        BenjaminiHochbergFDR BHadjPValue = new BenjaminiHochbergFDR(pValue);
        BHadjPValue.calculate();

        double[] orderAdjPValue = BHadjPValue.getAdjustedPvalues();
        Integer[] indexes = BHadjPValue.getIndexes();

        for(int i = 0; i < orderAdjPValue.length; i++){
            data.get(indexes[i]).adjPValue = Math.round(orderAdjPValue[i] * 10000.0) / 10000.0;
        }*/
        
        //ObservableList<DataSelectionResults> aux_data = data;
        //data.clear();
//        ArrayList<Double> pValue = new ArrayList<Double>();
//
//        for(DFISelectionResultsAssociation dsr : data){
//            idV = dsr.sameFavored.getValue();
//            idN = dsr.oppositeFavored.getValue();
//            int tV = totalV - idV;
//            int tN = totalN - idN;
//
//            FisherExact fTest = new FisherExact(idV + idN + tV + tN);
//            double pVal = Math.round(fTest.getRightTailedP(idV, idN, tV, tN) * 10000.0) / 10000.0;
//            dsr.pValue = pVal;
//            pValue.add(pVal);
//            //System.out.println(dsr.pValue);
//            //data.add(dsr);
//        }

        //AdjPValue
//        BenjaminiHochbergFDR BHadjPValue = new BenjaminiHochbergFDR(pValue);
//        BHadjPValue.calculate();
//
//        double[] orderAdjPValue = BHadjPValue.getAdjustedPvalues();
//        Integer[] indexes = BHadjPValue.getIndexes();
//
//        for(int i = 0; i < orderAdjPValue.length; i++){
//            data.get(indexes[i]).adjPValue = Math.round(orderAdjPValue[i] * 10000.0) / 10000.0;
//        }
        
        // set table data and select first row
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            if(cd.id.equals("SOURCE1")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDFI.DFISelectionResultsAssociation dr : data)
                    hm.put(dr.getSource1(), null);
                ArrayList<String> lstSelections = new ArrayList<>();
                for(String source : hm.keySet())
                    lstSelections.add(source);
                Collections.sort(lstSelections);
                cd.lstSelections = (List)lstSelections;
            }
            else if(cd.id.equals("FEATURE1")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDFI.DFISelectionResultsAssociation dr : data)
                    hm.put(dr.getFeature1(), null);
                ArrayList<String> lstSelections = new ArrayList<>();
                for(String feature : hm.keySet())
                    lstSelections.add(feature);
                Collections.sort(lstSelections);
                cd.lstSelections = (List)lstSelections;
            }
            else if(cd.id.equals("SOURCE2")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDFI.DFISelectionResultsAssociation dr : data)
                    hm.put(dr.getSource2(), null);
                ArrayList<String> lstSelections = new ArrayList<>();
                for(String source : hm.keySet())
                    lstSelections.add(source);
                Collections.sort(lstSelections);
                cd.lstSelections = (List)lstSelections;
            }
            else if(cd.id.equals("FEATURE2")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDFI.DFISelectionResultsAssociation dr : data)
                    hm.put(dr.getFeature2(), null);
                ArrayList<String> lstSelections = new ArrayList<>();
                for(String feature : hm.keySet())
                    lstSelections.add(feature);
                Collections.sort(lstSelections);
                cd.lstSelections = (List)lstSelections;
            }
            // Note: there could be hundreds or even thousands of feature IDs so not to do a list
        }

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataType.GENE, true, false, lstTblCols, data, DFISelectionResultsAssociation.class.getName());
        setFocusNode(tblData, true);
    }
    private void drillDownData(TableView tbl, ArrayList<DataDFI.DFIResultsData> lstResultsData) {
        // get highlighted row's data and show gene data visualization
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            String msg = "You have multiple rows highlighted.\nHighlight a single row with the feature ids\nfor which you want to see drill down data.";
            app.ctls.alertInformation("Show Gene Feature Id Association Data", msg);
        }
        else {
            DFISelectionResultsAssociation rdata = (DFISelectionResultsAssociation) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            app.ctlr.drillDownDFIAssociation(lstResultsData, rdata); 
        }
    }
        
    //
    // Data Load
    //
    //@Override
    //protected void runDataLoadThread() {
    //    ArrayList<DataDFI.DFIResultsData> lstResultsData = dfiData.getDFIResultsData(dfiParams.sigValue, analysisId);
    //    ObservableList<DFISelectionResultsAssociation> data = dfiData.getDFISelectionResultsAssociation(dfiParams.sigValue, analysisId);
    //    FXCollections.sort(data);
    //    showSubTab(data, lstResultsData);
    //}


    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            ArrayList<DataDFI.DFIResultsData> lstResultsData = dfiData.getDFIResultsData(dfiParams.sigValue, analysisId);
            ObservableList<DFISelectionResultsAssociation> data = dfiData.getDFISelectionResultsAssociation(dfiParams.sigValue, analysisId);
            FXCollections.sort(data);
            showSubTab(data, lstResultsData);
        }
        else {
            service = new SubTabDFIAssociation.DataService();
            service.start();
        }
    }
    private class DataService extends TaskHandler.ServiceExt {
        ObservableList<DFISelectionResultsAssociation> data = null;
        ArrayList<DataDFI.DFIResultsData> lstResultsData = null;

        @Override
        protected void onRunning() {
            showProgress();
        }
        @Override
        protected void onStopped() {
            hideProgress();
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("DFI Association analysis failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DFI Association analysis - task aborted.");
            app.ctls.alertWarning("Differential Feature Inclusion Association Analysis", "DFI Association Analysis failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                pageDataReady = true;
                showSubTab(data, lstResultsData);
            }
            else {
                app.ctls.alertWarning("DFI Association Analysis", "Unable to load DFI data.");
                populated = true;
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    ObservableList<DFISelectionResultsAssociation> results = dfiData.getDFISelectionResultsAssociation(dfiParams.sigValue, analysisId);
                    ArrayList<DataDFI.DFIResultsData> lstResults = dfiData.getDFIResultsData(dfiParams.sigValue, analysisId);
                    if(!results.isEmpty() && !lstResults.isEmpty()) {
                        data = results;
                        lstResultsData = lstResults;
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("DFI Association Analysis", task);
            return task;
        }
    }
}
