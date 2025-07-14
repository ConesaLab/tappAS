/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import tappas.DataApp.DataType;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TabProjectDataViz extends TabBase {
    // WARNING: You must keep this in sync with subTabPlus if needed
    public static enum Panels {
                  //Summary All
                  SUMMARYALL, SUMMARYTRANS, SUMMARYPROT, SUMMARYGENE, SUMMARYEXPMATRIX, SUMMARYANNOTATION,
                  DPERGENE, DPERCHROMO, SDTRANS, SDTRANSLENGTHS, SDEXONS,
                  //FDA
                  SUMMARYFDA, SUMMARYFDACOMBINEDRESULTS,
                  //DEA
                  SUMMARYDEATRANS, SUMMARYDEAPROT, SUMMARYDEAGENE,
                  CLUSTERSDEATRANS, CLUSTERSDEAPROT, CLUSTERSDEAGENE,
                  VENNDIAGDEATRANS, VENNDIAGDEAPROT, VENNDIAGDEAGENE,
                  //DIU
                  SUMMARYDIUTRANS, SUMMARYDIUPROT,
                  //FDI
                  SUMMARYDFI,
                  //DPA
                  SUMMARYDPAGENE, CLUSTERSDPA,
                  VENNDIAGDPA,
                  //UTR Lengthening
                  SUMMARYUTRL, CLUSTERSUTRL,
                  //FEA
                  SUMMARYFEA, ENRICHEDFEA, CLUSTERSFEA, CLUSTERSGOFEA,
                  //GSEA
                  SUMMARYGSEA, ENRICHEDGSEA, CLUSTERSGSEA, CLUSTERSGOGSEA
    };

    // static templates, each instance gets a copy and the static one is used by dlgSelectGraph
    static SubTabBase.SubTabInfo static_subTabsInfo[] = {
        // subTabId, resId, group, title, tooltip, menuName, imgName, description
        new SubTabBase.SubTabInfo(Panels.SUMMARYALL.name(), "AllSummary", "Project Data", "Transcripts;Proteins;Genes", "General Summary", "Project Data Summary", "Project Data", "sumData.png", "Summary information for project data"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYTRANS.name(), "TransSummary", "Project Data", "Transcripts", "Transcripts Summary", "Transcripts Summary", "Project Data", "sumData.png", "Summary information for project transcripts"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYPROT.name(), "ProteinSummary", "Project Data", "Proteins", "Proteins Summary", "Proteins Summary", "Project Data", "sumData.png", "Summary information for project proteins"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYGENE.name(), "GeneSummary", "Project Data", "Genes", "Genes Summary", "Genes Summary", "Project Data", "sumData.png", "Summary information for project genes"),
        new SubTabBase.SubTabInfo(Panels.DPERGENE.name(), "BarChart", "Project Data", "Transcripts;Proteins;Genes", "Distribution Per Gene", "Distribution per Gene", "Project Data", "distribution.png", "Distribution per gene for project transcripts and proteins"),
        new SubTabBase.SubTabInfo(Panels.DPERCHROMO.name(), "BarChart", "Project Data", "Transcripts;Proteins;Genes", "Transcripts, Protein, and Genes Per Chromo", "Transcripts, Protein, and Genes per Chromosome", "Project Data", "distribution.png", "Transcripts, proteins, and genes per chromosome"),
        new SubTabBase.SubTabInfo(Panels.SDTRANS.name(), "BarChart", "Project Data", "Transcripts", "Transcripts Per Structural Category", "Transcripts per Structural Category", "Project Data", "distribution.png", "Transcripts per structural category"),
        new SubTabBase.SubTabInfo(Panels.SDTRANSLENGTHS.name(), "BarChart", "Project Data", "Transcripts", "Transcript Lengths Distribution", "Transcript Lengths Distribution per Structural Category", "Project Data", "boxplot.png", "Transcript lengths distribution per structural category"),
        new SubTabBase.SubTabInfo(Panels.SDEXONS.name(), "BarChart", "Project Data", "Transcripts", "Transcript Exons Distribution", "Number of Transcript Exons Distribution per Structural Category", "Project Data", "boxplot.png", "Number of transcript exons distribution per structural category"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYEXPMATRIX.name(), "ExpMatrixSummary", TabProjectData.EM_GROUP, "ExpressionMatrix", "Expression Matrix Summary", "Expression Matrix Summary", "Project Data", "sumData.png", "Summary information for input expression matrix"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYANNOTATION.name(), "AnnotationSummary", TabProjectData.EM_GROUP, "Features;Transcripts;Proteins;Genes", "Annotation Summary", "Annotation Summary", "Project Data", "sumData.png", "Summary Information for Project Annotation Features"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYDPAGENE.name(), "DPAGeneSummary", TabProjectData.DA_GROUP, "DPA", "DPA Summary", "Summary for Differential PolyAdenylation Analysis", "Differential Analysis", "sumData.png", "Summary for Differential PolyAdenylation Analysis"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYUTRL.name(), "UTRLSummary", TabProjectData.DA_GROUP, "UTRL", "UTRL Summary", "Summary for UTR Lengthening Analysis", "Lengthening Analysis", "sumData.png", "Summary for UTR Lengthening Analysis"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYDIUTRANS.name(), "DIUSummary", TabProjectData.DA_GROUP, "DIU", "DIU Transcript Summary", "Summary for Transcript Differential Isoform Usage Analysis", "Differential Analysis", "sumData.png", "Summary for Transcript Differential Isoform Usage Analysis"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYDIUPROT.name(), "DIUSummary", TabProjectData.DA_GROUP, "DIU", "DIU Protein Summary", "Summary for Protein Differential Isoform Usage Analysis", "Differential Analysis", "sumData.png", "Summary for Protein Differential Isoform Usage Analysis")
        //new SubTabBase.SubTabInfo(Panels.VENNDIAGDEATRANS.name(), "DEAVennDiag", TabProjectData.DA_GROUP, "DEATranscripts", "DEA Transcripts Venn Diagram", "Venn Diagram for Transcripts Differential Expression Analysis", "Differential Analysis", "sumData.png", "Venn Diagram for transcripts DEA"),
        //new SubTabBase.SubTabInfo(Panels.VENNDIAGDEAPROT.name(), "DEAVennDiag", TabProjectData.DA_GROUP, "DEAProteins", "DEA Proteins Venn Diagram", "Venn Diagram for Proteins Differential Expression Analysis", "Differential Analysis", "sumData.png", "Venn Diagram for proteins DEA"),
        //new SubTabBase.SubTabInfo(Panels.VENNDIAGDEAGENE.name(), "DEAVennDiag", TabProjectData.DA_GROUP, "DEAGenes", "DEA Genes Venn Diagram", "Venn Diagram for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Venn Diagram for genes DEA"),
        //new SubTabBase.SubTabInfo(Panels.VENNDIAGDPA.name(), "DPAVennDiag", TabProjectData.DA_GROUP, "DPA", "DPA Genes Venn Diagram", "Venn Diagram for Genes Differential PolyAdenylation", "Differential Analysis", "sumData.png", "Venn Diagram for genes DPA")
    };
    static SubTabBase.SubTabInfo static_subTabsDynamicInfo[] = {
        // subTabId, resId, group, title, tooltip, menuName, imgName, description
        new SubTabBase.SubTabInfo(Panels.SUMMARYFEA.name(), "FEASummary", TabProjectData.EA_GROUP, "FEA", "FEA", "FEA Summary", "", "sumData.png", "FEA Summary"),
        new SubTabBase.SubTabInfo(Panels.ENRICHEDFEA.name(), "ScatterChartScroll", TabProjectData.EA_GROUP, "FEA", "FEA Enriched Features", "FEA Enriched", "", "sumData.png", "FEA Enriched Features"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYGSEA.name(), "GSEASummary", TabProjectData.EA_GROUP, "GSEA", "GSEA", "GSEA Summary", "", "sumData.png", "GSEA Summary"),
        new SubTabBase.SubTabInfo(Panels.ENRICHEDGSEA.name(), "ScatterChartScroll", TabProjectData.EA_GROUP, "GSEA", "GSEA Enriched Features", "GSEA Enriched", "", "sumData.png", "GSEA Enriched Features"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYDEATRANS.name(), "DEATransSummary", TabProjectData.DA_GROUP, "DEATranscripts", "DEA Transcripts Summary", "Summary for Transcripts Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for transcripts DEA"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYDEAPROT.name(), "DEAProteinSummary", TabProjectData.DA_GROUP, "DEAProteins", "DEA Proteins Summary", "Summary for Proteins Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for proteins DEA"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYDEAGENE.name(), "DEAGeneSummary", TabProjectData.DA_GROUP, "DEAGenes", "DEA Genes Summary", "Summary for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for genes DEA"),
        new SubTabBase.SubTabInfo(Panels.CLUSTERSDEATRANS.name(), "DEAClusters", TabProjectData.DA_GROUP, "DEATranscripts", "DEA Transcripts Cluster Profiles", "Expression Profile Clusters for Transcripts Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for transcripts DEA"),
        new SubTabBase.SubTabInfo(Panels.CLUSTERSDEAPROT.name(), "DEAClusters", TabProjectData.DA_GROUP, "DEAProteins", "DEA Proteins Cluster Profiles", "Cluster Expression Profiles for Proteins Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for proteins DEA"),
        new SubTabBase.SubTabInfo(Panels.CLUSTERSDEAGENE.name(), "DEAClusters", TabProjectData.DA_GROUP, "DEAGenes", "DEA Genes Cluster Profiles", "Cluster Expression Profiles for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for genes DEA"),
        new SubTabBase.SubTabInfo(Panels.CLUSTERSDEAGENE.name(), "DPAClusters", TabProjectData.DA_GROUP, "DPAGenes", "DPA Genes Cluster Profiles", "Cluster Expression Profiles for Genes Differential PolyAdenylation Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for genes DPA"),

        new SubTabBase.SubTabInfo(Panels.SUMMARYFDA.name(), "FDASummary", TabProjectData.AF_GROUP, "FDA", "FDA Summary", "Functional Diversity", "Project Data", "sumData.png", "Summary for functional diversity analysis"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYFDACOMBINEDRESULTS.name(), "FDASummaryCombinedResults", TabProjectData.AF_GROUP, "FDA", "FDA Summary Combined Results", "Functional Diversity", "Project Data", "sumData.png", "Summary for functional diversity analysis combined results"),
        new SubTabBase.SubTabInfo(Panels.SUMMARYDFI.name(), "DFISummary", TabProjectData.AF_GROUP, "DFI", "DFI Summary", "Summary for Differential Feature Inclusion Analysis", "Differential Analysis", "sumData.png", "Summary for Differential Feature Inclusion Analysis")

    };
    SubTabBase.SubTabInfo _subTabsInfo[] = getSubTabInfo();
    SubTabBase.SubTabInfo _subTabsDynamicInfo[] = getSubTabDynamicInfo();
    TabBase.TabDef _tabDef = new TabBase.TabDef("PDV", "Project Data Visualization", "Project Data Visualization", "Project Data Visualization", "Project Data Visualization: ", "dataVizTab.png", _subTabsInfo);

    public TabProjectDataViz(Project project) {
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

        boolean result = initializeTab(_tabDef);
        if(result)
        {
            // always open the definition page
            if(project.data.hasResults()) {
                // determine which data panel(s) to open
                if(!openRequestedPanels())
                    openSubTab(tabDef.subTabsInfo[Panels.SUMMARYALL.ordinal()], true);
            }
            else
                result = false;
        }
        return result;
    }
    private void setTitle() {
        _tabDef.title = "\"" + getShortName(project.getProjectName()) + "\"";
        _tabDef.tooltip = "Graphs and Charts for Project: " + project.getProjectName();
    }
    
    @Override
    public SubTabBase createSubTabInstance(SubTabBase.SubTabInfo info) {
        SubTabBase sub = null;
        HashMap<String, Object> subArgs = new HashMap<>();
        for(String key : args.keySet())
            subArgs.put(key, args.get(key));
        try {
            if(info.subTabId.startsWith(Panels.SUMMARYFEA.name()))
                sub = new SubTabFEASummary(project);
            else if(info.subTabId.startsWith(Panels.SUMMARYDFI.name()))
                sub = new SubTabDFISummary(project);
            else if(info.subTabId.startsWith(Panels.SUMMARYUTRL.name()))
                sub = new SubTabUTRLSummary(project);
            else if(info.subTabId.startsWith(Panels.ENRICHEDFEA.name()))
                sub = new SubTabFEAEnriched(project);
            else if(info.subTabId.startsWith(Panels.CLUSTERSFEA.name()))
                sub = new SubTabFEAClustersGraph(project);
            else if(info.subTabId.startsWith(Panels.CLUSTERSGOFEA.name()))
                sub = new SubTabFEAClustersGO(project);
            else if(info.subTabId.startsWith(Panels.ENRICHEDGSEA.name()))
                sub = new SubTabGSEAEnriched(project);
            else if(info.subTabId.startsWith(Panels.CLUSTERSGSEA.name()))
                sub = new SubTabGSEAClustersGraph(project);
            else if(info.subTabId.startsWith(Panels.CLUSTERSGOGSEA.name()))
                sub = new SubTabGSEAClustersGO(project);
            else if(info.subTabId.startsWith(Panels.SUMMARYGSEA.name()))
                sub = new SubTabGSEASummary(project);
            else if(info.subTabId.startsWith(Panels.SUMMARYGSEA.name()))
                sub = new SubTabGSEASummary(project);
            else if(info.subTabId.startsWith(Panels.SUMMARYDEAGENE.name())) {
                subArgs.put("dataType", DataType.GENE);
                sub = new SubTabDEAGeneSummary(project);
            }
            else if(info.subTabId.startsWith(Panels.SUMMARYDEAPROT.name())) {
                subArgs.put("dataType", DataType.PROTEIN);
                sub = new SubTabDEAProteinSummary(project);
            }
            else if(info.subTabId.startsWith(Panels.SUMMARYDEATRANS.name())) {
                subArgs.put("dataType", DataType.TRANS);
                sub = new SubTabDEATransSummary(project);
            }
            else if(info.subTabId.startsWith(Panels.SUMMARYDPAGENE.name())) {
                subArgs.put("dataType", DataType.GENE);
                sub = new SubTabDPAGeneSummary(project);
            }
            else if(info.subTabId.startsWith(Panels.CLUSTERSDEAGENE.name())) {
                subArgs.put("dataType", DataType.GENE);
                sub = new SubTabDEAClusters(project);
            }
            else if(info.subTabId.startsWith(Panels.CLUSTERSDEAPROT.name())) {
                subArgs.put("dataType", DataType.PROTEIN);
                sub = new SubTabDEAClusters(project);
            }
            else if(info.subTabId.startsWith(Panels.CLUSTERSDEATRANS.name())) {
                subArgs.put("dataType", DataType.TRANS);
                sub = new SubTabDEAClusters(project);
            }
            else if(info.subTabId.startsWith(Panels.CLUSTERSDPA.name())) {
                subArgs.put("dataType", DataType.GENE);
                sub = new SubTabDPAClusters(project);
            }else if(info.subTabId.startsWith(Panels.CLUSTERSUTRL.name())) {
                subArgs.put("dataType", DataType.GENE);
                sub = new SubTabUTRLClusters(project);
            }
            else {
                Panels panel = Panels.valueOf(info.subTabId);
                switch(panel) {
                    case SUMMARYALL:
                        sub = new SubTabDataAllSummary(project);
                        break;
                    case SUMMARYTRANS:
                        sub = new SubTabDataTransSummary(project);
                        break;
                    case SUMMARYPROT:
                        sub = new SubTabDataProteinSummary(project);
                        break;
                    case SUMMARYGENE:
                        sub = new SubTabDataGeneSummary(project);
                        break;
                    case SUMMARYEXPMATRIX:
                        sub = new SubTabExpMatrixSummary(project);
                        break;
                    case SUMMARYANNOTATION:
                        sub = new SubTabAnnotationSummary(project);
                        break;
                    case SUMMARYFDA:
                        sub = new SubTabFDASummary(project);
                        break;
                    case SUMMARYFDACOMBINEDRESULTS:
                        sub = new SubTabFDASummaryCombinedResults(project);
                        break;
                    case DPERGENE:
                        sub = new SubTabDataPerGene(project);
                        break;
                    case DPERCHROMO:
                        sub = new SubTabDataPerChromo(project);
                        break;
                    case SDTRANS:
                        sub = new SubTabDataTransPerSC(project);
                        break;
                    case SDTRANSLENGTHS:
                        sub = new SubTabDataLengthPerSC(project);
                        break;
                    case SDEXONS:
                        sub = new SubTabDataExonsPerSC(project);
                        break;
                    case SUMMARYDIUTRANS:
                        subArgs.put("dataType", DataType.TRANS);
                        sub = new SubTabDIUSummary(project);
                        break;
                    case SUMMARYDIUPROT:
                        subArgs.put("dataType", DataType.PROTEIN);
                        sub = new SubTabDIUSummary(project);
                        break;
                    case SUMMARYDFI:
                        sub = new SubTabDFISummary(project);
                        break;
                    /*case VENNDIAGDEATRANS:
                        subArgs.put("dataType", DataType.TRANS);
                        sub = new SubTabDEAVennDiag(project);
                        break;
                    case VENNDIAGDEAPROT:
                        subArgs.put("dataType", DataType.PROTEIN);
                        sub = new SubTabDEAVennDiag(project);
                        break;
                    case VENNDIAGDEAGENE:
                        subArgs.put("dataType", DataType.GENE);
                        sub = new SubTabDEAVennDiag(project);
                        break;
                    case VENNDIAGDPA:
                        subArgs.put("dataType", DataType.GENE);
                        sub = new SubTabDPAVennDiag(project);
                        break;*/
                }
            }
        }
        catch(Exception e) { 
            logger.logError("internal program error: " + e.getMessage());
        }
        if(sub != null) {
            if(!sub.initialize(tbObj, info, subArgs))
                sub = null;
        }
        return sub;
    }
    @Override
    protected void setupSpecialPanel(String panelId) {
        SubTabBase.SubTabInfo panelInfo = getPanelInfo(panelId);
        if(panelId.startsWith(Panels.SUMMARYFEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FEASummary", TabProjectData.EA_GROUP, "", "FEA", "FEA Summary", "", "sumData.png", "FEA summary");
        }
        else if(panelId.startsWith(Panels.SUMMARYFDACOMBINEDRESULTS.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FDASummaryCombinedResults", TabProjectData.AF_GROUP, "", "FDA Combined Results Summary", "FDA Combined Results Summary", "", "sumData.png", "FDA Combined Results Summary");
        }
        else if(panelId.startsWith(Panels.SUMMARYFDA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FDASummary", TabProjectData.AF_GROUP, "", "FDA", "FDA Summary", "", "sumData.png", "FDA summary");
        }
        else if(panelId.startsWith(Panels.SUMMARYDFI.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DFISummary", TabProjectData.AF_GROUP, "", "DFI", "DFI Summary", "", "sumData.png", "DFI summary");
        }
        else if(panelId.startsWith(Panels.ENRICHEDFEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "ScatterChartScroll", TabProjectData.EA_GROUP, "FEA Enriched", "FEA Enriched Features", "FEA Enriched", "", "sumData.png", "FEA Enriched Features");
        }
        else if(panelId.startsWith(Panels.CLUSTERSFEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "WebView", "Clusters", "", "FEA", "FEA Enriched Features Clusters", "", "ea.png", "FEA Enriched Features Clusters");
        }
        else if(panelId.startsWith(Panels.ENRICHEDGSEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "ScatterChartScroll", TabProjectData.EA_GROUP, "GSEA Enriched", "GSEA Enriched Features", "GSEA Enriched", "", "sumData.png", "GSEA Enriched Features");
        }
        else if(panelId.startsWith(Panels.CLUSTERSGSEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "WebView", "Clusters", "", "GSEA", "GSEA Enriched Multidimensional Features", "", "ea.png", "GSEA Enriched Multidimensional Features");
        }
        else if(panelId.startsWith(Panels.CLUSTERSGOFEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "FEAClustersGO", "Clusters", "", "FEA", "FEA Enriched Features Clusters GO Terms", "", "ea.png", "FEA Enriched Features Clusters GO Terms");
        }
        else if(panelId.startsWith(Panels.CLUSTERSGOGSEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "GSEAClustersGO", "Clusters", "", "GSEA", "GSEA Enriched Multidimensional Features Clusters GO Terms", "", "ea.png", "GSEA Enriched Multidimensional Features Clusters GO Terms");
        }
        else if(panelId.startsWith(Panels.SUMMARYGSEA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "GSEASummary", TabProjectData.EA_GROUP, "", "GSEA", "GSEA Summary", "", "sumData.png", "GSEA summary");
        }
        else if(panelId.startsWith(Panels.SUMMARYDEAGENE.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DEAGeneSummary", TabProjectData.DA_GROUP, "DEAGenes", "DEA Genes Summary", "Summary for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for genes DEA");
        }
        else if(panelId.startsWith(Panels.SUMMARYDEAPROT.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DEAProteinSummary", TabProjectData.DA_GROUP, "DEAProteins", "DEA Proteins Summary", "Summary for Proteins Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for proteins DEA");
        }
        else if(panelId.startsWith(Panels.SUMMARYDEATRANS.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DEATransSummary", TabProjectData.DA_GROUP, "DEATranscripts", "DEA Transcripts Summary", "Summary for Transcripts Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for transcripts DEA");
        }
        else if(panelId.startsWith(Panels.CLUSTERSDEAGENE.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DEAClusters", TabProjectData.DA_GROUP, "DEAGenes", "DEA Genes Cluster Profiles", "Cluster Expression Profiles for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for genes DEA");
        }
        else if(panelId.startsWith(Panels.CLUSTERSDEAPROT.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DEAClusters", TabProjectData.DA_GROUP, "DEAProteins", "DEA Proteins Cluster Profiles", "Cluster Expression Profiles for Proteins Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for proteins DEA");
        }
        else if(panelId.startsWith(Panels.CLUSTERSDEATRANS.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DEAClusters", TabProjectData.DA_GROUP, "DEATranscripts", "DEA Transcripts Cluster Profiles", "Expression Profile Clusters for Transcripts Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for transcripts DEA");
        }
        else if(panelId.startsWith(Panels.CLUSTERSDPA.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "DPAClusters", TabProjectData.DA_GROUP, "DPAGenes", "DPA Genes Cluster Profiles", "Cluster Expression Profiles for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for genes DPA");
        }
        else if(panelId.startsWith(Panels.CLUSTERSUTRL.name())) {
            if(panelInfo == null)
                panelInfo = new SubTabBase.SubTabInfo(panelId, "UTRLClusters", TabProjectData.DA_GROUP, "UTRLGenes", "UTRL Genes Cluster Profiles", "Cluster Expression Profiles for Genes UTRL Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for genes UTRL");
        }
        else
            panelInfo = null;
        
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
            // check if it is one  of the Id tabs, see if already there and if not add to subtabsinfo array?
            String[] panels = ((String)reqArgs.get("panels")).split(";");
            String featureId = reqArgs.containsKey("id")? (String) reqArgs.get("id") : "";
            for(String panelName : panels) {
                String id = panelName + featureId;
                SubTabBase.SubTabInfo panelInfo = null;
//                if(panelName.equals(Panels.SUMMARYFEA.name())) {
//                    panelInfo = getPanelInfo(id);
//                    if(panelInfo == null)
//                        panelInfo = new SubTabBase.SubTabInfo(id, "FEASummary", TabProjectData.EA_GROUP, "", "FEA", "FEA Summary", "", "sumData.png", "FEA summary");
//                }else
                if(panelName.equals(Panels.ENRICHEDFEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "ScatterChartScroll", TabProjectData.EA_GROUP, "FEA Enriched", "FEA Enriched Features", "FEA Enriched", "", "sumData.png", "FEA Enriched Features");
                }
                else if(panelName.equals(Panels.CLUSTERSFEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "WebView", "Clusters", "", "FEA", "FEA Enriched Features Clusters", "", "ea.png", "FEA Enriched Features Clusters");
//                }else if(panelName.equals(Panels.SUMMARYDFI.name())) {
//                    panelInfo = getPanelInfo(id);
//                    if(panelInfo == null)
//                        panelInfo = new SubTabBase.SubTabInfo(id, "DFISummary", TabProjectData.AF_GROUP, "DFISummary", "DFI Summary Results", "Summary for Differential Feature Inclusion Analysis", "Differential Analysis", "dfi.png", "DFI Summary");
                }else if(panelName.equals(Panels.CLUSTERSGOFEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "FEAClustersGO", "Clusters", "", "FEA", "FEA Enriched Features Clusters GO Terms", "", "ea.png", "FEA Enriched Features Clusters GO Terms");
                }
                if(panelName.equals(Panels.ENRICHEDGSEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "ScatterChartScroll", TabProjectData.EA_GROUP, "GSEA Enriched", "GSEA Enriched Features", "GSEA Enriched", "", "sumData.png", "GSEA Enriched Features");
                }
                else if(panelName.equals(Panels.CLUSTERSGSEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if (panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "WebView", "Clusters", "", "GSEA", "GSEA Enriched Multidimensional Features", "", "ea.png", "GSEA Enriched Multidimensional Features");
                }
                else if(panelName.equals(Panels.CLUSTERSGOGSEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "GSEAClustersGO", "Clusters", "", "GSEA", "GSEA Enriched Multidimensional Features Clusters GO Terms", "", "ea.png", "FEA Enriched Multidimensional Features Clusters GO Terms");
                }
                else if(panelName.equals(Panels.SUMMARYGSEA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "GSEASummary", TabProjectData.EA_GROUP, "", "GSEA", "GSEA Summary", "", "sumData.png", "GSEA summary");
                }
                else if(panelName.equals(Panels.SUMMARYDEAGENE.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DEAGeneSummary", TabProjectData.DA_GROUP, "DEAGenes", "DEA Genes Summary", "Summary for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for genes DEA");
                }
                else if(panelName.equals(Panels.SUMMARYDEAPROT.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DEAProteinSummary", TabProjectData.DA_GROUP, "DEAProteins", "DEA Proteins Summary", "Summary for Proteins Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for proteins DEA");
                }
                else if(panelName.equals(Panels.SUMMARYDEATRANS.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DEATransSummary", TabProjectData.DA_GROUP, "DEATranscripts", "DEA Transcripts Summary", "Summary for Transcripts Differential Expression Analysis", "Differential Analysis", "sumData.png", "Summary for transcripts DEA");
                }
                else if(panelName.equals(Panels.SUMMARYDPAGENE.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DPAGeneSummary", TabProjectData.DA_GROUP, "DEAGenes", "DPA Genes Summary", "Summary for Genes Differential PolyAdenylation Analysis", "Differential PolyAdenylation Analysis", "sumData.png", "Summary for genes DPA");
                }
                else if(panelName.equals(Panels.SUMMARYUTRL.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "UTRLSummary", TabProjectData.DA_GROUP, "DEAGenes", "UTRL Summary", "Summary for UTRL Lengthening Analysis", "UTRL Lengthening Analysis", "sumData.png", "Summary for UTRL Lengthening");
                }
                else if(panelName.equals(Panels.CLUSTERSDEAGENE.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DEAClusters", TabProjectData.DA_GROUP, "DEAGenes", "DEA Genes Cluster Profiles", "Cluster Expression Profiles for Genes Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for genes DEA");
                }
                else if(panelName.equals(Panels.CLUSTERSDEAPROT.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DEAClusters", TabProjectData.DA_GROUP, "DEAProteins", "DEA Proteins Cluster Profiles", "Cluster Expression Profiles for Proteins Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for proteins DEA");
                }
                else if(panelName.equals(Panels.CLUSTERSDEATRANS.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DEAClusters", TabProjectData.DA_GROUP, "DEATranscripts", "DEA Transcripts Cluster Profiles", "Expression Profile Clusters for Transcripts Differential Expression Analysis", "Differential Analysis", "sumData.png", "Cluster Expression Profiles for transcripts DEA");
                }
                else if(panelName.equals(Panels.CLUSTERSDPA.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "DPAClusters", TabProjectData.DA_GROUP, "DPAGenes", "DPA Genes Cluster Profiles", "Cluster Expression Profiles for Differential PolyAdenylation Analysis", "Differential PolyAdenylation Analysis", "sumData.png", "Cluster Expression Profiles for genes DPA");
                }
                else if(panelName.equals(Panels.CLUSTERSUTRL.name())) {
                    panelInfo = getPanelInfo(id);
                    if(panelInfo == null)
                        panelInfo = new SubTabBase.SubTabInfo(id, "UTRLClusters", TabProjectData.DA_GROUP, "UTRLGenes", "UTRL Genes Cluster Profiles", "Cluster Expression Profiles for UTRL Analysis", "UTR Lengtening", "sumData.png", "Cluster Expression Profiles for genes UTRL");
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
        if(hm.containsKey("closePanels"))
            closePanels((String)hm.get("closePanels"));
        if(hm.containsKey("closePanelsStartWith"))
            closePanelsStartWith((String)hm.get("closePanelsStartWith"));
        if(hm.containsKey("updateDIUResults")) {
            int index = 0;
            for(int ordinal = 0; ordinal < static_subTabsInfo.length; ordinal++){
                if(static_subTabsInfo[ordinal].subTabId.equals(Panels.SUMMARYDIUTRANS)){
                    index = ordinal;
                    break;
                }
            }
            if(tabDef.subTabsInfo[index].subTabBase != null)
                obj = tabDef.subTabsInfo[index].subTabBase.processRequest(hm);
            
            for(int ordinal = 0; ordinal < static_subTabsInfo.length; ordinal++){
                if(static_subTabsInfo[ordinal].subTabId.equals(Panels.SUMMARYDIUPROT)){
                    index = ordinal;
                    break;
                }
            }            
            if(tabDef.subTabsInfo[index].subTabBase != null)
                obj = tabDef.subTabsInfo[index].subTabBase.processRequest(hm);
        }
        if(hm.containsKey("updateDEAResults")) {
            String deaType = (String) hm.get("updateDEAResults");
            if(deaType.equals("DEA" + DataType.TRANS)) {
                int index = 0;
                for(int ordinal = 0; ordinal < static_subTabsInfo.length; ordinal++){
                    if(static_subTabsInfo[ordinal].subTabId.equals(Panels.SUMMARYDEATRANS)){
                        index = ordinal;
                        break;
                    }
                }
                if(tabDef.subTabsInfo[index].subTabBase != null)
                    obj = tabDef.subTabsInfo[index].subTabBase.processRequest(hm);
            }
            else if(deaType.equals("DEA" + DataType.PROTEIN)) {
                int index = 0;
                for(int ordinal = 0; ordinal < static_subTabsInfo.length; ordinal++){
                    if(static_subTabsInfo[ordinal].subTabId.equals(Panels.SUMMARYDEAPROT)){
                        index = ordinal;
                        break;
                    }
                }
                if(tabDef.subTabsInfo[index].subTabBase != null)
                    obj = tabDef.subTabsInfo[index].subTabBase.processRequest(hm);
            }
            else if(deaType.equals("DEA" + DataType.GENE)) {
                int index = 0;
                for(int ordinal = 0; ordinal < static_subTabsInfo.length; ordinal++){
                    if(static_subTabsInfo[ordinal].subTabId.equals(Panels.SUMMARYDEAGENE)){
                        index = ordinal;
                        break;
                    }
                }
                if(tabDef.subTabsInfo[index].subTabBase != null)
                    obj = tabDef.subTabsInfo[index].subTabBase.processRequest(hm);
            }
        }
        if(hm.containsKey("updateFEAResults")) {
            String featureId = (String) hm.get("updateFEAResults");
            //SUMMARYFEA, ENRICHEDFEA, CLUSTERSFEA, CLUSTERSGOFEA
            SubTabBase.SubTabInfo sti = getPanelInfo(Panels.SUMMARYFEA + featureId);
            if(sti != null && sti.subTabBase != null)
                obj = sti.subTabBase.processRequest(hm);
            sti = getPanelInfo(Panels.ENRICHEDFEA + featureId);
            if(sti != null && sti.subTabBase != null)
                obj = sti.subTabBase.processRequest(hm);
        }
        if(hm.containsKey("updateGSEAResults")) {
            String featureId = (String) hm.get("updateGSEAResults");
            //SUMMARYFEA, ENRICHEDFEA, CLUSTERSFEA, CLUSTERSGOFEA
            SubTabBase.SubTabInfo sti = getPanelInfo(Panels.SUMMARYGSEA + featureId);
            if(sti != null && sti.subTabBase != null)
                obj = sti.subTabBase.processRequest(hm);
            sti = getPanelInfo(Panels.ENRICHEDGSEA + featureId);
            if(sti != null && sti.subTabBase != null)
                obj = sti.subTabBase.processRequest(hm);
        }
        if(hm.containsKey("updateProjectName")) {
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
                    if(tb.equals(obj))
                        info.subTabBase.search(obj, txt, hide);
                }
            }
        }
    }
    
    public void showExpLevelsChart(DataType type, BarChart barChart) {
        String name = "Transcript";
        if(type == DataType.PROTEIN)
            name = "Protein";
        else if(type == DataType.GENE)
            name = "Gene";
        barChart.setTitle(name + " Expression Levels");
        barChart.getXAxis().setLabel("Normalized Expression Levels (Log10 of mean)");
        barChart.getYAxis().setLabel(name + "s");
        DataExpMatrix.ExpressionLevelsDistribution expLevelsDist = project.data.getMeanLog10ExpressionLevelsDistribution(type, project.data.getResultsTrans());

        String[] names = project.data.getGroupNames();
        XYChart.Series<String, Number> series[] = new XYChart.Series[expLevelsDist.conditions];
        int totalcnt = 0;
        for(int i = 0; i < expLevelsDist.conditions; i++) {
            series[i] = new XYChart.Series<>();
            series[i].setName(names[i]);
            // relies on min always starting at -1
            for(int j = expLevelsDist.min; j <= expLevelsDist.max; j++) {
                series[i].getData().add(new XYChart.Data("" + j, expLevelsDist.conditionLogLevels[i][j+1]));
                if(i == 0)
                    totalcnt += expLevelsDist.conditionLogLevels[i][j+1];
            }
        }
        barChart.getXAxis().tickLabelFontProperty().set(Font.font(10));
        for(XYChart.Series<String, Number> pcs : series)
            barChart.getData().add(pcs);
        app.ctls.setupChartExport(barChart, name + " Expression Levels Chart", "tappAS_" + name + "ExpLevelsChart.png", null);
        Tooltip tt;
        HashMap<String, Integer> hmCats = project.data.getAlignmentCategoriesTransCount(project.data.getExpressedTrans(null));
        for(int c = 0; c < series.length; c++) {
            for(int i = 0; i < series[c].getData().size(); i++) {
                XYChart.Data item = series[c].getData().get(i);
                double pct = (double)(Integer)item.getYValue() * 100 / (double)totalcnt;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                String tooltip = names[c] + " Expression Level " + (i-1) + "\nPercent: " + pct + "%";
                tooltip += "\nCount: " + NumberFormat.getInstance().format(item.getYValue()) + " out of " + NumberFormat.getInstance().format(totalcnt);
                tt = new Tooltip(tooltip);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
        }
    }
    public boolean genExpLevelsPlotData(DataType type) {
        boolean result = false;
        double[][] expLevels = project.data.getMeanExpressionLevels(type, project.data.getResultsTrans());
        String filepath = project.data.getMatrixMeanLogExpLevelsFilepath(type.name());
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
            String header = "";
            String[] names = project.data.getGroupNames();
            for(String cname : names)
                header += (header.isEmpty()? "" : "\t") + cname;
            writer.write(header + "\n");
            int rows = expLevels[0].length;
            for(int row = 0; row < rows; row++) {
                String rowvals = "";
                for(int cond = 0; cond < expLevels.length; cond++)
                    rowvals += (rowvals.isEmpty()? "" : "\t") + Math.log10(expLevels[cond][row]);
                writer.write(rowvals + "\n");
            }
            result = true;
        } catch(Exception e) {
            app.logError("Unable to write expression matrix mean log file: " + e.getMessage());
        } finally {
           try { if(writer != null) writer.close(); } catch (Exception e) {System.out.println("Exception within exception: " + e.getMessage());}
        }
        
        // make sure to remove bad file if needed
        if(!result)
            Utils.removeFile(Paths.get(filepath));
        return result;
    }

    //
    // DPA Functions
    //
    
    public void showFoldChangeChart(ArrayList<DataDPA.DPAResultsData> dpaResults, BarChart barChart, String label) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        FoldChangeData[] fc = new FoldChangeData[16];
        int idx = 0;
        // down
        fc[idx++] = new FoldChangeData("-20+", 0);       // 0
        fc[idx++] = new FoldChangeData("[-15, -20)", 0); // 1
        fc[idx++] = new FoldChangeData("[-10, -15)", 0); // 2
        fc[idx++] = new FoldChangeData("[-5, -10)", 0);  // 3
        fc[idx++] = new FoldChangeData("-4", 0);         // 4
        fc[idx++] = new FoldChangeData("-3", 0);         // 5
        fc[idx++] = new FoldChangeData("-2", 0);         // 6
        fc[idx++] = new FoldChangeData("[0, -2)", 0);    // 7
        // up
        fc[idx++] = new FoldChangeData("[0, 2)", 0);     // 8
        fc[idx++] = new FoldChangeData("2", 0);          // 9
        fc[idx++] = new FoldChangeData("3", 0);          // 10
        fc[idx++] = new FoldChangeData("4", 0);          // 11
        fc[idx++] = new FoldChangeData("[5, 10)", 0);    // 12
        fc[idx++] = new FoldChangeData("[10, 15)", 0);   // 13
        fc[idx++] = new FoldChangeData("[15, 20)", 0);   // 14
        fc[idx++] = new FoldChangeData("20+", 0);        // 15
        double logFC;
        int upoff = 8;
        int dnoff = 0;
        int totalcnt = 0;
        for(DataDPA.DPAResultsData rd : dpaResults) {
            if(rd.ds) {
                //Methods DEXSEQ or MASIGPRO --- Distal/Proximal
                logFC = Math.log10(rd.distalMeanExpression/rd.proximalMeanExpression)/Math.log10(2);
                if(logFC >= 0) {
                    if(logFC < 2)
                        fc[upoff].count++;
                    else if(logFC < 3)
                        fc[upoff+1].count++;
                    else if(logFC < 4)
                        fc[upoff+2].count++;
                    else if(logFC < 5)
                        fc[upoff+3].count++;
                    else if(logFC < 10)
                        fc[upoff+4].count++;
                    else if(logFC < 15)
                        fc[upoff+5].count++;
                    else if(logFC < 20)
                        fc[upoff+6].count++;
                    else
                        fc[upoff+7].count++;
                    totalcnt++;
                }
                else {
                    if(logFC <= -20)
                        fc[dnoff].count++;
                    else if(logFC <= -15)
                        fc[dnoff+1].count++;
                    else if(logFC <= -10)
                        fc[dnoff+2].count++;
                    else if(logFC <= -5)
                        fc[dnoff+3].count++;
                    else if(logFC <= -4)
                        fc[dnoff+4].count++;
                    else if(logFC <= -3)
                        fc[dnoff+5].count++;
                    else if(logFC <= -2)
                        fc[dnoff+6].count++;
                    else
                        fc[dnoff+7].count++;
                    totalcnt++;
                }
            }
        }
        String info = "No data available";
        double val, pct;
        for(int i = 0; i < fc.length; i++) {
            val = fc[i].count;
            pct = Math.abs(val) * 100.00 / totalcnt;
            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
            series.getData().add(new XYChart.Data<>(fc[i].label, val));
            series.getData().get(i).setExtraValue((int)pct);
        }
        barChart.setLegendVisible(false);
        barChart.getXAxis().setLabel("Log2 Fold Change");
        barChart.getYAxis().setLabel(label + "s");
        barChart.getData().addAll(series);
        app.ctls.setupChartExport(barChart, label + "Log2 Fold Change Chart", "tappAS_DE_" + label + "L2FoldChangeChart.png", null);

        Tooltip tt;
        for(int i = 0; i < series.getData().size(); i++) {
            XYChart.Data item = series.getData().get(i);
            tt = new Tooltip(fc[i].label + " Log2 Fold Change:  " + Math.abs((int)Double.parseDouble(item.getYValue().toString())) + " " + label.toLowerCase() + "s, " + item.getExtraValue().toString() + "% ");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
    }
    
    //
    // DEA Functions
    //
    public void showFoldChangeChart(DataDEA.DEAResults deaResults, BarChart barChart, String label) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        FoldChangeData[] fc = new FoldChangeData[16];
        int idx = 0;
        // down
        fc[idx++] = new FoldChangeData("-20+", 0);       // 0
        fc[idx++] = new FoldChangeData("[-15, -20)", 0); // 1
        fc[idx++] = new FoldChangeData("[-10, -15)", 0); // 2
        fc[idx++] = new FoldChangeData("[-5, -10)", 0);  // 3
        fc[idx++] = new FoldChangeData("-4", 0);         // 4
        fc[idx++] = new FoldChangeData("-3", 0);         // 5
        fc[idx++] = new FoldChangeData("-2", 0);         // 6
        fc[idx++] = new FoldChangeData("[0, -2)", 0);    // 7
        // up
        fc[idx++] = new FoldChangeData("[0, 2)", 0);     // 8
        fc[idx++] = new FoldChangeData("2", 0);          // 9
        fc[idx++] = new FoldChangeData("3", 0);          // 10
        fc[idx++] = new FoldChangeData("4", 0);          // 11
        fc[idx++] = new FoldChangeData("[5, 10)", 0);    // 12
        fc[idx++] = new FoldChangeData("[10, 15)", 0);   // 13
        fc[idx++] = new FoldChangeData("[15, 20)", 0);   // 14
        fc[idx++] = new FoldChangeData("20+", 0);        // 15
        double logFC;
        int upoff = 8;
        int dnoff = 0;
        int totalcnt = 0;
        for(DataDEA.DEAResultsData rd : deaResults.lstResults) {
            if(rd.de) {
                // Method EDGER, NOISEQ and MASIGPRO
                if(deaResults.getMethod().equals(DlgDEAnalysis.Params.Method.EDGER))
                    logFC = rd.values[DataDEA.EdgeRValues.log2FC.ordinal()];
                else
                    logFC = rd.values[DataDEA.NOISeqValues.log2FC.ordinal()];
                if(logFC >= 0) {
                    if(logFC < 2)
                        fc[upoff].count++;
                    else if(logFC < 3)
                        fc[upoff+1].count++;
                    else if(logFC < 4)
                        fc[upoff+2].count++;
                    else if(logFC < 5)
                        fc[upoff+3].count++;
                    else if(logFC < 10)
                        fc[upoff+4].count++;
                    else if(logFC < 15)
                        fc[upoff+5].count++;
                    else if(logFC < 20)
                        fc[upoff+6].count++;
                    else
                        fc[upoff+7].count++;
                    totalcnt++;
                }
                else {
                    if(logFC <= -20)
                        fc[dnoff].count--;
                    else if(logFC <= -15)
                        fc[dnoff+1].count--;
                    else if(logFC <= -10)
                        fc[dnoff+2].count--;
                    else if(logFC <= -5)
                        fc[dnoff+3].count--;
                    else if(logFC <= -4)
                        fc[dnoff+4].count--;
                    else if(logFC <= -3)
                        fc[dnoff+5].count--;
                    else if(logFC <= -2)
                        fc[dnoff+6].count--;
                    else
                        fc[dnoff+7].count--;
                    totalcnt++;
                }
            }
        }
        String info = "No data available";
        double val, pct;
        for(int i = 0; i < fc.length; i++) {
            val = fc[i].count;
            pct = Math.abs(val) * 100.00 / totalcnt;
            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
            series.getData().add(new XYChart.Data<>(fc[i].label, val));
            series.getData().get(i).setExtraValue((int)pct);
        }
        barChart.setLegendVisible(false);
        barChart.getXAxis().setLabel("Log2 Fold Change");
        barChart.getYAxis().setLabel(label + "s");
        barChart.getData().addAll(series);
        app.ctls.setupChartExport(barChart, label + "Log2 Fold Change Chart", "tappAS_DE_" + label + "L2FoldChangeChart.png", null);

        Tooltip tt;
        for(int i = 0; i < series.getData().size(); i++) {
            XYChart.Data item = series.getData().get(i);
            tt = new Tooltip(fc[i].label + " Log2 Fold Change:  " + Math.abs((int)Double.parseDouble(item.getYValue().toString())) + " " + label.toLowerCase() + "s, " + item.getExtraValue().toString() + "% ");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
    }

    //
    // Static Functions
    //
    
    static private SubTabBase.SubTabInfo[] getSubTabInfo() {
        SubTabBase.SubTabInfo[] newsti = new SubTabBase.SubTabInfo[static_subTabsInfo.length];
        int idx = 0;
        for(SubTabBase.SubTabInfo sti : static_subTabsInfo)
            newsti[idx++] = new SubTabBase.SubTabInfo(sti);
        return newsti;
    }
    static private SubTabBase.SubTabInfo[] getSubTabDynamicInfo() {
        SubTabBase.SubTabInfo[] newsti = new SubTabBase.SubTabInfo[static_subTabsDynamicInfo.length];
        int idx = 0;
        for(SubTabBase.SubTabInfo sti : static_subTabsDynamicInfo)
            newsti[idx++] = new SubTabBase.SubTabInfo(sti);
        return newsti;
    }
    static ArrayList<SubTabBase.SubTabInfo> getItemTypePanels(DlgSelectGraph.Params.ItemType itemType) {
        ArrayList<SubTabBase.SubTabInfo> items = new ArrayList<>();
        String group = "";
        switch(itemType) {
            case AF:
                group = TabProjectData.AF_GROUP;
                break;
            case Data:
                group = TabProjectData.DATA_GROUP;
                break;
            case DA:
                group = TabProjectData.DA_GROUP;
                break;
            case EA:
                group = TabProjectData.EA_GROUP;
                break;
            case EM:
                group = TabProjectData.EM_GROUP;
                break;
        }
        if(!group.isEmpty()) {
            for(SubTabBase.SubTabInfo sti : static_subTabsInfo) {
                if(sti.group.equals(group))
                    items.add(sti);
            }
        }
        Collections.sort(items);
        return items;
    }
    static ArrayList<SubTabBase.SubTabInfo> getItemTypeDynamicPanels(DlgSelectGraph.Params.ItemType itemType) {
        ArrayList<SubTabBase.SubTabInfo> items = new ArrayList<>();
        String group = "";
        switch(itemType) {
            case AF:
                group = TabProjectData.AF_GROUP;
                break;
            case Data:
                group = TabProjectData.DATA_GROUP;
                break;
            case DA:
                group = TabProjectData.DA_GROUP;
                break;
            case EA:
                group = TabProjectData.EA_GROUP;
                break;
            case EM:
                group = TabProjectData.EM_GROUP;
                break;
        }
        if(!group.isEmpty()) {
            for(SubTabBase.SubTabInfo sti : static_subTabsDynamicInfo) {
                if(sti.group.equals(group))
                    items.add(sti);
            }
        }
        Collections.sort(items);
        return items;
    }
    
    //
    // Data Classes
    //
    
    public static class FoldChangeData {
        String label;
        int count;
        public FoldChangeData(String label, int count) {
            this.label = label;
            this.count = count;
        }
    }

    public static class SummaryDataCount {
        public final SimpleStringProperty field;
        public final SimpleIntegerProperty count;
 
        public SummaryDataCount(String field, int count) {
            this.field = new SimpleStringProperty(field);
            this.count = new SimpleIntegerProperty(count);
        }
        public String getField() { return field.get(); }
        public int getCount() { return count.get(); }
    }
    public static class GeneSummaryData1 {
        public final SimpleStringProperty field;
        public final SimpleIntegerProperty count1;
        public final SimpleDoubleProperty pct1;
 
        public GeneSummaryData1(String field, int count1, double pct1) {
            this.field = new SimpleStringProperty(field);
            this.count1 = new SimpleIntegerProperty(count1);
            this.pct1 = new SimpleDoubleProperty(pct1);
        }
        public String getField() { return field.get(); }
        public int getCount1() { return count1.get(); }
        public double getPct1() { return pct1.get(); }
    }
    public static class SummaryDataPct {
        public final SimpleStringProperty field;
        public final SimpleIntegerProperty count;
        public final SimpleDoubleProperty pct;
 
        public SummaryDataPct(String field, int count, double pct) {
            this.field = new SimpleStringProperty(field);
            this.count = new SimpleIntegerProperty(count);
            this.pct = new SimpleDoubleProperty(pct);
        }
        public String getField() { return field.get(); }
        public int getCount() { return count.get(); }
        public double getPct() { return pct.get(); }
    }
}
