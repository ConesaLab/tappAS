/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFEAClusters extends SubTabBase {
    TableView tblClusters, tblNodes;

    boolean hasData = false;
    DataFEA feaData;
    HashMap<String, String> feaParams = new HashMap<>();
    DlgFEAnalysis.Params params;
    String analysisId = "";
    String feature = "";
    String clustersInfo = "";
    String clustersHTML = "";
    Cluster.ClusterData cd = null;
    HashMap<String, String> clusterParams = null;
    ArrayList<ArrayList<String>> lstClusterTerms = new ArrayList<>();
    ObservableList<ClustersTableData> clustersData = FXCollections.observableArrayList();
    ObservableList<NodesTableData> nodesData = FXCollections.observableArrayList();
    ObservableList<LinksTableData> linksData = FXCollections.observableArrayList();
    ArrayList<Integer> lstClusters = new ArrayList<>();
    
    public SubTabFEAClusters(Project project) {
        super(project);
    }

    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnRun = true;
        subTabInfo.btnDataViz = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            feaData = new DataFEA(project);
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.CLUSTERSFEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.CLUSTERSFEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");
                
                // FEA parameter file is required
                result = Files.exists(Paths.get(feaData.getFEAParamsFilepath(analysisId)));
                if(result) {
                    feaParams = feaData.getFEAParams(analysisId);
                    params = new DlgFEAnalysis.Params(feaParams);
                    String shortName = getShortName(params.name);
                    subTabInfo.title = "FEA Clusters: '" + shortName + "'";
                    subTabInfo.tooltip = "FEA enriched features clusters for '" + params.name + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    feature = params.getFeaturesName();

                    // FEA data is required
                    result = feaData.hasFEAData(params.dataType.name(), analysisId);
                    if(result) {
                        // clusters parameter file is required
                        result = Files.exists(Paths.get(feaData.getFEAClusterParamsFilepath(analysisId)));
                        if(result) {
                            clusterParams = feaData.getFEAClusterParams(analysisId);
                            hasData = feaData.hasFEAClusterData(analysisId);
                        }
                        else
                            app.ctls.alertInformation("FEA Cluster Results", "Missing FEA clusters parameter file");
                    }
                    else
                        app.ctls.alertInformation("FEA Cluster Results", "Missing FEA data");
                }
                else
                    app.ctls.alertInformation("FEA Cluster Results", "Missing FEA parameter file");
            }
            else
                result = false;
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonRun() {
        app.ctlr.runFEAClusterAnalysis(analysisId, clusterParams);
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        if(obj != null) {
            if(obj.equals(tblClusters)) {
                if(txt.isEmpty() && !hide)
                    setTableSearchActive(tblClusters, false);
                else
                    setTableSearchActive(tblClusters, true);
                TableView<ClustersTableData> tblView = (TableView<ClustersTableData>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<ClustersTableData> items = (ObservableList<ClustersTableData>) info.data;
                if(items != null) {
                    ObservableList<ClustersTableData> matchItems;
                    if(txt.isEmpty()) {
                        // restore original dataset
                        matchItems = items;
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(ClustersTableData data : items) {
                            if(data.getID().toString().contains(txt) || data.getName().toLowerCase().contains(txt))
                                matchItems.add(data);
                        }
                    }
                    tblView.setItems(matchItems);
                }
            }
            else if(obj.equals(tblNodes)) {
                if(txt.isEmpty() && !hide)
                    setTableSearchActive(tblNodes, false);
                else
                    setTableSearchActive(tblNodes, true);
                TableView<NodesTableData> tblView = (TableView<NodesTableData>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<NodesTableData> items = (ObservableList<NodesTableData>) info.data;
                if(items != null) {
                    ObservableList<NodesTableData> matchItems;
                    if(txt.isEmpty()) {
                        // restore original dataset
                        matchItems = items;
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(NodesTableData data : items) {
                            if(data.getID().toLowerCase().contains(txt) || data.getName().toLowerCase().contains(txt))
                                matchItems.add(data);
                        }
                    }
                    tblView.setItems(matchItems);
                }
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
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        tblClusters = (TableView) tabNode.lookup("#tblClusters");
        tblNodes = (TableView) tabNode.lookup("#tblNodes");

        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // create data visualization context menu
        cm = new ContextMenu();
        item = new MenuItem("View Clusters Graph");
        item.setOnAction((event) -> { 
            HashMap<String, Object> graphArgs = new HashMap<>();
            graphArgs.put("html", clustersHTML);
            graphArgs.put("lstClusters", lstClusters);
            app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.CLUSTERSFEA, analysisId, graphArgs);
        });
        cm.getItems().add(item);
        AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
        if(feature.contains(defs.srcGO)) {
            item = new MenuItem("View Clusters GO Terms");
            item.setOnAction((event) -> {
                HashMap<String, Object> goArgs = new HashMap<>();
                goArgs.put("lstClusterTerms", lstClusterTerms);
                app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.CLUSTERSGOFEA, analysisId, goArgs);
            });
            cm.getItems().add(item);
        }
        subTabInfo.cmDataViz = cm;

        // get FEA data type
        String itemType;
        switch(params.dataType) {
            case TRANS:
                itemType = "Transcripts";
                break;
            case PROTEIN:
                itemType = "Proteins";
                break;
            default:
                itemType = "Genes";
                break;
        }

        // setup nodes table
        tblNodes.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<TableColumn> cols = tblNodes.getColumns();
        int colIdx = 0;
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("ClusterID"));
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("ID"));
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("Name"));
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("Category"));
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("PValue"));
        cols.get(colIdx).setCellValueFactory(new PropertyValueFactory<>("TestGenes"));
        cols.get(colIdx++).setText("Test " + itemType);
        cols.get(colIdx).setCellValueFactory(new PropertyValueFactory<>("NonTestGenes"));
        cols.get(colIdx++).setText("Non-Test " + itemType);
        cols.get(colIdx).setCellValueFactory(new PropertyValueFactory<>("TotalGenes"));
        cols.get(colIdx++).setText("Total " + itemType);
        addRowNumbersCol(tblNodes);

        cm = new ContextMenu();
        item = new MenuItem("Drill down data");
        item.setOnAction((event) -> { drillDownNodeData(); });
        cm.getItems().add(item);
        app.export.setupSimpleTableExport(tblNodes, cm, true, "tappAS_FEA_ClusterNodes.tsv");
        tblNodes.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblNodes);
        setupTableSearch(tblNodes);
        subTabInfo.lstSearchTables.add(tblNodes);

        tblClusters.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cols = tblClusters.getColumns();
        colIdx = 0;
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("ID"));
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("Name"));
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("Nodes"));
        cols.get(colIdx++).setCellValueFactory(new PropertyValueFactory<>("Percent"));
        tblClusters.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                ObservableList<ClustersTableData> lstItems = tblClusters.getSelectionModel().getSelectedItems();
                ObservableList<NodesTableData> data = FXCollections.observableArrayList();
                HashMap<Integer, Object> hmClusters = new HashMap<>();
                for(ClustersTableData cd : lstItems)
                    hmClusters.put(cd.id.get(), null);
                for(NodesTableData nd : nodesData) {
                    if(hmClusters.containsKey(nd.getClusterID()))
                        data.add(nd);
                }
                FXCollections.sort(data);
                tblNodes.setItems(data);
                if(data.size() > 0)
                    tblNodes.getSelectionModel().select(0);
                tblNodes.setUserData(new TabBase.SearchInfo(tblNodes.getItems(), "", false));
                setTableSearchActive(tblNodes, false);
            }
            else {
                ObservableList<NodesTableData> data = FXCollections.observableArrayList();
                tblNodes.setItems(data);
                tblNodes.setUserData(new TabBase.SearchInfo(tblNodes.getItems(), "", false));
                setTableSearchActive(tblNodes, false);
            }
        });
        addRowNumbersCol(tblClusters);
        cm = new ContextMenu();
        item = new MenuItem("Drill down data");
        item.setOnAction((event) -> { drillDownClusterData(); });
        cm.getItems().add(item);
        app.export.setupSimpleTableExport(tblClusters, cm, true, "tappAS_FEA_Clusters.tsv");
        tblClusters.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblClusters);

        FXCollections.sort(clustersData);
        tblClusters.setItems(clustersData);
        if(clustersData.size() > 0)
            tblClusters.getSelectionModel().select(0);
        tblClusters.setUserData(new TabBase.SearchInfo(tblClusters.getItems(), "", false));
        setupTableSearch(tblClusters);
        subTabInfo.lstSearchTables.add(tblClusters);

        // manage focus on both tables
        tblClusters.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblClusters;
        });
        tblNodes.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblNodes;
        });
        
        setupExportMenu();
        setFocusNode(tblClusters, true);
    }
    private void viewLog() {
        String logdata = app.getLogFromFile(feaData.getFEAClusterLogFilepath(analysisId), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export clusters table...");
        item.setOnAction((event) -> {
            app.export.exportTableData(tblClusters, true, "tappAS_FEA_Clusters.tsv");
        });
        cm.getItems().add(item);
        item = new MenuItem("Export cluster nodes table...");
        item.setOnAction((event) -> { 
            app.export.exportTableData(tblNodes, true, "tappAS_FEA_ClusterNodes.tsv");
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void drillDownClusterData() {
        // get highlighted row's data and show drilldown data
        ObservableList<Integer> lstIdxs = tblClusters.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            String msg = "You have multiple rows highlighted.\nHighlight a single row\nwith the cluster you want to drill down.";
            app.ctls.alertInformation("Drill Down Data", msg);
        }
        else {
            int idx = tblClusters.getSelectionModel().getSelectedIndex();
            if(idx != -1) {
                String cluster = ((ClustersTableData)tblClusters.getItems().get(lstIdxs.get(0))).getName();
                int clusterId = ((ClustersTableData)tblClusters.getItems().get(lstIdxs.get(0))).getID();
                app.ctlr.drillDownFEACluster(params, analysisId, cluster, clusterId, nodesData);
            }
        }
    }
    private void drillDownNodeData() {
        // get highlighted row's data and show drilldown data
        ObservableList<Integer> lstIdxs = tblNodes.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            String msg = "You have multiple rows highlighted.\nHighlight a single row\nwith the cluster node you want to drill down.";
            app.ctls.alertInformation("Drill Down Data", msg);
        }
        else {
            int idx = tblClusters.getSelectionModel().getSelectedIndex();
            if(idx != -1) {
                String cluster = ((ClustersTableData)tblClusters.getItems().get(idx)).getName();
                String node = ((NodesTableData)tblNodes.getItems().get(lstIdxs.get(0))).getID();
                app.ctlr.drillDownFEAClusterNode(params, analysisId, cluster, node);
            }
        }
    }
    private boolean writeClusterResultsFiles(ObservableList<ClustersTableData> clusters,
                                      ObservableList<NodesTableData> nodes,
                                      ObservableList<LinksTableData> links, String html) {
        boolean result = false;
        Writer writer = null;
        String filepath = feaData.getFEAResultsClusterFilepath("clusters", analysisId);
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
            for(ClustersTableData data : clusters)
                writer.write(data.toTSV() + "\n");
            result = true;
        }
        catch (Exception e) {
            result = false;
            app.logError("Unable to write FEA clusters file: " + e.getMessage());
        } finally {
           try { if(writer != null) writer.close(); } catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        
        if(result)  {
            // write html contents out to file
            writer = null;
            filepath = feaData.getFEAResultsClusterFilepath("nodes", analysisId);
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                for(NodesTableData data : nodes)
                    writer.write(data.toTSV() + "\n");
            } catch(Exception e) {
                result = false;
                app.logError("Unable to write FEA cluster nodes file: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) {System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }

        if(result)  {
            writer = null;
            filepath = feaData.getFEAResultsClusterFilepath("links", analysisId);
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                for(LinksTableData data : links)
                    writer.write(data.toTSV() + "\n");
            } catch(Exception e) {
                result = false;
                app.logError("Unable to write FEA cluster links file: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }

        if(result)  {
            writer = null;
            filepath = feaData.getFEAResultsClusterFilepath("graph", analysisId);
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                writer.write(html);
            } catch(Exception e) {
                result = false;
                app.logError("Unable to write FEA clusters graph html file: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }

        if(result)  {
            // write analysis completed successfully file
            writer = null;
            filepath = feaData.getFEAClusterDoneFilepath(analysisId);
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
                writer.write("end");
            } catch (IOException e) {
                result = false;
                app.logError("Unable to write FEA cluster analysis completed file: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        return result;
    }
    private boolean readClusterResultsFiles() {
        boolean result = false;
        clustersData.clear();
        nodesData.clear();
        linksData.clear();
        lstClusters.clear();
        lstClusterTerms.clear();
        clustersHTML = "";
        String filepath = feaData.getFEAResultsClusterFilepath("clusters", analysisId);
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines) {
                    if(line.charAt(0) != '#')
                        clustersData.add(new ClustersTableData(line));
                }
                result = true;
            }
        }
        catch (Exception e) {
            result = false;
            app.logError("Unable to read FEA clusters file: " + e.getMessage());
        }
        
        if(result)  {
            filepath = feaData.getFEAResultsClusterFilepath("nodes", analysisId);
            try {
                if(Files.exists(Paths.get(filepath))) {
                    List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                    for(String line : lines) {
                        if(line.charAt(0) != '#')
                            nodesData.add(new NodesTableData(line));
                    }
                    // generate list of features - used by GO Terms graph
                    HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
                    int idmax = 0;
                    for(ClustersTableData ctd : clustersData) {
                        if(ctd.id.get() > idmax)
                            idmax = ctd.id.get();
                    }
                    if(idmax > 0 && idmax < 10000) {
                        for(int i = 0; i < idmax; i++)
                            lstClusterTerms.add(new ArrayList<>());
                        for(NodesTableData data : nodesData) {
                            lstClusterTerms.get(data.getClusterID() - 1).add(data.getID());
                        }
                        for(int i = 0; i < idmax; i++)
                            Collections.sort(lstClusterTerms.get(i));
                        result = true;
                    }
                }
            } catch(Exception e) {
                result = false;
                app.logError("Unable to read FEA cluster nodes file: " + e.getMessage());
            }
        }

        if(result)  {
            filepath = feaData.getFEAResultsClusterFilepath("links", analysisId);
            try {
                if(Files.exists(Paths.get(filepath))) {
                    List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                    for(String line : lines) {
                        if(line.charAt(0) != '#')
                            linksData.add(new LinksTableData(line));
                    }
                    result = true;
                }
            } catch(Exception e) {
                result = false;
                app.logError("Unable to read FEA cluster links file: " + e.getMessage());
            }
        }

        if(result)  {
            filepath = feaData.getFEAResultsClusterFilepath("graph", analysisId);
            try {
                if(Files.exists(Paths.get(filepath))) {
                    clustersHTML = new String(Files.readAllBytes(Paths.get(filepath)));
                    result = true;
                }
            } catch(Exception e) {
                result = false;
                app.logError("Unable to read FEA clusters graph html file: " + e.getMessage());
            }
        }
        
        // setup list of clusters with members count
        HashMap<String, Integer> hmClusters = new HashMap<>();
        for(NodesTableData node :  nodesData) {
            String cid = node.getClusterID().toString();
            if(!hmClusters.containsKey(cid))
                hmClusters.put(cid, 1);
            else
                hmClusters.put(cid, hmClusters.get(cid) + 1);
        }
        for(int cnt : hmClusters.values())
            lstClusters.add(cnt);
        return result;
    }
    private String buildHTML(String memberType, ArrayList<GraphLink> lstLinks, ArrayList<DataFEA.FEAStatsData> lstStats, 
                   ArrayList<Cluster.ClusterDef> lstClusters, ArrayList<ArrayList<Integer>> lstClusterNums, boolean clustersOnly, String logfilepath) {
        String htmlClusters = app.data.getFileContentFromResource("/tappas/scripts/GOClusters.html");
        String htmlArrays = getJSArrays(lstLinks, lstStats, lstClusters, lstClusterNums, clustersOnly, logfilepath);
        String filepath = app.data.getAppDataFolder();
        String url;
        if(Utils.isWindowsOS()) {
            // need an additional slash when using drive letters
            url = "/" + filepath.replaceAll("\\\\", "/");
            logger.logInfo("fp: " + url);
        }
        else
            url = filepath;
        String htmlBase = "<base href=\"file://" + url + "/\" />";
        htmlClusters = htmlClusters.replace("<<filebase>>", htmlBase);
        htmlClusters = htmlClusters.replace("<<memberType>>", memberType);
        htmlClusters = htmlClusters.replace("<<d3arrays>>", htmlArrays);
        htmlClusters = htmlClusters.replace("<<d3clusters>>", htmlClusters);
        return htmlClusters;
    }
    private String getJSArrays(ArrayList<GraphLink> lstLinks, ArrayList<DataFEA.FEAStatsData> lstStats, 
                   ArrayList<Cluster.ClusterDef> lstClusters, ArrayList<ArrayList<Integer>> lstClusterNums, boolean clustersOnly, String logfilepath) {
        HashMap<Integer, Integer> hmNodes = new HashMap<>();
        String strGroups = "var dgroups = [0";
        for(Cluster.ClusterDef def : lstClusters)
            strGroups += "," + def.members.size();
        strGroups += "];\n";
        String strGroupNames = "var groupNames = [\"\"";
        String strClusterLabels = "var dclusters = [\n";
        int wLabel = 200;
        int hLabel = 20;
        int x = 20;
        int y = 10;
        for(Cluster.ClusterDef def : lstClusters) {
            strGroupNames += ", \"Cluster " + def.num + "\"";
            strClusterLabels += "{id: " + def.num + ", idx: " + def.num + ", width: " + wLabel + ", height: " + hLabel + ", x: " + x + ", y: " + y + ", \"cx\": " + x + ", \"cy\": " + y + ", text: \"Cluster " + def.num + "\"},\n";
            y += 20;
        }
        strGroupNames += "];\n";
        strClusterLabels += "];\n";
        String strNodes = "var dnodes = [\n";
        String strEdges = "var dlinks = [\n";
        
        int idx = 0;
        int cluster;
        String clusters;
        GO go = new GO();
        appendLogLine("Creating Node's information...", logfilepath);
        for(GraphLink gl : lstLinks) {
            if(go.getGOTermInfo(lstStats.get(gl.dst).getFeature()) == null) //we update go.obo and maybe the GO change and they are not any more
                continue;
            // add namespace: later, after term: - will need to instantiate GO class or add call to GOTerms
            // GO go = new GO();  go.getGOTermInfo(term).namespace;
            if(!hmNodes.containsKey(gl.src)) {
                cluster = gl.srcClusters.isEmpty()? 0 : gl.srcClusters.size() > 1? getMainCluster(gl.src, lstLinks, lstClusterNums) : gl.srcClusters.get(0);
                clusters = gl.srcClusters.size() > 1? ", groups: \"" + gl.srcClusters.toString().replaceAll("[\\[ \\]]", "") + "\"" : "";
                strNodes += "{id: " + idx + ", idx: " + gl.src + ", term: \"" + lstStats.get(gl.src).getFeature() + "\", namespace: \"" + go.getGOTermInfo(lstStats.get(gl.src).getFeature()).namespace + "\", label: \"" + GO.getShortName(lstStats.get(gl.src).getDescription(), 27) + "\", name: \"" + lstStats.get(gl.src).getDescription() + 
                        "\", grpcnt: " + gl.srcClusters.size() + ", group: " + cluster + clusters + ", genes: " + lstStats.get(gl.src).getTestItemsCnt() + ", isos: " + lstStats.get(gl.src).getTestItemsCnt() + "},\n";
                hmNodes.put(gl.src, idx++);
            }
            if(!hmNodes.containsKey(gl.dst)) {
                cluster = gl.dstClusters.isEmpty()? 0 : gl.dstClusters.size() > 1? getMainCluster(gl.dst, lstLinks, lstClusterNums) : gl.dstClusters.get(0);
                clusters = gl.dstClusters.size() > 1? ", groups: \"" + gl.dstClusters.toString().replaceAll("[\\[ \\]]", "") + "\"" : "";
                strNodes += "{id: " + idx + ", idx: " + gl.dst + ", term: \"" + lstStats.get(gl.dst).getFeature() + "\", namespace: \"" + go.getGOTermInfo(lstStats.get(gl.dst).getFeature()).namespace +  "\", label: \"" + GO.getShortName(lstStats.get(gl.dst).getDescription(), 27) + "\", name: \"" + lstStats.get(gl.dst).getDescription() +
                        "\", grpcnt: " + gl.dstClusters.size() + ", group: " + cluster + clusters + ", genes: " + lstStats.get(gl.dst).getTestItemsCnt() + ", isos: " + lstStats.get(gl.src).getTestItemsCnt() + "},\n";
                hmNodes.put(gl.dst, idx++);
            }
            strEdges += "{source: " + hmNodes.get(gl.src) + ", target: " + hmNodes.get(gl.dst) + ", value: " + gl.kappa +  ", shared: " + gl.shared + "},\n";
        }
        // add the nodes that have no links
        int cnt = lstStats.size();
        for(int i = 0; i < cnt; i++) {
            if(!clustersOnly || !lstClusterNums.get(i).isEmpty()) {
                if(!hmNodes.containsKey(i)) {
                    strNodes += "{id: " + idx + ", idx: " + i + ", label: \"" + lstStats.get(i).getFeature() + "\", name: \"" + lstStats.get(i).getDescription() + 
                            "\", grpcnt: 1, group: 0, genes: " + lstStats.get(i).getTestItemsCnt() + ", isos: " + lstStats.get(i).getTestItemsCnt() + "},\n";
                    hmNodes.put(i, idx++);
                }
            }
        }
        strNodes += "];\n";
        strEdges += "];\n";
        return (strGroups + strGroupNames + strClusterLabels + strNodes + strEdges);
    }
    private int getMainCluster(int src, ArrayList<GraphLink> lstLinks, ArrayList<ArrayList<Integer>> lstClusterNums) {
        int num = 0;
        int dst, clnum;
        HashMap<Integer, Integer> hmClusters = new HashMap<>();
        for(GraphLink gl : lstLinks) {
            dst = -1;
            if(gl.src == src)
                dst = gl.dst;
            else if(gl.dst == src)
                dst = gl.src;
            if(dst != -1) {
                // this can be an issue if many of the nodes have multiple clusters
                // we'll assume that's not the case for now, revisit if needed
                clnum = lstClusterNums.get(dst).get(0);
                if(!hmClusters.containsKey(clnum))
                    hmClusters.put(clnum, 1);
                else
                    hmClusters.put(clnum, hmClusters.get(clnum) + 1);
            }
            int cnt = 0;
            for(int cln : hmClusters.keySet()) {
                int newcnt = hmClusters.get(cln);
                if(newcnt > cnt) {
                    cnt = newcnt;
                    num = cln;
                }
            }
        }
        System.out.println("node: " + src + ", cln: " + num);
        return num;
    }
    private void getUnClusteredLinks(double minKappa, ArrayList<DataFEA.FEAStatsData> lstStats, 
                                     ArrayList<double[]> lstKappas, ArrayList<int[]> lstShared,
                                     ArrayList<ArrayList<Integer>> lstClusterNums, ArrayList<GraphLink> lstLinks) {
        HashMap<String, Object> hmLinks = new HashMap<>();
        int[] shared;
        double[] kappas;
        int idx = 0;
        for(ArrayList<Integer> lst : lstClusterNums) {
            if(lst.isEmpty()) {
                shared = lstShared.get(idx);
                kappas = lstKappas.get(idx);
                for(int i = 0; i < lstKappas.size(); i++) {
                    if(i != idx && kappas[i] >= minKappa) {
                        // we want to have two separate entries in linksData but not in lstLinks
                        if(!hmLinks.containsKey(idx + "-" + i)) {
                            hmLinks.put(idx + "-" + i, null);
                            if(!hmLinks.containsKey(i + "-" + idx)) {
                                hmLinks.put(i + "-" + idx, null);
                                lstLinks.add(new GraphLink(idx, i, lstClusterNums.get(idx), lstClusterNums.get(i), "normal", shared[i], kappas[i]));
                            }
                            DataFEA.FEAStatsData src = lstStats.get(idx);
                            DataFEA.FEAStatsData dst = lstStats.get(i);
                            System.out.println("src: " + idx + ", dst: " + i);
                            linksData.add(new LinksTableData(0, src.getFeature(), src.getDescription(), dst.getFeature(), dst.getDescription(), shared[i], kappas[i]));
                        }
                    }
                }
            }
            idx++;
        }
    }
    private void getClusterLinks(int clusterNum, double minKappa, ArrayList<DataFEA.FEAStatsData> lstStats, 
            ArrayList<double[]> lstKappas, ArrayList<int[]> lstShared,
            ArrayList<Cluster.ClusterDef> lstClusters, ArrayList<ArrayList<Integer>> lstClusterNums,
            ArrayList<GraphLink> lstLinks, boolean clustersOnly) {
        HashMap<String, Object> hmLinks = new HashMap<>();
        int[] shared;
        double[] kappas;
        ArrayList<Integer> members = lstClusters.get(clusterNum).members;
        for(int idx : members) {
            shared = lstShared.get(idx);
            kappas = lstKappas.get(idx);
            for(int i = 0; i < lstKappas.size(); i++) {
                if(i != idx && kappas[i] >= minKappa) {
                    if(!clustersOnly || !lstClusterNums.get(i).isEmpty()) {
                        // we want to have two separate entries in linksData but not in lstLinks
                        if(!hmLinks.containsKey(idx + "-" + i)) {
                            double kappa = Double.parseDouble(String.format("%.2f", ((double)Math.round(kappas[i]*100)/100.0)));
                            hmLinks.put(idx + "-" + i, null);
                            if(!hmLinks.containsKey(i + "-" + idx)) {
                                hmLinks.put(i + "-" + idx, null);
                                lstLinks.add(new GraphLink(idx, i, lstClusterNums.get(idx), lstClusterNums.get(i), "normal", shared[i], kappa));
                            }
                            DataFEA.FEAStatsData src = lstStats.get(idx);
                            DataFEA.FEAStatsData dst = lstStats.get(i);
                            //System.out.println("cn: " + clusterNum + ", src: " + idx + ", dst: " + i);
                            linksData.add(new LinksTableData(clusterNum+1, src.getFeature(), src.getDescription(), dst.getFeature(), dst.getDescription(), shared[i], kappa));
                        }
                    }
                }
            }
        }
    }


    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            readClusterResultsFiles();
            showSubTab();
        }
        else {
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        boolean loaded = false;
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "FEA Clustering in Progress...");
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
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("FEA Clusters failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("FEA Clusters failed - task aborted.");
            app.ctls.alertWarning("Funtional Enrichment Analysis Clusters", "FEA Clusters failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(loaded) {
                app.logInfo("FEA clustering completed successfully.");
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab();
            }
            else {
                app.logInfo("FEA clustering " + (taskAborted? "aborted by request." : "failed."));
                app.ctls.alertWarning("FEA Clustering", "FEA clustering " + (taskAborted? "aborted by request." : "failed."));
                setRunAnalysisScreenTitle("FEA clustering " + (taskAborted? "Aborted by Request" : "Failed"));
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    clustersData.clear();
                    nodesData.clear();
                    linksData.clear();
                    clustersHTML = "";
                    String logfilepath = feaData.getFEAClusterLogFilepath(analysisId);
                    Utils.removeFile(Paths.get(logfilepath));
                    outputFirstLogLine("FEA enriched features clustering thread running...", logfilepath);

                    String dataType = feaParams.get(DlgFEAnalysis.Params.DATATYPE_PARAM);
                    String itemsName = app.data.getDataTypePlural(dataType);

                    // get test list
                    appendLogLine("Loading test list...", logfilepath);
                    HashMap<String, Object> hmTest = new HashMap<>();
                    ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(feaData.getFEATestListFilepath(dataType, analysisId), false);
                    if(!lstItems.isEmpty()) {
                        for(String item : lstItems)
                            hmTest.put(item, null);
                    }
                    appendLogLine("Loading background list...", logfilepath);
                    HashMap<String, Object> hmBkgnd = new HashMap<>();
                    lstItems = Utils.loadSingleItemListFromFile(feaData.getFEABkgndListFilepath(dataType, analysisId), false);
                    if(!lstItems.isEmpty()) {
                        for(String item : lstItems)
                            hmBkgnd.put(item, null);
                    }
                    double sigValue = Double.parseDouble(feaParams.get(DlgFEAnalysis.Params.SIGVAL_PARAM));
                    DataFEA.EnrichedFeaturesData data = feaData.getFEAEnrichedFeaturesData(dataType, analysisId, sigValue, hmTest, hmBkgnd);
                    double pct;
                    int nodes = data.lstStats.size();
                    String html = "";
                    appendLogLine("Generating clusters...", logfilepath);
                    Cluster cluster = new Cluster(new DlgClusterAnalysis.Params(clusterParams));
                    cd = cluster.getClusters(data.lstMembers.size(), data.lstTermMembers);
                    appendLogLine("Number of clusters found: " + cd.clusters.size(), logfilepath);
                    if(!cd.clusters.isEmpty()) {
                        appendLogLine("Building cluster data tables...", logfilepath);
                        double pctTotal = 0;
                        int cnodes = 0;
                        DataFEA.FEAStatsData sd;
                        System.out.println("lstStatsSize: " + data.lstStats.size() + ", clusters: " + cd.clusters.size());
                        for(Cluster.ClusterDef def : cd.clusters) {
                            pct = def.members.size() * 100 / (double) nodes;
                            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                            pctTotal += pct;
                            cnodes += def.members.size();
                            clustersData.add(new ClustersTableData(def.num, "Cluster " + def.num, def.members.size(), pct));
                            for(int idx : def.members) {
                                System.out.println("defMbrIdx: " + idx);
                                sd = data.lstStats.get(idx);
                                nodesData.add(new NodesTableData(def.num, sd.getFeature(), sd.getDescription(), sd.getCategory(), sd.getAdjustedPV(), sd.getTestItemsCnt(), sd.getNonTestItemsCnt()));
                            }
                        }
                        pctTotal = Double.parseDouble(String.format("%.2f", ((double)Math.round(pctTotal*100)/100.0)));
                        clustersInfo = "Clusters: " + cd.clusters.size() + "  Clustered Nodes: " + cnodes + " (" + pctTotal + "% of " + nodes + ")";

                        ArrayList<Cluster.ClusterDef> lstClusters = cd.clusters;
                        //System.out.println(cluster.getString(lstClusters));
                        String memberType = itemsName;

                        boolean clustersOnly = true;
                        ArrayList<GraphLink> lstLinks = new  ArrayList();
                        HashMap<String, Object> hmLinks = new HashMap<>();
                        if(!clustersOnly)
                            getUnClusteredLinks(cluster.getMinKappa(), data.lstStats, cd.kappas, cd.shared, cd.clusterNums, lstLinks);
                        for(int i = 0; i < cd.clusters.size(); i++)
                            getClusterLinks(i, cluster.getMinKappa(), data.lstStats, cd.kappas, cd.shared, cd.clusters, cd.clusterNums, lstLinks, clustersOnly);
                        System.out.println("links: " + lstLinks.size());
                        for(Cluster.ClusterDef def : cd.clusters) {
                            ArrayList<String> lstCT = new ArrayList<>();
                            for(int i = 0; i < def.members.size(); i++)
                                lstCT.add(data.lstStats.get(def.members.get(i)).getFeature());
                            lstClusterTerms.add(lstCT);
                        }

                        // build graph html file
                        clustersHTML = buildHTML(memberType, lstLinks, data.lstStats, cd.clusters, cd.clusterNums, clustersOnly, logfilepath);
                    }else {
                        clustersHTML = "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<body style=\"width:100%; height:100%;\">\n" +
                        "<div style=\"left:50%; top:50%; transform:translate(-50%,-50%); -webkit-transform:translate(-50%,-50%); position:absolute;\">\n" +
                        "   <span style=\"color:black; font-size:small;\">No content in graph</span>\n" +
                        "</div>\n" +
                        "</body>\n" +
                        "</html>\n";
                    }

                    // assume everything is OK - change to use flag if needed
                    writeClusterResultsFiles(clustersData, nodesData, linksData, clustersHTML);
                    if(readClusterResultsFiles())
                        loaded = true;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("FEA Clustering", task);
            return task;
        }
    }

    //
    // Static Functions
    //
    
    // Just add all possible sub tabs - no need to bother with experiment type, etc.
    // List is used to close all subtabs that start with given panel names
    // Used to make sure we close all associated subtabs when running the analysis - data will be deleted
    public static String getAssociatedDVSubTabs(String id) {
        String subTabs = TabProjectDataViz.Panels.CLUSTERSFEA.toString() + id + ";";
        subTabs += TabProjectDataViz.Panels.CLUSTERSGOFEA.toString() + id;
        return subTabs;
    }

    //
    // Data Classes
    //
    public  class ClustersTableData implements Comparable<ClustersTableData> {
        public final SimpleIntegerProperty id;
        public final SimpleStringProperty name;
        public final SimpleIntegerProperty nodes;
        public final SimpleDoubleProperty pct;
 
        public ClustersTableData(int id, String name, int nodes, double pct) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.nodes = new SimpleIntegerProperty(nodes);
            this.pct = new SimpleDoubleProperty(pct);
        }
        public ClustersTableData(String tsv) throws Exception {
            String[] fields;
            fields = tsv.split("\t");
            if(fields.length == 4) {
                this.id = new SimpleIntegerProperty(Integer.parseInt(fields[0]));
                this.name = new SimpleStringProperty(fields[1].trim());
                this.nodes = new SimpleIntegerProperty(Integer.parseInt(fields[2]));
                this.pct = new SimpleDoubleProperty(Double.parseDouble(fields[3]));
            }
            else
                throw new Exception("Invalid ClustersTableData values.");
        }
        public String toTSV() {
            String line = id.get() + "\t" + name.get() + "\t" + nodes.get() + "\t" + pct.get();
            return line;
        }
        public Integer getID() { return id.get(); }
        public String getName() { return name.get(); }
        public void setName(String val) { name.set(val); }
        public Integer getNodes() { return nodes.get(); }
        public Double getPercent() { return pct.get(); }
        @Override
        public int compareTo(ClustersTableData td) {
            return (getID().compareTo(td.getID()));
        }
    }
    public  class NodesTableData implements Comparable<NodesTableData> {
        public final SimpleIntegerProperty clusterId;
        public final SimpleStringProperty id;
        public final SimpleStringProperty name;
        public final SimpleStringProperty cat;
        public final SimpleDoubleProperty pvalue;
        public final SimpleIntegerProperty testGenes;
        public final SimpleIntegerProperty nonTestGenes;
        public final SimpleIntegerProperty totalGenes;
 
        public NodesTableData(int clusterId, String id, String name, String cat, double pvalue, int testGenes, int nonTestGenes) {
            this.clusterId = new SimpleIntegerProperty(clusterId);
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.cat = new SimpleStringProperty(cat);
            this.pvalue = new SimpleDoubleProperty(pvalue);
            this.testGenes = new SimpleIntegerProperty(testGenes);
            this.nonTestGenes = new SimpleIntegerProperty(nonTestGenes);
            this.totalGenes = new SimpleIntegerProperty(testGenes + nonTestGenes);
        }
        public NodesTableData(String tsv) throws Exception {
            String[] fields;
            fields = tsv.split("\t");
            if(fields.length == 7) {
                this.clusterId = new SimpleIntegerProperty(Integer.parseInt(fields[0]));
                this.id = new SimpleStringProperty(fields[1].trim());
                this.name = new SimpleStringProperty(fields[2].trim());
                this.cat = new SimpleStringProperty(fields[3].trim());
                this.pvalue = new SimpleDoubleProperty(Double.parseDouble(fields[4]));
                this.testGenes = new SimpleIntegerProperty(Integer.parseInt(fields[5]));
                this.nonTestGenes = new SimpleIntegerProperty(Integer.parseInt(fields[6]));
                this.totalGenes = new SimpleIntegerProperty(testGenes.get() + nonTestGenes.get());
            }
            else
                throw new Exception("Invalid NodesTableData values.");
        }
        public String toTSV() {
            String line = clusterId.get() + "\t" + id.get() + "\t" + name.get() + "\t" + cat.get() + "\t" + pvalue.get() + "\t" + testGenes.get() + "\t" + nonTestGenes.get();
            return line;
        }
        public Integer getClusterID() { return clusterId.get(); }
        public String getID() { return id.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return cat.get(); }
        public Double getPValue() { return pvalue.get(); }
        public Integer getTestGenes() { return testGenes.get(); }
        public Integer getNonTestGenes() { return nonTestGenes.get(); }
        public Integer getTotalGenes() { return totalGenes.get(); }

        @Override
        public int compareTo(NodesTableData td) {
            if(clusterId.get() == td.clusterId.get())
                return (id.get().compareTo(td.id.get()));
            else
                return (getClusterID().compareTo(td.getClusterID()));
        }
    }
    public  class LinksTableData  implements Comparable<LinksTableData> {
        public final SimpleIntegerProperty clusterId;
        public final SimpleStringProperty id1;
        public final SimpleStringProperty name1;
        public final SimpleStringProperty id2;
        public final SimpleStringProperty name2;
        public final SimpleIntegerProperty shared;
        public final SimpleDoubleProperty kappa;
 
        public LinksTableData(int clusterId, String id1, String name1, String id2, String name2, int shared, double kappa) {
            this.clusterId = new SimpleIntegerProperty(clusterId);
            this.id1 = new SimpleStringProperty(id1);
            this.name1 = new SimpleStringProperty(name1);
            this.id2 = new SimpleStringProperty(id2);
            this.name2 = new SimpleStringProperty(name2);
            this.shared = new SimpleIntegerProperty(shared);
            this.kappa = new SimpleDoubleProperty(kappa);
        }
        public String toTSV() {
            String line = clusterId.get() + "\t" + id1.get() + "\t" + name1.get() + "\t" + id2.get() + "\t" + name2.get() + "\t" + shared.get() + "\t" + kappa.get();
            return line;
        }
        public LinksTableData(String tsv) throws Exception {
            String[] fields;
            fields = tsv.split("\t");
            if(fields.length == 7) {
                this.clusterId = new SimpleIntegerProperty(Integer.parseInt(fields[0]));
                this.id1 = new SimpleStringProperty(fields[1].trim());
                this.name1 = new SimpleStringProperty(fields[2].trim());
                this.id2 = new SimpleStringProperty(fields[3].trim());
                this.name2 = new SimpleStringProperty(fields[4].trim());
                this.shared = new SimpleIntegerProperty(Integer.parseInt(fields[5]));
                this.kappa = new SimpleDoubleProperty(Double.parseDouble(fields[6]));
            }
            else
                throw new Exception("Invalid LinksTableData values.");
        }
        public Integer getClusterID() { return clusterId.get(); }
        public String getID1() { return id1.get(); }
        public String getName1() { return name1.get(); }
        public String getID2() { return id2.get(); }
        public String getName2() { return name2.get(); }
        public Integer getShared() { return shared.get(); }
        public Double getKappa() { return kappa.get(); }
        @Override
        public int compareTo(LinksTableData td) {
            if(clusterId.get() == td.clusterId.get())
                if(id1.get().equals(td.id1.get()))
                    return(id2.get().compareTo(td.id2.get()));
                else
                    return(id1.get().compareTo(td.id1.get()));
            else
                return (getClusterID().compareTo(td.getClusterID()));
        }
    }
    public static class GraphLink {
        int src, dst;
        String type;
        int shared;
        double kappa;
        ArrayList<Integer> srcClusters, dstClusters;
        public GraphLink(int src, int dst, ArrayList<Integer> srcClusters, ArrayList<Integer> dstClusters, String type, int shared, double kappa) {
            this.src = src;
            this.dst = dst;
            this.srcClusters = srcClusters;
            this.dstClusters = dstClusters;
            this.type = type;
            this.kappa = kappa;
            this.shared = shared;
        }
    }
}
