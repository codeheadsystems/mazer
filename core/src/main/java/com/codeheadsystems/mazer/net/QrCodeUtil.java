package com.codeheadsystems.mazer.net;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Generates QR code textures from strings using ZXing.
 */
public class QrCodeUtil {

    /**
     * Generates a LibGDX Texture containing a QR code for the given string.
     *
     * @param content the text to encode
     * @param size    pixel dimensions of the texture (square)
     * @return a Texture containing the QR code, or null on failure
     */
    public static Texture generateQrTexture(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);

            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();

            pixmap.setColor(Color.BLACK);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (matrix.get(x, y)) {
                        pixmap.drawPixel(x, y);
                    }
                }
            }

            Texture texture = new Texture(pixmap);
            pixmap.dispose();
            return texture;
        } catch (WriterException e) {
            return null;
        }
    }
}
