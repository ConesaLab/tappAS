/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

// todo: review each IS view.trans or not !!!

import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import tappas.DataAnnotation.AnnotFeature;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.*;


/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class ViewAnnotation extends AppObject {
    static public enum SortBy {
        ID, LENGTH, EXPLEVEL, PROVEAN
    }
    public enum Views { TRANS, PROTEIN, GENOMIC };
    
    // application objects
    protected SubTabBase subTabBase;
    protected TabBase tabBase;
    
    // class data
    protected Canvas canvas;
    protected ScrollPane canvasPane;
    protected Font titleFont;
    protected Color titleColor;
    protected Font defaultFont;
    protected Font verySmallFont;
    protected Font smallFont;
    protected Color defaultColor;
    protected Font subFont;
    protected Color subColor;
    protected Font copyrightFont;
    protected Color copyrightColor;
    protected Font legendFont;
    protected Color legendColor;
    protected AnnotationEntry geneEntry = null;
    protected final ArrayList<AnnotationEntry> annotationList = new ArrayList<>();
    protected final ArrayList<ProveanData> proveanList = new ArrayList<>();
    protected final ArrayList<ToolTipPoints> tipsList = new ArrayList<>();
    protected final Tooltip tip = new Tooltip("");
    protected AnnotationEntry tipEntry = null;
    protected final double levelOffset = 7;
    protected double zoomPPB = 0;
    protected double scale = 1;
    protected double widthFactor = 1;
    protected int maxExpLevel = 0;
    protected AnnotationSymbols symbols;
    protected final int maxCaptionLength = 45;  // max number of chars to display for legend, etc.
    protected final int maxTitleLength = 80;    // max number of chars to display for title
    protected String filterCaption = "";
    protected String lastGene = "";
    protected int rows = 0;
    protected boolean displayed = false;
    protected boolean lastAlignment = false;
    protected boolean limitExceeded = false;
    protected SortBy lastSortBy = SortBy.ID;
    protected boolean lastSortDescending = false;
    protected SeqAlign seqAlign = new SeqAlign();
    protected HashMap<String, DataProject.TransExpLevels> geneIsoformsExpression = new HashMap<>();
    protected HashMap<String, Boolean> geneIsoformsDEResults = new HashMap<>();
    protected final List<String> annotationLines = new ArrayList<>();
    protected final List<String> transcriptDisplayOrder = new ArrayList<>();
    protected HashMap<String, HashMap<String, HashMap<String, Object>>> hmFeatureIDs = new HashMap<>();
    protected HashMap<String, HashMap<String, HashMap<String, Object>>> hmGeneDiversity = null;
    protected HashMap<String, HashMap<String, HashMap<String, Object>>> hmProteinGeneDiversity = null;
    protected HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPos = null;
        
    public ViewAnnotation(Project project) {
        super(project, null);
    }
    public boolean initialize(SubTabBase subTabBase, Canvas canvas, ScrollPane canvasPane, String filterCaption) {
        /*defaultFont = Font.font("Arial", 10);
        smallFont = Font.font("Arial", 9);
        verySmallFont = Font.font("Arial", 7);
        defaultColor = Color.BLACK;
        titleFont = Font.font("Arial", 16);
        titleColor = Color.STEELBLUE;
        subFont = Font.font("Arial", 13);
        subColor = Color.STEELBLUE;
        copyrightFont = Font.font("Arial", 10);
        copyrightColor = Color.DARKGREY;
        legendFont = Font.font("Arial", 10);
        legendColor = Color.BLACK;*/

	defaultFont = Font.font("Arial", 12);
        smallFont = Font.font("Arial", 11);
        verySmallFont = Font.font("Arial", 9);
        defaultColor = Color.BLACK;
        titleFont = Font.font("Arial", 18);
        titleColor = Color.STEELBLUE;
        subFont = Font.font("Arial", 15);
        subColor = Color.STEELBLUE;
        copyrightFont = Font.font("Arial", 12);
        copyrightColor = Color.DARKGREY;
        legendFont = Font.font("Arial", 12);
        legendColor = Color.BLACK;
        
        this.subTabBase = subTabBase;
        this.tabBase = subTabBase.tabBase;
        this.canvas = canvas;
        this.canvasPane = canvasPane;
        this.filterCaption = filterCaption;
        symbols = new AnnotationSymbols(project, null, this);
        symbols.initialize(project.data.getAFStats());
        return true;
    }
    protected void addFeatureID(AnnotationEntry entry) {
        if(!hmFeatureIDs.containsKey(entry.source)) {
            HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
            HashMap<String, Object> hmIDs = new HashMap<>();
            hmIDs.put(entry.featureId, null);
            hmFeatures.put(entry.feature, hmIDs);
            hmFeatureIDs.put(entry.source, hmFeatures);
        }
        else {
            HashMap<String, HashMap<String, Object>> hmFeatures = hmFeatureIDs.get(entry.source);
            if(!hmFeatures.containsKey(entry.feature)) {
                HashMap<String, Object> hmIDs = new HashMap<>();
                hmIDs.put(entry.featureId, null);
                hmFeatures.put(entry.feature, hmIDs);
            }
            else {
                HashMap<String, Object> hmIDs = hmFeatures.get(entry.feature);
                if(!hmIDs.containsKey(entry.featureId))
                    hmIDs.put(entry.featureId, null);
            }
        }
    }
    protected boolean isDisplayableFeatureId(AnnotationEntry entry, Prefs prefs) {
        boolean result = false;
        
        // check if feature not in the exclusion list
        if(!prefs.isNotFeatureID(entry.source, entry.feature, entry.featureId)) {
            // check if we are only including varying feature
            if(prefs.showVaryingOnly) {
                HashMap<String, Object> hmTrans = project.data.getGeneTrans(lastGene);
                if(hmTransAFIdPos == null) {
                    ArrayList<String> lstTrans = new ArrayList();
                    for(String id : hmTrans.keySet())
                        lstTrans.add(id);
                    hmTransAFIdPos = project.data.loadTransAFIdPos(lstTrans);
                }                
                HashMap<String, HashMap<String, HashMap<String, Object>>> hmGD;
                HashMap<String, HashMap<String, Object>> hmGeneTrans = new HashMap();
                // we want to get all the features just once - user can change features filter anytime
                HashMap<String, HashMap<String, Object>> hmFeatures = new HashMap<>();
                for(String source : hmFeatureIDs.keySet()) {
                    HashMap<String, Object> hm = new HashMap<>();
                    HashMap<String, HashMap<String, Object>> hmTypes = hmFeatureIDs.get(source);
                    for(String type : hmTypes.keySet())
                        hm.put(type, null);
                    hmFeatures.put(source, hm);
                }

                Views view = getViewType(this.getClass().toString());
                if(view.equals(Views.PROTEIN)) {
                    // see if we don't have the varying information
                    if(hmProteinGeneDiversity == null) {
                        HashMap<String, Object> hmProteins = new HashMap<>();
                        for(String trans : hmTrans.keySet()) {
                            if(project.data.isTransCoding(trans))
                                hmProteins.put(trans, null);
                        }
                        hmGeneTrans.put(lastGene, hmProteins);
                        hmProteinGeneDiversity = project.data.getDiverseFeaturesUsingGenomicPositionExons(hmGeneTrans, hmFeatures, true, hmTransAFIdPos);
                    }
                    hmGD = hmProteinGeneDiversity;
                }
                else {
                    // see if we don't have the varying information
                    if(hmGeneDiversity == null) {
                        hmGeneTrans.put(lastGene, hmTrans);
                        hmGeneDiversity = project.data.getDiverseFeaturesUsingGenomicPositionExons(hmGeneTrans, hmFeatures, true, hmTransAFIdPos);
                    }
                    hmGD = hmGeneDiversity;
                }

                if(hmGD != null) {
                    String fields[];
                    for(String info : hmGD.keySet()) {
                        fields = info.split("\t");
                        if(fields.length == 4) {
                            if(fields[0].equals(entry.source)) {
                                if(fields[1].equals(entry.feature)) {
                                    if(fields[2].equals(entry.featureId)) {
                                        HashMap<String, HashMap<String, Object>> hmGenes = hmGD.get(info);
                                        if(hmGenes.containsKey(geneEntry.id)) {
                                            HashMap<String, Object> hmGDTrans = hmGenes.get(geneEntry.id);
                                            if(hmGDTrans.containsKey(entry.id)) { // && (!protein || project.data.isTransCoding(entry.id))) {
                                                DataAnnotation.IdPosition idpos = (DataAnnotation.IdPosition) hmGDTrans.get(entry.id);
                                                if(idpos.pos.start == entry.strpos && idpos.pos.end == entry.endpos) {
                                                    result = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else
                result = true;
        }
        return result;
    }
    public void handleMouseClickAction(MouseEvent event) {
        // override
    }
    public WritableImage getImage() {
        WritableImage wi;
        SnapshotParameters sp = new SnapshotParameters();
        wi = canvas.snapshot(sp, null);
        PixelWriter pw = wi.getPixelWriter();
        return wi;
    }
    public double getZoomPPB() { return zoomPPB; }
    public boolean hasBeenDisplayed() { return displayed; }
    
    //
    // Mouse event handlers
    //
    public void onMouseMoved(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        boolean setHandCursor = false;
        ArrayList<ToolTipPoints> fndtips = new ArrayList<>();
        Scene scene = canvas.getScene();
        Point2D.Double p = new Point2D.Double(x/scale, y/scale);
        for(ToolTipPoints tips : tipsList) {
            if(tips.rect.contains(p)) {
                if(tips.setHandCursor)
                    setHandCursor = true;
                fndtips.add(tips);
            }
        }
        scene.setCursor(setHandCursor? Cursor.HAND : Cursor.DEFAULT);
        if(fndtips.isEmpty()) {
            tip.hide();
            tipEntry = null;
        }
        else {
            String tooltip = "";
            for(ToolTipPoints tip : fndtips) {
                tipEntry = tip.entry;
                if(!tip.tooltip.isEmpty()) {
                    if(!tooltip.isEmpty())
                        tooltip += "\n\n";
                    if(tipEntry != null && lastAlignment)
                        tooltip +=  tip.tooltip + tip.entry.tooltipAligned;
                    else
                        tooltip += tip.tooltip;
                }
            }
            if(!tooltip.isEmpty()) {
                tip.setText(tooltip);
                tip.setX(event.getScreenX());
                tip.setY(event.getScreenY() + 10);
                tip.show(canvas.getScene().getWindow());
            }
            else
                tip.hide();
        }
    }
    public void onMouseExited(MouseEvent event) {
        tip.hide();
        tipEntry = null;
        //Scene scene = canvas.getScene();
        //scene.setCursor(Cursor.DEFAULT);
    }
    protected void onGeneChanged(String gene) {
        // Override
    }
    
    public void update(String gene, ArrayList<String[]> data, Prefs prefs) {
        boolean update = false;
        if(!lastGene.equals(gene)) {
            limitExceeded = false;
            update = true;
        }
        if(update) {
            limitExceeded = false;
            if(project.data.analysis != null) {
                double max = 0;
                Views view = getViewType(this.getClass().toString());
                if(view.equals(Views.PROTEIN)) {
                    maxExpLevel = 0;
                    geneIsoformsDEResults = project.data.analysis.getGeneProteinsDEFlags(gene);
                    geneIsoformsExpression = project.data.getGeneProtsExpLevels(gene, null);
                    for(DataProject.TransExpLevels el : geneIsoformsExpression.values()) {
                        if(el.X1_mean > max)
                            max = el.X1_mean;
                        if(el.X2_mean > max)
                            max = el.X2_mean;
                    }
                    if(max > 0)
                        maxExpLevel = (int) Math.round(max + 1);
                }
                else {
                    maxExpLevel = 0;
                    geneIsoformsDEResults = project.data.analysis.getGeneIsoformsDEFlags(gene);
                    geneIsoformsExpression = project.data.getGeneIsosExpLevels(gene, null);
                    for(DataProject.TransExpLevels el : geneIsoformsExpression.values()) {
                        if(el.X1_mean > max)
                            max = el.X1_mean;
                        if(el.X2_mean > max)
                            max = el.X2_mean;
                    }
                    if(max > 0)
                        maxExpLevel = (int) Math.round(max + 1);
                }
            }
            onGeneChanged(gene);
        }
        if(!lastGene.equals(gene) || lastAlignment != prefs.alignment || lastSortBy != prefs.sortBy || lastSortDescending != prefs.sortDescending)
            getAnnotationData(gene, data, prefs);
        
        long tstart = System.nanoTime();
        System.out.println("Updating viewer. Canvas scrollPane w,h: " + canvasPane.getWidth() + ", " + canvasPane.getHeight());
        this.scale = prefs.scale;
        this.widthFactor = prefs.widthFactor;
        double cw = canvasPane.getWidth() - 2;
        double ch = canvasPane.getHeight() - 2;
        // don't bother with initial calls where size hasn't been set yet
        if(cw > 100 && ch > 100) {
            double sbHeight = 0.0;
            double sbWidth = 0.0;
            Set<Node> nodes = canvasPane.lookupAll(".scroll-bar");
            for (final Node node : nodes) {
                if (node instanceof ScrollBar) {
                    ScrollBar sb = (ScrollBar) node;
                    if (sb.getOrientation() == Orientation.HORIZONTAL) // && sb.isVisible())
                            sbHeight = sb.getHeight();
                    else if (sb.getOrientation() == Orientation.VERTICAL) // && sb.isVisible())
                            sbWidth = sb.getWidth();
                }
            }
            if(sbHeight > 0)
                ch -= sbHeight;
            if(sbWidth > 0)
                cw -= sbWidth;
            canvas.setWidth(cw / scale);
            canvas.setHeight(ch);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.save();
            gc.scale(scale, scale);
            displayed = true;
            DrawArgs args = new DrawArgs(prefs, 1, 50, false);
            draw(gc, args);
            gc.restore();
        }
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Drawing time: " + duration + " ms");
    }
    protected void draw(GraphicsContext gc, DrawArgs args) {
        // override
    }
    protected void drawTrackSymbol(AnnotationEntry entry, AnnotationEntry geneEntry, AnnotationEntry seqEntry, 
                                 ArrayList<SeqAlign.Position> tgexonsList,
                                 ArrayList<AnnotationEntry> cdsList, ArrayList<ArrayList<UsedPoints>> seqList,
                                 GraphicsContext gc, double x, double y, double ppb, 
                                 boolean negStrand, boolean genomic, boolean alignment, SeqAlign.WithinRegion within) {
        if(entry.symbol == null || entry.symbol.symbolDef == null)
            return;

        // assign the arrows to the bottom by default
        AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
        AnnotationSymbols.SymbolDef symdef = entry.symbol.symbolDef;
        boolean bottom = (symdef.shape == AnnotationSymbols.Shape.ARROW);
        boolean bottomOnly = false;
        boolean blockFeature = AnnotationSymbols.isBlockSymbol(symdef);
        double w;
        double h = (symdef.shape != AnnotationSymbols.Shape.DROP && symdef.shape != AnnotationSymbols.Shape.NONE)? symdef.height : symdef.value;
        boolean extended = false;
        if(!defs.isDomain(seqEntry.source, seqEntry.feature) && !blockFeature) {
            double sp = entry.dspstrpos * ppb;
            double dp = entry.dsppos * ppb;
            double sw = dp - AnnotationSymbols.MaxSymbolWidth/2 - 2;
            if(sw > sp) {
                double rw = (entry.endpos - entry.strpos + 1); // * ppb;
                double rwd = rw * ppb;
                double rx = x + (entry.dspstrpos - geneEntry.strpos + 1); // * ppb;
                double rxd = x + (entry.dspstrpos - geneEntry.strpos + 1) * ppb;
                int level = -1;
                for(AnnotationEntry cds : cdsList) {
                    // do not allow adding on top if CDS section, can crowd splice junctions
                    if(entry.dsppos > (cds.strpos - rw/2) && entry.dsppos < (cds.endpos + rw/2)) {
                        bottom = true;
                        bottomOnly = true;
                        // will also not try to add to seqList(1) since it can cause display issues (think Tetris)
                        while(seqList.size() < 3)
                            seqList.add(new ArrayList<>());
                        break;
                    }
                }
                if(!bottomOnly) {
                    // check to see if there is room, top or botom
                    if(bottom) {
                        if(IsThereRoom(seqList.get(1), symdef, rx, rw, rxd, rwd))
                            level = 1; //!bottomOnly && 
                        else if(IsThereRoom(seqList.get(0), symdef, rx, rw, rxd, rwd)) {
                            level = 0;
                            bottom = false;
                        }
                    }
                    else {
                        if(IsThereRoom(seqList.get(0), symdef, rx, rw, rxd, rwd))
                            level = 0;
                        else if(IsThereRoom(seqList.get(1), symdef, rx, rw, rxd, rwd)) {
                            level = 1;
                            bottom = true;
                        }
                    }
                }
                if(level == -1) {
                    // only add new rows to bottom
                    int cnt = seqList.size();
                    for(int i = 2; i < cnt; i++) {
                        if(IsThereRoom(seqList.get(i), symdef, rx, rw, rxd, rwd)) {
                            level = i;
                            break;
                        }
                    }
                    if(level == -1) {
                        level = seqList.size();
                        // do not allow intron symbols to get out of control - may want to refine later (add + symbol)!!!
//figure out why this is -> return !!! did I decide to only allow 5 rows? i don't think so, look into SeqAlign.WithinRegion.YES
                        if(!within.equals(SeqAlign.WithinRegion.YES)) {
                            if(level > 5)
                                return;
                        }
                        seqList.add(new ArrayList<>());
                        //System.out.println("Added new level, " + level + ", for " + "x: " + entry.strpos);
                    }
                    bottom = true;
                    y += (level - 1) * levelOffset;
                }
                seqList.get(level).add(new UsedPoints(rx, rw, rxd, rwd));
                //dbg: System.out.println("Adding to seqList: " + rxd + ", " + rwd + ", entry: " + entry.strpos + ", " + entry.endpos);
                y += bottom? 2 : -(h + 2);
                //System.out.println("sym cdslist: " + cdsList.size() + ", y: " + y);

                // call proper drawing function based on alignment option value
                if(alignment) {
                    // get min/max to set tool tip
                    AnnotationSymbols.SymbolExtensionInfo sei = symbols.drawSymbolExtension(gc, x, y, Math.max(symdef.height, symdef.value), ppb, 
                            AnnotationSymbols.getFillColor(symdef.colorsIdx), tgexonsList, seqAlign, entry, geneEntry);
                    boolean reverse = (negStrand && genomic);
                    String pos = String.format("%s%s", NumberFormat.getInstance().format(reverse? entry.endpos : entry.strpos),
                                    (entry.strpos == entry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(reverse? entry.strpos : entry.endpos)));
                    tipsList.add(new ToolTipPoints(entry, sei.minX, y, (sei.maxX - sei.minX + 1), Math.max(symdef.height, symdef.value),
                                        String.format("%s\nPosition: %s", entry.tooltip, pos), true));
                    // if symbol extension spanned across introns, need to reserve full area
                    if(sei.segments > 1) {
                        System.out.println("Symbol extension spanned across intron, updating seqList with " + sei.minX + ", " + (sei.maxX - sei.minX + 1) + ", entry: " + entry.strpos + ", " + entry.endpos);
                        seqList.get(level).add(new UsedPoints(sei.minX, (sei.maxX - sei.minX + 1), sei.minX, (sei.maxX - sei.minX + 1)));
                    }
                }
                else {
                    symbols.drawSymbolExtension(gc, rxd, y, rwd, Math.max(symdef.height, symdef.value), AnnotationSymbols.getFillColor(symdef.colorsIdx));
                    boolean reverse = (negStrand && genomic);
                    String pos = String.format("%s%s", NumberFormat.getInstance().format(reverse? entry.endpos : entry.strpos),
                                    (entry.strpos == entry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(reverse? entry.strpos : entry.endpos)));
                    tipsList.add(new ToolTipPoints(entry, rxd, y, rwd, Math.max(symdef.height, symdef.value),
                                        String.format("%s\nPosition: %s", entry.tooltip, pos), true));
                }
                extended = true;
            }
        }
        else {
            // set the block width if block feature
            if(blockFeature)
                entry.symbol.wBlock = (entry.dspendpos - entry.dspstrpos + 1);
        }
        
        // check if doing a block feature, set x and w accordingly
        double xt, xd, wd;
        if(blockFeature) {
            w = entry.symbol.wBlock;
            wd = entry.symbol.wBlock * ppb;
            entry.symbol.wBlock = wd;
            xd = x;
            if(!alignment)
                xd += ((entry.dspstrpos - geneEntry.strpos + 1) * ppb);
            xt = x;
            x += (entry.dspstrpos - geneEntry.strpos + 1); // * ppb;
            xt += ((entry.dspstrpos - geneEntry.strpos + 1) * ppb);
        }
        else {
            w = symdef.width;
            wd = symdef.width;
            xd = x;
            x += (entry.dsppos - geneEntry.strpos + 1); // * ppb;
            x -= w/2;
            xd += (entry.dsppos - geneEntry.strpos + 1) * ppb;
            xd -= w/2;
            xt = xd;
        }
        int level = -1;
        if(!extended) {
            for(AnnotationEntry cds : cdsList) {
                // do not allow adding on top if CDS section, can crowd splice junctions
                if(entry.dsppos > (cds.strpos - w/2) && entry.dsppos < (cds.endpos + w/2)) {
                    bottom = true;
                    bottomOnly = true;
                    // will also not try to add to seqList(1) since it can cause display issues (think Tetris)
                    while(seqList.size() < 3)
                        seqList.add(new ArrayList<>());
                    break;
                }
            }
            if(!bottomOnly) {
                // check to see if there is room, top or bottom
                if(bottom) {
                    if(IsThereRoom(seqList.get(1), symdef, x, w, xt, wd))
                        level = 1; //!bottomOnly && 
                    else if(IsThereRoom(seqList.get(0), symdef, x, w, xt, wd)) {
                        level = 0;
                        bottom = false;
                    }
                }
                else {
                    if(IsThereRoom(seqList.get(0), symdef, x, w, xt, wd))
                        level = 0;
                    else if(IsThereRoom(seqList.get(1), symdef, x, w, xt, wd)) {
                        level = 1;
                        bottom = true;
                    }
                }
            }
            if(level == -1) {
                // only add new rows to bottom
                int cnt = seqList.size();
                for(int i = 2; i < cnt; i++) {
                    if(IsThereRoom(seqList.get(i), symdef, x, w, xt, wd)) {
                        level = i;
                        break;
                    }
                }
                if(level == -1) {
                    level = seqList.size();
                    // do not allow intron symbols to get out of control - may want to refine later (add + symbol)!!!
//figure out why this is -> return !!! did I decide to only allow 5 rows? i don't think so, look into SeqAlign.WithinRegion.YES
                    if(!within.equals(SeqAlign.WithinRegion.YES)) {
                        if(level > 5)
                            return;
                    }
                    seqList.add(new ArrayList<>());
                    //System.out.println("Added new level, " + level + ", for " + "x: " + entry.strpos);
                }
                bottom = true;
                y += (level - 1) * levelOffset;
            }
            seqList.get(level).add(new UsedPoints(x, w, xt, wd));
            //dbg: System.out.println("Adding to seqList: " + xt + ", " + wd + ", entry: " + entry.strpos + ", " + entry.endpos);
            y += bottom? 2 : -(h + 2);
            //System.out.println("sym cdslist: " + cdsList.size() + ", y: " + y);
            boolean reverse = (negStrand && genomic);
            String pos = String.format("%s%s", NumberFormat.getInstance().format(reverse? entry.endpos : entry.strpos),
                            (entry.strpos == entry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(reverse? entry.strpos : entry.endpos)));
            tipsList.add(new ToolTipPoints(entry, xt, y, wd, Math.max(symdef.height, symdef.value),
                                String.format("%s\nPosition: %s", entry.tooltip, pos), true));
                                //(entry.tooltip.isEmpty()? "" : String.format("\n\n%s", entry.tooltip)))));
        }
        
        // draw actual feature symbol
        // Note that when drawing block symbols xd = the original x passed which is just the offset to start if alignment is on
        // this is done since block symbols need to handle being split into chunks to avoid introns (when alignment is on)
        // the offset to y below is just for looks - centering the block in its lane since it has a smaller than normal height
        if(blockFeature && level != -1) {
            if(level == 0)
                y -= 1;
            else
                y += 1;
            AnnotationSymbols.SymbolExtensionInfo sei = symbols.draw(entry.symbol, gc, xd, y, ppb, bottom, within, tgexonsList, (alignment? seqAlign : null), entry, geneEntry);
            if(alignment && sei != null && sei.segments > 1) {
                // if block spanned across introns, need to reserve full area
                System.out.println("Block feature spanned across intron, updating seqList with " + sei.minX + ", " + (sei.maxX - sei.minX + 1) + ", entry: " + entry.strpos + ", " + entry.endpos);
                seqList.get(level).add(new UsedPoints(sei.minX, (sei.maxX - sei.minX + 1), sei.minX, (sei.maxX - sei.minX + 1)));
            }
        }
        else
            symbols.draw(entry.symbol, gc, xd, y, ppb, bottom, within, tgexonsList, (alignment? seqAlign : null), entry, geneEntry);
    }
    // Note: strpos and endpos denot the lowest and highest positions of the genomic display
    protected double drawGenomicTracks(HashMap<String, ArrayList<AnnotationEntry>> hmAnnots, 
                                 GraphicsContext gc, double x, double y, double ppb, double strpos, double endpos, boolean negStrand) {
        double newy = y;
        for(String key : hmAnnots.keySet()) {
            ArrayList<AnnotationEntry> lst = hmAnnots.get(key);
            String[] fields = key.split("\t");
            gc.setFont(verySmallFont);
            gc.setFill(Color.BLACK);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(fields[0], x - 5, newy);
            gc.fillText(fields[1], x - 5, newy+10);
            double dspx;
            double dspy;
            double refpos = negStrand? endpos : strpos;
            for(AnnotationEntry ae : lst) {
                dspx = adjustX(x + Math.abs((negStrand? ae.endpos : ae.strpos) - refpos) * ppb);
                dspy = adjustY(newy - (AnnotationSymbols.CDSblockHeight/2));
                double width = Math.max((Math.abs(ae.endpos - ae.strpos) + 1), 2) * ppb;
                symbols.drawGenomicSymbol(gc, dspx, dspy, width);
                AnnotationEntry ttae = ae;
                if(negStrand)
                    ttae = new AnnotationEntry(ae.id, ae.source, ae.feature, ae.featureId, ae.caption, ae.strand, ae.endpos, ae.strpos, ae.attrs, ae.tooltip, ae.urlValue);
                addToolTipToList(ttae, dspx - 1, dspy, Math.abs(ae.endpos - ae.strpos) * ppb + 2, AnnotationSymbols.CDSblockHeight, false);
            }
            newy += AnnotationSymbols.CDSblockHeight + 10;
        }
        return newy;
    }
    public Views getViewType(String className) {
        Views view = Views.TRANS;
        if(className.contains("ViewProtein"))
            view = Views.PROTEIN;
        if(className.contains("ViewGenomic"))
            view = Views.GENOMIC;
        return view;
    }

    protected void getAnnotationData(String gene, ArrayList<String[]> data, Prefs prefs) {
        long tstart = System.nanoTime();
        lastGene = gene;
        lastAlignment = prefs.alignment;
        lastSortBy = prefs.sortBy;
        lastSortDescending = prefs.sortDescending;
        seqAlign.clearData();
        annotationList.clear();
        proveanList.clear();
        transcriptDisplayOrder.clear();
        Views view = getViewType(this.getClass().toString());
        //System.out.println("getAnnotationData for " + view.name());

        // build list of gene transcripts and provean data
        List<String> transcripts = new ArrayList<>();
        AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
        for(String[] fields : data) {
            String source = fields[1];
            String type = fields[2];
            if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.TRANSCRIPT))
                transcripts.add(fields[0]);
            else if(fields[1].equals(defs.pScore.source) && fields[2].equals(defs.pScore.feature))
                proveanList.add(new ProveanData(fields[0], fields[8]));
        }
        if(prefs.sortBy == SortBy.EXPLEVEL) {
            List<TranscriptSortEntry> lst = new ArrayList<>();
            for(String trans : transcripts) {
                double level = 0.0;
                if(view.equals(Views.PROTEIN)) {
                    String prot = project.data.getTransProtein(trans);
                    if(geneIsoformsExpression.containsKey(prot)) {
                        DataProject.TransExpLevels el = geneIsoformsExpression.get(prot);
                        level = (el.X1_mean + el.X2_mean) / 2;
                    }
                }
                else {
                    if(geneIsoformsExpression.containsKey(trans)) {
                        DataProject.TransExpLevels el = geneIsoformsExpression.get(trans);
                        level = (el.X1_mean + el.X2_mean) / 2;
                    }
                }
                lst.add(new TranscriptSortEntry(level, trans));
            }
            Collections.sort(lst);
            Collections.reverse(lst);
            if(prefs.sortDescending)
                Collections.reverse(lst);
            transcripts.clear();
            for(TranscriptSortEntry tse : lst)
                transcripts.add(tse.id);
        }
        else if(prefs.sortBy == SortBy.LENGTH) {
            List<TranscriptSortEntry> lst = new ArrayList<>();
            for(String trans : transcripts) {
                if(view.equals(Views.TRANS))
                    lst.add(new TranscriptSortEntry(project.data.getTransLength(trans), trans));
                else if(view.equals(Views.GENOMIC))
                    lst.add(new TranscriptSortEntry(project.data.getTransGenomicLength(trans), trans));
                else
                    lst.add(new TranscriptSortEntry(project.data.getTransProteinLength(trans), trans));
            }
            Collections.sort(lst);
            if(prefs.sortDescending)
                Collections.reverse(lst);
            transcripts.clear();
            for(TranscriptSortEntry tse : lst)
                transcripts.add(tse.id);
        }
        else if(prefs.sortBy == SortBy.PROVEAN) {
            Collections.sort(proveanList);
            if(prefs.sortDescending)
                Collections.reverse(proveanList);

            // it is possible that all transcripts are not in provean list
            List<String> tmp = transcripts;
            transcripts = new ArrayList<>();
            for(ProveanData pd : proveanList)
                transcripts.add(pd.trans);
            for(String trans : tmp) {
                if(!transcripts.contains(trans))
                    transcripts.add(trans);
            }
        }
        else {
            Collections.sort(transcripts, String.CASE_INSENSITIVE_ORDER);
            if(prefs.sortDescending)
                Collections.reverse(transcripts);
        }

        // get annotations for each transcript in the list
        // get gene description and largest size of all transcript
        // sort transcript annotations based on type and size - chosen convention for display
        String geneName = gene;
        String geneAttrs = "";
        int largest = 0;
        ArrayList<ViewAnnotationEntry> gtlst = new ArrayList<>();
        //System.out.println("Transcripts:");
        for(String rsid : transcripts) {
            ArrayList<AnnotationEntry> alst = new ArrayList<>();
            getRSAnnotationData(rsid, data, alst);
            for(AnnotationEntry a : alst) {
                if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(view.equals(Views.PROTEIN)? AnnotFeature.PROTEIN : AnnotFeature.TRANSCRIPT)) {
                    ViewAnnotationEntry gta = new ViewAnnotationEntry(a.id, Math.abs(a.endpos - a.strpos) + 1, alst);
                    gtlst.add(gta);
                    if(gta.size > largest)
                        largest = gta.size;
                }
                else if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE)) {
                    if(geneAttrs.isEmpty()) {
                        HashMap<String, String> attrs = DataAnnotation.getAttributes(a.attrs);
                        if(attrs.containsKey(project.data.getAnnotationFileDefs().attrName))
                            geneName = attrs.get(project.data.getAnnotationFileDefs().attrName);
                        geneAttrs = a.attrs;
                    }
                    break;
                }
            }
        }
        //System.out.println("Largest size: " + largest);

        // process the individual transcript annotations
        boolean chkStrand = false;
        boolean negStrand = false;
        String strand;
        boolean warnlen = true;
        ArrayList<SeqAlign.Position> ntexonsList = new ArrayList<>();
        ArrayList<SeqAlign.Position> ntcdsList = new ArrayList<>();
        for(ViewAnnotationEntry gta : gtlst) {
            ArrayList<AnnotationEntry> al = new ArrayList<>();

            // save order
            transcriptDisplayOrder.add(gta.id);

            // check if using alignment
            int addit = 1;
            boolean warnStrand = true;
            AnnotationEntry cds = null;
            AnnotationEntry trans = null;
            AnnotationEntry ntexon = null;
            
            // right now only the exon entries have the strand set
            // we need it in the transcript entry for display purposes
            strand = "";
            for(AnnotationEntry a : gta.alst) {
                if(!a.strand.isEmpty() && !a.strand.equals(".")) {
                    strand = a.strand;
                    break;
                }
            }
            negStrand = strand.equals("-");
            if(prefs.alignment) {
                addit = 0;
                for(AnnotationEntry a : gta.alst) {
                    if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.TRANSCRIPT)) {
                        trans = a;
                        cds = null;
                        ntexon = null;
                        ntexonsList = new ArrayList<>();
                    }
                    if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.CDS))
                        cds = a;
                    
                    // only add transcript if we have alignment data
                    if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON)) {
                        // build alignment exons data for gap calculations
                        addit |= 1;
                        if(ntexon == null)
                            ntexon = a;
                        boolean ns = a.strand.equals("-");
                        if(chkStrand) {
                            if(negStrand != ns) {
                                //System.out.println("addit: " + addit);
                                if(warnStrand) {
                                    warnStrand = false;
                                    logger.logWarning("Gene, " + gene + ", contains transcripts on both strands. Only showing alignment for " + (negStrand? "negative" : "positive") + " strand.");
                                }
                                addit = 0;
                            }
                        }
                        else
                            chkStrand = true;
                    }
                }
            }
            if(addit == 1) {
                // add all annotations, except exons, for this transcript to annotation list
                for(AnnotationEntry a : gta.alst) {
                    if(!project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON))
                        al.add(a);
                }

                // add all annotations for this transcript to annotation list
                ArrayList<AnnotationEntry> tmpList = new ArrayList<>();
                for(AnnotationEntry a : gta.alst) {
                    // exons must be in order: asc for + strand and desc for - strand
                    if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON)) {
                        int idx = 0;
                        boolean append = true;
                        for(AnnotationEntry ta : tmpList) {
                            if(negStrand) {
                                if(a.strpos > ta.strpos) {
                                    tmpList.add(idx, a);
                                    append = false;
                                    break;
                                }
                            }
                            else {
                                if(a.strpos < ta.strpos) {
                                    tmpList.add(idx, a);
                                    append = false;
                                    break;
                                }
                            }
                            idx++;
                        }
                        if(append)
                            tmpList.add(a);
                    }
                }
                int sortidx = 0;
                AnnotationEntry ea;
                AnnotationEntry lastExon = null;
                AnnotationEntry firstExon = null;
                for(AnnotationEntry a : gta.alst) {
                    // exons must be in order - asc for + strand and desc for - strand
                    if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON)) {
                        ea = tmpList.get(sortidx++);
                        ntexonsList.add(new SeqAlign.Position(ea.strpos, ea.endpos));
                        seqAlign.updGeneExonsList(ea.strpos, ea.endpos);
                        al.add(ea);
                        //System.out.println("Adding sorted exon: " + ea.strpos + " - " + ea.endpos);
                        if(lastExon == null || ea.endpos > lastExon.endpos)
                            lastExon = ea;
                        if(firstExon == null || ea.strpos < lastExon.strpos)
                            firstExon = ea;
                    }
                }
                //System.out.println("SeqAlign exons: " + seqAlign.getGeneExonsString());

                // check if alignment and CDS
                if(prefs.alignment && cds != null && trans != null) {
                    // generate internal CDS values
                    Collections.sort(ntexonsList);
                    //System.out.println("ntexonslist:");
                    int translen = trans.endpos - trans.strpos + 1;
                    int exonlen = 0;
                    for(SeqAlign.Position pos : ntexonsList) {
                        exonlen += Math.abs(pos.end - pos.start) + 1;
                        //System.out.println(pos.start + ".." + pos.end);
                    }
                    int strpos;
                    int diff = Math.abs(translen - exonlen);
                    if(translen > exonlen) {
                        // make msg strand sensitive
                        if(negStrand) {
                            firstExon.strpos -= diff;
                            firstExon.ostrpos = firstExon.strpos;
                            ntexonsList.get(0).start -= diff;
                        }
                        else {
                            lastExon.endpos += diff;
                            lastExon.oendpos = lastExon.endpos;
                            ntexonsList.get(ntexonsList.size()-1).end += diff;
                        }
                        if(warnlen) {
                            warnlen = false;
                            logger.logWarning("The transcript length and the sum of the exon lengths are different. Alignment may not be accurate.");
                        }
                        logger.logInfo("Transcript " + trans.id + " length is longer, " + diff + "nt, than the sum of the exon lengths. Alignment may not be accurate.");
                    }
                    else if(translen < exonlen) {
                        // make msg strand sensitive
                        if(negStrand) {
                            int lastlen = firstExon.endpos - firstExon.strpos + 1;
                            if(lastlen > diff) {
                                firstExon.strpos += diff;
                                firstExon.ostrpos = firstExon.strpos;
                                ntexonsList.get(0).start += diff;
                            }
                            else {
//DO now!!! - when removing an exon just chg to ~ignore~ type and ignore it in the display
                                // must go back until proper exon found, and adjust that entry
                                // in addition, must delete extra exons entries at the end!!!
                                // not very clean - leave out until I get the new tsv file
                            }
                        }
                        else {
                            int lastlen = lastExon.endpos - lastExon.strpos + 1;
                            if(lastlen > diff) {
                                lastExon.endpos -= diff;
                                lastExon.oendpos = lastExon.endpos;
                                ntexonsList.get(ntexonsList.size()-1).end -= diff;
                            }
                            else {
//DO now!!! - when removing an exon just chg to ~ignore~ type and ignore it in the display
                                // must go back until proper exon found, and adjust that entry
                                // in addition, must delete extra exons entries at the end!!!
                                // not very clean - leave out until I get the new tsv file
                            }
                        }
                        if(warnlen) {
                            warnlen = false;
                            logger.logWarning("The transcript length and the sum of the exon lengths are different. Alignment may not be accurate.");
                        }
                        logger.logInfo("Transcript " + trans.id + " length is shorter, " + diff + "nt, than the sum of the exon lengths. Alignment may not be accurate.");
                    }
                    //System.out.println("Trans: " + trans.strpos + " to " + trans.endpos + ", (" + strpos + ", " + endpos + ")");
                    int cdslen = cds.endpos - cds.strpos + 1;
                    int cdsstrpos = cds.strpos;
                    int cdsendpos = cds.endpos;
                    if(negStrand) {
                        cdsstrpos = (trans.endpos - cds.endpos + 1);
                        cdsendpos = cdsstrpos + cdslen - 1;
                        strpos = (int) seqAlign.getGenomicPos((double) cdsstrpos, ntexonsList);
                    }
                    else {
                        strpos = (int) SeqAlign.getGenomicPos((double) cds.strpos, ntexonsList);
                    }
                    // generate 5/3 internal UTR genomic start and end values, only one entry which can span multiple exons
                    // will be split up properly later on at display time...
                    int lendpos, rstrpos, rendpos;
                    int lstrpos = (int) SeqAlign.getGenomicPos((double) trans.strpos, ntexonsList);
                    // the transcript length and the length computed by exons does not match often, go with trans length
                    rendpos = (int) SeqAlign.getGenomicPos((double) trans.endpos, ntexonsList); //ntexonsList.get(ntexonsList.size()-1).end;
                    if(negStrand) {
                        if(cds.strpos > 1) {
                            rstrpos = (int) SeqAlign.getGenomicPos((double) (trans.endpos - cds.strpos + 2), ntexonsList);
                            AnnotationEntry autr = new AnnotationEntry(ntexon.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_5UTR, "", "", "", rstrpos, rendpos, ".", "", "");
                            if(view.equals(Views.TRANS))
                                al.add(autr);
                            //System.out.println("Calc neg strand 5'UTR for " + ntexon.id + ": " + rstrpos + " to " + rendpos);
                        }
                        if(cds.endpos < trans.endpos) {
                            lendpos = (int) SeqAlign.getGenomicPos((double) (trans.endpos - (cds.endpos + 1) + 1), ntexonsList);
                            AnnotationEntry autr = new AnnotationEntry(ntexon.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_3UTR, "", "", "", lstrpos, lendpos, ".", "", "");
                            if(view.equals(Views.TRANS))
                                al.add(autr);
                            //System.out.println("Calc neg strand 3'UTR for " + ntexon.id + ": " + lstrpos + " to " + lendpos);
                        }
                    }
                    else {
                        if(cds.strpos > 1) {
                            lendpos = (int) SeqAlign.getGenomicPos((double) (cds.strpos - 1), ntexonsList);
                            AnnotationEntry autr = new AnnotationEntry(ntexon.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_5UTR, "", "", "", lstrpos, lendpos, ".", "", "");
                            if(view.equals(Views.TRANS))
                                al.add(autr);
                            //System.out.println("Calc pos strand 5'UTR for " + ntexon.id + ": " + lstrpos + " to " + lendpos);
                        }
                        if(cds.endpos < trans.endpos) {
                            rstrpos = (int) SeqAlign.getGenomicPos((double) (cds.endpos + 1), ntexonsList);
                            AnnotationEntry autr = new AnnotationEntry(ntexon.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_3UTR, "", "", "", rstrpos, rendpos, ".", "", "");
                            if(view.equals(Views.TRANS))
                                al.add(autr);
                            //System.out.println("Calc pos strand 3'UTR for " + ntexon.id + ": " + rstrpos + " to " + rendpos);
                        }
                    }

                    // generate CDS exons using given exon entries which are already in ascending order
                    boolean strfnd = false;
                    strpos = (int) seqAlign.getGenomicPos((double) cdsstrpos, ntexonsList);
                    //System.out.println("CDS start width: " + cdslen);
                    //System.out.println("CDS genomic pos: " + strpos + " to " + endpos);
                    tmpList.clear();
                    for(SeqAlign.Position pos : ntexonsList) {
                        if(strfnd) {
                            // add exon partial or full based on cdslen left
                            int cnt = pos.end - pos.start + 1;
                            int end = pos.end;
                            if(cnt > cdslen) {
                                end = pos.start + cdslen - 1;
                                cdslen = 0;
                            }
                            else
                                cdslen -= cnt;
                            ntcdsList.add(new SeqAlign.Position(pos.start, end));
                            AnnotationEntry acds = new AnnotationEntry(ntexon.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_CDS, "", "", "", pos.start, end, ".", "", "");
                            tmpList.add(acds);
                        }
                        else {
                            // find exon where left position falls in
                            if(strpos >= pos.start && strpos <= pos.end) {
                                strfnd = true;
                                int cnt = pos.end - strpos + 1;
                                int end = pos.end;
                                if(cnt > cdslen) {
                                    end = strpos + cdslen - 1;
                                    cdslen = 0;
                                }
                                else
                                    cdslen -= cnt;
                                ntcdsList.add(new SeqAlign.Position(strpos, end));
                                AnnotationEntry acds = new AnnotationEntry(ntexon.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_CDS, "", "", "", strpos, end, ".", "", "");
                                tmpList.add(acds);
                            }
                        }
                        if(cdslen <= 0)
                            break;
                    }
                    //for(SeqAlign.Position pos : ntcdsList)
                    //    System.out.println("cds_exon: " + pos.start + " to " + pos.end);
                    if(negStrand) {
                        if(!tmpList.isEmpty()) {
                            for(int idx = tmpList.size() - 1; idx >= 0; idx--)
                                al.add(tmpList.get(idx));
                        }
                    }
                    else {
                        for(AnnotationEntry ta : tmpList)
                            al.add(ta);
                    }
                }
                if(!view.equals(Views.TRANS) && !view.equals(Views.GENOMIC)) {
                    ArrayList<AnnotationEntry> alst = new ArrayList<>();
                    for(AnnotationEntry a : al)
                        alst.add(a);
                    al.clear();
                    for(AnnotationEntry a : alst) {
                        if(!project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.TRANSCRIPT))
                            al.add(a);
                    }
                }
            }
            else {
                // there is no alignment data for this transcript
                // add basic data and append a display line for 'no alignment data'
                for(AnnotationEntry a : gta.alst) {
                    if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(view.equals(Views.PROTEIN)? AnnotFeature.PROTEIN : AnnotFeature.TRANSCRIPT)
                             || project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE))
                        al.add(a);
                    if(view.equals(Views.GENOMIC)) {
                        if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENOMIC))
                            al.add(a);
                    }
                }
                al.add(new AnnotationEntry("NoAlignmentData", DataAnnotation.INTDB, DataAnnotation.INTCAT_NODATA, "", "", "", 0, 0, "", "", ""));
            }
            // if the transcript does not have any annotation symbols, we need to flush out the exon alignment data
            if(prefs.alignment)
                al.add(new AnnotationEntry("FlushAlignmentData", DataAnnotation.INTDB, DataAnnotation.INTCAT_FLUSHDATA, "", "", "", 0, 0, "", "", ""));

            for(AnnotationEntry a : al) {
                if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(view.equals(Views.PROTEIN)? AnnotFeature.PROTEIN : AnnotFeature.TRANSCRIPT)) {
                    if(a.strand.isEmpty() || a.strand.equals("."))
                        a.strand = strand;
                    annotationList.add(a);
                    break;
                }
            }
            if(view.equals(Views.GENOMIC)) {
                for(AnnotationEntry a : al) {
                    if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENOMIC)) {
                        al.add(a);
                        break;
                    }
                }
            }
            for(AnnotationEntry a : al) {
                if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE)) {
                    annotationList.add(a);
                    break;
                }
            }
            for(AnnotationEntry a : al) {
                if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.CDS)) {
                    annotationList.add(a);
                    break;
                }
            }
            for(AnnotationEntry a : al) {
                if(a.feature.equals(DataAnnotation.INTCAT_5UTR) || a.feature.equals(DataAnnotation.INTCAT_3UTR))
                    annotationList.add(a);
            }
            for(AnnotationEntry a : al) {
                if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON)) {
                    annotationList.add(a);
                    // add copy so that it can be modified, if negative strand, w/o affecting the original
                    AnnotationEntry ane = new AnnotationEntry(a.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_EXON, "", "", "", a.strpos, a.endpos, ".", "", "");
                    annotationList.add(ane);
                }
                else if(a.feature.equals(DataAnnotation.INTCAT_CDS))
                    annotationList.add(a);
            }
            for(AnnotationEntry a : al) {
                if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.SPLICEJUNCTION)) {
                    annotationList.add(a);
                    // add copy so that it can be modified, if negative strand, w/o affecting the original
                    AnnotationEntry ane = new AnnotationEntry(a.id, DataAnnotation.INTDB, DataAnnotation.INTCAT_SPLICEJUNCTION, "", "", "", a.strpos, a.endpos, ".", "", "");
                    annotationList.add(ane);
                }
            }
            for(AnnotationEntry a : al) {
                if(a.feature.equals(DataAnnotation.INTCAT_FLUSHDATA) || a.feature.equals(DataAnnotation.INTCAT_NODATA))
                    annotationList.add(a);
            }
            for(AnnotationEntry a : al) {
                if(!project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.TRANSCRIPT) &&
                        !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.PROTEIN) &&
                        !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE) &&
                        !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.CDS) && 
                        !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON) &&
                        !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.SPLICEJUNCTION)
                        && !project.data.isInternalFeature(a.source, a.feature))
                    annotationList.add(a);
            }
        }
        
        // check if using alignment - do additional processing if so
        if(prefs.alignment && !ntexonsList.isEmpty()) {
            if(view.equals(Views.PROTEIN)) {    //!!!?
                seqAlign.clearData();
                for(SeqAlign.Position pos : ntcdsList)
                    seqAlign.updGeneExonsList(pos.start, pos.end);
            }
            // calculate size, start and end position for gene display
            // exons from all transcripts count toward the total size
            largest = seqAlign.getGeneExonsSize();
            if(view.equals(Views.PROTEIN))
                largest = largest / 3;
            //System.out.println("Gene Alignment: " + 1 + ".." + largest);
            // reverse positions if negative strand
            if(negStrand) {
                // calculate lowest and highest genome position values for all isoforms, and then
                // reverse values for lstGeneExons and sort - required for proper alignment
                SeqAlign.Position pos = seqAlign.getGeneExonsRange();
                int end = pos.end;
                // WARNING: this function changes seqAlign values from genomic to 1 based
                // this was an easy way to deal with the negative strand but it has a drawback - must keep in mind
                seqAlign.reverseGeneExons();
                
                // reverse values for all internal annotations
                for(AnnotationEntry a : annotationList) {
                    if(project.data.isInternalFeature(a.source, a.feature)) {
                        a.strpos = end - a.oendpos + 1;
                        a.endpos = end - a.ostrpos + 1;
                        if(a.feature.equals(DataAnnotation.INTCAT_SPLICEJUNCTION)) {
                            // SJs position is located in the intron, adjust for display
                            a.strpos--;
                            a.endpos++;
                            System.out.println(a.feature + ": " + a.ostrpos + ".." + a.oendpos + " to " + a.strpos + ".." + a.endpos);
                        }
                    }
                }
            }
            else {
                for(AnnotationEntry a : annotationList) {
                    if(a.feature.equals(DataAnnotation.INTCAT_SPLICEJUNCTION)) {
                        a.strpos--;
                        a.endpos++;
                    }
                }
            }
        }
        // handle length of 0
        if(largest < 1)
            largest = 1;
        String tooltip = geneName + "\n";
        String urlValue = "";
        if(!geneAttrs.isEmpty()) {
            HashMap<String, String> attrs = DataAnnotation.getAttributes(geneAttrs);
            if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId)) {
                urlValue = attrs.get(project.data.getAnnotationFileDefs().attrId);
                tooltip += "ID: " + urlValue + "\n";
            }
            if(attrs.containsKey(project.data.getAnnotationFileDefs().attrDesc))
                tooltip += attrs.get(project.data.getAnnotationFileDefs().attrDesc) + "\n";
        }
        DataAnnotation.AnnotFeatureDef dbcdef = project.data.getRsvdAnnotFeatureDef(AnnotFeature.GENE);
        geneEntry = new AnnotationEntry(geneName, dbcdef.source, dbcdef.feature, "", "", "", 1, largest, geneAttrs, tooltip, urlValue);

        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("Get gene '" + gene + "' annotation, largest: " + largest + ", time: " + duration + " ms");
    }
    protected void getRSAnnotationData(String rsid, ArrayList<String[]> lines, ArrayList<AnnotationEntry> alst) {
        // override
    }  
    
    // should we change starting number to 1 (need to adjust first interval width)?!!!
    protected void drawRuler(GraphicsContext gc, double x, double y, double rgnWidth, double rgnHeight, 
                            double ppb, double strpos, double endpos, double dspOffset, boolean incflg) {
        double range = endpos - strpos + 1;
        double intervalBases = 1;
        while(range/(intervalBases * 10) > 1)
            intervalBases *= 10;
        int count = (int) (range / intervalBases);
        if((range % intervalBases) > 0)
            count++;
        if(count < 5 || count > 12) {
            int idx = 0;
            double ib = intervalBases;
            double[] factors = {2,4,5,10};
            if(count < 5) {
                //System.out.println("Small count: " + count + ", intBases: " + intervalBases);
                while(count < 5 && idx < factors.length) {
                    ib = intervalBases / factors[idx++];
                    count = (int) (range / ib);
                    if((range % ib) > 0)
                        count++;
                }
                intervalBases = ib;
            }
            else {
                //System.out.println("Large count: " + count + ", inBases: " + intervalBases);
                while(count > 10 && idx < factors.length) {
                    ib = intervalBases * factors[idx++];
                    count = (int) (range / ib);
                    if((range % ib) > 0)
                        count++;
                }
                intervalBases = ib;                
            }
        }
        int height = 7;
        double interval = intervalBases * ppb;
        double xoffset = 0;
        if((strpos % intervalBases) > 1) {
            xoffset = (intervalBases - (strpos % intervalBases))/intervalBases * interval;
            count -= 1;
        }

        // draw main horizontal line and large unit ticks
        gc.setFont(defaultFont);
        gc.setStroke(Color.GRAY);
        gc.setFill(Color.GRAY);
        gc.setLineWidth(1.0);
        gc.strokeLine(x, y, x+ rgnWidth, y);
        gc.setTextAlign(TextAlignment.CENTER);
        String label;
        for(int i = 0; i < count; i++) {
            gc.setLineWidth(0.5);
            gc.setStroke(Color.valueOf("#D0D0D0"));
            gc.strokeLine(adjustX(i * interval + x + xoffset), y + 1, i * interval + x + xoffset, y + rgnHeight - 5);
            gc.setLineWidth(1.0);
            gc.setStroke(Color.GRAY);
            double xc = adjustX(i * interval + x + xoffset);
            gc.strokeLine(xc, y + 1, xc, y + height - 1);
            if(incflg)
                label = getRegionXLabel(((Math.floor((strpos + i * intervalBases)/intervalBases) + (xoffset == 0? 0 : 1)) * intervalBases) + dspOffset);
            else
                label = getRegionXLabel(dspOffset - ((Math.floor((strpos + i * intervalBases)/intervalBases) + (xoffset == 0? 0 : 1)) * intervalBases));
            gc.fillText(label, i * interval + x + xoffset, y - 5);
        }
        
        int subCount = 0;
        int cmpval = -1;
        double subInterval = 1;
        double subIntervalBases = intervalBases;
        if(interval > 100) {
            cmpval = 5;
            subCount = 10;
            subInterval = interval / subCount;
            subIntervalBases = intervalBases / subCount;
        }
        else if(interval > 50) {
            cmpval = -1;
            subCount = 5;
            subInterval = interval / subCount;
            subIntervalBases = intervalBases / subCount;
        }
        else if(interval > 10) {
            cmpval = -1;
            subCount = 2;
            subInterval = interval / subCount;
            subIntervalBases = intervalBases / subCount;
        }
        double xadjusted = x;
        double yoffset = 5;
        if(xoffset != 0) {
            xadjusted += xoffset - interval;
            count += 1;
        }
        for(int i = 0; i < count; i++) {
            for(int j = 0; j < subCount; j++) {
                if(j > 0) {
                    if((i == 0 && (j * subIntervalBases) < strpos) || 
                       (i == (count - 1) && (i * intervalBases + j * subIntervalBases) > endpos)) {
                        //System.out.println("Skipping " + (i * intervalBases + j * subIntervalBases));
                        continue;
                    }
                    gc.setLineWidth(0.25);
                    gc.setStroke(Color.valueOf("#E0E0E0"));
                    gc.strokeLine(i * interval + j * subInterval + xadjusted, y + yoffset, i * interval + j * subInterval + xadjusted, y + rgnHeight - yoffset);
                    gc.setLineWidth(1.0);
                    gc.setStroke(Color.GRAY);
                    double xc = adjustX(i * interval + j * subInterval + xadjusted);
                    if(j == cmpval) {
                        gc.strokeLine(xc, y + height - yoffset, xc, y + height);
                    }
                    else
                        gc.strokeLine(xc, y + yoffset, xc, y + height - yoffset);
                }
            }
        }
    }
    protected void displayExons(String trans, GraphicsContext gc, double xoffset, double leftPadding, double ymain, double ppb, 
                              boolean negStrand, ArrayList<SeqAlign.Position> genoexonsList, 
                              ArrayList<SeqAlign.Position> tgexonsList, ArrayList<SeqAlign.Position> tgcdsList, 
                              ArrayList<SpliceJuncData> genoSpliceJuncList, ArrayList<SeqAlign.Position> spliceJuncList, 
                              ArrayList<AnnotationEntry> cdsList,
                              AnnotationEntry seqEntry, AnnotationEntry align5utr, AnnotationEntry align3utr, boolean nmd) {
        // determine starting and ending exon positions for transcript
        // and draw dashed line for full length
        Collections.sort(tgexonsList);
        Collections.sort(genoexonsList);
        int min = 0;
        int max = 0;
        for(SeqAlign.Position ptg : tgexonsList) {
            if(ptg.start < min || min == 0)
                min = ptg.start;
            if(ptg.end > max)
                max = ptg.end;
        }
        gc.setLineWidth(0.25);
        double x = xoffset + leftPadding + seqAlign.getXlatedPos(min) * ppb;
        x = adjustX(x);
        double ddw = seqAlign.getXlatedPos(max) - seqAlign.getXlatedPos(min);
        symbols.drawDashedLine(gc, x, ymain, x + (ddw * ppb), defaultColor);
        gc.setLineWidth(1.0);

        //System.out.println("DSPEXONS " + seqAlign.getXlatedPos(min) + ".." + seqAlign.getXlatedPos(max) + " --------------------------------");

        // add aligned position to transcript tooltip
        double ttstr = seqAlign.getAlignedPos(seqEntry.strpos, tgexonsList);
        double ttend = seqAlign.getAlignedPos(seqEntry.endpos, tgexonsList);
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(ttstr),
                        (ttstr == ttend)? "" : String.format(" - %s", NumberFormat.getInstance().format(ttend)));
        seqEntry.tooltipAligned = String.format("\nAligned: %s", fmtpos);
        //System.out.println(String.format("\nTrans Aligned: %s", fmtpos));
        
        // take into account gaps and alignment to get actual utr and cds segments to display
        ArrayList<SeqAlign.SegmentDisplay> utr5List = new ArrayList<>();
        if(align5utr != null)
            utr5List = seqAlign.getUtrSegments(align5utr.strpos, align5utr.endpos, tgexonsList);
        //System.out.println("dspexon xoff: " + xoffset + ", leftpad: " + leftPadding);
