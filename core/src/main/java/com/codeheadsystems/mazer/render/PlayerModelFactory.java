package com.codeheadsystems.mazer.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;

/**
 * Creates wireframe 3D models for the different player shapes.
 */
public class PlayerModelFactory {

    public enum Shape {
        CUBE, SPHERE, EYEBALL;

        public static Shape fromIndex(int index) {
            return values()[index % values().length];
        }
    }

    private static final float SIZE = 0.8f; // units across

    /**
     * Creates a wireframe model for the given shape and color.
     */
    public static Model create(Shape shape, Color color) {
        return switch (shape) {
            case CUBE -> buildCube(color);
            case SPHERE -> buildSphere(color);
            case EYEBALL -> buildEyeball(color);
        };
    }

    private static Model buildCube(Color color) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        MeshPartBuilder b = mb.part("cube", GL20.GL_LINES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());
        b.setColor(color);

        float s = SIZE / 2f;

        // Bottom face
        b.line(-s, -s, -s, s, -s, -s);
        b.line(s, -s, -s, s, -s, s);
        b.line(s, -s, s, -s, -s, s);
        b.line(-s, -s, s, -s, -s, -s);

        // Top face
        b.line(-s, s, -s, s, s, -s);
        b.line(s, s, -s, s, s, s);
        b.line(s, s, s, -s, s, s);
        b.line(-s, s, s, -s, s, -s);

        // Vertical edges
        b.line(-s, -s, -s, -s, s, -s);
        b.line(s, -s, -s, s, s, -s);
        b.line(s, -s, s, s, s, s);
        b.line(-s, -s, s, -s, s, s);

        return mb.end();
    }

    private static Model buildSphere(Color color) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        MeshPartBuilder b = mb.part("sphere", GL20.GL_LINES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());
        b.setColor(color);

        float r = SIZE / 2f;
        int segments = 16;

        // Three circles: XY, XZ, YZ planes
        buildCircle(b, r, segments, 0); // XZ plane (equator)
        buildCircle(b, r, segments, 1); // XY plane (front-back)
        buildCircle(b, r, segments, 2); // YZ plane (left-right)

        return mb.end();
    }

    private static void buildCircle(MeshPartBuilder b, float r, int segments, int plane) {
        for (int i = 0; i < segments; i++) {
            float a1 = MathUtils.PI2 * i / segments;
            float a2 = MathUtils.PI2 * (i + 1) / segments;

            float x1, y1, z1, x2, y2, z2;
            switch (plane) {
                case 0 -> { // XZ
                    x1 = MathUtils.cos(a1) * r; y1 = 0; z1 = MathUtils.sin(a1) * r;
                    x2 = MathUtils.cos(a2) * r; y2 = 0; z2 = MathUtils.sin(a2) * r;
                }
                case 1 -> { // XY
                    x1 = MathUtils.cos(a1) * r; y1 = MathUtils.sin(a1) * r; z1 = 0;
                    x2 = MathUtils.cos(a2) * r; y2 = MathUtils.sin(a2) * r; z2 = 0;
                }
                default -> { // YZ
                    x1 = 0; y1 = MathUtils.cos(a1) * r; z1 = MathUtils.sin(a1) * r;
                    x2 = 0; y2 = MathUtils.cos(a2) * r; z2 = MathUtils.sin(a2) * r;
                }
            }
            b.line(x1, y1, z1, x2, y2, z2);
        }
    }

    private static Model buildEyeball(Color color) {
        ModelBuilder mb = new ModelBuilder();
        mb.begin();
        MeshPartBuilder b = mb.part("eyeball", GL20.GL_LINES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.ColorPacked,
                new Material());
        b.setColor(color);

        float r = SIZE / 2f;
        int segments = 16;

        // Outer sphere (3 circles)
        buildCircle(b, r, segments, 0);
        buildCircle(b, r, segments, 1);
        buildCircle(b, r, segments, 2);

        // Iris: smaller circle on the front face (+X direction), offset forward
        float irisR = r * 0.35f;
        float irisOffset = r * 0.85f;
        b.setColor(Color.RED);
        for (int i = 0; i < segments; i++) {
            float a1 = MathUtils.PI2 * i / segments;
            float a2 = MathUtils.PI2 * (i + 1) / segments;
            b.line(
                    irisOffset, MathUtils.cos(a1) * irisR, MathUtils.sin(a1) * irisR,
                    irisOffset, MathUtils.cos(a2) * irisR, MathUtils.sin(a2) * irisR);
        }

        // Pupil: even smaller circle
        float pupilR = r * 0.15f;
        float pupilOffset = r * 0.9f;
        for (int i = 0; i < segments; i++) {
            float a1 = MathUtils.PI2 * i / segments;
            float a2 = MathUtils.PI2 * (i + 1) / segments;
            b.line(
                    pupilOffset, MathUtils.cos(a1) * pupilR, MathUtils.sin(a1) * pupilR,
                    pupilOffset, MathUtils.cos(a2) * pupilR, MathUtils.sin(a2) * pupilR);
        }

        return mb.end();
    }
}
