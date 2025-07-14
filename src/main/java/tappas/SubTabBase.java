/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import tappas.Analysis.DataSelectionResults;
import tappas.DataApp.DataType;
import tappas.DataApp.SelectionDataResults;
import tappas.DataDPA.DPASelectionResults;
import tappas.DataUTRL.UTRLSelectionResults;
import tappas.DlgSelectRows.ColumnDefinition;

import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


// TODO: get rid of each tab adding a selectchg handler, best to do at tabPane level - once we get this working

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabBase extends AppObject {
    // to make table column groups more easy to distinguish, the group column header is colored
    // currently set to just altternate colors - originally set to four different colors
    final String[] colGroups = { "groupA", "groupB", "groupC", "groupD" };
    final int colGrpsMax = colGroups.length;

    final String ROW_SELECT_COLID = "RowSelect";
    final int MAX_ANNOTCOLS = 9;
    public final static int BTN_IMGSIZE = 24;
    final double leftMenuWidth = BTN_IMGSIZE + 13;
    final double BTN_IMG_yOffset = 32.0;
    final Image imgTab = new Image(getClass().getResourceAsStream("/tappas/images/tab.png"));
    final Chromosome.ChromoComparator chromoComparator = new Chromosome.ChromoComparator();
    
    // instance data
    Tabs tabs;                                  /* applications Tabs class instance */
    Tab subTab;                                 /* subtab node (Tab object) */
    protected TabBase tabBase;                  /* subtab's parent tabBase instance */
    protected Node tabNode;                     /* subtab node loaded via FXML */
    protected SubTabInfo subTabInfo;
    Pane menuPane;                              /* normal subtab pane */
    Pane menuRunPane;                           /* pane shown while running analysis */
    AnchorPane bkgndPane;                       /* background pane, provides thin horizontal skyblue line */
    TableView tblSelection = null;              /* table to do selection on - one table per subtab */
    String selDataClass = "";                   /* selection table class */
    ContextMenu cmTableCols = null;             /* table columns context menu - top left shows up as + (only for main table select/annot) */
    protected Node onSelectFocusNode = null;    /* default focus node when subtab selected */
    protected HashMap<String, Object> args;
    protected boolean populated = false;
    TaskHandler.ServiceExt service = null;
    Thread thread = null;
    Process process;
    boolean threadResult = false;
    boolean ignoreHideEvent = true;
    boolean ignoreMenuChange = false;
    ArrayList<String> lstAnnotationCols = new ArrayList<>();
    ArrayList<ColumnDefinition> lstFieldDefinitions;

    // special controls
    Button btnOptions, btnEdit, btnZoomIn, btnZoomOut, btnZoomFit, btnSignificance, btnRun;
    Button btnExport, btnDataViz, btnClusters, btnHelp, btnSelect;
    Label lblHeader;
    GridPane grdMain, grdWait;
    Pane paneTabHeader, panePI;
    ProgressIndicator pi;
    Node nodeWait;
    Node nodeLog = null;
    Button btnAbortTask = null;
    
    //
    // Public interface
    //
    public SubTabBase(Project project) {
        super(project, null);
        this.tabs = app.tabs;
    }
    // must Override and implement functionality
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        boolean result = false;
        
        this.tabBase = tabBase;
        this.subTabInfo = subTabInfo;
        this.args = args;
        if(initializeTab()) {
            // do not set subTabBase unless tab was successfully initialized
            result = true;
            subTabInfo.subTabBase = this;
        }
        //if(result) {
        //    System.out.println("UTLM from SubTabBase::initialize()");
        //    app.ctlr.updateTopLevelMenus(tabBase.tabId, false);
        //}
        return result;
    }
    public String getShortName(String name) { return name.length() > 20? name.substring(0, 20) + "...": name; }
    // tentative way to get subtab state when exiting the application - finalize!!!
    public String getState() {
        // override
        return "";
    }
    public Object processRequest(HashMap<String, Object> hm) {
        // override
        return null;
    }
    public void search(Object obj, String txt, boolean hide) {
        // override
    }
    public void setTabLabel(String txt, String tooltip) {
        //lblHeader.setText(txt + (txt.equals("+")? " " : "  "));
        //lblHeader.setTooltip(new Tooltip(tooltip));
        subTab.setText(txt);
        subTab.setTooltip(new Tooltip(tooltip));
    }
    public void disableSideButtons(boolean disable) {
        if(btnOptions != null)
            btnOptions.setDisable(disable);
        if(btnSelect != null)
            btnSelect.setDisable(disable);
        if(btnEdit != null)
            btnEdit.setDisable(disable);
        if(btnZoomFit != null)
            btnZoomFit.setDisable(disable);
        if(btnZoomIn != null)
            btnZoomIn.setDisable(disable);
        if(btnZoomOut != null)
            btnZoomOut.setDisable(disable);
        if(btnExport != null)
            btnExport.setDisable(disable);
        if(btnDataViz != null)
            btnDataViz.setDisable(disable);
        if(btnClusters != null)
            btnClusters.setDisable(disable);
        if(btnSignificance != null)
            btnSignificance.setDisable(disable);
        if(btnRun != null)
            btnRun.setDisable(disable);
    }

    //
    // Event Notifications
    //
    
    // Left Menu Buttons
    protected void onButtonOptions() {
        // override or setup data viz menu items
        if(subTabInfo.cmOptions != null)
        {
            subTabInfo.cmOptions.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px; -fx-font-weight: normal;");
            subTabInfo.cmOptions.show(btnOptions, Side.BOTTOM, 0, 0);
        }
    }
    protected void onButtonSelect() {
        // override or setup data viz menu items
        if(subTabInfo.cmSelect != null)
        {
            subTabInfo.cmSelect.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px; -fx-font-weight: normal;");
            subTabInfo.cmSelect.show(btnSelect, Side.BOTTOM, 0, 0);
        }
    }
    protected void onButtonEdit() {
        // override
    }
    public void setBtnZoomInDisable(boolean disable) { if (btnZoomIn != null) btnZoomIn.setDisable(disable); }
    protected void onButtonZoomIn() { /* override */ }
    public void setBtnZoomOutDisable(boolean disable) { if (btnZoomOut != null) btnZoomOut.setDisable(disable); }
    protected void onButtonZoomOut() { /* override */  }
    public void setBtnZoomFitDisable(boolean disable) { if (btnZoomFit != null) btnZoomFit.setDisable(disable); }
    protected void onButtonZoomFit() { /* override */  }
    protected void onButtonExport() {
        // override or setup export menu items
        if(subTabInfo.cmExport != null)
        {
            subTabInfo.cmExport.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px; -fx-font-weight: normal;");
            subTabInfo.cmExport.show(btnExport, Side.BOTTOM, 0, 0);
        }
    }
    protected void onButtonClusters() {
        // override
    }
    protected void onButtonDataViz() {
        // override or setup data viz menu items
        if(subTabInfo.cmDataViz != null)
        {
            subTabInfo.cmDataViz.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px; -fx-font-weight: normal;");
            subTabInfo.cmDataViz.show(btnDataViz, Side.BOTTOM, 0, 0);
        }
    }
    protected void onButtonSignificance() { /* override */ }
    protected void onButtonRun()   { /* override */ }
    protected void onButtonFloat() { 
        // override if needed
        subTabInfo.dock = false;
        tabBase.floatSubTab(subTabInfo);
    }
    protected void onButtonDock()  {
        if(subTabInfo.dlgFloat != null) {
            // request to dock and close dialog
            subTabInfo.dock = true;
            subTabInfo.dlgFloat.dialog.close();

            // restore subtab content, set back to float selection, and clear dialog object
            subTabInfo.subTabBase.subTab.setContent(subTabInfo.dlgFloat.node);
            subTabInfo.subTabBase.setDockToFloat();
            subTabInfo.dlgFloat = null;
            // add subtab back into tab pane - add at end since changes could have taken place
            // note: when parent tab closes, it also closes floating subtabs
            // so it should be safe to assume that we can dock it
            int idx = tabBase.tpSubTabs.getTabs().size() - 1;
            if(idx >= 0) {
                tabBase.tpSubTabs.getTabs().add(idx, subTabInfo.subTabBase.subTab);
                tabBase.tpSubTabs.getSelectionModel().select(subTabInfo.subTabBase.subTab);
            }
        }
    }
    protected void setFocusNode(Node node, boolean focus) {
        onSelectFocusNode = node;
        if(focus) {
            Platform.runLater(() -> {
                onSelectFocusNode.requestFocus();
            });
        }
    }
    // WARNING: if overriding, make sure to call this base class (see Gene DV for Trans, Protein, and Genomics)
    protected void onSelect(boolean selected) {
        System.out.println("onSelect subTab " + subTabInfo.subTabId + " selected: " + selected);
        if(selected) {
            show();
            if(!app.ctlr.isOpeningTabs()) {
                /*
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() { subTab.getContent().requestFocus(); }
                });*/
                if(onSelectFocusNode != null) {
                    Platform.runLater(() -> {
                        onSelectFocusNode.requestFocus();
                    });
                    System.out.println("onSelectFocusNode.requestFocus(): " + onSelectFocusNode);
                }
                else {
                    System.out.println("onSelectFocusNode is still null");
                    // this means that the focus will not be given to the table/node and it won't show up highlighted
                    // set focus to tab, onSelectFocusNode may not be set yet (loading data)
                    if(subTab.getContent() != null) {
                        subTab.getContent().requestFocus();
                        System.out.println("subTab.getContent().requestFocus()");
                    }
                }
            }
        }
        // should this be only if selected?
        setTabFocusStyle(true);
    }
    public void setTabFocusStyle(boolean focused) {
        if(subTab != null) {
            if(focused) {
                System.out.println("resid: " + subTabInfo.resId);
                if(subTabInfo.resId.equals("Plus")) {
                    System.out.println("+++++++++++ found plus");
                    if(subTab.isSelected())
                        subTab.setStyle("");
                    else
                        subTab.setStyle("-fx-background-color: rgba(152,251,152,0.25);");
                }
                else
                    subTab.setStyle("");
                menuPane.setStyle("-fx-background-color: #a2ca72; -fx-border-width:0;");
                bkgndPane.setStyle("-fx-background-color: #a2ca72;");
                
                if(subTab.getTabPane() != null) {
                    Node node = subTab.getTabPane().lookup(".tab-header-background");
                    if(node != null)
                        node.setStyle("");
                }
            }
            else {
                if(subTab.getTabPane() != null) {
                    Node node = subTab.getTabPane().lookup(".tab-header-background");
                    if(node != null) {
                        System.out.println("Setting tabPane .tab-header-background style");
                        node.setStyle("-fx-border-color: -fx-selection-bar-non-focused;");
                    }
                    else {
                        // we get a NULL for the node initially when the sub tabs are created
                        // not sure why since the tab pane, tpSubTabs, in the tab should have been created
                        // I guess there is some kind of delay
                        // Not a significant issue just causes a think border line on first subtab opened (not focused)
                        // only happens on the first set of tabs opened, goes away after that
                        System.out.println("******************* No tabpane...");
                    }
                }
                else
                    System.out.println("NO subTab.getTabPane() to set style for");
                
                // there is a thin blue line still present from the subtab style in style.css - could change style class or leave it alone
                subTab.setStyle("-fx-background-color: -fx-selection-bar-non-focused;");
                bkgndPane.setStyle("-fx-background-color: -fx-selection-bar-non-focused;");
                menuPane.setStyle("-fx-background-color: -fx-selection-bar-non-focused; -fx-border-width:0;");
            }
        }
    }
    protected boolean onCloseRequestOK() {
        boolean ok = true;
        if(service != null && service.isRunning()) {
            String name = service.taskInfo != null? service.taskInfo.name : "Running Task";
            ok = app.ctls.alertConfirmation("Abort " + name, "If you close this sub tab, the running task will be aborted.\nAre sure you want to close the tab?\n", ButtonType.CANCEL);
        }
        return ok;
    }
    protected void onClosing() {
        // closing was already approved so abort if needed
        if(service != null && service.isRunning()) {
            String name = service.taskInfo != null? service.taskInfo.name : "Running Task";
            app.logInfo("Aborting '" + name + "' by user request.");
            service.abort();
        }
    }
    // if override - keep in mind that tab object is a goner
    protected void onTabClosed() {
        System.out.println("SubTabBase: OnTabClosed event");
        subTabInfo.clear();
    }

    
    //
    // Protected Functions
    //
    protected void show() {
        // can only load once, do not load while loading initial tabs for project
        if(!populated && !app.ctlr.isLoadingTabs()) {
            if(tabBase.tabId.equals(Tabs.TAB_AI) || (project != null && project.data.isDataLoaded())) {
                populated = true;
                //hideProgress();
                runDataLoadThread();
            }
            /*
            else {
                if(project != null && !project.data.isDataLoaded()) {
                    if(panePI == null)
                        showProgress();
                }
            }*/
        }
    }
    protected void runDataLoadThread() {
        // override
    }
    protected void refreshSubTab(HashMap<String, Object> hmArgs) {
        // override
    }

    // create tab, load fxml, add to tabPane, adn setup control references
    protected boolean initializeTab() {
        boolean result = false;
        
        try {
            // create tab control
            String fxml = "/tappas/subTabs/" + subTabInfo.resId + ".fxml";
            subTab = createTab();
            // style class won't work - parent tabpane is set to use style.css so not sure whay?!!!
            //if(subTabInfo.resId.equals("Plus"))
            //    subTab.getStyleClass().add("tabPlus");
            if(subTabInfo.resId.equals("Plus"))
                subTab.setStyle("-fx-background-color: rgba(152,251,152,0.35);");
            // this event won't trigger if the subtab is already selected
            // even if we are coming from another tab pane
            subTab.setOnSelectionChanged((Event t) -> {
                if(subTab.isSelected()) {
                    System.out.println("UTLM SELECTED from SubTabBase::setOnSelectionChanged() for " + subTabInfo.resId);
                    app.ctlr.updateTopLevelMenus(tabBase.tabId, false);
                }
                else
                    System.out.println("UTLM NOT selected from SubTabBase::setOnSelectionChanged() for " + subTabInfo.resId);
                onSelect(subTab.isSelected());
            });
            // add actual tab content
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            tabNode = loader.load();
            bkgndPane = new AnchorPane();
            bkgndPane.setStyle("-fx-background-color: #a2ca72;");
            AnchorPane.setTopAnchor(tabNode, 2.0);
            AnchorPane.setBottomAnchor(tabNode, 0.0);
            AnchorPane.setLeftAnchor(tabNode, leftMenuWidth);
            AnchorPane.setRightAnchor(tabNode, 0.0);
            
            // create left menu pane
            menuPane = new Pane();
            menuPane.setPrefWidth(leftMenuWidth);
            menuPane.setMaxWidth(leftMenuWidth);
            menuPane.setMinWidth(leftMenuWidth);
            menuPane.setStyle("-fx-background-color:#a2ca72; -fx-border-width:0;");
            //menuPane.setStyle("-fx-background-color:#efefef; -fx-border-width:1 1 0 0; -fx-border-color:lightgray"); //#F4FAFA#efefef
            AnchorPane.setTopAnchor(menuPane, 1.0);
            AnchorPane.setBottomAnchor(menuPane, 0.0);
            AnchorPane.setLeftAnchor(menuPane, 0.0);

            // add menu buttons
            double yoffset = 2.0; //6.0;
            if(subTabInfo.btnOptions) {
                btnOptions = app.ctls.addImgButton(menuPane, "options.png", "Options...", yoffset, 32, false);
                btnOptions.setOnAction((event) -> { onButtonOptions(); });
                yoffset += BTN_IMG_yOffset;
            }
            if(subTabInfo.btnSelect) {
                btnSelect = app.ctls.addImgButton(menuPane, "select.png", "Table row selection...", yoffset, 32, false);
                btnSelect.setOnAction((event) -> { onButtonSelect(); });
                yoffset += BTN_IMG_yOffset;
            }
            if(subTabInfo.btnEdit) {
                btnEdit = app.ctls.addImgButton(menuPane, "edit.png", "Edit...", yoffset, 32, false);
                btnEdit.setOnAction((event) -> { onButtonEdit(); });
                yoffset += BTN_IMG_yOffset;
            }
            if(subTabInfo.btnExport) {
                btnExport = app.ctls.addImgButton(menuPane, "export.png", "Export data or images...", yoffset, 32, false);
                btnExport.setOnAction((event) -> { onButtonExport(); });
                yoffset += BTN_IMG_yOffset;
            }
            if(subTabInfo.btnZoom) {
                Separator separator1 = new Separator();
                separator1.setLayoutX(2);
                separator1.setLayoutY(29);
                separator1.setPrefWidth(leftMenuWidth - 9);
                Separator separator2 = new Separator();
                separator2.setLayoutX(2);
                separator2.setLayoutY(57);
                separator2.setPrefWidth(leftMenuWidth - 9);
                Pane paneZoom = new Pane();
                paneZoom.setPrefWidth(leftMenuWidth - 5);
                paneZoom.setMaxWidth(leftMenuWidth - 5);
                paneZoom.setMinWidth(leftMenuWidth - 5);
                paneZoom.setPrefHeight(89); //77
                paneZoom.setMaxHeight(89);
                paneZoom.setMinHeight(89);
                paneZoom.setLayoutX(2.0);
                paneZoom.setLayoutY(yoffset);
                paneZoom.setStyle("-fx-background-color:transparent; -fx-border-width:0;");
                paneZoom.getChildren().add(separator1);
                paneZoom.getChildren().add(separator2);
                
                double poffset = 2.0;
                btnZoomIn = app.ctls.addImgButton(paneZoom, "plus.png", "Zoom out", poffset, 28, false);
                btnZoomIn.setOnAction((event) -> { onButtonZoomIn(); });
                btnZoomIn.setDisable(true);
                poffset += 28.0;
                btnZoomFit = app.ctls.addImgButton(paneZoom, "fit.png", "Fit to view", poffset, 28, false);
                btnZoomFit.setOnAction((event) -> { onButtonZoomFit(); });
                btnZoomFit.setDisable(true);
                poffset += 28.0;
                btnZoomOut = app.ctls.addImgButton(paneZoom, "minus.png", "Zoom in", poffset, 28, false);
                btnZoomOut.setOnAction((event) -> { onButtonZoomOut(); });
                yoffset += 87;
                menuPane.getChildren().add(paneZoom);
            }
            if(subTabInfo.btnDataViz) {
                btnDataViz = app.ctls.addImgButton(menuPane, "dataViz.png", "Data visualization options...", yoffset, 32, false);
                btnDataViz.setOnAction((event) -> { onButtonDataViz(); });
                yoffset += BTN_IMG_yOffset;
            }
            if(subTabInfo.btnClusters) {
                btnClusters = app.ctls.addImgButton(menuPane, "clustersData.png", "Enriched feature clustering...", yoffset, 32, false);
                btnClusters.setOnAction((event) -> { onButtonClusters(); });
                yoffset += BTN_IMG_yOffset;
            }

            // significance level and run button
            if(subTabInfo.btnSignificance) {
                btnSignificance = app.ctls.addImgButton(menuPane, "sigLevel.png", "Change significance level", yoffset, 32, false);
                btnSignificance.setOnAction((event) -> { onButtonSignificance(); });
                yoffset += BTN_IMG_yOffset;
            }
            if(subTabInfo.btnRun) {
                //if(subTabInfo.subTabId.equals(TabProjectData.Panels.GRPDEF.name()))
                //    btnRun = app.ctls.addImgButton(menuPane, "run.png", "Re-run data filter...", yoffset, 32, false);
                //else
                    btnRun = app.ctls.addImgButton(menuPane, "run.png", "Rerun analysis...", yoffset, 32, false);
                btnRun.setOnAction((event) -> { onButtonRun(); });
                yoffset += BTN_IMG_yOffset;

                // create separate run analysis left menu pane
                menuRunPane = new Pane();
                menuRunPane.setPrefWidth(leftMenuWidth);
                menuRunPane.setMaxWidth(leftMenuWidth);
                menuRunPane.setMinWidth(leftMenuWidth);
                menuRunPane.setStyle("-fx-background-color:orange;"); // -fx-border-width:1 1 0 0; -fx-border-color:lightgray"); //#F4FAFA#efefef
                AnchorPane.setTopAnchor(menuRunPane, 0.0);
                AnchorPane.setBottomAnchor(menuRunPane, 0.0);
                AnchorPane.setLeftAnchor(menuRunPane, 0.0);
                menuRunPane.setVisible(false);

                // add abort task button
                btnAbortTask = new Button();
                Image img = new Image(getClass().getResourceAsStream("/tappas/images/abort.png"));
                btnAbortTask.setGraphicTextGap(0.0);
                btnAbortTask.setStyle("-fx-padding:0px;");
                btnAbortTask.setPrefSize(32.0, 32.0);
                ImageView iv = new ImageView(img);
                iv.setFitHeight(BTN_IMGSIZE);
                iv.setFitWidth(BTN_IMGSIZE);
                btnAbortTask.setGraphic(iv);
                btnAbortTask.getStyleClass().add("buttonHelpHMenu");
                btnAbortTask.setLayoutX(2.0);
                btnAbortTask.setLayoutY(6.0);
                btnAbortTask.setTooltip(new Tooltip("Abort Analysis Task"));
                btnAbortTask.setOnAction((event) -> { abortServiceTask(service); });
                menuRunPane.getChildren().add(btnAbortTask);
            }

            // help button
            btnHelp = app.ctls.addImgButton(menuPane, "help.png", "Help", yoffset, 32, false);
            btnHelp.setOnAction((event) -> { tabBase.openHelpDialog(subTabInfo); });

            // add additional UI layout controls
            grdMain = (GridPane) tabNode.lookup("#grdMain");
            bkgndPane.getChildren().add(tabNode);
            bkgndPane.getChildren().add(menuPane);
            if(menuRunPane != null)
                bkgndPane.getChildren().add(menuRunPane);
            subTab.setContent(bkgndPane);
            subTab.setId(subTabInfo.subTabId);
            System.out.println("subTab " + subTabInfo.subTabId + " initialized");
            result = true;
        } catch(Exception e) {
            app.logError("Unable to create " + subTabInfo.title + " subTab: " + e.getMessage());
        }
        return result;
    }
    public void setFloatToDock() {
        if(subTabInfo.miFloat != null) {
            subTabInfo.miFloat.setText("Dock window back in tab panel");
            subTabInfo.miFloat.setOnAction((event) -> { onButtonDock(); });
        }
        /*
        if(btnFloat != null) {
            app.ctls.changeImgButton(btnFloat, "dock.png", "Dock window back in subtab");
            btnFloat.setOnAction((event) -> { onButtonDock(); });
        }*/
    }
    public void setDockToFloat() {
        if(subTabInfo.miFloat != null) {
            subTabInfo.miFloat.setText("Float subtab in window");
            subTabInfo.miFloat.setOnAction((event) -> { onButtonFloat(); });
        }
        /*
        if(btnFloat != null) {
            app.ctls.changeImgButton(btnFloat, "float.png", "Float subtab in window");
            btnFloat.setOnAction((event) -> { onButtonFloat(); });
        }*/
    }

    public void setDisableAbortTaskButton(boolean value) {
        if(btnAbortTask != null)
            btnAbortTask.setDisable(value);
    }
    protected void showRunAnalysisScreen(boolean showFlg, String title) {
        // need to show the Cancel button and disable all other menus - adding a pane like the menupane will probably be easiest
        if(showFlg) {
            try {
                String fxml = "/tappas/tabs/LogPopup.fxml";
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                nodeLog = loader.load();
                AnchorPane pane = new AnchorPane();
                double pad = 0.0;
                AnchorPane.setTopAnchor(nodeLog, pad);
                AnchorPane.setBottomAnchor(nodeLog, pad);
                AnchorPane.setLeftAnchor(nodeLog, leftMenuWidth);
                AnchorPane.setRightAnchor(nodeLog, pad);
                ((Pane)subTab.getContent()).getChildren().add(nodeLog);
                Label lblTitle = (Label) nodeLog.lookup("#lblA_LogTitle");
                lblTitle.setStyle("-fx-background-color: orange; -fx-text-fill: white;");
                lblTitle.setText(title);
                
                // show run menu pane
                if(menuRunPane != null) {
                    // code will enable abort button when applicable
                    btnAbortTask.setDisable(true);
                    menuRunPane.setVisible(true);
                }
            } catch(Exception e) {
                app.logError("Unable to create log display window: " + e.getMessage());
            }
        }
        else {
            if(nodeLog != null) {
                ((Pane)subTab.getContent()).getChildren().remove(nodeLog);
                nodeLog = null;
            }
            if(menuRunPane != null) {
                btnAbortTask.setDisable(true);
                menuRunPane.setVisible(false);
            }
        }
    }
    protected void setRunAnalysisScreenTitle(String title) {
        if(nodeLog != null) {
            Label lblTitle = (Label) nodeLog.lookup("#lblA_LogTitle");
            lblTitle.setStyle("-fx-background-color: orange; -fx-text-fill: white;");
            lblTitle.setText(title);
        }
    }
