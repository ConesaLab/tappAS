/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Cluster extends AppObject {
    // MIN_SIMILARITY_COUNT should be set differently for genes and transcripts
    private int MIN_SIMILAR_TERMS = 4;
    private int MIN_CLUSTER_NODES = 4;
    private int MIN_SEEDING_NODES = 4;
    private double KAPPA_THRESHOLD = 0.35;
    private double SEED_PCT_THRESHOLD = 0.5;
    private double GROUP_PCT_THRESHOLD = 0.5;
    
    public Cluster(DlgClusterAnalysis.Params params) {
        super(null, null);
        
        if(params.similarTerms >= 0)
            MIN_SIMILAR_TERMS = params.similarTerms;
        if(params.kappa >= 0.0 && params.kappa <= 1.0)
            KAPPA_THRESHOLD = params.kappa;
        if(params.seeds >= 2)
            MIN_SEEDING_NODES = params.seeds;
        if(params.clusterNodes >= 2)
            MIN_CLUSTER_NODES = params.clusterNodes;
        if(params.linkage >= 0.0 && params.linkage <= 1.0) {
            SEED_PCT_THRESHOLD = params.linkage;
            GROUP_PCT_THRESHOLD = params.linkage;
        }
    }
    public double getMinKappa() { return KAPPA_THRESHOLD; }
    public String getString(ArrayList<ClusterDef> lst) {
        String str = "";
        int cnt = 0;
        for(ClusterDef cd : lst) {
            cnt += cd.members.size();
            str += "Cluster: " + cd.num + ". Members (" + cd.members.size() + "): ";
            for (Integer member : cd.members)
                str += member + ", ";
            str += "\n";
        }
        str += "\nTotal members: " + cnt + "\n";
        return str;
    }
    public ClusterData getClusters(int cols, ArrayList<BitSet> lstTermMembers) {
        ArrayList<ClusterDef> lstPreClusters = new ArrayList<>();
        ArrayList<ClusterDef> lstClusters = new ArrayList<>();
        ArrayList<ArrayList<Integer>> lstClusterNums = new ArrayList<>();
        ArrayList<double[]> lstKappas = new ArrayList<>();
        ArrayList<int[]> lstShared = new ArrayList<>();
        ClusterData cd = new ClusterData(lstKappas, lstShared, lstClusterNums, lstClusters);
        if(lstTermMembers != null && lstTermMembers.size() > 0) {
            int terms = lstTermMembers.size();
            double [][] kappas = new double[terms][terms];
            int [][] shared = new int[terms][terms];
            getKappas(cols, lstTermMembers, kappas, shared);
            /*System.out.println("Kappas (" + kappas.length + "): \n");
            String str = "";
            for(int row = 0; row < kappas.length; row++) {
                str = "[" + row + "]: ";
                for(int col = 0; col < kappas[row].length; col++)
                    str += kappas[row][col] + ", ";
                System.out.println(str);
            }*/
            
            // get initial seeds
            int cnum = 1;
            for(int row = 0; row < kappas.length; row++) {
                ArrayList<Integer> members = getSeedMembers(row, kappas);
                if(members != null)
                    lstPreClusters.add(new ClusterDef(cnum++, members));
                lstKappas.add(kappas[row]);
                lstShared.add(shared[row]);
            }
            
            // merge clusters
            mergeClusters(lstPreClusters);

            // set cluster(s) for each member
            int num = 1;
            int cnt = lstTermMembers.size();
            for(int i = 0; i < cnt; i++)
                lstClusterNums.add(new ArrayList<>());
            for(ClusterDef cdef : lstPreClusters) {
                if(cdef.members.size() >= MIN_CLUSTER_NODES) {
                    lstClusters.add(cdef);
                    for(int termIdx : cdef.members)
                        lstClusterNums.get(termIdx).add(num);
                    cdef.num = num++;
                }
            }
        }
        return cd;
    }
    private void getKappas(int cols, ArrayList<BitSet> termGenes, double [][] kappas, int [][] shared) {
        int terms = termGenes.size();
        app.logDebug("Calculate kappas for " + terms + " terms by " + cols + " transcripts or genes matrix.");

        // get kappa values
        KappaData kappaData;
        int mid = (terms + 1) / 2;
        for(int a = 0; a < terms; a++) {
            for(int b = 0; b < terms; b++) {
                if(a == b)
                    kappas[a][b] = 1;
                else {
                    kappaData = calcKappa(cols, termGenes.get(a), termGenes.get(b));
                    kappas[a][b] = kappaData.kappa;
                    shared[a][b] = kappaData.shared;
                }
            }
        }
    }
    
    // using bitsets turned out to have an unexpected issue, all the bits used to store the set
    // (based on 64 bit storage unit - will vary) are included in the computations
    // decided to leave it as is and just step through the bits one by one just like an array
    private KappaData calcKappa(int cols, BitSet a, BitSet b) {
        int ones = 0;
        int zeros = 0;
        int row1 = 0;
        int col1 = 0;
        int row0 = 0;
        int col0 = 0;
        boolean aval;
        for(int col = 0; col < cols; col++) {
            aval = a.get(col);
            if(aval == b.get(col)) {
                if(aval) { ones++; row1++; col1++; }
                else { zeros++; row0++; col0++; }
            }
            else {
                if(aval) { row0++; col1++; }
                else { row1++; col0++; }
            }
        }
        
        double Kab = 0;
        if(ones >= MIN_SIMILAR_TERMS) {
            double Tab = (double) cols;
            double Oab = (ones + zeros) / Tab;
            double Aab = ((double)row1 * col1 + (double)row0 * col0) / (Tab * Tab);
            Kab = (Oab - Aab) / (1 - Aab);
            //if(Kab > 0.1 && ones > 10)
            //    System.out.println("tab: " + Tab + ", oab: " + Oab + ", aab: " + Aab + ", kab: " + Kab + ", ones: " + ones);
        }
        return new KappaData(Kab, ones);
    }
    private ArrayList<Integer> getSeedMembers(int idx, double[][] kappas) {
        ArrayList<Integer> seedMembers = null;
        int cnt = 0;
        int n = kappas.length;
        int[] members = new int[n];
        // check if it meets the initial group membership requirement
        for(int i = 0; i < n; i++) {
            if(i != idx) {
                if(kappas[idx][i] >= KAPPA_THRESHOLD)
                    members[cnt++] = i;
            }          
        }
        //System.out.println("idx: " + idx + ", mbrs: " + cnt + ", min: " + MIN_KAPPA_COUNT);
        if(cnt >= MIN_SEEDING_NODES) {
            double mrcnt = cnt * (cnt - 1); // we are double checking ab and ba ( / 2);
            int mbr, grpmbr;
            int subcnt = 0;
            for(int mbridx = 0; mbridx < cnt; mbridx++) {
                mbr = members[mbridx];
                for(int i = 0; i < cnt; i++) {
                    grpmbr = members[i];
                    if(grpmbr != mbr) {
                        //System.out.println("mbr: " + mbr + ", grpmbr: " + grpmbr + ", val: " + kappas[mbr][grpmbr]);
                        if(kappas[mbr][grpmbr] >= KAPPA_THRESHOLD)
                            subcnt++;
                    }          
                }
            }
            if(((double)subcnt / mrcnt) > SEED_PCT_THRESHOLD) {
                seedMembers = new ArrayList<>();
                seedMembers.add(idx);
                //System.out.print("Seed " + idx + ": " + idx);
                for(int i = 0; i < cnt; i++) {
                    seedMembers.add(members[i]);
                    //System.out.print(", " + members[i]);
                }
                //System.out.print("\n");
            }
        }
        return seedMembers;
    }
    private void mergeClusters(ArrayList<ClusterDef> lst) {
        int cnt = lst.size();
        for(int i = 0; i < cnt; i++) {
            mergeCluster(i, lst);
            cnt = lst.size();
        }
    }
    private void mergeCluster(int idx, ArrayList<ClusterDef> lst) {
        ArrayList<ClusterData> lstnew = new ArrayList<>();
        int cnt = lst.size();
        //System.out.println("mergClusters for idx [" + idx + "] " + lst.get(idx).num);
        for(int i = 1; (idx + i) < cnt; i++) {
            HashSet src = new HashSet();
            src.addAll(lst.get(idx).members);
            HashSet dst = new HashSet();
            dst.addAll(lst.get(idx+i).members);
            Set<Integer> intersection = new HashSet<>(src);
            intersection.retainAll(dst);
            // Note: not sure if we should be using the min size of the two clusters
            if((double)intersection.size() / Math.min(src.size(), dst.size()) > GROUP_PCT_THRESHOLD) {
                Set<Integer> union = new HashSet<>(src);
                union.addAll(dst);
                //System.out.println("Can merge [" + idx + "] " + lst.get(idx).num + " with [" + (idx+i) + "] " + lst.get(idx+i).num);
                ArrayList<Integer> members = lst.get(idx).members;
                members.clear();
                members.addAll(union);
                lst.remove(idx+i);
                mergeCluster(idx, lst);
                break;
            }
        }
    }

    //
    // Data Classes
    //
    
    public static class ClusterData {
        ArrayList<double[]> kappas;
        ArrayList<int[]> shared;
        ArrayList<ArrayList<Integer>> clusterNums;
        ArrayList<ClusterDef> clusters;

        public ClusterData(ArrayList<double[]> kappas, ArrayList<int[]> shared, ArrayList<ArrayList<Integer>> clusterNums, ArrayList<ClusterDef> clusters) {
            this.kappas = kappas;
            this.shared = shared;
            this.clusterNums = clusterNums;
            this.clusters = clusters;
        }
    }
    public static class ClusterDef {
        int num;
        ArrayList<Integer> members;
        
        public ClusterDef(int num, ArrayList<Integer> members) {
            this.num = num;
            this.members = members;
        }
    }
    public static class KappaData {
        double kappa;
        int shared;
        
        public KappaData(double kappa, int shared) {
            this.kappa = kappa;
            this.shared = shared;
        }
    }
}
