/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import tappas.DataApp.EnumData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgSelectGraph extends DlgBase {
    Label lblItemGroups;
    ComboBox cbItemGroups;
    ListView lvGraphs;

    Params dfltParams;
    private static ArrayList<ImageData> lstImages = new ArrayList<>();
    ArrayList<EnumData> lstItemGroups = new ArrayList<>();
    AppController.DAMenuFlags daflags;
    AppController.EAMenuFlags eaflags;
    AppController.MAMenuFlags maflags;
    
    public DlgSelectGraph(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParamsArg) {
        // create default parameters if not given
        dfltParams = dfltParamsArg == null? new Params() : dfltParamsArg;

        // setup acording to item type specified
        String type = "data";
        String lbl = "Data Type";
        switch(dfltParams.itemType) {
            case AF:
                daflags = app.ctlr.getDAMenuFlags();
                maflags = app.ctlr.getMAMenuFlags();

                lstItemGroups.add(new EnumData("", "All"));
                lstItemGroups.add(new EnumData(Params.AFItems.Features.name(), "Annotation Features"));
                if(maflags.viewDiversity)
                    lstItemGroups.add(new EnumData(Params.AFItems.FDA.name(), "Functional Diversity Analysis"));
                if(daflags.hasDFI)
                    lstItemGroups.add(new EnumData(Params.AFItems.DFI.name(), "Differential Feature Inclusion Analysis"));
                type = "annotation features";
                lbl = "Visualization";
                break;
            case Data:
                lstItemGroups.add(new EnumData("", "All"));
                lstItemGroups.add(new EnumData(Params.DataItems.Transcripts.name(), "Transcripts"));
                lstItemGroups.add(new EnumData(Params.DataItems.Proteins.name(), "Proteins"));
                lstItemGroups.add(new EnumData(Params.DataItems.Genes.name(), "Genes"));
                break;
            case DA:
                daflags = app.ctlr.getDAMenuFlags();
                lstItemGroups.add(new EnumData("", "All"));
                if(daflags.hasDEA) {
                    if(daflags.hasDEA_Trans)
                        lstItemGroups.add(new EnumData(Params.DAItems.DEATranscripts.name(), "Differential Expression Analysis - Transcripts"));
                    if(daflags.hasDEA_Protein)
                        lstItemGroups.add(new EnumData(Params.DAItems.DEAProteins.name(), "Differential Expression Analysis - Proteins"));
                    if(daflags.hasDEA_Gene)
                        lstItemGroups.add(new EnumData(Params.DAItems.DEAGenes.name(), "Differential Expression Analysis - Genes"));
                }
                if(daflags.hasDIU)
                    lstItemGroups.add(new EnumData(Params.DAItems.DIU.name(), "Differential Isoform Usage"));
                type = "differential analysis";
                lbl = "Analysis";
                break;
            case EA:
                eaflags = app.ctlr.getEAMenuFlags();
                lstItemGroups.add(new EnumData("", "All"));
                if(eaflags.hasFEA)
                    lstItemGroups.add(new EnumData(Params.EAItems.FEA.name(), "Functional Enrichment Analysis"));
                if(eaflags.hasGSEA)
                    lstItemGroups.add(new EnumData(Params.EAItems.GSEA.name(), "Gene Set Enrichment Analysis"));
                type = "enrichment analysis";
                lbl = "Analysis";
                break;
            case EM:
                lstItemGroups.add(new EnumData("", "All"));
                lstItemGroups.add(new EnumData(Params.EMItems.ExpressionMatrix.name(), "Expression Matrix"));
                break;
        }
        
        // create dialog
        String dlgTitle = "View " + type + " graphs, charts, and summaries";
        if(createDialog("SelectGraph.fxml", dlgTitle, true, "Help_Dlg_SelGraph.html")) {
            setProjectName();

            // get control objects
            lblItemGroups = (Label) scene.lookup("#lblItemGroups");
            cbItemGroups = (ComboBox) scene.lookup("#cbItemGroups");
            lvGraphs = (ListView) scene.lookup("#lvGraphs");

            // populate dialog
            lblItemGroups.setText(lbl + ":");
            for(EnumData item : lstItemGroups)
                cbItemGroups.getItems().add(item.name);
                        
            // setup listeners and bindings
            lvGraphs.setCellFactory(listView -> new ListCell<SubTabBase.SubTabInfo>() {
                private ImageView imageView = new ImageView();
                @Override
                public void updateItem(SubTabBase.SubTabInfo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText("  " + item.description);
                        imageView.setImage(getImage(item.imgName));
                        setGraphic(imageView);
                        setOnMouseClicked((MouseEvent event) -> {
                            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1){
                                ObservableList<ButtonType> lst = dialog.getDialogPane().getButtonTypes();
                                for(ButtonType bt : lst) {
                                    if(bt.getText().equals("OK")) {
                                        if(dialog.getDialogPane().lookupButton(bt) != null) {
                                            System.out.println("Firing OK button...");
                                            ((Button) dialog.getDialogPane().lookupButton(bt)).fire();
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            });            
            cbItemGroups.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                ArrayList<SubTabBase.SubTabInfo> lstItems = TabProjectDataViz.getItemTypePanels(dfltParams.itemType);
                ObservableList<SubTabBase.SubTabInfo> olItems = FXCollections.observableArrayList();
                ArrayList<SubTabBase.SubTabInfo> dynamicItems;
                String sel = "";
                if(cbItemGroups.getSelectionModel().getSelectedItem() != null)
                    sel = (String) cbItemGroups.getSelectionModel().getSelectedItem();
                boolean allflg = sel.equals("All");
                switch(dfltParams.itemType) {
                    case AF:
                        for(SubTabBase.SubTabInfo sti : lstItems) {
                            String[] items = sti.item.split(";");
                            for(String item : items) {
                                boolean addit = false;
                                if(item.equals(Params.AFItems.Features.name())) {
                                    if(allflg || sel.equals("Annotation Features"))
                                        addit = true;
                                }
                                else if(item.equals(Params.AFItems.FDA.name())) {
                                    if(maflags.viewDiversity) {
                                        if(allflg || sel.equals("Functional Diversity Analysis"))
                                            addit = true;
                                    }
                                }
                                else if(item.equals(Params.AFItems.DFI.name())) {
                                    if(daflags.hasDFI) {
                                        if(allflg || sel.equals("Differential Feature Inclusion Analysis"))
                                            addit = true;
                                    }
                                }
                                
                                // check if OK to add item
                                if(addit) {
                                    olItems.add(sti);
                                    break;
                                }
                            }
                        }
                        break;
                    case Data:
                        for(SubTabBase.SubTabInfo sti : lstItems) {
                            String[] items = sti.item.split(";");
                            for(String item : items) {
                                boolean addit = false;
                                if(item.equals(Params.DataItems.Transcripts.name())) {
                                    if(allflg || sel.equals("Transcripts"))
                                        addit = true;
                                }
                                else if(item.equals(Params.DataItems.Proteins.name())) {
                                    if(allflg || sel.equals("Proteins"))
                                        addit = true;
                                }
                                else if(item.equals(Params.DataItems.Genes.name())) {
                                    if(allflg || sel.equals("Genes"))
                                        addit = true;
                                }
                                
                                // check if OK to add item
                                if(addit) {
                                    olItems.add(sti);
                                    break;
                                }
                            }
                        }
                        break;
                    case DA:
                        for(SubTabBase.SubTabInfo sti : lstItems) {
                            String[] items = sti.item.split(";");
                            for(String item : items) {
                                boolean addit = false;
                                if(item.equals(Params.DAItems.DEATranscripts.name())) {
                                    if(daflags.hasDEA_Trans) {
                                        if(allflg || sel.equals("Differential Expression Analysis - Transcripts"))
                                            addit = true;
                                    }
                                }
                                else if(item.equals(Params.DAItems.DEAProteins.name())) {
                                    if(daflags.hasDEA_Protein) {
                                        if(allflg || sel.equals("Differential Expression Analysis - Proteins"))
                                            addit = true;
                                    }
                                }
                                else if(item.equals(Params.DAItems.DEAGenes.name())) {
                                    if(daflags.hasDEA_Gene) {
                                        if(allflg || sel.equals("Differential Expression Analysis - Genes"))
                                            addit = true;
                                    }
                                }
                                else if(item.equals(Params.DAItems.DIU.name())) {
                                    if(daflags.hasDIU) {
                                        if(allflg || sel.equals("Differential Isoform Usage "))
                                            addit = true;
                                    }
                                }
                                
                                // check if OK to add item
                                if(addit) {
                                    olItems.add(sti);
                                    break;
                                }
                            }
                        }
                        break;
                    case EA:
                        dynamicItems = TabProjectDataViz.getItemTypeDynamicPanels(dfltParams.itemType);
                        ArrayList<DataApp.EnumData> lstFEA = project.data.analysis.getFEAResultsList();
                        ArrayList<DataApp.EnumData> lstGSEA = project.data.analysis.getGSEAResultsList();
                        for(SubTabBase.SubTabInfo sti : lstItems) {
                            String[] items = sti.item.split(";");
                            for(String item : items) {
                                boolean addit = false;
                                if(item.equals(Params.EAItems.FEA.name())) {
                                    if(eaflags.hasFEA) {
                                        if(allflg || sel.equals("Functional Enrichment Analysis"))
                                            addit = true;
                                    }
                                }
                                else if(item.equals(Params.EAItems.GSEA.name())) {
                                    if(eaflags.hasGSEA) {
                                        if(allflg || sel.equals("Gene Set Enrichment Analysis"))
                                            addit = true;
                                    }
                                }
                                
                                // check if OK to add item
                                if(addit) {
                                    olItems.add(sti);
                                    break;
                                }
                            }
                        }
                        if(eaflags.hasFEA && (allflg || sel.equals("Functional Enrichment Analysis"))) {
                            for(DataApp.EnumData data : lstFEA) {
                                for(SubTabBase.SubTabInfo sti : dynamicItems) {
                                    if(sti.item.equals(Params.EAItems.FEA.name())) {
                                        SubTabBase.SubTabInfo dsti = new SubTabBase.SubTabInfo(sti.subTabId, sti.resId, sti.group, sti.item, sti.title, sti.tooltip, sti.menuName, sti.imgName, sti.description);
                                        dsti.subTabId = sti.subTabId + "\t" + data.id;
                                        dsti.description = sti.description + " - " + data.name;
                                        olItems.add(dsti);
                                    }
                                }
                            }
                        }
                        if(eaflags.hasGSEA && (allflg || sel.equals("Gene Set Enrichment Analysis"))) {
                            for(DataApp.EnumData data : lstGSEA) {
                                for(SubTabBase.SubTabInfo sti : dynamicItems) {
                                    if(sti.item.equals(Params.EAItems.GSEA.name())) {
                                        SubTabBase.SubTabInfo dsti = new SubTabBase.SubTabInfo(sti.subTabId, sti.resId, sti.group, sti.item, sti.title, sti.tooltip, sti.menuName, sti.imgName, sti.description);
                                        dsti.subTabId = sti.subTabId + "\t" + data.id;
                                        dsti.description = sti.description + " - " + data.name;
                                        olItems.add(dsti);
                                    }
                                }
                            }
                        }
                        
                        FXCollections.sort(olItems);
                        break;
                    case EM:
                        for(SubTabBase.SubTabInfo sti : lstItems) {
                            String[] items = sti.item.split(";");
                            for(String item : items) {
                                boolean addit = false;
                                if(item.equals(Params.EMItems.ExpressionMatrix.name())) {
                                    if(allflg || sel.equals("Expression Matrix"))
                                        addit = true;
                                }
                                
                                // check if OK to add item
                                if(addit) {
                                    olItems.add(sti);
                                    break;
                                }
                            }
                        }
                        break;
                }
                lvGraphs.setItems(olItems);
                if(lvGraphs.getItems().size() > 0)
                    lvGraphs.getSelectionModel().select(0);
            });
            
            // select after adding listener
            if(cbItemGroups.getItems().size() > 0)
                cbItemGroups.getSelectionModel().select(0);

            // setup dialog event handlers
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> params = null;
                System.out.println(b.getButtonData().toString());
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    params = validate(dialog);
                return params;
            });

            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return new Params(result.get());
        }
        return null;
    }
    private Image getImage(String imgName) {
        Image img = null;
        for(ImageData id : lstImages) {
            if(id.name.equals(imgName)) {
                img = id.image;
                break;
            }
        }
        if(img == null) {
            img = new Image(getClass().getResourceAsStream("/tappas/images/" + imgName));
            lstImages.add(new ImageData(imgName, img));
        }
        return img;
    }

    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        int gidx = cbItemGroups.getSelectionModel().getSelectedIndex();
        if(gidx >= 0) {
            results.put(Params.ITEMTYPE_PARAM, dfltParams.itemType.name());
            results.put(Params.ITEMGROUP_PARAM, lstItemGroups.get(gidx).id);
            SubTabBase.SubTabInfo item = (SubTabBase.SubTabInfo) lvGraphs.getSelectionModel().getSelectedItem();
            if(item != null)
                results.put(Params.ITEMPANEL_PARAM, item.subTabId);
            else {
                lvGraphs.requestFocus();
                errmsg = "You must select an item from list.";
            }
        }
        else {
            cbItemGroups.requestFocus();
            errmsg = "You must select a type from drop-down list.";
        }
        if(!errmsg.isEmpty())
            results.put("ERRMSG", errmsg);
        return results;
    }

    //
    // Data Classes
    //

    public static class ImageData {
        String name;
        Image image;
        public ImageData(String name, Image image) {
            this.name = name;
            this.image = image;
        }
    }
    public static class Params extends DlgParams {
        public static final String ITEMTYPE_PARAM = "itemType";
        public static final String ITEMGROUP_PARAM = "itemGroup";
        public static final String ITEMPANEL_PARAM = "itemPanel";
        private static ItemType dfltItemType = ItemType.Data;
        
        public static enum ItemType { AF, Data, DA, EA, EM }
        public static enum DataItems { Transcripts, Proteins, Genes }
        public static enum AFItems { Features, FDA, DFI }
        public static enum DAItems { DEATranscripts, DEAProteins, DEAGenes, DIU }
        public static enum EAItems { FEA, GSEA }
        public static enum EMItems { ExpressionMatrix }
        
        ItemType itemType;
        String itemGroup, itemPanel;
        public Params() {
            this.itemType = dfltItemType;
            this.itemGroup = "";
            this.itemPanel = "";
        }
        public Params(ItemType itemType, String itemGroup, String itemPanel) {
            this.itemType = itemType;
            this.itemGroup = itemGroup;
            this.itemPanel = itemPanel;
        }
        public Params(HashMap<String, String> hmParams) {
            this.itemType = hmParams.containsKey(ITEMTYPE_PARAM)? ItemType.valueOf(hmParams.get(ITEMTYPE_PARAM)) : dfltItemType;
            this.itemGroup = hmParams.containsKey(ITEMGROUP_PARAM)? hmParams.get(ITEMGROUP_PARAM) : "";
            this.itemPanel = hmParams.containsKey(ITEMPANEL_PARAM)? hmParams.get(ITEMPANEL_PARAM) : "";
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(ITEMTYPE_PARAM, itemType.name());
            hm.put(ITEMPANEL_PARAM, itemPanel);
            return hm;
        }
        // base class implements boolean save(String filepath)
        
        //
        // Static functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
    }
}
