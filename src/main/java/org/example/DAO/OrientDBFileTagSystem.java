package org.example.DAO;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

import java.util.*;
import java.util.stream.Collectors;


public class OrientDBFileTagSystem {
    private static String dbUrl = "plocal:mydatabase";
    public static ODatabaseSession db;

    static {
        db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin");
        // Устанавливаем базу данных для текущего потока
        ODatabaseRecordThreadLocal.instance().set((ODatabaseDocumentInternal) db);
    }


    public static void main(String[] args) {
        String dbUrl = "plocal:mydatabase"; // Замените на ваш URL базы данных
        try (ODatabaseSession db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin")) {
            createSchema();
            // addFiles(db);
            // searchFilesByTag("java");
        } catch (Exception e) {

        }
    }




    public static void connect() {

    }


    private static void createSchema() {
        OSchema schema = db.getMetadata().getSchema();
        OClass fileClass = schema.createClass("File");
        fileClass.createProperty("path", OType.STRING);
        fileClass.createProperty("tags", OType.LINKSET, schema.getClass("Tag")); // Используем LINKSET для множества тегов

        OClass tagClass = schema.createClass("Tag");
        tagClass.createProperty("name", OType.STRING);

        db.commit();
    }

    public static void addFile(String path, ArrayList<String> tags) {
        OElement file1 = db.newElement("File");
        file1.setProperty("path", path);
        file1.setProperty("tags", createTags(tags));
        db.save(file1);
    }

    public static void addTagToFile(String path, String tag) {
        String updateQuery = "UPDATE File SET tags = tags || (SELECT FROM Tag WHERE name = ?) WHERE path = ?";
        db.command(updateQuery, tag, path);
    }

    public static void removeTagFromFile(String path, String tag) {
        String updateQuery = "UPDATE File REMOVE tags = (SELECT FROM Tag WHERE name = ?) WHERE path = ?";
        db.command(updateQuery, tag, path);
    }



    public static OElement getTagByName(String tagName) {
        String query = "SELECT FROM Tag WHERE name = ?";

        try (OResultSet resultSet = db.query(query, tagName)) {
            return resultSet.stream() // Преобразуем ResultSet в Stream
                    .findFirst() // Находим первый элемент (если есть)
                    .map(OResult::toElement) // Преобразуем OResult в OElement
                    .orElse(null); // Возвращаем null, если элемент не найден
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }



    public static boolean createTags(ArrayList<String> tagNames) {
        Set<OElement> tags = new HashSet<>();
        for (String tagName : tagNames) {
            OElement element = getTagByName(tagName);
            System.out.println(element);
            if (element != null){
                tags.add(element);
            } else {
                OElement tag = db.newElement("Tag");
                tag.setProperty("name", tagName);
                db.save(tag);
                tags.add(tag);
            }
        }
        return true;
    }

    public static boolean removeTags(ArrayList<String> tagNames) {
        for (String tagName : tagNames) {
            OElement element = getTagByName(tagName);
            try {
                assert element != null;
                element.delete();
            } catch (NullPointerException e){
                System.out.println("INCORRECT TAG FOR REMOVE");
                return false;
            }
        }
        return true;
    }

    public static HashSet<String> getTags() {
        HashSet<String> tags = (HashSet<String>) db.query("SELECT name FROM Tag").stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        HashSet<String> normalViewTags = new HashSet<>();
        for(String tag: tags){
            normalViewTags.add(tag.substring(tag.toString().indexOf("name:") + 5, tag.toString().indexOf("}")).replace("\n", "").replace(" ", ""));
        }
        return normalViewTags;
    }


    public static List<String> searchFilesByTag(String tagName) {
        List<String> files = new ArrayList<>();
        String query = "SELECT path FROM File WHERE tags CONTAINS (SELECT FROM Tag WHERE name = ?)";

        try (OResultSet result = db.query(query, tagName)) {
            while (result.hasNext()) {
                OResult item = result.next();
                String path = item.getProperty("path");
                if (path != null) {
                    files.add(path);
                    System.out.println("Found file: " + path);
                } else {
                    System.err.println("Path is null for a file. Check database integrity.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            e.printStackTrace();
        }

        if (files.isEmpty()) {
            System.out.println("No files found with tag: " + tagName);
        }

        return files;
    }


    public static ArrayList<String> getTagsByPath(String path) {
        // Запрос с параметром для поиска по пути
        String query = "SELECT FROM File WHERE path = ?";

        try (OResultSet result = db.query(query, path)) {
            if (result == null || !result.hasNext()) {
                // System.out.println("No files found with this path: " + path);
                return new ArrayList<>(); // Возвращаем пустой список, если не нашли файл
            }

            // Получаем первый результат
            OResult item = result.next();

            // Получаем поле tags и приводим к нужному типу
            Object tags = item.getProperty("tags");

            if (tags == null) {
                return new ArrayList<>(); // Если tags пусто, возвращаем пустой список
            }

            // Если tags - это коллекция, например Set или List
            if (tags instanceof Iterable) {
                ArrayList<String> tagList = new ArrayList<>();
                for (Object tag : (Iterable<?>) tags) {
                    if (tag != null) {
                        tagList.add(tag.toString().substring(tag.toString().indexOf("name:") + 5, tag.toString().indexOf("}"))); // Преобразуем элементы в строки
                    }
                }
                return tagList;
            } else {
                System.err.println("Tags field is not a valid collection: " + tags.getClass().getName());
                return new ArrayList<>(); // Если tags не коллекция, возвращаем пустой список
            }
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            return new ArrayList<>(); // В случае ошибки возвращаем пустой список
        }
    }


}
