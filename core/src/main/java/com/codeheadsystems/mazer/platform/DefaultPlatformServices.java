package com.codeheadsystems.mazer.platform;

/**
 * Default (no-op) implementation for platforms that don't support scanning.
 */
public class DefaultPlatformServices implements PlatformServices {

    @Override
    public boolean isQrScanAvailable() {
        return false;
    }

    @Override
    public void scanQrCode(QrScanCallback callback) {
        callback.onResult(null);
    }
}
