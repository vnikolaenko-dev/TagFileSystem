package org.example.dao.service;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с файлами в базе данных OrientDB.
 * Включает методы для добавления файлов, изменения их тегов, удаления тегов и поиска файлов по тегам.
 */
public class FileService {

    /**
     * Добавляет новый файл в базу данных.
     *
     * @param db    Сессия базы данных.
     * @param path  Путь к файлу.
     * @param tags  Список тегов, ассоциированных с файлом.
     */
    public static void addFile(ODatabaseSession db, String path, ArrayList<String> tags) {
        // Создание нового элемента типа "File" с заданным путем и тегами.
        OElement file1 = db.newElement("File");
        file1.setProperty("path", path);
        file1.setProperty("tags", TagService.createTags(db, tags));  // Создание и добавление тегов с использованием TagService
        db.save(file1);  // Сохранение элемента в базе данных
    }

    /**
     * Добавляет тег к существующему файлу.
     * Если файл не существует, создается новый файл с этим тегом.
     *
     * @param db    Сессия базы данных.
     * @param path  Путь к файлу.
     * @param tag   Тег для добавления.
     */
    public static void addTagToFile(ODatabaseSession db, String path, String tag) {
        String checkFileQuery = "SELECT FROM File WHERE path = ?";  // Проверка существования файла по пути
        OResultSet resultSet = db.query(checkFileQuery, path);

        // Если файл не найден, создаем новый файл с этим тегом
        if (!resultSet.hasNext()) {
            System.out.println("Файл с путем " + path + " не найден в базе данных.");
            ArrayList<String> tags = new ArrayList<>();
            tags.add(tag);
            addFile(db, path, tags);  // Добавляем новый файл
            resultSet.close();
            return;
        }
        System.out.println(resultSet.next().toString());  // Вывод информации о найденном файле

        resultSet.close();
        String updateQuery = "";

        // Обновление тегов файла
        if (!getTagsByPath(db, path).isEmpty()){
            updateQuery = "UPDATE File SET tags = tags || (SELECT FROM Tag WHERE name = ?) WHERE path = ?";
        } else {
            updateQuery = "UPDATE File SET tags = (SELECT FROM Tag WHERE name = ?) WHERE path = ?";
        }

        try {
            db.command(updateQuery, tag, path);  // Выполнение команды для добавления тега
        } catch (Exception e){
            System.out.println(e.getMessage());  // Обработка исключений
        }
    }

    /**
     * Удаляет тег у файла по его пути.
     *
     * @param db    Сессия базы данных.
     * @param path  Путь к файлу.
     * @param tag   Тег, который необходимо удалить.
     */
    public static void removeTagFromFile(ODatabaseSession db, String path, String tag) {
        String updateQuery = "UPDATE File REMOVE tags = (SELECT FROM Tag WHERE name = ?) WHERE path = ?";
        db.command(updateQuery, tag, path);  // Выполнение команды для удаления тега
    }

    /**
     * Ищет все файлы с заданным тегом.
     *
     * @param db        Сессия базы данных.
     * @param tagName   Имя тега для поиска.
     * @return          Список путей файлов, которые имеют указанный тег.
     */
    public static List<String> searchFilesByTag(ODatabaseSession db, String tagName) {
        List<String> files = new ArrayList<>();
        String query = "SELECT path FROM File WHERE tags CONTAINS (SELECT FROM Tag WHERE name = ?)";  // Запрос для поиска файлов по тегу

        try (OResultSet result = db.query(query, tagName)) {
            // Обработка результатов запроса
            while (result.hasNext()) {
                OResult item = result.next();
                String path = item.getProperty("path");
                if (path != null) {
                    files.add(path);  // Добавление найденного пути в список
                    System.out.println("Found file: " + path);  // Вывод найденного пути
                } else {
                    System.err.println("Path is null for a file. Check database integrity.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();  // Вывод ошибки при выполнении запроса
        }

        if (files.isEmpty()) {
            System.out.println("No files found with tag: " + tagName);  // Сообщение о том, что файлы не найдены
        }

        return files;
    }

    /**
     * Возвращает список тегов для файла по его пути.
     *
     * @param db    Сессия базы данных.
     * @param path  Путь к файлу.
     * @return      Список тегов, связанных с файлом.
     */
    public static ArrayList<String> getTagsByPath(ODatabaseSession db, String path) {
        String query = "SELECT FROM File WHERE path = ?";  // Запрос для поиска файла по пути

        try (OResultSet result = db.query(query, path)) {
            if (result == null || !result.hasNext()) {
                return new ArrayList<>();  // Возвращаем пустой список, если файл не найден
            }

            OResult item = result.next();
            Object tags = item.getProperty("tags");

            if (tags == null) {
                return new ArrayList<>();  // Если тегов нет, возвращаем пустой список
            }

            // Если tags - это коллекция
            if (tags instanceof Iterable) {
                ArrayList<String> tagList = new ArrayList<>();
                for (Object tag : (Iterable<?>) tags) {
                    if (tag != null) {
                        tagList.add(tag.toString().substring(tag.toString().indexOf("name:") + 5, tag.toString().indexOf("}")));
                    }
                }
                return tagList;  // Возвращаем список тегов
            } else {
                System.err.println("Tags field is not a valid collection: " + tags.getClass().getName());
                return new ArrayList<>();  // Если тег не коллекция, возвращаем пустой список
            }
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            return new ArrayList<>();  // В случае ошибки возвращаем пустой список
        }
    }
}
