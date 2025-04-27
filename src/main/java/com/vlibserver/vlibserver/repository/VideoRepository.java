package com.vlibserver.vlibserver.repository;

import com.vlibserver.vlibserver.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Video findByPath(String path);

    List<Video> findByAvailable(Boolean available);
}