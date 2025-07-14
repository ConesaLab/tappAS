/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SeqAlign extends AppObject {
    static public enum WithinRegion { YES, NO, PARTIALLY };
    
    protected final ArrayList<Position> lstGeneExons = new ArrayList<>();
    protected final ArrayList<Position> lstGenomicGeneExons = new ArrayList<>();
    
    public SeqAlign() {
        super(null, null);
    }
    public void clearData() { lstGeneExons.clear(); lstGenomicGeneExons.clear(); }

    // build exon alignment table for the whole gene so only areas used are considered to determine gaps
    // Note: most functions in this class are based on the values set by calling updGeneExonsList
    protected void updGeneExonsList(int strpos, int endpos)
    {
        if(lstGeneExons.isEmpty()) {
            //System.out.println("Added first exon " + strpos + "-" + endpos);
            lstGeneExons.add(new Position(strpos, endpos));
        }
        else {
            boolean processed = false;
            int cnt = lstGeneExons.size();
            // positions must be sorted in ascending order
            Collections.sort(lstGeneExons);

            // check if need to add whole exon at start
            Position expos = lstGeneExons.get(0);
            if(endpos < expos.start) {
                lstGeneExons.add(0, new Position(strpos, endpos));
                //System.out.println("Added whole exon " + strpos + "-" + endpos + " at start");
                processed = true;
            }
            else {
                // check if need to add whole exon at end
                expos = lstGeneExons.get(lstGeneExons.size() - 1);
                if(strpos > expos.end) {
                    //System.out.println("Added whole exon " + strpos + "-" + endpos + " at end");
                    lstGeneExons.add(new Position(strpos, endpos));
                    processed = true;
                }
                else {
                    // check if we have a whole exon to insert somewhere
                    Position p1, p2;
                    for(int idx = 0; idx < (cnt - 1); idx++) {
                        p1 = lstGeneExons.get(idx);
                        p2 = lstGeneExons.get(idx+1);
                        if(strpos > p1.end && endpos < p2.start) {
                            //System.out.println("Inserted whole exon " + strpos + "-" + endpos + " at position " + (idx+1));
                            lstGeneExons.add((idx+1), new Position(strpos, endpos));
                            cnt++;
                            processed = true;
                            break;
                        }
                    }
                }
            }
            if(!processed) {
                // check if we have an earlier starting point but flows into next exon(s)
                expos = lstGeneExons.get(0);
                if(strpos < expos.start) {
                    expos.start = strpos;
                    //System.out.println("Adding new starting exon point " + expos.start + " from new exon " + strpos + "-" + endpos);
                    if(endpos > expos.end) {
                        expos.end = endpos;
                        for(int idx = 1; idx < cnt; idx++) {
                            // check if need to merge and remove
                            Position nxtpos = lstGeneExons.get(idx);
                            if(expos.end >= nxtpos.start) {
                                //System.out.println("Removing exon " + nxtpos.start + "-" + nxtpos.end + " (part of new exon)");
                                lstGeneExons.remove(idx);
                                cnt--;
                                if(nxtpos.end > expos.end) {
                                    expos.end = nxtpos.end;
                                    //System.out.println("Setting ending exon point to " + expos.end);
                                    break;
                                }
                            }
                            else {
                                //System.out.println("Setting ending exon point to " + expos.end);
                                break;
                            }
                        }
                    }
                    processed = true;
                }
            }
            
            if(!processed) {
                int idx = 0;
                // will always break from loop after removing an entry so looping is OK
                for(Position pos : lstGeneExons) {
                    // check if start falls within existing exon
                    if(strpos >= pos.start && strpos <= pos.end) {
                        if(endpos > pos.end) {
                            //System.out.println("Found exon " + strpos + "-" + endpos + " starting point in existing exon");
                            pos.end = endpos;
                            for(idx += 1; idx < cnt; idx++) {
                                // check if need to merge and remove
                                Position nxtpos = lstGeneExons.get(idx);
                                if(pos.end >= nxtpos.start) {
                                    //System.out.println("Removing exon " + nxtpos.start + "-" + nxtpos.end + " (part of new exon)");
                                    lstGeneExons.remove(idx);
                                    cnt--;
                                    if(nxtpos.end > pos.end) {
                                        pos.end = nxtpos.end;
                                        //System.out.println("Setting ending exon point to " + pos.end);
                                        break;
                                    }
                                }
                                else {
                                    //System.out.println("Setting ending exon point to " + pos.end);
                                    break;
                                }
                            }
                        }
                        //else
                        //    System.out.println("Found exon " + strpos + "-" + endpos + " contained in existing exon");
                        processed = true;
                        break;
                    }
                    else {
                        // check if end falls within existing exon
                        if(endpos >= pos.start && endpos <= pos.end) {
                            if(strpos < pos.start) {
                                //System.out.println("Found exon " + strpos + "-" + endpos + " ending point in existing exon");
                                pos.start = strpos;
                                for(idx -= 1; idx >= 0; idx--) {
                                    // check if need to merge and remove
                                    Position prvpos = lstGeneExons.get(idx);
                                    if(prvpos.end >= pos.start) {
                                        //System.out.println("Removing exon " + prvpos.start + "-" + prvpos.end + " (part of new exon)");
                                        lstGeneExons.remove(idx);
                                        if(prvpos.start < pos.start) {
                                            pos.start = prvpos.start;
                                            //System.out.println("Setting starting exon point to " + pos.end);
                                            break;
                                        }
                                    }
                                    else {
                                        //System.out.println("Setting starting exon point to " + pos.start);
                                        break;
                                    }
                                }
                            }
                            //else
                            //   System.out.println("Found exon " + strpos + "-" + endpos + " contained in existing exon");
                            processed = true;
                            break;
                        }
                    }
                    idx++;
                }
                if(!processed) {
                    // we are adding an entry that starts in between but flows into sequentials exon(s)
                    Position p1, p2, p3;
                    for(int i = 0; i < (cnt - 1); i++) {
                        p1 = lstGeneExons.get(i);
                        p2 = lstGeneExons.get(i+1);
                        if(strpos > p1.end && strpos < p2.start) {
                            // we got the starting point, set to insert
                            int stridx = i+1;
                            boolean insert = true;

                            // now we must see where it ends
                            for(int j = stridx; j < cnt; j++) {
                                p3 = lstGeneExons.get(j);
                                if(endpos > p3.end) {
                                    // this exon is no longer needed, remove and adjust index and count
                                    //System.out.println("Removed gene exon: " + p3.start + "-" + p3.end);
                                    lstGeneExons.remove(j--);
                                    cnt--;
                                }
                                else {
                                    if(endpos > p3.start) {
                                        // we end inside this exon, just update the start position and done
                                        insert = false;
                                        //System.out.println("Modified gene exon: " + p3.start + "-" + p3.end);
                                        p3.start = strpos;
                                        //System.out.println(" to: " + p3.start + "-" + p3.end);
                                    }

                                    // regardless, we are done - will insert new exon if 'insert' not cleared
                                    break;
                                }
                            }
                            if(insert) {
                                lstGeneExons.add(stridx, new Position(strpos, endpos));
                                //System.out.println("Inserted gene exon: " + strpos + "-" + endpos);
                            }
                            processed = true;
                            break;
                        }
                    }
                    if(!processed) {
                        logger.logWarning("ERROR - SeqAlign unable to place gene exon: " + strpos + "-" + endpos);
                        logger.logDebug("Current seqAlign gene exon: ");
                        for(Position p : lstGeneExons)
                            logger.logDebug(p.start + "-" + p.end);
                    }
                }
            }
        }
        
        // keep a copy of the genomic values
        // lstGeneExons will be converted by reverseGeneExons() call
        lstGenomicGeneExons.clear();
        for(Position p : lstGeneExons)
            lstGenomicGeneExons.add(new Position(p.start, p.end));
    }
    /*
    public WithinRegion isWithinGeneRegion(Position pos) {
        WithinRegion within = WithinRegion.NO;
        for(Position psge : lstGeneExons) {
            if(pos.start >= psge.start && pos.start <= psge.end) {
                if(pos.end <= psge.end)
                    within = WithinRegion.YES;
                else
                    within = WithinRegion.PARTIALLY;
                break;
            }
        }
        return within;
    }
    public WithinRegion isWithinGenomicGeneRegion(Position pos) {
        WithinRegion within = WithinRegion.NO;
        for(Position psge : lstGenomicGeneExons) {
            if(pos.start >= psge.start && pos.start <= psge.end) {
                if(pos.end <= psge.end)
                    within = WithinRegion.YES;
                else
                    within = WithinRegion.PARTIALLY;
                break;
            }
        }
        return within;
    }*/
    public static WithinRegion isWithinExonsRegion(Position pos, ArrayList<Position> lstTransExons) {
        WithinRegion within = WithinRegion.NO;
        for(Position psge : lstTransExons) {
            if(pos.start >= psge.start && pos.start <= psge.end) {
                if(pos.end <= psge.end)
                    within = WithinRegion.YES;
                else
                    within = WithinRegion.PARTIALLY;
                break;
            }
        }
        return within;
    }
    // get contigous size of gene exons
    public int getGeneExonsSize() {
        int size = 0;
        for(SeqAlign.Position pos : lstGeneExons)
            size += Math.abs(pos.end - pos.start) + 1;
        return size;
    }
    // get genome range values of gene exons
    public Position getGeneExonsRange() {
        int start = 0;
        int end = 0;
        for(SeqAlign.Position pos : lstGeneExons) {
            if(Math.min(pos.start, pos.end) < start || start == 0)
                start = Math.min(pos.start, pos.end);
            if(Math.max(pos.start, pos.end) > end)
                end = Math.max(pos.start, pos.end);
        }
        Position pos = new Position(start, end);
        return pos;
    }
    // reverse values for lstGeneExons and sort - required for proper alignment
    // values become 1 based vs genomic coordinates
    // e.g. 55419788..55419919 to 1..132 where gexons: 55394501..55419919
    // WARNING: this function changes seqAlign values from genomic to 1 based
    // easy way to deal with the negative strand BUT must always take it into account
    public void reverseGeneExons() {
        int tmp;
        Position epos = getGeneExonsRange();
        int end = epos.end;
        for(Position pos : lstGeneExons) {
            tmp = pos.start;
            pos.start = end - pos.end + 1;
            pos.end = end - tmp + 1;
        }
        Collections.sort(lstGeneExons);
    }
    public String getGeneExonsString() { return getExonsString(lstGeneExons); }
    
    //
    // Internal Functions
    //
    
    // get aligned position given a relative, to 1, non-gapped position (e.g. value from RefSeq)
    protected double getAlignedPos(double start, ArrayList<Position> lstTransExons) {
        int cnt = lstTransExons.size();
        double ori_start = start;
        double end_position = 0.0D;


        boolean isGenomic = false;
        Position firstExon = this.lstGenomicGeneExons.get(0);
        Position lastExon = this.lstGenomicGeneExons.get(this.lstGenomicGeneExons.size() - 1);
        if (firstExon.start <= start && start <= lastExon.end) {
            isGenomic = true;
        } else {
            isGenomic = false;
        } 

        if (!isGenomic) {
            double tgstart = start;
            for (Position psge : lstTransExons) {
                cnt--;
                int val = Math.abs(psge.end - psge.start) + 1;
                end_position = psge.end;
                if (start <= val || cnt == 0) {
                    if (cnt == 0 && start > val) {
                        continue;
                    }
                    tgstart = psge.start + start - 1.0D;
                    break;
                } 
                start -= val;
            } 

            if (ori_start == tgstart) {

                double d = getXlatedPos(end_position);
                return d;
            } 
            double d1 = getXlatedPos(tgstart);
            return d1;
        } 

        double pstart = getXlatedPos(start);
        return pstart;
}



protected double getAlignedPos(double start, ArrayList<Position> lstTransExons, boolean negStrand) {
    int cnt = lstTransExons.size();
    double ori_start = start;
    double end_position = 0.0D;

    boolean isGenomic = false;
    Position firstExon = this.lstGenomicGeneExons.get(0);
    Position lastExon = this.lstGenomicGeneExons.get(this.lstGenomicGeneExons.size() - 1);
    if (firstExon.start <= start && start <= lastExon.end) {
        isGenomic = true;
    } else {
        isGenomic = false;
    } 

    if (!isGenomic) {
        double tgstart = start;
        for (Position psge : lstTransExons) {
            cnt--;
            int val = Math.abs(psge.end - psge.start) + 1;
            end_position = psge.end;
            if (start <= val || cnt == 0) {
                if (cnt == 0 && start > val) {
                    continue;
                }
                tgstart = psge.start + start - 1.0D;
                break;
            } 
            start -= val;
        } 

        if (ori_start == tgstart) {

            double d = getXlatedPos(end_position);
            return d;
        } 
        double d1 = getXlatedPos(tgstart);
        return d1;
    } 

    double pstart = getGenomicXlatedPos(start, negStrand);
    return pstart;
}


    // note that if the start does not fall within an exon region, it will return the last matching position
    protected double getXlatedPos(double start) {
        double pstart = 0;
        for(Position psge : lstGeneExons) {
            if(start <= psge.end) {
                // make sure we are within range, otherwise return last pstart
                if(start >= psge.start)
                    pstart += start - psge.start + 1;
                break;
            }
            pstart += Math.abs(psge.end - psge.start) + 1;
        }
        return pstart;
    }
    // note that if the start does not fall within an exon region, it will return the last matching position
    // this is by design since it is used for displaying intergenic annotations in the transcript view
    protected double getGenomicXlatedPos(double start, boolean negStrand) {
        double pstart = 0;
        if(!negStrand)
            pstart = getXlatedPos(start);
        else {
            ArrayList<Position> lst = new ArrayList<>();
            for(Position psge : lstGenomicGeneExons)
                lst.add(psge);
            Collections.reverse(lst);
            int idx = 0;
            int cnt = lst.size();
            for(Position psge : lst) {
                // return last pstart if within exons - keep in mind this is the negative strand (higher numbers to lower numbers)
                if(start >= psge.end)
                    break;
                else {
                    if(++idx == cnt) {
                        if(start >= psge.start)
                            pstart += Math.abs(psge.end - start) + 1;
                    }
                    else
                        pstart += Math.abs(psge.end - psge.start) + 1;
                }
            }
        }
        return pstart;
    }
    protected static double getGenomicXlatedPos(double start, boolean negStrand, ArrayList<Position> lstTransExons) {
        double pstart = 0;
        if(!negStrand) {
            for(Position psge : lstTransExons) {
                if(start <= psge.end) {
                    // make sure we are within range, otherwise return last pstart
                    if(start >= psge.start)
                        pstart += start - psge.start + 1;
                    break;
                }
                pstart += Math.abs(psge.end - psge.start) + 1;
            }
        }
        else {
            for(Position psge : lstTransExons) {
                if(start >= psge.end) {
                    // make sure we are within range, otherwise return last pstart
                    if(start <= psge.start)
                        pstart += psge.start - start + 1;
                    break;
                }
                pstart += Math.abs(psge.end - psge.start) + 1;
            }
        }
        return pstart;
    }
    // Get UTR segments based on the transcript exons
    // WARN: the strpos and endpos must have been set based on the transcript exons
    protected ArrayList<SegmentDisplay> getUtrSegments(int alignUTRstrpos, int alignUTRendpos, ArrayList<Position> lstTransExons) {
        ArrayList<SegmentDisplay> segList = new ArrayList<>();
        double start = 0;
        double end = 0;
        double utrStart = -1;
        double utrRelativeStart = -1;
        double utrWidth = 0;

        Position pdraw;
        boolean flush;
        int cnt = 0;
        int tgcnt = lstTransExons.size();
        //System.out.println("autr: " + alignUTRstrpos + " to " + alignUTRendpos);
        for(Position pos : lstTransExons) {
            //System.out.println("tgepos: " + pos.start + " to " + pos.end);
            pdraw = null;
            flush = false;
            if(++cnt == tgcnt)
                flush = true;
            if(pos.start >= alignUTRstrpos && pos.end <= alignUTRendpos)
                pdraw = pos;
            else if(pos.start >= alignUTRstrpos && pos.start <= alignUTRendpos)
                pdraw = new Position(pos.start, alignUTRendpos);
            else if(pos.end >= alignUTRstrpos && pos.end <= alignUTRendpos)
                pdraw = new Position(alignUTRstrpos, pos.end);
            else {
                flush = true;
                //System.out.println("No if condition, utr: " + alignUTRstrpos + " to " + alignUTRendpos + ", pos: " + pos.start + " to " + pos.end);
            }
            if(pdraw != null) {
                double pstart = getXlatedPos(pdraw.start);
                double pend = getXlatedPos(pdraw.end);
                //if((pend - pstart) != (pdraw.end - pdraw.start))
                //    System.out.println("Utr: " + pdraw.start + ".." + pdraw.end + " vs " + pstart + ".." + pend);
                double relstart = getRelativePos(pdraw.start, lstTransExons);
                if(utrStart == -1) {
                    start = pdraw.start;
                    end = pdraw.end;
                    utrStart = pstart;
                    utrRelativeStart = relstart;
                    utrWidth = Math.abs(pdraw.end - pdraw.start) + 1;
                    //System.out.println("Utr start: " + pstart + ", pds: " + pdraw.start);
                }
                else {
                    if((pstart - (utrStart + utrWidth)) > 1) {
                        //System.out.println("Utr new start after break: " + pstart + ", " + (utrStart + utrWidth));
                        segList.add(new SegmentDisplay(start, end, utrStart, utrRelativeStart, utrWidth));

                        start = pdraw.start;
                        end = pdraw.end;
                        utrStart = pstart;
                        utrRelativeStart = relstart;
                        utrWidth = Math.abs(pdraw.end - pdraw.start) + 1;
                    }
                    else {
                        //System.out.println("Utr append: " + pstart);
                        end = pdraw.end;
                        utrWidth += Math.abs(pdraw.end - pdraw.start) + 1;
                    }
                    if(flush) {
                        flush = false;
                        //System.out.println("Utr flush: " + utrStart + ".." + (utrStart+utrWidth-1));
                        segList.add(new SegmentDisplay(start, end, utrStart, utrRelativeStart, utrWidth));
                        utrStart = -1;
                    }
                }
            }
            if(flush) {
                if(utrStart != -1) {
                    //System.out.println("Utr end flush: " + utrStart + ".." + (utrStart+utrWidth-1));
                    segList.add(new SegmentDisplay(start, end, utrStart, utrRelativeStart, utrWidth));
                    utrStart = -1;
                }
            }
        }
        //System.out.println("seglst: " + segList.size());
        return segList;
    }
    // Get CDS segments based on the transcript exons
    protected ArrayList<SegmentDisplay> getCdsSegments(ArrayList<Position> tgcdsList, ArrayList<Position> lstTransExons) {
        ArrayList<SegmentDisplay> segList = new ArrayList<>();
        double start = 0;
        double end = 0;
        double cdsStart = -1;
        double cdsRelativeStart = -1;
        double cdsWidth = 0;
        Position pdraw;
        boolean flush;
        int cnt = 0;
        int tgcnt = tgcdsList.size();
        for(Position pos : tgcdsList) {
            pdraw = pos;
            flush = false;
            if(++cnt == tgcnt)
                flush = true;
            double pstart = getXlatedPos(pdraw.start);
            double pend = getXlatedPos(pdraw.end);
            double relstart = getRelativePos(pdraw.start, lstTransExons);
            if(cdsStart == -1) {
                start = pdraw.start;
                end = pdraw.end;
                cdsStart = pstart;
                cdsRelativeStart = relstart;
                cdsWidth = Math.abs(pdraw.end - pdraw.start) + 1;
                //System.out.println("CDS start: " + pstart + ", pds: " + pdraw.start + ", cdsWidth: " + cdsWidth);
            }
            else {
                // changing from 1 to 0 takes care of issue with CDS not having a gap if only a difference of 1 in exon (special condition)
                // now the donor splice junction is showing together with the acceptor
                if((pstart - (cdsStart + cdsWidth)) > 0) { //1
                    //System.out.println("CDS new start after break: " + pstart + ", " + (cdsStart + cdsWidth));
                    segList.add(new SegmentDisplay(start, end, cdsStart, cdsRelativeStart, cdsWidth));

                    start = pdraw.start;
                    end = pdraw.end;
                    cdsStart = pstart;
                    cdsRelativeStart = relstart;
                    cdsWidth = Math.abs(pdraw.end - pdraw.start) + 1;
                }
                else {
                    //System.out.println("CDS append: " + pstart + ", w: " + (Math.abs(pdraw.end - pdraw.start) + 1));
                    end = pdraw.end;
                    cdsWidth += Math.abs(pdraw.end - pdraw.start) + 1;
                }
            }
            if(flush) {
                if(cdsStart != -1) {
                    //System.out.println("CDS flush: " + cdsStart + ".." + (cdsStart+cdsWidth-1) + ", cdsw: " + cdsWidth);
                    segList.add(new SegmentDisplay(start, end, cdsStart, cdsRelativeStart, cdsWidth));
                    cdsStart = -1;
                }
            }
        }
        return segList;
    }
    
    //
    // Static Functions
    //
    
    // get genomic position, passing nt_exons, given a relative, to 1, non-gapped position (e.g. value from RefSeq)
    protected static double getGenomicPos(double start, ArrayList<Position> lstExons) {
        int cnt = lstExons.size();
        double tgstart = start;
        for(Position psge : lstExons) {
            cnt -= 1;
            int val = Math.abs(psge.end - psge.start) + 1;
            if(start <= val || cnt == 0) {
                if(cnt == 0 && start > val)
                    //Stop the for loop and continue
                    //Tappas.getApp().logger.logInfo("WARNING: Position is located past end of aligned range, by " + (start - val) + " bases.");
                    continue;
                tgstart = psge.start + start - 1;
                break;
            } 
            else
                start -= val;
        }
        return tgstart;
    }
    // get relative, to 1, non-gapped position given a relative, to nt_exon, position (e.g. specific transcript value from GRCm38)
    // used to display position in tooltips so their value is the same as that of the non-aligned display
    protected static double getRelativePos(int start, ArrayList<Position> lstTransExons) {
        double pstart = 0;
        for(Position psge : lstTransExons) {
            if(start <= psge.end) {
                pstart += start - psge.start + 1;
                break;
            }
            pstart += Math.abs(psge.end - psge.start) + 1;
        }
        return pstart;
    }
    // get total contigous size of exons in list
    protected static double getTotalExonsSize(ArrayList<Position> lstTransExons) {
        double size = 0;
        for(Position psge : lstTransExons)
            size += Math.abs(psge.end - psge.start) + 1;
        return size;
    }
    // return displayable string for given list of exons (in same order of list): 123454-123544, 123789-124005
    public static String getExonsString(ArrayList<Position> lstExons) {
        String str = "";
        for(Position pos : lstExons) {
            if(!str.isEmpty())
                str += ", ";
            str += pos.start + "-" + pos.end;
        }
        return str;
    }
    // get list of CDS exons given CDS start and end position (one based) and the list of transcript exons (genomic position)
    // the returned CDS exon values are in ascending genomic position regardless of strand
    // these values are normally used to compare two transcripts to see if the protein is identical based onthe CDS
    public static ArrayList<Position> getCDSExons(boolean negStrand, Position posCDS, ArrayList<Position> lstExons) {
        ArrayList<Position> lst = new ArrayList<>();
        if(posCDS.start != posCDS.end && lstExons.size() > 0) {
            // calculate CDS and exons length
            int strpos;
            int cdslen = posCDS.end - posCDS.start + 1;
            int exonslen = 0;
            for(Position pos : lstExons)
                exonslen += pos.end - pos.start + 1;
            
            // copy positions and sort ascending
            ArrayList<Position> lstSortedExons = new ArrayList<>();
            for(Position pos : lstExons)
                lstSortedExons.add(pos);
            Collections.sort(lstSortedExons);
            
            // build a SeqAlign object to use existing functionality
            SeqAlign seqAlign = new SeqAlign();
            for(Position pos : lstSortedExons)
                seqAlign.updGeneExonsList(pos.start, pos.end);
            
            // get genomic CDS start and end positions
            int cdsstrpos = posCDS.start;
            if(negStrand)
                cdsstrpos = (exonslen - posCDS.end + 1);
            strpos = (int) SeqAlign.getGenomicPos((double) cdsstrpos, lstSortedExons);
            
            // generate CDS exons based on CDS genomic position and exons
            boolean strfnd = false;
            for(SeqAlign.Position pos : lstSortedExons) {
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
                    lst.add(new SeqAlign.Position(pos.start, end));
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
                        lst.add(new SeqAlign.Position(strpos, end));
                    }
                }
                if(cdslen <= 0)
                    break;
            }
        }
        //System.out.println("CDS exons: " + getExonsString(lst));
        Collections.sort(lst);
        return lst;
    }
    // get list of UTR exons given start and end position (one based) and the list of transcript exons (genomic position)
    // the returned exon values are in ascending genomic position regardless of strand
    // these values are normally used to compare two transcripts to see if the UTRs are similar based on location and size
    public static ArrayList<Position> getUTRExons(boolean negStrand, Position posUTR, ArrayList<Position> lstExons) {
        ArrayList<Position> lst = new ArrayList<>();
        if(posUTR.start != posUTR.end && lstExons.size() > 0) {
            // calculate CDS and exons length
            int strpos;
            int utrlen = posUTR.end - posUTR.start + 1;
            int exonslen = 0;
            for(Position pos : lstExons)
                exonslen += pos.end - pos.start + 1;
            
            // copy positions and sort ascending
            ArrayList<Position> lstSortedExons = new ArrayList<>();
            for(Position pos : lstExons)
                lstSortedExons.add(pos);
            Collections.sort(lstSortedExons);
            
            // build a SeqAlign object to use existing functionality
            SeqAlign seqAlign = new SeqAlign();
            for(Position pos : lstSortedExons)
                seqAlign.updGeneExonsList(pos.start, pos.end);
            
            // get genomic UTR start and end positions
            int utrstrpos = posUTR.start;
            if(negStrand)
                utrstrpos = (exonslen - posUTR.end + 1);
            strpos = (int) SeqAlign.getGenomicPos((double) utrstrpos, lstSortedExons);
            
            // generate UTR exons based on UTR genomic position and exons
            boolean strfnd = false;
            for(SeqAlign.Position pos : lstSortedExons) {
                if(strfnd) {
                    // add exon partial or full based on cdslen left
                    int cnt = pos.end - pos.start + 1;
                    int end = pos.end;
                    if(cnt > utrlen) {
                        end = pos.start + utrlen - 1;
                        utrlen = 0;
                    }
                    else
                        utrlen -= cnt;
                    lst.add(new SeqAlign.Position(pos.start, end));
                }
                else {
                    // find exon where left position falls in
                    if(strpos >= pos.start && strpos <= pos.end) {
                        strfnd = true;
                        int cnt = pos.end - strpos + 1;
                        int end = pos.end;
                        if(cnt > utrlen) {
                            end = strpos + utrlen - 1;
                            utrlen = 0;
                        }
                        else
                            utrlen -= cnt;
                        lst.add(new SeqAlign.Position(strpos, end));
                    }
                }
                if(utrlen <= 0)
                    break;
            }
        }
        //System.out.println("UTR exons: " + getExonsString(lst));
        Collections.sort(lst);
        return lst;
    }
    // get list of feature exons given feature start and end position (one based) and the list of transcript exons (genomic position)
    // the returned feature exon values are in ascending genomic position regardless of strand
    public static ArrayList<Position> getFeatureExons(boolean negStrand, Position posFeature, ArrayList<Position> lstExons) {
        ArrayList<Position> lst = new ArrayList<>();
        // features can be given as a single nucleotide start==end
        if(lstExons.size() > 0) {
            // calculate feature and exons length
            int strpos;
            int flen = posFeature.end - posFeature.start + 1;
            int exonslen = 0;
            for(Position pos : lstExons)
                exonslen += pos.end - pos.start + 1;
            
            // copy positions and sort ascending
            ArrayList<Position> lstSortedExons = new ArrayList<>();
            for(Position pos : lstExons)
                lstSortedExons.add(pos);
            Collections.sort(lstSortedExons);
            
            // build a SeqAlign object to use existing functionality
            SeqAlign seqAlign = new SeqAlign();
            for(Position pos : lstSortedExons)
                seqAlign.updGeneExonsList(pos.start, pos.end);
            
            // get genomic start and end positions
            int fstrpos = posFeature.start;
            if(negStrand)
                fstrpos = (exonslen - posFeature.end + 1);
            strpos = (int) SeqAlign.getGenomicPos((double) fstrpos, lstSortedExons);
            
            // generate CDS exons based on CDS genomic position and exons
            boolean strfnd = false;
            for(SeqAlign.Position pos : lstSortedExons) {
                if(strfnd) {
                    // add exon partial or full based on cdslen left
                    int cnt = pos.end - pos.start + 1;
                    int end = pos.end;
                    if(cnt > flen) {
                        end = pos.start + flen - 1;
                        flen = 0;
                    }
                    else
                        flen -= cnt;
                    lst.add(new SeqAlign.Position(pos.start, end));
                }
                else {
                    // find exon where left position falls in
                    if(strpos >= pos.start && strpos <= pos.end) {
                        strfnd = true;
                        int cnt = pos.end - strpos + 1;
                        int end = pos.end;
                        if(cnt > flen) {
                            end = strpos + flen - 1;
                            flen = 0;
                        }
                        else
                            flen -= cnt;
                        lst.add(new SeqAlign.Position(strpos, end));
                    }
                }
                if(flen <= 0)
                    break;
            }
        }
        Collections.sort(lst);
        return lst;
    }
    
    //
    // Data Classes
    //
    
    // WARNING: will re-arrange if needed so that start <= end
    public static class Position implements Comparable<Position>{
        public int start;
        public int end;
        public Position(int start, int end) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
        }
        @Override
        public int compareTo(Position pos) {
            return ((start > pos.start)? 1 : (start < pos.start)? -1 : 0);
        }
    }
    public static class SegmentDisplay {
        public double start;
        public double end;
        public double x;
        public double xRelative;
        public double width;
        public SegmentDisplay(double start, double end, double x, double xRelative, double width) {
            this.start = start;
            this.end = end;
            this.x = x;
            this.xRelative = xRelative;
            this.width = width;
        }
    }
}