// remove later...!!!
    protected void showLog(boolean showFlg) {
        // need to show the Cancel button and disable all other menus - adding a pane like the menupane will probably be easiest
        if(showFlg) {
            try {
                String fxml = "/tappas/tabs/LogPopup.fxml";
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                nodeLog = loader.load();
                AnchorPane pane = new AnchorPane();
                double pad = 0.0;
                AnchorPane.setTopAnchor(nodeLog, pad);
                AnchorPane.setBottomAnchor(nodeLog, pad);
                AnchorPane.setLeftAnchor(nodeLog, leftMenuWidth);
                AnchorPane.setRightAnchor(nodeLog, pad);
                ((Pane)subTab.getContent()).getChildren().add(nodeLog);
            } catch(Exception e) {
                app.logError("Unable to create log display window: " + e.getMessage());
            }
        }
        else {
            if(nodeLog != null)
                ((Pane)subTab.getContent()).getChildren().remove(nodeLog);
            if(btnAbortTask != null)
                btnAbortTask.setVisible(false);
        }
    }
    protected void viewLog(String txt) {
        try {
            String fxml = "/tappas/tabs/LogViewPopup.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node node = loader.load();
            TextArea txtLog = (TextArea) node.lookup("#txtViewLog");
                txtLog.setText(txt);
            Button btnClose = (Button) node.lookup("#btnClose");
            btnClose.setOnAction((event) -> { 
                disableSideButtons(false);
                ((Pane)subTab.getContent()).getChildren().remove(node);
            });
            AnchorPane pane = new AnchorPane();
            AnchorPane.setTopAnchor(node, 30.0);
            AnchorPane.setBottomAnchor(node, 10.0);
            AnchorPane.setLeftAnchor(node, 45.0);
            AnchorPane.setRightAnchor(node, 10.0);
            disableSideButtons(true);
            ((Pane)subTab.getContent()).getChildren().add(node);
        } catch(Exception e) {
            app.logError("Unable to create log display window: " + e.getMessage());
        }
    }
    protected void showProgress() {
        if(panePI == null) {
            // PI leaves a large area around it so placed in a pane for now
            // would like to get a better solution since the layout settings may be off based on display!
            pi = new ProgressIndicator();
            pi.setScaleX(0.25);
            pi.setScaleY(0.25);
            double width = 0; //!!!com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(lblHeader.getText(), lblHeader.getFont());
            panePI = new Pane();
            int imgSize = 0;
            if(!subTabInfo.imgName.isEmpty())
                imgSize += BTN_IMGSIZE;
            panePI.setPrefWidth(20 + width + imgSize + 6);
            panePI.setPrefHeight(BTN_IMGSIZE);
            pi.setLayoutX(-16);
            pi.setLayoutY(-16);
            pi.setStyle(" -fx-progress-color: darkblue;");
            pi.setProgress(-1);
            panePI.getChildren().add(pi);
        }
        panePI.getChildren().remove(paneTabHeader);
        panePI.getChildren().add(paneTabHeader);
        paneTabHeader.setLayoutX(23);
        paneTabHeader.setLayoutY(0);
        subTab.setGraphic(panePI);
        nodeWait = null;
        grdWait = null;
        if(grdMain != null) {
            try {
                grdWait = grdMain;
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/tappas/tabs/Wait.fxml"));
                nodeWait = loader.load();
                grdWait.add(nodeWait, 0, 0, grdWait.getColumnConstraints().size(), grdWait.getRowConstraints().size());
            }
            catch(Exception e) {
                logger.logWarning("SubTab waiting display code exception: " + e.getMessage() + "\n");
            }
        }
    }
    protected void hideProgress() {
        if(panePI != null) {
            pi.setProgress(0);
            panePI.getChildren().remove(paneTabHeader);
            subTab.setGraphic(paneTabHeader);
            if(grdWait != null && nodeWait != null)
                grdWait.getChildren().remove(nodeWait);
        }
        grdWait = null;
        nodeWait = null;
    }
    // called from parent tab, confirmation dialog already done for all
    public void abortServiceTask() {
        if(service != null && service.taskInfo != null && service.taskInfo.canAbort()) {
            String name = service.taskInfo.name;
            app.logInfo("Aborting '" + name + "' by user request.");
            service.abort();
        }
    }
    protected void abortServiceTask(TaskHandler.ServiceExt svc) {
        if(svc != null && svc.taskInfo != null && svc.taskInfo.canAbort()) {
            String name = svc.taskInfo.name;
            if(app.ctls.alertConfirmation("Abort " + name, "Are you sure you want to abort " + name + "?", null)) {
                app.logInfo("Aborting '" + name + "' by user request.");
                svc.abort();
            }
        }
        else
            app.ctls.alertInformation("Abort Task", "Unable to process abort request.\nTask information is not available.");
    }
    protected String getSignificanceLevel(double sigValue, String title) {
        String val = "";
        DlgSigLevel dlg = new DlgSigLevel(project, Tappas.getWindow());
        DlgSigLevel.Params results;
        DlgSigLevel.Params params = new DlgSigLevel.Params();
        results = dlg.showAndWait(params);
        params.title = title;
        params.sigValue = sigValue;

        if(results != null)
            val = "" + results.sigValue;
        return val;
    }
    protected String[] getSignificanceLevelByFC(double sigValue, double FC, String title) {
        String[] val = new String[2];
        val[0] = "";
        val[1] = "";
        DlgSigLevel dlg = new DlgSigLevel(project, Tappas.getWindow());
        DlgSigLevel.ParamsDEA results;
        DlgSigLevel.ParamsDEA params = new DlgSigLevel.ParamsDEA();
        results = dlg.showAndWaitDEA(params);
        params.title = title;
        params.sigValue = sigValue;

        if(results != null){
            val[0] = "" + results.sigValue;
            val[1] = "" + results.FCValue;
        }
        
        return val;
    }
    // call to setup search function for table
    protected void setupTableSearch(TableView tbl) {
        if(tbl != null) {
            tbl.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                    TabBase.SearchInfo si = (TabBase.SearchInfo)tbl.getUserData();
                    searchSet(tbl, si.txt, si.hide);
                    System.out.println("tbl got focus **********************");
                    System.out.println("txt: " + si.txt + ", hide: " + si.hide);
                }
                else {
                    // we need to make sure the focus is not in the selection check box or another part of the table
                    boolean fnd = false;
                    if(tbl.getScene() == null) {
                        app.logInfo("Found tbl: " + tbl.toString() + " with NULL scene");
                    }
                    else {
                        Node node = tbl.getScene().getFocusOwner();
                        if(node != null) {
                            Parent parent = node.getParent();
                            while(parent != null) {
                                if(parent.equals(tbl)) {
                                    fnd = true;
                                    break;
                                }
                                parent = parent.getParent();
                            }
                        }
                    }
                    if(!fnd && !app.ctlr.fxdc.isSearchFocused()) {
                        tabs.searchSet(null);
                        System.out.println("subTabBase tbl LOST focus **********************");
                    }
                }
            });
            subTabInfo.lstSearchTables.add(tbl);
        }
    };
    
