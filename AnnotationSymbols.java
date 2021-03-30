/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import tappas.DbProject.AFStatsData;
import tappas.ViewAnnotation.AnnotationEntry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static tappas.DataAnnotation.POLYA_FEATURE;
import static tappas.DataAnnotation.STRUCTURAL_SOURCE;

/**
 * Class to draw all annotation symbols
 * <p>
 * All annotation symbols in the Gene Data Visualization tab
 * are defined here and drawn by the functions in this class.
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 * 
 */
public class AnnotationSymbols extends AppObject {
    public static final double MaxSymbolWidth = 9;
    public static final double CDSblockHeight = 15;
    public static final double RNAblockHeight = 3;
    public static final double IntronBlockHeight = 1;
    private final HashMap<String, SourceSymbol> hmSymbols = new HashMap<>();
    private final HashMap<String, HashMap<String, Object>> hmBlockFeatures = new HashMap<>();
    private final ArrayList<String> lstDomainSources = new ArrayList<>();
    
    private final ArrayList<SymbolType> symbolTypesList = new ArrayList<>();
    int MAX_SHAPE_IDX = 12;  // 0 based index
    int MAX_BLOCK_IDX = 5;  // 0 based index
    int BLOCK_IDX = MAX_SHAPE_IDX + 1;
    static public enum Shape {
        STAR, ARROW, ARROWDOWN, DIAMOND, DIAMONDDOWN, DROP, DROPDOWN, TRIANGLE, TRIANGLEDOWN, 
        OVAL, POINTEDRECT, BARREL, DOT,
        // block symbols (rectangular strips) used if majority of source:feature
        // are longer or equal to TRANS_CUTOFF_LEN, or PROTEIN_CUTOFF_LEN
        BLOCK1, BLOCK2, BLOCK3, BLOCK4, BLOCK5, BLOCK6,
        // special cases below, place normal symbols above...
        CDS, ORF, DOMAIN, DOMAIN2, DOMAIN3, DOMAIN4, NONE
    }
    static public boolean isBlockSymbol(SymbolDef sd) {
        boolean result = false;
        if(sd.shape.equals(Shape.BLOCK1) || sd.shape.equals(Shape.BLOCK2) ||
                sd.shape.equals(Shape.BLOCK3) || sd.shape.equals(Shape.BLOCK4) ||
                sd.shape.equals(Shape.BLOCK5) || sd.shape.equals(Shape.BLOCK6))
            result = true;
        return result;
    }
    // Annotation symbols definition list - MUST BE IN SAME ORDER AS enum
    // Note: should use either oval or pointedrectangle (not both) - they look too similar
    private final List<SymbolDef> symbolsDefList = Arrays.asList(
        // diamonds
        new SymbolDef(Shape.POINTEDRECT, 5, 8, 2, 0.75, 10),
        new SymbolDef(Shape.ARROW, 4, 7, 3, 1.0, 5),
        new SymbolDef(Shape.ARROWDOWN, 4, 7, 3, 1.0, 10),
        new SymbolDef(Shape.DROP, 6, 6, 8, 0.75, 25),
        new SymbolDef(Shape.DROPDOWN, 6, 6, 8, 0.75, 30),
        new SymbolDef(Shape.OVAL, 5, 9, 0, 0.75, 0),
        new SymbolDef(Shape.DIAMOND, 5, 8, 0, 0.75, 17), //!!!
        new SymbolDef(Shape.TRIANGLE, 7, 6.062, 0, 0.75, 3),
        new SymbolDef(Shape.TRIANGLEDOWN, 7, 6.062, 0, 0.75, 35),
        new SymbolDef(Shape.STAR, 8, 8, 0, 0.75, 23), //!!!
        new SymbolDef(Shape.DIAMONDDOWN, 5, 8, 0, 0.75, 20),
        new SymbolDef(Shape.BARREL, 4, 6, 0, 0.75, 15),
        new SymbolDef(Shape.DOT, 6, 6, 0, 0.75, 36), //!!!
        
        // block symbols - must be at offset BLOCK_IDX (SymbolDef.value is used as block number)
        new SymbolDef(Shape.BLOCK1, 9, 5, 1, 0.5, 9),//10),
        new SymbolDef(Shape.BLOCK2, 9, 5, 2, 0.5, 17),//15),
        new SymbolDef(Shape.BLOCK3, 9, 5, 3, 0.5, 0),//20),
        new SymbolDef(Shape.BLOCK4, 9, 5, 4, 0.5, 16),//25),
        new SymbolDef(Shape.BLOCK5, 9, 5, 5, 0.5, 42),//30),
        new SymbolDef(Shape.BLOCK6, 9, 5, 6, 0.5, 43),//35),
        // special symbols below
        new SymbolDef(Shape.CDS, 9, 9, 0, 0.75, 42),//0),            // special case for legend display only
        new SymbolDef(Shape.ORF, 9, 3, 0, 0.75, 40),//0),            // special case for legend display only
        new SymbolDef(Shape.DOMAIN, 9, 9, 0, 0.75, 40),//10),        // special case for legend display only
        new SymbolDef(Shape.DOMAIN2, 9, 9, 0, 0.75, 40),//19),       // special case for legend display only
        new SymbolDef(Shape.DOMAIN3, 9, 9, 0, 0.75, 40),//28),       // special case for legend display only
        new SymbolDef(Shape.DOMAIN4, 9, 9, 0, 0.75, 40),//0),        // special case for legend display only
        new SymbolDef(Shape.NONE, 2, 5, 9, 0.75, 42)//0)            // Unassigned source: none
    );
    // Annotation symbols color list - whole idea is to spread the colors around
    // Colors will be used sequentially from the starting point and continue
    // until end of list then wrap around if the number of feature Ids is large enough
    // Feel free to add or re-arrange as long as you understand how it works.
    // In general, don't keep colors that are too similar near each other.
    static private final List<SymbolColors> COLORS_LIST = Arrays.asList(
        /*new SymbolColors(Color.GOLDENROD, Color.BLACK),
        new SymbolColors(Color.PALETURQUOISE, Color.BLACK),
        new SymbolColors(Color.MAROON, Color.BLACK),
        new SymbolColors(Color.ORANGERED, Color.BLACK),
        new SymbolColors(Color.LAVENDER, Color.BLACK),
        
        new SymbolColors(Color.FUCHSIA, Color.BLACK),
        new SymbolColors(Color.ORANGE, Color.BLACK),
        new SymbolColors(Color.STEELBLUE, Color.BLACK),
        new SymbolColors(Color.MEDIUMVIOLETRED, Color.BLACK),
        new SymbolColors(Color.BROWN, Color.BLACK),
        
        new SymbolColors(Color.ORANGE, Color.BLACK),
        new SymbolColors(Color.GREEN, Color.BLACK),
        new SymbolColors(Color.DARKVIOLET, Color.BLACK),
        new SymbolColors(Color.RED, Color.BLACK),
        new SymbolColors(Color.LIGHTSLATEGRAY, Color.BLACK),

        new SymbolColors(Color.HOTPINK, Color.BLACK),
        new SymbolColors(Color.YELLOW, Color.BLACK),
        new SymbolColors(Color.CYAN, Color.BLACK),
        new SymbolColors(Color.LIMEGREEN, Color.BLACK),
        new SymbolColors(Color.ANTIQUEWHITE, Color.BLACK),

        new SymbolColors(Color.SKYBLUE, Color.BLACK),
        new SymbolColors(Color.SILVER, Color.BLACK),
        new SymbolColors(Color.TEAL, Color.BLACK),
        new SymbolColors(Color.LIGHTPINK, Color.BLACK),
        new SymbolColors(Color.PEACHPUFF, Color.BLACK),
        
        new SymbolColors(Color.GREENYELLOW, Color.BLACK),
        new SymbolColors(Color.MAGENTA, Color.BLACK),
        new SymbolColors(Color.PALETURQUOISE, Color.BLACK),
        new SymbolColors(Color.BLUEVIOLET, Color.BLACK),
        new SymbolColors(Color.ROSYBROWN, Color.BLACK),
        
        new SymbolColors(Color.CORNSILK, Color.BLACK),
        new SymbolColors(Color.CHOCOLATE, Color.BLACK),
        new SymbolColors(Color.AQUAMARINE, Color.BLACK),
        new SymbolColors(Color.DODGERBLUE, Color.BLACK),
        new SymbolColors(Color.FORESTGREEN, Color.BLACK)*/
        new SymbolColors(Color.DEEPSKYBLUE, Color.BLACK),
        new SymbolColors(Color.CRIMSON, Color.BLACK),
        new SymbolColors(Color.LIGHTGRAY, Color.BLACK),
        new SymbolColors(Color.BLUEVIOLET, Color.BLACK),
        new SymbolColors(Color.BURLYWOOD, Color.BLACK),
        
        //5
        new SymbolColors(Color.CADETBLUE, Color.BLACK),
        new SymbolColors(Color.MEDIUMVIOLETRED, Color.BLACK), //!!!
        new SymbolColors(Color.ROYALBLUE, Color.BLACK), 
        new SymbolColors(Color.PALEGREEN, Color.BLACK),
        new SymbolColors(Color.LIGHTCYAN, Color.BLACK),
        
        //10
        new SymbolColors(Color.TURQUOISE, Color.BLACK),
        new SymbolColors(Color.DEEPPINK, Color.BLACK),
        new SymbolColors(Color.FIREBRICK, Color.BLACK),
        new SymbolColors(Color.ORANGE, Color.BLACK),
        new SymbolColors(Color.FUCHSIA, Color.BLACK),
        
        new SymbolColors(Color.GOLD, Color.BLACK),
        new SymbolColors(Color.GREENYELLOW, Color.BLACK),
        new SymbolColors(Color.HOTPINK, Color.BLACK),
        new SymbolColors(Color.LAVENDERBLUSH, Color.BLACK),
        new SymbolColors(Color.LAWNGREEN, Color.BLACK),
        
        //20
        new SymbolColors(Color.FORESTGREEN, Color.BLACK),
        new SymbolColors(Color.LIGHTGREEN, Color.BLACK),
        new SymbolColors(Color.LIGHTSEAGREEN, Color.BLACK),
        new SymbolColors(Color.MEDIUMORCHID, Color.BLACK),
        new SymbolColors(Color.MEDIUMSEAGREEN, Color.BLACK),
        
        new SymbolColors(Color.MEDIUMVIOLETRED, Color.BLACK),
        new SymbolColors(Color.ORANGE, Color.BLACK),
        new SymbolColors(Color.PALETURQUOISE, Color.BLACK),
        new SymbolColors(Color.PALEVIOLETRED, Color.BLACK),
        new SymbolColors(Color.PLUM, Color.BLACK),
        
        //30
        new SymbolColors(Color.ROYALBLUE, Color.BLACK),
        new SymbolColors(Color.SALMON, Color.BLACK),
        new SymbolColors(Color.SEASHELL, Color.BLACK),
        new SymbolColors(Color.MEDIUMSPRINGGREEN, Color.BLACK),
        new SymbolColors(Color.SLATEBLUE, Color.BLACK),
        
        new SymbolColors(Color.STEELBLUE, Color.BLACK),
        new SymbolColors(Color.DARKTURQUOISE, Color.BLACK),
        new SymbolColors(Color.VIOLET, Color.BLACK),
        new SymbolColors(Color.YELLOW, Color.BLACK),
        new SymbolColors(Color.TURQUOISE, Color.BLACK),
        
        //40
        new SymbolColors(Color.YELLOWGREEN, Color.BLACK),
        new SymbolColors(Color.SPRINGGREEN, Color.BLACK),
        new SymbolColors(Color.CRIMSON, Color.BLACK),
        new SymbolColors(Color.GOLDENROD, Color.BLACK),
        new SymbolColors(Color.DARKORCHID, Color.BLACK),
            
        //45
        new SymbolColors(Color.ROYALBLUE, Color.BLACK),
        new SymbolColors(Color.GREEN, Color.BLACK),
        new SymbolColors(Color.ORANGE, Color.BLACK)
    );
    static public Color getFillColor(int colorIdx) {
        Color color = Color.TRANSPARENT;
        if(colorIdx >= 0 && colorIdx < COLORS_LIST.size())
            color = COLORS_LIST.get(colorIdx).fill;
        return color;
    }
    private final ViewAnnotation viewer;
    AnnotationFileDefs definitions;
    