// for negstrand all values are not genomic - the relative values are already off here so we can not match the SJ below!
        for(SeqAlign.SegmentDisplay sd : utr5List)
            dspUTR(gc, xoffset + leftPadding, ymain, ppb, sd, true);
        ArrayList<SeqAlign.SegmentDisplay> utr3List = new ArrayList<>();
        if(align3utr != null) {
            utr3List = seqAlign.getUtrSegments(align3utr.strpos, align3utr.endpos, tgexonsList);
            //System.out.println("au3: " + align3utr.strpos + " to " + align3utr.endpos);
        }
            
        for(SeqAlign.SegmentDisplay sd : utr3List) {
            //System.out.println("dspu3: " + sd.start + ", to " + sd.end);
            dspUTR(gc, xoffset + leftPadding, ymain, ppb, sd, false);
        }
        boolean cdsflg = !tgcdsList.isEmpty();
        ArrayList<SeqAlign.SegmentDisplay> segList = seqAlign.getCdsSegments(cdsflg? tgcdsList : tgexonsList, tgexonsList);
        for(SeqAlign.SegmentDisplay sd : segList) {
            AnnotationEntry ae = new AnnotationEntry("", "", "", "", "", "", (int)seqAlign.getXlatedPos(sd.start), (int)seqAlign.getXlatedPos(sd.end), "", "", "");
            // only add if displaying CDS otherwise symbols will be displayed as if there was a thick CDS box...
            if(cdsflg)
                cdsList.add(ae);
            //System.out.println("CDSLISTADD: " + sd.start + ".." + sd.end + ", " + ae.strpos + ".." + ae.endpos);
            dspCDS(gc, xoffset + leftPadding, ymain, ppb, sd, cdsflg, nmd);
        }
        
        // add exon tool tip
        int idx = 0;
        int genoidx = genoexonsList.size() - 1;
        double height = 6;
        double y = ymain;
        int genostart, genoend;
        String tooltip;
        ArrayList<ExonPos> lstExonsPos = new ArrayList<>();
        for(SeqAlign.Position ptg : tgexonsList) {
            double pos = seqAlign.getXlatedPos(ptg.start);
            x = adjustX(xoffset + leftPadding + pos * ppb);
            if(negStrand) {
                if(genoidx >= 0) {
                    genostart = genoexonsList.get(genoidx).end;
                    genoend = genoexonsList.get(genoidx).start;
                }
                else {
                    genostart = 0;
                    genoend = 0;
                }
            }
            else {
                genostart = ptg.start;
                genoend = ptg.end;
            }
            lstExonsPos.add(new ExonPos(genostart, pos));
            tooltip = String.format("Exon %d\nGenome: %s - %s  (%s bases)\nAligned: %s - %s",
                    (idx + 1),
                    NumberFormat.getInstance().format(genostart), NumberFormat.getInstance().format(genoend), 
                    NumberFormat.getInstance().format(Math.abs(ptg.end - ptg.start) + 1),
                    NumberFormat.getInstance().format(seqAlign.getXlatedPos(ptg.start)),
                    NumberFormat.getInstance().format(seqAlign.getXlatedPos(ptg.end)));
            AnnotationEntry blk = new AnnotationEntry("", "", "", "", "", "", 0, 0, "", "", "");
            tipsList.add(new ToolTipPoints(blk, x, adjustY(y - (height/2)), 
                        (Math.abs(ptg.end - ptg.start) + 1) * ppb, adjustValue(6), tooltip, false));
            idx++;
            genoidx--;
        }
        if(spliceJuncList != null && !spliceJuncList.isEmpty() && genoSpliceJuncList != null && !genoSpliceJuncList.isEmpty() &&
                spliceJuncList.size() == genoSpliceJuncList.size()) {
            int num = 1;
            Collections.sort(spliceJuncList);
            Collections.sort(genoSpliceJuncList);
            if(negStrand)
                genoidx = genoSpliceJuncList.size() - 1;
            else
                genoidx = 0;
            double apos[] = new double[2];
            double ax[] = new double[2];
            //System.out.println("ppb splice: " + ppb);
            //System.out.println("splice xoff: " + xoffset + ", leftpad: " + leftPadding);
            AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
            for(SeqAlign.Position ptg : spliceJuncList) {
                SpliceJuncData sjd = genoSpliceJuncList.get(genoidx);
                apos[0] = seqAlign.getXlatedPos(ptg.start);
                boolean found = false;
                int sjgenostart = negStrand? sjd.donor : ptg.start;
                for(ExonPos ep : lstExonsPos) {
                    if(sjgenostart == ep.genomic) {
                        apos[0] = ep.xlated;
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    for(SeqAlign.SegmentDisplay sd : segList) {
                        if(ptg.start == sd.start) {
                            apos[0] = sd.x;
                            found = true;
                            break;
                        }
                        else if(ptg.start == sd.end) {
                            apos[0] = sd.x + sd.width;
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        for(SeqAlign.SegmentDisplay sd : utr5List) {
                            if(ptg.start == sd.start) {
                                apos[0] = sd.x;
                                found = true;
                                break;
                            }
                            else if(ptg.start == sd.end) {
                                apos[0] = sd.x + sd.width;
                                found = true;
                                break;
                            }
                        }
                        if(!found) {
                            for(SeqAlign.SegmentDisplay sd : utr3List) {
                                if(ptg.start == sd.start) {
                                    apos[0] = sd.x;
                                    found = true;
                                    break;
                                }
                                else if(ptg.start == sd.end) {
                                    apos[0] = sd.x + sd.width;
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                ax[0] = adjustX(xoffset + leftPadding + apos[0] * ppb);
                apos[1] = seqAlign.getXlatedPos(ptg.end);
                int sjgenoend = negStrand? sjd.acceptor : ptg.end;
                for(ExonPos ep : lstExonsPos) {
                    if(sjgenoend == ep.genomic) {
                        apos[1] = ep.xlated;
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    for(SeqAlign.SegmentDisplay sd : segList) {
                        if(ptg.end == sd.start) {
                            apos[1] = sd.x;
                            found = true;
                            break;
                        }
                        else if(ptg.end == sd.end) {
                            apos[1] = sd.x + sd.width;
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        for(SeqAlign.SegmentDisplay sd : utr5List) {
                            if(ptg.end == sd.start) {
                                apos[1] = sd.x;
                                found = true;
                                break;
                            }
                            else if(ptg.end == sd.end) {
                                apos[1] = sd.x + sd.width;
                                found = true;
                                break;
                            }
                        }
                        if(!found) {
                            for(SeqAlign.SegmentDisplay sd : utr3List) {
                                if(ptg.end == sd.start) {
                                    apos[1] = sd.x;
                                    found = true;
                                    break;
                                }
                                else if(ptg.end == sd.end) {
                                    apos[1] = sd.x + sd.width;
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                ax[1] = (xoffset + leftPadding + apos[1] * ppb); //adjustX
                // depending on the zooming, the values in ax will differ by more 
                // even if next to each other - use apos which is not affected by resolution
                //boolean combined = Math.abs(ax[1] - ax[0]) < 2;
// there is no way to tell the difference between an SJ that is across a 1 nt gap
// and an SJ that is continous not across a visual gap (alignment) since they are both 1 apart
// would need to flag gapped vs non-gapped somehow or just ignore it since it is rare and fairly insignificant
                boolean combined = Math.abs(apos[1] - apos[0]) < 2;
                if(combined)
                    ax[0] = ax[1];
                for(int i = 0; i < 2; i++) {
                    // display splice junction line
                    // Note: using the ID to determine if it is a novel junction (if it has the word "novel" in it) bad decision!
                    symbols.drawSpliceJunction(gc, ax[i], ymain, AnnotationSymbols.CDSblockHeight + 9, sjd.id.contains(defs.valSJ), (combined? 3 : i+1));
                    // setup tooltip
                    if(negStrand) {
                        if(genoidx >= 0) {
                            genostart = genoSpliceJuncList.get(genoidx).pos.end;
                            genoend = genoSpliceJuncList.get(genoidx).pos.start;
                        }
                        else {
                            genostart = 0;
                            genoend = 0;
                        }
                    }
                    else {
                        genostart = ptg.start;
                        genoend = ptg.end;
                    }
                    int tipidx = i;
                    if(sjd.name.isEmpty())
                        tooltip = String.format("Splice Junction %d\nID: %s", num, sjd.id);
                    else
                        tooltip = String.format("Splice Junction %d\nID: %s\nName: %s", num, sjd.id, sjd.name);
                    tooltip += String.format("\nGenome " + (i==0? "Donor" : "Acceptor") + ": %s\nAligned " + (i==0? "Donor" : "Acceptor") + ": %s",
                            NumberFormat.getInstance().format(i==0? (negStrand? (genostart-1) : (genostart+1)) : (negStrand? (genoend+1) : (genoend-1))),
                            NumberFormat.getInstance().format(apos[i]));
                    // if there is no significant separation, consolidate into just one symbol with tooltip for both donor/acceptor
                    if(combined && i == 0) {
                        tooltip += String.format("\nGenome Acceptor: %s\nAligned Acceptor: %s",
                                NumberFormat.getInstance().format(negStrand? (genoend+1) : (genoend-1)), 
                                NumberFormat.getInstance().format(apos[1]));
                        i++;
                    }
                    AnnotationEntry blk = new AnnotationEntry("", "", "", "", "", "", 0, 0, "", "", "");
                    tipsList.add(new ToolTipPoints(blk, ax[tipidx]-2.5, adjustY(ymain - ((AnnotationSymbols.CDSblockHeight+9+4)/2)), 5, (AnnotationSymbols.CDSblockHeight+6), tooltip, false));
                }
                num++;
                if(negStrand)
                    genoidx--;
                else
                    genoidx++;
            }
        }
    }
    protected void dspUTR(GraphicsContext gc, double x, double y, double ppb, SeqAlign.SegmentDisplay sd, boolean utr5flg) {
        String type = utr5flg? "5'UTR" : "3'UTR";
        x = adjustX(x + sd.x * ppb);
        y = adjustY(y - (AnnotationSymbols.RNAblockHeight/2)); 
        symbols.drawRNAblock(gc, x, y, sd.width * ppb);
        AnnotationEntry utr = new AnnotationEntry("", "", "", "", "", "", 0, 0, "", "", "");
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(sd.xRelative),
                        String.format(" - %s", NumberFormat.getInstance().format(sd.xRelative+sd.width-1)));
        tipsList.add(new ToolTipPoints(utr, x, y + 1, sd.width * ppb, adjustValue(AnnotationSymbols.RNAblockHeight),
                            String.format("%s\nPosition: %s", type, fmtpos), false));
        double ttstr = sd.x;
        double ttend = sd.x+sd.width-1;
        fmtpos = String.format("%s%s", NumberFormat.getInstance().format(ttstr),
                        (ttstr == ttend)? "" : String.format(" - %s", NumberFormat.getInstance().format(ttend)));
        utr.tooltipAligned = String.format("\nAligned: %s", fmtpos);
    }
    // Note: Since we display the CDS in blocks the tooltip can be misleading in cases where the blocks are very close together.
    //       When you mouse over one part of the CDS you may think the tool-tip range shown is for the whole CDS since it looks like one block
    //       It won't be clear until you zoom-in real close, PB.1403.3 is an example
    protected void dspCDS(GraphicsContext gc, double x, double y, double ppb, SeqAlign.SegmentDisplay sd, boolean cdsflg, boolean nmd) {
        String type = cdsflg? "CDS" : "RNA";
        double height = cdsflg? AnnotationSymbols.CDSblockHeight : AnnotationSymbols.RNAblockHeight;
        x = adjustX(x + sd.x * ppb);
        y = adjustY(y - (height/2));
        if(cdsflg)
            symbols.drawCDSblock(gc, x, y, sd.width * ppb, nmd); // add nullstring because is a param to color NMD CDS!!!
        else
            symbols.drawRNAblock(gc, x, y, sd.width * ppb);
        AnnotationEntry blk = new AnnotationEntry("", "", "", "", "", "", 0, 0, "", "", "");
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(sd.xRelative),
                        String.format(" - %s", NumberFormat.getInstance().format(sd.xRelative+sd.width-1)));
        tipsList.add(new ToolTipPoints(blk, x, y + 1, sd.width * ppb, adjustValue(height),
                            String.format("%s\nPosition: %s", type, fmtpos), false));
        double ttstr = sd.x;
        double ttend = sd.x+sd.width-1;
        fmtpos = String.format("%s%s", NumberFormat.getInstance().format(ttstr),
                        (ttstr == ttend)? "" : String.format(" - %s", NumberFormat.getInstance().format(ttend)));
        blk.tooltipAligned = String.format("\nAligned: %s", fmtpos);
    }
    // this does not take into account the 'ppb' so when the ppb is really low, it is possible to end up with stacked/hidden symbols
    private boolean IsThereRoom(ArrayList<UsedPoints> seqList, AnnotationSymbols.SymbolDef symdef, 
            double str, double width, double strd, double widthd) {
        boolean result = true;
        double xs = strd;
        double xe = strd + widthd;
        for(UsedPoints up : seqList) {
            if((xs >= up.xd && xs <= (up.xd + up.widthd)) || (xe >= up.xd && xe <= (up.xd + up.widthd)) ||
                    (xs < up.xd && xe > (up.xd + up.widthd))) {
                //System.out.println("No room: " + xs + ":" + xe);
                result = false;
                break;
            }
        }
        return result;
    }
    public double adjustX(double x) {
        if(scale != 1)
        {
            double i = x * scale;
            i = Math.floor(i) + 0.5;
            x = i / scale;
        }
        else
            x = Math.floor(x) + 0.5;
        return x;
    }
    public double adjustY(double y) {
        if(scale != 1)
        {
            double i = y * scale;
            i = Math.floor(i) + 0.5;
            y = i / scale;
        }
        else
            y = Math.floor(y) + 0.5;
        return y;
    }
    public double adjustValue(double value) {
        if(scale != 1)
            return (Math.round(value * scale)/scale);
        return value;
    }
    
    public void addToolTipToList(AnnotationEntry entry, double x, double y, double width, double height, boolean setHandCursor) {
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(entry.strpos),
                            (entry.strpos == entry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(entry.endpos)));
        tipsList.add(new ToolTipPoints(entry, x, y, width, height,
                            String.format("%s\nPosition: %s", entry.tooltip, fmtpos), setHandCursor));
    }
    public void addToolTipToList(String tip, double x, double y, double width, double height, boolean setHandCursor) {
        tipsList.add(new ToolTipPoints(null, x, y, width, height, tip, setHandCursor));
    }
    static public String getRegionXLabel(double pos) {
        double divisor = 1;
        int[] factors = {1, 1000, 1000000, 1000000000};
        for(int idx = 0; idx < factors.length; idx++) {
            divisor = factors[idx];
            if(pos/divisor < 1000)
                break;
        }

        String lbl;
        String units = getUnits(divisor);
        if(pos >= 1000000000)
            lbl = String.format("%.7f", (pos/divisor));
        else if(pos >= 1000000)
            lbl = String.format("%.4f", (pos/divisor));
        else
            lbl = String.format("%.2f", (pos/divisor));
        while(lbl.endsWith("0"))
            lbl = lbl.substring(0, lbl.length() - 1);
        if(lbl.endsWith("."))
            lbl = lbl.substring(0, lbl.length() - 1);
        return lbl + units;
    }
    static public String getUnits(double value) {
        String units = "";
        if(value >= 1000000000)
            units = "G";
        else if(value >= 1000000)
            units = "M";
        else if(value >= 1000)
            units = "K";   
        return units;
    }

    //
    // Data Classes
    //
    //
    protected class DrawArgs {
        public Prefs prefs;
        public int redrawCount;
        public double hRegion;
        public boolean singleColumn;
        public DrawArgs(Prefs prefs, int redrawCount, double hRegion, boolean singleColumn) {
            this.prefs = prefs;
            this.redrawCount = redrawCount;
            this.hRegion = hRegion;
            this.singleColumn = singleColumn;
        }
    }
    public class AnnotationEntry {
        public String id;
        public String source;
        public AnnotationSymbols.SymbolAssignment symbol;
        public String feature;
        public String featureId;
        public String caption;
        public String strand;
        public int strpos, endpos;
        public int ostrpos, oendpos;
        public double dsppos, dspstrpos, dspendpos;
        public String attrs;        // optional attributes in form of key1=value1;key2=value2
        public String parent;
        public int offset;
        public String tooltip;
        public String tooltipAligned;
        public String urlValue;
        
        public AnnotationEntry(String id, String source, String feature, String featureId, String caption, String strand, int strpos, int endpos, 
                                String attrs, String tooltip, String urlValue) {
            this.id = id;
            this.source = source;
            this.feature = feature;
            this.featureId = featureId;
            this.caption = caption;
            if(strpos == 0 || endpos == 0 || feature.equals("") || project.data.isRsvdFeature(source, feature)) {
                this.symbol = null;
            }
            else {
                String subId = "";
                AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
                if(defs.isDomain(source, feature))
                    subId = caption;
                this.symbol = symbols.getSymbolAssignment(source, feature, caption, subId);
            }
            this.strand = strand;
            this.strpos = strpos;
            this.ostrpos = strpos;
            this.endpos = endpos;
            this.oendpos = endpos;
            this.dsppos = strpos;
            this.dspstrpos = strpos;
            this.dspendpos = endpos;
            if(strpos != endpos)
                this.dsppos = strpos + (endpos - strpos + 1)/2;
            //this.algdsppos = this.dsppos;
            this.attrs = attrs;
            this.parent = "";
            this.offset = 0;
            this.tooltip = tooltip;
            this.urlValue = urlValue;
            this.tooltipAligned = "";
        } 
    }
    public class ProveanData implements Comparable<ProveanData> {
        public String trans;
        public String id;
        public String score;
        public double pscore = 0;
        public ProveanData(String trans, String attrsField) {
            this.trans = trans;
            this.id = "'";
            this.score = "";
            if(!attrsField.trim().isEmpty()) {
                AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
                HashMap<String, String> attrs = DataAnnotation.getAttributes(attrsField);
                if(attrs.containsKey(defs.attrPScore)) {
                    score = attrs.get(defs.attrPScore);
                    try {
                        if(score.equals(DataAnnotation.PROVEAN_EXACT))
                            pscore = DataAnnotation.PEXACT_SCORE;
                        else
                            pscore = Double.parseDouble(score);
                    }
                    catch(Exception e) { System.out.println("Provean score processing exception: " + e.getMessage());}
                    //System.out.println("PS(" + trans + "): " + pscore);
                }
                if(attrs.containsKey(defs.attrId))
                    id = attrs.get(defs.attrId);
            }
        }
        @Override
        public int compareTo(ProveanData pd) {
            return ((pscore > pd.pscore)? 1 : (pscore < pd.pscore)? -1 : 0);
        }
    }    
    public class SpliceJuncData implements Comparable<SpliceJuncData> {
        public SeqAlign.Position pos;
        public int donor, acceptor;
        public String id;
        public String name;
        public SpliceJuncData(boolean negStrand, SeqAlign.Position pos, String attrsField) {
            this.pos = pos;
            this.donor = negStrand? pos.end : pos.start;
            this.acceptor = negStrand? pos.start : pos.end;
            this.id = "";
            this.name = "";
            if(!attrsField.trim().isEmpty()) {
                HashMap<String, String> attrs = DataAnnotation.getAttributes(attrsField);
                if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId))
                    id = attrs.get(project.data.getAnnotationFileDefs().attrId);
                if(attrs.containsKey(project.data.getAnnotationFileDefs().attrName))
                    name = attrs.get(project.data.getAnnotationFileDefs().attrName);
            }
        }
        @Override
        public int compareTo(SpliceJuncData sjd) {
            return ((pos.start > sjd.pos.start)? 1 : (pos.start < sjd.pos.start)? -1 : 0);
        }
    }    
    public class ToolTipPoints {
        public AnnotationEntry entry;
        public double x;
        public double y;
        public double width;
        public double height;
        public Rectangle2D.Double rect;
        public String tooltip;
        public boolean setHandCursor;
        public ToolTipPoints(AnnotationEntry entry, double x, double y, double width, double height, String tooltip, boolean setHandCursor) {
            this.entry = entry;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.rect = new Rectangle2D.Double(this.x, this.y, width, height);
            this.tooltip = tooltip;
            this.setHandCursor = setHandCursor;
        }
    }
    public class UsedPoints {
        public double x, xd;
        public double width, widthd;
        public UsedPoints(double x, double width, double xd, double widthd) {
            this.x = x;
            this.width = width;
            this.xd = xd;
            this.widthd = widthd;
        }
    }
    public class ViewAnnotationEntry implements Comparable<ViewAnnotationEntry>{
        public String id;
        public int size;
        ArrayList<AnnotationEntry> alst;
        public ViewAnnotationEntry(String id, int size, ArrayList<AnnotationEntry> alst) {
            this.id = id;
            this.size = size;
            this.alst = alst;
        }
        @Override
        public int compareTo(ViewAnnotationEntry gta) {
            int lastCmp = id.compareTo(gta.id);
            if (id.substring(0,2).equals(gta.id.substring(0, 2)))
                return ((size > gta.size)? 1 : (size < gta.size)? -1 : 0);
            else
                return (lastCmp != 0 ? lastCmp : ((size > gta.size)? 1 : (size < gta.size)? -1 : 0));
        }
    }
    public static class Prefs {
        public boolean ruler;
        public boolean details;
        public double scale;               // must be >= 1 and <= 2
        public double widthFactor;         // must be >= 1 and <= 4
        public boolean provean;
        public boolean alignment;
        public boolean spliceJuncs;
        public boolean structSubClass;
        public boolean sortDescending;
        public ViewAnnotation.SortBy sortBy;
        public boolean showVaryingOnly;
        public HashMap<String, HashMap<String, HashMap<String, Object>>> hmNotFeatureIDs;
        public Prefs(boolean ruler, double scale, double widthFactor, boolean provean, boolean alignment, 
                boolean spliceJuncs, boolean structSubClass, boolean details,
                boolean sortDescending, ViewAnnotation.SortBy sortBy) {
            this.ruler = ruler;
            this.scale = scale;
            this.widthFactor = widthFactor;
            this.provean = provean;
            this.alignment = alignment;
            this.spliceJuncs = spliceJuncs;
            this.structSubClass = structSubClass;
            this.details = details;
            this.sortDescending = sortDescending;
            this.sortBy = sortBy;
            // annotation features filter
            this.showVaryingOnly = false;
            this.hmNotFeatureIDs = new HashMap<>();
        }
        public boolean isNotFeatureID(String source, String feature, String id) {
            boolean result = false;
            if(hmNotFeatureIDs != null) {
                if(hmNotFeatureIDs.containsKey(source)) {
                    HashMap<String, HashMap<String, Object>> hmNotFeatures = hmNotFeatureIDs.get(source);
                    if(hmNotFeatures.isEmpty())
                        result = true;
                    else {
                        if(hmNotFeatures.containsKey(feature)) {
                            HashMap<String, Object> hmNotIDs = hmNotFeatures.get(feature);
                            if(hmNotIDs.isEmpty())
                                result = true;
                            else {
                                if(hmNotIDs.containsKey(id))
                                    result = true;
                            }
                        }
                    }
                }
            }
            return result;
        }
    }
    public class TranscriptSortEntry implements Comparable<TranscriptSortEntry>{
        public String id;
        public double val;
        public TranscriptSortEntry(double val, String id) {
            this.id = id;
            this.val = val;
        }
        @Override
        public int compareTo(TranscriptSortEntry tse) {
            int result = ((val > tse.val)? 1 : (val < tse.val)? -1 : 0);
            if(result == 0)
                result = id.compareTo(tse.id);
            return result;
        }
    }
    public class ExonPos {
        public int genomic;
        public double xlated;
        public ExonPos(int genomic, double xlated) {
            this.genomic = genomic;
            this.xlated = xlated;
        }
    }
}