// remove use one below!
protected void setTableSearchActive(TableView tbl, boolean value) {
    if(value) {
        if(!tbl.getStyleClass().contains("search-active"))
            tbl.getStyleClass().add("search-active");
        tbl.setStyle("");
    }
    else {
        tbl.getStyleClass().removeAll("search-active");
        tbl.setStyle("-fx-background-color: #FFFFF0;");
    }
}
    protected void setTableFiltered(TableView tbl, boolean filtered) {
        if(filtered) {
            if(!tbl.getStyleClass().contains("search-active"))
                tbl.getStyleClass().add("search-active");
            tbl.setStyle("");
        }
        else {
            tbl.getStyleClass().removeAll("search-active");
            tbl.setStyle("-fx-background-color: #FFFFF0;");
        }
    }
    
    // if obj is null, txt and hide are ignored
    protected void searchSet(Object obj, String txt, boolean hide) {
        if(obj == null)
            tabs.searchSet(null);
        else {
            System.out.println("tabBase searchset: '" + txt + "', hide: " + hide);
            tabs.searchSet(new Tabs.SearchInfo(tabBase, obj, txt, hide));
        }
    }
    
    protected void setupTableMenu(TableView tblData, DataType dataType, boolean selectedMenu, boolean customMenu, 
                                  ArrayList<ColumnDefinition> lstFieldDefinitions,
                                  ObservableList data, String dataClass) {
        // only needed if using selection or custom annotation columns
        if(!selectedMenu && !customMenu)
            return;
        
        if(selectedMenu) {
            tblSelection = tblData;
            selDataClass = dataClass;
            setupTableRowSelection(lstFieldDefinitions, data);
        }
        else {
            tblSelection = null;
            selDataClass = "";
        }
        // table object may or may not be fully available depending on when table is created
        // in relationship to this call - maybe find a cleaner way later!!!
        ContextMenu tmcm = getContextMenu(tblData);
        if(tmcm != null)
            setupContextMenu(tblData, dataType, selectedMenu, customMenu);
        tblData.widthProperty().addListener((obs, oldData, newData) -> { setupContextMenu(tblData, dataType, selectedMenu, customMenu); });
    }
    
    void setupContextMenu(TableView tblData, DataType dataType, boolean selectedMenu, boolean customMenu) {
        ContextMenu tmcm = getContextMenu(tblData);
        if(tmcm != null) {
            if(cmTableCols == null || cmTableCols != tmcm) {
                cmTableCols = tmcm;
                System.out.println("Setting table menu onShown handler");
                tmcm.setOnShown((WindowEvent e) -> {
                    ObservableList<MenuItem> lst = tmcm.getItems();
                    ArrayList<MenuItem> lstRemove = new ArrayList<>();
                    Menu menuRemove = null;
                    for(MenuItem mi : lst) {
                        if(!mi.getClass().equals(SeparatorMenuItem.class)) {
                            if(selectedMenu && mi.getText().isEmpty()) // || mi.getText().equals("#"))
                                mi.setText("Selected [check box]"); //Visible(false);
                            if(customMenu && mi.getId() != null) {
                                if(mi.getId().equals("ADDANNOTCOL"))
                                    lstRemove.add(mi);
                                if(mi.getId().equals("RMVANNOTCOL")) {
                                    menuRemove = (Menu) mi;
                                    lstRemove.add(mi);
                                }
                            }
                            // required to avoid losing first "_" character in name
                            mi.setMnemonicParsing(false);
                        }
                        else
                            lstRemove.add(mi);
                    }
                    for(MenuItem mi : lstRemove) {
                        System.out.println("Moving existing menu items......");
                        tmcm.getItems().remove(mi);
                        tmcm.getItems().add(mi);
                    }
                    if(customMenu) {
                        if(lstRemove.isEmpty()) {
                            System.out.println("Adding new menu items......");
                            SeparatorMenuItem smi = new SeparatorMenuItem();
                            smi.setId("ADDCOLSEP");
                            tmcm.getItems().add(smi);
                            
                            MenuItem mi = new MenuItem("Add annotation feature column...");
                            mi.setId("ADDANNOTCOL");
                            mi.setOnAction((event) -> { AddAnnotationCol(tblData, dataType); });
                            tmcm.getItems().add(mi);
                            
                            if(!lstAnnotationCols.isEmpty()) {
                                smi = new SeparatorMenuItem();
                                tmcm.getItems().add(smi);
                                smi.setId("RMVCOLSEP");
                                Menu m = new Menu("Remove annotation feature column");
                                m.setId("RMVANNOTCOL");
                                tmcm.getItems().add(m);
                                for(String colName : lstAnnotationCols) {
                                    mi = new MenuItem(colName);
                                    // required to avoid losing first "_" character!
                                    mi.setMnemonicParsing(false);
                                    mi.setOnAction((event) -> {
                                        removeAnnotationCol(colName);
                                        Platform.runLater(() -> {
                                            cmTableCols.hide();
                                        });
                                    });
                                    m.getItems().add(mi);
                                }
                            }
                        }
                        else {
                            // need to update, or remove, the remove menu, could have changed
                            if(lstAnnotationCols.isEmpty()) {
                                if(menuRemove != null) {
                                    tmcm.getItems().remove(menuRemove);
                                    menuRemove = null;
                                    for(MenuItem mi : tmcm.getItems()) {
                                        if(mi.getId() != null && mi.getId().equals("RMVCOLSEP")) {
                                            tmcm.getItems().remove(mi);
                                            break;
                                        }
                                    }
                                }
                            }
                            else {
                                if(menuRemove == null) {
                                    SeparatorMenuItem smi = new SeparatorMenuItem();
                                    tmcm.getItems().add(smi);
                                    menuRemove = new Menu("Remove annotation feature column");
                                    menuRemove.setId("RMVANNOTCOL");
                                    tmcm.getItems().add(menuRemove);
                                }
                                else
                                    menuRemove.getItems().clear();
                                for(String colName : lstAnnotationCols) {
                                    MenuItem mi = new MenuItem(colName);
                                    // required to avoid losing first "_" character!
                                    mi.setMnemonicParsing(false);
                                    mi.setOnAction((event) -> {
                                        removeAnnotationCol(colName);
                                        Platform.runLater(() -> {
                                            cmTableCols.hide();
                                        });
                                    });
                                    menuRemove.getItems().add(mi);
                                }
                            }
                        }
                    }
                });
            }
        }
    }    
    private void setupTableRowSelection(ArrayList<ColumnDefinition> lstFieldDefinitions, ObservableList data) {
        setupSelectMenu(lstFieldDefinitions);
        for(SelectionDataResults row : (ObservableList<SelectionDataResults>) data) {
            // add listener to handle a row selection change when 'hide unselected rows' is checked
            row.SelectedProperty().addListener((ov, oldValue, newValue) -> {
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblSelection.getUserData();
                if(info != null && !ignoreHideEvent) {
                    System.out.println("setupTableRowSelection");
                    tabBase.searchSet(tblSelection, info.txt, info.hide);
                    if(info.hide) {
                        // process to remove unselected rows
                        search(tblSelection, info.txt, info.hide);
                        System.out.println("Select data changed - row hidden if applicable");
                    }
                }
                tblSelection.requestFocus();
            });
        }
    }
    // setup table for row selection
    // All columns must have been added to the table
    private void setupSelectMenu(ArrayList<ColumnDefinition> lstFieldDefinitions) {
        // create column header menu and add to all except select and row number
        this.lstFieldDefinitions = lstFieldDefinitions;
        ObservableList<TableColumn> cols = tblSelection.getColumns();
        for(TableColumn col : cols) {
            if(!col.getText().isEmpty() && !col.getText().equals("#")) {
                if(!col.getColumns().isEmpty()) {
                    col.setContextMenu(new ContextMenu());
                    ObservableList<TableColumn> subcols = col.getColumns();
                    for(TableColumn subcol : subcols) {
                        ContextMenu cm = new ContextMenu();
                        MenuItem item = new MenuItem("Add/Remove row selections based on '" + col.getText() + ": " + subcol.getText() + "'...");
                        item.setOnAction((event) -> { addSelectedRows(col.getText(), subcol.getText().trim()); });
                        cm.getItems().add(item);
                        subcol.setContextMenu(cm);
                    }
                }
                else {
                    ContextMenu cm = new ContextMenu();
                    MenuItem item = new MenuItem("Add/Remove row selections based on '" + col.getText() + "'...");
                    item.setOnAction((event) -> { addSelectedRows("", col.getText().trim()); });
                    cm.getItems().add(item);
                    col.setContextMenu(cm);
                }
            }
            else {
                // a right click is still processed by the selection column but only for sort up/down
                // it will not check/uncheck the selection checkbox - don't see it as an issue...
                // would need to have a real menu item, can be disabled, or deal with mouse click event consuming
                ContextMenu cm = new ContextMenu();
                col.setContextMenu(cm);
            }
        }

// we need to add a checkmenuitem for 'Hide unselected rows'
// this will create problems, since we must call setItems with only those with selected
// however, we must use the original list with all for any additional work!
        // create row select menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Add/Remove row selections...");
        item.setOnAction((event) -> { addSelectedRows("", ""); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Select all rows");
        item.setOnAction((event) -> { changeAllRows(RowSelection.ActionType.SELECT); });
        cm.getItems().add(item);
        item = new MenuItem("Deselect all rows");
        item.setOnAction((event) -> { changeAllRows(RowSelection.ActionType.DESELECT); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Invert selection");
        item.setOnAction((event) -> { changeAllRows(RowSelection.ActionType.INVERT); });
        cm.getItems().add(item);
        subTabInfo.tblSelect = tblSelection;
        subTabInfo.cmSelect = cm;
    }
    private void addSelectedRows(String colGroup, String colTitle) {
        if(tblSelection == null)
            return;

        // set default field if specified
        HashMap<String, String> hm = new HashMap<>();
        if(!colTitle.isEmpty()) {
            ColumnDefinition fd = ColumnDefinition.getColDefFromColGroupTitle(colGroup, colTitle, lstFieldDefinitions);
            if(fd != null)
                hm.put(DlgSelectRows.Params.FIELD_PARAM, fd.id);
        }

        ObservableList<TableColumn<Object, ?>> cols = tblSelection.getColumns();
        for(TableColumn col : cols) {
            if(!col.getColumns().isEmpty()) {
                ObservableList<TableColumn> subcols = col.getColumns();
                for(TableColumn subcol : subcols) {
                    ColumnDefinition fd = ColumnDefinition.getColDefFromColGroupTitle(col.getText(), subcol.getText(), lstFieldDefinitions);
                    if(fd != null)
                        fd.visible = subcol.isVisible();
                }
            }
            else {
                ColumnDefinition fd = ColumnDefinition.getColDefFromColTitle(col.getText(), lstFieldDefinitions);
                if(fd != null)
                    fd.visible = col.isVisible();
            }
        }
        
        // get row selection criteria and select rows if needed
        Window wnd = Tappas.getWindow();
        DlgSelectRows dlg = new DlgSelectRows(project, wnd);
        TabBase.SearchInfo info = (TabBase.SearchInfo) tblSelection.getUserData();
        int cnt = RowSelection.getSelectedRowsCount((ObservableList<Object>)info.data, selDataClass);
        DlgSelectRows.Params results = dlg.showAndWait(lstFieldDefinitions, hm, cnt);
        if(results != null)
            selectRows(results);
        else
            tblSelection.requestFocus();
    }
    public void selectRows(DlgSelectRows.Params results) {
        if(tblSelection == null)
            return;
        
        ignoreHideEvent = true;
        TabBase.SearchInfo info = (TabBase.SearchInfo) tblSelection.getUserData();
        if(info != null) {
            System.out.println("RowSelect called: '" + info.txt + ", hide: " + info.hide);
            
            // select rows accordingly - always work with full data list
            RowSelection.selectRows(results, lstFieldDefinitions, (ObservableList<Object>)info.data, selDataClass);
            tblSelection.setItems((ObservableList<SelectionDataResults>) info.data);
            int selcnt = RowSelection.getSelectedRowsCount((ObservableList<Object>)info.data, selDataClass);
            info.hide = (selcnt > 0);
            //if(!info.txt.isEmpty() || info.hide)
                search(tblSelection, info.txt, info.hide);
            updateSelectedColumn(tblSelection, (selcnt > 0));
        }
        tblSelection.requestFocus();
        ignoreHideEvent = false;
    }
    protected void updateSelectedColumn(TableView tbl, boolean selected) {
        ObservableList<TableColumn> cols = tbl.getColumns();
        int idx = 0;
        for(TableColumn tc : cols) {
            if(tc.getId() != null && tc.getId().equals(ROW_SELECT_COLID)) {
                if(selected) {
                    if(!tc.getStyleClass().contains("column-selected"))
                        tc.getStyleClass().add("column-selected");
                }
                else
                    tc.getStyleClass().remove("column-selected");
                break;
            }
            idx++;
        }
    }
    protected void changeAllRows(RowSelection.ActionType action) {
        if(tblSelection == null)
            return;

        ignoreHideEvent = true;
        TabBase.SearchInfo info = (TabBase.SearchInfo) tblSelection.getUserData();
        if(info != null) {
            ObservableList<SelectionDataResults> items = (ObservableList<SelectionDataResults>) info.data;
            switch(action) {
                case SELECT:
                    for(SelectionDataResults data : items)
                        data.setSelected(true);
                    break;
                case DESELECT:
                    for(SelectionDataResults data : items)
                        data.setSelected(false);
                    break;
                case INVERT:
                    for(SelectionDataResults data : items)
                        data.setSelected(!data.isSelected());
                    break;
            }
            tblSelection.setItems((ObservableList<SelectionDataResults>) info.data);
            int selcnt = RowSelection.getSelectedRowsCount((ObservableList<Object>)info.data, selDataClass);
            info.hide = (selcnt > 0);
            // must call searchSet for any change to info.hide to be reflected on the UI
            // otherwise would need to select another subtab and then come back to this one to refresh UI
            searchSet(tblSelection, info.txt, info.hide);
            //if(!info.txt.isEmpty() || info.hide)
                search(tblSelection, info.txt, info.hide);
            updateSelectedColumn(tblSelection, (selcnt > 0));
        }
        tblSelection.requestFocus();
        ignoreHideEvent = false;
    }
    protected void removeAnnotationCol(String colName) {
        if(tblSelection == null)
            return;

        if(lstAnnotationCols.contains(colName))
            lstAnnotationCols.remove(colName);
        ObservableList<TableColumn> cols = tblSelection.getColumns();
        for(TableColumn col : cols) {
            if(col.getText().equals(colName))
            {
                tblSelection.getColumns().remove(col);
                System.out.println("Removed column '" + colName + "' from table");
                break;
            }
        }
        for(ColumnDefinition cf : lstFieldDefinitions) {
            if(cf.colTitle.equals(colName)) {
                lstFieldDefinitions.remove(cf);
                System.out.println("Removed column '" + colName + "' from field definitions");
                break;
            }
        }
    }
    private void AddAnnotationCol(TableView tbl, DataType dataType) {
        // check limit
        if(lstAnnotationCols.size() >= MAX_ANNOTCOLS) {
            app.ctls.alertInformation("Add Annotation Feature Column", "You have reached the limit for additional columns.\nYou may remove some columns and try again.");
            return;
        }
        if(service != null && service.isRunning()) {
            app.ctls.alertInformation("Add Annotation Feature Column", "You are currently loading data.\nOnce data is loaded you may try again.");
            return;
        }
        
        // it takes 2 to 4 secs to get the data, getAnnotationFeaturesData, if not already loaded
        // if we do it as background here then we need to sync to foreground access to the data!
        
        // get row selection criteria and select rows if needed
        Window wnd = Tappas.getWindow();
        DlgAddColumn dlg = new DlgAddColumn(project, wnd);
        ContextMenu cm = getContextMenu(tbl);
        ArrayList<String> lstColNames = new ArrayList<>();
        if(cm != null) {
            ObservableList<MenuItem> lstMenuItems = cm.getItems();
            for(MenuItem mi : lstMenuItems) {
                if(!mi.getClass().equals(SeparatorMenuItem.class))
                    lstColNames.add(mi.getText());
            }
        }
        DlgAddColumn.Params results = dlg.showAndWait(lstColNames, new HashMap<>());
        if(results != null)
            runGetDataThread(tbl, dataType, results);
    }
    
    //
    // Background Data Retrieval
    //
    private boolean runGetDataThread(TableView tbl, DataType dataType,  DlgAddColumn.Params results) {
        boolean result = false;
        
        // run getData service/task
        service = new GetDataService(tbl, dataType, results);
        service.initialize();
        service.start();
        result = true;
        return result;
    }
    
    //
    // GSEA service/task
    //
    private class GetDataService extends TaskHandler.ServiceExt {
        TableView tbl;
        DataType dataType;
        DlgAddColumn.Params results;
        HashMap<String, HashMap<String, String>> hmTreeSelections = null;
        
        public GetDataService(TableView tbl, DataType dataType,  DlgAddColumn.Params results) {
            this.tbl = tbl;
            this.dataType = dataType;
            this.results = results;
        }
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            hmTreeSelections = null;
        }
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
                app.logWarning("TabBase - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("TabBase - task aborted.");
            app.ctls.alertWarning("TabBase", "TabBase failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(hmTreeSelections != null) {
                // set page data ready flag and show subtab
                pageDataReady = true;
                Platform.runLater(() -> {
                    for(String colId : results.lstColumnSels) {
                        String colName = DlgAddColumn.Params.getColName(colId, results.name);
                        System.out.println("Adding column: " + colName + " to table");
                        if(colId.equals(DlgAddColumn.Params.ColType.COUNT.name())) {
                            addAnnotationCountCol(tbl, colName);
                            lstFieldDefinitions.add(new ColumnDefinition(colName, "", colName, RowSelection.ARG_PREFIX + "AnnotationInt\t" + colName,
                                    50, "", "Number of features: " + colName, DlgSelectRows.Params.CompType.NUMINT));
                        }
                        else {
                            addAnnotationTextCol(tbl, colName);
                            HashMap<String, ArrayList<String>> hm = new HashMap<>();
                            if(colId.equals(DlgAddColumn.Params.ColType.ID.name())) {
                                // get tree selection values
                                for(String cat: hmTreeSelections.keySet()) {
                                    ArrayList<String> lst = new ArrayList<>();
                                    hm.put(cat, lst);
                                    HashMap<String, String> hmCatSels = hmTreeSelections.get(cat);
                                    for(String id: hmCatSels.keySet())
                                        lst.add(id);
                                }
                                ColumnDefinition fd = new ColumnDefinition(colName, "", colName, RowSelection.ARG_PREFIX + "AnnotationString\t" + colName,
                                        250, "", "Annotation features: " + colName, DlgSelectRows.Params.CompType.TEXT);
                                fd.hmTreeSelections = hm;
                                lstFieldDefinitions.add(fd);
                            }
                            else if(colId.equals(DlgAddColumn.Params.ColType.DESCRIPTION.name())) {
                                // not sure how we want to deal with this - analyze the data and see if description like (multiple fields sep by space)?
                                ColumnDefinition fd = new ColumnDefinition(colName, "", colName, RowSelection.ARG_PREFIX + "AnnotationString\t" + colName,
                                        250, "", "Annotation features: " + colName, DlgSelectRows.Params.CompType.TEXT);
                                lstFieldDefinitions.add(fd);
                            }
                        }
                        lstAnnotationCols.add(colName);
                    }
                });
            }
            else
                app.ctls.alertWarning("Annotation Features Data", "Unable to retrieve annotation features data");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    hmTreeSelections = getAnnotationFeaturesData(dataType, results);
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Get annotation features data", task);
            return task;
        }
    }
    
    public static ContextMenu getContextMenu(TableView tblView) {
        ContextMenu contextMenu = null;
        
        try {
            TableViewSkin<?> skin = (TableViewSkin<?>) tblView.getSkin();
            Field headerRowField = TableViewSkin.class.getDeclaredField("tableHeaderRow");
            headerRowField.setAccessible(true);
            TableHeaderRow headerRow = (TableHeaderRow) headerRowField.get(skin);

            //TableHeaderRow headerRow = (TableHeaderRow) tblView.lookup("TableHeaderRow");
            if(headerRow != null) {
                // get columnPopupMenu field
                Field privateContextMenuField = TableHeaderRow.class.getDeclaredField("columnPopupMenu");
                // make field public and get its value
                privateContextMenuField.setAccessible(true);
                contextMenu = (ContextMenu) privateContextMenuField.get(headerRow);
            }
        } catch (Exception ex) {
            Tappas.getApp().logError("GetContextMenu exception: " + ex.getMessage());
        }

        return contextMenu;
    }
    protected Tab createTab() {
        Tab tab = new Tab();
        paneTabHeader = new Pane();
        int offx = 2;
        if(!subTabInfo.imgName.isEmpty()) {
            Image img = new Image(getClass().getResourceAsStream("/tappas/images/" + subTabInfo.imgName));
            ImageView iv = new ImageView(img);
            iv.setFitHeight(BTN_IMGSIZE);
            iv.setFitWidth(BTN_IMGSIZE);
            paneTabHeader.getChildren().add(iv);
            iv.setLayoutX(offx);
            iv.setLayoutY(0);
            offx += BTN_IMGSIZE + 5;
        }
        // use tab text for now
        //!!!lblHeader = new Label(subTabInfo.title + (subTabInfo.resId.equals("Plus")? " " : "  "));
        //paneTabHeader.getChildren().add(lblHeader);
        //lblHeader.setLayoutX(offx);
        //lblHeader.setLayoutY(2);
        //lblHeader.setTooltip(new Tooltip(subTabInfo.tooltip));
/* later!!! only allow within same tab
        paneTabHeader.setOnDragDetected(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                Dragboard dragboard = paneTabHeader.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(Tabs.TAPPAS_TAB_DD);
                dragboard.setContent(clipboardContent);
                dragboard.setDragView(imgTab);
//!!!                tabs.draggingTab.set(tab);
                event.consume();
            }
        });*/
        //todo: the tabpane overflow button uses the tab text to get the text to display on the drop down menu!!!
        tab.setText(subTabInfo.title + (subTabInfo.resId.equals("Plus")? " " : "  "));
        tab.setTooltip(new Tooltip(subTabInfo.tooltip));
        tab.setGraphic(paneTabHeader);
        tab.setClosable(true);
        tab.setOnCloseRequest((Event event) -> {
            Tab tab1 = (Tab) event.getSource();
            System.out.println("OnCloseRequest: " + tab1.getId() + " in " + tab1.getTabPane().getId());
            if(!onCloseRequestOK())
                event.consume();
            else
                onClosing();
        });
        tab.setOnClosed((Event event) -> {
            // by the time this is called, 'tab' is a goner
            System.out.println("TabOnClosed: " + subTabInfo.subTabId);
            onTabClosed();
        });
        return tab;
    }
    public boolean isServiceTaskRunning() {
        boolean running = false;
        if(service != null && service.isRunning())
            running = true;
        return running;
    }
    // run script and log output to file - also pass log output to subtab for display on window
    protected void runScript(TaskHandler.TaskInfo taskInfo, List<String> lst, String name, String logFilepath) {
        // run script
        try {
            ProcessBuilder pb = new ProcessBuilder(lst);
            System.out.println(lst);
            pb.redirectErrorStream(true);
            process = pb.start();
            taskInfo.process = process;
            app.logDebug(name + " process started, process id: " + process.toString());

            // monitor process output
            // could change to have PB send it to a log but still need to update screen
            Writer writer = null;
            try {
                String line, dspline;
                LocalDate date = LocalDate.now();
                LocalTime time = LocalTime.now();
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFilepath, true), "utf-8"));
                dspline = name + " script is running...\n";
                outputLogLine(dspline);
                writer.write(dspline);
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int totalChars = 0;
                boolean dspinfo = true;
                while ((line = input.readLine()) != null) {
                    time = LocalTime.now();
                    dspline = time.toString() + " " + line + "\n";
                    writer.write(dspline);
                    totalChars += dspline.length();
                    if(totalChars < 100000)
                        outputLogLine(dspline);
                    else {
                        if(dspinfo) {
                            outputLogLine("Log display exceeded limit - no additional information will be displayed...");
                            dspinfo = false;
                        }
                    }
                }
                try { input.close(); } catch(Exception e) { }
                int ev = process.waitFor();
                dspline = time.toString() + " " + name + " script ended. Exit value: " + ev + "\n";
                writer.write(dspline);
                outputLogLine(dspline);
            } catch(Exception e) {
                app.logError("Unable to capture " + name + " process output: " + e.getMessage());
            } finally {
               try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        } catch(Exception e) {
            app.logError("Unable to run " + name + " script: " + e.getMessage());
        }
    }
    protected void outputLogLine(String line) {
        Platform.runLater(() -> {
            HashMap<String, Object> hmArgs = new HashMap<>();
            hmArgs.put("appendTextLog", line);
            processRequest(hmArgs);
        });
    }
    // individual analysis log handling functions
    protected void outputFirstLogLine(String msg, String logfilepath) {
        Platform.runLater(() -> {
            String line = app.getLogMsgLine(msg, true);
            if(nodeLog != null) {
                TextArea txtLog = (TextArea) nodeLog.lookup("#txtA_Log");
                if(txtLog != null)
                    txtLog.appendText(line);
            }
            app.logMsgToFile(line, logfilepath);
        });
    }
    protected void appendLogLine(String msg, String logfilepath) {
        Platform.runLater(() -> {
            String line = app.getLogMsgLine(msg, false);
            if(nodeLog != null) {
                TextArea txtLog = (TextArea) nodeLog.lookup("#txtA_Log");
                if(txtLog != null)
                    txtLog.appendText(line);
            }
            app.logMsgToFile(line, logfilepath);
        });
    }

    
    public void setTotalRowSummaryDataCount(TableView tbl) {
        tbl.setRowFactory(tableView -> {
            TableRow<TabProjectDataViz.SummaryDataCount> row = new TableRow<>();
            row.selectedProperty().addListener((ov, oldValue, newValue) -> {
                TabProjectDataViz.SummaryDataCount sd = row.getItem();
                if(sd != null && sd.getField().toLowerCase().equals("total")) {
                    if(newValue)
                        row.setStyle("");
                    else
                        row.setStyle("-fx-background-color: honeydew;");
                }
            });
            row.itemProperty().addListener((ov, oldValue, newValue) -> {
                if(newValue != null && newValue.getField().toLowerCase().equals("total"))
                    row.setStyle("-fx-background-color: honeydew;");
            });
            return row;
        });
    }
    public void setTotalRowSummaryDataPct(TableView tbl) {
        tbl.setRowFactory(tableView -> {
            TableRow<TabProjectDataViz.SummaryDataPct> row = new TableRow<>();
            row.selectedProperty().addListener((ov, oldValue, newValue) -> {
                TabProjectDataViz.SummaryDataPct sd = row.getItem();
                if(sd != null && sd.getField().toLowerCase().equals("total")) {
                    if(newValue)
                        row.setStyle("");
                    else
                        row.setStyle("-fx-background-color: honeydew;");
                }
            });
            row.itemProperty().addListener((ov, oldValue, newValue) -> {
                if(newValue != null && newValue.getField().toLowerCase().equals("total"))
                    row.setStyle("-fx-background-color: honeydew;");
            });
            return row;
        });
    }
    public ContextMenu processTableRightClick(MouseEvent event, ContextMenu cm, TableView tbl, String filename) {
        if(cm != null)
            cm.hide();
        cm = null;
        if(event.getButton().equals(MouseButton.SECONDARY)) {
            Node node = event.getPickResult().getIntersectedNode();
            if(node != null) {
                System.out.println("node: " + node);
                if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                    node = node.getParent();
                    System.out.println("parent: " + node);
                }
                if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                    cm = new ContextMenu();
                    app.export.setupSimpleTableExport(tbl, cm, false, filename);
                    cm.setAutoHide(true);
                    cm.show(tbl, event.getScreenX(), event.getScreenY());
                }
            }
        }
        return cm;
    }
    public static void addRowNumbersCol(TableView tblView) {
        TableColumn<ObservableList, Number> col = new TableColumn<>("#");
        col.setMinWidth(30);
        col.setMaxWidth(150);
        col.setPrefWidth(50);
        col.setEditable(false);
        col.setSortable(false);
        col.setStyle("-fx-alignment:TOP-CENTER");
        col.setCellValueFactory(column-> new ReadOnlyObjectWrapper<>(tblView.getItems().indexOf(column.getValue()) + 1));
        tblView.getColumns().add(0, col);
    }
    public CheckBox addRowNumsSelCols(TableView tblView) {
        TableColumn<ObservableList, Number> col = new TableColumn<>("#");
        col.setMinWidth(30);
        col.setMaxWidth(150);
        col.setPrefWidth(50);
        col.setEditable(false);
        col.setSortable(false);
        col.setStyle("-fx-alignment:TOP-CENTER");
        col.setCellValueFactory(column-> new ReadOnlyObjectWrapper<>(tblView.getItems().indexOf(column.getValue()) + 1));
        tblView.getColumns().add(0, col);
        TableColumn<ObservableList, Boolean> scol = new TableColumn<>("");
        scol.setMinWidth(50);
        scol.setMaxWidth(60);
        scol.setPrefWidth(50);
        scol.setEditable(true);
        scol.setSortable(true);
        scol.setId(ROW_SELECT_COLID);
        scol.setStyle("-fx-alignment:TOP-CENTER");
        scol.setCellValueFactory(new PropertyValueFactory<>("Selected"));
        scol.setCellFactory(CheckBoxTableCell.forTableColumn((Integer idx) -> {
            // update column color based on any row selected
            boolean rowsel = false;
            if(((SelectionDataResults)tblView.getItems().get(idx)).isSelected())
                rowsel = true;
            else {
                ObservableList<SelectionDataResults> items = (ObservableList<SelectionDataResults>)tblView.getItems();
                for(SelectionDataResults sdr : items) {
                    if(sdr.isSelected()) {
                        rowsel = true;
                        break;
                    }
                }
            }
            updateSelectedColumn(tblView, rowsel);
            return ((SelectionDataResults)tblView.getItems().get(idx)).SelectedProperty();
        }));
        CheckBox chk = new CheckBox();
        Tooltip tt = new Tooltip("Check to select ALL rows or uncheck to deselect ALL rows\nRows that are not visible, hidden or filtered via search, will also be modified");
        chk.setTooltip(tt);
        scol.setGraphic(chk);
        tblView.getColumns().add(0, scol);
        tblView.setEditable(true);
        return chk;
    }
    
    private void addAnnotationTextCol(TableView tblView, String title) {
        TableColumn<ObservableList, String> col = new TableColumn<>(title);
        col.setMinWidth(30);
        col.setPrefWidth(300);
        col.setEditable(false);
        col.setSortable(true);
        col.setStyle("-fx-text-wrap:true; -fx-alignment:TOP-LEFT");
        col.setCellValueFactory(SelectionDataResults.getAnnotationDataCallbackString(title));
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Add/Remove row selections based on '" + title + "'...");
        // required to avoid losing first "_" character in name
        item.setMnemonicParsing(false);
        item.setOnAction((event) -> { addSelectedRows("", col.getText().trim()); });
        cm.getItems().add(item);
        col.setContextMenu(cm);
        tblView.getColumns().add(col);
    }
    private void addAnnotationCountCol(TableView tblView, String title) {
        TableColumn<ObservableList, String> col = new TableColumn<>(title);
        col.setMinWidth(30);
        col.setPrefWidth(50);
        col.setEditable(false);
        col.setSortable(true);
        col.setStyle("-fx-alignment:TOP-RIGHT");
        col.setCellValueFactory(SelectionDataResults.getAnnotationDataCallbackInt(title));
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Add/Remove row selections based on '" + title + "'...");
        // required to avoid losing first "_" character in name
        item.setMnemonicParsing(false);
        item.setOnAction((event) -> { addSelectedRows("", col.getText().trim()); });
        cm.getItems().add(item);
        col.setContextMenu(cm);
        tblView.getColumns().add(col);
    }
    protected HashMap<String, HashMap<String, String>> getAnnotationFeaturesData(DataType dataType, DlgAddColumn.Params params) {
        if(tblSelection != null) {
            // get annotation features, rowid:db:cat:ids[name/desc]
            HashMap<String, HashMap<String, Object>> hmFeatures = params.getFeatures();
            HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmFilter;
            boolean expandGO = false;
            boolean incNameDesc = false;
            for(String id : params.lstColumnSels) {
                if(id.equals(DlgAddColumn.Params.ColType.DESCRIPTION.name())) {
                    incNameDesc = true;
                    break;
                }
            }
            switch(dataType) {
                case GENE:
                    hmFilter = project.data.getGeneFeatures(hmFeatures, project.data.getResultsTrans(), incNameDesc, expandGO);
                    break;
                case PROTEIN:
                    hmFilter = project.data.getProteinFeatures(hmFeatures, project.data.getResultsTrans(), incNameDesc, expandGO);
                    break;
                default:
                    hmFilter = project.data.getTransFeatures(hmFeatures, project.data.getResultsTrans(), incNameDesc, expandGO);
                    break;
            }
            return SelectionDataResults.getAnnotationFeaturesData(dataType, params, hmFilter, tblSelection);
        }
        else
            return new HashMap<>();
    }
    protected void addTableColumns(TableView tbl, ArrayList<ColumnDefinition> lstTblCols, boolean addMeanExpLevels, boolean addSampleExpLevels) {
        ObservableList<TableColumn> cols = tbl.getColumns();
        int grpidx = 0;
        for(ColumnDefinition cd : lstTblCols)
            grpidx = addTableColumn(cd, cols,  grpidx);
        if(addMeanExpLevels)
            addMeanExpLevelColumns(cols, lstTblCols);
        if(addSampleExpLevels)
            addSampleExpLevelColumns(cols, lstTblCols);
    }
    
    protected void addTableColumnsDPA(TableView tbl, ArrayList<ColumnDefinition> lstTblCols, ObservableList<DPASelectionResults> data){
        ObservableList<TableColumn> cols = tbl.getColumns();
        addDPAExpLevelColumns( cols, lstTblCols, data);
    }

    protected void addTableColumnsUTRL(TableView tbl, ArrayList<ColumnDefinition> lstTblCols, ObservableList<UTRLSelectionResults> data){
        ObservableList<TableColumn> cols = tbl.getColumns();
        addUTRLExpLevelColumns( cols, lstTblCols, data);
    }
    
    private int addTableColumn(ColumnDefinition cd, ObservableList<TableColumn> cols, int grpidx) {
        TableColumn col = new TableColumn(cd.colTitle);
        col.setMinWidth(cd.minWidth);
        if(cd.maxWidth > 0)
            col.setMaxWidth(cd.maxWidth);
        col.setPrefWidth(cd.prefWidth);
        col.setMinWidth(cd.minWidth == 0? 50 : cd.minWidth);
        col.setEditable(false);
        col.setVisible(cd.visible);
        col.setCellValueFactory(new PropertyValueFactory<>(cd.propName));
        if(!cd.alignment.isEmpty())
            col.setStyle("-fx-alignment:" + cd.alignment + ";");
        
        // handle special case for chromosome sorting - alpha-numeric string but sort numbers separate from letters
        if(cd.id.equals("CHROMO"))
            col.setComparator(chromoComparator);
        if(cd.colGroup.isEmpty())
            cols.add(col);
        else {
            boolean found = false;
            for(TableColumn tblcol : cols) {
                if(tblcol.getText().equals(cd.colGroup)) {
                    tblcol.getColumns().add(col);
                    found = true;
                    break;
                }
            }
            if(!found) {
                TableColumn tblcol = new TableColumn(cd.colGroup);
                tblcol.setMinWidth(100);
                tblcol.setEditable(false);
                System.out.println("Group: " + colGroups[grpidx] + ", out of " + colGrpsMax);
                tblcol.getStyleClass().add(colGroups[grpidx++]);
                if(grpidx >= colGrpsMax)
                    grpidx = 0;
                cols.add(tblcol);
                tblcol.getColumns().add(col);
            }
        }
        return grpidx;
    }
    protected void addSampleExpLevelColumns(ObservableList<TableColumn> cols, ArrayList<ColumnDefinition> lstTblCols) {
        DataInputMatrix.ExpMatrixParams emParams = project.data.getInputMatrixParams();
        int subcolWidth = 80;
        int sampleIdx = 0;
        if(colGroups != null && emParams != null) {
            int grpnum = 0;
            for (DlgInputData.Params.ExpMatrixGroup emg : emParams.lstGroups) {
                int nsamples = emg.getTotalSamplesCount();
                TableColumn col = new TableColumn<>(emg.name);
                col.setPrefWidth(nsamples * subcolWidth);
                col.setEditable(false);
                col.getStyleClass().add((grpnum & 1) != 0? "groupA" : "groupB");
                int tnum = 0;
                for(DlgInputData.Params.ExpMatrixTime emt : emg.lstTimes) {
                    for (String sample : emt.lstSampleNames) {
                        TableColumn subcol = new TableColumn<>(sample);
                        subcol.setMinWidth(25);
                        subcol.setPrefWidth(subcolWidth);
                        subcol.setEditable(false);
                        subcol.setCellValueFactory(getExpMatrixDataCallback(sampleIdx));
                        subcol.getStyleClass().add((tnum & 1) != 0? "timeOdd" : "timeEven");
                        subcol.setStyle("-fx-alignment:CENTER_RIGHT;");
                        col.getColumns().add(subcol);
                        ColumnDefinition cd = new ColumnDefinition("SAMPLE" + sampleIdx, col.getText(), subcol.getText(), RowSelection.ARG_PREFIX + "Sample\t" + sampleIdx,
                                                                    100, " CENTER-RIGHT", col.getText() + ": " + subcol.getText(), DlgSelectRows.Params.CompType.NUMDBL);
                        cd.visible = false;
                        lstTblCols.add(cd);
                        sampleIdx++;
                    }
                    tnum++;
                }
                cols.add(col);
                grpnum++;
            }
        }
    }
    private Callback getExpMatrixDataCallback(int idx) {
        return (new Callback<TableColumn.CellDataFeatures<DataSelectionResults, Double>, ObservableValue<Number>>() {
            @Override
            public ObservableValue<Number> call(TableColumn.CellDataFeatures<DataSelectionResults, Double> p) {
                return p.getValue().samples[idx];
            }
        });
    }
    
    protected void addDPAExpLevelColumns(ObservableList<TableColumn> cols, ArrayList<ColumnDefinition> lstTblCols, ObservableList<DPASelectionResults> data) {
         for(int k = 0; k<project.data.getGroupNames().length;k++){
            //create columns (distal and proximal) and populate it
            for(int i = 0; i<project.data.getTimePoints();i++){
                TableColumn subcol_prox = new TableColumn<>("Proximal " + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] + " Exp. Level");
                TableColumn subcol_dist = new TableColumn<>("Distal " + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] + " Exp. Level");

                subcol_prox.setMinWidth(25);
                subcol_prox.setPrefWidth(80);
                subcol_prox.setEditable(false);

                subcol_dist.setMinWidth(25);
                subcol_dist.setPrefWidth(80);
                subcol_dist.setEditable(false);
                
                subcol_prox.setCellValueFactory(SelectionDataResults.getValueProximalCallback(data, i));
                subcol_dist.setCellValueFactory(SelectionDataResults.getValueDistalCallback(data, i));
                
                subcol_prox.setStyle("-fx-alignment:TOP-RIGHT;");
                subcol_dist.setStyle("-fx-alignment:TOP-RIGHT;");
                
                ColumnDefinition cd_prox = new ColumnDefinition("TIME_PROXIMAL_" + i, "", "Proximal " + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] + 
                        " Exp. Level", "time_proximal", 125, "TOP-CENTER", "Proximal " + project.data.getTimePointNames()[i] + " Exp. Level", DlgSelectRows.Params.CompType.NUMDBL);
                ColumnDefinition cd_dist = new ColumnDefinition("TIME_DISTAL_" + i,   "", "Distal "   + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] + 
                        " Exp. Level", "time_distal",   125, "TOP-CENTER", "Distal "   + project.data.getTimePointNames()[i] + " Exp. Level", DlgSelectRows.Params.CompType.NUMDBL);

                cd_prox.visible = true;
                lstTblCols.add(cd_prox);
                cd_dist.visible = true;
                lstTblCols.add(cd_dist);
                
                cols.add(subcol_prox);
                cols.add(subcol_dist);
            }
         }
    }

    //Change for UTRL!!!
    protected void addUTRLExpLevelColumns(ObservableList<TableColumn> cols, ArrayList<ColumnDefinition> lstTblCols, ObservableList<UTRLSelectionResults> data) {
        for(int k = 0; k<project.data.getGroupNames().length;k++){
            //create columns (distal and proximal) and populate it
            for(int i = 0; i<project.data.getTimePoints();i++){
                TableColumn subcol_prox = new TableColumn<>("Proximal " + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] + " Exp. Level");
                TableColumn subcol_dist = new TableColumn<>("Distal " + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] + " Exp. Level");

                subcol_prox.setMinWidth(25);
                subcol_prox.setPrefWidth(80);
                subcol_prox.setEditable(false);

                subcol_dist.setMinWidth(25);
                subcol_dist.setPrefWidth(80);
                subcol_dist.setEditable(false);

                subcol_prox.setCellValueFactory(SelectionDataResults.getValueProximalUTRLCallback(data, i));
                subcol_dist.setCellValueFactory(SelectionDataResults.getValueDistalUTRLCallback(data, i));

                subcol_prox.setStyle("-fx-alignment:TOP-RIGHT;");
                subcol_dist.setStyle("-fx-alignment:TOP-RIGHT;");

                ColumnDefinition cd_prox = new ColumnDefinition("TIME_PROXIMAL_" + i, "", "Proximal " + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] +
                        " Exp. Level", "time_proximal", 125, "TOP-CENTER", "Proximal " + project.data.getTimePointNames()[i] + " Exp. Level", DlgSelectRows.Params.CompType.NUMDBL);
                ColumnDefinition cd_dist = new ColumnDefinition("TIME_DISTAL_" + i,   "", "Distal "   + project.data.getGroupNames()[k] + project.data.getTimePointNames()[i] +
                        " Exp. Level", "time_distal",   125, "TOP-CENTER", "Distal "   + project.data.getTimePointNames()[i] + " Exp. Level", DlgSelectRows.Params.CompType.NUMDBL);

                cd_prox.visible = true;
                lstTblCols.add(cd_prox);
                cd_dist.visible = true;
                lstTblCols.add(cd_dist);

                cols.add(subcol_prox);
                cols.add(subcol_dist);
            }
        }
    }
    
    protected void addMeanExpLevelColumns(ObservableList<TableColumn> cols, ArrayList<ColumnDefinition> lstTblCols) {
        // add expression level columns
        TableColumn<?, Number> col;
        String[] cNames = project.data.getExpTypeGroupNames();
        int cond = 1;
        int sampleIdx = 0;
        for(String cname : cNames) {
            col = new TableColumn<>(cname + " MeanExpLevel");
            col.setMinWidth(25);
            col.setPrefWidth(cname.length()*10 + 100);
            col.setEditable(false);
            col.setVisible(true);
            col.setCellValueFactory(SelectionDataResults.getExpMatrixDataCallback(sampleIdx));
            col.setStyle("-fx-alignment:TOP-RIGHT;");
//is this comment valid?!!!
            // need to figure out what to do with the property name - rowselection will try to call a method by that name!
            // maybe pass a method name and the sampleIdx - must parse and extract idx value! also add method to class
            ColumnDefinition cd = new ColumnDefinition("CONDITION" + cond, "", cname + " MeanExp", RowSelection.ARG_PREFIX + "Condition\t" + sampleIdx,
                                                        100, " CENTER-RIGHT", cname + " Mean Expression Level", DlgSelectRows.Params.CompType.NUMDBL);
            cd.visible = true;
            lstTblCols.add(cd);
            sampleIdx++;
            cond++;
            cols.add(col);
        }
    }
    // modify to allow FDA to pass panel relevant information, e.g. initial source for annotation data panel
    protected void addGeneDVItem(ContextMenu cm, TableView tbl, String panel) {
        MenuItem item = new MenuItem("Show gene data visualization");
        cm.getItems().add(item);
        item.setOnAction((event) -> {
            // get highlighted row's data and show gene data visualization
            ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
            if(lstIdxs.size() != 1)
                app.ctls.alertInformation("Display Gene Data Visualization", "You have multiple gene rows highlighted.\nHighlight a single row with the gene you want to visualize.");
            else {
                DataApp.SelectionDataResults rdata = (DataApp.SelectionDataResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                String gene = rdata.getGene().trim();
                String genes[] = gene.split(",");
                if(genes.length > 1) {
                    List<String> lst = Arrays.asList(genes);
                    Collections.sort(lst);
                    ChoiceDialog dlg = new ChoiceDialog(lst.get(0), lst);
                    dlg.setTitle("Gene Data Visualization");
                    dlg.setHeaderText("Select gene to show data visualization for.");
                    Optional<String> result = dlg.showAndWait();
                    if(result.isPresent()) {
                        gene = result.get();
                        showGeneDataVisualization(gene, panel);
                    }
                }
                else
                    showGeneDataVisualization(gene, panel);
            }
        });
        
        // check if we can provide online references
        if(WebRefs.hasOnlineReferences(project)) {
            item = new MenuItem("Show gene online references in web browser");
            cm.getItems().add(item);
            item.setOnAction((event) -> {
                // get highlighted row's data and show gene data visualization
                ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
                if(lstIdxs.size() != 1)
                    app.ctls.alertInformation("Display Gene Online Reference", "You have multiple gene rows highlighted.\nHighlight a single row with the gene to show online references for.");
                else {
                    DataApp.SelectionDataResults rdata = (DataApp.SelectionDataResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                    String gene = rdata.getGene().trim();
                    String genes[] = gene.split(",");
                    if(genes.length > 1) {
                        List<String> lst = Arrays.asList(genes);
                        Collections.sort(lst);
                        ChoiceDialog dlg = new ChoiceDialog(lst.get(0), lst);
                        dlg.setTitle("Gene Online Reference");
                        dlg.setHeaderText("Select gene to show online references for");
                        Optional<String> result = dlg.showAndWait();
                        if(result.isPresent()) {
                            gene = result.get();
                            WebRefs.showGeneOnlineReferences(gene, project);
                        }
                    }
                    else
                        WebRefs.showGeneOnlineReferences(gene, project);
                }
            });
        }
    }
    public void showGeneDataVisualization(String gene, String panel) {
        HashMap<String, Object> hmArgs = new HashMap<>();
        hmArgs.put("panels", panel);
        app.tabs.openTabGeneDataViz(project, project.getDef().id, gene, project.data.getResultsGeneTrans().get(gene), "Project '" + project.getDef().name + "'", hmArgs);
    }
    
    //
    // Data Classes
    //
    static public class SubTabInfo implements Comparable<SubTabInfo>  {
        String subTabId, resId, group, item, title, tooltip, imgName, menuName, description;
        SubTabBase subTabBase;
        Tabs.FloatWindow dlgFloat;
        MenuItem miFloat;
        boolean dock;
        TableView tblSelect;
        ArrayList<TableView> lstSearchTables = null;
        ArrayList<MenuItem> menuOptionsItems = null;
        ContextMenu cmOptions, cmDataViz, cmSelect, cmExport;
        boolean btnOptions, btnEdit, btnSignificance, btnRun, btnExport, btnDataViz, btnDataVizMenu, btnZoom, btnClusters, btnSelect;
        boolean visible, disabled;
        public SubTabInfo(String subTabId, String resId, String group, String item, String title, String tooltip, String menuName, String imgName, String description) {
            // definition data
            this.subTabId = subTabId;
            this.resId = resId;
            this.group = group;
            this.item = item;
            this.title = title;
            this.tooltip = tooltip;
            this.menuName = menuName;
            this.imgName = imgName;
            this.description = description;
            this.btnOptions = false;
            this.btnEdit = false;
            this.btnZoom = false;
            this.btnSignificance = false;
            this.btnRun = false;
            this.btnExport = false;
            this.btnDataViz = false;
            this.btnClusters = false;
            this.btnSelect = false;
            
            // working data
            clear();
        }
        public SubTabInfo(SubTabInfo sti) {
            this.subTabId = sti.subTabId;
            this.resId = sti.resId;
            this.group = sti.group;
            this.item = sti.item;
            this.title = sti.title;
            this.tooltip = sti.tooltip;
            this.menuName = sti.menuName;
            this.imgName = sti.imgName;
            this.description = sti.description;
            this.btnOptions = false;
            this.btnEdit = false;
            this.btnZoom = false;
            this.btnSignificance = false;
            this.btnRun = false;
            this.btnExport = false;
            this.btnDataViz = false;
            this.btnClusters = false;
            this.btnSelect = false;
            
            // working data
            clear();
        }
        public void release() {
            clear();
        }
        private void clear() {
            subTabBase = null;
            dlgFloat = null;
            miFloat = null;
            dock = false;
            visible = true;
            disabled = false;
            tblSelect = null;
            if(lstSearchTables != null)
                lstSearchTables.clear();
            else
                lstSearchTables = new ArrayList<>();
            if(menuOptionsItems != null)
                menuOptionsItems.clear();
            else
                menuOptionsItems = new ArrayList<>();
            cmOptions = null;
            cmDataViz = null;
            cmSelect = null;
            cmExport = null;
        }
        
        @Override
        public int compareTo(SubTabInfo sti) {
            return (description.compareTo(sti.description));
        }
    }
}
