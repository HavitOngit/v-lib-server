package com.vlibserver.vlibserver.controller;

import com.vlibserver.vlibserver.service.VideoScanService;
import com.example.videostreaming.service.VideoStreamService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RestController
@RequestMapping("/api/videos")
public class VideoController {
    private final VideoScanService videoScanService;
    private final VideoStreamService videoStreamService;

    public VideoController(VideoScanService videoScanService, VideoStreamService videoStreamService) {
        this.videoScanService = videoScanService;
        this.videoStreamService = videoStreamService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Video> getAllVideos() {
        return videoScanService.getAllVideos();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Video>> getVideo(@PathVariable String id) {
        return videoScanService.getVideoById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{id}/stream")
    public Mono<ResponseEntity<Flux<DataBuffer>>> streamVideo(
            @PathVariable String id,
            @RequestHeader HttpHeaders headers,
            ServerHttpResponse response) {

        return videoStreamService.getVideoDetails(id)
                .map(tuple -> {
                    Video video = tuple.getT1();
                    MediaType mediaType = tuple.getT2();

                    Tuple2<Long, Long> rangeValues = videoStreamService.calculateRangeValues(id, headers);
                    long rangeStart = rangeValues.getT1();
                    long rangeEnd = rangeValues.getT2();
                    long contentLength = rangeEnd - rangeStart + 1;
                    long fullContentLength = videoStreamService.getContentLength(id);

                    // Set up headers for streaming
                    HttpHeaders responseHeaders = response.getHeaders();
                    responseHeaders.setContentType(mediaType);
                    responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.getTitle() + "\"");
                    responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                    responseHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));

                    HttpStatus status;
                    if (!headers.getRange().isEmpty()) {
                        status = HttpStatus.PARTIAL_CONTENT;
                        String contentRange = "bytes " + rangeStart + "-" + rangeEnd + "/" + fullContentLength;
                        responseHeaders.add(HttpHeaders.CONTENT_RANGE, contentRange);
                    } else {
                        status = HttpStatus.OK;
                    }

                    Flux<DataBuffer> body = videoStreamService.streamVideo(id, headers);
                    return ResponseEntity.status(status).body(body);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/scan")
    public Mono<ResponseEntity<String>> scanDirectory() {
        videoScanService.refreshCatalog();
        return Mono.just(ResponseEntity.ok("Video catalog refreshed successfully"));
    }
}