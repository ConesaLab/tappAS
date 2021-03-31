/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tappas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import tappas.DataAnnotation.AnnotFeature;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static tappas.DataAnnotation.STRUCTURAL_SOURCE;


/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

// add a sort menu selection - by trans len, trans name alpha, cds len, 5utr len, 3utr len?
// add a show - coding/noncoding, expressed, exp filtered, select specific ones, all (annotated) - display showing...
// add separator to option menu - have single menu for trans/protein?


public class ViewTranscript extends ViewAnnotation {
    public ViewTranscript(Project project) {
        super(project);
    }

    public List<String> getTranscriptDisplayOrder() {
        return transcriptDisplayOrder;
    }
    public String getGeneTitle() {
        return geneEntry.id;
    }
    public AnnotationEntry getGeneEntry() {
        return geneEntry;
    }
    
    // Initialization
    @Override
    public boolean initialize(SubTabBase subTabBase, Canvas canvasViewer, ScrollPane canvasScrollPane, String filterCaption) {
        return super.initialize(subTabBase, canvasViewer, canvasScrollPane, filterCaption);
    }
    
    @Override
    public void handleMouseClickAction(MouseEvent event) {
        if(event.getClickCount() == 2) {
            HashMap<String, Object> hm = new HashMap<>();
            if(app.ctlr.fxdc.isShiftKeyDown())
                hm.put("zoomOut", TabGeneDataViz.Views.TRANS);
            else
                hm.put("zoomIn", TabGeneDataViz.Views.TRANS);
            subTabBase.processRequest(hm);
        }
        if(tipEntry != null && event.getButton() == MouseButton.PRIMARY) {
            String url = "";
            AnnotFeature dbcatid = project.data.getRsvdAnnotFeature(tipEntry.source, tipEntry.feature);
            String species = project.data.getGenus() + "_" + project.data.getSpecies();
            DataApp.RefType rt = project.data.getRefType();
            if(species.equals("Homo_sapiens") || species.equals("Mus_musculus")) {
                switch(dbcatid) {
                    case GENE:
                        if(rt.equals(DataApp.RefType.Ensembl))
                            url = "http://www.ensembl.org/" + species + "/Gene/Summary?g=" + tipEntry.id;
                        else { //if(rt.equals(DataApp.RefType.Demo)) { // chg to MMDemo to allow other demos?!
                            String id = app.data.getNCBIGeneId(species, tipEntry.id);
                            if(!id.isEmpty())
                                url = "https://www.ncbi.nlm.nih.gov/gene/" + id;
                            else
                                app.ctls.alertInformation("Gene Online Reference", "Unable to determine what online references to use for this gene.");
                        }
                        break;
                    case TRANSCRIPT:
                        if(rt.equals(DataApp.RefType.Ensembl))
                            url = "http://www.ensembl.org/" + species + "/Transcript/Summary?t=" + tipEntry.id;
                        else { //if(rt.equals(DataApp.RefType.Demo)) { // chg to MMDemo to allow other demos?!
                            // our demo uses the PacBio transcript as the Id
                            String id = tipEntry.id;
                            if(rt.equals(DataApp.RefType.Demo))
                                id = tipEntry.featureId;
                            
                            if(id.isEmpty())
                                app.ctls.alertInformation("Transcript Online Reference", "There are no online references for this transcript.");
                            else {
                                if(id.startsWith("ENSMUST"))
                                    url = "http://www.ensembl.org/" + species + "/Gene/Summary?g=" + id;
                                else if(id.startsWith("NM_") || id.startsWith("NR_") || id.startsWith("XM_") || id.startsWith("XR_"))
                                    url = "https://www.ncbi.nlm.nih.gov/nuccore/" + id;
                                else
                                    app.ctls.alertInformation("Transcript Online Reference", "Unable to determine what online references to use for this transcript.");
                            }
                        }
                        break;
                        
                    // Note: proteins are handled in the ViewProtein class
                }
            }
            if(!url.isEmpty())
                Tappas.getHost().showDocument(url);

        }
    }
    @Override
    protected void draw(GraphicsContext gc, DrawArgs args) {
        ArrayList<AnnotationEntry> cdsList = new ArrayList<>();
        //ArrayList<AnnotationEntry> emptyList = new ArrayList<>();
        ArrayList<SeqAlign.Position> genoexonsList = new ArrayList<>();
        ArrayList<SpliceJuncData> genoSpliceJuncList = new ArrayList<>();
        ArrayList<SeqAlign.Position> spliceJuncList = new ArrayList<>();
        ArrayList<SeqAlign.Position> tgexonsList = new ArrayList<>();
        ArrayList<SeqAlign.Position> tgcdsList = new ArrayList<>();
        AnnotationEntry align5utr = null;
        AnnotationEntry align3utr = null;
        AnnotationEntry seqEntry = null;
        
        //System.out.println("draw at " + args.prefs.scale + ", " + args.redrawCount + ", align: " + args.prefs.alignment);
        gc.setFill(Color.valueOf("white"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double opacity = limitExceeded? 0.10 : 1.0;
        gc.setFill(Color.web(limitExceeded? "darkorange" : ((args.prefs.hmNotFeatureIDs.isEmpty() && !args.prefs.showVaryingOnly)? "white" : "white"), opacity));
        if(annotationList.isEmpty() || geneEntry == null) {
            double cwo = canvas.getWidth();
            double cyo = canvas.getHeight();
            canvas.setWidth(canvas.getWidth() * scale);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setGlobalAlpha(0.75);
            gc.setFill(Color.LIGHTGRAY);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("No Transcript Information Available", cwo/2, cyo/scale/2);
            gc.setGlobalAlpha(1.0);
            return;
        }
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // note that the first time this function is called, redrawCount = 1,
        // the canvas width has been set to ((canvasPane.getWidth() - 2) / scale)
        // this allows the proper expansion of the width when scaled
        // otherwise you always end up having (width * scale) and horizontal scroll bar
        double ch = canvas.getHeight();
        double chs = ch / scale;
        double cw = canvas.getWidth();
        double cws = cw / scale;
        if(args.redrawCount > 1) {
            ch = chs;
            cw = cws;
        }
            
        // initially allow for one level of symbols on top and one on bottom
        // if there is no room, add levels on bottom
        ArrayList<String> symbolsList = new ArrayList<>();
        ArrayList<ArrayList<UsedPoints>> seqList = new ArrayList<>();
        seqList.add(new ArrayList<>());   // top, 1st level
        seqList.add(new ArrayList<>());   // bottom, 1st level

        // display title
        tipsList.clear();
        double ytitle = 20;
        double ysubtitle = 40;
        gc.setFont(titleFont);
        gc.setFill(titleColor);
        gc.setTextAlign(TextAlignment.CENTER);
        // set gene display title - name plus description if available
        String title = geneEntry.id;
        if(!geneEntry.attrs.isEmpty()) {
            HashMap<String, String> attrs = DataAnnotation.getAttributes(geneEntry.attrs);
            if(attrs.containsKey(project.data.getAnnotationFileDefs().attrDesc)) {
                String desc = attrs.get(project.data.getAnnotationFileDefs().attrDesc);
                if(!desc.isEmpty())
                    title += " - " + desc.substring(0,1).toUpperCase() + desc.substring(1);
                if(title.length() > maxTitleLength)
                    title = title.substring(0, maxTitleLength) + "...";
            }
        }
        gc.fillText(title, (cw/2), ytitle);
        double width = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(title, gc.getFont());
        tipsList.add(new ToolTipPoints(geneEntry, (cw/2 - width/2), ytitle - 10, width, 16,
                            String.format("%s\n", geneEntry.tooltip), true));

        // display subtitle
        gc.setFont(subFont);
        gc.setFill(subColor);
        gc.fillText("Transcripts View - " + (args.prefs.alignment? "Aligned" : "Unaligned"), (cw/2), ysubtitle);
        //gc.fillText("Transcripts View - " + (args.prefs.alignment? "Aligned" : "Unaligned") + (filterCaption.isEmpty()? "" : (" for " + filterCaption)), (cw/2), ysubtitle);

        double yoffset = ysubtitle + 20;
        if(args.prefs.ruler)
            yoffset += 30;
        double xoffset = 12;
        double rightPadding = 60;
        double leftPadding = 60;
        double htop = 20;
        double hbottom = 17;
        double seqHeight = 40;
        double yRegion = yoffset - 20;
        double ymain = yoffset + htop;
        double wRegion = (cw - 2 * xoffset - rightPadding - leftPadding);
        double bases = geneEntry.endpos - geneEntry.strpos + 1;
        if(bases <= 0)
            bases = 1;
        SeqAlign.Position cdspos = new SeqAlign.Position(0,0);
        SeqAlign.Position utr5pos = new SeqAlign.Position(0,0);
        SeqAlign.Position utr3pos = new SeqAlign.Position(0,0);
        double dspsizeX = 0;
        double dspsizeY = 0;
        double ppb = wRegion / bases;
        zoomPPB = ppb;
        //System.out.println("Bases: " + bases + ", ppb: " + ppb);

        double rw = AnnotationSymbols.CDSblockHeight;
        double rws = adjustValue(rw);
        String trans = "";
        String fmtpos, str1;
        double x, y, ttstr, ttend;
        boolean dspExons = false;
        boolean negStrand = false;
        double wExpLevel = leftPadding - 10;
        double expLevelPpb = (maxExpLevel == 0)? 0 : wExpLevel / maxExpLevel;
        boolean genomic = false;
        boolean intronLegend = false;
        boolean inexLegend = false;
        
        //System.out.println("wRgn: " + wRegion + ", bases: " + bases + ", ppb: " + ppb);
        if(args.prefs.ruler)
            drawRuler(gc, xoffset + leftPadding, yoffset - 20, wRegion, args.hRegion, ppb, geneEntry.strpos, geneEntry.endpos, 0, true);

        // process all gene annotations
        //int fkcnt = 0;
        //if(args.prefs.alignment)
        //    System.out.println("Alignment ON **************************");
        int transCount = 0;
        boolean endloop = false;
        
        // Create a list with NMD transcripts for colored with other color
        String[] gene_transcripts = geneIsoformsExpression.keySet().toArray(new String[geneIsoformsExpression.size()]);
        ArrayList<String> transNMD = new ArrayList<String>();
        for(int i=0;i<gene_transcripts.length;i++){
            if(project.data.getIfTransHasAnnot(gene_transcripts[i], "NMD") > 0)
                transNMD.add(gene_transcripts[i]);
        }
        
        for(AnnotationEntry entry : annotationList) {
            if(entry.source.equals(STRUCTURAL_SOURCE))
                continue;
            
            AnnotFeature dbcatid = project.data.getRsvdAnnotFeature(entry.source, entry.feature);
            if(seqEntry == null && dbcatid != AnnotFeature.TRANSCRIPT) {
                logger.logError("Gene transcript annotations must have 'transcript' as first entry. Entry: " + entry.feature);
                return;
            }
            gc.setStroke(defaultColor);
            gc.setFill(defaultColor);
            gc.setFont(defaultFont);
            String cat = project.data.getTransAlignmentCategory(entry.id);
            boolean nmd = false;
            
            //used in CDS switch but now the code doesn't enter 
            if(transNMD.contains(entry.id))
                nmd = true;
            
            //System.out.println("Trans ALoop Entry: " + entry.type);
            switch(dbcatid) {
                case TRANSCRIPT:
                    if(transCount >= TabGeneDataViz.MAX_GENE_TRANS && !limitExceeded) {
                        if(args.redrawCount == 1)
                            Utils.showAlertLater(Alert.AlertType.WARNING, "Gene Transcripts Limit Exceeded", "The selected gene contains too many isoforms,\nshowing only the first " + TabGeneDataViz.MAX_GENE_TRANS + ".");
                        limitExceeded = true;
                        endloop = true;
                        break;
                    }
                    genomic = false;
                    if(seqEntry == null) {
                        gc.setFont(smallFont);
                        x = xoffset + leftPadding - 5;
                        y = yoffset - 10;
                        gc.setFill(Color.PLUM);
                        gc.setTextAlign(TextAlignment.RIGHT);
                        gc.fillText("5' UTR", x, y);
                        x = cw - xoffset - rightPadding + 5;
                        gc.setTextAlign(TextAlignment.LEFT);
                        gc.fillText("3' UTR", x, y);
                        //System.out.println("Starting transcript: " + entry.id);
                        gc.setFont(defaultFont);
                    }
                    else {
                        cdsList.clear();
                        genoexonsList.clear();
                        genoSpliceJuncList.clear();
                        spliceJuncList.clear();
                        tgexonsList.clear();
                        tgcdsList.clear();
                        dspExons = false;
                        align5utr = null;
                        align3utr = null;
                        cdspos.start = 0; cdspos.end = 0;
                        utr5pos.start = 0; utr5pos.end = 0;
                        utr3pos.start = 0; utr3pos.end = 0;

                        // clear all used points from last sequence
                        int cnt = seqList.size();
                        for(int i = seqList.size() - 1; i >= 0; i--)
                        {
                            if(i < 2)
                                seqList.get(i).clear();
                            else
                                seqList.remove(i);
                        }
                        // adjust starting 'y' position, taking into account additional levels (if any)
                        ymain += adjustY(seqHeight + (cnt - 2) * levelOffset);
                    }
                    seqEntry = entry;
                    negStrand = entry.strand.equals("-");

                    // display transcript ID
                    gc.setFill(Color.MAROON);
                    gc.setTextAlign(TextAlignment.LEFT);
                    String rsid = "";
                    HashMap<String, String> attrs = DataAnnotation.getAttributes(entry.attrs);
                    if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId)) {
                        String tid = attrs.get(project.data.getAnnotationFileDefs().attrId);
                        if(!tid.equals(entry.id))
                            rsid = " " + tid;
                    }
                    trans = entry.id;
                    String dsptransid = entry.id + " (" + entry.strand + ")" + rsid;
                    gc.fillText(dsptransid, xoffset, ymain - 4);
                    double wtxt = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(dsptransid, gc.getFont());
//                    if(!cat.isEmpty()) {
//                        String dspid = "";
//                        if(args.prefs.structSubClass) {
//                            if(attrs.containsKey(project.data.getAnnotationFileDefs().attrSecClass))
//                                dspid = "  (" + attrs.get(project.data.getAnnotationFileDefs().attrSecClass).replaceAll("[,]", ", ") + ")";
//                        }
//                        gc.setFill(Color.STEELBLUE);
//                        gc.fillText(cat + dspid, xoffset + wtxt + 10, ymain - 4);
//                    }
                    
                    // display expression levels if not time series and available
                    if(project.data.isCaseControlExpType() && geneIsoformsExpression.containsKey(seqEntry.id)) {
                        Font f = gc.getFont();
                        DataProject.TransExpLevels el = geneIsoformsExpression.get(seqEntry.id);
                        //System.out.println("maxEL: " + maxExpLevel + ", wEL: " + wExpLevel + ", ppb: " + expLevelPpb + "x1: " + el.X1_mean + ", x2: " + el.X2_mean);
                        double xc = xoffset;
                        double fy = adjustY(hbottom + ymain - (AnnotationSymbols.RNAblockHeight/2) - 2);
                        gc.setFill(Color.ORANGE);
                        double wexp1 = adjustX(el.X1_mean * expLevelPpb);
                        gc.fillRect(xc + 0.5, fy - 2, wexp1, 4);
                        double wexp2 = adjustX(el.X2_mean * expLevelPpb);
                        gc.setFill(Color.ORANGERED);
                        gc.fillRect(xc + 0.5, fy + 3, wexp2, 4);
                        gc.setFill(Color.BLACK);
                        gc.fillRect(xc, fy - 4, 0.5, 12);
                        gc.fillRect(xc, fy + 8.5, wExpLevel, 0.5);
                        gc.setTextAlign(TextAlignment.RIGHT);
                        gc.setFont(new Font(8)); // some font sizes will create a gap between digits, e.g. 7
                        gc.setFill(Color.GRAY);
                        gc.fillText(NumberFormat.getInstance().format(maxExpLevel), xc + wExpLevel, fy + 17);
                        String dea = "";
                        if(project.data.analysis.hasDEAData(DataApp.DataType.TRANS)) {
                            String der = "(not included in analysis)";
                            if(geneIsoformsDEResults.containsKey(seqEntry.id)) {
                                der = geneIsoformsDEResults.get(seqEntry.id)? "DE" : "Not DE";
                                gc.setTextAlign(TextAlignment.LEFT);
                                gc.setFont(new Font(8));
                                gc.setFill(Color.CHOCOLATE);
                                gc.fillText(der, xc + 1, fy-5);
                            }
                            dea = "DEA Result: " + der + "\n";
                        }
                        double el1 = Double.parseDouble(String.format("%.2f", ((double)Math.round(el.X1_mean*100)/100.0)));
                        double el2 = Double.parseDouble(String.format("%.2f", ((double)Math.round(el.X2_mean*100)/100.0)));
                        String[] names = project.data.getExpTypeGroupNames();
                        String ttip = String.format("Isoform Expression Level (normalized mean)\n" + dea +
                                     names[0] + " ExpLevel: %s   (top line)\n" +
                                     names[1] + " ExpLevel: %s   (bottom line)", 
                                     NumberFormat.getInstance().format(el1), NumberFormat.getInstance().format(el2));
                        addToolTipToList(ttip, xc, fy - 10, wExpLevel, 25, false);
                        gc.setTextAlign(TextAlignment.LEFT);
                        gc.setFont(f);
                    }
                    
                    width = com.sun.javafx.tk.Toolkit.getToolkit().getFontLoader().computeStringWidth(dsptransid, gc.getFont());
                    addToolTipToList(entry, xoffset, ymain - 12, width, 10, true);
                    ymain += hbottom;
                    ymain = adjustY(ymain);

                    x = xoffset + leftPadding + (entry.strpos - geneEntry.strpos) * ppb;
                    gc.setFill(defaultColor);
                    gc.setStroke(defaultColor);
                    x = adjustX(x);
                    int dspstr = entry.strpos;
                    int dspend = entry.endpos;
                    if(args.prefs.alignment) {
                        dspstr = geneEntry.strpos;
                        dspend = geneEntry.endpos;
                    }
                    double dw = dspend - dspstr + 1;
                    if(dw > 1) {
                        if(!args.prefs.alignment) {
                            y = adjustY(ymain - (AnnotationSymbols.RNAblockHeight/2)); 
                            symbols.drawRNAblock(gc, x, y, (dw * ppb));
                            gc.setFill(Color.DARKGRAY);
                            x = xoffset + leftPadding + 5 + dw * ppb;
                            str1 = NumberFormat.getInstance().format(entry.endpos-entry.strpos+1);
                            gc.fillText(str1 + "nt", x, ymain + 4);
                        }
                        else {
                            dspsizeX = xoffset + leftPadding + 5 + dw * ppb;
                            dspsizeY = ymain + 4;
                        }
                    }
                    transCount++;
                    break;
                case GENOMIC:
                    // genomic section is always the last section within a transcript
                    genomic = true;
                    break;
                case EXON:
                    genoexonsList.add(new SeqAlign.Position(entry.strpos, entry.endpos));
                    //System.out.println("added genome pos exon: " + entry.strpos + " to " + entry.endpos);
                    break;
                case SPLICEJUNCTION:
                    if(args.prefs.alignment && args.prefs.spliceJuncs) {
                        // annotation file positions junctions at the start of the intron
                        // move to the exon part for display purposes - adjust based on strand
                        int start = negStrand? entry.strpos - 1 : entry.strpos - 1;
                        int end = negStrand? entry.endpos + 1 : entry.endpos + 1;
                        genoSpliceJuncList.add(new SpliceJuncData(negStrand, new SeqAlign.Position(start, end), entry.attrs));
                        //System.out.println("added genome pos SJ: " + entry.strpos + " to " + entry.endpos);
                    }
                    break;
                case CDS:
                    //now the code not enter here!!!
                    cdspos = new SeqAlign.Position(entry.strpos, entry.endpos);
                    if(!args.prefs.alignment) {
                        cdsList.add(entry);
                        x = adjustX(xoffset + leftPadding + (entry.strpos - geneEntry.strpos) * ppb);
                        y = adjustY(ymain - (rw/2));
                        //gc.setGlobalAlpha(0.9);
                        symbols.drawCDSblock(gc, x, y, (entry.endpos - entry.strpos + 1) * ppb, nmd);
                        addToolTipToList(entry, x, y, (entry.endpos - entry.strpos + 1) * ppb, rws, false);
                    }
                    break;
                case GENE:
                    break;
                default:
                    switch(entry.feature) {
                        case DataAnnotation.INTCAT_EXON:
                            if(args.prefs.alignment) {
                                dspExons = true;
                                tgexonsList.add(new SeqAlign.Position(entry.strpos, entry.endpos));
                                //System.out.println("added exon: " + entry.strpos + " to " + entry.endpos);
                            }
                            break;
                        case DataAnnotation.INTCAT_SPLICEJUNCTION:
                            if(args.prefs.alignment && args.prefs.spliceJuncs) {
                                // annotation file positions junctions at the start of the intron
                                // move to the exon part for display purposes - adjust based on strand
                                //int start = negStrand? entry.strpos + 1 : entry.strpos - 1;
                                //int end = negStrand? entry.endpos - 1 : entry.endpos + 1;
                                //spliceJuncList.add(new SeqAlign.Position(start, end));
                                spliceJuncList.add(new SeqAlign.Position(entry.strpos, entry.endpos));
                                //System.out.println("added aligned pos SJ: " + entry.strpos + " to " + entry.endpos);
                            }
                            break;
                        case DataAnnotation.INTCAT_CDS:
                            if(args.prefs.alignment) {
                                tgcdsList.add(new SeqAlign.Position(entry.strpos, entry.endpos));
                            }
                            break;
                        case DataAnnotation.INTCAT_NODATA:
                            gc.setFill(Color.DARKGREY);
                            x = xoffset + leftPadding;
                            gc.fillText("No alignment data available...", x, ymain + 4);
                            break;
                        case DataAnnotation.INTCAT_5UTR:
                            align5utr = entry;
                            utr5pos = new SeqAlign.Position(entry.strpos, entry.endpos);
                            //System.out.println("5utr: " + entry.strpos + ".." + entry.endpos);
                            break;
                        case DataAnnotation.INTCAT_3UTR:
                            align3utr = entry;
                            utr3pos = new SeqAlign.Position(entry.strpos, entry.endpos);
                            //System.out.println("3utr: " + entry.strpos + ".." + entry.endpos);
                            break;
                        default:
                            //System.out.println("Entry type: " + entry.type);
                            if(!project.data.isInternalFeature(entry.source, entry.feature) || entry.feature.equals(DataAnnotation.INTCAT_FLUSHDATA)) {
                                double dsppos = entry.strpos + Math.abs((entry.endpos - entry.strpos + 1)/2);
                                if(args.prefs.alignment) {
                                    if(dspExons){
                                        //do it once for each nmd
                                        if(transNMD.contains(trans)){
                                            transNMD.remove(trans);
                                            nmd=true;
                                        }
                                        dspExons = false;
                                        double exsize = SeqAlign.getTotalExonsSize(tgexonsList);
                                        gc.setFill(Color.DARKGRAY);
                                        str1 = NumberFormat.getInstance().format(exsize);
                                        gc.fillText(str1 + "nt", dspsizeX, dspsizeY);
                                        displayExons(trans, gc, xoffset, leftPadding, ymain, ppb, negStrand, genoexonsList,
                                                    tgexonsList, tgcdsList, genoSpliceJuncList, spliceJuncList, cdsList, seqEntry, align5utr, align3utr, nmd);
                                    }
                                    // check if we just wanted to make sure exon data was flushed
                                    if(entry.feature.equals(DataAnnotation.INTCAT_FLUSHDATA))
                                        break;

                                    if(!genomic) {  // temp?
                                        entry.dspstrpos = seqAlign.getAlignedPos(entry.strpos, tgexonsList);
                                        entry.dspendpos = seqAlign.getAlignedPos(entry.endpos, tgexonsList);
                                        fmtpos = String.format("%s%s", NumberFormat.getInstance().format(entry.dspstrpos),
                                                        (entry.dspstrpos == entry.dspendpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(entry.dspendpos)));
                                        entry.tooltipAligned = String.format("\nAligned: %s", fmtpos);
                                    }
                                }
                                else {
                                    entry.dspstrpos = entry.strpos;
                                    entry.dspendpos = entry.endpos;
                                }
                                entry.dsppos = entry.dspstrpos + (entry.dspendpos - entry.dspstrpos + 1)/2;

                                // check for genomic display - need to handle differently based on whether it is contained
                                // in a region that is being displayed or not
                                SeqAlign.WithinRegion within = SeqAlign.WithinRegion.YES;
                                if(genomic) {
                                    SeqAlign.Position chkpos = new SeqAlign.Position(entry.strpos, entry.endpos);
                                    within = SeqAlign.isWithinExonsRegion(chkpos, genoexonsList);
// is this right?
                                    if(args.prefs.alignment) {
                                        entry.dsppos = seqAlign.getGenomicXlatedPos(dsppos, negStrand);
                                        entry.dspstrpos = seqAlign.getGenomicXlatedPos(entry.strpos, negStrand);
                                        entry.dspendpos = seqAlign.getGenomicXlatedPos(entry.endpos, negStrand);
                                    }
                                    else {
                                        entry.dsppos = SeqAlign.getGenomicXlatedPos(dsppos, negStrand, genoexonsList);
                                        entry.dspstrpos = SeqAlign.getGenomicXlatedPos(entry.strpos, negStrand, genoexonsList);
                                        entry.dspendpos = SeqAlign.getGenomicXlatedPos(entry.endpos, negStrand, genoexonsList);
                                    }
                                }
                                if(within.equals( SeqAlign.WithinRegion.NO))
                                    intronLegend = true;
                                else if(within.equals( SeqAlign.WithinRegion.PARTIALLY))
                                    inexLegend = true;
                                addFeatureID(entry);
                                if(isDisplayableFeatureId(entry, args.prefs))
                                    drawTrackSymbol(entry, geneEntry, seqEntry, tgexonsList, cdsList, seqList, gc, xoffset + leftPadding, ymain, ppb, negStrand, genomic, args.prefs.alignment, within);
                                if(symbolsList.indexOf(entry.feature) == -1) {
                                    symbolsList.add(entry.feature);
                                }
                            }
                            break;
                    }
                    break;
            }
            if(endloop)
                break;
        }
            
        // draw legend
        int cnt = seqList.size();
        y = ymain + seqHeight + (cnt - 2) * levelOffset;
        args.hRegion = y - yRegion;
        //Rectangle2D.Double rc = new Rectangle2D.Double(xoffset, y, (cw - 2 * xoffset), 0);
        Rectangle2D.Double rc = new Rectangle2D.Double(xoffset, y, (cw - 2 * xoffset), 0);
        //System.out.println("Calling with y: " + rc.y + ", cnt: " + cnt);
        if(args.redrawCount == 1)
            args.singleColumn = symbols.isSingleColumnLegend(gc, legendFont, rc);
        Point2D.Double pmax = symbols.drawLegend(gc, legendColor, legendFont, rc, TextAlignment.CENTER, true, args.singleColumn, true, intronLegend, inexLegend);
        y = pmax.y;

        // adjust size accordingly if needed
        y += 30/scale;
        double crh = 0;
        boolean redraw = false;
        double reqHeight = (y + crh) * scale;
        System.out.println("Required canvas height (" + y + "): " + ((y + crh) * scale) + ", current canvas height: " + ch);
        if(args.redrawCount == 1) {
            if(ch < ((y + crh) * scale)) {
                System.out.println("Setting canvas height to: " + ((y + crh) * scale));
                canvas.setHeight(reqHeight);
                redraw = true;
            }

            double sbHeight = 0.0;
            double sbWidth = 0.0;
            double xadd = 0;
            if(redraw)
                xadd -= sbWidth;
            // note: the sb width is reported wrong initially, 20, if window is resized then it goes to 15
            System.out.println("Setting canvas width to: " + ((cw * scale + xadd) * widthFactor) + ", sbw: " + sbWidth);
            canvas.setWidth((cw * scale + xadd) * widthFactor);
            if(scale != 1) {
                if(pmax.x > cw)
                    System.out.println("Legend exceeded canvas width, " + pmax.x);

                // check if we have a vertical bar and adjust if so
                if(widthFactor > 1)
                    canvas.setHeight(canvas.getHeight() - sbHeight);
                redraw = true;
            }
            else if(widthFactor > 1) {
                canvas.setHeight(canvas.getHeight() - sbHeight);
                redraw = true;
            }
            else if(args.prefs.ruler)
                redraw = true;

            if(redraw) {
                args.redrawCount++;
                draw(gc, args);
            }
        }
        else {
            double setHeight = canvas.getHeight();
            System.out.println("Canvas height after recalc: " + setHeight + ", redrawcnt: " + args.redrawCount);
            if(args.redrawCount < 3 && Math.abs(setHeight - reqHeight) > 10) {
                canvas.setHeight(reqHeight);
                args.redrawCount++;
                draw(gc, args);
            }
        }
    }
    @Override
    protected void getRSAnnotationData(String rsid, ArrayList<String[]> lines, ArrayList<AnnotationEntry> alst) {
        ArrayList<AnnotationEntry> al = new ArrayList<>();
        try {
            boolean process = false;
            boolean genomic = false;
            int strpos, endpos;
            HashMap<String, String> attrs;
            for(String[] fields : lines) {
                if(fields[0].equals(rsid)) {
                    String source = fields[1];
                    String type = fields[2];
                    if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.TRANSCRIPT))
                        process = true;
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.PROTEIN))
                        process = false;
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.GENOMIC)) {
                        process = true;
                        genomic = true;
                    }
                    if(process) {
                        // skip past non-positional annotations
                        if(fields[3].isEmpty() || fields[3].equals(".") || fields[4].isEmpty() || fields[4].equals("."))
                            continue;
                        String id = "";
                        String name = "";
                        String desc;
                        String caption = type;
                        attrs = DataAnnotation.getAttributes(fields[8]);
                        if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId))
                            id = attrs.get(project.data.getAnnotationFileDefs().attrId);
                        if(attrs.containsKey(project.data.getAnnotationFileDefs().attrName))
                            name = attrs.get(project.data.getAnnotationFileDefs().attrName);
                        String tooltip = "Source: " + source + "\nFeature: " + type;
                        if(!id.isEmpty()) {
                            caption = id;
                            tooltip += "\nID: " + id;
                        }
                        if(!name.isEmpty() && !name.toLowerCase().equals(id.toLowerCase())
                                           && !name.toLowerCase().equals(type.toLowerCase())) {
                            if(id.isEmpty())
                                caption = name;
                            tooltip += "\nName: " + name;
                        }
                        if(attrs.containsKey(project.data.getAnnotationFileDefs().attrDesc)) {
                            desc = attrs.get(project.data.getAnnotationFileDefs().attrDesc);
                            if(!desc.isEmpty() && !desc.toLowerCase().equals(id.toLowerCase())
                                               && !desc.toLowerCase().equals(name.toLowerCase())
                                               && !desc.toLowerCase().equals(type.toLowerCase()))
                                tooltip += "\nDescription: " + desc;
                        }
                        if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.TRANSCRIPT)) {
                            String strid = id;
                            if(id.equals(fields[0]))
                                strid = "";
                            tooltip = "Transcript " + fields[0];
                            tooltip += "\nStrand: " + (fields[6].trim().equals("-")? "Negative" : "Positive");
                            if(!strid.isEmpty())
                                tooltip += "\nRefID: " + id;
                            if(attrs.containsKey("Alias"))
                                tooltip += " (" + attrs.get("Alias") + ")";
                        }

                        String urlValue = "";
                        strpos = Integer.parseInt(fields[3]);
                        endpos = Integer.parseInt(fields[4]);
                        if(endpos < strpos) {
                            int tmp = endpos;
                            endpos = strpos;
                            strpos = tmp;
                        }
                        
                        String idval = id.isEmpty()? name : id;
                        al.add(new AnnotationEntry(fields[0],source,type,idval,caption,fields[6],strpos,endpos,fields[8], tooltip, urlValue));
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.logError("Unable to process annotation data for transcript " + rsid + ": " + ex.getMessage());
            al.clear();
        }
        
        // must add in proper order for processing at drawing time
        // these values come from the annotation file, there are no internal categories
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.TRANSCRIPT)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.CDS)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON))
                alst.add(a);
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.SPLICEJUNCTION))
                alst.add(a);
        }
        for(AnnotationEntry a : al) {
            if(!project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.TRANSCRIPT) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.CDS) && 
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.SPLICEJUNCTION) &&
                    !a.source.equals(STRUCTURAL_SOURCE))
                alst.add(a);
        }
    }
}
