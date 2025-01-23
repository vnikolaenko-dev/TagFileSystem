package org.example.app.components;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import org.example.app.MainApp;
import org.example.dao.service.FileService;
import org.example.util.FileNode;

import java.io.File;
import java.util.List;

/**
 * Компонент для отображения файловой структуры на холсте (canvas).
 * Поддерживает взаимодействие с пользователем, такие как перетаскивание, зум и выбор элементов.
 */
public class CanvasComponent {
    private Canvas canvas;  // Холст для рисования
    private GraphicsContext gc;  // Контекст для рисования на холсте
    private double centerX;  // Центр холста по оси X
    private double centerY;  // Центр холста по оси Y
    private static double offsetX = 0;  // Смещение холста по оси X
    private static double offsetY = 0;  // Смещение холста по оси Y
    private double mouseStartX;  // Начальная позиция мыши по оси X
    private double mouseStartY;  // Начальная позиция мыши по оси Y
    private static double zoomLevel = 1.0;  // Уровень масштабирования
    private static double zoomCenterX;  // Координата центра масштабирования по оси X
    private static double zoomCenterY;  // Координата центра масштабирования по оси Y
    private boolean dragging = false;  // Флаг для отслеживания перетаскивания
    public FileNode selectedFileNode;  // Выделенный файл

    /**
     * Сбрасывает параметры зума и смещения к исходным значениям.
     */
    public void reset() {
        zoomLevel = 1;
        zoomCenterX = 0;
        zoomCenterY = 0;
        offsetX = 0;
        offsetY = 0;
    }

