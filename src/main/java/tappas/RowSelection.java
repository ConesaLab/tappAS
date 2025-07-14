/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class RowSelection {
    public static final String ARG_PREFIX = "ARG:";
    public static enum ActionType { SELECT, DESELECT, INVERT };
    
    public static void selectRows(DlgSelectRows.Params results, List<ColumnDefinition> lstFields, 
                            ObservableList<Object> data, String className) {
        System.out.println("rowSelect by: " + results.field);
        App app = Tappas.getApp();

        // get field definition for selected field
        ColumnDefinition cd = ColumnDefinition.getColDefFromId(results.field, lstFields);
        if(cd != null) {
            try {
                // get method to execute from property name
                Class<?> c = Class.forName(className);
                /* debug display
                for (Method method : c.getMethods()) {
                    String name = method.getName();
                    Type returnType = method.getReturnType();
                    System.out.println("'" + name + "' : " + returnType);
                }
                */
                Method mIsSelected = c.getMethod("isSelected");
                Method mSelectedProperty = c.getMethod("SelectedProperty");
                Method mGetData;
                String arg = "";
                if(cd.propName.startsWith(ARG_PREFIX)) {
                    // don't check, just generate exception if messed up
                    String[] fields = cd.propName.substring(ARG_PREFIX.length()).split("\t");
                    arg = fields[1].trim();
                    mGetData = c.getMethod("get" + fields[0], String.class);
                }
                else
                    mGetData = c.getMethod("get" + cd.propName);
                Type returnType = mGetData.getReturnType();
                System.out.println("get" + cd.propName + ": " + returnType.getTypeName());

                // check if request to start new selection
                if(results.newSelect) {
                    for(Object row : data) {
                        SimpleBooleanProperty sel =  (SimpleBooleanProperty) mSelectedProperty.invoke(row);
                        sel.set(!results.select);
                    }
                }

                int cnt = 0;
                int totalSelCnt = 0;
                for(Object row : data) {
                    boolean selected = (boolean)mIsSelected.invoke(row);
                    if(selected)
                        totalSelCnt++;
                    if(selected != results.select) {
                        boolean result = false;
                        
                        // check criteria
                        if(!arg.isEmpty()) {
                            if(returnType.getTypeName().equals(String.class.getName())) {
                                // special cases:
                                // BOOLEAN: cell has "YES" or "NO"
                                // STRAND: cell has "+" or "-"
                                // LIST: cell has list of items "a;b;c;..."
                                if(results.compType.equals(CompType.BOOL))
                                    result = results.isCriteriaMet(((String) mGetData.invoke(row, arg)).equals("YES"));
                                else if(results.compType.equals(CompType.STRAND))
                                    result = results.isCriteriaMet(((String) mGetData.invoke(row, arg)).equals("+"));
                                else if(results.compType.equals(CompType.LIST) || results.compType.equals(CompType.LISTIDVALS)) {
                                    ArrayList<String> lstSelections = new ArrayList<>();
                                    String values = (String) mGetData.invoke(row, arg);
                                    String[] items = values.split(";");
                                    for(String item : items)
                                        lstSelections.add(item.trim());
                                    result = results.isCriteriaMet(lstSelections);
                                }
                                else
                                    result = results.isCriteriaMet((String) mGetData.invoke(row, arg));
                            }
                            else if(returnType.getTypeName().equals(Double.class.getName()))
                                result = results.isCriteriaMet((double) mGetData.invoke(row, arg));
                            else if(returnType.getTypeName().equals(Integer.class.getName()))
                                result = results.isCriteriaMet((Integer) mGetData.invoke(row, arg));
                        }
                        else {
                            if(returnType.getTypeName().equals(String.class.getName())) {
                                // special cases:
                                // BOOLEAN: cell has "YES" or "NO"
                                // STRAND: cell has "+" or "-"
                                // LIST: cell has list of items "a,b,c,..."
                                if(results.compType.equals(CompType.BOOL))
                                    result = results.isCriteriaMet(((String) mGetData.invoke(row)).equals("YES"));
                                else if(results.compType.equals(CompType.STRAND))
                                    result = results.isCriteriaMet(((String) mGetData.invoke(row)).equals("+"));
                                else if(results.compType.equals(CompType.LIST) || results.compType.equals(CompType.LISTIDVALS)) {
                                    ArrayList<String> lstSelections = new ArrayList<>();
                                    String values = (String) mGetData.invoke(row);
                                    String[] items = values.split(",");
                                    for(String item : items)
                                        lstSelections.add(item.trim());
                                    result = results.isCriteriaMet(lstSelections);
                                }
                                else
                                    result = results.isCriteriaMet((String) mGetData.invoke(row));
                            }
                            else if(returnType.getTypeName().equals(Double.class.getName()))
                                result = results.isCriteriaMet((double) mGetData.invoke(row));
                            else if(returnType.getTypeName().equals(Integer.class.getName()))
                                result = results.isCriteriaMet((Integer) mGetData.invoke(row));
                        }

                        // check if criteria met and change selection if so
                        if(result) {
                            SimpleBooleanProperty sel =  (SimpleBooleanProperty) mSelectedProperty.invoke(row);
                            sel.set(results.select);
                            cnt++;
                            if(results.select)
                                totalSelCnt++;
                            else
                                totalSelCnt--;
                        }
                    }
                }
                String additional = results.select? " additional" : "";
                String msg = (cnt == 0)? "No" + additional + " rows " + (results.select? "selected." : "deselected.") : (results.select? "Selected " : "Deselected ") + NumberFormat.getInstance().format(cnt) + additional + " row(s).";
                msg += "\nTotal of " + totalSelCnt + " rows selected.\n'Hide unselected rows' checkbox will be " + ((totalSelCnt > 0)? "checked.\n" : "unchecked.\n");
                app.ctls.alertInformation("Row Selection", msg);
            }
            catch(Exception e) { 
                app.logError("Unable to process row selection request: " + e.getMessage());
            }
        }
        else
            app.logError("Unable to find selected field (" + results.field + ")");
    }
    public static int getSelectedRowsCount(ObservableList<Object> data, String className) {
        int cnt = 0;
        App app = Tappas.getApp();
        try {
            // get method to execute from property name
            Class<?> c = Class.forName(className);
            Method mIsSelected = c.getMethod("isSelected");
            for(Object row : data) {
                boolean selected = (boolean)mIsSelected.invoke(row);
                if(selected)
                    cnt++;
            }
        }
        catch(Exception e) { 
            app.logError("Unable to process get selected rows count request: " + e.getMessage());
        }
        return cnt;
    }

    public static List<Integer> getSelectRowIdxs(ObservableList data, String className) {
        List<Integer> lstRows = new ArrayList<>();
        App app = Tappas.getApp();
        try {
            // get method to execute from property name
            Class<?> c = Class.forName(className);
            Method mIsSelected = c.getMethod("isSelected");
            int rowNum = 0;
            for(Object row : data) {
                if((boolean)mIsSelected.invoke(row))
                    lstRows.add(rowNum);
                rowNum++;
            }
        }
        catch(Exception e) { 
            app.logError("Unable to process row selection request: " + e.getMessage());
        }
        return lstRows;
    }
}
