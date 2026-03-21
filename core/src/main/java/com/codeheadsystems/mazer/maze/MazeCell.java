package com.codeheadsystems.mazer.maze;

/**
 * Represents a single cell in the maze grid.
 * Each cell tracks which of its four walls are present.
 */
public class MazeCell {

    public final int x;
    public final int y;
    public boolean wallNorth = true;
    public boolean wallSouth = true;
    public boolean wallEast = true;
    public boolean wallWest = true;
    boolean visited = false; // package-private, used during generation

    public MazeCell(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
