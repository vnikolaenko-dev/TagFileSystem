package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;


// todo поток который будет следить за файлами в текущей дирректории
// todo добавление тэгов

// todo вывод только выбранных тэгов
// todo подключить бд

public class FileSystemVisualizer extends Application {
    private Canvas canvas;
    private GraphicsContext gc;
    private List<FileNode> fileNodes = new ArrayList<>();
    private Stack<String> directoryStack = new Stack<>(); // Стек для хранения истории директорий
    private double centerX;
    private double centerY;
    private double radius;
    private static double offsetX = 0; // Смещение по X
    private static double offsetY = 0; // Смещение по Y
    private double mouseStartX; // Начальная позиция мыши по X
    private double mouseStartY; // Начальная позиция мыши по Y
    private boolean dragging = false; // Флаг для отслеживания перетаскивания
    private Pane infoPane; // Панель для отображения информации о файле
    private Label fileNameLabel;
    private Label filePathLabel;
    private Label fileSizeLabel;
    private Label creationDateLabel;
    private Label modificationDateLabel;
    private static double zoomLevel = 1.0; // Уровень масштабирования
    private static double zoomCenterX; // Координата центра масштабирования по X
    private static double zoomCenterY; // Координата центра масштабирования по Y
    private static FileNode selectedFileNode;
    private ListView<String> folderListView = new ListView<>();
    private static BorderPane root = new BorderPane();
    private static String directory;
    private Stage myPrimaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    public static Path getDesktopPath() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        Path desktopPath;

        if (os.contains("win")) {
            // Windows
            desktopPath = Paths.get(System.getenv("USERPROFILE"), "Desktop");
        } else if (os.contains("mac")) {
            // macOS
            desktopPath = Paths.get(System.getProperty("user.home"), "Desktop");
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            desktopPath = Paths.get(System.getProperty("user.home")).resolve("Desktop");
        } else {
            // Неизвестная ОС - возвращаем null или выбрасываем исключение, в зависимости от ваших нужд
            throw new IOException("Неподдерживаемая операционная система");
            // Или: return null;
        }

        // Проверка на существование папки
        if (!Files.exists(desktopPath) ) {
            throw new IOException("Папка рабочего стола не найдена");
        }

