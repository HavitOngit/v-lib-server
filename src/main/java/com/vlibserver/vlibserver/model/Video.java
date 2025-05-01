package com.vlibserver.vlibserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String path;

    private String format;

    private Long size; // in bytes

    private LocalDateTime lastModified;

    private Integer width;

    private Integer height;

    private Long duration; // in milliseconds

    @Column(nullable = false)
    private Boolean available = true;

    // Added fields for movie metadata
    private Long tmdbId;

    private String omdbId;

    private String predictedName;

    public Video() {
    }

    public Video(String name, String path, String format, Long size, LocalDateTime lastModified) {
        this.name = name;
        this.path = path;
        this.format = format;
        this.size = size;
        this.lastModified = lastModified;
        this.available = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getOmdbId() {
        return omdbId;
    }

    public void setOmdbId(String omdbId) {
        this.omdbId = omdbId;
    }

    public String getPredictedName() {
        return predictedName;
    }

    public void setPredictedName(String predictedName) {
        this.predictedName = predictedName;
    }

    @Override
    public String toString() {
        return "Video{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", format='" + format + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified +
                ", width=" + width +
                ", height=" + height +
                ", duration=" + duration +
                ", available=" + available +
                ", tmdbId=" + tmdbId +
                ", omdbId='" + omdbId + '\'' +
                ", predictedName='" + predictedName + '\'' +
                '}';
    }
}