    /**
     * Рисует на холсте файлы и их связи, учитывая текущие параметры зума и смещения.
     *
     * @param fileNodes Список узлов файлов (FileNode), которые будут отображаться на холсте.
     */
    public void draw(List<FileNode> fileNodes) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());  // Очищаем холст

        // Расчёт координат с учётом зума и центра зума
        double adjustedX, adjustedY;

        gc.setFill(Color.web("#2b2d30"));
        gc.fillRect(centerX - canvas.getWidth() * 2, centerY - canvas.getHeight() * 2, canvas.getWidth() * 10, canvas.getHeight() * 10);  // Рисуем фон

        FileNode rootNode = fileNodes.get(0);  // Корневой узел
        adjustedX = (rootNode.getX() + offsetX - zoomCenterX) * zoomLevel + zoomCenterX;
        adjustedY = (rootNode.getY() + offsetY - zoomCenterY) * zoomLevel + zoomCenterY;

        for (int i = 1; i < fileNodes.size(); i++) {  // Начинаем с 1, чтобы пропустить корневую директорию
            FileNode node = fileNodes.get(i);

            // Расчёт координат для текущего узла
            double nodeX = (node.getX() + offsetX - zoomCenterX) * zoomLevel + zoomCenterX;
            double nodeY = (node.getY() + offsetY - zoomCenterY) * zoomLevel + zoomCenterY;

            // Рисуем линию от корня до текущего узла
            gc.strokeLine(adjustedX + 15 * zoomLevel, adjustedY + 15 * zoomLevel,
                    nodeX + 15 * zoomLevel, nodeY + 15 * zoomLevel);

            gc.setFill(node.getColor());  // Устанавливаем цвет для текущего узла

            node.setTransformX(node.getX() + offsetX);
            node.setTransformY(node.getY() + offsetY);

            gc.fillOval(nodeX, nodeY, 30 * zoomLevel, 30 * zoomLevel);  // Рисуем круг для узла
            gc.fillText(node.getFile().getName(), nodeX, nodeY - 10 * zoomLevel);  // Добавляем имя файла
        }

        // Рисуем круг для начальной директории
        gc.setFill(rootNode.getColor());
        gc.fillOval(adjustedX, adjustedY, 30 * zoomLevel, 30 * zoomLevel);
        gc.fillText(rootNode.getFile().getName(), adjustedX, adjustedY - 10 * zoomLevel);
    }

    /**
     * Создаёт и настраивает холст для отображения файлов. Обрабатывает взаимодействие с мышью для перетаскивания и зума.
     *
     * @param app       Основное приложение (MainApp).
     * @param fileNodes Список узлов файлов (FileNode) для отображения.
     * @return          Объект Canvas для отображения.
     */
    public Canvas createCanvas(MainApp app, List<FileNode> fileNodes) {
        canvas = new Canvas(800, 600);  // Создаём холст
        canvas.getStyleClass().add("canvas");  // Добавляем стиль для холста
        gc = canvas.getGraphicsContext2D();  // Получаем контекст для рисования

        centerX = canvas.getWidth() / 2;  // Устанавливаем центр по оси X
        centerY = canvas.getHeight() / 2;  // Устанавливаем центр по оси Y

        // Обработка нажатия кнопки мыши для начала перетаскивания
        canvas.setOnMousePressed(event -> {
            mouseStartX = event.getX();
            mouseStartY = event.getY();
            dragging = true;  // Устанавливаем флаг перетаскивания

            if (event.getClickCount() == 1) {
                double adjustedX = event.getX();
                double adjustedY = event.getY();
                for (FileNode node : fileNodes) {
                    if (node.containsPoint(adjustedX, adjustedY, offsetX, offsetY, zoomLevel)) {
                        for (FileNode nodeUnselected : fileNodes) {
                            if (!node.equals(nodeUnselected)) {
                                nodeUnselected.setSelected(false);  // Снимаем выделение с других узлов
                            }
                        }
                        node.setSelected(true);  // Выделяем текущий узел
                        draw(fileNodes);
                        selectedFileNode = node;  // Сохраняем выбранный узел
                        break;
                    }
                }
            }
        });

        // Обработка перетаскивания холста
        canvas.setOnMouseDragged(event -> {
            if (dragging) {
                offsetX += event.getX() - mouseStartX;
                offsetY += event.getY() - mouseStartY;
                mouseStartX = event.getX();
                mouseStartY = event.getY();
                draw(fileNodes);
            }
        });

        // Обработка кликов мыши (выбор кружка и отображение информации о файле)
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            boolean foundNode = false;
            double adjustedX = event.getX();
            double adjustedY = event.getY();
            for (FileNode node : fileNodes) {
                if (node.containsPoint(adjustedX, adjustedY, offsetX, offsetY, zoomLevel)) {
                    app.getFileInfoComponent().showFileInfo(node, canvas);  // Показываем информацию о файле
                    foundNode = true;
                    break;
                }
            }
            if (!foundNode) {
                app.getFileInfoComponent().getInfoPane().setVisible(false);  // Скрываем информацию, если узел не найден
            }
        });

        // Обработка двойного клика по кружку (переход в директорию)
        canvas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                double adjustedX = event.getX();
                double adjustedY = event.getY();
                for (FileNode node : fileNodes) {
                    if (node.containsPoint(adjustedX, adjustedY, offsetX, offsetY, zoomLevel)) {
                        offsetX = 0;
                        offsetY = 0;
                        if (node.isDirectory()) {
                            app.goToDirectory(node.getFile().getAbsolutePath());  // Переход в директорию
                        }
                        break;
                    }
                }
            }
        });

        // Обработка отпускания кнопки мыши (завершение перетаскивания)
        canvas.setOnMouseReleased(event -> dragging = false);

        // Обработка прокрутки колесом мыши для изменения зума
        canvas.addEventFilter(ScrollEvent.ANY, event -> {
            zoomCenterX = event.getX();
            zoomCenterY = event.getY();
            zoomLevel *= (event.getDeltaY() > 0) ? 1.1 : 0.9;
            zoomLevel = Math.max(0.5, Math.min(zoomLevel, 1.0));  // Ограничиваем уровень зума
            draw(fileNodes);
            event.consume();
        });

        return canvas;
    }

    /**
     * Обновляет расположение файлов на холсте, пересчитывая их координаты в зависимости от директории и файлов.
     *
     * @param directoryPath Путь к директории.
     * @param files         Массив файлов в директории.
     * @param fileNodes     Список узлов файлов для отображения.
     * @param tableView     Таблица для отображения информации о файлах.
     * @param session       Сессия базы данных для работы с тегами.
     */
    public void updateFilesPosition(String directoryPath, File[] files, List<FileNode> fileNodes, TableView<FileNode> tableView, ODatabaseSession session) {
        reset();
        File directory = new File(directoryPath);
        if (files != null) {
            fileNodes.clear();
            int maxDistance = 150;  // Максимальное расстояние для расположения файлов
            double distanceFactor = Math.max(files.length * 15, maxDistance);

            // Добавляем корневой узел
            FileNode main = new FileNode(directory, centerX + offsetX, centerY + offsetY, true);
            main.setTags(FileService.getTagsByPath(session, main.getFile().getPath()));
            fileNodes.add(main);

            // Добавляем дочерние файлы
            for (int i = 0; i < files.length; i++) {
                double angle = (2 * Math.PI / files.length) * i;
                double x = centerX + distanceFactor * Math.cos(angle) + offsetX;
                double y = centerY + distanceFactor * Math.sin(angle) + offsetY;

                FileNode fileNode = new FileNode(files[i], x, y, false);
                fileNode.tags = FileService.getTagsByPath(session, files[i].getPath());
                fileNodes.add(fileNode);
            }
            tableView.getItems().setAll(fileNodes);  // Обновляем таблицу с файлами
        }
    }
}
