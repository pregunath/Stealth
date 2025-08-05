package Game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class Player {
    // Movement properties
    private double x, y;
    private final double speed = 3;
    private final double width = 23;
    private final double height = 23;
    
    private boolean isJumping = false;
    private double verticalVelocity = 0;
    private static final double GRAVITY = 0.5;
    private static final double JUMP_FORCE = -30; // Increased from -12 to -15 for higher jump
    private static final double GRAPPLE_SPEED = 8;
    private boolean isGrappling = false;
    private double grappleTargetX, grappleTargetY;
    
   
    // Add this variable to track toggle state
    private boolean hideToggleRequested = false;
    
    private boolean holdToHideEnabled = false; // Default to toggle mode
    
    public void setHoldToHide(boolean enabled) {
        this.holdToHideEnabled = enabled;
    }
    
    public void jump() {
        if (!isJumping && !isGrappling) {
            verticalVelocity = JUMP_FORCE;
            isJumping = true;
        }
    }
    
    
    public void startGrapple(double targetX, double targetY) {
        if (!isGrappling) {
            grappleTargetX = targetX;
            grappleTargetY = targetY;
            isGrappling = true;
            verticalVelocity = 0;
        }
    }
    
    private void updateGrapple() {
        if (!isGrappling) return;
        
        double dx = grappleTargetX - (x + width/2);
        double dy = grappleTargetY - (y + height/2);
        double distance = Math.sqrt(dx*dx + dy*dy);
        
        if (distance < 10) { // Reached target
            isGrappling = false;
            isJumping = true;
            verticalVelocity = 0;
            return;
        }
        
        // Move toward grapple point
        double speedX = (dx / distance) * GRAPPLE_SPEED;
        double speedY = (dy / distance) * GRAPPLE_SPEED;
        
        x += speedX;
        y += speedY;
    }
    
    
    private void applyGravity(LevelGenerator level) {
        if (isGrappling) return;
        
        verticalVelocity += GRAVITY;
        y += verticalVelocity;
        
        // Check if landed on platform
        if (verticalVelocity > 0) {
            int tileBelow1 = level.getTile((int)(x / LevelGenerator.TILE_SIZE), 
                              (int)((y + height) / LevelGenerator.TILE_SIZE));
            int tileBelow2 = level.getTile((int)((x + width - 1) / LevelGenerator.TILE_SIZE), 
                              (int)((y + height) / LevelGenerator.TILE_SIZE));
            
            // Check if either foot is on a walkable tile
            boolean onPlatform1 = level.isWalkable((int)(x / LevelGenerator.TILE_SIZE), 
                                (int)((y + height) / LevelGenerator.TILE_SIZE));
            boolean onPlatform2 = level.isWalkable((int)((x + width - 1) / LevelGenerator.TILE_SIZE), 
                                (int)((y + height) / LevelGenerator.TILE_SIZE));
            
            if ((onPlatform1 || onPlatform2) && 
                y + height >= ((int)((y + height) / LevelGenerator.TILE_SIZE)) * LevelGenerator.TILE_SIZE) {
                y = ((int)((y + height) / LevelGenerator.TILE_SIZE)) * LevelGenerator.TILE_SIZE - height;
                isJumping = false;
                verticalVelocity = 0;
            }
        }
    }
    
    
    //holdable hiding keys
    private boolean hideKeyWasPressed = false;
    private boolean hideKeyJustPressed = false;
    
    // Visual properties
    private final Image idleImage;
    private final Image runImage;
    private Image currentImage;
    private boolean isMoving = false;
    
    // Hiding mechanics
    private boolean isHidden = false;
    private boolean nearHideable = false;
    private long lastHideTime = 0;
    private static final long HIDE_COOLDOWN_MS = 1000; // 1 second cooldown
    private boolean inCooldown = false;
    
    // Input handling
    private final Map<KeyCode, Boolean> keys = new HashMap<>();
    
    // UI elements
    private final Font statusFont = new Font("Arial Bold", 28);
    private final Font cooldownFont = new Font("Arial", 16);

    public Player(LevelGenerator level) {
        int[] pos = level.getRandomFloorPosition();
        this.x = pos[0] * LevelGenerator.TILE_SIZE;
        this.y = pos[1] * LevelGenerator.TILE_SIZE;
        
        this.idleImage = loadImage("/sprites/idle.gif");
        this.runImage = loadImage("/sprites/run.gif");
        this.currentImage = idleImage;
    }

    public void handleInput(KeyCode code, boolean pressed) {
        keys.put(code, pressed);
        
        // Track if hide key was just pressed (not held)
        if (code == KeyCode.H) {
            hideKeyJustPressed = pressed && !hideKeyWasPressed;
            hideKeyWasPressed = pressed;
        }
    }
    
    private boolean isOnPlatform(double x, double y, LevelGenerator level) {
        int tileX = (int)(x / LevelGenerator.TILE_SIZE);
        int tileY = (int)(y / LevelGenerator.TILE_SIZE);
        
        // Only solid tiles count as platforms (not grapple points)
        return level.isWalkable(tileX, tileY) && 
               level.getTile(tileX, tileY) != LevelGenerator.GRAPPLE_POINT;
    }

    public void update(LevelGenerator level) {
    	
    	
        if (isGrappling) {
            updateGrapple();
        } else {
            applyGravity(level);
        }
        
        checkHideableProximity(level);
        handleHiding();
        
        if (!isHidden) {
            boolean up = keys.getOrDefault(KeyCode.W, false);
            boolean down = keys.getOrDefault(KeyCode.S, false);
            boolean left = keys.getOrDefault(KeyCode.A, false);
            boolean right = keys.getOrDefault(KeyCode.D, false);
            move(up, down, left, right, level);
        }
        
        // Reset the just pressed flag
        hideKeyJustPressed = false;
    }

    private void handleHiding() {
        if (holdToHideEnabled) {
            // Original hold-to-hide implementation
            boolean hideKeyPressed = keys.getOrDefault(KeyCode.H, false);
            
            if (hideKeyPressed) {
                if (!isHidden && nearHideable && !inCooldown) {
                    isHidden = true;
                    SoundManager.playHideSound();
                } else if (isHidden) {
                    isHidden = false;
                    lastHideTime = System.currentTimeMillis();
                    inCooldown = true;
                    SoundManager.playUnhideSound();
                }
            }
        } else {
            // New toggle implementation
            if (hideKeyJustPressed) {
                if (!isHidden && nearHideable && !inCooldown) {
                    hideToggleRequested = true;
                } else if (isHidden) {
                    hideToggleRequested = true;
                }
            }

            if (hideToggleRequested) {
                if (!isHidden && nearHideable && !inCooldown) {
                    isHidden = true;
                    SoundManager.playHideSound();
                } else if (isHidden) {
                    isHidden = false;
                    lastHideTime = System.currentTimeMillis();
                    inCooldown = true;
                    SoundManager.playUnhideSound();
                }
                hideToggleRequested = false;
            }
        }

        // Update cooldown state
        if (inCooldown && 
            System.currentTimeMillis() - lastHideTime > HIDE_COOLDOWN_MS) {
            inCooldown = false;
        }
    }
    public void move(boolean up, boolean down, boolean left, boolean right, LevelGenerator level) {
        double moveX = 0;
        double moveY = 0;
        isMoving = false;

        // Calculate movement direction
        if (up) { moveY -= 1; isMoving = true; }
        if (down) { moveY += 1; isMoving = true; }
        if (left) { moveX -= 1; isMoving = true; }
        if (right) { moveX += 1; isMoving = true; }

        // Normalize diagonal movement to maintain consistent speed
        if (moveX != 0 && moveY != 0) {
            double len = Math.sqrt(moveX * moveX + moveY * moveY);
            moveX = moveX / len;
            moveY = moveY / len;
        }

        // Apply speed
        moveX *= speed;
        moveY *= speed;

        currentImage = isMoving ? runImage : idleImage;

        // Check if we can move in the desired direction
        boolean canMoveX = moveX == 0 || !checkCollision(x + moveX, y, level);
        boolean canMoveY = moveY == 0 || !checkCollision(x, y + moveY, level);

        // If we can't move diagonally, try moving along one axis
        if (!canMoveX && !canMoveY) {
            return; // Can't move at all
        } else if (!canMoveX) {
            moveX = 0; // Only move vertically
        } else if (!canMoveY) {
            moveY = 0; // Only move horizontally
        }

        // Apply movement
        x += moveX;
        y += moveY;
    }

    public void render(GraphicsContext gc) {
        if (!isHidden) {
            gc.drawImage(currentImage, x, y, width, height);
        }
    }

    public void renderHUD(GraphicsContext gc, double canvasWidth) {
    	

        
        
        // Hide prompt
        if (nearHideable && !isHidden && !inCooldown) {
            renderCenteredText(gc, "PRESS [H] TO HIDE", Color.YELLOW, canvasWidth, 50);
        }
        
        // Hidden status
        if (isHidden) {
            renderCenteredText(gc, "HIDDEN (PRESS [H] TO UNHIDE)", Color.GREEN, canvasWidth, 50);
        }
        
        // Cooldown indicator
        if (inCooldown) {
            double cooldownProgress = 1 - Math.min(1.0, 
                (double)(System.currentTimeMillis() - lastHideTime) / HIDE_COOLDOWN_MS);
            
            // Cooldown bar background
            gc.setFill(Color.rgb(100, 100, 100, 0.7));
            gc.fillRoundRect((canvasWidth - 200)/2, 80, 200, 15, 10, 10);
            
            // Cooldown progress
            gc.setFill(Color.rgb(255, (int)(255 * (1 - cooldownProgress)), 0));
            gc.fillRoundRect((canvasWidth - 200)/2, 80, 200 * cooldownProgress, 15, 10, 10);
            
            // Cooldown text
            gc.setFont(cooldownFont);
            gc.setFill(Color.WHITE);
            gc.fillText("Hiding cooldown: " + (int)(cooldownProgress * 100) + "%", 
                       (canvasWidth - 150)/2, 75);
        }
    }

    private void renderCenteredText(GraphicsContext gc, String text, 
                                  Color color, double canvasWidth, double yPos) {
        gc.setFont(statusFont);
        gc.setFill(color);
        double textWidth = gc.getFont().getSize() * text.length() * 0.5;
        double xPos = (canvasWidth - textWidth)/2;
        
        // Text outline
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.strokeText(text, xPos, yPos);
        
        // Main text
        gc.setFill(color);
        gc.fillText(text, xPos, yPos);
    }

    private void checkHideableProximity(LevelGenerator level) {
        int playerTileX = (int)(x / LevelGenerator.TILE_SIZE);
        int playerTileY = (int)(y / LevelGenerator.TILE_SIZE);
        nearHideable = false;
        
        // Check 3x3 area around player
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (level.isHideable(playerTileX + dx, playerTileY + dy)) {
                    nearHideable = true;
                    return;
                }
            }
        }
    }

    private boolean checkCollision(double x, double y, LevelGenerator level) {
        // Check all four corners of the player
        int[] checkX = {
            (int)(x / LevelGenerator.TILE_SIZE),
            (int)((x + width - 1) / LevelGenerator.TILE_SIZE)
        };
        int[] checkY = {
            (int)(y / LevelGenerator.TILE_SIZE),
            (int)((y + height - 1) / LevelGenerator.TILE_SIZE)
        };

        // Check all points
        for (int cx : checkX) {
            for (int cy : checkY) {
                if (!level.isWalkable(cx, cy) && level.getTile(cx, cy) != LevelGenerator.EXIT) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private Image loadImage(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                return new Image(is);
            }
        } catch (Exception e) {
            System.err.println("Error loading player image: " + path);
            e.printStackTrace();
        }
        return createPlaceholderImage();
    }

    private Image createPlaceholderImage() {
        WritableImage img = new WritableImage((int)width, (int)height);
        PixelWriter pw = img.getPixelWriter();
        Color color = isHidden ? Color.BLUE : Color.RED;
        
        for (int px = 0; px < width; px++) {
            for (int py = 0; py < height; py++) {
                pw.setColor(px, py, color);
            }
        }
        return img;
    }

    // Getters
    public double getX() { return x + width/2; }
    public double getY() { return y + height/2; }
    public boolean isHidden() { return isHidden; }
    public boolean isInCooldown() { return inCooldown; }
    public void setPosition(double x, double y) { 
        this.x = x; 
        this.y = y; 
    }
    
    
}