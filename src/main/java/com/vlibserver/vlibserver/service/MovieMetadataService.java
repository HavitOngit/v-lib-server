package com.vlibserver.vlibserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vlibserver.vlibserver.model.Video;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class MovieMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MovieMetadataService.class);
    private static final String API_TOKEN = "super-user-token-749";
    private static final String NAME_API_URL = "https://real-name.havitonline.workers.dev/get-name";
    private static final String IDS_API_URL = "https://real-name.havitonline.workers.dev/get-ids";

    private final WebClient webClient;

    public MovieMetadataService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Fetch metadata for a video from external APIs and update the video object
     * 
     * @param video The video object to update with metadata
     * @return Mono<Video> The updated video object
     */
    public Mono<Video> fetchAndUpdateMetadata(Video video) {
        // Skip if we already have the metadata
        if (video.getTmdbId() != null && video.getOmdbId() != null) {
            logger.debug("Video already has metadata: {}", video.getName());
            return Mono.just(video);
        }

        return getPredictedName(video.getName())
                .flatMap(predictedName -> {
                    logger.info("Got predicted name for '{}': '{}'", video.getName(), predictedName);
                    video.setPredictedName(predictedName);
                    return getMovieIds(predictedName);
                })
                .flatMap(ids -> {
                    logger.info("Got IDs for '{}': TMDB={}, OMDB={}",
                            video.getPredictedName(), ids.tmdbId, ids.omdbId);
                    video.setTmdbId(ids.tmdbId);
                    video.setOmdbId(ids.omdbId);
                    return Mono.just(video);
                })
                .onErrorResume(e -> {
                    logger.warn("Error fetching metadata for video '{}': {}", video.getName(), e.getMessage());
                    return Mono.just(video);
                });
    }

    /**
     * Get the predicted name for a video file
     * 
     * @param filename The name of the video file
     * @return Mono<String> The predicted movie name
     */
    private Mono<String> getPredictedName(String filename) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", API_TOKEN);
        requestBody.put("name", filename);

        return webClient.post()
                .uri(NAME_API_URL)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.get("predicted_name").asText())
                .onErrorResume(WebClientResponseException.class, e -> {
                    logger.error("Error calling name API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(e);
                });
    }

    /**
     * Get movie IDs for a movie name
     * 
     * @param movieName The name of the movie
     * @return Mono<MovieIds> The TMDB and OMDB IDs
     */
    private Mono<MovieIds> getMovieIds(String movieName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", API_TOKEN);
        requestBody.put("name", movieName);

        return webClient.post()
                .uri(IDS_API_URL)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> new MovieIds(
                        response.get("tmdb_id").asLong(),
                        response.get("omdb_id").asText()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    logger.error("Error calling IDs API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(e);
                });
    }

    /**
     * Helper class for movie IDs
     */
    private static class MovieIds {
        private final Long tmdbId;
        private final String omdbId;

        public MovieIds(Long tmdbId, String omdbId) {
            this.tmdbId = tmdbId;
            this.omdbId = omdbId;
        }
    }
}