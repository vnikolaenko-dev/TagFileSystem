package org.example;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.DAO.OrientDBFileTagSystem;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.DAO.OrientDBFileTagSystem.db;


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
    private TableView<FileNode> tableView = new TableView<>();
    private static BorderPane root = new BorderPane();
    private static String directory;
    private Stage myPrimaryStage;
    private HashSet<String> tags = OrientDBFileTagSystem.getTags();
    private ObservableList<String> availableTags;

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
        if (!Files.exists(desktopPath)) {
            throw new IOException("Папка рабочего стола не найдена");
        }

        return desktopPath;
    }

    public void checkDir() {
        ODatabaseRecordThreadLocal.instance().set((ODatabaseDocumentInternal) db);
        while (true) {
            try {
                Thread.sleep(1000);
                if (tags.contains(directory)) {
                    continue;
                }
                ArrayList<FileNode> updatedListOfFiles = loadFiles(directory);
                for (FileNode myFile : updatedListOfFiles) {
                    if (!fileNodes.contains(myFile)) {
                        System.out.println("ОБНУРАЖЕНО ИЗМЕНЕНИЕ В ДИРРЕКТОРИИ");
                        updateFiles(directory, new File(directory).listFiles());
                    }
                }
                for (FileNode myFile : fileNodes) {
                    if (!updatedListOfFiles.contains(myFile)) {
                        System.out.println("ОБНУРАЖЕНО ИЗМЕНЕНИЕ " + myFile.getFile().getName());
                        updateFiles(directory, new File(directory).listFiles());
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Font.loadFont(getClass().getResourceAsStream("/fonts/Roboto-Light.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Roboto-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf"), 14);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/static/logo.png")));

        myPrimaryStage = primaryStage;
        directory = getDesktopPath().toString();
        directoryStack.push(directory);

        Pane rootPane = new Pane();
        rootPane.getStyleClass().add("root");

        createFilesPane(directory);
        HBox searchBox = createSearchPane();

        canvas = createCanvas();
        gc = canvas.getGraphicsContext2D();
        centerX = canvas.getWidth() / 2;
        centerY = canvas.getHeight() / 2;

        updateFiles(directory, new File(directory).listFiles());

        rootPane.getChildren().add(canvas);
        rootPane.getChildren().add(createInfoPane());
        rootPane.getChildren().add(createWorkPane());

        VBox mainBox = createMainBox(searchBox, root, rootPane);
        // Создаем сцену и устанавливаем ее на основную сцену
        Scene scene = new Scene(mainBox, 1200, 600);

        scene.getStylesheets().add(getClass().getResource("/static/style.css").toExternalForm());
        primaryStage.setTitle("File System Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();

        draw(gc);

        Thread checker = new Thread(this::checkDir);
        checker.start();
    }

    private BorderPane createFilesPane(String path) {
        TableColumn<FileNode, String> pathColumn = new TableColumn<>("Путь");
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("file"));

        TableColumn<FileNode, String> tagColumn = new TableColumn<>("Тэг");
        tagColumn.setCellValueFactory(new PropertyValueFactory<>("tags"));

        tableView.getColumns().add(pathColumn);
        tableView.getColumns().add(tagColumn);

        System.out.println("ОТОБРАЖЕНИЕ ДИРРЕКТОРИИ: " + path);
        tableView.getItems().addAll(fileNodes);
        root.setCenter(tableView);
        return root;
    }

    private Canvas createCanvas() {
        Canvas canvas = new Canvas(800, 600);
        canvas.getStyleClass().add("canvas");

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

    private HBox createSearchPane() {
        HBox searchBox = new HBox(10); // Отступы между элементами
        searchBox.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPromptText("Введите путь или название тэга...");
        searchField.setMinWidth(300);

        Button searchButton = new Button("Поиск");
        searchButton.setOnAction(e -> {
            offsetX = 0;
            offsetY = 0;
            String searchQuery = searchField.getText();
            if (tags.contains(searchQuery)) {
                showFilesByTag(searchQuery);
            }
            goToDirectory(searchQuery); // Метод для выполнения поиска
            draw(gc);
        });

        Button backButton = new Button("Назад");
        backButton.setOnAction(e -> goBack());

        searchBox.getChildren().addAll(searchField, searchButton, backButton); // Добавление элементов в HBox
        return searchBox;
    }

    // Информация о точке
    private Pane createInfoPane() {
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
        return infoPane;
    }


    private VBox createWorkPane() {
        // Кнопки для работы с файлами
        Button createFileButton = new Button("Редактировать тэги");
        createFileButton.setOnAction(e -> editTag()); // Метод для создания файла

        Button editFileButton = new Button("Редактировать файл");
        editFileButton.setOnAction(e -> editFile()); // Метод для редактирования файла

        Button openFileButton = new Button("Открыть файл");
        openFileButton.setOnAction(e -> openFile()); // Метод для редактирования файла

        // Создаем VBox для кнопок и настраиваем его
        VBox buttonBox = new VBox(10); // 10 - отступ между кнопками
        buttonBox.getChildren().addAll(createFileButton, editFileButton, openFileButton);
        buttonBox.setMinWidth(150); // Ширина бокса с кнопками
        buttonBox.setAlignment(Pos.BOTTOM_LEFT); // Выравнивание по правой стороне

        return buttonBox;
    }

    private void openFile() {
        try {
            File selectedFile = selectedFileNode.getFile();
            Desktop desktop = Desktop.getDesktop();

            if (selectedFile.isDirectory()) {
                desktop.open(selectedFile);  // Открывает проводник в указанной директории

            } else {
                String path = selectedFile.getAbsolutePath();
                String command = "explorer.exe /select," + path;
                Process process = Runtime.getRuntime().exec(command);  // Выполняем команду
                process.waitFor();   // Открывает директорию, где находится файл
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    private VBox createMainBox(HBox searchBox, BorderPane root, Pane rootPane) {
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
        for (String tag : selectedFileNode.tags) {
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
            updateFiles(selectedFileNode.getFile().getAbsolutePath(), new File(selectedFileNode.getFile().getAbsolutePath()).listFiles());
        });
        vbox.getChildren().addAll(fileLabel, tagsLabel, tagLabel, tagField, addButton);
        Scene scene = new Scene(vbox);
        popupStage.setScene(scene);
        popupStage.show();
    }

    private void editFile() {
        if (selectedFileNode != null) {
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Редактор файла");
            // dialogStage.initModality(Modality.APPLICATION_MODAL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(10));

            grid.getStyleClass().add("root");

            // Информация о файле
            Label nameLabel = new Label("Имя файла:");
            Label nameValue = new Label(selectedFileNode.getFile().getName());
            Label pathLabel = new Label("Путь:");
            Label pathValue = new Label(selectedFileNode.getFile().getAbsolutePath());
            Label sizeLabel = new Label("Размер:");
            Label sizeValue = new Label(getFileSize(selectedFileNode.getFile()));

            grid.addRow(0, nameLabel, nameValue);
            grid.addRow(1, pathLabel, pathValue);

            if (selectedFileNode.getFile().isFile()){
                grid.addRow(2, sizeLabel, sizeValue);
            }


            ListView<String> tagsListView = new ListView<>();
            ObservableList<String> availableTags = FXCollections.observableArrayList(tags); // tags - ваш список тегов
            tagsListView.setItems(availableTags);

            // Разрешаем множественный выбор
            tagsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // Устанавливаем начальное значение выбранных тегов (если нужно)
            if (selectedFileNode != null && selectedFileNode.tags != null) {
                for (String tag : selectedFileNode.tags) {
                    int index = availableTags.indexOf(tag);
                    if (index != -1) {
                        tagsListView.getSelectionModel().select(index);
                    }
                }
            }

            Label tagsLabel = new Label("Теги:"); // Измененная метка
            // Получение выбранных тегов (например, по нажатию кнопки "Сохранить")
            ObservableList<String> selectedTags = tagsListView.getSelectionModel().getSelectedItems();

            // Кнопки
            Button saveButton = new Button("Сохранить");
            saveButton.setOnAction(e -> handleSave(dialogStage, selectedTags));
            saveButton.setStyle("");
            Button cancelButton = new Button("Отмена");
            cancelButton.setOnAction(e -> dialogStage.close());

            grid.addRow(5, tagsLabel, tagsListView);
            grid.addRow(6, saveButton, cancelButton);

            Scene scene = new Scene(grid);
            scene.getStylesheets().add(getClass().getResource("/static/style.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.show();
        } else {
            showErrorAlert("Выберете файл");
        }

    }

    private void handleSave(Stage dialogStage, ObservableList<String> tagsFromApp) {
        ArrayList<String> tags = new ArrayList<>(tagsFromApp);
        // Обработка сохранения имени и тегов (здесь необходима логика сохранения)
        if (selectedFileNode != null) {
            if (selectedFileNode.tags.isEmpty()) {
                OrientDBFileTagSystem.addFile(selectedFileNode.getFile().getPath(), tags);
            } else {
                for (String tag : tags) {
                    OrientDBFileTagSystem.addTagToFile(selectedFileNode.getFile().getPath(), tag);
                }
            }

            // Удаляем невыбранные тэги
            for (String tag : OrientDBFileTagSystem.getTagsByPath(selectedFileNode.getFile().getPath())) {
                if (!tagsFromApp.contains(tag))
                    OrientDBFileTagSystem.removeTagFromFile(selectedFileNode.getFile().getPath(), tag);
            }

            selectedFileNode.tags = tags;
            dialogStage.close();
        } else {
            showErrorAlert("Файл не выбран");
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        Scene alertScene = alert.getDialogPane().getScene();
        alert.setTitle("Ошибка: " + message);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alertScene.getStylesheets().add(getClass().getResource("/static/style.css").toExternalForm());
        alert.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        Scene alertScene = alert.getDialogPane().getScene();
        alert.setTitle(message);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alertScene.getStylesheets().add(getClass().getResource("/static/style.css").toExternalForm());
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

    private void editTag() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Создание тэга");
        // dialogStage.initModality(Modality.APPLICATION_MODAL);


        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        ListView<String> tagsListView = new ListView<>();
        availableTags = FXCollections.observableArrayList(tags); // tags - ваш список тегов
        tagsListView.setItems(availableTags);

        Label newTag = new Label("Создать новый тэг:");
        TextField tagField = new TextField();
        Button addButton = new Button("Создать");
        addButton.setOnAction(e -> {
            String tagsText = tagField.getText();
            List<String> newTags = Arrays.stream(tagsText.split(","))
                    .map(String::trim)          // Удаляем пробелы вокруг каждого тега
                    .filter(s -> !s.isEmpty())  // Фильтруем пустые строки
                    .collect(Collectors.toList());
            if (OrientDBFileTagSystem.createTags((ArrayList<String>) newTags)) {
                showAlert("Тэг успешно создан.");
                updateTagsList();
            } else {
                showErrorAlert("Ошибка при создании тэга.");
            }
        });
        grid.addRow(0, newTag);
        grid.addRow(1, tagField, addButton);

        Label removeTag = new Label("Удалить тэг:");
        TextField removeTagField = new TextField();
        Button removeButton = new Button("Удалить");
        removeButton.setOnAction(e -> {
            String tagsText = removeTagField.getText();
            List<String> newTags = Arrays.stream(tagsText.split(","))
                    .map(String::trim)          // Удаляем пробелы вокруг каждого тега
                    .filter(s -> !s.isEmpty())  // Фильтруем пустые строки
                    .collect(Collectors.toList());
            if (OrientDBFileTagSystem.removeTags((ArrayList<String>) newTags)) {
                showAlert("Тэг успешно удален.");
                updateTagsList();
            } else {
                showErrorAlert("Ошибка при удалении тэга, проверьте название тэга и повторите попытку.");
            }
        });
        grid.addRow(2, removeTag);
        grid.addRow(3, removeTagField, removeButton);
        grid.addRow(4, tagsListView);

        Scene scene = new Scene(grid);
        scene.getStylesheets().add(getClass().getResource("/static/style.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.show();
    }

    private void updateTagsList() {
        // Получаем обновленный список тегов из базы данных
        tags = OrientDBFileTagSystem.getTags();
        availableTags.setAll(tags); // Обновляем список в ListView
    }

    private void goToDirectory(String directoryPath) {
        directory = directoryPath;
        directoryStack.push(directoryPath); // Сохраняем текущую директорию в стек
        updateFiles(directoryPath, new File(directory).listFiles());
        draw(gc);
    }

    private void showFilesByTag(String tag) {
        ArrayList<String> list = (ArrayList<String>) OrientDBFileTagSystem.searchFilesByTag(tag);
        File[] filesWithTag = new File[list.size()];
        for (int i = 0; i < list.size(); i++) {
            filesWithTag[i] = new File(list.get(i));
        }
        updateFiles(tag, filesWithTag);
        draw(gc);
    }

    private void goBack() {
        if (!directoryStack.isEmpty()) {
            String previousDirectory = directoryStack.pop(); // Удаляем текущую директорию из стека
            directory = directoryStack.peek();
            updateFiles(directoryStack.peek(), new File(directory).listFiles());
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
                FileNode fileNode = new FileNode(new File(files[i].getPath()));
                mf.add(fileNode);
            }
        }
        return mf;
    }

    // todo данные о тэгах должны браться из БД
    private void updateFiles(String directoryPath, File[] files) {
        zoomLevel = 1;
        zoomCenterX = 0;
        zoomCenterY = 0;

        File directory = new File(directoryPath);
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

                FileNode fileNode = new FileNode(files[i], x, y, false);
                fileNode.tags = OrientDBFileTagSystem.getTagsByPath(files[i].getPath());
                fileNodes.add(fileNode);
            }
        }
        tableView.getItems().clear();
        ObservableList<FileNode> observableFileNodes = FXCollections.observableArrayList(fileNodes);
        tableView.setItems(observableFileNodes);

        offsetX = 0;
        offsetY = 0;
        draw(gc);
    }

    private void draw(GraphicsContext gc) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // Расчёт координат с учётом зума и центра зума
        double adjustedX, adjustedY;

        gc.setFill(Color.web("#2b2d30"));
        gc.fillRect(centerX - canvas.getWidth() * 2, centerY - canvas.getHeight() * 2, canvas.getWidth() * 10, canvas.getHeight() * 10);

        FileNode rootNode = fileNodes.get(0);
        adjustedX = (rootNode.getX() + offsetX - zoomCenterX) * zoomLevel + zoomCenterX;
        adjustedY = (rootNode.getY() + offsetY - zoomCenterY) * zoomLevel + zoomCenterY;

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
        // Рисуем кружок для начальной директории
        gc.setFill(rootNode.getColor());
        gc.fillOval(adjustedX, adjustedY, 30 * zoomLevel, 30 * zoomLevel);
        gc.fillText(rootNode.getFile().getName(), adjustedX, adjustedY - 10 * zoomLevel);
    }

    private void showFileInfo(FileNode node) {
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