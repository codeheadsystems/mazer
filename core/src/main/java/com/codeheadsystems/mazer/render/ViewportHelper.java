package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Creates density-aware viewports so UI elements are legible on high-DPI screens.
 */
public class ViewportHelper {

    /**
     * Creates a ScreenViewport that scales UI based on screen density.
     * On standard desktop (density ~1.0), no scaling. On high-DPI phones
     * (density 2-4x), UI elements are scaled up proportionally.
     */
    public static ScreenViewport createScaledViewport() {
        ScreenViewport viewport = new ScreenViewport();
        float density = Gdx.graphics.getDensity();
        // On desktop, density is typically ~1.0; on phones, 2.0-4.0
        // We want roughly 160 DPI equivalent sizing
        if (density > 1.0f) {
            viewport.setUnitsPerPixel(1f / density);
        }
        return viewport;
    }
}
