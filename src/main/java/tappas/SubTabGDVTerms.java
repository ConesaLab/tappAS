/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Callback;
import tappas.DbProject.AFStatsData;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabGDVTerms extends SubTabBase {
    TableView<TermData> tblTerms;
    ArrayList<String> lstTrans;
    ArrayList<String> lstColNames;
    String gene;
    boolean varyingDisabled = true;
    boolean varyingOnly = false;
    HashMap<String, HashMap<String, AFStatsData>> hmSources = new HashMap<>();
    HashMap<String, HashMap<String, HashMap<String, Object>>> hmFeatureIDs = new HashMap<>();
    HashMap<String, HashMap<String, HashMap<String, Object>>> hmGeneDiversity = null;
    HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPos = null;
    HashMap<String, HashMap<String, HashMap<String, Object>>> hmNotFeatureIDs = new HashMap<>();
    
    public SubTabGDVTerms(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            HashMap<String, Object> hmTrans = (HashMap<String, Object>)args.get("trans");
            lstTrans = new ArrayList<>();
            lstColNames = new ArrayList<>();
            for(String trans : hmTrans.keySet())
                lstTrans.add(trans);
            Collections.sort(lstTrans, String.CASE_INSENSITIVE_ORDER);
            varyingDisabled = lstTrans.size() < 2;
            gene = (String)args.get("gene");
        }
        return result;
    }
    @Override
    protected void onButtonExport() {
        app.export.exportTableData(tblTerms, true, "tappAS_"+gene+"_Gene_AnnotationFeatures.tsv");
    }    

    //
    // Internal Functions
    //
    private void showSubTab(HashMap<String, HashMap<String, Object>> hmTerms, DataAnnotation.TransTermsData ttData) throws SQLException {
        GridPane grdTerms = (GridPane) tabNode.getParent().lookup("#grdTerms");

        ContextMenu cm = new ContextMenu();
        CheckMenuItem check;
        MenuItem mi = new MenuItem("Annotation Features Filter...");
        mi.setOnAction((event) -> { try {
            selectFeaturesDisplayed(hmTerms, ttData);
            } catch (SQLException ex) {
                Logger.getLogger(SubTabGDVTerms.class.getName()).log(Level.SEVERE, null, ex);
            }
});
        cm.getItems().add(mi);
        check = new CheckMenuItem("Show ONLY Varying Features");
        check.setDisable(varyingDisabled);
        check.setOnAction((event) -> { 
            varyingOnly = ((CheckMenuItem)(event.getSource())).isSelected();
            try {
                updateTermTableData(hmTerms, ttData);
            } catch (SQLException ex) {
                Logger.getLogger(SubTabGDVTerms.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        cm.getItems().add(check);
        subTabInfo.cmOptions = cm;

        // add column names in same order as transcripts
        for(String trans : lstTrans) {
            String protein = project.data.getTransProtein(trans);
            lstColNames.add(trans + (protein.isEmpty()? "" : "\n" + protein));
        }
        createTermTable(lstColNames, grdTerms);
        updateTermTableData(hmTerms, ttData);
        cm = new ContextMenu();
        app.export.setupSimpleTableExport(tblTerms, cm, false, "tappAS_"+gene+"_Gene_AnnotationFeatures.tsv");
        setFocusNode(tblTerms, true);
    }
    private void selectFeaturesDisplayed(HashMap<String, HashMap<String, Object>> hmTerms, DataAnnotation.TransTermsData ttData) throws SQLException {
        Window wnd = Tappas.getWindow();
        DlgGDVFilterFeatures dlg = new DlgGDVFilterFeatures(project, wnd);
        DlgGDVFilterFeatures.Params results = dlg.showAndWait(new DlgGDVFilterFeatures.Params(hmNotFeatureIDs, false), "Gene", hmFeatureIDs);
        if(results != null) {
            hmNotFeatureIDs = results.hmNotFeatureIDs;
            updateTermTableData(hmTerms, ttData);
        }
    }
    private void createTermTable(ArrayList<String> colNames, GridPane grdTerms) {
        ObservableList<TermData> data = FXCollections.observableArrayList();
        tblTerms = new TableView<>(data);
        tblTerms.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ArrayList<TableColumn<TermData, String>> cols = new ArrayList<>();

        TableColumn<TermData, String> colType = new TableColumn<>("Feature");
        colType.setMinWidth(20);
        colType.setPrefWidth(120);
        colType.setEditable(false);
        colType.getStyleClass().add("fastair-table-col-bottomcenter");
        colType.setCellValueFactory(new PropertyValueFactory<>("Category"));
        cols.add(colType);
        TableColumn<TermData, String> colTerm = new TableColumn<>("Id");
        colTerm.setMinWidth(50);
        colTerm.setPrefWidth(100);
        colTerm.setEditable(false);
        colTerm.getStyleClass().add("fastair-table-col-bottomcenter");
        colTerm.setCellValueFactory(new PropertyValueFactory<>("Term"));
        cols.add(colTerm);
        TableColumn<TermData, String> colName = new TableColumn<>("Name/Description");
        colName.setMinWidth(50);
        colName.setPrefWidth(200);
        colName.setEditable(false);
        colName.getStyleClass().add("fastair-table-col-bottomcenter");
        colName.setCellValueFactory(new PropertyValueFactory<>("Name"));
        cols.add(colName);

        // loop through all transcripts
        TableColumn<TermData, String> col;
        int colNum = 1;
        int colcnt = colNames.size();
        for(String name : colNames) {
            col = new TableColumn<>("");
            if(colcnt > 1) {
                col.setMinWidth(40);
                col.setPrefWidth(45);
                col.setEditable(false);
                col.getStyleClass().add("fastair-table-col-bottomcenter");
                Label l = new Label(name);
                l.setStyle("-fx-font-size: small;");
                VBox vbox = new VBox(l);
                vbox.setRotate(-90);
                vbox.setPadding(new Insets(1,1,1,1));
                Group g = new Group(vbox);
                col.setGraphic(g);
            }
            else {
                col.setText(name);
                col.setMinWidth(60);
                col.setPrefWidth(150);
                col.setEditable(false);
            }
            col.setStyle("-fx-alignment:CENTER;");
            col.setCellValueFactory(getTermDataCallback(colNum));
            cols.add(col);
            colNum++;
        }
        tblTerms.getColumns().addAll(cols);
        ContextMenu cm = new ContextMenu();
        app.export.setupTableExport(tblTerms, cm, false, "tappas_DV_SelectedDBTerms", "tappas_DV_DBTerms");
        tblTerms.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblTerms);
        grdTerms.add(tblTerms, 0, 0);
    }
    private Callback getTermDataCallback(int colnum) {
        return (new Callback<TableColumn.CellDataFeatures<TermData, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<TermData, String> p) {
                final int colidx = colnum - 1;
                return p.getValue().transcripts[colidx];
            }
        });
    }
    private void updateTermTableData(HashMap<String, HashMap<String, Object>> hmTerms, DataAnnotation.TransTermsData ttData) throws SQLException {
        ObservableList<TermData> data = FXCollections.observableArrayList();
        int transcnt = lstTrans.size();
        String[] trans = new String[transcnt];
        for(DataAnnotation.Term term : ttData.terms) {
            // check if we want to exclude this source:feature from display
            if(project.data.isRsvdNoDiversityFeature(term.db, term.category))
                continue;
            // check source:feature:featureId is excluded
            if(hmNotFeatureIDs.containsKey(term.db)) {
                //HashMap<String, HashMap<String, HashMap<String, Object>>> hmNotFeatureIDs = new HashMap<>();
                HashMap<String, HashMap<String, Object>> hmFeatures = hmNotFeatureIDs.get(term.db);
                if(hmFeatures.isEmpty())
                    continue;
                else if(hmFeatures.containsKey(term.category)) {
                    HashMap<String, Object> hmFIds = hmFeatures.get(term.category);
                    if(hmFIds.isEmpty() || hmFIds.containsKey(term.id))
                        continue;
                }
            }
            for(int i = 0; i < transcnt; i++)
                trans[i] = "";
            int idx = 0;
            String key;
            int contains = 0;
            boolean diffVals = false;
            int value = Integer.MAX_VALUE;
            for(HashMap<String, Integer> hmt : ttData.transTerms) {
                key = term.db + term.category + term.id;
                if(hmt.containsKey(key)) {
                    trans[idx] = hmt.get(key).toString();
                    if(idx == 0)
                        value = hmt.get(key);
                    else {
                        if(value != hmt.get(key))
                            diffVals = true;
                    }
                    contains++;
                }
                idx++;
            }

            // check if varying ONLY not selected, show all processed features
            if(!varyingOnly)
                data.add(new TermData(term.id, term.name, term.db + ": " + term.category, trans));
            else {
                // varying ONLY - add if the values are different, count wise
                if((diffVals || (contains != 0 && contains != transcnt)))
                    data.add(new TermData(term.id, term.name, term.db + ": " + term.category, trans));
                else {
                    // it is possible that they all had the same count but still have genomic position overlap differences
                    if(isVaryingFeatureId(gene, hmTerms, term.db, term.category, term.id))
                        data.add(new TermData(term.id, term.name, term.db + ": " + term.category, trans));
                }
            }
        }
        FXCollections.sort(data);
        tblTerms.setItems(data);
        if(data.size() > 0)
            tblTerms.getSelectionModel().select(0);
        boolean filtered = varyingOnly | !hmNotFeatureIDs.isEmpty();
        setTableFiltered(tblTerms, filtered);
    }
    private HashMap<String, HashMap<String, HashMap<String, Object>>> getFeatureIds(HashMap<String, HashMap<String, Object>> hmTerms, DataAnnotation.TransTermsData ttData) {
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmFIDs = new HashMap<>(); 
        for(DataAnnotation.Term term : ttData.terms) {
            if(!project.data.isRsvdNoDiversityFeature(term.db, term.category)) {
                if(hmFIDs.containsKey(term.db)) {
                    HashMap<String, HashMap<String, Object>> hmS = hmFIDs.get(term.db);
                    if(hmS.containsKey(term.category)) {
                        HashMap<String, Object> hmSC = hmS.get(term.category);
                        hmSC.put(term.id, null);
                    }
                    else {
                        HashMap<String, Object> hmSC = new HashMap<>();
                        hmSC.put(term.id, null);
                        hmS.put(term.category, hmSC);
                    }
                }
                else {
                    HashMap<String, HashMap<String, Object>> hmS = new HashMap<>();
                    HashMap<String, Object> hmSC = new HashMap<>();
                    hmSC.put(term.id, null);
                    hmS.put(term.category, hmSC);
                    hmFIDs.put(term.db, hmS);
                }
            }
        }
        return hmFIDs;
    }
    private boolean isVaryingFeatureId(String gene, HashMap<String, HashMap<String, Object>> hmFeatures, String source, String feature, String featureId) throws SQLException {
        boolean result = false;
        
        // see if don't have the varying information
        if(hmGeneDiversity == null) {
            HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap();
            HashMap<String, Object> hmTrans = project.data.getGeneTrans(gene);
            hmGeneTrans.put(gene, hmTrans);
            if(hmTransAFIdPos == null) {
                ArrayList<String> lstGT = new ArrayList();
                for(String id : hmTrans.keySet())
                    lstGT.add(id);
                hmTransAFIdPos = project.data.loadTransAFIdPos(lstGT);
            }                
            hmGeneDiversity = project.data.getDiverseFeaturesUsingGenomicPositionExons(hmGeneTrans, hmFeatures, true, hmTransAFIdPos);
        }
        if(hmGeneDiversity != null) {
            String fields[];
            for(String info : hmGeneDiversity.keySet()) {
                fields = info.split("\t");
                if(fields.length == 4) {
                    if(fields[0].equals(source)) {
                        if(fields[1].equals(feature)) {
                            if(fields[2].equals(featureId)) {
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        HashMap<String, HashMap<String, Object>> hmTerms = new HashMap<>();
        HashMap<String, Object> hm = new HashMap<>();
        hmSources = project.data.getAFStats();
        for(String source : hmSources.keySet())
            hmTerms.put(source, hm);
        DataAnnotation.TransTermsData data = project.data.getTransTermsData(gene, lstTrans, hmTerms, true);
        hmFeatureIDs = getFeatureIds(hmTerms, data);
        try {
            showSubTab(hmTerms, data);
        } catch (SQLException ex) {
            Logger.getLogger(SubTabGDVTerms.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //
    // Data Classes
    //
    public static class TermData  implements Comparable<TermData> {
        public final SimpleStringProperty term;
        public final SimpleStringProperty name;
        public final SimpleStringProperty cat;
        public final SimpleStringProperty[] transcripts;
 
        public TermData(String term, String name, String cat, String[] transcripts) {
            this.term = new SimpleStringProperty(term);
            this.name = new SimpleStringProperty(name);
            this.cat = new SimpleStringProperty(cat);
            int cnt = transcripts.length;
            this.transcripts = new SimpleStringProperty[cnt];
            for(int i = 0; i < cnt; i++)
                this.transcripts[i] = new SimpleStringProperty(transcripts[i]);
        }
        public String getTerm() { return term.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return cat.get(); }
        
        @Override
        public int compareTo(TermData td) {
            return (cat.get().compareToIgnoreCase(td.cat.get()));
        }
    }
}
