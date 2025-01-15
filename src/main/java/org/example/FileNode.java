package org.example;

import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class FileNode {
    private File file;
    private double x;
    private double y;
    private double transformX;
    private double transformY;
    private boolean isSelected;
    public ArrayList<String> tags = new ArrayList<>();

    public FileNode(File file) {
        this.file = file;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public FileNode(File file, double x, double y, boolean isRoot) {
        this.file = file;
        this.x = x;
        this.y = y;
        this.isSelected = isRoot; // Корневая директория всегда выделена
    }

    public File getFile() {
        return file;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public double getTransformX() {
        return transformX;
    }

    public void setTransformX(double transformX) {
        this.transformX = transformX;
    }

    public double getTransformY() {
        return transformY;
    }

    public void setTransformY(double transformY) {
        this.transformY = transformY;
    }

    public Color getColor() {
        return isSelected ? Color.web("#007bff") : Color.GRAY;
    }

    public boolean containsPoint(double x, double y, double offsetX, double offsetY, double zoomLevel) {
        double localX = this.x + offsetX;
        double localY = this.y + offsetY;
        return Math.pow(x - localX, 2) + Math.pow(y - localY, 2) <= Math.pow(25 * zoomLevel, 2);
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileNode fileNode)) return false;
        return Objects.equals(file, fileNode.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}
