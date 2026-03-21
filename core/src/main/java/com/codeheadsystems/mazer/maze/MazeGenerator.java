package com.codeheadsystems.mazer.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Generates a perfect maze using the recursive backtracker (depth-first search) algorithm.
 * Seeded for deterministic generation — same seed produces the same maze on all clients.
 */
public class MazeGenerator {

    private static final int[][] DIRECTIONS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    /**
     * Generates a maze of the given dimensions.
     *
     * @param width    number of cells wide (1-32)
     * @param height   number of cells tall (1-32)
     * @param cellSize world units per cell
     * @param seed     random seed for deterministic generation
     * @return a fully generated MazeGrid
     */
    public static MazeGrid generate(int width, int height, float cellSize, long seed) {
        if (width < 1 || width > 32 || height < 1 || height > 32) {
            throw new IllegalArgumentException("Maze dimensions must be 1-32, got " + width + "x" + height);
        }

        MazeGrid grid = new MazeGrid(width, height, cellSize);
        Random random = new Random(seed);

        Deque<MazeCell> stack = new ArrayDeque<>();
        MazeCell start = grid.getCell(0, 0);
        start.visited = true;
        stack.push(start);

        while (!stack.isEmpty()) {
            MazeCell current = stack.peek();
            List<MazeCell> unvisitedNeighbors = getUnvisitedNeighbors(grid, current);

            if (unvisitedNeighbors.isEmpty()) {
                stack.pop();
            } else {
                MazeCell next = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                grid.removeWallBetween(current.x, current.y, next.x, next.y);
                next.visited = true;
                stack.push(next);
            }
        }

        return grid;
    }

    private static List<MazeCell> getUnvisitedNeighbors(MazeGrid grid, MazeCell cell) {
        List<MazeCell> neighbors = new ArrayList<>(4);
        for (int[] dir : DIRECTIONS) {
            MazeCell neighbor = grid.getCell(cell.x + dir[0], cell.y + dir[1]);
            if (neighbor != null && !neighbor.visited) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }
}
