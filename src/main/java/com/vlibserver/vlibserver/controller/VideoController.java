package com.vlibserver.vlibserver.controller;

import com.vlibserver.vlibserver.model.Video;
import com.vlibserver.vlibserver.service.VideoScanService;
import com.vlibserver.vlibserver.service.VideoStreamService;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2; // Added import

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
    public Mono<ResponseEntity<ResourceRegion>> streamVideo(
            @PathVariable String id,
            @RequestHeader HttpHeaders headers) {

        return videoStreamService.getVideoDetails(id)
                .flatMap(videoDetails -> {
                    // Video video = videoDetails.getT1(); // Video object might be needed later,
                    // keep commented if unused
                    MediaType mediaType = videoDetails.getT2();

                    return videoStreamService.getResourceRegion(id, headers)
                            .map(resourceRegion -> {
                                long contentLength = videoStreamService.getContentLength(id);
                                Tuple2<Long, Long> range = videoStreamService.calculateRangeValues(id, headers);
                                long start = range.getT1();
                                long end = range.getT2();
                                long resourceLength = resourceRegion.getCount();

                                HttpStatus status = headers.getRange().isEmpty() ? HttpStatus.OK
                                        : HttpStatus.PARTIAL_CONTENT;

                                ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(status)
                                        .contentType(mediaType)
                                        .header(HttpHeaders.ACCEPT_RANGES, "bytes");

                                if (status == HttpStatus.PARTIAL_CONTENT) {
                                    responseBuilder.header(HttpHeaders.CONTENT_RANGE,
                                            "bytes " + start + "-" + end + "/" + contentLength);
                                    responseBuilder.contentLength(resourceLength);
                                } else {
                                    responseBuilder.contentLength(contentLength);
                                }

                                return responseBuilder.body(resourceRegion);
                            });
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/scan")
    public Mono<ResponseEntity<String>> scanDirectory() {
        videoScanService.refreshCatalog();
        return Mono.just(ResponseEntity.ok("Video catalog refreshed successfully"));
    }
}