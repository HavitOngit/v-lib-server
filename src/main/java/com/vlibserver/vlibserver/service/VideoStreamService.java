package com.example.videostreaming.service;

import com.example.videostreaming.model.Video;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class VideoStreamService {
    private final VideoScanService videoScanService;
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    public VideoStreamService(VideoScanService videoScanService) {
        this.videoScanService = videoScanService;
    }

    public Mono<Tuple2<Video, MediaType>> getVideoDetails(String id) {
        return videoScanService.getVideoById(id)
                .map(video -> {
                    MediaType mediaType;
                    switch (video.getFormat().toLowerCase()) {
                        case "mp4":
                            mediaType = MediaType.parseMediaType("video/mp4");
                            break;
                        case "webm":
                            mediaType = MediaType.parseMediaType("video/webm");
                            break;
                        case "mkv":
                            mediaType = MediaType.parseMediaType("video/x-matroska");
                            break;
                        case "avi":
                            mediaType = MediaType.parseMediaType("video/x-msvideo");
                            break;
                        default:
                            mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    }
                    return Tuples.of(video, mediaType);
                });
    }

    public Flux<DataBuffer> streamVideo(String id, HttpHeaders headers) {
        return videoScanService.getVideoById(id)
                .flatMapMany(video -> {
                    Resource resource = new FileSystemResource(video.getPath());
                    File file = new File(video.getPath());
                    long contentLength = file.length();

                    try {
                        // Handle range request
                        List<HttpRange> ranges = headers.getRange();
                        if (ranges.isEmpty()) {
                            // No range header, stream the entire file
                            return DataBufferUtils.read(resource,
                                    new DefaultDataBufferFactory(),
                                    CHUNK_SIZE);
                        } else {
                            // Range request handling
                            HttpRange range = ranges.get(0);
                            long start = range.getRangeStart(contentLength);
                            long end = range.getRangeEnd(contentLength);
                            long rangeLength = end - start + 1;

                            return DataBufferUtils.read(resource,
                                    start,
                                    rangeLength,
                                    new DefaultDataBufferFactory(),
                                    CHUNK_SIZE);
                        }
                    } catch (IOException e) {
                        return Flux.error(e);
                    }
                });
    }

    public long getContentLength(String id) {
        return videoScanService.getVideoById(id)
                .map(video -> new File(video.getPath()).length())
                .block();
    }

    public Tuple2<Long, Long> calculateRangeValues(String id, HttpHeaders headers) {
        long contentLength = getContentLength(id);
        List<HttpRange> ranges = headers.getRange();

        if (ranges.isEmpty()) {
            return Tuples.of(0L, contentLength - 1);
        } else {
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            return Tuples.of(start, end);
        }
    }
}