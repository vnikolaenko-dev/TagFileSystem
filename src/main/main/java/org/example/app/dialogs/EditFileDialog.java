package org.example.app.dialogs;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.app.MainApp;
import org.example.app.alerts.Alert;
import org.example.app.utils.FileUtil;

import java.util.HashSet;

public class EditFileDialog {
    /**
     * Метод для отображения диалога редактирования файла, где пользователь может просматривать информацию о файле
     * и редактировать связанные с ним теги.
     *
     * @param app Приложение, в контексте которого показывается диалог. Используется для обновления данных в приложении.
     * @param session Сессия базы данных, используется для взаимодействия с данными.
     * @param availableTags Список доступных тегов для отображения и выбора.
     * @param tags Множество текущих тегов, которые могут быть добавлены или удалены.
     */
    public static void showEditFileDialog(MainApp app, ODatabaseSession session, ObservableList<String> availableTags, HashSet<String> tags) {
        // Проверяем, выбран ли файл
        if (app.getCanvasComponent().selectedFileNode != null) {
            // Создание нового окна для диалога
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редактор файла"); // Заголовок окна

            // Создание и настройка GridPane для размещения элементов в диалоге
            GridPane grid = new GridPane();
            grid.setHgap(10); // Установка горизонтальных отступов между элементами
            grid.setVgap(10); // Установка вертикальных отступов между элементами
            grid.setPadding(new Insets(10)); // Установка отступов внутри контейнера

            grid.getStyleClass().add("root"); // Добавление стиля для элементов

            // Информация о файле
            Label nameLabel = new Label("Имя файла:");
            Label nameValue = new Label(app.getCanvasComponent().selectedFileNode.getFile().getName());
            Label pathLabel = new Label("Путь:");
            Label pathValue = new Label(app.getCanvasComponent().selectedFileNode.getFile().getAbsolutePath());
            Label sizeLabel = new Label("Размер:");
            Label sizeValue = new Label(FileUtil.getFileSize(app.getCanvasComponent().selectedFileNode.getFile())); // Получаем размер файла

            // Добавляем элементы с информацией о файле в GridPane
            grid.addRow(0, nameLabel, nameValue);
            grid.addRow(1, pathLabel, pathValue);

            // Если выбранный элемент является файлом, показываем его размер
            if (app.getCanvasComponent().selectedFileNode.getFile().isFile()) {
                grid.addRow(2, sizeLabel, sizeValue);
            }

            // Создаем ListView для отображения доступных тегов
            ListView<String> tagsListView = new ListView<>();
            availableTags = FXCollections.observableArrayList(tags); // Заполнение ListView доступными тегами
            tagsListView.setItems(availableTags);

            // Разрешаем множественный выбор тегов
            tagsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // Устанавливаем начальное значение для выбранных тегов (если они есть)
            if (app.getCanvasComponent().selectedFileNode != null && app.getCanvasComponent().selectedFileNode.tags != null) {
                for (String tag : app.getCanvasComponent().selectedFileNode.tags) {
                    int index = availableTags.indexOf(tag);
                    if (index != -1) {
                        tagsListView.getSelectionModel().select(index); // Выбираем соответствующие теги
                    }
                }
            }

            Label tagsLabel = new Label("Теги:"); // Метка для списка тегов
            // Получение выбранных тегов
            ObservableList<String> selectedTags = tagsListView.getSelectionModel().getSelectedItems();

            // Кнопки для сохранения и отмены
            Button saveButton = new Button("Сохранить");
            saveButton.setOnAction(e -> app.handleSave(dialogStage, selectedTags)); // Сохранение изменений
            Button cancelButton = new Button("Отмена");
            cancelButton.setOnAction(e -> dialogStage.close()); // Закрытие окна без сохранения

            // Добавляем элементы в GridPane
            grid.addRow(5, tagsLabel, tagsListView);
            grid.addRow(6, saveButton, cancelButton);

            // Создание и отображение сцены с настройками стилей
            Scene scene = new Scene(grid);
            scene.getStylesheets().add(app.getClass().getResource("/static/style.css").toExternalForm()); // Добавляем стили
            dialogStage.setScene(scene);
            dialogStage.show(); // Показываем окно диалога
        } else {
            // Если файл не выбран, показываем ошибку
            Alert.showErrorAlert("Выберете файл", app);
        }
    }
}
