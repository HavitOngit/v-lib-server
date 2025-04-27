package com.vlibserver.vlibserver.controller;

import com.vlibserver.vlibserver.model.Video;
import com.vlibserver.vlibserver.service.VideoScanService;
import com.vlibserver.vlibserver.service.VideoStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoStreamService videoStreamService;

    @Autowired
    private VideoScanService videoScanService;

    /**
     * Root endpoint to list all available video links
     * 
     * @return ResponseEntity with list of video links
     */
    @GetMapping(value = "/video-list", produces = "application/json")
    public ResponseEntity<Map<String, Object>> listVideoLinks() {
        List<Video> videos = videoScanService.getAvailableVideos();
        List<Map<String, Object>> videoLinks = videos.stream()
                .map(video -> {
                    Map<String, Object> videoData = new HashMap<>();
                    videoData.put("name", video.getName());
                    videoData.put("link", "/api/videos/" + video.getName());
                    videoData.put("format", video.getFormat());
                    videoData.put("size", video.getSize());
                    videoData.put("duration", video.getDuration());
                    videoData.put("available", video.getAvailable());
                    return videoData;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("videos", videoLinks);
        response.put("count", videoLinks.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to get all videos
     * 
     * @return Flux of Video objects
     */
    @GetMapping(produces = "application/json")
    public Flux<Video> getAllVideos() {
        return Flux.defer(() -> Flux.fromIterable(videoScanService.getAllVideos()));
    }

    /**
     * Endpoint to get only available videos
     * 
     * @return Flux of Video objects
     */
    @GetMapping(value = "/available", produces = "application/json")
    public Flux<Video> getAvailableVideos() {
        return Flux.defer(() -> Flux.fromIterable(videoScanService.getAvailableVideos()));
    }

    /**
     * Endpoint to scan the videos directory and update the database
     * 
     * @return ResponseEntity with the scan results
     */
    @PostMapping("/scan")
    public Mono<ResponseEntity<Map<String, Object>>> scanVideos() {
        return videoScanService.scanVideos()
                .map(count -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Video scan completed",
                        "count", count)));
    }

    /**
     * Endpoint to stream a specific video
     * 
     * @param title Video file name to stream
     * @param range HTTP Range header for partial content
     * @return ResponseEntity with video resource
     */
    @GetMapping(value = "/{title:.+}", produces = "video/mp4")
    public Mono<ResponseEntity<Resource>> getVideo(@PathVariable String title,
            @RequestHeader(value = "Range", required = false) String range) {
        System.out.println("range in bytes: " + range);
        return videoStreamService.getVideo(title)
                .map(resource -> ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                        .body(resource));
    }
}