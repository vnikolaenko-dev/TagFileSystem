package org.example.dao.service;

import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис для работы с тегами в базе данных OrientDB.
 * Включает методы для поиска тегов, создания новых тегов, удаления тегов и получения всех тегов.
 */
public class TagService {

    /**
     * Ищет тег по его имени в базе данных.
     *
     * @param db        Сессия базы данных.
     * @param tagName   Имя тега, который нужно найти.
     * @return          Элемент базы данных, представляющий найденный тег, или null, если тег не найден.
     */
    public static OElement getTagByName(ODatabaseSession db, String tagName) {
        String query = "SELECT FROM Tag WHERE name = ?";  // Запрос для поиска тега по имени.

        try (OResultSet resultSet = db.query(query, tagName)) {
            // Преобразование результата запроса в поток и извлечение первого найденного элемента.
            return resultSet.stream()
                    .findFirst()  // Находим первый элемент, если он существует.
                    .map(OResult::toElement)  // Преобразуем результат в OElement.
                    .orElse(null);  // Возвращаем null, если элемент не найден.
        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());  // Логируем ошибку выполнения запроса.
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Создает теги в базе данных. Если тег уже существует, возвращает его, иначе создает новый.
     *
     * @param db        Сессия базы данных.
     * @param tagNames  Список имен тегов для создания.
     * @return          Множество объектов OElement, представляющих созданные или найденные теги.
     */
    public static Set<OElement> createTags(ODatabaseSession db, ArrayList<String> tagNames) {
        Set<OElement> tags = new HashSet<>();  // Множество для хранения тегов.
        for (String tagName : tagNames) {
            OElement element = getTagByName(db, tagName);  // Поиск тега по имени.
            System.out.println(element);
            if (element != null) {
                System.out.println("Tag already exist");  // Тег уже существует, добавляем его в множество.
                tags.add(element);
            } else {
                // Если тег не найден, создаем новый и сохраняем в базе данных.
                OElement tag = db.newElement("Tag");
                tag.setProperty("name", tagName);
                db.save(tag);
                tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * Удаляет теги из базы данных.
     *
     * @param db        Сессия базы данных.
     * @param tagNames  Список имен тегов, которые нужно удалить.
     * @return          true, если все теги успешно удалены, иначе false.
     */
    public static boolean removeTags(ODatabaseSession db, ArrayList<String> tagNames) {
        for (String tagName : tagNames) {
            OElement element = getTagByName(db, tagName);  // Поиск тега по имени.
            try {
                assert element != null;  // Проверка, что элемент существует.
                element.delete();  // Удаление тега из базы данных.
            } catch (NullPointerException e) {
                System.out.println("INCORRECT TAG FOR REMOVE");  // Если тег не найден, выводим сообщение об ошибке.
                return false;
            }
        }
        return true;  // Все теги успешно удалены.
    }

    /**
     * Получает все уникальные имена тегов из базы данных.
     *
     * @param db    Сессия базы данных.
     * @return      Множество строк, представляющих имена всех тегов в базе данных.
     */
    public static HashSet<String> getTags(ODatabaseSession db) {
        // Запрос для получения всех тегов и их имен.
        HashSet<String> tags = (HashSet<String>) db.query("SELECT name FROM Tag").stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        HashSet<String> normalViewTags = new HashSet<>();
        // Обработка тегов, преобразуем их в более читаемый вид.
        for (String tag : tags) {
            normalViewTags.add(tag.substring(tag.toString().indexOf("name:") + 5, tag.toString().indexOf("}"))
                    .replace("\n", "").replace(" ", ""));
        }
        return normalViewTags;  // Возвращаем множество тегов в удобочитаемом формате.
    }
}
