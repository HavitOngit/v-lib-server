package com.vlibserver.vlibserver.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;

/**
 * Utility class for generating QR codes
 */
public class QRCodeGenerator {

    /**
     * Generate a QR code as a BufferedImage from a given text
     *
     * @param text   The text to encode in the QR code
     * @param width  Width of the QR code in pixels
     * @param height Height of the QR code in pixels
     * @return BufferedImage representing the QR code
     * @throws WriterException If there is an error writing the QR code
     */
    public static BufferedImage generateQRCode(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}