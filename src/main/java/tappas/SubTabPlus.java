/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import tappas.DataApp.EnumData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabPlus extends SubTabBase {
    WebView web;
    WebEngine engine;
    HashMap<String, EnumData> hmFEA = new HashMap<>();
    HashMap<String, EnumData> hmGSEA = new HashMap<>();
    HashMap<String, EnumData> hmFDA = new HashMap<>();
    HashMap<String, EnumData> hmDFI = new HashMap<>();
    boolean hasDAData = false;
    boolean hasDEADataGene = false;
    boolean hasDEADataTrans = false;
    boolean hasDEADataProtein = false;
    boolean hasDIUDataTrans = false;
    boolean hasDIUDataProtein = false;
    boolean hasFDAData = false;
    boolean hasDFIData = false;
    boolean hasDPAData = false;
    boolean hasUTRLData = false;
    
    public SubTabPlus(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }
    @Override
    protected void show() {
        // can only load once, do not load while loading initial tabs for project
        if(!populated) {
            if(!app.ctlr.isLoadingTabs()) {
                populated = true;
                runDataLoadThread();
            }
        }
        else
            checkForUpdates();
    }
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        web = (WebView) tabNode.lookup("#webView");
        engine = web.getEngine();
        web.setOnMouseClicked((event) -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                String subTabId = (String) engine.executeScript("getSubTabId()");
                System.out.println("subTabId: " + subTabId);
                if(!subTabId.isEmpty()) {
                    try {
                        tabBase.openSubTab(subTabId);
                    }
                    catch(Exception e) {
                        app.logError("Unable to show requested sub tab: " + e.getMessage());
                    }
                }
            }
        });
        refreshSubTab(null);
        setFocusNode(web, true);
    }
    private void checkForUpdates() {
        if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA) || tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV)) {
            boolean refresh = false;
            if(hasDAData != project.data.analysis.hasAnyDAData() ||
                hasDEADataGene != project.data.analysis.hasDEAData(DataApp.DataType.GENE) ||
                hasDEADataTrans != project.data.analysis.hasDEAData(DataApp.DataType.TRANS) || 
                hasDEADataProtein != project.data.analysis.hasDEAData(DataApp.DataType.PROTEIN) ||
                hasDIUDataTrans != project.data.analysis.hasDIUDataTrans() ||
                hasDIUDataProtein != project.data.analysis.hasDIUDataProtein() ||
                hasFDAData != project.data.analysis.hasAnyFDAData() ||
                hasDFIData != project.data.analysis.hasAnyDFIData() ||
                hasDPAData != project.data.analysis.hasDPAData()) {
                    refresh = true;
            }
            else {
                // check if FDA data changed
                HashMap<String, EnumData> hm = new HashMap<>();
                ArrayList<EnumData> lst = project.data.analysis.getFDAResultsList();
                for(EnumData ed : lst)
                    hm.put(ed.id + "\t" + ed.name, ed);
                if(hm.size() != hmFDA.size()) {
                    System.out.println("Need refresh - FDA entries are not up to date (size)");
                    refresh = true;
                }
                else {
                    for(String id : hm.keySet()) {
                        if(!hmFDA.containsKey(id)) {
                            System.out.println("Need refresh - FDA entries are not up to date");
                            refresh = true;
                            break;
                        }
                    }
                }
                // check if DFI data changed
                hm = new HashMap<>();
                lst = project.data.analysis.getDFIResultsList();
                for(EnumData ed : lst)
                    hm.put(ed.id + "\t" + ed.name, ed);
                if(hm.size() != hmDFI.size()) {
                    System.out.println("Need refresh - DFI entries are not up to date (size)");
                    refresh = true;
                }
                else {
                    for(String id : hm.keySet()) {
                        if(!hmDFI.containsKey(id)) {
                            System.out.println("Need refresh - DFI entries are not up to date");
                            refresh = true;
                            break;
                        }
                    }
                }
                // check if FEA data changed
                hm = new HashMap<>();
                lst = project.data.analysis.getFEAResultsList();
                for(EnumData ed : lst)
                    hm.put(ed.id + "\t" + ed.name, ed);
                if(hm.size() != hmFEA.size()) {
                    System.out.println("Need refresh - FEA entries are not up to date (size)");
                    refresh = true;
                }
                else {
                    for(String id : hm.keySet()) {
                        if(!hmFEA.containsKey(id)) {
                            System.out.println("Need refresh - FEA entries are not up to date");
                            refresh = true;
                            break;
                        }
                    }
                }
                if(!refresh) {
                    lst = project.data.analysis.getGSEAResultsList();
                    hm.clear();
                    for(EnumData ed : lst)
                        hm.put(ed.id + "\t" + ed.name, ed);
                    if(hm.size() != hmGSEA.size()) {
                        System.out.println("Need refresh - GSEA entries are not up to date (size)");
                        refresh = true;
                    }
                    else {
                        for(String id : hm.keySet()) {
                            if(!hmGSEA.containsKey(id)) {
                                System.out.println("Need refresh - GSEA entries are not up to date");
                                refresh = true;
                                break;
                            }
                        }
                    }
                }
            }

            if(refresh)
                engine.loadContent(buildHTML());
        }
    }
    @Override
    protected void refreshSubTab(HashMap<String, Object> hmArgs) {
        engine.loadContent(buildHTML());
    }
    private String buildHTML() {
        String html = "<!DOCTYPE html>\n" +
                    "<meta charset=\"utf-8\">\n" +
                    "<title>tappAS</title>\n" +
                    "<head>\n" +
                    "</head>\n" +
                    "<style>\n" +
                    "html, body {font-family: 'Verdana', sans-serif;}\n" +
                    "img:hover { cursor:pointer; }\n" +
                    ".clickTab { cursor:pointer; }\n" +
                    "</style>\n";
        html += "<script>\n" +
                "var selSubTabId = \"\";" +
                "function setSubTabId(subTabId) {\n" +
                "selSubTabId = subTabId;\n" +
                "}\n" +
                "function getSubTabId() {\n" +
                "var id = selSubTabId;\n" +
                "selSubTabId = \"\";\n" +
                "return id;\n" +
                "}\n" +
                "</script>\n" +
                "<body>\n" +
                "<H3><span style=\"color:#a2ca72;\">" + tabBase.getTabName() + " Tab</span></H3>\n" +
                "<div><span style=\"font-size:medium;\">You may view sub tabs listed below:</div><br/>\n";
        String subTabs = "<div style=\"padding-left: 10px; padding-top:5px;\"><table>";
        String lastGroup = "";
        boolean pad = false;
        if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA) || tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV)) {
            hasDAData = project.data.analysis.hasAnyDAData();
            hasDEADataGene = project.data.analysis.hasDEAData(DataApp.DataType.GENE);
            hasDEADataTrans = project.data.analysis.hasDEAData(DataApp.DataType.PROTEIN);
            hasDEADataProtein = project.data.analysis.hasDEAData(DataApp.DataType.PROTEIN);
            hasDIUDataTrans = project.data.analysis.hasDIUDataTrans();
            hasDIUDataProtein = project.data.analysis.hasDIUDataProtein();
            //hasFDAData = project.data.analysis.hasAnyFDAData();
            //hasDFIData = project.data.analysis.hasAnyDFIData();
            hasDPAData = project.data.analysis.hasDPAData();
            hasUTRLData = project.data.analysis.hasUTRLData();
            System.out.println("hasDIUDataTrans: " + hasDIUDataTrans);
        }
        SubTabBase.SubTabInfo[] subTabsInfo = tabBase.getSubTabsInfo();
        for(SubTabBase.SubTabInfo sti : subTabsInfo) {
            if(sti.subTabId.equals("DPERCHROMO")){
                String genus = project.data.getGenus();
                String specie = project.data.getSpecies();
                List<String> names = project.data.getChromoDataSpecies();
                if(!names.contains(genus + "_" + specie))
                    continue;
            }
            
            // check if dynamic sub tab and skip it, will be handled below [FEA, GSEA, FDA, DFI]
            if(sti.group.equals(TabProjectData.EA_GROUP) || sti.group.equals(TabProjectData.AF_GROUP))
                continue;

            // check if we have data to display for these sub tabs, otherwise skip
            if(sti.subTabId.equals(TabProjectData.Panels.DARESULTS.name())) {
                if(!hasDAData)
                    continue;
            }
            else if(sti.subTabId.equals(TabProjectData.Panels.STATSDEAGENE.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYDEAGENE.name())) {
                if(!hasDEADataGene)
                    continue;
            }
            else if(sti.subTabId.equals(TabProjectData.Panels.STATSDEATRANS.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYDEATRANS.name())) {
                if(!hasDEADataTrans)
                    continue;
            }
            else if(sti.subTabId.equals(TabProjectData.Panels.STATSDEAPROT.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYDEAPROT.name())) {
                if(!hasDEADataProtein)
                    continue;
            }
            else if(sti.subTabId.equals(TabProjectData.Panels.STATSDIUTRANS.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYDIUTRANS.name())) {
                if(!hasDIUDataTrans)
                    continue;
                else
                    System.out.println("hasDIUDataTrans OK");
            }
            else if(sti.subTabId.equals(TabProjectData.Panels.STATSDIUPROT.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYDIUPROT.name())) {
                if(!hasDIUDataProtein)
                    continue;
            }
            /*else if(sti.subTabId.equals(TabProjectData.Panels.STATSFDA.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYFDA.name())) {
                if(hasFDAData){
                    hmFDA.clear();
                    ArrayList<EnumData> lst = project.data.analysis.getFDAResultsList();
                    for(EnumData ed : lst)
                        hmFDA.put(ed.id + "\t" + ed.name, ed);
                    System.out.println("Updated FDA entries in Plus tab");
                    
                    if(!hmFDA.isEmpty()) {
                        // Functional Diversity Analysis - dynamic results
                        subTabs += "<tr><td colspan=\"2\">&nbsp;</td></tr>";
                        subTabs += "<tr style=\"margin: 20px;\">";
                        subTabs += "<td colspan=\"2\" style=\"background-color:#a2ca72; padding:5px;\">";
                        subTabs += "<span style=\"font-size:medium; color:white\">" + "Functional Diversity Analysis" + "</span>";
                        subTabs += "</td>";
                        subTabs += "</tr>";
                        if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV)) {
                            // Functional Diversity Analysis
                            String padding = "7";
                            if(!hmFDA.isEmpty()) {
                                // display non-clickable header
                                subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                                subTabs += "<span style=\"font-size:medium;\">FDA Summary</span>";
                                subTabs += " - <span style=\"font-size:small;\">";
                                subTabs += "&nbsp;" + "Functional Diversity Analysis (FDA) Summary:" + "</span></td></tr>";
                                for(EnumData data : hmFDA.values()) {
                                    String entry = "<tr>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYFDA + data.id + "')\" ";
                                    entry += "style=\"height:32px;width:32px;";
                                    entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fda.png");
                                    entry += "\"></td>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYFDA + data.id + "')\"";
                                    entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                                    subTabs += entry;
                                    padding = "0";
                                }
                            }
                        }
                        else if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA)) {
                            // Functional Diversity Analysis Analysis
                            String padding = "7";
                            if(!hmFDA.isEmpty()) {
                                // display non-clickable header
                                subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                                subTabs += "<span style=\"font-size:medium;\">FDA Results</span>";
                                subTabs += " - <span style=\"font-size:small;\">";
                                subTabs += "&nbsp;" + "Functional Diversity Analysis (FDA) Results:" + "</span></td></tr>";
                                for(EnumData data : hmFDA.values()) {
                                    String entry = "<tr>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSFDA + data.id + "')\" ";
                                    entry += "style=\"height:32px;width:32px;";
                                    entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fda.png");
                                    entry += "\"></td>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSFDA + data.id + "')\"";
                                    entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                                    subTabs += entry;
                                    padding = "0";
                                }
                            }
                        }
                    }
                    
                }else
                    continue;
            }*/
            /*else if(sti.subTabId.equals(TabProjectData.Panels.STATSDFI.name()) ||
                    sti.subTabId.equals(TabProjectData.Panels.STATSDFIRESULTSSUMMARY.name()) ||
                    sti.subTabId.equals(TabProjectData.Panels.STATSDFIASSOCIATION.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYDFI.name())) {
                if(hasDFIData){
                    hmDFI.clear();
                    ArrayList<EnumData> lst = project.data.analysis.getDFIResultsList();
                    for(EnumData ed : lst)
                        hmDFI.put(ed.id + "\t" + ed.name, ed);
                    System.out.println("Updated DFI entries in Plus tab");
                    
                    if(!hmDFI.isEmpty()) {
                        // Differential Feature Inclusion - dynamic results
                        subTabs += "<tr><td colspan=\"2\">&nbsp;</td></tr>";
                        subTabs += "<tr style=\"margin: 20px;\">";
                        subTabs += "<td colspan=\"2\" style=\"background-color:#a2ca72; padding:5px;\">";
                        subTabs += "<span style=\"font-size:medium; color:white\">" + "Differential Feature Inclusion Analysis" + "</span>";
                        subTabs += "</td>";
                        subTabs += "</tr>";
                        if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV)) {
                            // Differential Feature Inclusion Analysis
                            String padding = "7";
                            if(!hmDFI.isEmpty()) {
                                // display non-clickable header
                                subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                                subTabs += "<span style=\"font-size:medium;\">DFI Summary</span>";
                                subTabs += " - <span style=\"font-size:small;\">";
                                subTabs += "&nbsp;" + "Differential Feature Inclusion Analysis (DFI) Summary:" + "</span></td></tr>";
                                for(EnumData data : hmDFI.values()) {
                                    String entry = "<tr>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYDFI + data.id + "')\" ";
                                    entry += "style=\"height:32px;width:32px;";
                                    entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fa.png");
                                    entry += "\"></td>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYDFI + data.id + "')\"";
                                    entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                                    subTabs += entry;
                                    padding = "0";
                                }
                            }
                        }
                        else if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA)) {
                            // Differential Feature Inclusion 
                            String padding = "7";
                            if(!hmDFI.isEmpty()) {
                                // display non-clickable header
                                subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                                subTabs += "<span style=\"font-size:medium;\">DFI Results</span>";
                                subTabs += " - <span style=\"font-size:small;\">";
                                subTabs += "&nbsp;" + "Differential Feature Inclusion Analysis (DFI) Results:" + "</span></td></tr>";
                                for(EnumData data : hmDFI.values()) {
                                    String entry = "<tr>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSDFI + data.id + "')\" ";
                                    entry += "style=\"height:32px;width:32px;";
                                    entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fa.png");
                                    entry += "\"></td>";
                                    entry += "<td style=\"padding-top:" + padding + "px;\">";
                                    entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSDFI + data.id + "')\"";
                                    entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                                    subTabs += entry;
                                    padding = "0";
                                }
                            }
                        }
                    }
                    
                }else
                    continue;
            }*/
            else if(sti.subTabId.equals(TabProjectData.Panels.STATSDPA.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYDPAGENE.name())) {
                if(!hasDPAData)
                    continue;
            }
            else if(sti.subTabId.equals(TabProjectData.Panels.STATSUTRL.name()) ||
                    sti.subTabId.equals(TabProjectDataViz.Panels.SUMMARYUTRL.name())) {
                if(!hasUTRLData)
                    continue;
            }
            if(!sti.group.isEmpty() && !sti.group.equals(lastGroup)) {
                if(!lastGroup.isEmpty())
                    subTabs += "<tr><td colspan=\"2\">&nbsp;</td></tr>";
                subTabs += "<tr style=\"margin: 20px;\">";
                subTabs += "<td colspan=\"2\" style=\"background-color:#a2ca72; padding:5px;\">";
                subTabs += "<span style=\"font-size:medium; color:white\">" + sti.group + "</span>";
                subTabs += "</td>";
                subTabs += "</tr>";
                lastGroup = sti.group;
                pad = true;
            }
            subTabs += "<tr>";
            String imgName = sti.imgName;
            if(imgName.isEmpty())
                imgName = "subTab.png";
            String padding = "0";
            if(pad) {
                pad = false;
                padding = "7";
            }

            subTabs += "<td style=\"padding-top:" + padding + "px;\">";
            subTabs += "<img onClick=\"setSubTabId('" + sti.subTabId + "')\" ";
            subTabs += "style=\"height:32px;width:32px;";
            subTabs += "\" src=\"" + Tappas.class.getResource("/tappas/images/" + imgName);
            subTabs += "\"></td>";
            subTabs += "<td style=\"padding-top:" + padding + "px;\">";
            subTabs += "<span class=\"clickTab\" onClick=\"setSubTabId('" + sti.subTabId + "')\"";
            subTabs += "style=\"font-size:medium;";
            subTabs += "\">&nbsp;" + sti.title;
            subTabs += "</span> - <span class=\"clickTab\" onClick=\"setSubTabId('" + sti.subTabId + "')\"";
            subTabs += "style=\"font-size:small;\">";
            subTabs += "&nbsp;" + sti.description + "</span></td>";
            subTabs += "</tr>";
        }
        if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA) || tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV)) {
            hmFDA.clear();
            ArrayList<EnumData> lst = project.data.analysis.getFDAResultsList();
            for(EnumData ed : lst)
                hmFDA.put(ed.id + "\t" + ed.name, ed);

            hmDFI.clear();
            lst = project.data.analysis.getDFIResultsList();
            for(EnumData ed : lst)
                hmDFI.put(ed.id + "\t" + ed.name, ed);
            System.out.println("Updated FDA and DFI entries in Plus tab");

            if(!hmFDA.isEmpty() || !hmDFI.isEmpty()) {
                // Annotation Feature Analysis - dynamic results
                subTabs += "<tr><td colspan=\"2\">&nbsp;</td></tr>";
                subTabs += "<tr style=\"margin: 20px;\">";
                subTabs += "<td colspan=\"2\" style=\"background-color:#a2ca72; padding:5px;\">";
                subTabs += "<span style=\"font-size:medium; color:white\">" + "Annotation Feature Analysis" + "</span>";
                subTabs += "</td>";
                subTabs += "</tr>";
                if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV)) {
                    // FDA
                    String padding = "7";
                    if(!hmFDA.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">FDA Summary</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Functional Diversity Analysis (FDA) Summary:" + "</span></td></tr>";
                        for(EnumData data : hmFDA.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + TabProjectDataViz.Panels.SUMMARYFDA + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fa.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + TabProjectDataViz.Panels.SUMMARYFDA + data.id + "')\"";

                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }

                    // Differential Feature Inclusion
                    padding = "7";
                    if(!hmDFI.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\"></td></tr>";
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">DFI Summary</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Differential Feature Inclusion Analysis (DFI) Summary:" + "</span></td></tr>";
                        for(EnumData data : hmDFI.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYDFI + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fa.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYDFI + data.id + "')\"";
                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }
                }
                else if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA)) {
                    // Functional Enrichment Analysis
                    String padding = "7";
                    if(!hmFDA.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">FDA Results</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Functional Diversity Analysis (FDA) Results:" + "</span></td></tr>";
                        for(EnumData data : hmFDA.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSFDA + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fa.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSFDA + data.id + "')\"";
                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }

                    // Gene Set Enrichment Analysis
                    padding = "7";
                    if(!hmDFI.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\"></td></tr>";
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">DFI Results</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Differential Feature Inclusion Analysis (DFI) Statistical Results:" + "</span></td></tr>";
                        for(EnumData data : hmDFI.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSDFI + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/fa.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSDFI + data.id + "')\"";
                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }
                }
            }

            hmFEA.clear();
            lst = project.data.analysis.getFEAResultsList();
            for(EnumData ed : lst)
                hmFEA.put(ed.id + "\t" + ed.name, ed);

            hmGSEA.clear();
            lst = project.data.analysis.getGSEAResultsList();
            for(EnumData ed : lst)
                hmGSEA.put(ed.id + "\t" + ed.name, ed);
            System.out.println("Updated FEA and GSEA entries in Plus tab");
            
            if(!hmFEA.isEmpty() || !hmGSEA.isEmpty()) {
                // Enrichment Analysis - dynamic results
                subTabs += "<tr><td colspan=\"2\">&nbsp;</td></tr>";
                subTabs += "<tr style=\"margin: 20px;\">";
                subTabs += "<td colspan=\"2\" style=\"background-color:#a2ca72; padding:5px;\">";
                subTabs += "<span style=\"font-size:medium; color:white\">" + "Enrichment Analysis" + "</span>";
                subTabs += "</td>";
                subTabs += "</tr>";
                if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV)) {
                    // Functional Enrichment Analysis
                    String padding = "7";
                    if(!hmFEA.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">FEA Summary</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Functional Enrichment Analysis (FEA) Summary:" + "</span></td></tr>";
                        for(EnumData data : hmFEA.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYFEA + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/ea.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYFEA + data.id + "')\"";
                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }

                    // Gene Set Enrichment Analysis
                    padding = "7";
                    if(!hmGSEA.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\"></td></tr>";
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">GSEA Summary</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Gene Set Enrichment Analysis (GSEA) Summary:" + "</span></td></tr>";
                        for(EnumData data : hmGSEA.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYGSEA + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/ea.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectDataViz.Panels.SUMMARYGSEA + data.id + "')\"";
                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }
                }
                else if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA)) {
                    // Functional Enrichment Analysis
                    String padding = "7";
                    if(!hmFEA.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">FEA Results</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Functional Enrichment Analysis (FEA) Statistical Results:" + "</span></td></tr>";
                        for(EnumData data : hmFEA.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSFEA + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/ea.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSFEA + data.id + "')\"";
                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }

                    // Gene Set Enrichment Analysis
                    padding = "7";
                    if(!hmGSEA.isEmpty()) {
                        // display non-clickable header
                        subTabs += "<tr><td colspan=\"2\"></td></tr>";
                        subTabs += "<tr><td colspan=\"2\" style=\"padding-top:" + padding + "px;\">";
                        subTabs += "<span style=\"font-size:medium;\">GSEA Results</span>";
                        subTabs += " - <span style=\"font-size:small;\">";
                        subTabs += "&nbsp;" + "Gene Set Enrichment Analysis (GSEA) Statistical Results:" + "</span></td></tr>";
                        for(EnumData data : hmGSEA.values()) {
                            String entry = "<tr>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;&nbsp;<img onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSGSEA + data.id + "')\" ";
                            entry += "style=\"height:32px;width:32px;";
                            entry += "\" src=\"" + Tappas.class.getResource("/tappas/images/ea.png");
                            entry += "\"></td>";
                            entry += "<td style=\"padding-top:" + padding + "px;\">";
                            entry += "&nbsp;<span class=\"clickTab\" onClick=\"setSubTabId('" + '!' + TabProjectData.Panels.STATSGSEA + data.id + "')\"";
                            entry += " style=\"font-size:medium;\">" + data.name + "</span></td></tr>";
                            subTabs += entry;
                            padding = "0";
                        }
                    }
                }
            }
        }
        else if(tabBase.tabId.equals(Tabs.TAB_AI)) {
        }
        html += subTabs + "<table></div>";
        if(tabBase.tabId.startsWith(Tabs.TAB_PROJECTDV) || tabBase.tabId.startsWith(Tabs.TAB_PROJECTDATA))
            html += "<br/><br/><div><span style=\"font-size:small; color:gray;\">Note: Only sub tabs that have data available are shown.</div><br/>\n";
        else
            html +="<br/>";
        html += "</body>\n";
        System.out.println("\n" + html + "\n");
        return html;
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
