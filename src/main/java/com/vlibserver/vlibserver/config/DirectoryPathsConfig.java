package com.vlibserver.vlibserver.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DirectoryPathsConfig {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryPathsConfig.class);
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    @Value("${app.video.output.location:./video-output.json}")
    private String outputLocation;
    
    private List<String> paths = new ArrayList<>();
    
    public DirectoryPathsConfig(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource("classpath:paths.json");
            InputStream inputStream = resource.getInputStream();
            paths = objectMapper.readValue(inputStream, new TypeReference<List<String>>() {});
            logger.info("Loaded {} directory paths from configuration", paths.size());
        } catch (IOException e) {
            logger.error("Failed to load paths.json", e);
            // Initialize with empty list if file can't be read
            paths = new ArrayList<>();
        }
    }
    
    public List<String> getPaths() {
        return paths;
    }
    
    public String getOutputLocation() {
        return outputLocation;
    }
}

