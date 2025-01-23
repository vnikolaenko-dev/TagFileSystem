package org.example.app.components;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.example.util.FileNode;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileInfoComponent {
    private Label fileNameLabel;
    private Label filePathLabel;
    private Label fileSizeLabel;
    private Label creationDateLabel;
    private Label modificationDateLabel;
    private Pane infoPane;
    public FileInfoComponent() {
        infoPane = new Pane();
        infoPane.setVisible(false);
        infoPane.setPrefWidth(400);
        infoPane.getStyleClass().add("infoPane");
        infoPane.setPadding(new Insets(10));
        // Создаем метки для информации о файле
        fileNameLabel = new Label();
        filePathLabel = new Label();
        fileSizeLabel = new Label();
        creationDateLabel = new Label();
        modificationDateLabel = new Label();
        infoPane.getChildren().addAll(fileNameLabel, filePathLabel, fileSizeLabel, creationDateLabel, modificationDateLabel);
    }

    public Pane getInfoPane() {
        return infoPane;
    }

    public void showFileInfo(FileNode node, Canvas canvas) {
        File file = node.getFile();
        // Получаем атрибуты файла
        try {
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

            // Обновляем метки с информацией о файле
            fileNameLabel.setText("Имя файла: " + file.getName());
            fileNameLabel.setStyle("-fx-text-fill: white; -fx-padding: 10px");
            filePathLabel.setText("\nПолный путь: " + file.getAbsolutePath());
            filePathLabel.setStyle("-fx-text-fill: white; -fx-padding: 10px");

            if (file.isFile()){
                fileSizeLabel.setText("\n\nРазмер: " + file.length() + " байт");
                fileSizeLabel.setStyle("-fx-text-fill: white; -fx-padding: 10px");
            } else {
                fileSizeLabel.setText("");
            }

            creationDateLabel.setText("\n\n\nДата создания: " + dateFormat.format(new Date(attributes.creationTime().toMillis())));
            creationDateLabel.setStyle("-fx-text-fill: #b4b4b4; -fx-padding: 10px");
            modificationDateLabel.setText("\n\n\n\nДата последней модификации: " + dateFormat.format(new Date(attributes.lastModifiedTime().toMillis())));
            modificationDateLabel.setStyle("-fx-text-fill: #b4b4b4; -fx-padding: 10px");


            infoPane.setLayoutX(canvas.getWidth() - 400);
            infoPane.setLayoutY(0);
            // Делаем панель видимой
            infoPane.setVisible(true);
            infoPane.toFront();
        } catch (Exception e) {
            // e.printStackTrace();
            // Обработка ошибок (например, если файл не существует)
        }

    }
}
