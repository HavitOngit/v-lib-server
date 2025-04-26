package com.vlibserver.vlibserver.service;

import com.vlibserver.vlibserver.model.Video; // Added import
import jakarta.annotation.PostConstruct; // Changed import from javax.annotation
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoScanService {
    private Map<String, Video> videoMap = new ConcurrentHashMap<>();
    private final String videoDirectory;

    public VideoScanService(@Value("${video.directory}") String videoDirectory) {
        this.videoDirectory = videoDirectory;
    }

    @PostConstruct
    public void scanVideoDirectory() {
        try {
            File directory = new File(videoDirectory);
            if (!directory.exists() || !directory.isDirectory()) {
                System.err.println("Video directory does not exist or is not a directory: " + videoDirectory);
                return;
            }

            System.out.println("Scanning video directory: " + videoDirectory);
            videoMap.clear();

            Files.walk(Paths.get(videoDirectory))
                    .filter(Files::isRegularFile)
                    .filter(path -> isVideoFile(path.toString()))
                    .forEach(this::processVideoFile);

            System.out.println("Found " + videoMap.size() + " video files");
        } catch (IOException e) {
            System.err.println("Error scanning video directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isVideoFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                lower.endsWith(".avi") || lower.endsWith(".mov") ||
                lower.endsWith(".webm") || lower.endsWith(".flv") ||
                lower.endsWith(".wmv") || lower.endsWith(".m4v");
    }

    private String getFormat(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    private long calculateSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            System.err.println("Error calculating file size: " + e.getMessage());
            return 0;
        }
    }

    private void processVideoFile(Path path) {
        try {
            String id = UUID.randomUUID().toString();
            String filename = path.getFileName().toString();

            Video video = new Video();
            video.setId(id);
            video.setTitle(filename);
            video.setPath(path.toString());
            video.setFormat(getFormat(filename));
            video.setSize(calculateSize(path));
            video.setDuration("Unknown"); // You could use a library like Xuggler to extract real duration

            videoMap.put(id, video);
            System.out.println("Added video: " + video.getTitle());
        } catch (Exception e) {
            System.err.println("Error processing video file " + path + ": " + e.getMessage());
        }
    }

    public Flux<Video> getAllVideos() {
        return Flux.fromIterable(videoMap.values());
    }

    public Mono<Video> getVideoById(String id) {
        return Mono.justOrEmpty(videoMap.get(id));
    }

    public void refreshCatalog() {
        scanVideoDirectory();
    }
}