        return desktopPath;
    }

    public void checkDir(){
        while (true) {
            try {
                Thread.sleep(1000);
                ArrayList<FileNode> updatedListOfFiles = loadFiles(directory);
                for (FileNode myFile : updatedListOfFiles) {
                    if (!fileNodes.contains(myFile)) {
                        System.out.println("ОБНУРАЖЕНО ИЗМЕНЕНИЕ");
                        updateFiles(directory);
                    }
                }
                for (FileNode myFile : fileNodes) {
                    if (!updatedListOfFiles.contains(myFile)) {
                        System.out.println("ОБНУРАЖЕНО ИЗМЕНЕНИЕ");
                        updateFiles(directory);
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        myPrimaryStage = primaryStage;
        directory = getDesktopPath().toString();
        directoryStack.push(directory);
        Pane rootPane = new Pane();

        createFilesPane(directory);
        HBox searchBox = createSearchPane();

        canvas = createCanvas();
        gc = canvas.getGraphicsContext2D();
        centerX = canvas.getWidth() / 2;
        centerY = canvas.getHeight() / 2;
        radius = Math.min(canvas.getWidth(), canvas.getHeight()) / 3;

        updateFiles(directory);

        rootPane.getChildren().add(canvas);
        rootPane.getChildren().add(createInfoPane());
        rootPane.getChildren().add(createWorkPane());

        VBox mainBox = createMainBox(searchBox, root, rootPane);
        // Создаем сцену и устанавливаем ее на основную сцену
        Scene scene = new Scene(mainBox, 1200, 600);
        primaryStage.setTitle("File System Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();

        draw(gc);

        Thread checker = new Thread(this::checkDir);
        checker.start();
    }

    private BorderPane createFilesPane(String path){
        Button upButton = new Button("Вверх");
        Button backButton = new Button("Назад");

        TableView<FileNode> tableView = new TableView<>();

        TableColumn<FileNode, String> pathColumn = new TableColumn<>("Путь");
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("file"));

        TableColumn<FileNode, String> tagColumn = new TableColumn<>("Тэг");
        tagColumn.setCellValueFactory(new PropertyValueFactory<>("tags"));

        tableView.getColumns().add(pathColumn);
        tableView.getColumns().add(tagColumn);

        System.out.println("ОТОБРАЖЕНИЕ ДИРРЕКТОРИИ: " + path);
        updateFiles(path);
        tableView.getItems().addAll(fileNodes);

        root.setCenter(tableView);
        // root.setRight(tags);

        HBox bBox = new HBox(10, upButton, backButton);
        bBox.setAlignment(Pos.CENTER);
        bBox.setPadding(new Insets(10));
        root.setBottom(bBox);
        return root;
    }

    private Canvas createCanvas(){
        Canvas canvas = new Canvas(800, 600);

        // Обработка нажатия кнопки мыши для начала перетаскивания
        canvas.setOnMousePressed(event -> {
            mouseStartX = event.getX();
            mouseStartY = event.getY();
            dragging = true; // Устанавливаем флаг перетаскивания

            if (event.getClickCount() == 1) {
                double adjustedX = event.getX(); // Корректируем координату X
                double adjustedY = event.getY(); // Корректируем координату Y
                for (FileNode node : fileNodes) {
                    if (node.containsPoint(adjustedX, adjustedY, offsetX, offsetY, zoomLevel)) {
                        for (FileNode nodeUnselected : fileNodes) {
                            if (!node.equals(nodeUnselected)) {
                                nodeUnselected.setSelected(false);
                            }
                        }
                        node.setSelected(true); // Устанавливаем выделение для узла
                        draw(gc);
                        selectedFileNode = node;
                        break;
                    }
                }
            }
        });
        // Обработка перетаскивания канваса
        canvas.setOnMouseDragged(event -> {
            if (dragging) {
                offsetX += event.getX() - mouseStartX;
                offsetY += event.getY() - mouseStartY;
                mouseStartX = event.getX();
                mouseStartY = event.getY();
                draw(gc);
            }
        });

        // клик по кружку (выбор кружка)
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            boolean foundNode = false; // Флаг для отслеживания найденного узла
            double adjustedX = event.getX(); // Корректируем координату X
            double adjustedY = event.getY(); // Корректируем координату Y
            for (FileNode node : fileNodes) {
                if (node.containsPoint(adjustedX, adjustedY, offsetX, offsetY, zoomLevel)) {
                    showFileInfo(node);
                    foundNode = true; // Установить флаг в true, если узел найден
                    // selectedFileNode = node;
                    break; // Выход из цикла после нахождения узла
                }
            }
            if (!foundNode) {
                infoPane.setVisible(false); // Скрыть панель, если узел не найден
            }
        });

        // Обработка двойного клика по кружку
        canvas.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                double adjustedX = event.getX(); // Корректируем координату X
                double adjustedY = event.getY(); // Корректируем координату Y
                for (FileNode node : fileNodes) {
                    if (node.containsPoint(adjustedX, adjustedY, offsetX, offsetY, zoomLevel)) {
                        offsetX = 0;
                        offsetY = 0;
                        if (node.isDirectory()) {
                            goToDirectory(node.getFile().getAbsolutePath());
                            // createFilesPane(node.getFile().getAbsolutePath());
                        }
                        break;
                    }
                }
            }
        });
        // Обработка отпускания кнопки мыши
        canvas.setOnMouseReleased(event -> dragging = false); // Сбрасываем флаг перетаскивания
        // Обработка прокрутки колесом мыши для зума
        canvas.addEventFilter(ScrollEvent.ANY, event -> {
            zoomCenterX = event.getX(); // Запоминаем координаты центра зума
            zoomCenterY = event.getY();
            zoomLevel *= (event.getDeltaY() > 0) ? 1.1 : 0.9; // Изменяем масштаб
            zoomLevel = Math.max(0.5, Math.min(zoomLevel, 1.0)); // Ограничиваем масштаб
            draw(gc);
            event.consume();
        });
        return canvas;
    }

    private HBox createSearchPane(){
        HBox searchBox = new HBox(10); // Отступы между элементами
        searchBox.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPromptText("Введите имя файла или каталога...");

        Button searchButton = new Button("Поиск");
        searchButton.setOnAction(e -> {
            String searchQuery = searchField.getText();
            goToDirectory(searchQuery); // Метод для выполнения поиска
        });

        Button backButton = new Button("Назад");
        backButton.setOnAction(e -> goBack());

        searchBox.getChildren().addAll(searchField, searchButton, backButton); // Добавление элементов в HBox
        return searchBox;
    }

    // Информация о точке
    private Pane createInfoPane(){
        infoPane = new Pane();
        infoPane.setVisible(false);
        infoPane.setPrefWidth(200);
        infoPane.setStyle("-fx-background-color: white; -fx-border-color: black;");
        infoPane.setPadding(new Insets(10));
        // Создаем метки для информации о файле
        fileNameLabel = new Label();
        filePathLabel = new Label();
        fileSizeLabel = new Label();
        creationDateLabel = new Label();
        modificationDateLabel = new Label();
        infoPane.getChildren().addAll(fileNameLabel, filePathLabel, fileSizeLabel, creationDateLabel, modificationDateLabel);
        return infoPane;
    }


    private VBox createWorkPane(){
        // Кнопки для работы с файлами
        Button createFileButton = new Button("Создать файл");
        createFileButton.setOnAction(e -> createFile()); // Метод для создания файла

        Button editFileButton = new Button("Редактировать файл");
        editFileButton.setOnAction(e -> editFile()); // Метод для редактирования файла

        /*
        Button addTagButton = new Button("Добавить тег");
        addTagButton.setOnAction(e -> addTag()); // Метод для добавления тега
         */

        // Создаем VBox для кнопок и настраиваем его
        VBox buttonBox = new VBox(10); // 10 - отступ между кнопками
        buttonBox.getChildren().addAll(createFileButton, editFileButton);
        // buttonBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8);"); // Полупрозрачный фон кнопок
        buttonBox.setMinWidth(150); // Ширина бокса с кнопками
        buttonBox.setAlignment(Pos.BOTTOM_LEFT); // Выравнивание по правой стороне

        return buttonBox;
    }

    private VBox createMainBox(HBox searchBox, BorderPane root, Pane rootPane){
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        // Создаем VBox для размещения корневого элемента и информационной метки
        VBox mainBox = new VBox();
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setSpacing(10);
        // Создаем HBox для размещения root и rootPane

        BorderPane borderPane = new BorderPane();
        root.setMinWidth(350);
        // root.setPadding(new Insets(10, 10, 10, 10));
        borderPane.setPadding(new Insets(10, 10, 10, 10));

        borderPane.setLeft(root);
        borderPane.setRight(rootPane);

        // Добавляем searchBox и HBox в mainBox
        mainBox.getChildren().addAll(searchBox, borderPane);
        return mainBox;
    }

    private List<String> getFolders(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles(File::isDirectory);
        if (files != null) {
            return Arrays.asList(Arrays.stream(files).map(File::getAbsolutePath).toArray(String[]::new));
        }
        return List.of();
    }

    private void addTag() {
        Stage popupStage = new Stage(StageStyle.UTILITY);
        popupStage.initModality(Modality.APPLICATION_MODAL); // Блокирует основное окно
        popupStage.initOwner(myPrimaryStage); // Устанавливает основное окно в качестве владельца

        popupStage.setTitle("Добавить тег");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setAlignment(Pos.CENTER);

        String tags = "";
        for(String tag: selectedFileNode.tags){
            tags += tag + ", ";
        }
        Label fileLabel = new Label("Имя файла: " + selectedFileNode.getFile().getName());
        Label tagsLabel = new Label("Теги: " + tags);
        Label tagLabel = new Label("Тег:");
        TextField tagField = new TextField();
        Button addButton = new Button("Добавить");

        addButton.setOnAction(e -> {
            selectedFileNode.tags.add(tagField.getText());
            popupStage.close();
            updateFiles(selectedFileNode.getFile().getAbsolutePath());});
        vbox.getChildren().addAll(fileLabel, tagsLabel, tagLabel, tagField, addButton);
        Scene scene = new Scene(vbox);
        popupStage.setScene(scene);
        popupStage.show();
    }

    private void editFile() {
        if (selectedFileNode != null){
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редактор файла");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));

            // Информация о файле
            Label nameLabel = new Label("Имя файла:" );
            Label nameValue = new Label(selectedFileNode.getFile().getName());
            Label pathLabel = new Label("Путь:");
            Label pathValue = new Label(selectedFileNode.getFile().getAbsolutePath());
            Label sizeLabel = new Label("Размер:");
            Label sizeValue = new Label(getFileSize(selectedFileNode.getFile()));
            Label lastModifiedLabel = new Label("Дата изменения:");
            // Label lastModifiedValue = new Label(getLastModifiedDate(selectedFileNode));

            grid.addRow(0, nameLabel, nameValue);
            grid.addRow(1, pathLabel, pathValue);
            grid.addRow(2, sizeLabel, sizeValue);
            // grid.addRow(3, lastModifiedLabel, lastModifiedValue);

            // Теги
            Label tagsLabel = new Label("Теги (через запятую):");
            TextField tagsField = new TextField(selectedFileNode.tags.toString()); //Начальное значение тегов

            // Кнопки
            Button saveButton = new Button("Сохранить");
            saveButton.setOnAction(e -> handleSave(dialogStage, tagsField));
            Button cancelButton = new Button("Отмена");
            cancelButton.setOnAction(e -> dialogStage.close());

            grid.addRow(4, tagsLabel, tagsField);
            grid.addRow(5, saveButton, cancelButton);

            Scene scene = new Scene(grid);
            dialogStage.setScene(scene);
            dialogStage.show();
        } else {
            showErrorAlert("Выберете файл");
        }

    }

    private void handleSave(Stage dialogStage, TextField tagsField) {
        List<String> tags = parseTags(tagsField.getText());
        // Обработка сохранения имени и тегов (здесь необходима логика сохранения)
        if (selectedFileNode != null) {
            selectedFileNode.tags = (ArrayList<String>) tags;
            dialogStage.close();
        } else {
            showErrorAlert("Файл не выбран");
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка: " + message);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<String> parseTags(String tagsString) {
        tagsString = tagsString.replace("[", "").replace("]", "");
        List<String> tags = new ArrayList<>();
        String[] tagArray = tagsString.split(",");
        for (String tag : tagArray) {
            String trimmedTag = tag.trim();
            if (!trimmedTag.isEmpty()) {
                tags.add(trimmedTag);
            }
        }
        return tags;
    }

    private String getFileSize(File file) {
        return String.valueOf(file.length()) + " байт";
    }

    private void createFile() {
    }

    private void goToDirectory(String directoryPath) {
        directory = directoryPath;
        directoryStack.push(directoryPath); // Сохраняем текущую директорию в стек
        updateFiles(directoryPath);
        draw(gc);
        createFilesPane(directoryPath);
    }

    private void goBack() {
        if (!directoryStack.isEmpty()) {
            String previousDirectory = directoryStack.pop(); // Удаляем текущую директорию из стека
            directory = directoryStack.peek();
            updateFiles(directoryStack.peek());
            createFilesPane(directoryStack.peek());
            draw(gc);
        }
    }

    private ArrayList<FileNode> loadFiles(String directoryPath) {
        ArrayList<FileNode> mf = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        mf.add(new FileNode(new File(directoryPath)));
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                mf.add(new FileNode(new File(files[i].getPath())));
            }
        }
        return mf;
    }

    // todo данные о тэгах должны браться из БД
    private void updateFiles(String directoryPath) {
        zoomLevel = 1;
        zoomCenterX = 0;
        zoomCenterY = 0;
        System.out.println(directoryPath);
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            fileNodes.clear();
            // Определяем максимальное расстояние от центра
            int maxDistance = 150; // Например, 150 пикселей
            // Вычисляем коэффициент для расчета расстояния в зависимости от количества файлов
            double distanceFactor = Math.max(files.length * 15, maxDistance); // Предотвращаем деление на 0
            // Добавляем кружок для начальной директории
            fileNodes.add(new FileNode(directory, centerX + offsetX, centerY + offsetY, true));
            for (int i = 0; i < files.length; i++) {
                double angle = (2 * Math.PI / files.length) * i;
                double x = centerX + distanceFactor * Math.cos(angle) + offsetX;
                double y = centerY + distanceFactor * Math.sin(angle) + offsetY;
                fileNodes.add(new FileNode(files[i], x, y, false));
            }
        }
    }

    private void draw(GraphicsContext gc) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // Расчёт координат с учётом зума и центра зума
        double adjustedX, adjustedY;

        gc.setFill(Color.web("#2b2d30"));
        gc.fillRect(centerX - canvas.getWidth() * 2, centerY - canvas.getHeight() * 2, canvas.getWidth() * 10, canvas.getHeight() * 10);

        // Рисуем кружок для начальной директории
        FileNode rootNode = fileNodes.get(0);
        gc.setFill(rootNode.getColor());

        adjustedX = (rootNode.getX() + offsetX - zoomCenterX) * zoomLevel + zoomCenterX;
        adjustedY = (rootNode.getY() + offsetY - zoomCenterY) * zoomLevel + zoomCenterY;
        gc.fillOval(adjustedX, adjustedY, 30 * zoomLevel, 30 * zoomLevel);
        gc.fillText(rootNode.getFile().getName(), adjustedX, adjustedY - 10 * zoomLevel);

        for (int i = 1; i < fileNodes.size(); i++) { // Начинаем с 1, чтобы пропустить корневую директорию
            FileNode node = fileNodes.get(i);

            // Расчёт координат
            double nodeX = (node.getX() + offsetX - zoomCenterX) * zoomLevel + zoomCenterX;
            double nodeY = (node.getY() + offsetY - zoomCenterY) * zoomLevel + zoomCenterY;

            // Рисуем линию от корневого узла до текущего узла
            gc.strokeLine(adjustedX + 15 * zoomLevel, adjustedY + 15 * zoomLevel,
                    nodeX + 15 * zoomLevel, nodeY + 15 * zoomLevel);

            gc.setFill(node.getColor());

            node.setTransformX(node.getX() + offsetX);
            node.setTransformY(node.getY() + offsetY);

            gc.fillOval(nodeX, nodeY, 30 * zoomLevel, 30 * zoomLevel);
            gc.fillText(node.getFile().getName(), nodeX, nodeY - 10 * zoomLevel);
        }
    }

    private void showFileInfo(FileNode node) {
        File file = node.getFile();
        // Получаем атрибуты файла
        try {
            BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

            // Обновляем метки с информацией о файле
            fileNameLabel.setText("Имя файла: " + file.getName());
            filePathLabel.setText("\nПолный путь: " + file.getAbsolutePath());
            fileSizeLabel.setText("\n\nРазмер: " + file.length() + " байт");
            creationDateLabel.setText("\n\n\nДата создания: " + dateFormat.format(new Date(attributes.creationTime().toMillis())));
            modificationDateLabel.setText("\n\n\n\nДата последней модификации: " + dateFormat.format(new Date(attributes.lastModifiedTime().toMillis())));


            infoPane.setLayoutX(node.getX() + offsetX + 35);
            infoPane.setLayoutY(node.getY() + offsetY);
            // Делаем панель видимой
            infoPane.setVisible(true);
            infoPane.toFront();
        } catch (Exception e) {
            // e.printStackTrace();
            // Обработка ошибок (например, если файл не существует)
        }
    }
}