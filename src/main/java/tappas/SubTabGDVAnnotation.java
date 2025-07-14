/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabGDVAnnotation extends SubTabBase {
    TableView tblAnnotation;

    String gene;
    ArrayList<String> lstTrans;
    
    public SubTabGDVAnnotation(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            gene = (String)args.get("gene");
        }
        return result;
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        if(obj != null && obj == tblAnnotation) {
            TabBase.SearchInfo si = (TabBase.SearchInfo) tblAnnotation.getUserData();
            si.txt = txt;
            ObservableList<TransAnnotationDataList> items = (ObservableList<TransAnnotationDataList>) si.data;
            if(items != null) {
                ObservableList<TransAnnotationDataList> matchItems;
                if(txt.isEmpty()) {
                    // restore original dataset
                    matchItems = items;
                }
                else {
                    matchItems = FXCollections.observableArrayList();
                    for(TransAnnotationDataList data : items) {
                        if(data.getTransId().toLowerCase().contains(txt) || data.getSource().toLowerCase().contains(txt) ||
                                data.getFeature().toLowerCase().contains(txt) || data.getAttributes().toLowerCase().contains(txt))
                            matchItems.add(data);
                    }
                }
                tblAnnotation.setItems(matchItems);
                if(tblAnnotation.getItems().size() > 0 && tblAnnotation.getSelectionModel().getSelectedIndices().isEmpty())
                    tblAnnotation.getSelectionModel().select(0);
            }
        }
    }
    @Override
    protected void onButtonExport() {
        app.export.exportTableData(tblAnnotation, true, "tappAS_"+gene+"_Gene_Annotation.tsv");
    }    

    //
    // Internal Functions
    //
    private void showSubTab(ArrayList<DataAnnotation.AnnotationDataEntry> taData) {
        tblAnnotation  = (TableView) tabNode.getParent().lookup("#tblAnnotation");
        
        tblAnnotation.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<TransAnnotationDataList> data = FXCollections.observableArrayList();
        for(DataAnnotation.AnnotationDataEntry tad : taData)
            data.add(new TransAnnotationDataList(tad.transID, tad.db, tad.category, tad.start, tad.end, 
                    tad.score, tad.strand, tad.phase, tad.attrs));
        ObservableList<TableColumn> cols = tblAnnotation.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("TransId"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Source"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Feature"));
        cols.get(3).setCellValueFactory(new PropertyValueFactory<>("Start"));
        cols.get(4).setCellValueFactory(new PropertyValueFactory<>("End"));
        cols.get(5).setCellValueFactory(new PropertyValueFactory<>("Score"));
        cols.get(6).setCellValueFactory(new PropertyValueFactory<>("Strand"));
        cols.get(7).setCellValueFactory(new PropertyValueFactory<>("Phase"));
        cols.get(8).setCellValueFactory(new PropertyValueFactory<>("Attributes"));
        tblAnnotation.setItems(data);
        if(tblAnnotation.getItems().size() > 0)
            tblAnnotation.getSelectionModel().select(0);
        ContextMenu cm = new ContextMenu();
        app.export.setupSimpleTableExport(tblAnnotation, cm, false, "tappAS_"+gene+"_Gene_Annotation.tsv");
        tblAnnotation.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblAnnotation);
        tblAnnotation.setUserData(new TabBase.SearchInfo(tblAnnotation.getItems(), "", false));
        setupTableSearch(tblAnnotation);
        subTabInfo.lstSearchTables.add(tblAnnotation);
        setFocusNode(tblAnnotation, true);
    }

    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        HashMap<String, Object> hmTrans = (HashMap<String, Object>)args.get("trans");
        lstTrans = new ArrayList<>();
        for(String trans : hmTrans.keySet())
            lstTrans.add(trans);
        Collections.sort(lstTrans, String.CASE_INSENSITIVE_ORDER);
        ArrayList<DataAnnotation.AnnotationDataEntry> data = project.data.getTransAnnotationData(gene, lstTrans);
        showSubTab(data);
    }

    //
    // Data Classes
    //
    public static class TransAnnotationDataList {
        public SimpleStringProperty transID;
        public SimpleStringProperty source;
        public SimpleStringProperty feature;
        public SimpleIntegerProperty start;
        public SimpleIntegerProperty end;
        public SimpleStringProperty score;
        public SimpleStringProperty strand;
        public SimpleStringProperty phase;
        public SimpleStringProperty attrs;
        
        public TransAnnotationDataList(String transID, String source, String feature, int start, int end, 
                String score, String strand, String phase, String attrs) {
            this.transID = new SimpleStringProperty(transID);
            this.source = new SimpleStringProperty(source);
            this.feature = new SimpleStringProperty(feature);
            this.start = new SimpleIntegerProperty(start);
            this.end = new SimpleIntegerProperty(end);
            this.score = new SimpleStringProperty(score);
            this.strand = new SimpleStringProperty(strand);
            this.phase = new SimpleStringProperty(phase);
            this.attrs = new SimpleStringProperty(attrs);
        }
        public String getTransId() { return transID.get(); }
        public String getSource() { return source.get(); }
        public String getFeature() { return feature.get(); }
        public Integer getStart() { return start.get(); }
        public Integer getEnd() { return end.get(); }
        public String getScore() { return score.get(); }
        public String getStrand() { return strand.get(); }
        public String getPhase() { return phase.get(); }
        public String getAttributes() { return attrs.get(); }
    }
}
