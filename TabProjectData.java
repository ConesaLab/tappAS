/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import tappas.DataApp.DataType;

import java.nio.file.Path;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TabProjectData extends TabBase {
    static final String DATA_GROUP = "Project Data";
    static final String DV_GROUP = "Diversity Analysis";
    static final String EM_GROUP = "Expression Matrix";
    static final String DA_GROUP = "Differential Analysis";
    static final String EA_GROUP = "Enrichment Analysis";
    static final String AF_GROUP = "Annotation Features";
    // WARNING: You must keep this in sync with subTabPlus if needed
    public static enum Panels { PROPS, TRANS, PROTEIN, GENE, EXPMATRIX, DARESULTS, FAIDCOMBINED,
                  STATSDIUTRANS, STATSDIUPROT, STATSDEATRANS, STATSDEAPROT, STATSDEAGENE, 
                  STATSDPA, STATSUTRL, FIRESULTSSUMMARY, FIASSOCIATION,
                  // dynamically added, multiple IDs sub tabs
                  STATSFDA, STATSFDASUMMARY, STATSDFI, STATSGSEA, CLUSTERSGSEA, STATSFEA, CLUSTERSFEA }
    SubTabBase.SubTabInfo _subTabsInfo[] = {
        // subTabId, resId, group, title, tooltip, menuName, imgName, description
        new SubTabBase.SubTabInfo(Panels.PROPS.name(), "ProjectProps", "Project Properties", "", "Project Properties", "Project Properties", "Project Properties", "definition.png", "Project properties"),
        new SubTabBase.SubTabInfo(Panels.TRANS.name(), "ProjectData", "Project Data", "", "Transcripts", "Project Transcripts", "Project Data", "data.png", "Transcripts in project"),
        new SubTabBase.SubTabInfo(Panels.PROTEIN.name(), "ProjectData", "Project Data", "", "Proteins", "Project Proteins", "Project Data", "data.png", "Proteins in project"),
        new SubTabBase.SubTabInfo(Panels.GENE.name(), "ProjectData", "Project Data", "", "Genes", "Project Genes", "Project Data", "data.png", "Genes in project"),
        new SubTabBase.SubTabInfo(Panels.EXPMATRIX.name(), "ExpMatrix", EM_GROUP, "", "Expression Matrix", "Expression Matrix", "Expression Matrix", "data_matrix.png", "Expression matrix data"),
        new SubTabBase.SubTabInfo(Panels.STATSFDA.name(), "FDAResults", DV_GROUP, "", "FDA Results", "Functional Diversity Analysis Statistical Results for Genes", "Miscellaneous Analysis", "fda.png", "Functional Diversity Analysis statistical results"),
        new SubTabBase.SubTabInfo(Panels.STATSFDASUMMARY.name(), "FDASummary", DV_GROUP, "", "FDA Summary", "Functional Diversity Analysis Summary", "Miscellaneous Analysis", "fda.png", "Functional Diversity Analysis Summary"),
        new SubTabBase.SubTabInfo(Panels.STATSDFI.name(), "DFIResults", AF_GROUP, "", "DFI Results", "Differential Feature Inclusion Analysis Statistical Results", "Differential Feature Inclusion Analysis", "fa.png", "Differential Feature Inclusion Analysis statistical results"),
        new SubTabBase.SubTabInfo(Panels.STATSDPA.name(), "DPAResults", AF_GROUP, "", "DPA Results", "Differential PolyAdenylation Analysis Statistical Results", "Differential PolyAdenylation Analysis", "fa.png", "Differential PolyAdenylation Analysis statistical results"),
        new SubTabBase.SubTabInfo(Panels.STATSUTRL.name(), "UTRLResults", AF_GROUP, "", "UTRL Results", "UTR Lengthening Analysis statistical results", "UTR Lengthening Analysis", "fa.png", "UTR Lengthening Analysis statistical results"),
        new SubTabBase.SubTabInfo(Panels.FIRESULTSSUMMARY.name(), "DFIResultsSummary", AF_GROUP, "", "DFI Results Summary", "Differential Feature Inclusion Results Summary", "Differential Feature Inclusion Analysis", "fa.png", "Differential Feature Inclusion Analysis results summary"),
        new SubTabBase.SubTabInfo(Panels.FIASSOCIATION.name(), "DFIAssociation", AF_GROUP, "", "Co-DFI Associations", "Co-Differential Feature Inclusion Analysis Associations", "Differential Feature Inclusion Analysis", "fa.png", "Co-Differential Feature Inclusion Analysis gene associations"),
        new SubTabBase.SubTabInfo(Panels.DARESULTS.name(), "DAResults", DA_GROUP, "", "Combined Results", "Differential Analysis Combined Results", "Differential Analysis", "da.png", "Differential Analysis combined results"),
        new SubTabBase.SubTabInfo(Panels.FAIDCOMBINED.name(), "FAResults", AF_GROUP, "", "FDA Combined Results", "Functional Analysis Combined Results", "Functional Analysis", "fda.png", "Functional Diversity Analysis combined results"),
         new SubTabBase.SubTabInfo(Panels.STATSDIUTRANS.name(), "DIUResults", DA_GROUP, "", "DIU Transcripts", "Differential Isoform Usage Analysis Statistical Results for Transcripts", "Differential Analysis", "da.png", "Differential Isoform Usage Analysis statistical results for transcripts"),
        new SubTabBase.SubTabInfo(Panels.STATSDIUPROT.name(), "DIUResults", DA_GROUP, "", "DIU Proteins", "Differential Isoform Usage Analysis Statistical Results for Proteins", "Differential Analysis", "da.png", "Differential Isoform Usage Analysis statistical results for proteins"),
        new SubTabBase.SubTabInfo(Panels.STATSDEATRANS.name(), "DEAResults", DA_GROUP, "", "DEA Transcripts", "Differential Expression Analysis Statistical Results for Transcripts", "Differential Analysis", "da.png", "Differential Expression Analysis statistical results for transcripts"),
        new SubTabBase.SubTabInfo(Panels.STATSDEAPROT.name(), "DEAResults", DA_GROUP, "", "DEA Proteins", "Differential Expression Analysis Statistical Results for Proteins", "Differential Analysis", "da.png", "Differential Expression Analysis statistical results for proteins"),
        new SubTabBase.SubTabInfo(Panels.STATSDEAGENE.name(), "DEAResults", DA_GROUP, "", "DEA Genes", "Differential Expression Analysis Statistical Results for Genes", "Differential Analysis", "da.png", "Differential Expression Analysis statistical results for genes")
    };
    TabBase.TabDef _tabDef = new TabBase.TabDef("PD", "Project Data", "Project Data", "Project data", "Project Data: ", "dataTab.png", _subTabsInfo);

    Chromosome.ChromoComparator chromoComparator = new Chromosome.ChromoComparator();
    Path runScriptPath;

    public TabProjectData(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(String tabId, TabPane tpParent, HashMap<String, Object> args) {
        super.initialize(tabId, tpParent, args);
        for(String key : args.keySet()) {
            Object obj = args.get(key);
            if(obj.getClass().equals(String.class))
                params.put(key, (String)obj);
        }
        setTitle();
        
        // set special button requests
        boolean result = initializeTab(_tabDef);
        if(result)
        {
            // determine which data panel(s) to open
            if(!openRequestedPanels())
                openSubTab(tabDef.subTabsInfo[Panels.TRANS.ordinal()], true);
        }
        return result;
    }
    private void setTitle() {
        _tabDef.title = "\"" + getShortName(project.getProjectName()) + "\"";
        _tabDef.tooltip = "Data for Project: " + project.getProjectName();
    }
    
    @Override
    public SubTabBase createSubTabInstance(SubTabBase.SubTabInfo info) {
        SubTabBase sub = null;
        HashMap<String, Object> subArgs = new HashMap<>();
        for(String key : args.keySet())
            subArgs.put(key, args.get(key));
        try {
            if(info.subTabId.startsWith(Panels.CLUSTERSGSEA.name()))
                sub = new SubTabGSEAClusters(project);
            else if(info.subTabId.startsWith(Panels.STATSGSEA.name()))
                sub = new SubTabGSEAResults(project);
            else if(info.subTabId.startsWith(Panels.STATSFEA.name()))
                sub = new SubTabFEAResults(project);
            else if(info.subTabId.startsWith(Panels.CLUSTERSFEA.name()))
                sub = new SubTabFEAClusters(project);
            else if(info.subTabId.startsWith(Panels.STATSFDASUMMARY.name()))
                sub = new SubTabFDAResults(project);
            else if(info.subTabId.startsWith(Panels.STATSFDA.name()))
                sub = new SubTabFDAResults(project);
            //dont chose DFI when DFISummary - order is important
            else if(info.subTabId.startsWith(Panels.FIRESULTSSUMMARY.name()))
                sub = new SubTabDFIResultsSummary(project);
            else if(info.subTabId.startsWith(Panels.FIASSOCIATION.name()))
                sub = new SubTabDFIAssociation(project);
            else if(info.subTabId.startsWith(Panels.STATSDFI.name()))
                sub = new SubTabDFIResults(project);
            else {
                Panels panel = Panels.valueOf(info.subTabId);
                switch(panel) {
                    case PROPS:
                        subArgs.put("params", params);
                        sub = new SubTabProjectProps(project);
                        break;
                    case TRANS:
                        subArgs.put("dataType", DataType.TRANS);
                        sub = new SubTabData(project);
                        break;
                    case PROTEIN:
                        subArgs.put("dataType", DataType.PROTEIN);
                        sub = new SubTabData(project);
                        break;
                    case GENE:
                        subArgs.put("dataType", DataType.GENE);
                        sub = new SubTabData(project);
                        break;
                    case EXPMATRIX:
                        sub = new SubTabExpMatrix(project);
                        break;
                    case DARESULTS:
                        sub = new SubTabDACombinedResults(project);
                        break;
                    case FAIDCOMBINED:
                        sub = new SubTabFACombinedResults(project);
                        break;
                    case STATSDIUTRANS:
                        subArgs.put("dataType", DataType.TRANS);
                        sub = new SubTabDIUResults(project);
                        break;
                    case STATSDIUPROT:
                        subArgs.put("dataType", DataType.PROTEIN);
                        sub = new SubTabDIUResults(project);
                        break;
                    case STATSDEATRANS:
                        subArgs.put("dataType", DataType.TRANS);
                        sub = new SubTabDEAResults(project);
                        break;
                    case STATSDEAPROT:
                        subArgs.put("dataType", DataType.PROTEIN);
                        sub = new SubTabDEAResults(project);
                        break;
                    case STATSDEAGENE:
                        subArgs.put("dataType", DataType.GENE);
                        sub = new SubTabDEAResults(project);
                        break;
//                    case STATSDFI:
//                        sub = new SubTabDFIResults(project);
//                        break;
//                    case STATSDFIRESULTSSUMMARY:
//                        sub = new SubTabDFIResultsSummary(project);
//                        break;
//                    case STATSDFIASSOCIATION:
//                        sub = new SubTabDFIAssociation(project);
//                        break;
//                    case STATSFDA:
//                        sub = new SubTabFDAResults(project);
//                        break;
                    case STATSDPA:
                        sub = new SubTabDPAResults(project);
                        break;
                    case STATSUTRL:
                        sub = new SubTabUTRLResults(project);
                        break;
                    default:
                        break;
                }
            }
        }
        catch(Exception e) { 
            logger.logError("Internal program error: " + e.getMessage());
        }
        if(sub != null) {
            if(!sub.initialize(tbObj, info, subArgs))
                sub = null;
        }
        return sub;
    }
    @Override
    protected void setupSpecialPanel(String panelId) {
        SubTabBase.SubTabInfo panelInfo = null;
        if(panelId.startsWith(Panels.CLUSTERSGSEA.name())) {
            panelInfo = getPanelInfo(panelId);
            if (panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "GSEAClusters", "GSEA Clusters", "", "GSEA Clusters", "GSEA enriched multidimensional features", "", "clusters.png", "GSEA enriched multidimensional features");
        }else if(panelId.startsWith(Panels.STATSGSEA.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "GSEAResults", EA_GROUP, "", "GSEA", "Gene Set Enrichment Analysis statistical results", "", "ea.png", "Gene Set Enrichment Analysis Statistical Results");
        }else if(panelId.startsWith(Panels.STATSFEA.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FEAResults", EA_GROUP, "", "FEA", "Functional Enrichment Analysis statistical results", "", "ea.png", "Functional Enrichment Analysis Statistical Results");
        }else if(panelId.startsWith(Panels.CLUSTERSFEA.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FEAClusters", "FEA Clusters", "", "FEA Clusters", "FEA enriched features clusters", "", "clusters.png", "FEA Enriched Features Clusters");
        }else if(panelId.startsWith(Panels.STATSFDASUMMARY.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FDASummary", DV_GROUP, "", "FDA Summary", "Functional Diversity Analysis Summary", "", "fda.png", "Functional Diversity Summary");
        }else if(panelId.startsWith(Panels.STATSFDA.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FDAResults", DV_GROUP, "", "FDA", "Functional Diversity Analysis results", "", "fda.png", "Functional Diversity Results");
        }else if(panelId.startsWith(Panels.FIASSOCIATION.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DFIResultsAssociation", AF_GROUP, "", "DFI Association", "Differential Feature Inclusion Association", "", "fa.png", "Differential Feature Inclusion Associations");
        }else if(panelId.startsWith(Panels.FIRESULTSSUMMARY.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DFIResultsSummary", AF_GROUP, "", "DFI Results Summary", "Differential Feature Inclusion Results Summary", "", "fa.png", "Differential Feature Inclusion Results Summary");
        }else if(panelId.startsWith(Panels.STATSDFI.name())) {
            panelInfo = getPanelInfo(panelId);
            if(panelInfo != null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DFIResults", AF_GROUP, "", "DFI", "Differential Feature Inclusion Results", "", "fa.png", "Differential Feature Inclusion Results");
        }

        if(panelInfo != null) {
            int cnt = tabDef.subTabsInfo.length + 1;
            SubTabBase.SubTabInfo[] sti = new SubTabBase.SubTabInfo[cnt];
            for(int i = 0; i < (cnt-1); i++)
                sti[i] = tabDef.subTabsInfo[i];
            sti[cnt-1] = panelInfo;
            tabDef.subTabsInfo = sti;
        }
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("panels")) {
            // this is not clean - we need args set since createSubTabInstance will be called
            // and it needs the arguments passed. However, we do not want to keep them or it 
            // will mess up future calls with stuff like the "run" parameter (no longer using run)
            HashMap<String, Object> savedArgs = args;
            args = (HashMap)hm.get("panels");
            HashMap<String, Object> reqArgs = (HashMap)hm.get("panels");
            // check if it is one  of the Id tabs, see if already there and if not add to subtabsinfo array
            String[] panels = ((String)reqArgs.get("panels")).split(";");
            for(String panelName : panels) {
                String featureId = (String) reqArgs.get("id");
                String id = panelName + featureId;
                SubTabBase.SubTabInfo panelInfo = null;
                if(panelName.equals(Panels.CLUSTERSGSEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "GSEAClusters", "GSEA Clusters", "", "GSEA Clusters", "GSEA enriched multidimensional features", "", "cluster.png", "GSEA enriched multidimensional features");
                }
                else if(panelName.equals(Panels.STATSGSEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "GSEAResults", "Enrichment Analysis", "", "GSEA", "Gene Set Enrichment Analysis statistical results", "", "ea.png", "Gene Set Enrichment Analysis Statistical Results");
                }
                else if(panelName.equals(Panels.STATSFEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "FEAResults", "Enrichment Analysis", "", "FEA", "Functional Enrichment Analysis statistical results", "", "ea.png", "Functional Enrichment Analysis Statistical Results");
                }
                else if(panelName.equals(Panels.CLUSTERSFEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "FEAClusters", "FEA Clusters", "", "FEA Clusters", "FEA enriched features clusters", "", "cluster.png", "FEA Enriched Features Clusters");
                }else if(panelName.equals(Panels.STATSFDA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "FDAResults", "Functional Diversity Analysis", "", "FDA", "Functional Diversity Analysis Results", "", "fda.png", "Functional Diversity Analysis Results");
                }else if(panelName.equals(Panels.FIASSOCIATION.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DFIAssociation", "Differential Feature Inclusion Associations", "", "Co-DFI Association", "Co- Differential Feature Inclusion Associations", "", "fa.png", "Co-Differential Feature Inclusion Associations");
                }else if(panelName.equals(Panels.FIRESULTSSUMMARY.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DFIResultsSummary", "Differential Feature Inclusion Restults Summary", "", "DFI Results Summary", "Differential Feature Inclusion Results Summary", "", "fa.png", "Differential Feature Inclusion Results Summary");
                }else if(panelName.equals(Panels.STATSDFI.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DFIResults", "Differential Feature Inclusion Analysis", "", "DFI", "Differential Feature Inclusion Results", "", "fa.png", "Differential Feature Inclusion Results");
                }
                else
                    openPanels(panelName);
                
                if(panelInfo != null) {
                    int cnt = tabDef.subTabsInfo.length + 1;
                    SubTabBase.SubTabInfo[] sti = new SubTabBase.SubTabInfo[cnt];
                    for(int i = 0; i < (cnt-1); i++)
                        sti[i] = tabDef.subTabsInfo[i];
                    sti[cnt-1] = panelInfo;
                    tabDef.subTabsInfo = sti;
                    openPanels(id);
                }
            }
            args = savedArgs;
        }
        else if(hm.containsKey("closePanels"))
            closePanels((String)hm.get("closePanels"));
        else if(hm.containsKey("updateDEACombinedResults")) {
            if(tabDef.subTabsInfo[Panels.DARESULTS.ordinal()].subTabBase != null)
                obj = tabDef.subTabsInfo[Panels.DARESULTS.ordinal()].subTabBase.processRequest(hm);
            TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDV + project.getDef().id);
            if(tb != null) {
                if(hm.get("updateDEACombinedResults").equals("DIU")) {
                    HashMap<String, Object> hmdiu = new HashMap<>();
                    hmdiu.put("updateDIUResults", "");
                    tb.processRequest(hmdiu);
                }
                else if(((String)hm.get("updateDEACombinedResults")).startsWith("DEA")) {
                    HashMap<String, Object> hmdea = new HashMap<>();
                    hmdea.put("updateDEAResults", hm.get("updateDEACombinedResults"));
                    tb.processRequest(hmdea);
                }
            }
        }
        else if(hm.containsKey("updateEACombinedResults")) {
            TabBase tb = tabs.getTabBase(Tabs.TAB_PROJECTDV + project.getDef().id);
            if(tb != null) {
                if(hm.get("updateEACombinedResults").equals("FEA")) {
                    HashMap<String, Object> hmdiu = new HashMap<>();
                    hmdiu.put("updateFEAResults", hm.get("featureId"));
                    tb.processRequest(hmdiu);
                }
            }
        }
        else if(hm.containsKey("updateProjectName")) {
            setTitle();
            updateTabTitle();
        }
        return obj;
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        if(obj != null) {
            for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
                for(TableView tb : info.lstSearchTables) {
                    if(tb.equals(obj)) {
                        info.subTabBase.search(obj, txt, hide);
                    }
                }
            }
        }
    }
}
