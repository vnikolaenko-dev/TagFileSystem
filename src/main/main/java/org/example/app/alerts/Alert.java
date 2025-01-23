package org.example.app.alerts;

import javafx.scene.Scene;
import org.example.app.MainApp;

public class Alert {
    /**
     * Метод для отображения окна ошибки с указанным сообщением.
     * Окно будет содержать только сообщение об ошибке, без заголовка и других дополнительных элементов.
     *
     * @param message Сообщение, которое будет отображаться в окне ошибки.
     * @param app Приложение, для добавления стилей в окно предупреждения.
     */
    public static void showErrorAlert(String message, MainApp app) {
        // Создаем новое окно с типом ошибки
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);

        // Получаем сцену окна предупреждения
        Scene alertScene = alert.getDialogPane().getScene();
        alert.setTitle("Ошибка: " + message);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Добавляем стили к сцене окна
        alertScene.getStylesheets().add(app.getClass().getResource("/static/style.css").toExternalForm());
        alert.showAndWait();
    }

    /**
     * Метод для отображения окна с информационным сообщением.
     * Окно будет содержать только информацию с заданным сообщением, без заголовка и других дополнительных элементов.
     *
     * @param message Сообщение, которое будет отображаться в информационном окне.
     * @param app Приложение, для добавления стилей в окно предупреждения.
     */
    public static void showAlert(String message, MainApp app) {
        // Создаем новое окно с типом информационного сообщения
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);

        // Получаем сцену окна предупреждения
        Scene alertScene = alert.getDialogPane().getScene();
        alert.setTitle(message);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Добавляем стили к сцене окна
        alertScene.getStylesheets().add(app.getClass().getResource("/static/style.css").toExternalForm());

        // Показываем окно и ждем закрытия пользователем
        alert.showAndWait();
    }

}
