package org.example.dao;

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;


public class OrientDBFileTagSystem {
    private static String dbUrl = "plocal:mydatabase";
    private static ODatabaseSession db;

    static {
        db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin");
        // Устанавливаем базу данных для текущего потока
        ODatabaseRecordThreadLocal.instance().set((ODatabaseDocumentInternal) db);
    }

    /*
    public static void main(String[] args) {
        String dbUrl = "plocal:mydatabase"; // Замените на ваш URL базы данных
        try (ODatabaseSession db = new ODatabaseDocumentTx(dbUrl).open("admin", "admin")) {
            createSchema();
            // addFiles(db);
            // searchFilesByTag("java");
        } catch (Exception e) {

        }
    }
     */

    private static void createSchema() {
        OSchema schema = db.getMetadata().getSchema();
        OClass fileClass = schema.createClass("File");
        fileClass.createProperty("path", OType.STRING);
        fileClass.createProperty("tags", OType.LINKSET, schema.getClass("Tag")); // Используем LINKSET для множества тегов

        OClass tagClass = schema.createClass("Tag");
        tagClass.createProperty("name", OType.STRING);

        db.commit();
    }

    public static ODatabaseSession getDb() {
        return db;
    }
}
