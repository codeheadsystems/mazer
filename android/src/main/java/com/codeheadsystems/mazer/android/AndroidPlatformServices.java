package com.codeheadsystems.mazer.android;

import com.codeheadsystems.mazer.platform.PlatformServices;

/**
 * Android implementation of PlatformServices.
 * Uses zxing-android-embedded for QR code scanning via the camera.
 */
public class AndroidPlatformServices implements PlatformServices {

    private final AndroidLauncher activity;
    private QrScanCallback pendingCallback;

    public AndroidPlatformServices(AndroidLauncher activity) {
        this.activity = activity;
    }

    @Override
    public boolean isQrScanAvailable() {
        return true;
    }

    @Override
    public void scanQrCode(QrScanCallback callback) {
        this.pendingCallback = callback;
        activity.launchQrScanner();
    }

    /**
     * Called by AndroidLauncher when the scan result comes back.
     */
    void onScanResult(String result) {
        if (pendingCallback != null) {
            QrScanCallback cb = pendingCallback;
            pendingCallback = null;
            com.badlogic.gdx.Gdx.app.postRunnable(() -> cb.onResult(result));
        }
    }
}
