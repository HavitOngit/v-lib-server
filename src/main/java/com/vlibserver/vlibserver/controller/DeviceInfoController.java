package com.vlibserver.vlibserver.controller;

import com.vlibserver.vlibserver.model.DeviceInfo;
import com.vlibserver.vlibserver.service.DeviceInfoService;
import com.vlibserver.vlibserver.repository.DeviceInfoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DeviceInfoController {

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    /**
     * Endpoint to get device information from the database
     * 
     * @return ResponseEntity with device information
     */
    @GetMapping("/device-info")
    public ResponseEntity<Map<String, Object>> getDeviceInfo() {
        // Get device info from database
        DeviceInfo deviceInfo = deviceInfoRepository.findFirstByOrderByIdAsc();

        // If no device info exists, create one
        if (deviceInfo == null) {
            deviceInfoService.updateDeviceInfo();
            deviceInfo = deviceInfoRepository.findFirstByOrderByIdAsc();
        }

        // Create response map
        Map<String, Object> response = new HashMap<>();
        response.put("deviceUUID", deviceInfo.getDeviceUUID());
        response.put("ipAddress", deviceInfo.getIpAddress());
        response.put("hostname", deviceInfo.getHostname());
        response.put("lastUpdated", deviceInfo.getLastUpdated());

        return ResponseEntity.ok(response);
    }
}