package com.vlibserver.vlibserver.controller;

import com.vlibserver.vlibserver.service.VideoStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideoStreamService videoStreamService;

    @GetMapping(value = "/{title}", produces = "video/mp4")
    public Mono<ResponseEntity<Resource>> getVideo(@PathVariable String title,
            @RequestHeader(value = "Range", required = false) String range) {
        System.out.println("range in bytes: " + range);
        return videoStreamService.getVideo(title)
                .map(resource -> ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                        .body(resource));
    }
}