package com.vlibserver.vlibserver.model;

import java.io.Serializable;

public class Video implements Serializable {
    private String id;
    private String title;
    private String path;
    private String format;
    private long size;
    private String duration;

    // Default constructor
    public Video() {
    }

    // Constructor with all fields
    public Video(String id, String title, String path, String format, long size, String duration) {
        this.id = id;
        this.title = title;
        this.path = path;
        this.format = format;
        this.size = size;
        this.duration = duration;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "Video{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", path='" + path + '\'' +
                ", format='" + format + '\'' +
                ", size=" + size +
                ", duration='" + duration + '\'' +
                '}';
    }
}

package com.vlibserver.vlibserver.model.Video;

