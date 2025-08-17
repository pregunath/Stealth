package Game;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.InputStream;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class Player {
    private double x, y;
    private final double baseSpeed = 3.0;
    private double currentSpeed = baseSpeed;
    private final double width = 23;
    private final double height = 23;
    
    private boolean hideToggleRequested = false;
    private boolean holdToHideEnabled = false;
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
    
    private List<Gear> equippedGear = new ArrayList<>();
    private boolean isSneakyClass = false;
    private boolean isAgileClass = false;
    
    // Cherry Bomb fields
    private int cherryBombs = 2;
    private final int MAX_CHERRY_BOMBS = 2;
    private long lastBombTime = 0;
    private static final long BOMB_COOLDOWN = 1000;

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
        
        if (code == KeyCode.H) {
            hideKeyJustPressed = pressed && !hideKeyWasPressed;
            hideKeyWasPressed = pressed;
        }
    }

    public void update(LevelGenerator level) {
        updateMovementSpeed();
        checkHideableProximity(level);
        handleHiding();
        checkBarrelReplenish(level);
        
        if (!isHidden) {
            move(level);
        }
        
        hideKeyJustPressed = false;
    }

    private void updateMovementSpeed() {
        currentSpeed = baseSpeed;
        
        if (isAgileClass) {
            currentSpeed *= 1.25;
        }
        
        for (Gear gear : equippedGear) {
            if (gear instanceof AgileGear && gear.getName().equals("Swift Boots")) {
                currentSpeed *= 1.25;
                break;
            }
        }
    }

    public boolean useCherryBomb(List<CherryBombEffect> activeEffects) {
        long currentTime = System.currentTimeMillis();
        
        if (cherryBombs > 0 && currentTime - lastBombTime > BOMB_COOLDOWN) {
            cherryBombs--;
            lastBombTime = currentTime;
            //SoundManager.playSound("cherry_bomb_activate");
            activeEffects.add(new CherryBombEffect(x + width/2, y + height/2));
            return true;
        }
        return false;
    }

    public void checkBarrelReplenish(LevelGenerator level) {
        int playerTileX = (int)(x / LevelGenerator.TILE_SIZE);
        int playerTileY = (int)(y / LevelGenerator.TILE_SIZE);
        
        if (level.isHideable(playerTileX, playerTileY)) {
            if (cherryBombs < MAX_CHERRY_BOMBS) {
                cherryBombs = MAX_CHERRY_BOMBS;
                //SoundManager.playSound("cherry_bomb_replenish");
            }
        }
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

    private void move(LevelGenerator level) {
        boolean up = keys.getOrDefault(KeyCode.W, false);
        boolean down = keys.getOrDefault(KeyCode.S, false);
        boolean left = keys.getOrDefault(KeyCode.A, false);
        boolean right = keys.getOrDefault(KeyCode.D, false);
        
        double moveX = (right ? 1 : 0) - (left ? 1 : 0);
        double moveY = (down ? 1 : 0) - (up ? 1 : 0);
        
        isMoving = (moveX != 0 || moveY != 0);

        if (moveX != 0 && moveY != 0) {
            double len = Math.sqrt(moveX * moveX + moveY * moveY);
            moveX = moveX / len * currentSpeed;
            moveY = moveY / len * currentSpeed;
        } else {
            moveX *= currentSpeed;
            moveY *= currentSpeed;
        }

        double newX = x + moveX;
        if (!checkCollision(newX, y, level)) {
            x = newX;
        }

        double newY = y + moveY;
        if (!checkCollision(x, newY, level)) {
            y = newY;
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
        
        gc.setFont(cooldownFont);
        gc.setFill(Color.PINK);
        gc.fillText("Cherry Bombs: " + cherryBombs + "/" + MAX_CHERRY_BOMBS, 20, 30);
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
    
    public List<Gear> getEquippedGear() {
        return equippedGear;
    }
    
    public void setClassType(String classType) {
        if (classType.equals("Sneaky")) {
            isSneakyClass = true;
            equipGear(new SneakyGear("Cloak"));
            equipGear(new SneakyGear("Boots"));
        } else if (classType.equals("Agile")) {
            isAgileClass = true;
            equipGear(new AgileGear("Boots"));
            equipGear(new AgileGear("Belt"));
        }
    }
    
    public boolean isSneakyClass() {
        return isSneakyClass;
    }

    public boolean isAgileClass() {
        return isAgileClass;
    }
    
    public void equipGear(Gear gear) {
        equippedGear.add(gear);
        gear.applyEffect(this);
    }
}