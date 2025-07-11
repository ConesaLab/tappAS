/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.util.Arrays;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

// WARNING: All these functions purposely generate an exception
//          if an empty array of values is passes since the result is undefined
public class Stats {
    // note: array will be sorted in ascending order
    static double getMedian(double[] vals) {
        double median;
        int vlen = vals.length;
        Arrays.sort(vals);
        if(vlen == 0)
            median = 0.0;
        else if(vlen == 1)
            median = vals[0];
        else {
            int pos = Math.floorDiv(vlen, 2);
            if((vlen & 1) != 0)
                median = vals[pos];
            else
                median = (vals[pos] + vals[pos-1]) / 2;
        }
        return median;
    }
    // get 1st, 2nd or 3rd quartile, qt = 1 to 3
    // note: array will be sorted in ascending order
    static double getQuartile(double[] vals, int qt) {
        if(qt == 2)
            return getMedian(vals);
        else if(qt != 1 && qt != 3)
            return -1.0;
        
        double qtile;
        int vlen = vals.length;
        Arrays.sort(vals);
        if(vlen == 0)
            qtile = 0.0;
        else if(vlen == 1)
            qtile = vals[0];
        else if(vlen == 2) {
            if(qt == 1)
                qtile = vals[0];
            else
                qtile = vals[1];
        }
        else if(vlen == 3) {
            if(qt == 1)
                qtile = vals[0];
            else
                qtile = vals[2];
        }
        else {
            int idx = Math.floorDiv(vlen, 2);
            if((vlen & 1) == 0)
                idx--;
            int idx1start = 0;
            int idx1end = idx - 1;
            int idx3start = idx + 1;
            int idx3end = vlen - 1;
            if((vlen & 1) == 0)
                idx1end++;
            int qtlen, qoffset;
            if(qt == 1) {
                qoffset = 0;
                qtlen = idx1end - idx1start + 1;
            }
            else {
                qoffset = idx + 1;
                qtlen = idx3end - idx3start + 1;
            }
            int pos = qoffset + Math.floorDiv(qtlen, 2);
            if((qtlen & 1) != 0)
                qtile = vals[pos];
            else
                qtile = (vals[pos] + vals[pos-1]) / 2;
        }
        return qtile;
    }
    static double getMin(double[] vals) {
        double min = vals.length > 0? vals[0] : 0;
        for(int i = 1; i < vals.length; i++)
            min = Math.min(min, vals[i]);
        return min;
    }
    static double getMax(double[] vals) {
        double max = vals.length > 0? vals[0] : 0;
        for(int i = 1; i < vals.length; i++)
            max = Math.max(max, vals[i]);
        return max;
    }
    static double getMean(double[] vals) {
        double mean = 0.0;
        for(int i = 0; i < vals.length; i++)
            mean += vals[i];
        mean /= vals.length;
        return mean;
    }
    static BoxPlotData getBoxPlotData(double[] vals) {
        return new BoxPlotData(vals);
    }
    
    //
    // Data Classes
    //
    
    // The whiskers can be the min and max values - no outliers (all data included)
    // or the minOutliers, maxOutliers values - outliers must be drawn outside this area
    public static class BoxPlotData {
        public double[] vals;
        public double min, max, median, q1, q3, iqr, lowerFence, upperFence, lowerWhisker, upperWhisker;
        public double upperOutliersCnt, lowerOutliersCnt;
        BoxPlotData(double[] vals) {
            Arrays.sort(vals);
            this.vals = vals;
            this.min = getMin(vals);
            this.max = getMax(vals);
            this.median = getMedian(vals);
            this.q1 = getQuartile(vals, 1);
            this.q3 = getQuartile(vals, 3);
            this.iqr = q3 - q1;
            this.lowerFence = q1 - 1.5 * this.iqr;
            this.upperFence = q3 + 1.5 * this.iqr;
            this.upperOutliersCnt = 0;
            this.lowerOutliersCnt = 0;
            for(int i = 0; i < vals.length; i++) {
                if(vals[i] > this.upperFence)
                    this.upperOutliersCnt++;
            }
            this.upperWhisker = this.max;
            if(this.upperOutliersCnt > 0)
                this.upperWhisker = this.upperFence;
            for(int i = vals.length - 1; i >= 0; i--) {
                if(vals[i] < this.lowerFence)
                    this.lowerOutliersCnt++;
            }
            this.lowerWhisker = this.min;
            if(this.lowerOutliersCnt > 0)
                this.lowerWhisker = this.lowerFence;
        }
    }
}
