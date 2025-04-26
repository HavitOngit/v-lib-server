package com.vlibserver.vlibserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoStreamService {

    @Value("${video.directory:/home/havit/Videos/}")
    private String videoDirectory;

    public Mono<Resource> getVideo(String title) {
        return Mono.fromSupplier(() -> {
            Path videoPath = Paths.get(videoDirectory, title);
            return new FileSystemResource(videoPath);
        });
    }
}