package com.vlibserver.vlibserver.repository;

import com.vlibserver.vlibserver.model.DeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceInfoRepository extends JpaRepository<DeviceInfo, Long> {
    DeviceInfo findFirstByOrderByIdAsc();
}