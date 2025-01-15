package org.example.DAO;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.OCommandParameters;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

import java.util.*;


public class OrientDBFileTagSystem {
    private static String dbUrl = "plocal:mydatabase";
    private static ODatabaseSession db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin");
    /*
    public static void main(String[] args) {
        String dbUrl = "plocal:mydatabase"; // Замените на ваш URL базы данных
        try (ODatabaseSession db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin")) {
            // createSchema(db);
            // addFiles(db);
            searchFilesByTag(db, "java");
        } catch (Exception e) {

        }
    }
     */

    public static void connect(){

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
        // Assuming tags is a collection (like a Set or List), we use concat to add the new tag.
        String query = "UPDATE File SET tags = concat(tags, [:newTag]) WHERE path = :path";
        try {

        } catch (Exception e) {
            System.err.println("Error adding tag to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Set<OElement> createTags(ArrayList<String> tagNames) {
        Set<OElement> tags = new HashSet<>();
        for (String tagName : tagNames) {
            OElement tag = db.newElement("Tag");
            tag.setProperty("name", tagName);
            db.save(tag);
            tags.add(tag);
        }
        return tags;
    }


    public static void searchFilesByTag(String tagName) {
        String query = "SELECT FROM File WHERE tags CONTAINS (SELECT FROM Tag WHERE name = ?)"; // Используем подготовленный запрос
        try (OResultSet result = db.query(query, tagName)) {
            System.out.println("Files with tag '" + tagName + "':");
            if (!result.hasNext()) {
                System.out.println("No files found with this tag.");
                return; // Выходим, если результатов нет
            }

            while (result.hasNext()) {
                OResult item = result.next();
                String path = item.getProperty("path");
                if (path != null) {
                    System.out.println(path);
                } else {
                    System.out.println("Path is null for this file. Check database integrity.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
    }

    public static ArrayList<String> getTagsByPath(String path) {
        // Запрос с параметром для поиска по пути
        String query = "SELECT FROM File WHERE path = ?";

        try (OResultSet result = db.query(query, path)) {
            if (!result.hasNext()) {
                System.out.println("No files found with this path: " + path);
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
            e.printStackTrace();
            return new ArrayList<>(); // В случае ошибки возвращаем пустой список
        }
    }


}
