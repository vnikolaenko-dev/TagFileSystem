package org.example.app.utils;

import org.example.app.components.CanvasComponent;

import java.awt.*;
import java.io.File;

public class FileUtil {
    /**
     * Метод для получения размера файла с форматированием в байтах, килобайтах, мегабайтах или гигабайтах.
     *
     * @param file Файл, для которого нужно определить размер.
     * @return Строка с размером файла в удобном формате (например, "2.3 MB").
     */
    public static String getFileSize(File file) {
        long sizeInBytes = file.length();
        if (sizeInBytes < 1024) {
            return sizeInBytes + " байт"; // Менее 1 КБ
        } else if (sizeInBytes < 1024 * 1024) {
            // Если размер меньше 1 МБ, отображаем в КБ
            return String.format("%.2f КБ", sizeInBytes / 1024.0);
        } else if (sizeInBytes < 1024 * 1024 * 1024) {
            // Если размер меньше 1 ГБ, отображаем в МБ
            return String.format("%.2f МБ", sizeInBytes / (1024.0 * 1024));
        } else {
            // Если размер больше 1 ГБ, отображаем в ГБ
            return String.format("%.2f ГБ", sizeInBytes / (1024.0 * 1024 * 1024));
        }
    }

    public static void openFile(File selectedFile) {
        try {
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
}
