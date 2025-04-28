package com.vlibserver.vlibserver.config;

import com.google.zxing.WriterException;
import com.vlibserver.vlibserver.service.DeviceInfoService;
import com.vlibserver.vlibserver.util.QRCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Component to generate and display a QR code of the server's UUID in the
 * terminal when the application starts.
 */
@Component
public class ServerAddressQRCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ServerAddressQRCodeGenerator.class);
    private static final int QR_CODE_SIZE = 25;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @EventListener(ApplicationStartedEvent.class)
    public void generateServerAddressQRCode() {
        try {
            // Update device info (IP, hostname, UUID) on startup
            deviceInfoService.updateDeviceInfo();

            // Get device UUID for QR code
            String deviceUUID = deviceInfoService.getDeviceUUID();
            String deviceIP = deviceInfoService.getLocalIPv4Address();

            if (deviceUUID != null) {
                // Generate QR code for the UUID instead of IP
                displayQRCodeInTerminal(deviceUUID);

                // Still display the IP address for convenience

                System.out.println("\nServer UUID: " + deviceUUID);
                System.out.println("Server IP: " + deviceIP);

                System.out.println("Scan the QR code to access this device's unique identifier.");

            } else {
                logger.error("Could not generate device UUID");
            }
        } catch (Exception e) {
            logger.error("Failed to generate QR code: {}", e.getMessage(), e);
        }
    }

    /**
     * Displays a QR code in the terminal as ASCII art.
     *
     * @param text The text to encode in the QR code
     * @throws WriterException if an error occurs during QR code generation
     */
    private void displayQRCodeInTerminal(String text) throws WriterException {
        var qrCodeImage = QRCodeGenerator.generateQRCode(text, QR_CODE_SIZE, QR_CODE_SIZE);

        StringBuilder asciiQR = new StringBuilder();
        asciiQR.append("\n");

        for (int y = 0; y < qrCodeImage.getHeight(); y++) {
            asciiQR.append("  ");
            for (int x = 0; x < qrCodeImage.getWidth(); x++) {
                int pixelColor = qrCodeImage.getRGB(x, y);
                // Dark pixels for QR code data, light pixels for background
                boolean isDark = (pixelColor & 0x00FFFFFF) == 0;
                asciiQR.append(isDark ? "██" : "  ");
            }
            asciiQR.append("\n");
        }

        System.out.println(asciiQR);
    }
}