    /*
     * Copyright (c) 2015 Memorial Sloan-Kettering Cancer Center.
     *
     * This library is distributed in the hope that it will be useful, but WITHOUT
     * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
     * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
     * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
     * obligations to provide maintenance, support, updates, enhancements or
     * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
     * liable to any party for direct, indirect, special, incidental or
     * consequential damages, including lost profits, arising out of the use of this
     * software and its documentation, even if Memorial Sloan-Kettering Cancer
     * Center has been advised of the possibility of such damage.
     */

    /*
     * This file is part of cBioPortal.
     *
     * cBioPortal is free software: you can redistribute it and/or modify
     * it under the terms of the GNU Affero General Public License as
     * published by the Free Software Foundation, either version 3 of the
     * License.
     *
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU Affero General Public License for more details.
     *
     * You should have received a copy of the GNU Affero General Public License
     * along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */

    /**
     * Bejamini Hochberg FDR Correction Code.
     * <p/>
     * For details, refer to:  http://www.tau.ac.il/cc/pages/docs/sas8/stat/chap43/sect14.htm
     *
     * @author Steven Maere, Karel Heymans, and Ethan Cerami
     */

package tappas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class BenjaminiHochbergFDR {

    /**
     * the raw p-values that were given as input for the constructor.
     */
    private Double[] pvalues;

    /**
     * the adjusted p-values ordened in ascending order.
     */
    private double[] adjustedPvalues;

    /**
     * the number of tests.
     */
    private int m;

    /**
     * The index array
     */
    private Integer[] indexes; 
    /**
     * Constructor.
     *
     * @param p P-Values.
     */
    public BenjaminiHochbergFDR(ArrayList<Double> p) {
        Double[] target = new Double[p.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = p.get(i);
        }
        
        this.pvalues = target;
        this.m = pvalues.length;
        this.adjustedPvalues = new double[m];
    }
    
    public BenjaminiHochbergFDR(double[] p) {
        Double[] target = new Double[p.length];
        for (int i = 0; i < target.length; i++) {
            target[i] = p[i];
        }
        
        this.pvalues = target;
        this.m = pvalues.length;
        this.adjustedPvalues = new double[m];
    }
    
    public BenjaminiHochbergFDR(Double[] p) {
        this.pvalues = p;
        this.m = pvalues.length;
        this.adjustedPvalues = new double[m];
    }

    /**
     * method that calculates the Benjamini and Hochberg correction of
     * the false discovery rate.
     */
    public void calculate() {

        // order the pvalues.
        ArrayIndexComparator comparator = new ArrayIndexComparator(pvalues);
        indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);
        Arrays.sort(pvalues);
        // iterate through all p-values:  largest to smallest
        for (int i = m - 1; i >= 0; i--) {
            if(i==10)
                System.out.println("Hi");
            if (i == m - 1) {
                adjustedPvalues[i] = pvalues[i];
            } else {
                double unadjustedPvalue = pvalues[i];
                int divideByM = i + 1;
                double left = adjustedPvalues[i + 1];
                double right = (m / (double) divideByM) * unadjustedPvalue;
                adjustedPvalues[i] = Math.min(left, right);
            }
        }
    }

    /**
     * getter for the ordened p-values.
     *
     * @return String[] with the ordened p-values.
     */
    public double[] getOrdenedPvalues() {
        double[] target = new double[pvalues.length];
        for (int i = 0; i < target.length; i++) {
            target[i] = pvalues[i];
        }
        return target;
    }
    
        /**
     * getter for the ordened p-values.
     *
     * @return String[] with the ordened p-values.
     */
    public Integer[] getIndexes() {
        return indexes;
    }

    /**
     * getter for the adjusted p-values.
     *
     * @return String[] with the adjusted p-values.
     */
    public double[] getAdjustedPvalues() {
        return adjustedPvalues;
    }
    
    public class ArrayIndexComparator implements Comparator<Integer>
    {
        private final Double[] array;

        public ArrayIndexComparator(Double[] array)
        {
            this.array = array;
        }

        public Integer[] createIndexArray()
        {
            Integer[] indexes = new Integer[array.length];
            for (int i = 0; i < array.length; i++)
            {
                indexes[i] = i; // Autoboxing
            }
            return indexes;
        }

        @Override
        public int compare(Integer index1, Integer index2)
        {
             // Autounbox from Integer to int to use as array indexes
            return array[index1].compareTo(array[index2]);
        }
    }
}
