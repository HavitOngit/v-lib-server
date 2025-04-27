package com.vlibserver.vlibserver.config;

import com.google.zxing.WriterException;
import com.vlibserver.vlibserver.util.QRCodeGenerator;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Component to generate and display a QR code of the server's address in the
 * terminal
 * when the application starts.
 */
@Component
public class ServerAddressQRCodeGenerator {

    private static final int PORT = 8080;
    private static final int QR_CODE_SIZE = 25;

    @EventListener(ApplicationStartedEvent.class)
    public void generateServerAddressQRCode() {
        try {
            String ipAddress = getLocalIPv4Address();
            if (ipAddress != null) {
                String serverAddress = "http://" + ipAddress + ":" + PORT;
                displayQRCodeInTerminal(serverAddress);
                System.out.println("\nServer running at: " + serverAddress);
                System.out.println("Scan the QR code to access the server from your device.");
            } else {
                System.out.println("Could not determine local IPv4 address.");
            }
        } catch (Exception e) {
            System.err.println("Failed to generate QR code: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets the local IPv4 address of the machine.
     * Looks for a non-loopback, non-virtual IPv4 address.
     *
     * @return The IPv4 address as a String, or null if not found
     * @throws Exception if an error occurs while getting network interfaces
     */
    private String getLocalIPv4Address() throws Exception {
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