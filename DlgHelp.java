/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.nio.file.Paths;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgHelp {
    static HashMap<String, DlgHelpData> dlgs = new HashMap<>();
    
    Dialog dialog;
    WebView web;
    WebEngine engine;

    public void show(String title, String htmlFile, Window window) {
        App app = Tappas.getApp();
        
        // check if help dialog is already opened, try to give focus if so
        if(dlgs.containsKey(htmlFile)) {
            DlgHelpData data = dlgs.get(htmlFile);
            try {
                // return if OK, otherwise will flow to creating a new dialog
                data.dlg.getDialogPane().getScene().getWindow().requestFocus();
                return;
            }
            catch(Exception e) { dlgs.remove(htmlFile); }
        }

        // create help dialog
        dialog = new Dialog();
        dialog.setTitle(title);
        dlgs.put(htmlFile, new DlgHelpData(title, dialog));
        dialog.initModality(Modality.NONE);
        dialog.initStyle(StageStyle.DECORATED);
        dialog.initOwner(window);
        dialog.setResizable(true);
        dialog.getDialogPane().autosize();
        dialog.setOnHidden((event) -> { dlgs.remove(htmlFile); });
        try {
            // get help content or use Help_NA.html if none available
            Node node = (Node) FXMLLoader.load(Tappas.class.getResource("/tappas/dialogs/Help.fxml"));
            dialog.getDialogPane().setContent(node);
            web = (WebView) node.getParent().lookup("#webHelp");
            engine = web.getEngine();
            String content = app.data.getFileContentFromResource("/tappas/resources/web/" + htmlFile);
            if(content.isEmpty())
                content = app.data.getFileContentFromResource("/tappas/resources/web/Help_NA.html");
            
            // handle includes and special tags
            content = replaceSharedSections(content);
            content = app.resources.replaceHTMLAllResources(content);
            
            // display contents
            engine.loadContent(content);
            web.setOnMouseClicked((event) -> {
                if(event.getButton() == MouseButton.PRIMARY) {
                    String cmd = (String) web.getEngine().executeScript("getCmdRequest()");
                    if(!cmd.trim().isEmpty()) {
                        showPage(cmd);
                        dialog.close();
                    }
                }
            });
            ButtonType btnOK = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnOK); 
            dialog.show();
        } catch(Exception e) {
            app.logError("Unable to create help dialog: " + e.getMessage());
        }
    }
    
    //
    // Internal Functions
    //
    
    private void showPage(String url) {
        App app = Tappas.getApp();
        
        // handle web and local content accordingly
        if(url.startsWith("http"))
            Tappas.getHost().showDocument(url);
        else {
            String filepath = Paths.get(app.data.getAppDataFolder(), "web", url).toString();
            app.logger.logDebug("web FP: " + filepath);
            String weburl;
            if(Utils.isWindowsOS()) {
                // need an additional slash when using drive letters
                weburl = "/" + filepath.replaceAll("\\\\", "/");
                System.out.println("fp: " + url);
            }
            else
                weburl = filepath;
            weburl = "file://" + weburl;
            app.logger.logDebug("Web URL: " + weburl);
            Tappas.getHost().showDocument(weburl);
        }
    }
    private String replaceSharedSections(String html) {
        String newHtml = "";
        String lines[] = html.split("\\n");
        for(String line : lines) {
            if(line.contains("<!-- RowColumns -->"))
                line = "<span class=\"notes\" style=\"padding-left:25px; padding-bottom:10px;\">All project data tables include row selection (<input type=\"checkbox\" style=\"vertical-align:middle; font-size:x-small;\" disabled>) and sequential id (#) columns</span><br/>";
            else if(line.contains("<!-- HiddenColumns -->"))
                line = "<p>The following table columns are NOT displayed by default:</p>\n";
            else if(line.contains("<!-- DVReminders -->"))
                line = "<p>" + replaceSharedSections("<!-- DVRemindersAdd -->") + "</p>";
            else if(line.contains("<!-- DVRemindersAdd -->")) {
                line = "Keep in mind that if your application window size is not big enough, the legend for some of the charts may not be displayed. ";
                line += "Don't forget to take advantage of mouseover and context-sensitive menus for additional information and functionality.";
            }
            else if(line.contains("<!-- AddHelp:"))
                line = getAddHelpHtml(line);
            else if(line.contains("<!-- SubtabMenuBarHeader"))
                line = "<br/><b><font color='#a2ca72'>Subtab Menu Bar</font></b><p>In addition to the Help button, the following menu buttons are provided:</p><table>";
            else if(line.contains("<!-- SubtabMenuBar:"))
                line = getSubtabMenuBarHtml(line, true);
            else if(line.contains("<!-- SubtabMenuBarNoHeader:"))
                line = getSubtabMenuBarHtml(line, false);
            newHtml += line + "\n";
        }
        return newHtml;
    }
    private String getAddHelpHtml(String line) {
        String newLine = "<br/><b><font color='#a2ca72'>Additional Help</font></b><ul>";
        String[] info = line.split(":");
        if(info.length == 2) {
            String[] fields = info[1].split(",");
            for(String field : fields) {
                String[] vals = field.split(" ");
                switch (vals[0]) {
                    case "gui":
                        newLine += "\n<li>For overview of the application user interface see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageAppXface.html#Top')\">";
                        newLine += " Application Interface</span> section in Overview</li>\n";
                        break;
                    case "projects":
                        newLine += "\n<li>For overview of application projects see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageProjects.html#Top')\">";
                        newLine += " Projects</span> section in Overview</li>\n";
                        break;
                    case "afFormat":
                        newLine += "\n<li>For annotation file format see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageProjects.html#AnnotationFileFormat')\">";
                        newLine += " Annotation File Format</span> section in Overview</li>\n";
                        break;
                    case "emFormat":
                        newLine += "\n<li>For expression matrix file format see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageProjects.html#ExpMatrixFileFormat')\">";
                        newLine += " Expression Matrix File Format</span> section in Overview</li>\n";
                        break;
                    case "tips":
                        newLine += "\n<li>For tips on using the application see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageAppTips.html#Tips')\">";
                        newLine += " Application Tips</span> section in Overview</li>\n";
                        break;
                    case "tables":
                        newLine += "\n<li>For help with data tables see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageAppXface.html#Tables')\">";
                        newLine += " Application Interface - Tables</span> section in Overview</li>\n";
                        break;
                    case "query":
                        newLine += "\n<li>For help with searching and filtering data tables see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageQuery.html#Top')\">";
                        newLine += " Ad Hoc Query</span> section in Overview</li>\n";
                        break;
                    case "genedv":
                        newLine += "\n<li>For help with gene data visualization see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageDataViz.html#GeneDV')\">";
                        newLine += " Data Visualization</span> section in Overview</li>\n";
                        break;
                    case "export":
                        newLine += "\n<li>For help with exporting data tables see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageExport.html#Top')\">";
                        newLine += " Export Data and Images</span> section in Overview</li>\n";
                        break;
                    case "visual":
                        newLine += "\n<li>For help with visual display controls see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageAppXface.html#Visual')\">";
                        newLine += " Visual Display Controls</span> section in Overview</li>\n";
                        break;
                    case "menus":
                        newLine += "\n<li>For help with context-sensitive menus see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageAppXface.html#Menus')\">";
                        newLine += " Context-Sensitive Menus</span> section in Overview</li>\n";
                        break;
                    case "inpdata":
                        newLine += "\n<li>For help with input data and filtering see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageProjects.html#InputData')\">";
                        newLine += " Input Data and Filtering</span> section in Overview</li>\n";
                        break;
                    case "dea":
                        newLine += "\n<li>For help with DEA see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageDEA.html#Top')\">";
                        newLine += " Differential Expression Analysis</span> section in Overview</li>\n";
                        break;
                    case "diu":
                        newLine += "\n<li>For help with DIU see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageDIU.html#Top')\">";
                        newLine += " Differential Isoform Usage </span> section in Overview</li>\n";
                        break;
                    case "dfi":
                        newLine += "\n<li>For help with DFI see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageAFA.html#DFI')\">";
                        newLine += " Annotation Features DIU</span> section in Overview</li>\n";
                        break;
                    case "fda":
                        newLine += "\n<li>For help with FDA see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageAFA.html#FDA')\">";
                        newLine += " Functional Diversity Analysis</span> section in Overview</li>\n";
                        break;
                    case "fea":
                        newLine += "\n<li>For help with FEA see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageEA.html#FEA')\">";
                        newLine += " Functional Enrichment Analysis</span> section in Overview</li>\n";
                        break;
                    case "feaclusters":
                        newLine += "\n<li>For help with FEA clusters see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageEA.html#FEAClusters')\">";
                        newLine += " Enriched Features Cluster Analysis</span> section in Overview</li>\n";
                        break;
                    case "gsea":
                        newLine += "\n<li>For help with GSEA see the ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageEA.html#GSEA')\">";
                        newLine += " Gene Set Enrichment Analysis</span> section in Overview</li>\n";
                        break;
                    case "noiseq":
                        newLine += "\n<li>To see the <span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/NOISeq.html')\">NOISeq</span> package documentation, click ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/NOISeq.html')\">here.</span></li>";
                        break;
                    case "edger":
                        newLine += "\n<li>To see the <span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/edgeR.html')\">edgeR</span> package documentation, click ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/edgeR.html')\">here.</span></li>";
                        break;
                    case "dexseq":
                        newLine += "\n<li>To see the <span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/DEXSeq.html')\">DEXSeq</span> package documentation, click ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/DEXSeq.html')\">here.</span></li>";
                        break;
                    case "goseq":
                        newLine += "\n<li>To see the <span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/goseq.html')\">goseq</span> package documentation, click ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://www.bioconductor.org/packages/release/bioc/html/goseq.html')\">here.</span></li>";
                        break;
                    case "goglm":
                        newLine += "\n<li>To see the <span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://github.com/gu-mi/GOglm')\">GOglm</span> package documentation, click ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://github.com/gu-mi/GOglm')\">here.</span></li>";
                        break;
                    case "gff3":
                        newLine += "\n<li>To see the <span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://github.com/The-Sequence-Ontology/Specifications/blob/master/gff3.md')\">Generic Feature Format Version 3 (GFF3)</span> specifications, click ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('https://github.com/The-Sequence-Ontology/Specifications/blob/master/gff3.md')\">here.</span></li>";
                        break;
                    case "go":
                        newLine += "\n<li>To see the <span class=\"fakeAnchor\" onclick=\"setCmdRequest('http://www.geneontology.org/')\">Gene Ontology</span> references, click ";
                        newLine += "<span class=\"fakeAnchor\" onclick=\"setCmdRequest('http://www.geneontology.org/')\">here.</span></li>";
                        break;
                }
            }
        }
        newLine += "</ul>";
        return newLine;
    }
    private String getSubtabMenuBarHtml(String line, boolean header) {
        String newLine = header? "<br/><b><font color='#a2ca72'>Subtab Menu Bar</font></b><p>In addition to the Help button, the following menu buttons are provided:</p><table>" : "<table>";
        String[] info = line.split(":");
        if(info.length == 2) {
            String[] fields = info[1].split(",");
            for(String field : fields) {
                String[] vals = field.split(" ");
                switch (vals[0]) {
                    case "optlog":
                        newLine += "\n<tr><td style=\"vertical-align:top;\">";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/optionsButton.png\" width=\"28\" height=\"28\" alt=\"Menu selections...\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- menu with the following selections:<ul><li>View Analysis Log - displays the analysis log</li></ul>";
                        newLine += "\n</td></tr>";
                        break;
                    case "optdbg":
                        newLine += "\n<tr><td style=\"vertical-align:top;\">";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/optionsButton.png\" width=\"28\" height=\"28\" alt=\"Menu selections...\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- menu with the following selections:<ul><li>Log debug messages - check to log application debug messages. WARNING do not turn this feature on unless needed and make sure to uncheck when done</li></ul>";
                        newLine += "\n</td></tr>";
                        break;
                    case "select":
                        newLine += "\n<tr><td>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/selectButton.png\" width=\"28\" height=\"28\" alt=\"Table row selection...\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- table row selection management menu. See <span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageQuery.html#Top')\">Ad Hoc Query</span> section in Overview";
                        newLine += "\n</td></tr>";
                        break;
                    case "export":
                        newLine += "\n<tr><td>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/exportButton.png\" width=\"28\" height=\"28\" alt=\"Export data or images...\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- export data or images to file. See <span class=\"fakeAnchor\" onclick=\"setCmdRequest('pageExport.html#Top')\">Export Data and Images</span> section in Overview";
                        newLine += "\n</td></tr>";
                        break;
                    case "dataViz":
                        newLine += "\n<tr><td>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/dataVizButton.png\" width=\"28\" height=\"28\" alt=\"Data visualization options...\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- data visualization menu will show menu selections for all relevant data visualization subtabs";
                        newLine += "\n</td></tr>";
                        break;
                    case "clusters":
                        newLine += "\n<tr><td>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/clustersDataButton.png\" width=\"28\" height=\"28\" alt=\"FEA  Clusters...\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- FEA clusters menu with selections to view clusters or run cluster analysis";
                        newLine += "\n</td></tr>";
                        break;
                    case "sigLevel":
                        newLine += "\n<tr><td>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/sigLevelButton.png\" width=\"28\" height=\"28\" alt=\"Change significance level.\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- change significance level, will recalculate analysis results";
                        newLine += "\n</td></tr>";
                        break;
                    case "rerun":
                        newLine += "\n<tr><td>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/rerunButton.png\" width=\"28\" height=\"28\" alt=\"Rerun analysis...\"/></div>";
                        newLine += "</td>\n<td>";
                        newLine += "&nbsp;- rerun analysis, will allow changing analysis parameters";
                        newLine += "\n</td></tr>";
                        break;
                    case "zoom":
                        newLine += "\n<tr><td><div class=\"subtabButton\" style=\"height:90px;\">";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/minusButton.png\" width=\"24\" height=\"24\" alt=\"Minus\"/></div>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/fitButton.png\" width=\"24\" height=\"24\" alt=\"Fit\"/></div>";
                        newLine += "<div class=\"subtabButton\"><img src=\"images/plusButton.png\" width=\"24\" height=\"24\" alt=\"Plus\"/></div>";
                        newLine += "</div></td>\n<td>";
                        newLine += "&nbsp;- use [-] to zoom out, [+] to zoom in, and [Fit] to display image so it fits in available space";
                        newLine += "\n</td></tr>";
                        break;
                }
            }
        }
        newLine += "</table>";
        return newLine;
    }
    
    //
    // Data Classes
    //
    public class DlgHelpData {
        String title;
        Dialog dlg;
        public DlgHelpData(String title, Dialog dlg) {
            this.title = title;
            this.dlg = dlg;
        }
    }
}
