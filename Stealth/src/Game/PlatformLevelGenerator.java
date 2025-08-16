package Game;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlatformLevelGenerator extends LevelGenerator {
    private static final int MIN_PLATFORM_WIDTH = 3;
    private static final int MAX_PLATFORM_WIDTH = 8;
    private static final int MIN_GAP_WIDTH = 2;
    private static final int MAX_GAP_WIDTH = 5;
    private static final int MIN_PLATFORM_HEIGHT = 2;
    private static final int MAX_PLATFORM_HEIGHT = 5;
    
    public PlatformLevelGenerator() {
        super();
        generatePlatformLevel();
    }
    
    public int[] getRandomFloorPosition() {
        ArrayList<int[]> validPositions = new ArrayList<>();
        
        // Find all platform tiles that have air above them (for player/guard spawning)
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                if (map[x][y] == PLATFORM && map[x][y-1] == GAP) {
                    validPositions.add(new int[]{x, y-1}); // Position above platform
                }
            }
        }
        
        if (validPositions.isEmpty()) {
            return new int[]{WIDTH/2, HEIGHT/2}; // Fallback position
        }
        
        return validPositions.get(rand.nextInt(validPositions.size()));
    }
    
    
    @Override
    public int[] getValidPlayerSpawn(List<int[]> guardPositions) {
        ArrayList<int[]> validPositions = new ArrayList<>();
        
        // Find positions on the left side of the level (for player spawn)
        for (int x = 1; x < WIDTH/4; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {
                if (map[x][y] == PLATFORM && map[x][y-1] == GAP) {
                    boolean tooCloseToGuard = false;
                    for (int[] guardPos : guardPositions) {
                        if (Math.abs(x - guardPos[0]) <= 5) {
                            tooCloseToGuard = true;
                            break;
                        }
                    }
                    if (!tooCloseToGuard) {
                        validPositions.add(new int[]{x, y-1});
                    }
                }
            }
        }
        
        if (validPositions.isEmpty()) {
            return getRandomFloorPosition(); // Fallback
        }
        
        return validPositions.get(rand.nextInt(validPositions.size()));
    }
    
    private void generatePlatformLevel() {
        // Clear the map
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                map[x][y] = GAP; // Start with all gaps
            }
        }
        
        // Create platforms
        int currentX = 1;
        Random rand = new Random();
        
        while (currentX < WIDTH - 1) {
            // Create a platform
            int platformWidth = MIN_PLATFORM_WIDTH + rand.nextInt(MAX_PLATFORM_WIDTH - MIN_PLATFORM_WIDTH);
            int platformHeight = MIN_PLATFORM_HEIGHT + rand.nextInt(MAX_PLATFORM_HEIGHT - MIN_PLATFORM_HEIGHT);
            int platformY = HEIGHT - platformHeight - 1;
            
            // Make sure we don't go off screen
            if (currentX + platformWidth >= WIDTH - 1) {
                platformWidth = WIDTH - currentX - 2;
            }
            
            // Create the platform
            for (int x = currentX; x < currentX + platformWidth; x++) {
                for (int y = platformY; y < HEIGHT - 1; y++) {
                    map[x][y] = PLATFORM;
                }
            }
            
            // Add grapple points randomly
            if (rand.nextDouble() < 0.3) { // 30% chance for grapple point
                int grappleX = currentX + rand.nextInt(platformWidth);
                map[grappleX][platformY - 1] = GRAPPLE_POINT;
                // Make sure there's a platform tile below the grapple point
                map[grappleX][platformY] = PLATFORM;
            }
            
            // Move to next platform position
            int gapWidth = MIN_GAP_WIDTH + rand.nextInt(MAX_GAP_WIDTH - MIN_GAP_WIDTH);
            currentX += platformWidth + gapWidth;
        }
        
        // Place exit at far right
        map[WIDTH-2][HEIGHT-2] = EXIT;
    }
    
    @Override
    public boolean isWalkable(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return false;
        int tile = map[x][y];
        // Platforms are walkable, and gaps are walkable when falling through them
        return tile == PLATFORM || tile == EXIT || tile == GAP;
    }
    
    @Override
    public boolean isHideable(int x, int y) {
        return false; // No hideable objects in platform levels
    }
}