    /**
     * Instantiates an AnnotationSymbols - constructor
     * 
     * @param   project project object if applicable, may be null
     * @param   logger  logger object if applicable, may be null - defaults to app.logger
     * @param   viewer  annotation viewer object
     */
    public AnnotationSymbols(Project project, Logger logger, ViewAnnotation viewer) {
        super(project, logger);
        this.viewer = viewer;
    }
    // source and features are hashed and should be unique
    public void initialize(HashMap<String, HashMap<String, AFStatsData>> hmSourceFeatures) {
        hmBlockFeatures.clear();
        HashMap<String, String> hmbf =  Utils.loadSingleStringTSVListFromFile(project.data.getAnnotationBlockFeaturesFilepath(), false);
        for(String source : hmbf.keySet()) {
            if(!hmBlockFeatures.containsKey(source)) {
                HashMap<String, Object> hm = new HashMap<>();
                hm.put(hmbf.get(source), null);
                hmBlockFeatures.put(source, hm);
            }
            else
                hmBlockFeatures.get(source).put(hmbf.get(source), null);
        }
        int idx = 0;
        int idxBlock = 0;
        hmSymbols.clear();
        if(hmSourceFeatures != null) {
            // Note: we currently need to process APP_SOURCE because of the CDS feature
            definitions = project.data.getAnnotationFileDefs();
            for(String source : hmSourceFeatures.keySet()) {
                boolean hasNonBlock = true;
                HashMap<String, AFStatsData> hmSF = hmSourceFeatures.get(source);
                boolean hasBlock = hmBlockFeatures.containsKey(source);
                HashMap<String, Object> hmBF = hasBlock? hmBlockFeatures.get(source) : new HashMap<>();

                // see if this is a source containing block symbol features
                if(hasBlock) {
                    // see if source has any normal features, we don't want to waste a symbol if not
                    hasNonBlock = false;
                    for(String feature : hmSF.keySet()) {
                        if(hmBF.containsKey(feature)) {
                            hasNonBlock = true;
                            break;
                        }
                    }
                }
                    
                // get next available symbol to use
                // recycle shapes if no new ones available
                if(idx > MAX_SHAPE_IDX)
                    idx = 0;
                SymbolDef sd = symbolsDefList.get(idx);

                // get next available block symbol to use
                // recycle block symbols if no new ones available
                if(idxBlock > MAX_BLOCK_IDX)
                    idxBlock = 0;
                SymbolDef sdb = symbolsDefList.get(BLOCK_IDX + idxBlock);

                // if this source has block/nonblock features then set for next symbol
                // otherwise we just assign the symbol for consistency
                // but will use it for the next source instead of wasting it
                if(hasNonBlock)
                    idx++;
                if(hasBlock)
                    idxBlock++;

                // add symbols, normal and block, for source - using one color index for both
                SourceSymbol ss = new SourceSymbol(sd, sdb, sd.colorsIdx);
                hmSymbols.put(source, ss);

                // process all source features
                hmSF.keySet().forEach((feature) -> {
                    if(source.equals(definitions.cds.source) && feature.equals(definitions.cds.feature))
                        ss.hmCats.put(feature, new SymbolAssignment(source, feature, "CDS", symbolsDefList.get(Shape.CDS.ordinal()), new SymbolColors(Color.LIGHTGRAY, Color.DARKGRAY)));
                    else if(definitions.isDomain(source, feature)) {
                        // we allow multiple domain sources so let's try to reduce the chance
                        // of having the same colors when they are on the same gene protein display
                        // OK to just use a single DOMAIN symbol if you want to get rid of this
                        int dbidx = lstDomainSources.indexOf(source);
                        if(dbidx == -1) {
                            dbidx = lstDomainSources.size();
                            lstDomainSources.add(source);
                        }
                        dbidx = dbidx & 0x0003;
                        ss.hmCats.put(feature, new SymbolAssignment(source, feature, "", symbolsDefList.get(Shape.DOMAIN.ordinal() + dbidx), new SymbolColors(Color.LIGHTGREEN, Color.GRAY)));
                    }
                    else {
                        if(hmBF.containsKey(feature))
                            ss.hmCats.put(feature, new SymbolAssignment(source, feature, "", ss.symbolDefBlock, COLORS_LIST.get(ss.colorsIdx)));
                        else
                            ss.hmCats.put(feature, new SymbolAssignment(source, feature, "", ss.symbolDef, COLORS_LIST.get(ss.colorsIdx)));
                        if(++ss.colorsIdx >= COLORS_LIST.size())
                            ss.colorsIdx = 0;
                    }
                });
            }
        }
    }
    // get symbol assignment based on database and category (source/type)
    public SymbolAssignment getSymbolAssignment(String db, String cat, String caption, String Id) {
        SymbolAssignment symbol = null;
        if(hmSymbols.containsKey(db)) {
            SourceSymbol ss = hmSymbols.get(db);
            if(ss.hmCats.containsKey(cat)) {
                symbol = ss.hmCats.get(cat);
                if(symbol.caption.isEmpty())
                    symbol.caption = caption;
            }
        }
        if(symbol == null)
            symbol = new SymbolAssignment(db, cat, caption, symbolsDefList.get(Shape.NONE.ordinal()), new SymbolColors(Color.RED, Color.BLACK));
        else {
            // check if special subId request
            AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
            if(defs.isDomain(db, cat) && !Id.isEmpty()) {
                if(!symbol.hmSubs.containsKey(Id)) {
                    symbol.colorsIdx++;
                    symbol.hmSubs.put(Id, new SymbolSubAssignment(Id, COLORS_LIST.get(symbol.colorsIdx)));
                }
                SymbolAssignment nsa = new SymbolAssignment(symbol.source, symbol.feature, caption, symbol.symbolDef, symbol.hmSubs.get(Id).colors);
                symbol = nsa;
            }
        }
        
        // check if symbol already in active list
        boolean fnd = false;
        for(SymbolType st : symbolTypesList) {
            if(st.shape == symbol.symbolDef.shape && st.source.equals(db) && st.feature.equals(cat) && st.id.equals(Id)) {
                fnd = true;
                break;
            }
        }
        if(!fnd) {
            SymbolType symtype = new SymbolType(symbol.symbolDef.shape, db, cat, Id, symbol.colors);
            symbolTypesList.add(symtype);
        }
        return symbol;
    }
    public SymbolExtensionInfo draw(SymbolAssignment symbol, GraphicsContext gc, double x, double y, double ppb,
                            boolean upsideDown, SeqAlign.WithinRegion within,
                            ArrayList<SeqAlign.Position> tgexonsList, SeqAlign seqAlign,
                            AnnotationEntry entry, AnnotationEntry geneEntry) {
        SymbolExtensionInfo sei = null;
        if(symbol != null & symbol.symbolDef != null)  {
            if(isBlockSymbol(symbol.symbolDef)) {
                if(seqAlign != null) {
                    SymbolDef symdef = symbol.symbolDef;
                    SymbolColors colors = symbol.colors;
                    // block symbols are only assigned for proteins and transcripts not genomics
                    // so the within region code is not relevant since it only applies to genomic features
                    gc.setLineWidth(symdef.lineWidth);
                    gc.setFill(colors.fill);
                    gc.setStroke(colors.stroke);
                    sei = drawBlock(symdef, gc, x, y, ppb, tgexonsList, seqAlign, entry, geneEntry);
                }
                else
                    draw(symbol.symbolDef.shape, gc, x, y, symbol.wBlock, upsideDown, symbol.colors, within);
            }
            else
                draw(symbol.symbolDef.shape, gc, x, y, 0, upsideDown, symbol.colors, within);
        }
        return sei;
    }
    public void draw(Shape shape, GraphicsContext gc, double x, double y, double wBlock,
                            boolean upsideDown, SymbolColors colors, SeqAlign.WithinRegion within) {
        SymbolDef symdef = null;
        for(SymbolDef sd : symbolsDefList) {
            if(sd.shape == shape) {
                symdef = sd;
                break;
            }
        }
        if(symdef != null) {
            if(!within.equals(SeqAlign.WithinRegion.YES)) {
                if(within.equals(SeqAlign.WithinRegion.NO))
                    gc.setFill(Color.web("0xFF0000",0.5));
                else
                    gc.setFill(Color.web("0xFFA500",0.5));
                x++;
                double w = 8.0;
                double h = 9.0;
                gc.fillRect(x-(Math.abs(w-symdef.width)/2), y-(Math.abs(h-Math.max(symdef.value, symdef.height))/2), w, h);
            }
            gc.setLineWidth(symdef.lineWidth);
            gc.setFill(colors.fill);
            gc.setStroke(colors.stroke);
            switch(shape) {
                case STAR:
                    drawStar(symdef, gc, x, y, upsideDown, colors);
                    break;
                case ARROW:
                    drawArrow(symdef, gc, x, y, upsideDown, colors);
                    break;
                case ARROWDOWN:
                    drawArrow(symdef, gc, x, y, !upsideDown, colors);
                    break;                    
                case DIAMOND:
                    drawDiamond(symdef, gc, x, y, upsideDown, colors);
                    break;
                case DIAMONDDOWN:
                    drawDiamondDOWN(symdef, gc, x, y, upsideDown, colors);
                    break;
                case DROP:
                    drawDrop(symdef, gc, x, y, upsideDown, colors);
                    break;
                case DROPDOWN:
                    drawDrop(symdef, gc, x, y, !upsideDown, colors);
                    break;
                case OVAL:
                    drawOval(symdef, gc, x, y, upsideDown, colors);
                    break;
                case DOT:
                    drawDot(symdef, gc, x, y, upsideDown, colors);
                    break;
                case CDS:
                    gc.setFill(Color.LIGHTGRAY);
                    gc.setStroke(Color.DARKGRAY);
                    drawRectangle(symdef, gc, x, y, upsideDown, colors);
                    break;
                case ORF:
                    // only used for legend
                    gc.setFill(Color.BLACK);
                    gc.setStroke(Color.BLACK);
                    drawRectangle(symdef, gc, x, y + 3, upsideDown, colors);
                    break;
                case DOMAIN:
                case DOMAIN2:
                case DOMAIN3:
                case DOMAIN4:
                    drawRectangle(symdef, gc, x, y, upsideDown, colors);
                    break;
                case POINTEDRECT:
                    drawPointedRectangle(symdef, gc, x, y, upsideDown, colors);
                    break;
                case TRIANGLE:
                    drawTriangle(symdef, gc, x, y, upsideDown, colors);
                    break;
                case TRIANGLEDOWN:
                    drawTriangle(symdef, gc, x, y, !upsideDown, colors);
                    break;                    
                case BARREL:
                    drawBarrel(symdef, gc, x, y, upsideDown, colors);
                    break;
                case BLOCK1:
                case BLOCK2:
                case BLOCK3:
                case BLOCK4:
                case BLOCK5:
                case BLOCK6:
                    // only used for drawing legend or when alignment option is off
                    // alignment display is invoked from draw function above this one
                    // adjust when drawing legend, wBlock == 0, to center vertically
                    drawBlock(symdef, gc, x, (wBlock == 0)? y+2 : y, wBlock, upsideDown, colors);
                    break;
                case NONE:
                    drawNone(symdef, gc, x, y, upsideDown, colors);
                    break;
            }
        }
    }
    public boolean isSingleColumnLegend(GraphicsContext gc,Font font, Rectangle2D.Double rc) {
    	boolean singleColumn = false;
    	double min = -1;
    	double max = 0;
    	double sum = 0;
    	double avg, width;
        int cnt = symbolTypesList.size();
        if(cnt > 0) {
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFont(font);
            for(SymbolType symtype : symbolTypesList) {
                String caption = symtype.feature;
            	width = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(caption, gc.getFont());
            	if(width < min || min == -1)
            		min = width;
            	if(width > max)
            		max = width;
            	sum += width;
            }
            avg = sum/cnt;
            if(max > (rc.width / 2 - 50))
            	singleColumn = true;
        }
        return singleColumn;
    }
    // NOTE: May need to handle case where a legend caption is too long to fit - or just limit length...
    public Point2D.Double drawLegend(GraphicsContext gc, Color fillColor, Font font, Rectangle2D.Double rc,
                                     TextAlignment align, boolean recalc, boolean singleColumn, boolean viewTrans,
                                     boolean intronSymbol, boolean inexSymbol) {
        double x = rc.x;
        double y = rc.y;
        Point2D.Double pmax = new Point2D.Double(x,y);
        double xs = x + 5;
        double ys = y + 5;
        double xmax = x;
        double ymax = y;
        double entryHeight = 20;
        double sxmax;
        
        // draw background if size and placement already calculated
        if(!recalc) {
            gc.setStroke(Color.valueOf("#E0E0E0"));
            gc.strokeRoundRect(rc.x, rc.y, rc.width, rc.height, 5, 5);
            gc.setFill(Color.valueOf("#F8F8F8"));
            gc.fillRoundRect(rc.x, rc.y, rc.width, rc.height, 5, 5);
        }
        
        // use symbols definition list to keep symbol types grouped in legend
        int cnt = symbolTypesList.size();
        if(cnt > 0) {
            int colcnt = (cnt + 1) / 2;
            for(SymbolDef symdef: symbolsDefList) {
                for(SymbolType symtype : symbolTypesList) {
                    if(symtype.shape == symdef.shape) {
                        if(!viewTrans && symtype.shape.equals(Shape.CDS)) {
                            SymbolType symLegend = new SymbolType(Shape.ORF, symtype.source, "ORF", symtype.id, new SymbolColors(Color.BLACK, Color.BLACK));
                            sxmax = drawLegendSymbol(symLegend, gc, fillColor, font, xs, ys, !recalc);
                        //We dont want show polyA_Site in proteins or Structual_Information!!!
                        }else if((symtype.feature.equals(POLYA_FEATURE) || symtype.source.equals(STRUCTURAL_SOURCE)) && !viewTrans)
                            continue;
                        else
                            sxmax = drawLegendSymbol(symtype, gc, fillColor, font, xs, ys, !recalc);
                        if(sxmax > xmax)
                            xmax = sxmax;
                        if(singleColumn) {
                            ys += entryHeight;
                            if(ys > ymax)
                                ymax = ys;
                        }
                        else {
                            if(--colcnt == 0) {
                                ys += entryHeight;
                                if(ys > ymax)
                                    ymax = ys;
                                colcnt = 99;
                                xs = xmax + 30;
                                ys = y + 5;
                            }
                            else {
                                ys += entryHeight;
                                if(ys > ymax)
                                    ymax = ys;
                            }
                        }
                    }
                }                
            }      
        }
        if(intronSymbol || inexSymbol) {
            ys = ymax;
            xs = x + 5;
            if(intronSymbol) {
                sxmax = drawLegendIntronExonSymbol(gc, fillColor, font, xs, ys, true, !recalc);
                if(sxmax > xmax)
                    xmax = sxmax;
                ys += entryHeight;
            }
            if(inexSymbol) {
                sxmax = drawLegendIntronExonSymbol(gc, fillColor, font, xs, ys, false, !recalc);
                if(sxmax > xmax)
                    xmax = sxmax;
                ys += entryHeight;
            }
            ymax = ys;
        }
        pmax.x = xmax + 10;
        pmax.y = ymax;
        if(recalc) {
            double totalWidth = rc.width;
            rc.width =  pmax.x - x;
            rc.height = pmax.y - y;
            if(align == TextAlignment.CENTER)
                x += (totalWidth - rc.width)/2;
            rc.x = x;
            rc.y = y;
            pmax = drawLegend(gc, fillColor, font, rc, align, false, singleColumn, viewTrans, intronSymbol, inexSymbol);
        }
        return pmax;
    }
    private double drawLegendIntronExonSymbol(GraphicsContext gc, Color fillColor, Font font, double x, double y, boolean intron, boolean draw) {
        if(draw) {
            if(intron)
                gc.setFill(Color.web("0xFF0000",0.5));
            else
                gc.setFill(Color.web("0xFFA500",0.5));
            double w = 8.0;
            double h = 9.0;
            gc.fillRect(x+2-(w/2), y, w, h);
        }
        double symWidth = 15;
        double yOffText = 8;
        String caption = intron? "Annotation (symbol within) located in intron region" : "Annotation (symbol within) overlaps intron and exon regions";
        double wtxt = drawLegendText(gc, fillColor, font, caption, x + symWidth, y + yOffText, draw);
        return x + symWidth + wtxt;
    }
    private double drawLegendText(GraphicsContext gc, Color fillColor, Font font, String caption, 
                                  double x, double y, boolean draw) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(fillColor);
        gc.setFont(font);
        if(draw)
            gc.fillText(caption, x, y);
        double width = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(caption, gc.getFont());
        return width;
    }
    private double drawLegendSymbol(SymbolType symtype, GraphicsContext gc, Color fillColor, Font font, 
                                    double x, double y, boolean draw) {
        double symWidth = 15;
        double yOffText = 8;
        boolean upsideDown = (symtype.shape == Shape.ARROW);
        if(draw) {
            double ydraw = y;
            // being picky - center the dot for legend display - looks awkward otherwise
            if(symtype.shape == Shape.DOT) {
                SymbolDef symdef = null;
                for(SymbolDef sd : symbolsDefList) {
                    if(sd.shape == Shape.DOT) {
                        symdef = sd;
                        break;
                    }
                }
                if(symdef != null) {
                    double adj = (9 - symdef.height) / 2;
                    ydraw += adj;
                }
            }
            draw(symtype.shape, gc, x, ydraw, 0, upsideDown, symtype.colors, SeqAlign.WithinRegion.YES);
        }
        String caption = symtype.source + " : " + symtype.feature;
        if(!symtype.id.isEmpty())
            caption += " - " + symtype.id;
        double wtxt = drawLegendText(gc, fillColor, font, caption, x + symWidth, y + yOffText, draw);
        return x + symWidth + wtxt;
    }
    public void drawGenomicSymbol(GraphicsContext gc, double x, double y, double width) {
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.BLACK);
        double height = viewer.adjustValue(CDSblockHeight);
        gc.fillRect(x, y, width, height);
        gc.strokeRect(x, y, width, height);
    }
    public void drawCDSblock(GraphicsContext gc, double x, double y, double width, boolean cat) {
        if(cat){
            gc.setFill(Color.LIGHTCORAL);
            gc.setStroke(Color.LIGHTCORAL);
        }
        else{
            gc.setFill(Color.LIGHTGRAY);
            gc.setStroke(Color.DARKGRAY);
        }
        double lw = gc.getLineWidth();
        gc.setLineWidth(0.25);
        double height = viewer.adjustValue(CDSblockHeight);
        gc.fillRect(x, y, width, height);
        gc.strokeRect(x, y, width, height);
        gc.setLineWidth(lw);
    }
    public void drawUTRblock(GraphicsContext gc, double x, double y, double width) {
        gc.setFill(Color.LIGHTGREEN);
        gc.setStroke(Color.DARKGRAY);
        double lw = gc.getLineWidth();
        gc.setLineWidth(0.25);
        double height = viewer.adjustValue(CDSblockHeight);
        gc.fillRect(x, y, width, height);
        gc.strokeRect(x, y, width, height);
        gc.setLineWidth(lw);
    }
    public void drawRNAblock(GraphicsContext gc, double x, double y, double width) {
        gc.setFill(Color.BLACK);
        double height = viewer.adjustValue(RNAblockHeight);
        gc.fillRect(x, y, width, height);
    }
    public void drawIntronBlock(GraphicsContext gc, double x, double y, double width) {
        gc.setFill(Color.BLACK);
        double height = viewer.adjustValue(IntronBlockHeight);
        gc.fillRect(x, y, width, height);
    }
    // draw splice junction
    // drawType: 1 - donor, 2 - acceptor, 3 - both
    public void drawSpliceJunction(GraphicsContext gc, double x, double y, double height, boolean novel, int drawType) {
        Color color;
        if(novel)
            color = Color.web("0xFF0000", 1);
        else
            color = Color.web("0x808080", 1);
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(0.5);
        gc.strokeLine(x, y-height/2, x, y+height/2-4.5);

        double w = 4;
        double h = 3;
        double[] xpts = new double[3];
        double[] ypts = new double[3];
        xpts[0] = x;
        ypts[0] = y - height/2;
        switch(drawType) {
            case 1:
                w = 5;
                xpts[1] = x - w/2;
                ypts[1] = y - height/2 - h;
                xpts[2] = x;
                ypts[2] = y - height/2 - h;
                break;
            case 2:
                w = 5;
                xpts[1] = x;
                ypts[1] = y - height/2 - h;
                xpts[2] = x + w/2;
                ypts[2] = y - height/2 - h;
                break;
            default:
                xpts[1] = x - w/2;
                ypts[1] = y - height/2 - h;
                xpts[2] = x + w/2;
                ypts[2] = y - height/2 - h;
                break;
        }
        gc.fillPolygon(xpts, ypts, 3);
        gc.strokePolygon(xpts, ypts, 3);
    }
    public void drawDashedLine(GraphicsContext gc, double x1, double y1, double x2, Color color) {
        double dw = 2;
        double offset;
        gc.beginPath();
        if(color != null)
            gc.setStroke(color);
        while((x1+dw) <= x2) {
            gc.moveTo(x1, y1);
            offset = Math.min(dw, x2-x1);
            x1 += offset;
            gc.lineTo(x1, y1);
            x1 += dw;
        }
        gc.closePath();
        gc.stroke();
    }
    public void drawDottedLine(GraphicsContext gc, double x1, double y1, double x2, Color color) {
        double dw = 2;
        double offset;
        gc.beginPath();
        if(color != null)
            gc.setStroke(color);
        x1 += dw;
        while((x1+dw) <= x2) {
            gc.fillOval(viewer.adjustX(x1), y1, 1, 1);
            offset = Math.min(dw, x2-x1);
            x1 += offset;
            x1 += dw;
        }
        gc.closePath();
        gc.stroke();
    }
    public void drawVerticalLine(GraphicsContext gc, double x1, double y1, double y2) {
        gc.beginPath();
        gc.moveTo(x1, y1);
        gc.lineTo(x1, y2);
        gc.closePath();
        gc.stroke();
    }
    public void drawLine(GraphicsContext gc, double x1, double x2, double y1, double y2) {
        gc.beginPath();
        gc.moveTo(x1, y1);
        gc.lineTo(x2, y2);
        gc.closePath();
        gc.stroke();
    }
    public SymbolExtensionInfo drawSymbolExtension(GraphicsContext gc, double xi, double y, double height, double ppb, Color color,
                                    ArrayList<SeqAlign.Position> tgexonsList, SeqAlign seqAlign,
                                    AnnotationEntry entry, AnnotationEntry geneEntry) {
        ArrayList<SeqAlign.SegmentDisplay> segList = seqAlign.getCdsSegments(tgexonsList, tgexonsList);
        double mult = viewer.getViewType(viewer.getClass().toString()).equals(ViewAnnotation.Views.PROTEIN)? 3.0 : 1.0;
        ArrayList<SeqAlign.Position> posList = new ArrayList<>();
        for(SeqAlign.SegmentDisplay sd : segList) {
            posList.add(new SeqAlign.Position((int)Math.floor((seqAlign.getXlatedPos(sd.start)+mult-1.0)/mult), 
                                     (int)Math.floor((seqAlign.getXlatedPos(sd.end)+mult-1.0)/mult)));
        }
        int endseg;
        double x, width;
        int strseg = (int)Math.floor(seqAlign.getAlignedPos(entry.strpos * mult, tgexonsList)/mult);
        int endpos = (int)Math.floor(seqAlign.getAlignedPos(entry.endpos * mult, tgexonsList)/mult);
        int totalWidth = 0;
        boolean done = false;
        int segcnt = 0;
        SymbolExtensionInfo sei = new SymbolExtensionInfo(0, 0, 0, 0);
        for(SeqAlign.Position pos : posList) {
            if(!done && strseg >= pos.start && strseg <= pos.end) {
                done = true;
                endseg = endpos;
                if(endpos > pos.end) {
                    endseg = pos.end;
                    done = false;
                }
                totalWidth += (endseg - strseg + 1);
                width = endseg - strseg + 1;
                x = (xi + (strseg - geneEntry.strpos + 1) * ppb);
                if(sei.minX == 0 || sei.minX > x)
                    sei.minX = x;
                width *= ppb;
                double x2 = x + width;
                if(sei.maxX == 0 || sei.maxX < x2)
                    sei.maxX = x2;
                double aw = width;
                double ys = viewer.adjustY(y);
                gc.setFill(Color.color(0.9, 0.9, 0.9));
                gc.fillRect(x, ys+1, aw, height-2);
                ys += height/2;
                sei.y = ys;
                gc.setStroke(Color.GRAY);
                gc.setLineWidth(0.5);
                drawDashedLine(gc, x, ys, x2, Color.GRAY);
                drawVerticalLine(gc, x, ys-2, ys+2);
                drawVerticalLine(gc, x2, ys-2, ys+2);
                
                // set for next segment if spanning across gap
                if(!done)
                    strseg = (int)Math.floor(seqAlign.getAlignedPos((entry.strpos + totalWidth) * mult, tgexonsList)/mult);
                segcnt++;
            }
        }
        if(sei.y != 0) {
            sei.segments = segcnt;
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(0.5);
            drawDashedLine(gc, sei.minX, sei.y, sei.maxX, Color.GRAY);
        }
        return sei;
    }
    public void drawSymbolExtension(GraphicsContext gc, double x, double y, double w, double h, Color color) {
        double x2 = x + w;
        double aw = w;
        y = viewer.adjustY(y);
        gc.setFill(Color.color(0.9, 0.9, 0.9));
        gc.fillRect(x, y+1, aw, h-2);
        y += h/2;
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(0.5);
        drawDashedLine(gc, x, y, x2, Color.GRAY);
        drawVerticalLine(gc, x, y-2, y+2);
        drawVerticalLine(gc, x2, y-2, y+2);
    }
    
    //
    // Internal Functions
    //
    
    private void drawStar(SymbolDef symdef, GraphicsContext gc, double x, double y,
                                 boolean upsideDown, SymbolColors colors) {
        double w = symdef.width;
        double h = symdef.height;
        gc.setLineWidth(symdef.lineWidth);
        gc.setFill(colors.fill);
        gc.setStroke(colors.stroke);
        double r = w/2;
        double cx = x + w/2;
        double cy = y + h/2;
        double delta = Math.PI / 2;
        double[] xpts = new double[5];
        double[] ypts = new double[5];
        double[] xinpts = new double[5];
        double[] yinpts = new double[5];
        for(int idx = 0; idx < 5; idx++) {
            double t = idx * 4 * Math.PI / 5 + (Math.PI / 5);
            xpts[idx] = cx + r * Math.cos(t + delta);
            ypts[idx] = cy + r * Math.sin(t + delta);
            xinpts[idx] = cx + (r - 1) * Math.cos(t + delta);
            yinpts[idx] = cy + (r - 1) * Math.sin(t + delta);
        }
        gc.strokePolygon(xpts, ypts, 5);
        gc.fillPolygon(xinpts, yinpts, 5);
    }
    private void drawArrow(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                  boolean upsideDown, SymbolColors colors) {
        double w = symdef.width;
        double h = symdef.height;
        double top = symdef.value;
        double[] xpts = new double[4];
        double[] ypts = new double[4];
        gc.setStroke(colors.fill);
        x = viewer.adjustX(x);
        if(upsideDown) {
            xpts[0] = x;
            ypts[0] = y + top;
            xpts[1] = x + w/2;
            ypts[1] = y;
            xpts[2] = x + w;
            ypts[2] = y + top;
        }
        else {
            xpts[0] = x;
            ypts[0] = y + h - top;
            xpts[1] = x + w/2;
            ypts[1] = y + h;
            xpts[2] = x + w;
            ypts[2] = y + h - top;            
        }
        gc.strokePolyline(xpts, ypts, 3);
        gc.strokeLine(x + w/2, y, x + w/2, y + h);
    }
    private void drawDiamond(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                    boolean upsideDown, SymbolColors colors) {
        double w = symdef.width;
        double h = symdef.height;
        double[] xpts = new double[4];
        double[] ypts = new double[4];
        xpts[0] = x;
        ypts[0] = y + h/2;
        xpts[1] = x + w/2;
        ypts[1] = y + h;
        xpts[2] = x + w;
        ypts[2] = y + h/2;
        xpts[3] = x + w/2;
        ypts[3] = y;
        gc.fillPolygon(xpts, ypts, 4);
        gc.strokePolygon(xpts, ypts, 4);
    }
    private void drawDiamondDOWN(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                    boolean upsideDown, SymbolColors colors) {
        double w = symdef.width;
        double h = symdef.height;
        double[] xpts = new double[4];
        double[] ypts = new double[4];
        xpts[0] = x;
        ypts[0] = y + w/2;
        xpts[1] = x + h/2;
        ypts[1] = y + w;
        xpts[2] = x + h;
        ypts[2] = y + w/2;
        xpts[3] = x + h/2;
        ypts[3] = y;
        gc.fillPolygon(xpts, ypts, 4);
        gc.strokePolygon(xpts, ypts, 4);
    }
    private void drawTriangle(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                    boolean upsideDown, SymbolColors colors) {
        double w = symdef.width;
        double h = symdef.height;
        double[] xpts = new double[3];
        double[] ypts = new double[3];
        if(upsideDown) {
            xpts[0] = x;
            ypts[0] = y + h;
            xpts[1] = x + w/2;
            ypts[1] = y;
            xpts[2] = x + w;
            ypts[2] = y + h;
        }
        else {
            xpts[0] = x;
            ypts[0] = y;
            xpts[1] = x + w/2;
            ypts[1] = y + h;
            xpts[2] = x + w;
            ypts[2] = y;
        }
        gc.fillPolygon(xpts, ypts, 3);
        gc.strokePolygon(xpts, ypts, 3);
    }
    private void drawDrop(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                 boolean upsideDown, SymbolColors colors) {
        double w = symdef.width;
        double h = symdef.height;
        double value = symdef.value;
        double[] xpts = new double[3];
        double[] ypts = new double[3];
        if(upsideDown) {
            gc.fillArc(x, y+value-h, w, h, 180, 180, ArcType.ROUND);
            gc.strokeArc(x, y+value-h, w, h, 180, 180, ArcType.OPEN);
            xpts[0] = x;
            ypts[0] = y + value - (h/2 - 0.5);
            xpts[1] = x + w/2;
            ypts[1] = y;
            xpts[2] = x + w;
            ypts[2] = y + value - (h/2 - 0.5);
            gc.fillPolygon(xpts, ypts, 3);
            gc.strokeLine(x, y+value-h+2, x+w/2, y);
            gc.strokeLine(x+w, y+value-h+2, x+w/2, y);
        }
        else {
            gc.fillArc(x, y, w, h, 0, 180, ArcType.ROUND);
            gc.strokeArc(x, y, w, h, 0, 180, ArcType.OPEN);
            xpts[0] = x;
            ypts[0] = y + h/2 - 0.5;
            xpts[1] = x + w/2;
            ypts[1] = y + value;
            xpts[2] = x + w;
            ypts[2] = y + h/2 - 0.5;
            gc.fillPolygon(xpts, ypts, 3);
            gc.strokeLine(x, y+h/2+0.25, x+w/2, y+value);
            gc.strokeLine(x+w, y+h/2+0.25, x+w/2, y+value);
        }
    }
    private void drawOval(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                 boolean upsideDown, SymbolColors colors) {
        double w = 4; //symdef.width;
        double h = 7; //symdef.height;
        gc.fillOval(x, y, w, h);
        gc.strokeOval(x, y, w, h);
    }
    private void drawDot(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                 boolean upsideDown, SymbolColors colors) {
        double w = symdef.width;
        double h = symdef.height;
        gc.fillOval(x, y, w, h);
        gc.strokeOval(x, y, w, h);
    }
    private void drawRectangle(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                      boolean upsideDown, SymbolColors colors) {
        double w = viewer.adjustValue(symdef.width);
        double h = viewer.adjustValue(symdef.height);
        x = viewer.adjustX(x);
        y = viewer.adjustY(y);
        gc.fillRect(x, y, w, h);
        gc.strokeRect(x, y, w, h);
    }
    private void drawBlock(SymbolDef symdef, GraphicsContext gc, double x, double y, double wBlock,
                                      boolean upsideDown, SymbolColors colors) {
        double w = (wBlock == 0)? symdef.width : wBlock;
        double h = symdef.height;
        int blknum = (int)symdef.value;
        double x2 = viewer.adjustX(x+w);
        x = viewer.adjustX(x);
        double aw = x2 - x;
        double ya = y + h/2;
        double ym = viewer.adjustY(ya-h/2);
        double yp = viewer.adjustY(ya+h/2);
        h = yp - ym;
        gc.fillRect(x, viewer.adjustY(y), aw, h);
        gc.strokeRect(x, viewer.adjustY(y), aw, h);
        y = ya;
        double lw = 5.0;
        double ln = aw / lw;
        int n = (int)Math.floor(ln - 1);
        if(n == 0)
            n = 1;
        lw = aw/(n + 1);
        double lx = x + lw;
        Paint fpaint = gc.getFill();
        gc.setFill(Color.web("0x404040"));
        Paint spaint = gc.getStroke();
        gc.setStroke(Color.web("0x404040"));
        for(int i = 0; i < n; i++) {
            switch(blknum) {
                case 1:
                    // no lines at all, just fill
                    break;
                case 2:
                    drawVerticalLine(gc, viewer.adjustX(lx), ym, yp);
                    break;
                case 3:
                    drawLine(gc, viewer.adjustX(lx-2), viewer.adjustX(lx+2), ym, yp);
                    break;
                case 4:
                    drawLine(gc, viewer.adjustX(lx+2), viewer.adjustX(lx-2), ym, yp);
                    break;
                case 5:
                    drawLine(gc, viewer.adjustX(lx-2), viewer.adjustX(lx+2), ym, yp);
                    drawLine(gc, viewer.adjustX(lx+2), viewer.adjustX(lx-2), ym, yp);
                    break;
                case 6:
                    drawDottedLine(gc, x, viewer.adjustY(y), x2, null);
                    break;
            }
            lx += lw;
        }
        gc.setFill(fpaint);
        gc.setStroke(spaint);
    }
    private SymbolExtensionInfo drawBlock(SymbolDef symdef, GraphicsContext gc, double xi, double y, double ppb,
                                    ArrayList<SeqAlign.Position> tgexonsList, SeqAlign seqAlign,
                                    AnnotationEntry entry, AnnotationEntry geneEntry) {
        ArrayList<SeqAlign.SegmentDisplay> segList = seqAlign.getCdsSegments(tgexonsList, tgexonsList);
        ArrayList<SeqAlign.Position> posList = new ArrayList<>();
        for(SeqAlign.SegmentDisplay sd : segList) {
            posList.add(new SeqAlign.Position((int)Math.floor((seqAlign.getXlatedPos(sd.start)+2)/3), 
                                     (int)Math.floor((seqAlign.getXlatedPos(sd.end)+2)/3)));
        }
        ArrayList<double[]> lstBlocks = new ArrayList<>();

        if(entry.source.equals("PAR-clip"))
            System.out.println("strseg es demasiado grande comparado con utrsite");

        if(entry.source.equals("UTRsite"))
            System.out.println("Ver porqué todo 0s");

        int endseg;
        double x, width;
        double mult = viewer.getViewType(viewer.getClass().toString()).equals(ViewAnnotation.Views.PROTEIN)? 3.0 : 1.0;
        int strseg = (int)Math.floor(seqAlign.getAlignedPos(entry.strpos * mult, tgexonsList)/mult);
        int endpos = (int)Math.floor(seqAlign.getAlignedPos(entry.endpos * mult, tgexonsList)/mult);
        ViewAnnotation.AnnotationEntry segEntry;
        int totalWidth = 0;
        boolean done = false;
        SymbolExtensionInfo sei = new SymbolExtensionInfo(0, 0, 0, 0);
        for(SeqAlign.Position pos : posList) {
            if((!done && strseg >= pos.start && strseg <= pos.end)){ // || entry.source.equals("PAR-clip")) {
                done = true;
                endseg = endpos;
                if(endpos > pos.end) {
                    endseg = pos.end;
                    done = false;
                }
                totalWidth += (endseg - strseg + 1);
                width = endseg - strseg + 1;
                x = viewer.adjustX(xi + (strseg - geneEntry.strpos + 1) * ppb);
                if(sei.minX == 0 || sei.minX > x)
                    sei.minX = x;
                width *= ppb;
                double x2 = x + width;
                if(sei.maxX == 0 || sei.maxX < x2)
                    sei.maxX = x2;
                double ys = viewer.adjustY(y);
                ys += symdef.height/2;
                sei.y = ys;
                double[] block = new double[3];
                block[0] = x;
                block[1] = y;
                block[2] = width;
                lstBlocks.add(block);
                
                // set for next segment if spanning across gap
                if(!done)
                    strseg = (int)Math.floor(seqAlign.getAlignedPos((entry.strpos + totalWidth) * mult, tgexonsList)/mult);
            }else{ //color blocks with other size
                done = true;
                totalWidth += (entry.dspendpos - entry.dspstrpos + 1);
                width = entry.dspendpos - entry.dspstrpos + 1;
                x = viewer.adjustX(xi + (entry.dspstrpos - geneEntry.strpos + 1) * ppb);
                if(sei.minX == 0 || sei.minX > x)
                    sei.minX = x;
                width *= ppb;
                double x2 = x + width;
                if(sei.maxX == 0 || sei.maxX < x2)
                    sei.maxX = x2;
                double ys = viewer.adjustY(y);
                ys += symdef.height/2;
                sei.y = ys;
                double[] block = new double[3];
                block[0] = x;
                block[1] = y;
                block[2] = width;
                lstBlocks.add(block);
            }
        }
        
        // check if split across introns - draw dashed lines if so
        if(lstBlocks.size() > 1) {
            if(sei.y != 0) {
                sei.segments = lstBlocks.size();
                Paint paint = gc.getStroke();
                double lw = gc.getLineWidth();
                gc.setStroke(Color.GRAY);
                gc.setLineWidth(0.5);
                drawDashedLine(gc, sei.minX, sei.y, sei.maxX, Color.GRAY);
                gc.setStroke(paint);
                gc.setLineWidth(lw);
            }
        }
        for(double[] block : lstBlocks)
            drawBlock(symdef, gc, block[0], block[1], block[2], false, null);
        return sei;
    }
    private void drawBarrel(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                      boolean upsideDown, SymbolColors colors) {
        double w = viewer.adjustValue(symdef.width);
        double h = viewer.adjustValue(symdef.height);
        x = viewer.adjustX(x);
        y = viewer.adjustY(y);
        double[] xpts = new double[6];
        double[] ypts = new double[6];
        xpts[0] = x + 1;
        ypts[0] = y;
        xpts[1] = x + w - 1;
        ypts[1] = y;
        xpts[2] = x + w;
        ypts[2] = y + h/2;
        xpts[3] = x + w - 1;
        ypts[3] = y + h;
        xpts[4] = x + 1;
        ypts[4] = y + h;
        xpts[5] = x;
        ypts[5] = y + h/2;
        gc.fillPolygon(xpts, ypts, 6);
        gc.strokePolygon(xpts, ypts, 6);
        //gc.fillRect(x, y, w, h);
        //gc.strokeRect(x, y, w, h);
        y = y + symdef.height/2;
        gc.strokeLine(x, y, x+w, y);
    }
    private void drawPointedRectangle(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                             boolean upsideDown, SymbolColors colors) {
         x = viewer.adjustX(x);
        double w = viewer.adjustValue(symdef.width);
        double h = symdef.height;
        double value = symdef.value;
        double[] xpts = new double[6];
        double[] ypts = new double[6];
        xpts[0] = x;
        ypts[0] = y + value;
        xpts[1] = x + w/2;
        ypts[1] = y;
        xpts[2] = x + w;
        ypts[2] = y + value;
        xpts[3] = x + w;
        ypts[3] = y + h - value;
        xpts[4] = x + w/2;
        ypts[4] = y + h;
        xpts[5] = x;
        ypts[5] = y + h - value;
        gc.fillPolygon(xpts, ypts, 6);
        gc.strokePolygon(xpts, ypts, 6);
    }
    private void drawNone(SymbolDef symdef, GraphicsContext gc, double x, double y, 
                                 boolean upsideDown, SymbolColors colors) {
        gc.setFill(Color.RED);
        double w = symdef.width;
        double h = symdef.height;
        double value = symdef.value;
        if(upsideDown) {
            gc.fillOval(x, y, w, w);
            gc.strokeOval(x, y, w, w);
            gc.fillRoundRect(x, y+w+2, w, h, 2, 2);
            gc.strokeRoundRect(x, y+w+2, w, h, 2, 2);
        }
        else {
            gc.fillRoundRect(x, y, w, h, 2, 2);
            gc.strokeRoundRect(x, y, w, h, 2, 2);
            gc.fillOval(x, y+value-w, w, w);
            gc.strokeOval(x, y+value-w, w, w);
        }
    }

    //
    // Data Classes
    //
    
    static public class SymbolDef {
        public Shape shape;
        public double width;
        public double height;
        public double value;
        public double lineWidth;
        public int colorsIdx;

        public SymbolDef(Shape shape, double width, double height, double value, double lineWidth, int colorsIdx) {
            this.shape = shape;
            this.width = width;
            this.height = height;
            this.value = value;
            this.lineWidth = lineWidth;
            this.colorsIdx = colorsIdx;
        }
    }
    static public class SymbolColors {
        public Color fill;
        public Color stroke;
        public SymbolColors(Color fill, Color stroke) {
            this.fill = fill;
            this.stroke = stroke;
        }
    }
    public class SymbolType {
        public Shape shape;
        public String source;
        public String feature;
        public String id;
        public SymbolColors colors;
        
        // Note: type will be set properly later by AnnotationViewer
        public SymbolType(Shape shape, String source, String feature, String id, SymbolColors colors) {
            this.shape = shape;
            this.source = source;
            this.feature = feature;
            this.id = id;
            this.colors = colors;
        }
    }
    public class SourceSymbol {
        public SymbolDef symbolDef;
        public SymbolDef symbolDefBlock;
        public int colorsIdx;
        HashMap<String, SymbolAssignment> hmCats;
        
        public SourceSymbol(SymbolDef symbolDef, SymbolDef symbolDefBlock, int colorsIdx) {
            this.symbolDef = symbolDef;
            this.symbolDefBlock = symbolDefBlock;
            this.colorsIdx = colorsIdx;
            hmCats = new HashMap<>();
        }
    }
    public class SymbolSubAssignment {
        public String Id;
        public SymbolColors colors;
        public SymbolSubAssignment(String Id, SymbolColors colors) {
            this.Id = Id;
            this.colors = colors;
        }
    }
    public class SymbolAssignment {
        public String source;
        public String feature;
        public String caption;
        public SymbolDef symbolDef;
        public SymbolColors colors;
        int colorsIdx;
        double wBlock;
        public HashMap<String, SymbolSubAssignment> hmSubs;
        
        public SymbolAssignment(String source, String feature, String caption, SymbolDef symbolDef, SymbolColors colors) {
            this.source = source;
            this.feature = feature;
            this.caption = caption;
            this.symbolDef = symbolDef;
            this.colors = colors;
            this.colorsIdx = symbolDef.colorsIdx;
            this.wBlock = 0;
            this.hmSubs = new HashMap<>();
        }
    }
    public class SymbolExtensionInfo {
        public double minX;
        public double maxX;
        public double y;
        public int segments;
        public SymbolExtensionInfo(double minX, double maxX, double y, int segments) {
            this.minX = minX;
            this.maxX = maxX;
            this.y = y;
            this.segments = segments;
        }
    }
}
