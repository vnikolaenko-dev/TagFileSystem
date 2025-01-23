package org.example.app.components;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import org.example.util.FileNode;

import java.util.List;

public class FileTableComponent {
    private TableView<FileNode> tableView = new TableView<>();
    public FileTableComponent(List<FileNode> fileNodes) {
        TableColumn<FileNode, String> pathColumn = new TableColumn<>("Путь");
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("file"));

        TableColumn<FileNode, String> tagColumn = new TableColumn<>("Тэг");
        tagColumn.setCellValueFactory(new PropertyValueFactory<>("tags"));

        tableView.getColumns().add(pathColumn);
        tableView.getColumns().add(tagColumn);
        tableView.getItems().addAll(fileNodes);
    }

    public TableView<FileNode> getTableView() {
        return tableView;
    }

    public void setTableView(TableView<FileNode> tableView) {
        this.tableView = tableView;
    }
}
