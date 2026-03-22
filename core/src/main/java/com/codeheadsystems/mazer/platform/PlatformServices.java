package com.codeheadsystems.mazer.platform;

/**
 * Platform-specific services interface. Implemented per-platform (Android, Desktop).
 * Injected into MazerGame at launch.
 */
public interface PlatformServices {

    /**
     * Returns true if QR code scanning is available on this platform.
     */
    boolean isQrScanAvailable();

    /**
     * Launches a QR code scanner. The result is delivered asynchronously
     * via the callback. Called on the GL thread.
     *
     * @param callback receives the scanned text, or null if cancelled/failed
     */
    void scanQrCode(QrScanCallback callback);

    interface QrScanCallback {
        void onResult(String scannedText);
    }
}
