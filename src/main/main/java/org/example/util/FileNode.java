package org.example.util;

import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Класс, представляющий файл или директорию как узел в графе. Содержит информацию о позиции на холсте,
 * о выделении узла и связанные теги.
 */
public class FileNode {
    private File file;  // Файл или директория
    private double x;  // Координата X на холсте
    private double y;  // Координата Y на холсте
    private double transformX;  // Преобразованная координата X (с учётом смещения)
    private double transformY;  // Преобразованная координата Y (с учётом смещения)
    private boolean isSelected;  // Флаг выделения узла
    public ArrayList<String> tags = new ArrayList<>();  // Список тегов, связанных с файлом

    /**
     * Конструктор для создания узла с файлом и по умолчанию без координат.
     *
     * @param file Файл или директория, представленные узлом.
     */
    public FileNode(File file) {
        this.file = file;
    }

    /**
     * Получает список тегов, связанных с файлом или директорией.
     *
     * @return Список тегов.
     */
    public ArrayList<String> getTags() {
        return tags;
    }

    /**
     * Устанавливает список тегов для файла или директории.
     *
     * @param tags Список тегов, которые будут связаны с файлом или директорией.
     */
    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    /**
     * Конструктор для создания узла с файлом, координатами и флагом корневой директории.
     *
     * @param file  Файл или директория, представленные узлом.
     * @param x     Координата X на холсте.
     * @param y     Координата Y на холсте.
     * @param isRoot Флаг, указывающий, является ли файл корневым (по умолчанию корневой узел выделен).
     */
    public FileNode(File file, double x, double y, boolean isRoot) {
        this.file = file;
        this.x = x;
        this.y = y;
        this.isSelected = isRoot; // Корневая директория всегда выделена
    }

    /**
     * Получает файл или директорию, представленные узлом.
     *
     * @return Файл или директория.
     */
    public File getFile() {
        return file;
    }

    /**
     * Получает координату X узла на холсте.
     *
     * @return Координата X.
     */
    public double getX() {
        return x;
    }

    /**
     * Получает координату Y узла на холсте.
     *
     * @return Координата Y.
     */
    public double getY() {
        return y;
    }

    /**
     * Проверяет, является ли узел директорией.
     *
     * @return true, если файл является директорией, иначе false.
     */
    public boolean isDirectory() {
        return file.isDirectory();
    }

    /**
     * Получает преобразованную координату X с учётом смещения.
     *
     * @return Преобразованная координата X.
     */
    public double getTransformX() {
        return transformX;
    }

    /**
     * Устанавливает преобразованную координату X с учётом смещения.
     *
     * @param transformX Преобразованная координата X.
     */
    public void setTransformX(double transformX) {
        this.transformX = transformX;
    }

    /**
     * Получает преобразованную координату Y с учётом смещения.
     *
     * @return Преобразованная координата Y.
     */
    public double getTransformY() {
        return transformY;
    }

    /**
     * Устанавливает преобразованную координату Y с учётом смещения.
     *
     * @param transformY Преобразованная координата Y.
     */
    public void setTransformY(double transformY) {
        this.transformY = transformY;
    }

    /**
     * Получает цвет узла в зависимости от его выделенности.
     *
     * @return Цвет узла.
     */
    public Color getColor() {
        return isSelected ? Color.web("#007bff") : Color.GRAY;  // Синий, если выбран, серый - если не выбран
    }

    /**
     * Проверяет, содержит ли узел точку с заданными координатами, с учётом смещения и зума.
     *
     * @param x           Координата X для проверки.
     * @param y           Координата Y для проверки.
     * @param offsetX     Смещение по оси X.
     * @param offsetY     Смещение по оси Y.
     * @param zoomLevel   Уровень зума.
     * @return true, если точка находится внутри узла, иначе false.
     */
    public boolean containsPoint(double x, double y, double offsetX, double offsetY, double zoomLevel) {
        double localX = this.x + offsetX;  // Корректируем координаты с учётом смещения
        double localY = this.y + offsetY;
        return Math.pow(x - localX, 2) + Math.pow(y - localY, 2) <= Math.pow(25 * zoomLevel, 2);  // Проверяем попадание в область круга
    }

    /**
     * Проверяет, выбран ли узел.
     *
     * @return true, если узел выбран, иначе false.
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Устанавливает флаг выбранности узла.
     *
     * @param selected true, если узел должен быть выбран, иначе false.
     */
    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    /**
     * Устанавливает файл или директорию для узла.
     *
     * @param file Файл или директория, который будет ассоциирован с узлом.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Переопределение метода equals для сравнения узлов по файлам.
     *
     * @param o Объект для сравнения.
     * @return true, если объекты равны, иначе false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileNode fileNode)) return false;
        return Objects.equals(file, fileNode.file);  // Сравниваем файлы
    }

    /**
     * Переопределение метода hashCode для использования в коллекциях.
     *
     * @return Хэш-код для узла.
     */
    @Override
    public int hashCode() {
        return Objects.hash(file);  // Хэшируем файл
    }
}
