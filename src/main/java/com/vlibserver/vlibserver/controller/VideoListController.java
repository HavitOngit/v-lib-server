package com.vlibserver.vlibserver.controller;

import com.vlibserver.vlibserver.model.Video;
import com.vlibserver.vlibserver.service.VideoScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class VideoListController {

    @Autowired
    private VideoScanService videoScanService;

    /**
     * Root endpoint to list all available video links
     * 
     * @return ResponseEntity with list of video links
     */
    @GetMapping("/videos")
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
}