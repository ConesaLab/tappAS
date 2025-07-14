/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import tappas.DataApp.DataType;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class AppFXMLDocumentController implements Initializable {
    //
    // Main Panels
    //
    @FXML
    private TabPane tabPaneMain;
    @FXML
    private TabPane tabPaneBottom;
    @FXML
    private VBox vboxApp;
    @FXML
    private VBox vboxAppInstall;

    //
    // Top Toolbar
    //
    @FXML
    private ImageView imgArrow;
    @FXML
    private Pane paneSeeAppLog;
    @FXML
    private TextField txtSearch;
    @FXML
    private CheckBox chkHideUnselected;
    @FXML
    private WebView webStart;
    @FXML
    private MenuButton btnProjects_Menu;
    @FXML
    private MenuButton btnProjectData_Menu;
    @FXML
    private MenuButton btnDA_Menu;
    @FXML
    private MenuButton btnEA_Menu;
    @FXML
    private MenuButton btnFDA_Menu;
    @FXML
    private MenuButton btnFA_Menu;
    @FXML
    private MenuButton btnProjectDataViz_Menu;
    @FXML
    private MenuItem itemProjectDataViz_AF;
    @FXML
    private MenuItem itemProjectDataViz_Data;
    @FXML
    private MenuItem itemProjectDataViz_DA;
    @FXML
    private MenuItem itemProjectDataViz_EA;
    @FXML
    private MenuItem itemProjectDataViz_EM;
    @FXML
    private MenuItem itemProjectDataViz_Tool_VennDiag;
    @FXML
    private MenuItem itemProject_InputData;
    
    //
    // Data Menus and Items
    //
    
    @FXML
    private Menu menuDA_RunAnalysis;
    @FXML
    private MenuItem itemDA_RunDEA;
    @FXML
    private MenuItem itemDA_RunDIU;
    @FXML
    private MenuItem itemDA_Results;
    @FXML
    private Menu menuDA_DEAResults;
    @FXML
    private MenuItem itemDA_StatsDEATrans;
    @FXML
    private MenuItem itemDA_StatsDEAProteins;
    @FXML
    private MenuItem itemDA_StatsDEAGenes;
    @FXML
    private Menu menuDA_DIUResults;
    @FXML
    private MenuItem itemDA_StatsDIUTrans;
    @FXML
    private MenuItem itemDA_StatsDIUProteins;
    @FXML
    private Menu menuDA_ClearDEA;
    @FXML
    private MenuItem itemDA_ClearDEATrans;
    @FXML
    private MenuItem itemDA_ClearDEAProteins;
    @FXML
    private MenuItem itemDA_ClearDEAGenes;
    @FXML
    private MenuItem itemDA_ClearDEAAll;
    @FXML
    private Menu menuDA_ClearDIU;
    @FXML
    private MenuItem itemDA_ClearDIUTrans;
    @FXML
    private MenuItem itemDA_ClearDIUProteins;
    @FXML
    private MenuItem itemDA_ClearAll;

    @FXML
    private Menu menuEA_RunAnalysis;
    @FXML
    private MenuItem itemEA_RunGSEA;
    @FXML
    private MenuItem itemEA_RunFEA;
    @FXML
    private Menu menuEA_GSEAResults;
    @FXML
    private Menu menuEA_FEAResults;
    @FXML
    private Menu menuEA_ClearGSEA;
    @FXML
    private Menu menuEA_ClearFEA;
    @FXML
    private MenuItem itemEA_ClearAll;

    @FXML
    private MenuItem itemMA_RunFDA;
//    @FXML
//    private MenuItem itemMA_ViewStatsFDA;
//    @FXML
//    private MenuItem itemMA_ClearFDA;
    @FXML
    private Menu menuMA_FDAResults;
    @FXML
    private MenuItem itemMA_Results;
    @FXML
    private Menu menuMA_ClearFDA;
    
    @FXML
    private MenuItem menuFA_RunAnalysis;
    @FXML
    private MenuItem itemFA_RunDFI;
    @FXML
    private MenuItem itemFA_RunDPA;
    @FXML
    private MenuItem itemFA_RunUTRL;
    @FXML
    private MenuItem menuFA_ViewDFI;
    
    @FXML
    private Menu menuFA_DFIResults;
    @FXML
    private Menu menuFA_DFIResultsSummary;
    @FXML
    private Menu menuFA_CoDFIAssociations;
    @FXML
    private Menu menuFA_ClearDFI;
//    @FXML
//    private MenuItem menuFA_ClearDFI;
//    @FXML
//    private MenuItem itemFA_StatsDFI;
//    @FXML
//    private MenuItem itemFA_SummaryDFI;
//    @FXML
//    private MenuItem itemFA_AssociationDFI;
    @FXML
    private MenuItem menuFA_ViewDPA;
    @FXML
    private MenuItem menuFA_ViewUTRL;
    @FXML
    private MenuItem itemFA_StatsDPA;
    @FXML
    private MenuItem itemFA_StatsUTRL;
    @FXML
    private MenuItem itemFA_ClearDFI;
    @FXML
    private MenuItem itemFA_ClearDPA;
    @FXML
    private MenuItem itemFA_ClearUTRL;
    @FXML
    private MenuItem itemFA_ClearAll;
    
    // class data
    App app;
    private boolean ignoreSearchEvent = false;
    private final Image imgArrowOn = new Image(getClass().getResourceAsStream("/tappas/images/tbArrow.png"));
    private final Image imgArrowOff = new Image(getClass().getResourceAsStream("/tappas/images/tbArrowOff.png"));
    
    // get shift key state
    private boolean shiftKey = false;
    public boolean isShiftKeyDown() { return shiftKey; }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        app = Tappas.getApp();
        app.ctlr.setFXDC(this);
        
        // assign images to split buttons
        setupMenuButon(btnProjects_Menu, "project.png");
        setupMenuButon(btnDA_Menu, "da.png");
        setupMenuButon(btnEA_Menu, "ea.png");
        setupMenuButon(btnFDA_Menu, "fda.png");
        setupMenuButon(btnFA_Menu, "fa.png");
        setupMenuButon(btnProjectDataViz_Menu, "graphs.png");
        setupMenuButon(btnProjectData_Menu, "data.png");
        
        // setup tooltip for log warning
        Tooltip tooltip = new Tooltip();
        tooltip.setText("A warning or error message\nhas been logged.\nSee application log for details...\n");
        Tooltip.install(paneSeeAppLog, tooltip);
        
        // initialize app and setup main menu button handlers
        app.initialize(tabPaneMain, tabPaneBottom);
        btnProjects_Menu.showingProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue) {
                ArrayList<MenuItem> lstItems = app.ctlr.getProjectsMenuItems();
                btnProjects_Menu.getItems().clear();
                btnProjects_Menu.getItems().addAll(lstItems);
            }
        });        
        btnDA_Menu.showingProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue) {
                AppController.DAMenuFlags flags = app.ctlr.getDAMenuFlags();
                itemDA_RunDEA.setDisable(!flags.runDEA);
                itemDA_RunDIU.setDisable(!flags.runDIU);
                menuDA_RunAnalysis.setDisable(!(flags.runDEA || flags.runDIU));
                if(flags.multipleTimes)
                    itemDA_Results.setVisible(false);
                else {
                    itemDA_Results.setVisible(true);
                    itemDA_Results.setDisable(!flags.hasAny);
                }
                itemDA_StatsDEATrans.setDisable(!flags.hasDEA_Trans);
                itemDA_StatsDEAProteins.setDisable(!flags.hasDEA_Protein);
                itemDA_StatsDEAGenes.setDisable(!flags.hasDEA_Gene);
                menuDA_DEAResults.setDisable(!flags.hasDEA);
                itemDA_StatsDIUTrans.setDisable(!flags.hasDIU_Trans);
                itemDA_StatsDIUProteins.setDisable(!flags.hasDIU_Protein);
                menuDA_DIUResults.setDisable(!flags.hasDIU);
                itemDA_ClearDEATrans.setDisable(!flags.hasDEA_Trans);
                itemDA_ClearDEAProteins.setDisable(!flags.hasDEA_Protein);
                itemDA_ClearDEAGenes.setDisable(!flags.hasDEA_Gene);
                itemDA_ClearDEAAll.setDisable(!flags.hasDEA);
                menuDA_ClearDEA.setDisable(!flags.hasDEA);
                menuDA_ClearDIU.setDisable(!flags.hasDIU);
                itemDA_ClearDIUTrans.setDisable(!flags.hasDIU_Trans);
                itemDA_ClearDIUProteins.setDisable(!flags.hasDIU_Protein);
                itemDA_ClearAll.setDisable(!flags.hasAny);
            }
        });
        btnEA_Menu.showingProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue) {
                AppController.EAMenuFlags flags = app.ctlr.getEAMenuFlags();
                itemEA_RunGSEA.setDisable(!flags.runGSEA);
                itemEA_RunFEA.setDisable(!flags.runFEA);
                menuEA_RunAnalysis.setDisable(!(flags.runFEA || flags.runGSEA));
                menuEA_GSEAResults.setDisable(!flags.hasGSEA);
                menuEA_FEAResults.setDisable(!flags.hasFEA);
                menuEA_ClearGSEA.setDisable(!flags.hasGSEA);
                menuEA_ClearFEA.setDisable(!flags.hasFEA);
                itemEA_ClearAll.setDisable(!flags.hasAny);
                
                menuEA_GSEAResults.getItems().clear();
                menuEA_GSEAResults.getItems().addAll(flags.lstGSEAViewItems);
                menuEA_ClearGSEA.getItems().clear();
                menuEA_ClearGSEA.getItems().addAll(flags.lstGSEAClearItems);
                menuEA_FEAResults.getItems().clear();
                menuEA_FEAResults.getItems().addAll(flags.lstFEAViewItems);
                menuEA_ClearFEA.getItems().clear();
                menuEA_ClearFEA.getItems().addAll(flags.lstFEAClearItems);
            }
        });        
        btnFDA_Menu.showingProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue) {
                AppController.MAMenuFlags flags = app.ctlr.getMAMenuFlags();
                itemMA_RunFDA.setDisable(!flags.runDiversity);
//                itemMA_ViewStatsFDA.setDisable(!flags.viewDiversity);
//                itemMA_ClearFDA.setDisable(!flags.viewDiversity);

                //we have to have 2 features analyzed by ID with pos and genomic
                itemMA_Results.setDisable(!flags.hasTwoData);

                menuMA_FDAResults.setDisable(!flags.hasFDA);
                menuMA_ClearFDA.setDisable(!flags.hasFDA);
                
                menuMA_FDAResults.getItems().clear();
                menuMA_FDAResults.getItems().addAll(flags.lstFDAViewItems);
                menuMA_ClearFDA.getItems().clear();
                menuMA_ClearFDA.getItems().addAll(flags.lstFDAClearItems);
            }
        });
        btnFA_Menu.showingProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue) {
                AppController.FAMenuFlags flags = app.ctlr.getFAMenuFlags();
                itemFA_RunDFI.setDisable(!flags.runDFI);
                menuFA_DFIResults.setDisable(!flags.hasDFI);
                menuFA_DFIResultsSummary.setDisable(!flags.hasDFI);
                menuFA_CoDFIAssociations.setDisable(!flags.hasDFI);
                menuFA_ClearDFI.setDisable(!flags.hasDFI);
                
                itemFA_RunDPA.setDisable(!flags.runDPA);
                itemFA_StatsDPA.setDisable(!flags.hasDPA);
                itemFA_ClearDPA.setDisable(!flags.hasDPA);
                
                itemFA_RunUTRL.setDisable(!flags.runUTRL);
                itemFA_StatsUTRL.setDisable(!flags.hasUTRL);
                itemFA_ClearUTRL.setDisable(!flags.hasUTRL);
                
                itemFA_ClearAll.setDisable(!flags.hasAny);
                
                menuFA_RunAnalysis.setDisable(!(flags.runDFI || flags.runDPA));
                menuFA_ViewDFI.setDisable(!flags.hasDFI);
                menuFA_ViewDPA.setDisable(!flags.hasDPA);
                menuFA_ViewUTRL.setDisable(!flags.hasUTRL);
                
                menuFA_DFIResults.getItems().clear();
                menuFA_DFIResults.getItems().addAll(flags.lstDFIViewItems);
                menuFA_DFIResultsSummary.getItems().clear();
                menuFA_DFIResultsSummary.getItems().addAll(flags.lstDFISummaryItems);
                menuFA_CoDFIAssociations.getItems().clear();
                menuFA_CoDFIAssociations.getItems().addAll(flags.lstCoDFIViewItems);
                menuFA_ClearDFI.getItems().clear();
                menuFA_ClearDFI.getItems().addAll(flags.lstDFIClearItems);
            }
        });        
        btnProjectDataViz_Menu.showingProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue) {
                AppController.GraphsMenuFlags flags = app.ctlr.getGraphsMenuFlags();
                itemProjectDataViz_AF.setDisable(!flags.hasData);
                itemProjectDataViz_Data.setDisable(!flags.hasData);
                itemProjectDataViz_DA.setDisable(!flags.hasDA);
                itemProjectDataViz_EA.setDisable(!flags.hasEA);
                itemProjectDataViz_EM.setDisable(!flags.hasData);
                itemProjectDataViz_Tool_VennDiag.setDisable(!flags.toolVennDiag);
            }
        });        
    }
    public void showAppPane(boolean flg) { vboxApp.setVisible(flg); }
    
    public void showAppPaneInstalling(boolean flg) { vboxAppInstall.setVisible(flg); }
    
    public void showSeeAppLog(boolean show) { 
        //if false error nullpointer but i dont know if this solution affects tappAS!!!
        if(show==true)
            if(paneSeeAppLog != null)
                paneSeeAppLog.setVisible(show);
            else{
                System.out.println("paneSeeAppLog is null");
            }
    }

    //
    // Start page - HTML content
    //
    public void hideStartPage() { webStart.setVisible(false); };
    public void showStartPage(String html, String version) {
        String htmlPage = app.data.getFileContentFromResource("/tappas/resources/web/Start.html");
        htmlPage = htmlPage.replace("<!--replaceVersion-->", version);
        htmlPage = htmlPage.replace("<!--replaceHTML-->", html);
        htmlPage = app.resources.replaceHTMLAllResources(htmlPage);
        webStart.getEngine().loadContent(htmlPage);
        webStart.setOnMouseClicked((event) -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                String cmd = (String) webStart.getEngine().executeScript("getCmdRequest()");
                System.out.println("cmd: " + cmd);
                if(!cmd.trim().isEmpty()) {
                    DlgHelp dlg;
                    switch(cmd) {
                        case "showMatrixFormat":
                            dlg = new DlgHelp();
                            dlg.show("Expression Matrix File Format", "ExpressionMatrixFileFormat.html", Tappas.getWindow());
                            break;
                        case "showAnnotationFormat":
                            dlg = new DlgHelp();
                            dlg.show("Annotation Features File Format", "AnnotationFileFormat.html", Tappas.getWindow());
                            break;
                        case "showAppReqs":
                            dlg = new DlgHelp();
                            dlg.show("Application Requirements", "AppRequirements.html", Tappas.getWindow());
                            break;
                        default:
                            app.ctlr.processStartCmd(cmd);
                            break;
                    }
                }
            }
        });
        webStart.setVisible(true);
    }

    //
    // Search and Menu Support Functions
    //
    
    public void setupSearch() {
        Image img = new Image( getClass().getResourceAsStream("/tappas/images/xcross.png"));
        ImageView imgv = new ImageView(img);
        imgv.setFitHeight(15);
        imgv.setFitWidth(15);
        txtSearch.textProperty().addListener((ov, oldValue, newValue) -> {
            if(!ignoreSearchEvent)
                app.tabs.search(txtSearch.getText().trim().toLowerCase(), chkHideUnselected.isSelected());
            txtSearch.setStyle(txtSearch.getText().trim().isEmpty()? "" : "-fx-background-color:yellow;");
            System.out.println("textfield changed from " + oldValue + " to " + newValue);
        });
        txtSearch.setOnAction((event) -> { 
            // change here if we add a case sensitive option
            if(!ignoreSearchEvent)
                app.tabs.search(txtSearch.getText().trim().toLowerCase(), chkHideUnselected.isSelected());
        });
        txtSearch.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if (!newValue && !isSearchFocused() && !app.tabs.isSearchTableFocused())
                app.tabs.searchSet(null);
        });
        chkHideUnselected.setOnAction((event) -> {
            if(chkHideUnselected.isSelected())
                chkHideUnselected.setStyle("-fx-background-color: yellow;");
            else
                chkHideUnselected.setStyle("");
            // change here if we add a case sensitive option
            if(!ignoreSearchEvent)
                app.tabs.search(txtSearch.getText().trim().toLowerCase(), chkHideUnselected.isSelected());
        });
        chkHideUnselected.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if (!newValue && !isSearchFocused() && !app.tabs.isSearchTableFocused())
                app.tabs.searchSet(null);
        });
        txtSearch.setDisable(true);
        chkHideUnselected.setDisable(true);
        Tappas.getScene().setOnKeyPressed((KeyEvent e) -> {
            if(e.isShiftDown())
                shiftKey = true;
            if (e.getCode() == KeyCode.F && e.isControlDown()) {
                if(!txtSearch.isDisabled())
                    txtSearch.requestFocus();
            }
            else if (e.getCode() == KeyCode.EQUALS && e.isShortcutDown()) {
                app.tabs.zoomIn();
            }
            else if (e.getCode() == KeyCode.MINUS && e.isShortcutDown()) {
                app.tabs.zoomOut();
            }
            else if (e.getCode() == KeyCode.DIGIT0 && e.isShortcutDown()) {
                app.tabs.zoomFit();
            }
        });
        Tappas.getScene().setOnKeyReleased((KeyEvent e) -> {
            if(!e.isShiftDown())
                shiftKey = false;
        });
    }
    public void searchSet(boolean disable, String txt, boolean hide) {
        if(disable) {
            txt = "";
            txtSearch.setStyle("");
            chkHideUnselected.setStyle("");
        }
        else {
            txtSearch.setStyle(txt.trim().isEmpty()? "" : "-fx-background-color:yellow;");
            chkHideUnselected.setStyle(hide? "-fx-background-color: yellow;" : "");
        }
        
        // we can not trigger events while handling a set request so set flag
        ignoreSearchEvent = true;
        txtSearch.setText(txt);
        txtSearch.setDisable(disable);
        chkHideUnselected.setSelected(hide);
        chkHideUnselected.setDisable(disable);
        ignoreSearchEvent = false;
        System.out.println("FDXC searchset called: '" + txt + "', hide: " + hide);
    }
    public boolean isSearchFocused() {
        Scene scene = Tappas.getScene();
        //if(scene != null && scene.getFocusOwner() != null && scene.getFocusOwner().getId() != null)
        //    System.out.println("scenefid: " + scene.getFocusOwner().getId() + ", " + scene.getFocusOwner().toString());
        return (scene != null && scene.getFocusOwner() != null && scene.getFocusOwner().getId() != null &&
                AppFXMLDocumentController.isSearchControlId(scene.getFocusOwner().getId()));
    }
    public void disableTopMenu(String menuId, boolean flg) {
        switch(menuId) {
            //
            // Top Toolbar SplitButton Menus
            //
            case "Project_Data":
                btnProjectData_Menu.setDisable(flg);
                break;
            case "Project_DataViz":
                btnProjectDataViz_Menu.setDisable(flg);
                break;
            case "DA":
                btnDA_Menu.setDisable(flg);
                break;
            case "EA":
                btnEA_Menu.setDisable(flg);
                break;
            case "FDA":
                btnFDA_Menu.setDisable(flg);
                break;
            case "FA":
                btnFA_Menu.setDisable(flg);
                break;
        }
        if(flg)
            imgArrow.setImage(imgArrowOff);
        else
            imgArrow.setImage(imgArrowOn );
    }
    
    //
    // Mouse Handler
    //
    @FXML
    private void tabPaneOnMouseClicked(MouseEvent event) {
        // the mouse event for tab menu happens after the close request is called and consumed
        System.out.println("tabPane OnMouseClicked: " + event);
        TabPane contextMenuClickTabPane = (TabPane)event.getSource();
        String contextMenuClickTabPaneId = contextMenuClickTabPane.getId();
        if(event.getButton() == MouseButton.PRIMARY) {
            String evtstr = event.toString();
            System.out.println("Primary for " + contextMenuClickTabPaneId + ", src: " + evtstr);
            TabPane tp = null;
            switch (contextMenuClickTabPaneId) {
                case "tabPaneMain":
                    tp = tabPaneMain;
                    break;
                case "tabPaneBottom":
                    tp = tabPaneBottom;
                    break;
            }
            if(tp != null) {
                if(evtstr.contains("styleClass=tab-pane") && evtstr.contains("styleClass=tab-close-button")) {
                    Window wnd = tp.getScene().getWindow();
                    tp.getSelectionModel().getSelectedItem().getContextMenu().show(wnd, event.getScreenX(), event.getScreenY());
                }
            }
        }
    }

    //
    // Application Menu (right top of window)
    //
    @FXML
    private void onActionViewAppInformation(ActionEvent event) {
        app.tabs.openTab(Tabs.TAB_AI, null, null);
    }
    @FXML
    private void onActionAbout(ActionEvent event) {
        Window wnd = Tappas.getScene().getWindow();
        DlgAbout dlg = new DlgAbout(null, wnd);
        dlg.showAndWait();
    }
    
    @FXML
    private void onActionContact(ActionEvent event) {
        Window wnd = Tappas.getScene().getWindow();
        DlgContact dlg = new DlgContact(null, wnd);
        dlg.showAndWait();
    }

    //
    // Project Menu
    //
    @FXML   // action for main button part of Projects split button
    private void onActionProjects(ActionEvent event) {
    }
    @FXML
    private void onActionNewProject(ActionEvent event) {
        app.ctlr.newProject();
    }
    @FXML
    private void onActionOpenProject(ActionEvent event) {
        app.ctlr.openProject();
    }
    @FXML
    private void onActionClearRecentProjects(ActionEvent event) {
        app.ctlr.clearRecentProjects();
    }
    
    //
    // Data Menu
    //
    @FXML private void onActionData(ActionEvent event) { } // action for main button part of Data split button
    @FXML private void onActionLoadInputData(ActionEvent event) { app.ctlr.loadInputData(); }
    @FXML private void onActionProjectDataTrans(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.TRANS); }
    @FXML private void onActionProjectDataProtein(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.PROTEIN); }
    @FXML private void onActionProjectDataGene(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.GENE); }
    @FXML private void onActionProjectDataExpMatrix(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.EXPMATRIX); }
    @FXML
    private void onActionStart(ActionEvent event) {
    }

    //
    // App Log
    //

    @FXML
    private void onSeeAppLog(MouseEvent event) {
        paneSeeAppLog.setVisible(false);
        app.ctlr.openAppLogSubTab();
    }
    
    //
    // Differential Analysis Menus
    //
    @FXML private void onActionDAClearAll(ActionEvent event) { app.ctlr.clearDAAll();  }
    @FXML private void onActionDAClearDEA(ActionEvent event) { app.ctlr.clearDEA();  }
    @FXML private void onActionDAClearDEATrans(ActionEvent event) { app.ctlr.clearDEA(DataType.TRANS, true);  }
    @FXML private void onActionDAClearDEAProteins(ActionEvent event) { app.ctlr.clearDEA(DataType.PROTEIN, true);  }
    @FXML private void onActionDAClearDEAGenes(ActionEvent event) { app.ctlr.clearDEA(DataType.GENE, true);  }
    @FXML private void onActionDAClearDIU(ActionEvent event) { app.ctlr.clearDIUAll(true);  }
    @FXML private void onActionDAClearDIUTrans(ActionEvent event) { app.ctlr.clearDIU(DataType.TRANS, true);  }
    @FXML private void onActionDAClearDIUProteins(ActionEvent event) { app.ctlr.clearDIU(DataType.PROTEIN, true);  }
    @FXML private void onActionDAClearDFI(ActionEvent event) { app.ctlr.clearDFI(true);  }
    @FXML private void onActionDAClearDPA(ActionEvent event) { app.ctlr.clearDPA(true);  }    
    // Run Analysis
    @FXML private void onActionDARunDIU(ActionEvent event) { app.ctlr.runDIUnalysis(null); }
    @FXML private void onActionDARunDEA(ActionEvent event) { app.ctlr.runDEAnalysis(null); }
        // Data
    @FXML private void onActionDAResults(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.DARESULTS); }
    @FXML private void onActionDAStatsDEATrans(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDEATRANS); }
    @FXML private void onActionDAStatsDEAProteins(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDEAPROT); }
    @FXML private void onActionDAStatsDEAGenes(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDEAGENE); }
    @FXML private void onActionDAStatsDIUTrans(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDIUTRANS); }
    @FXML private void onActionDAStatsDIUProteins(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDIUPROT); }
        // Data Visualization
    @FXML private void onActionDASumDIUTrans(ActionEvent event) { app.ctlr.openProjectDataVizSubTab(TabProjectDataViz.Panels.SUMMARYDIUTRANS); }
    @FXML private void onActionDASumDIUProteins(ActionEvent event) { app.ctlr.openProjectDataVizSubTab(TabProjectDataViz.Panels.SUMMARYDIUPROT); }
    @FXML private void onActionDASumDEATrans(ActionEvent event) { app.ctlr.openProjectDataVizSubTab(TabProjectDataViz.Panels.SUMMARYDEATRANS); }
    @FXML private void onActionDASumDEAProteins(ActionEvent event) { app.ctlr.openProjectDataVizSubTab(TabProjectDataViz.Panels.SUMMARYDEAPROT); }
    @FXML private void onActionDASumDEAGenes(ActionEvent event) { app.ctlr.openProjectDataVizSubTab(TabProjectDataViz.Panels.SUMMARYDEAGENE); }
    
    //
    // Enrichment Analysis Menus
    //
    
    // NOTE: we may want to consider using regular buttons instead of split menu buttons
    //       since we really do not have any use for the main area click
    @FXML
    private void onActionEA(ActionEvent event) {
    }
    @FXML
    private void onActionMA(ActionEvent event) {
    }
    @FXML
    private void onActionFA(ActionEvent event) {
    }
    @FXML private void onActionRunGSEA(ActionEvent event) { app.ctlr.runGSEAnalysis(""); }
    @FXML private void onActionRunFEA(ActionEvent event) { app.ctlr.runFEAnalysis(""); }
    @FXML private void onActionEAClearAll(ActionEvent event) { app.ctlr.clearEAAll(); }

    @FXML private void onActionMARunFDA(ActionEvent event) { app.ctlr.runFDAnalysis(""); }
    @FXML private void onActionFAResults(ActionEvent event) { app.ctlr.runFDACombinedResults(""); }
    //@FXML private void onActionFAResults(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSFDAIDCOMBINED); }
    @FXML private void onActionMAStatsFDA(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSFDA); }
    //@FXML private void onActionMAClearFDA(ActionEvent event) { app.ctlr.clearFDA(true); }
    
    @FXML private void onActionFARunDFI(ActionEvent event) { app.ctlr.runDFIAnalysis(""); }
    @FXML private void onActionFAStatsDFI(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDFI); }
    @FXML private void onActionFASummaryDFI(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.FIRESULTSSUMMARY); }
    @FXML private void onActionFAAssociationDFI(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.FIASSOCIATION); }
    //@FXML private void onActionFAClearDFI(ActionEvent event) { app.ctlr.clearFADFI(); }
    
    @FXML private void onActionFARunDPA(ActionEvent event) { app.ctlr.runDPAnalysis(); }
    @FXML private void onActionFAStatsDPA(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDPA); }
    @FXML private void onActionFAClearDPA(ActionEvent event) { app.ctlr.clearFADPA(); }
    
    @FXML private void onActionFARunUTRL(ActionEvent event) { app.ctlr.runUTRLAnalysis();}
    @FXML private void onActionFAStatsUTRL(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSUTRL); }
    @FXML private void onActionFAClearUTRL(ActionEvent event) { app.ctlr.clearFAUTRL(); }
    //@FXML private void onActionFAStatsUTRLengthening(ActionEvent event) { app.ctlr.openProjectDataSubTab(TabProjectData.Panels.STATSDPA); }
    //@FXML private void onActionFAClearUTRLengthening(ActionEvent event) { app.ctlr.clearFADPA(); }
    
    @FXML private void onActionFAClearAll(ActionEvent event) { app.ctlr.clearFAAll(); }

    
    //
    // Graphs and Charts
    //
    @FXML private void onActionProjectDV_AF(ActionEvent event) { app.ctlr.viewGraphs(DlgSelectGraph.Params.ItemType.AF); }
    @FXML private void onActionProjectDV_Data(ActionEvent event) { app.ctlr.viewGraphs(DlgSelectGraph.Params.ItemType.Data); }
    @FXML private void onActionProjectDV_DA(ActionEvent event) { app.ctlr.viewGraphs(DlgSelectGraph.Params.ItemType.DA); }
    @FXML private void onActionProjectDV_EA(ActionEvent event) { app.ctlr.viewGraphs(DlgSelectGraph.Params.ItemType.EA); }
    @FXML private void onActionProjectDV_EM(ActionEvent event) { app.ctlr.viewGraphs(DlgSelectGraph.Params.ItemType.EM); }
    @FXML private void onActionTool_VennDiag(ActionEvent event) { app.ctlr.toolVennDiag(); }

    //
    // Internal Support Functions
    //
    private void setupSplitMenuButon(SplitMenuButton btn, String imgName) {
        Image img = new Image( getClass().getResourceAsStream("/tappas/images/" + imgName));
        ImageView imgv = new ImageView(img);
        // picky beautification, should just redo images to look properly centered
        if(imgName.equals("ea.png") || imgName.equals("fda.png"))
            imgv.setTranslateY(3.0);
        imgv.setFitHeight(20);
        imgv.setFitWidth(20);
        btn.setGraphic(imgv);
        btn.setContentDisplay(ContentDisplay.TOP);
    }
    
    private void setupMenuButon(MenuButton btn, String imgName) {
        Image img = new Image( getClass().getResourceAsStream("/tappas/images/" + imgName));
        ImageView imgv = new ImageView(img);
        // picky beautification, should just redo images to look properly centered
        if(imgName.equals("ea.png") || imgName.equals("fda.png"))
            imgv.setTranslateY(3.0);
        imgv.setFitHeight(25);
        imgv.setFitWidth(25);
        btn.setGraphic(imgv);
        btn.setContentDisplay(ContentDisplay.TOP);
    }
    
    //
    // Static Functions
    //
    static public boolean isSearchControlId(String id) {
        boolean isctl = false;
        if(id.equals("txtSearch") || id.equals("btnSearch") || id.equals("chkHideUnselected"))
            isctl = true;
        return isctl;
    }
}
