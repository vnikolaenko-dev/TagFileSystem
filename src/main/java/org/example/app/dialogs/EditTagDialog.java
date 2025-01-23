package org.example.app.dialogs;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.app.MainApp;
import org.example.app.alerts.Alert;
import org.example.dao.service.TagService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class EditTagDialog {
    /**
     * Метод для отображения диалога редактирования тегов, в котором пользователи могут создавать и удалять теги.
     *
     * @param app Приложение, в контексте которого показывается диалог. Используется для обновления данных в приложении.
     * @param session Сессия базы данных, используется для взаимодействия с данными.
     * @param availableTags Список доступных тегов для отображения и выбора в диалоге.
     * @param tags Множество тегов, которые могут быть выбраны или добавлены/удалены.
     */
    public static void showEditTagDialog(MainApp app, ODatabaseSession session, ObservableList<String> availableTags, HashSet<String> tags) {
        // Создание нового окна для диалога
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Создание тэга"); // Заголовок окна

        // Создание и настройка GridPane для размещения элементов в диалоге
        GridPane grid = new GridPane();
        grid.setHgap(10); // Установка горизонтальных отступов между элементами
        grid.setVgap(10); // Установка вертикальных отступов между элементами
        grid.setPadding(new Insets(10)); // Установка отступов внутри контейнера

        // Список доступных тегов
        ListView<String> tagsListView = new ListView<>();
        availableTags = FXCollections.observableArrayList(tags); // Заполнение ListView доступными тегами
        tagsListView.setItems(availableTags);

        // Элементы для создания нового тега
        Label newTag = new Label("Создать новый тэг:");
        TextField tagField = new TextField(); // Поле для ввода новых тегов
        Button addButton = new Button("Создать"); // Кнопка для создания тега
        addButton.setOnAction(e -> {
            // Действие при нажатии на кнопку "Создать"
            String tagsText = tagField.getText();
            // Разделение текста на теги и очистка от пробелов
            List<String> newTags = Arrays.stream(tagsText.split(","))
                    .map(String::trim)          // Удаляем пробелы вокруг каждого тега
                    .filter(s -> !s.isEmpty())  // Фильтруем пустые строки
                    .collect(Collectors.toList()); // Собираем в список

            // Создание тегов в базе данных и отображение результата
            if (TagService.createTags(session, (ArrayList<String>) newTags) != null) {
                Alert.showAlert("Тэг успешно создан.", app); // Успешное создание
                app.updateTagsList(); // Обновляем список тегов в приложении
            } else {
                Alert.showErrorAlert("Ошибка при создании тэга.", app); // Ошибка при создании
            }
        });
        grid.addRow(0, newTag);
        grid.addRow(1, tagField, addButton); // Добавляем элементы для создания тега в GridPane

        // Элементы для удаления существующего тега
        Label removeTag = new Label("Удалить тэг:");
        TextField removeTagField = new TextField(); // Поле для ввода тега, который нужно удалить
        Button removeButton = new Button("Удалить"); // Кнопка для удаления тега
        removeButton.setOnAction(e -> {
            // Действие при нажатии на кнопку "Удалить"
            String tagsText = removeTagField.getText();
            // Разделение текста на теги и очистка от пробелов
            List<String> newTags = Arrays.stream(tagsText.split(","))
                    .map(String::trim)          // Удаляем пробелы вокруг каждого тега
                    .filter(s -> !s.isEmpty())  // Фильтруем пустые строки
                    .collect(Collectors.toList()); // Собираем в список

            // Удаление тегов из базы данных и отображение результата
            if (TagService.removeTags(session, (ArrayList<String>) newTags)) {
                Alert.showAlert("Тэг успешно удален.", app); // Успешное удаление
                app.updateTagsList(); // Обновляем список тегов в приложении
            } else {
                Alert.showErrorAlert("Ошибка при удалении тэга, проверьте название тэга и повторите попытку.", app); // Ошибка при удалении
            }
        });
        grid.addRow(2, removeTag);
        grid.addRow(3, removeTagField, removeButton); // Добавляем элементы для удаления тега в GridPane
        grid.addRow(4, tagsListView); // Добавляем список тегов для отображения

        // Создание и отображение сцены с настройками стилей
        Scene scene = new Scene(grid);
        scene.getStylesheets().add(app.getClass().getResource("/static/style.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.show(); // Показываем окно диалога
    }
}
