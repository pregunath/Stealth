package Game;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

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
        return tile == PLATFORM || tile == EXIT; // Only platforms and exit are walkable
    }
    
    @Override
    public boolean isHideable(int x, int y) {
        return false; // No hideable objects in platform levels
    }
}