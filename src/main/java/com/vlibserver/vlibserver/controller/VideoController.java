package com.vlibserver.vlibserver.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vlibserver.vlibserver.model.Video;
import com.vlibserver.vlibserver.service.VideoScanService;
import com.vlibserver.vlibserver.service.VideoStreamService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoStreamService videoStreamService;

    @Autowired
    private VideoScanService videoScanService;

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
    public Mono<ResponseEntity<Resource>> getVideo(@PathVariable(name = "title") String title,
            @RequestHeader(value = "Range", required = false) String range) {
        System.out.println("range in bytes: " + range);
        return videoStreamService.getVideo(title)
                .map(resource -> ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                        .body(resource));
    }
}