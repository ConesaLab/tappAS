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
public class SubTabGSEAClusters extends SubTabBase {
    TableView tblNodes;

    boolean hasData = false;
    DataGSEA gseaData;
    HashMap<String, String> gseaParams = new HashMap<>();
    DlgGSEAnalysis.Params params;
    String analysisId = "";
    String feature = "";
    String clustersInfo = "";
    String clustersHTML = "";
    ClusterGSEA.ClusterData cd = null;
    HashMap<String, String> clusterParams = null;
    ArrayList<ArrayList<String>> lstClusterTerms = new ArrayList<>();
    ObservableList<ClustersTableData> clustersData = FXCollections.observableArrayList();
    ObservableList<NodesTableData> nodesData = FXCollections.observableArrayList();
    ObservableList<LinksTableData> linksData = FXCollections.observableArrayList();
    ArrayList<Integer> lstClusters = new ArrayList<>();
    
    public SubTabGSEAClusters(Project project) {
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
            gseaData = new DataGSEA(project);
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.CLUSTERSGSEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.CLUSTERSGSEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");
                
                // GSEA parameter file is required
                result = Files.exists(Paths.get(gseaData.getGSEAParamsFilepath(analysisId)));
                if(result) {
                    gseaParams = gseaData.getGSEAParams(analysisId);
                    params = new DlgGSEAnalysis.Params(gseaParams);
                    String shortName = getShortName(params.name);
                    subTabInfo.title = "GSEA Clusters: '" + shortName + "'";
                    subTabInfo.tooltip = "GSEA enriched features clusters for '" + params.name + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    feature = params.getFeaturesName();

                    // GSEA data is required
                    result = gseaData.hasGSEAData(params.dataType.name(), analysisId);
                    if(result) {
                        // clusters parameter file is required
                        result = Files.exists(Paths.get(gseaData.getGSEAClusterParamsFilepath(analysisId)));
                        if(result) {
                            clusterParams = gseaData.getGSEAClusterParams(analysisId);
                            hasData = gseaData.hasGSEAClusterData(analysisId);
                        }
                        else
                            app.ctls.alertInformation("GSEA Cluster Results", "Missing GSEA clusters parameter file");
                    }
                    else
                        app.ctls.alertInformation("GSEA Cluster Results", "Missing GSEA data");
                }
                else
                    app.ctls.alertInformation("GSEA Cluster Results", "Missing GSEA parameter file");
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
        app.ctlr.runGSEAClusterAnalysis(analysisId, clusterParams);
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        if(obj != null) {
            //just node tables are visible for search function
            if(obj.equals(tblNodes)) {
                if(txt.isEmpty() && !hide)
                    setTableSearchActive(tblNodes, false);
                else
                    setTableSearchActive(tblNodes, true);
                TableView<NodesTableData> tblViewNodes = (TableView<NodesTableData>) obj;
                TabBase.SearchInfo infoNodes = (TabBase.SearchInfo) tblViewNodes.getUserData();
                infoNodes.txt = txt;
                infoNodes.hide = hide;
                ObservableList<NodesTableData> itemsNodes = (ObservableList<NodesTableData>) infoNodes.data;
                if(itemsNodes != null) {
                    ObservableList<NodesTableData> matchItems;
                    if(txt.isEmpty()) {
                        // restore original dataset
                        matchItems = itemsNodes;
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(NodesTableData data : itemsNodes) {
                            if(data.getID().toLowerCase().contains(txt) || data.getName().toLowerCase().contains(txt))
                                matchItems.add(data);
                        }
                    }
                    tblViewNodes.setItems(matchItems);
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
        //tblClusters = (TableView) tabNode.lookup("#tblClusters");
        tblNodes = (TableView) tabNode.lookup("#tblNodes");

        // create data visualization context menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Clusters Graph");
        item.setOnAction((event) -> { 
            HashMap<String, Object> graphArgs = new HashMap<>();
            graphArgs.put("html", clustersHTML);
            graphArgs.put("lstClusters", lstClusters);
            app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.CLUSTERSGSEA, analysisId, graphArgs);
        });

        cm.getItems().add(item);
        subTabInfo.cmDataViz = cm;

        // get GSEA data type
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
        
        cols.get(colIdx).setCellValueFactory(new PropertyValueFactory<>("PValue"));
        cols.get(colIdx).setPrefWidth(130 + params.rankedList1.name().length()*5);
        cols.get(colIdx++).setText("Over-Pvalue - " + params.rankedList1.name());
        
        cols.get(colIdx).setCellValueFactory(new PropertyValueFactory<>("PValue2"));
        cols.get(colIdx).setPrefWidth(130 + params.rankedList2.name().length()*5);
        cols.get(colIdx++).setText("Over-Pvalue - " + params.rankedList2.name());
        addRowNumbersCol(tblNodes);

        //hide cluster column
        cols.get(1).setVisible(false);

        cm = new ContextMenu();
        item = new MenuItem("Drill down data");
        item.setOnAction((event) -> { drillDownNodeData(tblNodes); });
        cm.getItems().add(item);
        app.export.setupSimpleTableExport(tblNodes, cm, true, "tappAS_GSEA_ClusterNodes.tsv");
        tblNodes.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblNodes);
        setupTableSearch(tblNodes);
        subTabInfo.lstSearchTables.add(tblNodes);

        //code from listener in cluster table
        ObservableList<NodesTableData> data = FXCollections.observableArrayList();
        for(NodesTableData nd : nodesData) {
            if(nd.getClusterID()==0)
                data.add(nd);
        }
        FXCollections.sort(data);
        tblNodes.setItems(data);
        if(data.size() > 0)
            tblNodes.getSelectionModel().select(0);
        tblNodes.setUserData(new TabBase.SearchInfo(tblNodes.getItems(), "", false));
        setTableSearchActive(tblNodes, false);
        //

        // manage focus on both tables
        tblNodes.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblNodes;
        });
        setupExportMenu();
    }
    private void viewLog() {
        String logdata = app.getLogFromFile(gseaData.getGSEAClusterLogFilepath(analysisId), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export cluster nodes table...");
        item.setOnAction((event) -> { 
            app.export.exportTableData(tblNodes, true, "tappAS_GSEA_ClusterNodes.tsv");
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 

    private void drillDownNodeData(TableView tbl) {
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            app.ctls.alertInformation("GSEA Drill Down Data", "You have multiple rows highlighted.\nHighlight a single row with the feature you are interested in.");
        }
        else {
            NodesTableData node = (NodesTableData) tbl.getItems().get(tbl.getSelectionModel().getSelectedIndex());
            String featureId = node.id.getValue().trim();
            String description = node.name.getValue().trim();
            String fileFeatures = Paths.get(gseaData.getGSEAFeaturesFilepath(analysisId, gseaParams.get(DlgGSEAnalysis.Params.DATATYPE_PARAM), 1)).toString();
            String fileFeatures2 = Paths.get(gseaData.getGSEAFeaturesFilepath(analysisId, gseaParams.get(DlgGSEAnalysis.Params.DATATYPE_PARAM), 2)).toString();
            // search in both list inside method drillDownGSEAFeatures
            ArrayList<String> lstTestItems = Utils.loadSingleItemListFromFile(gseaData.getGSEARankedListFilepath(gseaParams.get(DlgGSEAnalysis.Params.DATATYPE_PARAM), analysisId, 1), false);
            if(lstTestItems.isEmpty())
                lstTestItems = Utils.loadSingleItemListFromFile(gseaData.getGSEARankedListFilepath(gseaParams.get(DlgGSEAnalysis.Params.DATATYPE_PARAM), analysisId, 2), false);
            app.ctlr.drillDownGSEAFeatures(DataApp.DataType.valueOf(gseaParams.get(DlgGSEAnalysis.Params.DATATYPE_PARAM)), featureId, description, fileFeatures, fileFeatures2, lstTestItems);
        }
    }
//        // get highlighted row's data and show drilldown data
//        ObservableList<Integer> lstIdxs = tblNodes.getSelectionModel().getSelectedIndices();
//        if(lstIdxs.size() != 1) {
//            String msg = "You have multiple rows highlighted.\nHighlight a single row\nwith the cluster node you want to drill down.";
//            app.ctls.alertInformation("Drill Down Data", msg);
//        }
//        else {
//            //we have to add a name to the individual cluster
//            String cluster = "Features";//cluster.name();
//            String node = ((NodesTableData)tblNodes.getItems().get(lstIdxs.get(0))).getID();
//            app.ctlr.drillDownGSEAClusterNode(params, analysisId, cluster, node);
//        }
//    }

    private boolean writeClusterResultsFiles(ObservableList<ClustersTableData> clusters,
                                             ObservableList<NodesTableData> nodes,
                                             ObservableList<LinksTableData> links, String html) {
        boolean result = false;
        Writer writer = null;
        String filepath = gseaData.getGSEAResultsClusterFilepath("clusters", analysisId);
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
            for(ClustersTableData data : clusters)
                writer.write(data.toTSV() + "\n");
            result = true;
        }
        catch (Exception e) {
            result = false;
            app.logError("Unable to write GSEA clusters file: " + e.getMessage());
        } finally {
            try { if(writer != null) writer.close(); } catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }

        if(result)  {
            // write html contents out to file
            writer = null;
            filepath = gseaData.getGSEAResultsClusterFilepath("nodes", analysisId);
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                for(NodesTableData data : nodes)
                    writer.write(data.toTSV() + "\n");
            } catch(Exception e) {
                result = false;
                app.logError("Unable to write GSEA cluster nodes file: " + e.getMessage());
            } finally {
                try {if(writer != null) writer.close();} catch (Exception e) {System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }

        if(result)  {
            writer = null;
            filepath = gseaData.getGSEAResultsClusterFilepath("links", analysisId);
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
            filepath = gseaData.getGSEAResultsClusterFilepath("graph", analysisId);
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
            filepath = gseaData.getGSEAClusterDoneFilepath(analysisId);
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
        String filepath = gseaData.getGSEAResultsClusterFilepath("clusters", analysisId);
        result = true;

        if(result)  {
            filepath = gseaData.getGSEAResultsClusterFilepath("nodes", analysisId);
            try {
                if(Files.exists(Paths.get(filepath))) {
                    List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                    for(String line : lines) {
                        if(line.charAt(0) != '#')
                            nodesData.add(new NodesTableData(line));
                    }
                    // generate list of features - used by GO Terms graph
                    HashMap<String, HashMap<String, Object>> hmfeatures = new HashMap<>();
                    int idmax = 0;
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
                app.logError("Unable to read GSEA cluster nodes file: " + e.getMessage());
            }
        }

        if(result)  {
            filepath = gseaData.getGSEAResultsClusterFilepath("links", analysisId);
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
                app.logError("Unable to read GSEA cluster links file: " + e.getMessage());
            }
        }

        if(result)  {
            filepath = gseaData.getGSEAResultsClusterFilepath("graph", analysisId);
            try {
                if(Files.exists(Paths.get(filepath))) {
                    clustersHTML = new String(Files.readAllBytes(Paths.get(filepath)));
                    result = true;
                }
            } catch(Exception e) {
                result = false;
                app.logError("Unable to read GSEA clusters graph html file: " + e.getMessage());
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
    private String buildHTML(String memberType, ArrayList<GraphLink> lstLinks, ArrayList<DataGSEA.GSEAStatsData> lstStats, 
                   ArrayList<ClusterGSEA.ClusterDef> lstClusters, ArrayList<ArrayList<Integer>> lstClusterNums, boolean clustersOnly) {
        String htmlClusters = app.data.getFileContentFromResource("/tappas/scripts/GOClustersMDGSA.html");
        String htmlArrays = getJSArrays(lstLinks, lstStats, lstClusters, lstClusterNums, clustersOnly);
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
    private String getJSArrays(ArrayList<GraphLink> lstLinks, ArrayList<DataGSEA.GSEAStatsData> lstStats, 
                   ArrayList<ClusterGSEA.ClusterDef> lstClusters, ArrayList<ArrayList<Integer>> lstClusterNums, boolean clustersOnly) {
        HashMap<Integer, Integer> hmNodes = new HashMap<>();
        String strGroups = "var dgroups = [0";
        int total_members = 0;
        for(ClusterGSEA.ClusterDef def : lstClusters)
            total_members += def.members.size();
        strGroups += "," + total_members + "," + total_members;
        strGroups += "];\n";
        String strGroupNames = "var groupNames = [\"\"";
        String strClusterLabels = "var dclusters = [\n";
        String strClusterRealLabels = "var dclustersReal = [\n";
        int wLabel = 200;
        int hLabel = 20;
        double x1 = 227.5;
        int y1 = 80;
        int x2 = 100;
        int y2 = 50;
        int cy = 30;
        int cx = 80;
        
        //just one cluster
        strGroupNames += ", \"" + params.rankedList1.name() + "\"" + ", \"" + params.rankedList2.name() + "\"";
        strClusterLabels += "{id: " + 1 + ", idx: " + 1 + ", width: " + wLabel + ", height: " + hLabel + ", x: " + x1 + ", y: " + y1 + ", \"cx\": " + cx + ", \"cy\": " + cy + ", text: \"" + params.rankedList1.name() + "\"},\n";
        cy += 20;
        strClusterLabels += "{id: " + 2 + ", idx: " + 2 + ", width: " + wLabel + ", height: " + hLabel + ", x: " + x2 + ", y: " + y2 + ", \"cx\": " + cx + ", \"cy\": " + cy + ", text: \"" + params.rankedList2.name() + "\"},\n";
        strClusterRealLabels +=  "{id: " + 2 + ", idx: " + 2 + ", width: " + wLabel + ", height: " + hLabel + ", x: " + (x2+y1) + ", y: " + y2 + ", \"cx\": " + cx + ", \"cy\": " + cy + ", text: \"" + params.rankedList1.name() + "\"},\n";
        
        strGroupNames += "];\n";
        strClusterLabels += "];\n";
        strClusterRealLabels  += "];\n";
        String strNodes = "var dnodes = [\n";
        String strEdges = "var dlinks = [\n";
        
        int idx = 0;
        int cluster;
        String clusters;
        GO go = new GO();
        for(GraphLink gl : lstLinks) {
            if(go.getGOTermInfo(lstStats.get(gl.dst).getFeature()) == null) //we update go.obo and maybe the GO change and they are not any more
                continue;
            // add namespace: later, after term: - will need to instantiate GO class or add call to GOTerms
            // GO go = new GO();  go.getGOTermInfo(term).namespace;
            if(!hmNodes.containsKey(gl.src)) {
                //cluster = gl.srcClusters.isEmpty()? 0 : gl.srcClusters.size() > 1? getMainCluster(gl.src, lstLinks, lstClusterNums) : gl.srcClusters.get(0);
                //clusters = gl.srcClusters.size() > 1? ", groups: \"" + gl.srcClusters.toString().replaceAll("[\\[ \\]]", "") + "\"" : "";
                cluster = 2;
                clusters = ", groups: \"1,2\"";
                //label = GO.getShortName(lstStats.get(gl.src).getDescription(), 27)
                //always 2 totalgroups=grpcnt - 2 ranked lists
                strNodes += "{id: " + idx + ", idx: " + gl.src + ", term: \"" + lstStats.get(gl.src).getFeature() + "\", namespace: \"" + go.getGOTermInfo(lstStats.get(gl.src).getFeature()).namespace + "\", label: \"" + lstStats.get(gl.src).getDescription() + "\", name: \"" + lstStats.get(gl.src).getDescription() +
                        "\", grpcnt: " + 2 + ", group: " + cluster + clusters + ", test1: \"" + params.rankedList1.name() + "\", test2: \"" + params.rankedList2.name() + "\", pval1: " + lstStats.get(gl.src).overPV.get() + ", pval2: " + lstStats.get(gl.src).overPV2.get() + ", prop: " + lstStats.get(gl.src).prop.get() + ", genes: " + lstStats.get(gl.src).geneNumber() + "},\n";
                hmNodes.put(gl.src, idx++);
            }
            if(!hmNodes.containsKey(gl.dst)) {
                //cluster = gl.dstClusters.isEmpty()? 0 : gl.dstClusters.size() > 1? getMainCluster(gl.dst, lstLinks, lstClusterNums) : gl.dstClusters.get(0);
                //clusters = gl.dstClusters.size() > 1? ", groups: \"" + gl.dstClusters.toString().replaceAll("[\\[ \\]]", "") + "\"" : "";
                cluster = 2;
                clusters = ", groups: \"1,2\"";
                strNodes += "{id: " + idx + ", idx: " + gl.dst + ", term: \"" + lstStats.get(gl.dst).getFeature() + "\", namespace: \"" + go.getGOTermInfo(lstStats.get(gl.dst).getFeature()).namespace +  "\", label: \"" + lstStats.get(gl.dst).getDescription() + "\", name: \"" + lstStats.get(gl.dst).getDescription() +
                        "\", grpcnt: " + 2 + ", group: " + cluster + clusters + ", test1: \"" + params.rankedList1.name() + "\", test2: \"" + params.rankedList2.name() + "\", pval1: " + lstStats.get(gl.dst).overPV.get() + ", pval2: " + lstStats.get(gl.dst).overPV2.get() + ", prop: " + lstStats.get(gl.dst).prop.get() + ", genes: " + lstStats.get(gl.dst).geneNumber() + "},\n";
                hmNodes.put(gl.dst, idx++);
            }
            //strEdges += "{source: " + hmNodes.get(gl.src) + ", target: " + hmNodes.get(gl.dst) + ", value: " + gl.kappa +  ", shared: " + gl.shared + "},\n";
            strEdges += "{source: " + hmNodes.get(gl.src) + ", target: " + hmNodes.get(gl.dst) +  ", shared: " + gl.shared + "},\n";

        }
        // add the nodes that have no links - change in the future - add the same info that others nodes !!!
        int cnt = lstStats.size();
        int noAnnotated = 0;
        for(int i = 0; i < cnt; i++) {
            if(!clustersOnly || !lstClusterNums.get(i).isEmpty()) {
                if(!hmNodes.containsKey(i) && go.getGOTermInfo(lstStats.get(i).getFeature())!=null) {
                    cluster = 2;
                    clusters = ", groups: \"1,2\"";
                    strNodes += "{id: " + idx + ", idx: " + i + ", term: \"" + lstStats.get(i).getFeature() + "\", namespace: \"" + go.getGOTermInfo(lstStats.get(i).getFeature()).namespace +  "\", label: \"" + lstStats.get(i).getDescription() + "\", name: \"" + lstStats.get(i).getDescription() +
                            "\", grpcnt: " + 2 + ", group: " + cluster + clusters + ", test1: \"" + params.rankedList1.name() + "\", test2: \"" + params.rankedList2.name() + "\", pval1: " + lstStats.get(i).overPV.get() + ", pval2: " + lstStats.get(i).overPV2.get() + ", prop: " + lstStats.get(i).prop.get() + ", genes: " + lstStats.get(i).geneNumber() + "},\n";
                    hmNodes.put(i, idx++);
                }else{
                    noAnnotated++;
                }
            }
        }
        System.out.println("GOTerms not annotated: " + noAnnotated);
        
        strNodes += "];\n";
        strEdges += "];\n";
        
        return (strGroups + strGroupNames + strClusterLabels + strClusterRealLabels + strNodes + strEdges);
    }
//    private int getMainCluster(int src, ArrayList<GraphLink> lstLinks, ArrayList<ArrayList<Integer>> lstClusterNums) {
//        int num = 0;
//        int dst, clnum;
//        HashMap<Integer, Integer> hmClusters = new HashMap<>();
//        for(GraphLink gl : lstLinks) {
//            dst = -1;
//            if(gl.src == src)
//                dst = gl.dst;
//            else if(gl.dst == src)
//                dst = gl.src;
//            if(dst != -1) {
//                // this can be an issue if many of the nodes have multiple clusters
//                // we'll assume that's not the case for now, revisit if needed
//                clnum = lstClusterNums.get(dst).get(0);
//                if(!hmClusters.containsKey(clnum))
//                    hmClusters.put(clnum, 1);
//                else
//                    hmClusters.put(clnum, hmClusters.get(clnum) + 1);
//            }
//            int cnt = 0;
//            for(int cln : hmClusters.keySet()) {
//                int newcnt = hmClusters.get(cln);
//                if(newcnt > cnt) {
//                    cnt = newcnt;
//                    num = cln;
//                }
//            }
//        }
//        System.out.println("node: " + src + ", cln: " + num);
//        return num;
//    }
    private void getUnClusteredLinks(double minKappa, ArrayList<DataGSEA.GSEAStatsData> lstStats, 
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
                                lstLinks.add(new GraphLink(idx, i, lstClusterNums.get(idx), lstClusterNums.get(i), "normal", shared[i]));
                            }
                            DataGSEA.GSEAStatsData src = lstStats.get(idx);
                            DataGSEA.GSEAStatsData dst = lstStats.get(i);
                            System.out.println("src: " + idx + ", dst: " + i);
                            linksData.add(new LinksTableData(0, src.getFeature(), src.getDescription(), dst.getFeature(), dst.getDescription(), shared[i]));
                        }
                    }
                }
            }
            idx++;
        }
    }
    private void getClusterLinks(int clusterNum, double minKappa, ArrayList<DataGSEA.GSEAStatsData> lstStats, 
            ArrayList<double[]> lstKappas, ArrayList<int[]> lstShared,
            ArrayList<ClusterGSEA.ClusterDef> lstClusters, ArrayList<ArrayList<Integer>> lstClusterNums,
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
                            hmLinks.put(idx + "-" + i, null);
                            if(!hmLinks.containsKey(i + "-" + idx)) {
                                hmLinks.put(i + "-" + idx, null);
                                lstLinks.add(new GraphLink(idx, i, lstClusterNums.get(idx), lstClusterNums.get(i), "normal", shared[i]));
                            }
                            DataGSEA.GSEAStatsData src = lstStats.get(idx);
                            DataGSEA.GSEAStatsData dst = lstStats.get(i);
                            //System.out.println("cn: " + clusterNum + ", src: " + idx + ", dst: " + i);
                            linksData.add(new LinksTableData(clusterNum+1, src.getFeature(), src.getDescription(), dst.getFeature(), dst.getDescription(), shared[i]));
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
            showRunAnalysisScreen(true, "GSEA Clustering in Progress...");
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
            Throwable e = getException();
            if(e != null)
                app.logWarning("GSEA Multivariant data failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("GSEA  Multivariant data failed - task aborted.");
            app.ctls.alertWarning("GSEA Multivariant data", "GSEA  Multivariant data failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(loaded) {
                app.logInfo("GSEA selecting features completed successfully.");
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab();
            }
            else {
                app.logInfo("GSEA  Multivariant data " + (taskAborted? "aborted by request." : "failed."));
                app.ctls.alertWarning("GSEA  Multivariant data", "GSEA  Multivariant data " + (taskAborted? "aborted by request." : "failed."));
                setRunAnalysisScreenTitle("GSEA  Multivariant data " + (taskAborted? "Aborted by Request" : "Failed"));
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
                    String logfilepath = gseaData.getGSEAClusterLogFilepath(analysisId);
                    Utils.removeFile(Paths.get(logfilepath));
                    outputFirstLogLine("GSEA enriched features clustering thread running...", logfilepath);

                    String dataType = gseaParams.get(DlgGSEAnalysis.Params.DATATYPE_PARAM);
                    String itemsName = app.data.getDataTypePlural(dataType);
                    DataGSEA.EnrichedFeaturesData data = null;
                    
                    //list
                    String path = clusterParams.get(DlgGSEACluster.Params.LIST_PARAM);
                    if(path != null){
                        double sigValue = Double.parseDouble(gseaParams.get(DlgGSEAnalysis.Params.SIGVAL_PARAM));
                        ArrayList<String> lstFeatures = Utils.loadSingleItemListFromFile(clusterParams.get(DlgGSEACluster.Params.LIST_PARAM), true);
                        data = gseaData.getGSEAFeaturesData(dataType, analysisId, sigValue, lstFeatures);
                    // pvalue
                    }else{
                        double sigValue = Double.parseDouble(clusterParams.get(DlgGSEACluster.Params.PVAL_PARAM));
                        //we have to implement new method to get both over-pval list
                        data = gseaData.getGSEAEnrichedFeaturesDataCluster(dataType, analysisId, sigValue);
                        if(data == null){
                            outputFirstLogLine("We can not find significative features with " + sigValue + " significance value.", logfilepath);

                            clustersHTML = "<!DOCTYPE html>\n" +
                                    "<html>\n" +
                                    "<body style=\"width:100%; height:100%;\">\n" +
                                    "<div style=\"left:50%; top:50%; transform:translate(-50%,-50%); -webkit-transform:translate(-50%,-50%); position:absolute;\">\n" +
                                    "   <span style=\"color:black; font-size:small;\">No content in graph</span>\n" +
                                    "</div>\n" +
                                    "</body>\n" +
                                    "</html>\n";

                            return null;
                        }
                    }
                    appendLogLine("Generating clusters...", logfilepath);
                    ClusterGSEA cluster = new ClusterGSEA(new DlgGSEACluster.Params(clusterParams));
                    cd = cluster.getClusters(data.lstMembers.size(), data.lstTermMembers);

                    appendLogLine("Adding nodes...", logfilepath);
                    for(tappas.DataGSEA.GSEAStatsData idx : data.lstStats) {
                        System.out.println("defMbrIdx: " + idx.feature);
                        //always cluster = 0
                        nodesData.add(new NodesTableData(0, idx.getFeature(), idx.getDescription(), idx.getCategory(), idx.getOverPV(), idx.getOverPV2()));
                    }
                    boolean clustersOnly = true;
                    String memberType = itemsName;
                    ArrayList<GraphLink> lstLinks = new  ArrayList();

                    if(!clustersOnly)
                        getUnClusteredLinks(cluster.getMinKappa(), data.lstStats, cd.kappas, cd.shared, cd.clusterNums, lstLinks);
                    for(int i = 0; i < cd.clusters.size(); i++)
                        getClusterLinks(i, cluster.getMinKappa(), data.lstStats, cd.kappas, cd.shared, cd.clusters, cd.clusterNums, lstLinks, clustersOnly);

                    System.out.println("links: " + lstLinks.size());

                    double pct = data.lstMembers.size() * 100 / (double) data.lstStats.size();
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    clustersData.add(new ClustersTableData(0, "Cluster " + 0, data.lstStats.size(), pct));

                    // build graph html file
                    appendLogLine("Building network...", logfilepath);
                    clustersHTML = buildHTML(memberType, lstLinks, data.lstStats, cd.clusters, cd.clusterNums, clustersOnly);

                    /*double pct;
                    int nodes = data.lstStats.size();
                    String html = "";
                    appendLogLine("Generating clusters...", logfilepath);
                    ClusterGSEA cluster = new ClusterGSEA(new DlgGSEACluster.Params(clusterParams));
                    cd = cluster.getClusters(data.lstMembers.size(), data.lstTermMembers);
                    appendLogLine("Number of clusters found: " + cd.clusters.size(), logfilepath);
                    if(!cd.clusters.isEmpty()) {
                        appendLogLine("Building cluster data tables...", logfilepath);
                        double pctTotal = 0;
                        int cnodes = 0;
                        DataGSEA.GSEAStatsData sd;
                        System.out.println("lstStatsSize: " + data.lstStats.size() + ", clusters: " + cd.clusters.size());
                        for(ClusterGSEA.ClusterDef def : cd.clusters) {
                            pct = def.members.size() * 100 / (double) nodes;
                            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                            pctTotal += pct;
                            cnodes += def.members.size();
                            clustersData.add(new ClustersTableData(def.num, "Cluster " + def.num, def.members.size(), pct));
                            for(int idx : def.members) {
                                System.out.println("defMbrIdx: " + idx);
                                sd = data.lstStats.get(idx);
                                nodesData.add(new NodesTableData(def.num, sd.getFeature(), sd.getDescription(), sd.getCategory(), sd.getAdjustedPV()));
                            }
                        }
                        pctTotal = Double.parseDouble(String.format("%.2f", ((double)Math.round(pctTotal*100)/100.0)));
                        clustersInfo = "Clusters: " + cd.clusters.size() + "  Clustered Nodes: " + cnodes + " (" + pctTotal + "% of " + nodes + ")";

                        ArrayList<ClusterGSEA.ClusterDef> lstClusters = cd.clusters;
                        String memberType = itemsName;

                        boolean clustersOnly = true;
                        ArrayList<GraphLink> lstLinks = new  ArrayList();
                        System.out.println("links: " + lstLinks.size());
                        for(ClusterGSEA.ClusterDef def : cd.clusters) {
                            ArrayList<String> lstCT = new ArrayList<>();
                            for(int i = 0; i < def.members.size(); i++)
                                lstCT.add(data.lstStats.get(def.members.get(i)).getFeature());
                            lstClusterTerms.add(lstCT);
                        }

                        // build graph html file
                        clustersHTML = buildHTML(memberType, lstLinks, data.lstStats, cd.clusters, cd.clusterNums, clustersOnly);
                    }*/

                    // assume everything is OK - change to use flag if needed
                    appendLogLine("Saving network...", logfilepath);
                    writeClusterResultsFiles(clustersData, nodesData, linksData, clustersHTML);
                    if(readClusterResultsFiles())
                        loaded = true;
                    appendLogLine("Network complete.", logfilepath);
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("GSEA Clustering", task);
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
        String subTabs = TabProjectDataViz.Panels.CLUSTERSGSEA.toString() + id + ";";
        subTabs += TabProjectDataViz.Panels.CLUSTERSGOGSEA.toString() + id;
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
        public final SimpleDoubleProperty pvalue2;
 
        public NodesTableData(int clusterId, String id, String name, String cat, double pvalue, double pvalue2) {
            this.clusterId = new SimpleIntegerProperty(clusterId);
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.cat = new SimpleStringProperty(cat);
            this.pvalue = new SimpleDoubleProperty(pvalue);
            this.pvalue2 = new SimpleDoubleProperty(pvalue2);
        }
        public NodesTableData(String tsv) throws Exception {
            String[] fields;
            fields = tsv.split("\t");
            if(fields.length == 6) {
                this.clusterId = new SimpleIntegerProperty(Integer.parseInt(fields[0]));
                this.id = new SimpleStringProperty(fields[1].trim());
                this.name = new SimpleStringProperty(fields[2].trim());
                this.cat = new SimpleStringProperty(fields[3].trim());
                this.pvalue = new SimpleDoubleProperty(Double.parseDouble(fields[4]));
                this.pvalue2 = new SimpleDoubleProperty(Double.parseDouble(fields[5]));
            }
            else
                throw new Exception("Invalid NodesTableData values.");
        }
        public String toTSV() {
            String line = clusterId.get() + "\t" + id.get() + "\t" + name.get() + "\t" + cat.get() + "\t" + pvalue.get() + "\t" + pvalue2.get();
            return line;
        }
        public Integer getClusterID() { return clusterId.get(); }
        public String getID() { return id.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return cat.get(); }
        public Double getPValue() { return pvalue.get(); }
        public Double getPValue2() { return pvalue2.get(); }

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
 
        public LinksTableData(int clusterId, String id1, String name1, String id2, String name2, int shared) {
            this.clusterId = new SimpleIntegerProperty(clusterId);
            this.id1 = new SimpleStringProperty(id1);
            this.name1 = new SimpleStringProperty(name1);
            this.id2 = new SimpleStringProperty(id2);
            this.name2 = new SimpleStringProperty(name2);
            this.shared = new SimpleIntegerProperty(shared);
        }
        public String toTSV() {
            String line = clusterId.get() + "\t" + id1.get() + "\t" + name1.get() + "\t" + id2.get() + "\t" + name2.get() + "\t" + shared.get();
            return line;
        }
        public LinksTableData(String tsv) throws Exception {
            String[] fields;
            fields = tsv.split("\t");
            if(fields.length == 6) {
                this.clusterId = new SimpleIntegerProperty(Integer.parseInt(fields[0]));
                this.id1 = new SimpleStringProperty(fields[1].trim());
                this.name1 = new SimpleStringProperty(fields[2].trim());
                this.id2 = new SimpleStringProperty(fields[3].trim());
                this.name2 = new SimpleStringProperty(fields[4].trim());
                this.shared = new SimpleIntegerProperty(Integer.parseInt(fields[5]));
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
        ArrayList<Integer> srcClusters, dstClusters;
        public GraphLink(int src, int dst, ArrayList<Integer> srcClusters, ArrayList<Integer> dstClusters, String type, int shared) {
            this.src = src;
            this.dst = dst;
            this.srcClusters = srcClusters;
            this.dstClusters = dstClusters;
            this.type = type;
            this.shared = shared;
        }
    }
}
