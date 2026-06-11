package com.fileexplorer;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileItem {
    private final File file;

    public FileItem(File file) {
        this.file = file;
    }

    public File getFile() { return file; }

    public String getName() { return file.getName(); }

    public boolean isDirectory() { return file.isDirectory(); }

    public String getFormattedSize() {
        if (file.isDirectory()) return "";
        long size = file.length();
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return new DecimalFormat("#.#").format(size / 1024.0) + " KB";
        if (size < 1024 * 1024 * 1024) return new DecimalFormat("#.#").format(size / (1024.0 * 1024)) + " MB";
        return new DecimalFormat("#.#").format(size / (1024.0 * 1024 * 1024)) + " GB";
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(file.lastModified()));
    }

    public String getExtension() {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    public FileType getFileType() {
        if (file.isDirectory()) return FileType.FOLDER;
        switch (getExtension()) {
            case "jpg": case "jpeg": case "png": case "gif": case "webp": case "bmp": return FileType.IMAGE;
            case "mp4": case "mkv": case "avi": case "mov": case "3gp": return FileType.VIDEO;
            case "mp3": case "flac": case "wav": case "aac": case "ogg": case "m4a": return FileType.AUDIO;
            case "pdf": return FileType.PDF;
            case "zip": case "rar": case "7z": case "tar": case "gz": return FileType.ARCHIVE;
            case "apk": return FileType.APK;
            case "txt": case "log": case "md": case "xml": case "json": case "java": case "kt": return FileType.TEXT;
            case "doc": case "docx": case "xls": case "xlsx": case "ppt": case "pptx": return FileType.DOCUMENT;
            default: return FileType.OTHER;
        }
    }

    public enum FileType {
        FOLDER, IMAGE, VIDEO, AUDIO, PDF, ARCHIVE, APK, TEXT, DOCUMENT, OTHER
    }
}
