package Game;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelGenerator {
    // Tile constants
    public static final int TILE_SIZE = 32;
    public static final int WIDTH = 25;   // 800px / 32px
    public static final int HEIGHT = 18;   // 600px / 32px
    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int EXIT = 2;
    public static final int HIDEOBJ = 3;  // Hideable objects (crates/bushes)
    
 // Add to constants section
    public static final int PLATFORM = 4;
    public static final int GRAPPLE_POINT = 5;
    public static final int GAP = 6;

    protected  int[][] map;
    private final Random rand;
    private int exitX, exitY;

    public LevelGenerator() {
        this.map = new int[WIDTH][HEIGHT];
        this.rand = new Random();
        generateOpenArena();
    }

    private void generateOpenArena() {
        // 1. Fill entire map with floors
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                map[x][y] = FLOOR;
            }
        }

        // 2. Create border walls
        createBorders();

        // 3. Add decorative interior walls
        addDecorativeWalls();

        // 4. Place hideable objects
        placeHideableObjects(8); // Place 8 hideable objects

        // 5. Place exit
        placeExit();
    }

    private void createBorders() {
        // Horizontal borders
        for (int x = 0; x < WIDTH; x++) {
            map[x][0] = WALL;           // Top border
            map[x][HEIGHT-1] = WALL;    // Bottom border
        }
        // Vertical borders
        for (int y = 0; y < HEIGHT; y++) {
            map[0][y] = WALL;           // Left border
            map[WIDTH-1][y] = WALL;     // Right border
        }
    }

    private void addDecorativeWalls() {
        // Add some random wall segments for visual interest
        for (int i = 0; i < 5; i++) {
            int wallX = 3 + rand.nextInt(WIDTH - 6);
            int wallY = 3 + rand.nextInt(HEIGHT - 6);
            int length = 3 + rand.nextInt(4);
            boolean horizontal = rand.nextBoolean();

            if (horizontal) {
                for (int x = wallX; x < wallX + length && x < WIDTH - 1; x++) {
                    map[x][wallY] = WALL;
                }
            } else {
                for (int y = wallY; y < wallY + length && y < HEIGHT - 1; y++) {
                    map[wallX][y] = WALL;
                }
            }
        }
    }
    
 // In LevelGenerator.java
    public boolean hasLineOfSight(int x1, int y1, int x2, int y2) {
        // Bresenham's line algorithm to check for walls between two points
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        
        int err = dx - dy;
        
        while (true) {
            // If we've reached the target without hitting a wall
            if (x1 == x2 && y1 == y2) {
                return true;
            }
            
            // If we hit a wall along the way
            if (getTile(x1, y1) == WALL) {
                return false;
            }
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void placeHideableObjects(int count) {
        ArrayList<int[]> possiblePositions = new ArrayList<>();
        
        // Collect all valid positions (not near borders or exit)
        for (int x = 2; x < WIDTH - 2; x++) {
            for (int y = 2; y < HEIGHT - 2; y++) {
                if (map[x][y] == FLOOR && !isNearExit(x, y)) {
                    possiblePositions.add(new int[]{x, y});
                }
            }
        }
        
        // Shuffle and place the objects
        Collections.shuffle(possiblePositions, rand);
        int objectsToPlace = Math.min(count, possiblePositions.size());
        
        for (int i = 0; i < objectsToPlace; i++) {
            int[] pos = possiblePositions.get(i);
            map[pos[0]][pos[1]] = HIDEOBJ;
        }
    }

    private boolean isNearExit(int x, int y) {
        // Don't place hideable objects near exit
        return Math.abs(x - exitX) <= 2 && Math.abs(y - exitY) <= 2;
    }

    private void placeExit() {
        // Randomly select border (0=top, 1=right, 2=bottom, 3=left)
        int border = rand.nextInt(4);
        
        switch (border) {
            case 0: // Top border
                exitX = 1 + rand.nextInt(WIDTH - 2);
                exitY = 0;
                break;
            case 1: // Right border
                exitX = WIDTH - 1;
                exitY = 1 + rand.nextInt(HEIGHT - 2);
                break;
            case 2: // Bottom border
                exitX = 1 + rand.nextInt(WIDTH - 2);
                exitY = HEIGHT - 1;
                break;
            case 3: // Left border
                exitX = 0;
                exitY = 1 + rand.nextInt(HEIGHT - 2);
                break;
        }
        
        map[exitX][exitY] = EXIT;
        
        // Ensure path to exit is clear
        if (exitY == 0) map[exitX][exitY+1] = FLOOR;          // Top exit
        else if (exitX == WIDTH-1) map[exitX-1][exitY] = FLOOR; // Right exit
        else if (exitY == HEIGHT-1) map[exitX][exitY-1] = FLOOR; // Bottom exit
        else map[exitX+1][exitY] = FLOOR;                      // Left exit
    }

    public int getTile(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return WALL;
        return map[x][y];
    }

    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return false;
        int tile = map[x][y];
        return tile == FLOOR || tile == EXIT; // Both floor and exit tiles are walkable
    }

    public boolean isHideable(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return false;
        return map[x][y] == HIDEOBJ;
    }
    
    public void setTile(int x, int y, int tileType) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            map[x][y] = tileType;
        }
    }
    

    public int getExitX() { return exitX; }
    public int getExitY() { return exitY; }

    public int[] getRandomFloorPosition() {
        ArrayList<int[]> validPositions = new ArrayList<>();
        
        // Find all floor tiles not too close to borders or exit
        for (int x = 2; x < WIDTH - 2; x++) {
            for (int y = 2; y < HEIGHT - 2; y++) {
                if (map[x][y] == FLOOR && !isNearExit(x, y)) {
                    validPositions.add(new int[]{x, y});
                }
            }
        }
        
        if (validPositions.isEmpty()) {
            return new int[]{WIDTH/2, HEIGHT/2}; // Fallback position
        }
        
        return validPositions.get(rand.nextInt(validPositions.size()));
    }

    // Helper method for debugging
    public void printMap() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                System.out.print(map[x][y] + " ");
            }
            System.out.println();
        }
    }
    
    
    
    public boolean isValidSpawnPosition(int x, int y, List<int[]> guardPositions) {
        // Check if position is walkable and not a wall
        if (!isWalkable(x, y)) return false;
        
        // Check distance to exit (minimum 5 tiles away)
        if (Math.abs(x - exitX) <= 5 && Math.abs(y - exitY) <= 5) return false;
        
        // Check distance to guards (minimum 3 tiles away)
        for (int[] guardPos : guardPositions) {
            if (Math.abs(x - guardPos[0]) <= 3 && Math.abs(y - guardPos[1]) <= 3) {
                return false;
            }
        }
        
        return true;
    }
    
    public int[] getValidPlayerSpawn(List<int[]> guardPositions) {
        ArrayList<int[]> validPositions = new ArrayList<>();
        
        for (int x = 2; x < WIDTH - 2; x++) {
            for (int y = 2; y < HEIGHT - 2; y++) {
                if (isValidSpawnPosition(x, y, guardPositions)) {
                    validPositions.add(new int[]{x, y});
                }
            }
        }
        
        if (validPositions.isEmpty()) {
            return new int[]{WIDTH/2, HEIGHT/2}; // Fallback position
        }
        
        return validPositions.get(rand.nextInt(validPositions.size()));
    }
    
    
    
    public boolean isAtExit(int x, int y) {
        return x == exitX && y == exitY;
    }
    
}