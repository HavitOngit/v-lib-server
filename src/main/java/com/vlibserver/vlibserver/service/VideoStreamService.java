package com.vlibserver.vlibserver.service;

import com.vlibserver.vlibserver.model.Video;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class VideoStreamService {
    private final VideoScanService videoScanService;

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

    public Mono<ResourceRegion> getResourceRegion(String id, HttpHeaders headers) {
        return videoScanService.getVideoById(id)
                .map(video -> {
                    Resource resource = new FileSystemResource(video.getPath());
                    try {
                        long contentLength = resource.contentLength();
                        List<HttpRange> ranges = headers.getRange();

                        if (ranges.isEmpty()) {
                            // No range header, return the entire resource
                            return new ResourceRegion(resource, 0, contentLength);
                        } else {
                            // Return the requested range
                            HttpRange range = ranges.get(0);
                            long start = range.getRangeStart(contentLength);
                            long end = range.getRangeEnd(contentLength);
                            long rangeLength = end - start + 1;
                            return new ResourceRegion(resource, start, rangeLength);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
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