package com.codeheadsystems.mazer.maze;

import com.badlogic.gdx.math.Vector2;

/**
 * 2D grid of maze cells with world-coordinate helpers and collision detection.
 * The maze exists on the XZ plane in 3D space. Each cell is cellSize x cellSize world units.
 */
public class MazeGrid {

    private final int width;
    private final int height;
    private final float cellSize;
    private final MazeCell[][] cells;

    public MazeGrid(int width, int height, float cellSize) {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.cells = new MazeCell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = new MazeCell(x, y);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getCellSize() {
        return cellSize;
    }

    public MazeCell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return cells[x][y];
    }

    /**
     * Returns the world-space center of the given cell.
     */
    public Vector2 cellToWorld(int cx, int cy) {
        return new Vector2(cx * cellSize + cellSize / 2f, cy * cellSize + cellSize / 2f);
    }

    /**
     * Returns the grid coordinates for a world position.
     * Result is clamped to valid grid bounds.
     */
    public int worldToCellX(float wx) {
        int cx = (int) (wx / cellSize);
        return Math.max(0, Math.min(cx, width - 1));
    }

    public int worldToCellY(float wz) {
        int cy = (int) (wz / cellSize);
        return Math.max(0, Math.min(cy, height - 1));
    }

    /**
     * Checks if a circle at world position (wx, wz) with the given radius
     * collides with any maze wall. Uses axis-aligned wall segments.
     */
    public boolean collidesWithWall(float wx, float wz, float radius) {
        // Check the cell the point is in plus neighboring cells
        int cx = worldToCellX(wx);
        int cy = worldToCellY(wz);

        // Check a 3x3 area around the player's cell
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = cx + dx;
                int ny = cy + dy;
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    // Outer boundary — treat as solid
                    if (collidesWithBoundary(wx, wz, radius)) {
                        return true;
                    }
                    continue;
                }
                if (collidesWithCellWalls(wx, wz, radius, nx, ny)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean collidesWithBoundary(float wx, float wz, float radius) {
        float maxX = width * cellSize;
        float maxZ = height * cellSize;
        return wx - radius < 0 || wx + radius > maxX || wz - radius < 0 || wz + radius > maxZ;
    }

    private boolean collidesWithCellWalls(float wx, float wz, float radius, int cx, int cy) {
        MazeCell cell = cells[cx][cy];
        float cellLeft = cx * cellSize;
        float cellBottom = cy * cellSize;
        float cellRight = cellLeft + cellSize;
        float cellTop = cellBottom + cellSize;

        // North wall (top edge of cell): horizontal segment from cellLeft to cellRight at cellTop
        if (cell.wallNorth) {
            if (circleIntersectsHorizontalSegment(wx, wz, radius, cellLeft, cellRight, cellTop)) {
                return true;
            }
        }
        // South wall (bottom edge of cell): horizontal segment at cellBottom
        if (cell.wallSouth) {
            if (circleIntersectsHorizontalSegment(wx, wz, radius, cellLeft, cellRight, cellBottom)) {
                return true;
            }
        }
        // East wall (right edge of cell): vertical segment from cellBottom to cellTop at cellRight
        if (cell.wallEast) {
            if (circleIntersectsVerticalSegment(wx, wz, radius, cellBottom, cellTop, cellRight)) {
                return true;
            }
        }
        // West wall (left edge of cell): vertical segment at cellLeft
        if (cell.wallWest) {
            if (circleIntersectsVerticalSegment(wx, wz, radius, cellBottom, cellTop, cellLeft)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a circle at (cx, cz) with radius r intersects a horizontal line segment
     * from (x1, z) to (x2, z).
     */
    private boolean circleIntersectsHorizontalSegment(float cx, float cz, float r,
                                                       float x1, float x2, float z) {
        // Distance from circle center to the line z=z
        float dz = Math.abs(cz - z);
        if (dz >= r) return false;

        // Clamp circle center x to segment range
        float closestX = Math.max(x1, Math.min(cx, x2));
        float dx = cx - closestX;
        return dx * dx + dz * dz < r * r;
    }

    /**
     * Checks if a circle at (cx, cz) with radius r intersects a vertical line segment
     * from (x, z1) to (x, z2).
     */
    private boolean circleIntersectsVerticalSegment(float cx, float cz, float r,
                                                     float z1, float z2, float x) {
        float dx = Math.abs(cx - x);
        if (dx >= r) return false;

        float closestZ = Math.max(z1, Math.min(cz, z2));
        float dz = cz - closestZ;
        return dx * dx + dz * dz < r * r;
    }

    /**
     * Removes the wall between two adjacent cells.
     * Used during maze generation.
     */
    void removeWallBetween(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;

        if (dx == 1) {
            cells[x1][y1].wallEast = false;
            cells[x2][y2].wallWest = false;
        } else if (dx == -1) {
            cells[x1][y1].wallWest = false;
            cells[x2][y2].wallEast = false;
        } else if (dy == 1) {
            cells[x1][y1].wallNorth = false;
            cells[x2][y2].wallSouth = false;
        } else if (dy == -1) {
            cells[x1][y1].wallSouth = false;
            cells[x2][y2].wallNorth = false;
        }
    }
}
