/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import tappas.DataApp.KeyValueData;

import java.util.HashMap;
        
/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabProjectProps extends SubTabBase {
    TableView tblProperties;
    ContextMenu cmSummary;
    
    public SubTabProjectProps(Project project) {
        super(project);
    }
    
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }
    @Override
    protected void onButtonRun() {
        app.ctlr.loadInputData();
    }
    @Override
    protected void onButtonExport() {
        app.export.exportTableData(tblProperties, true, "tappAS_"+app.data.project.getProjectName().trim()+"_ProjectProperties.tsv");
    }    
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        tblProperties = (TableView) tabNode.lookup("#tblProperties");

        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Rename Project...");
        item.setOnAction((event) -> { app.ctlr.changeProjectName(); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Change Input Data...");
        item.setOnAction((event) -> { app.ctlr.loadInputData();});
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        ObservableList<TableColumn> cols = tblProperties.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Key"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Value"));
        ObservableList<KeyValueData> data = FXCollections.observableArrayList();
        data.add(new KeyValueData("Name", project.getProjectName()));
        data.add(new KeyValueData("Internal Id", project.getProjectId()));
        DlgInputData.Params params = project.data.getParams();
        DataInputMatrix.ExpMatrixParams emParams = new DataInputMatrix.ExpMatrixParams(params.getParams());

        String refType = "";
        if(params.useAppRef && params.refType != null)
            refType = params.refType.name();
        data.add(new KeyValueData("Genus", params.genus));
        data.add(new KeyValueData("Species", params.species));
        data.add(new KeyValueData("Experiment Type", emParams.getExperimentTypeName()));
        if(refType.equals(DataApp.RefType.Demo.toString()))
            data.add(new KeyValueData("Experimental Design File", "Provided by Application"));
        else
            data.add(new KeyValueData("Experimental Design File", params.edFilepath));
        if(params.useAppRef) {
            data.add(new KeyValueData("Annotation Source", "Provided by Application"));
            if(!refType.isEmpty()) {
                String type = refType + (!params.refRelease.isEmpty()? (" Release " + params.refRelease) : "");
                data.add(new KeyValueData("Annotation Type", type));
            }
            else
                data.add(new KeyValueData("Annotation Type", "N/A"));
            data.add(new KeyValueData("Annotation File", params.refFile));
        }
        else {
            data.add(new KeyValueData("Annotation Source", "Provided by User"));
            data.add(new KeyValueData("Annotation File", params.afFilepath));
        }
        if(refType.equals(DataApp.RefType.Demo.toString()))
            data.add(new KeyValueData("Expression Matrix File", "Provided by Application"));
        else
            data.add(new KeyValueData("Expression Matrix File", params.emFilepath));
        int i = 1;
        for(DlgInputData.Params.ExpMatrixGroup gt : params.lstGroups) {
            data.add(new KeyValueData("Experimental Group " + i, gt.name));
            for(DlgInputData.Params.ExpMatrixTime ts : gt.lstTimes) {
                if(params.experimentType.equals(DataApp.ExperimentType.Two_Group_Comparison))
                    data.add(new KeyValueData("Samples", ts.lstSampleNames.toString().replace("[", "").replace("]", "")));
                else
                    data.add(new KeyValueData("Time " + ts.name + " Samples", ts.lstSampleNames.toString().replace("[", "").replace("]", "")));
            }
            i++;
        }
        data.add(new KeyValueData("Expression Level Filtering", params.filter? "Yes" : "No"));
        if(params.filter) {
            data.add(new KeyValueData("Low Count Cutoff", "" + params.filterValue));
            data.add(new KeyValueData("Coefficient of Variation Cutoff", "" + params.filterCOV));
        }
        int idx = DlgInputData.Params.getFilterListIndexById(params.filterList.name());
        data.add(new KeyValueData("Transcripts Filter", DlgInputData.Params.lstFilterLists.get(idx).name));
        if(!params.filterList.equals(DlgInputData.Params.FilterList.NONE))
            data.add(new KeyValueData("Transcripts Filter List File", params.lstFilepath));

        tblProperties.setItems(data);
        if(tblProperties.getItems().size() > 0)
            tblProperties.getSelectionModel().select(0);
        app.export.addCopyToClipboardHandler(tblProperties);
        tblProperties.setOnMouseClicked((event) -> {
            if(cmSummary != null)
                cmSummary.hide();
            cmSummary = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn"))
                        node = node.getParent();
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmSummary = new ContextMenu();
                        app.export.setupSimpleTableExport(tblProperties, cmSummary, false, "tappAS_"+app.data.project.getProjectName().trim()+"_ProjectProperties.tsv");
                        cmSummary.setAutoHide(true);
                        cmSummary.show(tblProperties, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        setFocusNode(tblProperties, true);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
