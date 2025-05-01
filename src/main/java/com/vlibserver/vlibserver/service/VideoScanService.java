package com.vlibserver.vlibserver.service;

import com.vlibserver.vlibserver.model.Video;
import com.vlibserver.vlibserver.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class VideoScanService {

    private static final Logger logger = LoggerFactory.getLogger(VideoScanService.class);

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".mpg", ".mpeg", ".3gp"));

    @Value("${video.directory:/home/havit/Videos/}")
    private String videoDirectory;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private MovieMetadataService movieMetadataService;

    /**
     * Run video scan on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("Application started - running initial video scan");
        scanVideos().subscribe(
                count -> logger.info("Initial scan completed. Found {} videos", count),
                error -> logger.error("Error during initial scan", error));

        // Process existing videos that don't have metadata
        updateMissingMetadata();
    }

    /**
     * Updates metadata for videos that don't have it yet
     */
    private void updateMissingMetadata() {
        logger.info("Checking for videos without metadata");
        List<Video> videos = videoRepository.findAll()
                .stream()
                .filter(video -> video.getTmdbId() == null || video.getOmdbId() == null)
                .toList();

        if (videos.isEmpty()) {
            logger.info("No videos found without metadata");
            return;
        }

        logger.info("Found {} videos without metadata, fetching...", videos.size());

        Flux.fromIterable(videos)
                .flatMap(movieMetadataService::fetchAndUpdateMetadata)
                .flatMap(video -> Mono.fromCallable(() -> videoRepository.save(video)))
                .count()
                .subscribe(
                        count -> logger.info("Updated metadata for {} videos", count),
                        error -> logger.error("Error updating metadata", error));
    }

    /**
     * Scans the video directory for videos and saves them to the database.
     * 
     * @return Mono<Long> the number of videos found
     */
    public Mono<Long> scanVideos() {
        // First, mark all existing videos as unavailable
        return Mono.fromCallable(() -> {
            List<Video> existingVideos = videoRepository.findAll();

            // Mark all videos unavailable initially
            existingVideos.forEach(video -> {
                video.setAvailable(false);
                videoRepository.save(video);
            });

            logger.info("Marked {} existing videos as unavailable before scan", existingVideos.size());
            return existingVideos;
        }).flatMap(existingVideos -> {
            // Create a map of existing videos by path for efficient lookup
            Map<String, Video> videoMap = new HashMap<>();
            existingVideos.forEach(video -> videoMap.put(video.getPath(), video));

            // Scan for video files
            return Flux.fromIterable(findVideoFiles(new File(videoDirectory)))
                    .map(this::createVideoFromFile)
                    .doOnNext(video -> logger.info("Found video: {}", video.getName()))
                    .flatMap(scannedVideo -> Mono.fromCallable(() -> {
                        // Check if video already exists in database
                        Video existingVideo = videoMap.get(scannedVideo.getPath());

                        if (existingVideo != null) {
                            // Video exists in DB, update it
                            if (existingVideo.getLastModified().isBefore(scannedVideo.getLastModified()) ||
                                    !existingVideo.getSize().equals(scannedVideo.getSize())) {
                                // Update if the file was modified or size changed
                                existingVideo.setName(scannedVideo.getName());
                                existingVideo.setSize(scannedVideo.getSize());
                                existingVideo.setLastModified(scannedVideo.getLastModified());
                                existingVideo.setFormat(scannedVideo.getFormat());
                                logger.info("Updating existing video: {}", existingVideo.getName());
                            }
                            // Mark as available regardless of whether it was modified
                            existingVideo.setAvailable(true);
                            return existingVideo;
                        } else {
                            // New video, save it (already marked available by default)
                            logger.info("Adding new video to database: {}", scannedVideo.getName());
                            return scannedVideo;
                        }
                    }))
                    // Fetch and update metadata for new or modified videos
                    .flatMap(video -> {
                        // Only fetch metadata if it's a new video or the video doesn't have metadata
                        // yet
                        if (videoMap.get(video.getPath()) == null || video.getTmdbId() == null
                                || video.getOmdbId() == null) {
                            logger.info("Fetching metadata for video: {}", video.getName());
                            return movieMetadataService.fetchAndUpdateMetadata(video);
                        }
                        return Mono.just(video);
                    })
                    // Save the updated video to the database
                    .flatMap(video -> Mono.fromCallable(() -> videoRepository.save(video)))
                    .count();
        });
    }

    /**
     * Gets all videos from the database.
     * 
     * @return List<Video> list of all videos
     */
    public List<Video> getAllVideos() {
        return videoRepository.findAll();
    }

    /**
     * Gets all available videos from the database.
     * 
     * @return List<Video> list of available videos
     */
    public List<Video> getAvailableVideos() {
        return videoRepository.findByAvailable(true);
    }

    /**
     * Recursively finds all video files in a directory.
     * 
     * @param directory the directory to scan
     * @return List<File> a list of video files
     */
    private List<File> findVideoFiles(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            logger.warn("Directory does not exist or is not a directory: {}", directory.getAbsolutePath());
            return List.of();
        }

        logger.info("Scanning directory: {}", directory.getAbsolutePath());

        File[] files = directory.listFiles();
        if (files == null) {
            logger.warn("Failed to list files in directory: {}", directory.getAbsolutePath());
            return List.of();
        }

        return Arrays.stream(files)
                .flatMap(file -> {
                    if (file.isDirectory()) {
                        return findVideoFiles(file).stream();
                    } else if (isVideoFile(file)) {
                        return Arrays.stream(new File[] { file });
                    }
                    return Arrays.stream(new File[0]);
                })
                .toList();
    }

    /**
     * Checks if a file is a video file based on its extension.
     * 
     * @param file the file to check
     * @return boolean true if the file is a video file
     */
    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return VIDEO_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    /**
     * Creates a Video object from a File.
     * 
     * @param file the file to convert
     * @return Video the Video object
     */
    private Video createVideoFromFile(File file) {
        String name = file.getName();
        String path = file.getAbsolutePath();
        String format = getFileExtension(name);
        long size = file.length();
        LocalDateTime lastModified = LocalDateTime.ofInstant(
                java.nio.file.attribute.FileTime.fromMillis(file.lastModified()).toInstant(),
                ZoneId.systemDefault());

        Video video = new Video(name, path, format, size, lastModified);
        video.setAvailable(true); // Mark new videos as available by default
        return video;
    }

    /**
     * Gets the file extension of a file.
     * 
     * @param filename the name of the file
     * @return String the extension of the file
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
}