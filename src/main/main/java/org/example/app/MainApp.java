package org.example.app;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.example.app.alerts.Alert;
import org.example.app.components.CanvasComponent;
import org.example.app.components.FileInfoComponent;
import org.example.app.components.FileTableComponent;
import org.example.app.dialogs.EditFileDialog;
import org.example.app.dialogs.EditTagDialog;
import org.example.app.utils.FileUtil;
import org.example.dao.OrientDBFileTagSystem;
import org.example.dao.service.FileService;
import org.example.dao.service.TagService;
import org.example.util.FileNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;



public class MainApp extends Application {
    private Canvas canvas;
    private List<FileNode> fileNodes = new ArrayList<>();
    private Stack<String> directoryStack = new Stack<>();
    private static BorderPane root = new BorderPane();
    private static String directory;
    private ODatabaseSession session = OrientDBFileTagSystem.getDb();
    private HashSet<String> tags = TagService.getTags(session);
    private ObservableList<String> availableTags = FXCollections.observableArrayList();
    private CanvasComponent canvasComponent = new CanvasComponent();
    private FileTableComponent fileTableComponent;
    private FileInfoComponent fileInfoComponent = new FileInfoComponent();

    public MainApp() {
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Path getDesktopPath() throws IOException {
        Path desktopPath = Paths.get(System.getenv("USERPROFILE"), "Desktop");
        // Проверка на существование папки
        if (!Files.exists(desktopPath)) {
            throw new IOException("Папка рабочего стола не найдена");
        }
        return desktopPath;
    }

    /**
     * Метод для постоянной проверки изменений в указанной директории.
     * Этот метод работает в отдельном потоке, который каждую секунду проверяет наличие изменений
     * в файловой системе в указанной директории. Если изменения обнаружены (добавление или удаление файлов),
     * он обновляет список файлов, отображаемых на экране, и перерисовывает холст и таблицу с файлами.
     */
    public void checkDir() {
        // Устанавливаем текущую сессию для работы с базой данных
        ODatabaseRecordThreadLocal.instance().set((ODatabaseDocumentInternal) session);
        while (true) {
            try {
                Thread.sleep(1000);

                // Если теги содержат директорию, пропускаем обработку (директория не изменялась)
                if (tags.contains(directory)) {
                    continue;
                }

                // Загружаем обновлённый список файлов в директории
                ArrayList<FileNode> updatedListOfFiles = loadFiles(directory);

                // Проверяем, если в обновлённом списке есть файлы, которых нет в текущем списке
                for (FileNode myFile : updatedListOfFiles) {
                    if (!fileNodes.contains(myFile)) {
                        System.out.println("ОБНУРАЖЕНО ИЗМЕНЕНИЕ В ДИРРЕКТОРИИ");
                        // Обновляем отображение файлов
                        canvasComponent.updateFilesPosition(directory, new File(directory).listFiles(), fileNodes, fileTableComponent.getTableView(), session);
                    }
                }

                // Проверяем, если в текущем списке есть файлы, которых нет в обновлённом списке
                for (FileNode myFile : fileNodes) {
                    if (!updatedListOfFiles.contains(myFile)) {
                        System.out.println("ОБНУРАЖЕНО ИЗМЕНЕНИЕ " + myFile.getFile().getName());
                        // Обновляем отображение файлов
                        canvasComponent.updateFilesPosition(directory, new File(directory).listFiles(), fileNodes, fileTableComponent.getTableView(), session);
                    }
                }

                // Очищаем текущую таблицу файлов
                fileTableComponent.getTableView().getItems().clear();

                // Обновляем таблицу с новыми файлами
                ObservableList<FileNode> observableFileNodes = FXCollections.observableArrayList(fileNodes);
                fileTableComponent.getTableView().setItems(observableFileNodes);

                // Перерисовываем холст с обновлённым списком файлов
                canvasComponent.draw(fileNodes);

            } catch (Exception e) {
                // Логирование или обработка исключений (в текущем коде пусто)
            }
        }
    }


    /**
     * Метод для инициализации и запуска приложения.
     * В этом методе загружаются необходимые шрифты, иконки, создаются интерфейсные компоненты и сцена.
     * Также запускается поток, который периодически проверяет изменения в файловой системе (метод checkDir).
     *
     * @param primaryStage Основная сцена приложения.
     * @throws IOException Исключение, которое может быть выброшено при загрузке ресурсов.
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Загружаем шрифты для использования в интерфейсе
        Font.loadFont(getClass().getResourceAsStream("/fonts/Roboto-Light.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Roboto-Bold.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Roboto-Regular.ttf"), 14);

        // Устанавливаем иконку приложения
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/static/logo.png")));

        // Инициализируем путь к рабочей директории (по умолчанию на рабочем столе)
        directory = getDesktopPath().toString();
        directoryStack.push(directory);

        // Создаём основной контейнер для компонентов интерфейса
        Pane rootPane = new Pane();
        rootPane.getStyleClass().add("root");

        // Создаём панель для поиска
        HBox searchBox = createSearchPane();

        // Создаём холст (Canvas) и инициализируем его
        canvas = canvasComponent.createCanvas(this, fileNodes);
        fileTableComponent = new FileTableComponent(fileNodes);

        // Загружаем файлы в директории и обновляем их отображение на холсте и в таблице
        canvasComponent.updateFilesPosition(directory, new File(directory).listFiles(), fileNodes, fileTableComponent.getTableView(), session);

        // Добавляем холст и другие компоненты в основной контейнер
        rootPane.getChildren().add(canvas);
        rootPane.getChildren().add(fileInfoComponent.getInfoPane());
        rootPane.getChildren().add(createWorkPane());

        // Создаём основной вертикальный контейнер для интерфейса
        VBox mainBox = createMainBox(searchBox, root, rootPane);

        // Создаём сцену с заданными размерами и устанавливаем её
        Scene scene = new Scene(mainBox, 1200, 600);

        // Рисуем начальную информацию и компоненты
        canvasComponent.draw(fileNodes);
        root.setCenter(fileTableComponent.getTableView());

        // Подключаем внешний стиль CSS для оформления
        scene.getStylesheets().add(getClass().getResource("/static/style.css").toExternalForm());

        // Устанавливаем заголовок и сцену приложения
        primaryStage.setTitle("File System Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Запускаем поток для мониторинга изменений в директории
        Thread checker = new Thread(this::checkDir);
        checker.start();
    }

    /**
     * Метод для загрузки списка файлов в указанной директории.
     *
     * @param directoryPath Путь к директории для загрузки файлов.
     * @return Список объектов FileNode, представляющих файлы и директории.
     */
    private ArrayList<FileNode> loadFiles(String directoryPath) {
        ArrayList<FileNode> mf = new ArrayList<>();
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        mf.add(new FileNode(new File(directoryPath))); // Добавляем корневую директорию в список.
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                FileNode fileNode = new FileNode(new File(files[i].getPath()));
                mf.add(fileNode); // Добавляем каждый файл или директорию в список.
            }
        }
        return mf; // Возвращаем список файлов и директорий.
    }

    /**
     * Метод для создания панели поиска с текстовым полем и кнопками.
     *
     * @return Панель для поиска с кнопками "Поиск" и "Назад".
     */
    private HBox createSearchPane() {
        HBox searchBox = new HBox(10); // Устанавливаем отступы между элементами
        searchBox.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPromptText("Введите путь или название тэга...");
        searchField.setMinWidth(300);

        Button searchButton = new Button("Поиск");
        searchButton.setOnAction(e -> {
            canvasComponent.reset(); // Сброс состояния канваса перед поиском.
            String searchQuery = searchField.getText();
            if (tags.contains(searchQuery)) {
                showFilesByTag(searchQuery); // Показываем файлы по тэгу, если найден.
            }
            goToDirectory(searchQuery); // Переходим в директорию по запросу.
        });

        Button backButton = new Button("Назад");
        backButton.setOnAction(e -> goBack()); // Возвращаемся в предыдущую директорию.

        searchBox.getChildren().addAll(searchField, searchButton, backButton); // Добавляем элементы в панель.
        return searchBox;
    }

    /**
     * Метод для создания панели с кнопками для работы с файлами.
     *
     * @return Панель с кнопками для работы с файлами.
     */
    private VBox createWorkPane() {
        // Кнопки для работы с файлами
        Button createFileButton = new Button("Редактировать тэги");
        createFileButton.setOnAction(e -> EditTagDialog.showEditTagDialog(this, session, availableTags, tags)); // Открытие диалога для редактирования тегов.

        Button editFileButton = new Button("Редактировать файл");
        editFileButton.setOnAction(e -> EditFileDialog.showEditFileDialog(this, session, availableTags, tags)); // Открытие диалога для редактирования файла.

        Button openFileButton = new Button("Открыть файл");
        openFileButton.setOnAction(e -> FileUtil.openFile(canvasComponent.selectedFileNode.getFile())); // Открытие выбранного файла.

        VBox buttonBox = new VBox(10); // Панель с кнопками, отступы между кнопками.
        buttonBox.getChildren().addAll(createFileButton, editFileButton, openFileButton);
        buttonBox.setMinWidth(150); // Ширина бокса.
        buttonBox.setAlignment(Pos.BOTTOM_LEFT); // Выравнивание кнопок по левому нижнему краю.

        return buttonBox;
    }

    /**
     * Метод для создания главной панели интерфейса.
     *
     * @param searchBox Панель поиска.
     * @param root Основной корневой элемент интерфейса.
     * @param rootPane Панель с корневыми компонентами.
     * @return Главная панель, включающая в себя поиск и рабочую область.
     */
    private VBox createMainBox(HBox searchBox, BorderPane root, Pane rootPane) {
        HBox.setHgrow(searchBox, Priority.ALWAYS); // Настройка роста панели поиска.

        VBox mainBox = new VBox();
        mainBox.setAlignment(Pos.CENTER);
        mainBox.setSpacing(10); // Расстояние между элементами.

        BorderPane borderPane = new BorderPane();
        root.setMinWidth(350);
        borderPane.setPadding(new Insets(10, 10, 10, 10)); // Отступы от края для borderPane.

        borderPane.setLeft(root); // Размещение основной панели слева.
        borderPane.setRight(rootPane); // Размещение панели с корневыми компонентами справа.

        mainBox.getChildren().addAll(searchBox, borderPane); // Добавляем панели в mainBox.
        return mainBox;
    }

    /**
     * Метод для обработки сохранения новых тегов для выбранного файла.
     *
     * @param dialogStage Стадия диалога для закрытия после сохранения.
     * @param tagsFromApp Список тегов, которые были выбраны в приложении.
     */
    public void handleSave(Stage dialogStage, ObservableList<String> tagsFromApp) {
        ArrayList<String> tags = new ArrayList<>(tagsFromApp); // Преобразуем список тегов в ArrayList.
        if (canvasComponent.selectedFileNode != null) { // Проверяем, что файл выбран.
            for (String tag : tags) {
                FileService.addTagToFile(session, canvasComponent.selectedFileNode.getFile().getPath(), tag); // Добавляем новые теги.
            }

            // Удаляем теги, которые не выбраны.
            for (String tag : FileService.getTagsByPath(session, canvasComponent.selectedFileNode.getFile().getPath())) {
                if (!tagsFromApp.contains(tag))
                    FileService.removeTagFromFile(session, canvasComponent.selectedFileNode.getFile().getPath(), tag);
            }

            canvasComponent.selectedFileNode.tags = tags; // Обновляем теги для выбранного файла.
            dialogStage.close(); // Закрываем диалог.
        } else {
            Alert.showErrorAlert("Файл не выбран", this); // Показываем ошибку, если файл не выбран.
        }
    }

    /**
     * Метод для обновления списка доступных тегов.
     */
    public void updateTagsList() {
        tags = TagService.getTags(session); // Получаем обновленный список тегов из базы данных.
        availableTags.setAll(tags); // Обновляем список в ListView.
    }

    /**
     * Метод для отображения файлов, содержащих заданный тег.
     *
     * @param tag Тег для поиска файлов.
     */
    private void showFilesByTag(String tag) {
        ArrayList<String> list = (ArrayList<String>) FileService.searchFilesByTag(session, tag); // Ищем файлы по тегу.
        File[] filesWithTag = new File[list.size()];
        for (int i = 0; i < list.size(); i++) {
            filesWithTag[i] = new File(list.get(i)); // Преобразуем список путей в массив файлов.
        }
        canvasComponent.updateFilesPosition(tag, filesWithTag, fileNodes, fileTableComponent.getTableView(), session); // Обновляем позицию файлов на канвасе.
        canvasComponent.draw(fileNodes); // Перерисовываем канвас.
    }

    /**
     * Метод для перехода в указанную директорию.
     *
     * @param directoryPath Путь к директории, в которую нужно перейти.
     */
    public void goToDirectory(String directoryPath) {
        directory = directoryPath;
        directoryStack.push(directoryPath); // Сохраняем текущую директорию в стек.
        canvasComponent.updateFilesPosition(directory, new File(directory).listFiles(), fileNodes, fileTableComponent.getTableView(), session); // Обновляем позицию файлов.
        canvasComponent.draw(fileNodes); // Перерисовываем канвас.
    }

    /**
     * Метод для возврата в предыдущую директорию.
     */
    private void goBack() {
        if (!directoryStack.isEmpty()) {
            directoryStack.pop(); // Удаляем текущую директорию из стека.
            directory = directoryStack.peek(); // Получаем директорию сверху стека.
            canvasComponent.updateFilesPosition(directoryStack.peek(), new File(directory).listFiles(), fileNodes, fileTableComponent.getTableView(), session); // Обновляем позицию файлов.
            canvasComponent.draw(fileNodes); // Перерисовываем канвас.
        }
    }



    public CanvasComponent getCanvasComponent() {
        return canvasComponent;
    }

    public FileTableComponent getFileTableComponent() {
        return fileTableComponent;
    }

    public FileInfoComponent getFileInfoComponent() {
        return fileInfoComponent;
    }
}