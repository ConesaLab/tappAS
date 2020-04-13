/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TabGeneDataViz extends TabBase {
    static int TABIDX_TRANSCIPT = 0;
    static int TABIDX_PROTEIN = 1;
    static int TABIDX_GENOMIC = 2;
    public static int MAX_GENE_TRANS = 25;
    static double MAX_PPB = 2.0;
    static double MAX_WIDTHFACTOR = 10.0;
    static double MIN_WIDTHFACTOR = 1.0;
    static double DELTA_WIDTHFACTOR = 1.0;
    enum Panels { TRANS, PROTEIN, GENOMIC, ONTOLOGY, EXPCHARTS, TERMS, ANNOTATION };
    enum Views { TRANS, PROTEIN, GENOMIC };    
    SubTabBase.SubTabInfo _subTabsInfo[] = {
        // subTabId, resId, group, title, tooltip, menuName, imgName, description
        new SubTabBase.SubTabInfo(Panels.TRANS.name(),     "GeneDataViz", "", "", "Transcripts", "Transcript Data Visualization", "", "trans.png", "gene transcripts annotation data visualization"),
        new SubTabBase.SubTabInfo(Panels.PROTEIN.name(),   "GeneDataViz", "", "", "Proteins", "Protein Data Visualization", "", "prots.png", "gene proteins annotation data visualization"),
        new SubTabBase.SubTabInfo(Panels.GENOMIC.name(),   "GeneDataViz", "", "", "Genomic", "Genomic Data Visualization", "", "geno.png", "genomic annotation data visualization"),
        new SubTabBase.SubTabInfo(Panels.ONTOLOGY.name(),  "GOGraph", "", "", "Gene Ontology", "Gene Ontology Data Visualization", "", "go.png", "gene ontology data visualization"),
        new SubTabBase.SubTabInfo(Panels.EXPCHARTS.name(), "GeneDVExpCharts", "", "", "Expression Charts", "Expression Charts", "", "expCharts.png", "expression levels charts"),
        new SubTabBase.SubTabInfo(Panels.TERMS.name(),     "GeneDVTerms", "", "", "Annotation Features Diversity", "Annotation Features Diversity", "", "aiso.png", "annotation features cross table shows diversity among isoforms"),
        new SubTabBase.SubTabInfo(Panels.ANNOTATION.name(),"GeneDVAnnotation", "", "", "Annotation File Data", "Annotation File Data", "", "afile.png", "all annotation file entries for selected gene including transcripts, proteins, etc."),
    };
    TabBase.TabDef _tabDef = new TabBase.TabDef("GDV", "Gene Annotation Data Visualization", "Gene Annotation Data Visualization", "Gene Annotation Data Visualization", "Gene Annotation Data Visualization: ", "geneTab.png", _subTabsInfo);

    String gene = "";
    String caption = "";
    HashMap<String, Object> hmTrans = new HashMap<>();
    int transCnt = 0;
    int proteinCnt = 0;
    private ArrayList<String[]> geneAnnotations;
    
    public TabGeneDataViz(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(String tabId, TabPane tpParent, HashMap<String, Object> args) {
        super.initialize(tabId, tpParent, args);
        boolean result = false;
        if(args.containsKey("gene")) {
            gene = (String) args.get("gene");
            if(args.containsKey("trans"))
                hmTrans = (HashMap<String, Object>) args.get("trans");
            else
                hmTrans = project.data.getGeneTrans(gene);
            transCnt = hmTrans.size();
            for(String trans : hmTrans.keySet()) {
                if(project.data.isTransCoding(trans))
                    proteinCnt++;
            }
            if(args.containsKey("caption"))
                caption = (String) args.get("caption");
            _tabDef.title = "\"" + gene + "\"";
            _tabDef.tooltip = "Gene Data Visualization: '" + gene + "'" + (caption.isEmpty()? "" : " for " + caption);
            result = initializeTab(_tabDef);
            if(result)
            {
                // determine which panel(s) to open
                if(!openRequestedPanels())
                    openSubTab(_subTabsInfo[Panels.TRANS.ordinal()], true);
            }
        }
        else
            app.logError("Missing required argument for Data visualization (Internal Error).");        
        return result;
    }
    public String getCaption() { return caption; }
    @Override
    public String getState() {
        String state = "";
        if(args.containsKey("gene"))
            state += (state.isEmpty()? "" : "\t") + "gene\t" + args.get("gene");
        if(args.containsKey("caption"))
            state += (state.isEmpty()? "" : "\t") + "caption\t" + args.get("caption");
        return state;
    }
    @Override
    public SubTabBase createSubTabInstance(SubTabBase.SubTabInfo info) {
        SubTabBase sub = null;
        HashMap<String, Object> hmSubArgs = new HashMap<>();
        for(String key : args.keySet())
            hmSubArgs.put(key, args.get(key));
        hmSubArgs.put("gene", gene);
        hmSubArgs.put("trans", hmTrans);
        try {
            Panels panel = Panels.valueOf(info.subTabId);
            switch(panel) {
                case TRANS:
                    sub = new SubTabGDVTrans(project);
                    break;
                case PROTEIN:
                    sub = new SubTabGDVProtein(project);
                    break;
                case GENOMIC:
                    sub = new SubTabGDVGenomic(project);
                    break;
                case ONTOLOGY:
                    sub = new SubTabGOGraph(project);
                    break;
                case TERMS:
                    sub = new SubTabGDVTerms(project);
                    break;
                case EXPCHARTS:
                    sub = new SubTabGDVExpCharts(project);
                    break;
                case ANNOTATION:
                    sub = new SubTabGDVAnnotation(project);
                    break;
            }
        }
        catch(Exception e) {
            logger.logError("Internal program error: " + e.getMessage());
        }
        if(sub != null)
            sub.initialize(tbObj, info, hmSubArgs);
        return sub;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("panels")) {
            openPanels((String)hm.get("panels"));
        }
        else if(hm.containsKey("getData")) {
            // only used for GO terms - OK as long as not transcripts or proteins
            getGeneAnnotations();
            ArrayList<String> lst = new ArrayList<>();
            String sel = (String) hm.get("getData");
            if(sel.startsWith("All"))
                lst = getGOTerms("", geneAnnotations);
            else {
                int pos = sel.indexOf("Transcript ");
                if(pos != -1) {
                    String trans = sel.substring(pos + "Transcript ".length());
                    pos = trans.indexOf(" ");
                    if(pos != -1)
                        trans = trans.substring(0, pos);
                    lst = getGOTerms(trans, geneAnnotations);
                }
            }
            obj = lst;
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
                        break;
                    }
                }
            }
        }
    }
    
    public String getGene() { return gene; };
    public HashMap<String, Object> getTrans() { return hmTrans; };
    public ArrayList<String[]> getGeneAnnotations() {
        getGeneAnnotationsData();
        return geneAnnotations;
    }
    public boolean isTabSelected() { return tab.isSelected(); }
    public ContextMenu setupOptionsMenu(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs) {
        ContextMenu cm = new ContextMenu();
        CheckMenuItem check = null;
        if(dvView.getClass().toString().contains("ViewTranscript") || dvView.getClass().toString().contains("ViewProtein")) {
            MenuItem mi = new MenuItem("Annotation Features Filter...");
            mi.setOnAction((event) -> { selectFeaturesDisplayed(dvView, dvPrefs); });
            cm.getItems().add(mi);
            check = new CheckMenuItem("Show ONLY Varying Features");
            check.setDisable((dvView.getClass().toString().contains("ViewTranscript")? proteinCnt : transCnt) < 2);
            check.setOnAction((event) -> { updateVaryingFeatures(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
            cm.getItems().add(check);
            cm.getItems().add(new SeparatorMenuItem());
        }
        if(dvView.getClass().toString().contains("ViewProtein")) {
            check = new CheckMenuItem("Show PROVEAN Score");
            check.setSelected(true);
            check.setOnAction((event) -> { updateProvean(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
            cm.getItems().add(check);
        }
        if(dvView.getClass().toString().contains("ViewTranscript") || dvView.getClass().toString().contains("ViewProtein")) {
            check = new CheckMenuItem("Show Aligned");
            check.setSelected(true);
            check.setOnAction((event) -> { updateAlignment(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
            cm.getItems().add(check);
        }
        if(dvView.getClass().toString().contains("ViewTranscript")) {
            CheckMenuItem talign = check;
            check = new CheckMenuItem("Show Splice Junctions (Aligned)");
            check.setSelected(true);
            check.setOnAction((event) -> { updateSpliceJuncs(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
            if(talign != null)
                check.disableProperty().bind(talign.selectedProperty().not());
            cm.getItems().add(check);
            check = new CheckMenuItem("Show Additional Information");
            check.setOnAction((event) -> { updateStructural(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
            cm.getItems().add(check);
        }
        else if(dvView.getClass().toString().contains("ViewGenomic")) {
            check = new CheckMenuItem("Show Additional Information");
            check.setSelected(false);
            check.setOnAction((event) -> { updateDetails(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
            cm.getItems().add(check);
        }
        check = new CheckMenuItem("Show Ruler");
        check.setSelected(true);
        check.setOnAction((event) -> { updateRuler(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
        cm.getItems().add(check);
        // display size
        cm.getItems().add(new SeparatorMenuItem());
        Menu menu = new Menu("Display Size");
        ToggleGroup tg = new ToggleGroup();
        RadioMenuItem item = new RadioMenuItem("Small");
        item.setToggleGroup(tg);
        item.setOnAction((event) -> { updateSize(dvView, dvPrefs, 1.0);  });
        menu.getItems().add(item);
        item = new RadioMenuItem("Medium");
        item.setToggleGroup(tg);
        item.setSelected(true);
        item.setOnAction((event) -> { updateSize(dvView, dvPrefs, 1.25);  });
        menu.getItems().add(item);
        item = new RadioMenuItem("Large");
        item.setToggleGroup(tg);
        item.setOnAction((event) -> { updateSize(dvView, dvPrefs, 1.75);  });
        menu.getItems().add(item);
        cm.getItems().add(menu);
        // sort
        cm.getItems().add(new SeparatorMenuItem());
        menu = new Menu("Sort by");
        tg = new ToggleGroup();
        item = new RadioMenuItem("ID");
        item.setSelected(true);
        item.setToggleGroup(tg);
        item.setOnAction((event) -> { updateSortBy(dvView, dvPrefs, ViewAnnotation.SortBy.ID);  });
        menu.getItems().add(item);
        item = new RadioMenuItem("Length");
        item.setToggleGroup(tg);
        item.setOnAction((event) -> { updateSortBy(dvView, dvPrefs, ViewAnnotation.SortBy.LENGTH);  });
        menu.getItems().add(item);
        item = new RadioMenuItem("Expression Level (mean)");
        item.setToggleGroup(tg);
        item.setOnAction((event) -> { updateSortBy(dvView, dvPrefs, ViewAnnotation.SortBy.EXPLEVEL);  });
        menu.getItems().add(item);
        item = new RadioMenuItem("PROVEAN Score");
        item.setToggleGroup(tg);
        item.setOnAction((event) -> { updateSortBy(dvView, dvPrefs, ViewAnnotation.SortBy.PROVEAN);  });
        menu.getItems().add(item);
        menu.getItems().add(new SeparatorMenuItem());
        check = new CheckMenuItem("Descending Order");
        check.setOnAction((event) -> { updateSortDescending(dvView, dvPrefs, ((CheckMenuItem)(event.getSource())).isSelected()); });
        menu.getItems().add(check);
        cm.getItems().add(menu);
        return cm;
    }
    public void updateWidth(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, double widthFactor) {
        dvPrefs.widthFactor = (widthFactor < MIN_WIDTHFACTOR)? MIN_WIDTHFACTOR : (widthFactor > MAX_WIDTHFACTOR)? MAX_WIDTHFACTOR : widthFactor;
        updateView(dvView, dvPrefs);
    }
    
    //
    // Internal Functions
    //
    private void updateView(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs) {
        dvView.update(gene, getGeneAnnotations(), dvPrefs);
    }
    private void updateSize(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, double size) {
        dvPrefs.scale = (size < 1.0)? 1.0 : (size > 2.0)? 2.0 : size;
        updateView(dvView, dvPrefs);
    }
    private void updateRuler(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean value) {
        dvPrefs.ruler = value;
        updateView(dvView, dvPrefs);
    }
    private void updateDetails(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean value) {
        dvPrefs.details = value;
        updateView(dvView, dvPrefs);
    }
    private void updateProvean(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean value) {
        dvPrefs.provean = value;
        updateView(dvView, dvPrefs);
    }
    private void updateAlignment(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean value) {
        dvPrefs.alignment = value;
        updateView(dvView, dvPrefs);
    }
    private void updateSpliceJuncs(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean value) {
        dvPrefs.spliceJuncs = value;
        updateView(dvView, dvPrefs);
    }
    private void updateStructural(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean value) {
        dvPrefs.structSubClass = value;
        updateView(dvView, dvPrefs);
    }
    private void updateSortBy(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, ViewAnnotation.SortBy sortBy) {
        dvPrefs.sortBy = sortBy;
        updateView(dvView, dvPrefs);
    }
    private void updateSortDescending(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean descending) {
        dvPrefs.sortDescending = descending;
        updateView(dvView, dvPrefs);
    }
    private void updateVaryingFeatures(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs, boolean varyingOnly) {
        dvPrefs.showVaryingOnly = varyingOnly;
        updateView(dvView, dvPrefs);
    }
    private void selectFeaturesDisplayed(ViewAnnotation dvView, ViewAnnotation.Prefs dvPrefs) {
        Window wnd = Tappas.getWindow();
        DlgGDVFilterFeatures dlg = new DlgGDVFilterFeatures(project, wnd);
        String viewType = dvView.getClass().toString().contains("ViewTranscript")? "Transcripts" : "Proteins";
        DlgGDVFilterFeatures.Params results = dlg.showAndWait(new DlgGDVFilterFeatures.Params(dvPrefs.hmNotFeatureIDs, false), viewType, dvView.hmFeatureIDs);
        if(results != null) {
            dvPrefs.hmNotFeatureIDs = results.hmNotFeatureIDs;
        }
        updateView(dvView, dvPrefs);
    }
    private void getGeneAnnotationsData() {
        if(geneAnnotations == null)
            geneAnnotations = project.data.getData(gene, hmTrans);
    }
    protected ArrayList<String> getGOTerms(String rsid, ArrayList<String[]> lines) {
        ArrayList<String> lstTerms = new ArrayList<>();
        HashMap<String, Object> hmTerms = new HashMap<>();
        try {
            boolean process = false;
            HashMap<String, String> attrs;
            AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
            for(String[] fields : lines) {
                if(rsid.isEmpty() || fields[0].equals(rsid)) {
                    String source = fields[1];
                    String type = fields[2];
                    if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.PROTEIN))
                        process = true;
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.TRANSCRIPT) || project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.GENOMIC))
                        process = false;
                    if(process) {
                        if(source.equals(defs.srcGO)) {
                            attrs = DataAnnotation.getAttributes(fields[8]);
                            if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId))
                                hmTerms.put(attrs.get(project.data.getAnnotationFileDefs().attrId), null);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            app.logError("Unable to process annotation data for protein " + rsid + ": " + ex.getMessage());
            hmTerms.clear();
        }
        for(String term : hmTerms.keySet())
            lstTerms.add(term);
        return lstTerms;
    }
}
