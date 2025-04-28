package com.vlibserver.vlibserver.service;

import com.vlibserver.vlibserver.model.DeviceInfo;
import com.vlibserver.vlibserver.repository.DeviceInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.UUID;

@Service
public class DeviceInfoService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceInfoService.class);
    private static final String REMOTE_API_URL = "https://real-name.havitonline.workers.dev/set-ip";
    @Autowired
    private DeviceInfoRepository deviceInfoRepository;

    private final WebClient webClient;

    public DeviceInfoService() {
        this.webClient = WebClient.builder().build();
    }

    /**
     * Updates or creates device information in the database and syncs with remote
     * service
     * if IP has changed
     */
    public void updateDeviceInfo() {
        try {
            String currentIp = getLocalIPv4Address();
            String hostname = InetAddress.getLocalHost().getHostName();

            DeviceInfo deviceInfo = deviceInfoRepository.findFirstByOrderByIdAsc();
            boolean isNewDevice = false;
            boolean ipChanged = false;

            if (deviceInfo == null) {
                // First time running the application, create new device info
                String deviceUUID = UUID.randomUUID().toString();
                deviceInfo = new DeviceInfo(deviceUUID, currentIp, hostname, LocalDateTime.now());
                isNewDevice = true;
                ipChanged = true;
            } else if (!deviceInfo.getIpAddress().equals(currentIp) || !deviceInfo.getHostname().equals(hostname)) {
                // IP or hostname has changed, update it
                deviceInfo.setIpAddress(currentIp);
                deviceInfo.setHostname(hostname);
                deviceInfo.setLastUpdated(LocalDateTime.now());
                ipChanged = true;
            }

            // Save to database
            deviceInfoRepository.save(deviceInfo);

            if (isNewDevice) {
                logger.info("Created new device with UUID: {}", deviceInfo.getDeviceUUID());
            }

            if (ipChanged) {
                // Notify remote service about the IP change
                notifyRemoteService(deviceInfo.getDeviceUUID(), currentIp);
            } else {
                logger.info("Device information is up to date. No changes detected.", getLocalIPv4Address());
            }

        } catch (Exception e) {
            logger.error("Error updating device information", e);
        }
    }

    /**
     * Gets the device UUID. Creates a new one if none exists.
     */
    public String getDeviceUUID() {
        DeviceInfo deviceInfo = deviceInfoRepository.findFirstByOrderByIdAsc();
        if (deviceInfo == null) {
            updateDeviceInfo();
            deviceInfo = deviceInfoRepository.findFirstByOrderByIdAsc();
        }
        return deviceInfo.getDeviceUUID();
    }

    /**
     * Gets the local IPv4 address of the machine.
     * Looks for a non-loopback, non-virtual IPv4 address.
     *
     * @return The IPv4 address as a String, or null if not found
     * @throws Exception if an error occurs while getting network interfaces
     */
    public String getLocalIPv4Address() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            // Skip loopback, virtual, and inactive interfaces
            if (iface.isLoopback() || iface.isVirtual() || !iface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = iface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr.getHostAddress().indexOf(':') < 0) { // IPv4 address (no colons)
                    return addr.getHostAddress();
                }
            }
        }
        return null;
    }

    /**
     * Sends the device IP to the remote service
     *
     * @param deviceUUID the device UUID
     * @param ipAddress  the device IP address
     */
    private void notifyRemoteService(String deviceUUID, String ipAddress) {
        logger.info("Notifying remote service about IP change: {} -> {}", deviceUUID, ipAddress);

        webClient.post()
                .uri(REMOTE_API_URL)
                .bodyValue(new IpUpdateRequest(deviceUUID, ipAddress))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    logger.error("Failed to update remote service with IP change", e);
                    DeviceInfo deviceInfo = deviceInfoRepository.findFirstByOrderByIdAsc();
                    deviceInfo.setIpAddress("1.1.1.1");
                    deviceInfoRepository.save(deviceInfo);
                    return Mono.empty();
                })
                .subscribe(response -> logger.info("Successfully notified remote service about IP change"));
    }

    // Simple POJO for the request body
    private static class IpUpdateRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("token")
        private final String token;

        @com.fasterxml.jackson.annotation.JsonProperty("ip")
        private final String ip;

        public IpUpdateRequest(String token, String ip) {
            this.token = token;
            this.ip = ip;
        }

        // Removed unused getter methods
    }
}