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
    private double x, y;
    private final double speed = 3;
    private final double width = 23;
    private final double height = 23;
    
    private boolean isJumping = false;
    private double verticalVelocity = 0;
    private static final double GRAVITY = 0.5;
    private static final double JUMP_FORCE = -10;
    private static final double GRAPPLE_SPEED = 8;
    private boolean isGrappling = false;
    private double grappleTargetX, grappleTargetY;
    
    private boolean hideToggleRequested = false;
    private boolean holdToHideEnabled = false;
    
    private boolean isPlatformerLevel;
    private boolean hideKeyWasPressed = false;
    private boolean hideKeyJustPressed = false;
    
    private final Image idleImage;
    private final Image runImage;
    private Image currentImage;
    private boolean isMoving = false;
    
    private boolean isHidden = false;
    private boolean nearHideable = false;
    private long lastHideTime = 0;
    private static final long HIDE_COOLDOWN_MS = 1000;
    private boolean inCooldown = false;
    
    private final Map<KeyCode, Boolean> keys = new HashMap<>();
    private final Font statusFont = new Font("Arial Bold", 28);
    private final Font cooldownFont = new Font("Arial", 16);

    public Player(LevelGenerator level, boolean isPlatformerLevel) {
        this.isPlatformerLevel = isPlatformerLevel;
        int[] pos = level.getRandomFloorPosition();
        this.x = pos[0] * LevelGenerator.TILE_SIZE;
        this.y = pos[1] * LevelGenerator.TILE_SIZE;
        
        this.idleImage = loadImage("/sprites/idle.gif");
        this.runImage = loadImage("/sprites/run.gif");
        this.currentImage = idleImage;
    }

    public void handleInput(KeyCode code, boolean pressed) {
        keys.put(code, pressed);
        
        if (!isPlatformerLevel && code == KeyCode.H) {
            hideKeyJustPressed = pressed && !hideKeyWasPressed;
            hideKeyWasPressed = pressed;
        }
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
        
        if (distance < 10) {
            isGrappling = false;
            isJumping = true;
            verticalVelocity = 0;
            return;
        }
        
        double speedX = (dx / distance) * GRAPPLE_SPEED;
        double speedY = (dy / distance) * GRAPPLE_SPEED;
        
        x += speedX;
        y += speedY;
    }
    
    private void applyGravity(LevelGenerator level) {
        if (isGrappling) return;
        
        verticalVelocity += GRAVITY;
        y += verticalVelocity;
        
        // Check collision with platforms below
        if (verticalVelocity > 0) {
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
        
        // Check collision with platforms above (when jumping up)
        if (verticalVelocity < 0) {
            boolean hitCeiling1 = !level.isWalkable((int)(x / LevelGenerator.TILE_SIZE), 
                                 (int)(y / LevelGenerator.TILE_SIZE));
            boolean hitCeiling2 = !level.isWalkable((int)((x + width - 1) / LevelGenerator.TILE_SIZE), 
                                 (int)(y / LevelGenerator.TILE_SIZE));
            
            if ((hitCeiling1 || hitCeiling2) && 
                y <= ((int)(y / LevelGenerator.TILE_SIZE) + 1) * LevelGenerator.TILE_SIZE) {
                verticalVelocity = 0;
                y = ((int)(y / LevelGenerator.TILE_SIZE) + 1) * LevelGenerator.TILE_SIZE;
            }
        }
    }

    public void update(LevelGenerator level) {
        if (!isPlatformerLevel) {
            checkHideableProximity(level);
            handleHiding();
        }
        
        if (!isHidden) {
            move(level);
        }
        
        hideKeyJustPressed = false;
    }

    private void handleHiding() {
        if (holdToHideEnabled) {
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

        if (inCooldown && System.currentTimeMillis() - lastHideTime > HIDE_COOLDOWN_MS) {
            inCooldown = false;
        }
    }

    public void move(LevelGenerator level) {
        if (isPlatformerLevel) {
            movePlatformer(level);
        } else {
            moveStealth(level);
        }
    }

    private void movePlatformer(LevelGenerator level) {
        boolean left = keys.getOrDefault(KeyCode.A, false);
        boolean right = keys.getOrDefault(KeyCode.D, false);
        
        double moveX = 0;
        isMoving = false;

        if (left) { moveX -= 1; isMoving = true; }
        if (right) { moveX += 1; isMoving = true; }

        moveX *= speed;
        
        // Check if we can move horizontally
        boolean canMoveLeft = !left || !checkCollision(x + moveX, y, level);
        boolean canMoveRight = !right || !checkCollision(x + moveX, y, level);
        
        if ((moveX < 0 && canMoveLeft) || (moveX > 0 && canMoveRight)) {
            x += moveX;
        }

        currentImage = isMoving ? runImage : idleImage;
    }

    private void moveStealth(LevelGenerator level) {
        boolean up = keys.getOrDefault(KeyCode.W, false);
        boolean down = keys.getOrDefault(KeyCode.S, false);
        boolean left = keys.getOrDefault(KeyCode.A, false);
        boolean right = keys.getOrDefault(KeyCode.D, false);
        
        double moveX = 0;
        double moveY = 0;
        isMoving = false;

        if (up) { moveY -= 1; isMoving = true; }
        if (down) { moveY += 1; isMoving = true; }
        if (left) { moveX -= 1; isMoving = true; }
        if (right) { moveX += 1; isMoving = true; }

        // Normalize diagonal movement
        if (moveX != 0 && moveY != 0) {
            double len = Math.sqrt(moveX * moveX + moveY * moveY);
            moveX = moveX / len;
            moveY = moveY / len;
        }

        moveX *= speed;
        moveY *= speed;

        // Check collisions
        boolean canMoveX = moveX == 0 || !checkCollision(x + moveX, y, level);
        boolean canMoveY = moveY == 0 || !checkCollision(x, y + moveY, level);

        if (canMoveX) {
            x += moveX;
        }
        if (canMoveY) {
            y += moveY;
        }

        currentImage = isMoving ? runImage : idleImage;
    }
    
    public void render(GraphicsContext gc) {
        if (!isHidden) {
            gc.drawImage(currentImage, x, y, width, height);
        }
    }

    public void renderHUD(GraphicsContext gc, double canvasWidth) {
        if (nearHideable && !isHidden && !inCooldown) {
            renderCenteredText(gc, "PRESS [H] TO HIDE", Color.YELLOW, canvasWidth, 50);
        }
        
        if (isHidden) {
            renderCenteredText(gc, "HIDDEN (PRESS [H] TO UNHIDE)", Color.GREEN, canvasWidth, 50);
        }
        
        if (inCooldown) {
            double cooldownProgress = 1 - Math.min(1.0, 
                (double)(System.currentTimeMillis() - lastHideTime) / HIDE_COOLDOWN_MS);
            
            gc.setFill(Color.rgb(100, 100, 100, 0.7));
            gc.fillRoundRect((canvasWidth - 200)/2, 80, 200, 15, 10, 10);
            
            gc.setFill(Color.rgb(255, (int)(255 * (1 - cooldownProgress)), 0));
            gc.fillRoundRect((canvasWidth - 200)/2, 80, 200 * cooldownProgress, 15, 10, 10);
            
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
        
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.strokeText(text, xPos, yPos);
        
        gc.setFill(color);
        gc.fillText(text, xPos, yPos);
    }

    private void checkHideableProximity(LevelGenerator level) {
        int playerTileX = (int)(x / LevelGenerator.TILE_SIZE);
        int playerTileY = (int)(y / LevelGenerator.TILE_SIZE);
        nearHideable = false;
        
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
        int[] checkX = {
            (int)(x / LevelGenerator.TILE_SIZE),
            (int)((x + width - 1) / LevelGenerator.TILE_SIZE)
        };
        int[] checkY = {
            (int)(y / LevelGenerator.TILE_SIZE),
            (int)((y + height - 1) / LevelGenerator.TILE_SIZE)
        };

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

    public double getX() { return x + width/2; }
    public double getY() { return y + height/2; }
    public boolean isHidden() { return isHidden; }
    public boolean isInCooldown() { return inCooldown; }
    public void setPosition(double x, double y) { 
        this.x = x; 
        this.y = y; 
    }
    public void setHoldToHide(boolean enabled) {
        this.holdToHideEnabled = enabled;
    }
}