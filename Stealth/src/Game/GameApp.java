package Game;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameApp extends Application {
    private Player player;
    private GuardVariant standingGuard;
    private GuardVariant movingGuard;
    private LevelGenerator level;
    
    private boolean levelComplete = false;
    private boolean spotted = false;
    private boolean paused = false;
    
    private final Map<KeyCode, Boolean> keys = new HashMap<>();
    
    private Canvas canvas;
    private GraphicsContext gc;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    
    private int[][] initialLevelState;
    private double initialPlayerX, initialPlayerY;
    private double initialStandingGuardX, initialStandingGuardY;
    private double initialMovingGuardX, initialMovingGuardY;
    
    private boolean isPlatformerLevel;
    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 600;
    private static final Font MESSAGE_FONT = new Font("Arial", 24);

    public GameApp(boolean isPlatformerLevel) {
        this.isPlatformerLevel = isPlatformerLevel;
    }

    @Override
    public void start(Stage stage) {
        initializeGame();
        
        canvas = new Canvas(BASE_WIDTH, BASE_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        
        Scene scene = new Scene(root);
        setupEventHandlers(scene);
        
        configureStage(stage, scene);
        startGameLoop();
    }

    private void initializeGame() {
        level = isPlatformerLevel ? new PlatformLevelGenerator() : new LevelGenerator();
        
        initialLevelState = new int[LevelGenerator.WIDTH][LevelGenerator.HEIGHT];
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                initialLevelState[x][y] = level.getTile(x, y);
            }
        }
        
        if (isPlatformerLevel) {
            // For platformer mode - place one moving guard on a platform
            int[] movingGuardPos = findPlatformGuardPosition(level);
            movingGuard = new GuardVariant(level, GuardVariant.GuardType.MOVING);
            movingGuard.setPosition(movingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                  movingGuardPos[1] * LevelGenerator.TILE_SIZE);
            
            // No standing guard in platformer mode
            standingGuard = null;
            
            // Player spawn
            int[] playerPos = level.getValidPlayerSpawn(Arrays.asList(movingGuardPos));
            player = new Player(level, true);
            player.setPosition(playerPos[0] * LevelGenerator.TILE_SIZE, 
                             playerPos[1] * LevelGenerator.TILE_SIZE);
        } else {
            // Original stealth mode initialization
            int[] standingGuardPos = level.getRandomFloorPosition();
            int[] movingGuardPos = level.getRandomFloorPosition();
            List<int[]> guardPositions = Arrays.asList(standingGuardPos, movingGuardPos);
            
            int[] playerPos = level.getValidPlayerSpawn(guardPositions);
            
            player = new Player(level, false);
            player.setPosition(playerPos[0] * LevelGenerator.TILE_SIZE, 
                             playerPos[1] * LevelGenerator.TILE_SIZE);
            
            standingGuard = new GuardVariant(level, GuardVariant.GuardType.STANDING);
            standingGuard.setPosition(standingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                    standingGuardPos[1] * LevelGenerator.TILE_SIZE);
            
            movingGuard = new GuardVariant(level, GuardVariant.GuardType.MOVING);
            movingGuard.setPosition(movingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                  movingGuardPos[1] * LevelGenerator.TILE_SIZE);
        }
        
        SoundManager.initialize();
        SoundManager.playBGM();
    }

    private int[] findPlatformGuardPosition(LevelGenerator level) {
        // Find a platform position on the right side of the level
        for (int x = LevelGenerator.WIDTH * 3/4; x < LevelGenerator.WIDTH - 1; x++) {
            for (int y = 1; y < LevelGenerator.HEIGHT - 1; y++) {
                if (level.getTile(x, y) == LevelGenerator.PLATFORM && 
                    level.getTile(x, y-1) == LevelGenerator.GAP) {
                    return new int[]{x, y-1};
                }
            }
        }
        return level.getRandomFloorPosition(); // Fallback
    }

	private void setupEventHandlers(Scene scene) {
        scene.setOnKeyPressed(e -> {
            keys.put(e.getCode(), true);
            handleSpecialKeys(e);
        });
        
        scene.setOnKeyReleased(e -> {
            keys.put(e.getCode(), false);
            if (e.getCode() == KeyCode.P) paused = !paused;
        });
        
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());
    }

    private void handleSpecialKeys(KeyEvent e) {
        switch (e.getCode()) {
            case F11:
                toggleFullscreen();
                break;
            case R:
                if (spotted) resetLevel();
                break;
            case ENTER:
                if (levelComplete) {
                    generateNewLevel();
                    levelComplete = false;
                    spotted = false;
                    SoundManager.playBGM();
                }
                break;
            case ESCAPE:
                if (!spotted && !levelComplete) pauseGame();
                break;
        }
    }

    private void configureStage(Stage stage, Scene scene) {
        scene.setFill(Color.DARKSLATEGRAY);
        stage.setScene(scene);
        stage.setTitle("Stealth");
        stage.setMinWidth(600);
        stage.setMinHeight(450);
        stage.show();
    }

    private void startGameLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (paused) return;
                
                gc.setFill(Color.DARKSLATEGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                
                scaleX = canvas.getWidth() / BASE_WIDTH;
                scaleY = canvas.getHeight() / BASE_HEIGHT;
                
                gc.save();
                gc.scale(scaleX, scaleY);
                
                if (!spotted) {
                    if (isPlatformerLevel) {
                        handlePlatformerControls();
                        player.update(level);
                    } else {
                        handleStealthControls();
                        player.update(level);
                    }
                    
                    player.update(level);
                    if (movingGuard != null) {
                        movingGuard.update(level);
                    }
                    
                    if (!isPlatformerLevel && !player.isHidden()) {
                        boolean seenByStanding = standingGuard != null && standingGuard.canSee(player);
                        boolean seenByMoving = movingGuard != null && movingGuard.canSee(player);
                        if (seenByStanding || seenByMoving) {
                            spotted = true;
                            SoundManager.playAlert();
                        }
                    }
                }
                
                if (level.isAtExit((int)(player.getX() / LevelGenerator.TILE_SIZE), 
                        (int)(player.getY() / LevelGenerator.TILE_SIZE))) {
                    if (!levelComplete && keys.getOrDefault(KeyCode.E, false)) {
                        levelComplete();
                    }
                }
                
                renderMap();
                player.render(gc);
                if (standingGuard != null) {
                    standingGuard.render(gc);
                }
                if (movingGuard != null) {
                    movingGuard.render(gc);
                }
                
                renderUI();
                gc.restore();
            }
        }.start();
    }


    private void handlePlatformerControls() {
        player.handleInput(KeyCode.A, keys.getOrDefault(KeyCode.A, false));
        player.handleInput(KeyCode.D, keys.getOrDefault(KeyCode.D, false));
        
        if (keys.getOrDefault(KeyCode.SPACE, false)) {
            player.jump();
        }
        
        // Grapple hook logic remains only for platformer mode
        if (keys.getOrDefault(KeyCode.G, false)) {
            int playerTileX = (int)(player.getX() / LevelGenerator.TILE_SIZE);
            int playerTileY = (int)(player.getY() / LevelGenerator.TILE_SIZE);
            
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    if (level.getTile(playerTileX + dx, playerTileY + dy) == LevelGenerator.GRAPPLE_POINT) {
                        player.startGrapple(
                            (playerTileX + dx) * LevelGenerator.TILE_SIZE + LevelGenerator.TILE_SIZE/2,
                            (playerTileY + dy) * LevelGenerator.TILE_SIZE + LevelGenerator.TILE_SIZE/2
                        );
                        break;
                    }
                }
            }
        }
    }

    private void handleStealthControls() {
        player.handleInput(KeyCode.W, keys.getOrDefault(KeyCode.W, false));
        player.handleInput(KeyCode.S, keys.getOrDefault(KeyCode.S, false));
        player.handleInput(KeyCode.A, keys.getOrDefault(KeyCode.A, false));
        player.handleInput(KeyCode.D, keys.getOrDefault(KeyCode.D, false));
        player.handleInput(KeyCode.H, keys.getOrDefault(KeyCode.H, false));
        
        // Ensure no jumping in stealth mode
        player.handleInput(KeyCode.SPACE, false);
    }

    private void renderMap() {
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                int tile = level.getTile(x, y);
                double px = x * LevelGenerator.TILE_SIZE;
                double py = y * LevelGenerator.TILE_SIZE;
                
                switch (tile) {
                    case LevelGenerator.WALL:
                        gc.setFill(Color.DARKGRAY);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        gc.setStroke(Color.BLACK);
                        gc.strokeRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        break;
                    case LevelGenerator.EXIT:
                        gc.setFill(Color.GREEN);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        break;
                    case LevelGenerator.HIDEOBJ:
                        gc.setFill(Color.SIENNA);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        gc.setStroke(Color.BROWN);
                        gc.strokeRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        break;
                    case LevelGenerator.PLATFORM:
                        gc.setFill(Color.SANDYBROWN);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        gc.setStroke(Color.BROWN);
                        gc.strokeRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        break;
                    case LevelGenerator.GRAPPLE_POINT:
                        gc.setFill(Color.GOLD);
                        gc.fillOval(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        gc.setStroke(Color.DARKGOLDENROD);
                        gc.strokeOval(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        break;
                    case LevelGenerator.GAP:
                        gc.setFill(Color.SKYBLUE);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                        break;
                    default:
                        gc.setFill(Color.LIGHTGRAY);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                }
            }
        }
    }

    private void renderUI() {
        player.renderHUD(gc, BASE_WIDTH);
        
        if (isPlatformerLevel) {
            renderCenteredText(gc, "[A/D] MOVE  [SPACE] JUMP  [G] GRAPPLE", 
                              Color.WHITE, BASE_WIDTH, 30);
        }
        
        if (level.isAtExit((int)(player.getX() / LevelGenerator.TILE_SIZE), 
                (int)(player.getY() / LevelGenerator.TILE_SIZE)) && !levelComplete) {
            renderCenteredText(gc, "PRESS [E] TO ESCAPE", Color.GREEN, BASE_WIDTH, 50);
        }
        
        if (levelComplete) {
            gc.setFont(MESSAGE_FONT);
            gc.setFill(Color.GREEN);
            String message = "ESCAPED! PRESS [ENTER] FOR NEXT LEVEL";
            double textWidth = gc.getFont().getSize() * message.length() * 0.6;
            gc.fillText(message, (BASE_WIDTH - textWidth) / 2, BASE_HEIGHT / 2);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeText(message, (BASE_WIDTH - textWidth) / 2, BASE_HEIGHT / 2);
        }
        
        if (spotted) {
            gc.setFont(MESSAGE_FONT);
            gc.setFill(Color.RED);
            String message = "CAUGHT! PRESS R TO RESTART";
            double textWidth = gc.getFont().getSize() * message.length() * 0.6;
            gc.fillText(message, (BASE_WIDTH - textWidth) / 2, 50);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeText(message, (BASE_WIDTH - textWidth) / 2, 50);
        }
        
        if (paused) {
            gc.setFont(MESSAGE_FONT);
            gc.setFill(Color.WHITE);
            String message = "PAUSED";
            double textWidth = gc.getFont().getSize() * message.length() * 0.6;
            gc.fillText(message, (BASE_WIDTH - textWidth) / 2, BASE_HEIGHT / 2);
        }
    }

    private void toggleFullscreen() {
        Stage stage = (Stage) canvas.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
        if (!stage.isFullScreen()) {
            canvas.setWidth(BASE_WIDTH);
            canvas.setHeight(BASE_HEIGHT);
        }
        resizeCanvas();
    }

    private void resizeCanvas() {
        Stage stage = (Stage) canvas.getScene().getWindow();
        if (stage.isFullScreen()) {
            canvas.setWidth(stage.getWidth());
            canvas.setHeight(stage.getHeight());
        } else {
            double aspectRatio = (double)BASE_WIDTH / BASE_HEIGHT;
            double newWidth = canvas.getHeight() * aspectRatio;
            double newHeight = canvas.getWidth() / aspectRatio;
            
            if (newWidth > canvas.getWidth()) {
                canvas.setHeight(newHeight);
            } else {
                canvas.setWidth(newWidth);
            }
        }
    }

    private void resetLevel() {
        spotted = false;
        
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                level.setTile(x, y, initialLevelState[x][y]);
            }
        }
        
        player.setPosition(initialPlayerX, initialPlayerY);
        if (standingGuard != null) {
            standingGuard.setPosition(initialStandingGuardX, initialStandingGuardY);
        }
        if (movingGuard != null) {
            movingGuard.setPosition(initialMovingGuardX, initialMovingGuardY);
        }
    }
    
    private void renderCenteredText(GraphicsContext gc, String text, Color color, 
            double canvasWidth, double yPos) {
        gc.setFont(MESSAGE_FONT);
        gc.setFill(color);
        double textWidth = gc.getFont().getSize() * text.length() * 0.5;
        double xPos = (canvasWidth - textWidth)/2;
        
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeText(text, xPos, yPos);
        
        gc.setFill(color);
        gc.fillText(text, xPos, yPos);
    }
    
    private void levelComplete() {
        levelComplete = true;
        SoundManager.stopBGM();
        SoundManager.playAlert();
    }
    
    private void generateNewLevel() {
        level = isPlatformerLevel ? new PlatformLevelGenerator() : new LevelGenerator();
        
        initialLevelState = new int[LevelGenerator.WIDTH][LevelGenerator.HEIGHT];
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                initialLevelState[x][y] = level.getTile(x, y);
            }
        }
        
        if (isPlatformerLevel) {
            // Platformer mode - only moving guard
            int[] movingGuardPos = findPlatformGuardPosition(level);
            movingGuard = new GuardVariant(level, GuardVariant.GuardType.MOVING);
            movingGuard.setPosition(movingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                  movingGuardPos[1] * LevelGenerator.TILE_SIZE);
            
            standingGuard = null;
            
            int[] playerPos = level.getValidPlayerSpawn(Arrays.asList(movingGuardPos));
            player.setPosition(playerPos[0] * LevelGenerator.TILE_SIZE, 
                             playerPos[1] * LevelGenerator.TILE_SIZE);
            
            initialStandingGuardX = 0;
            initialStandingGuardY = 0;
            initialMovingGuardX = movingGuardPos[0] * LevelGenerator.TILE_SIZE;
            initialMovingGuardY = movingGuardPos[1] * LevelGenerator.TILE_SIZE;
        } else {
            // Stealth mode - both guards
            int[] standingGuardPos = level.getRandomFloorPosition();
            int[] movingGuardPos = level.getRandomFloorPosition();
            List<int[]> guardPositions = Arrays.asList(standingGuardPos, movingGuardPos);
            
            int[] playerPos = level.getValidPlayerSpawn(guardPositions);
            player.setPosition(playerPos[0] * LevelGenerator.TILE_SIZE, 
                             playerPos[1] * LevelGenerator.TILE_SIZE);
            
            standingGuard = new GuardVariant(level, GuardVariant.GuardType.STANDING);
            standingGuard.setPosition(standingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                    standingGuardPos[1] * LevelGenerator.TILE_SIZE);
            
            movingGuard = new GuardVariant(level, GuardVariant.GuardType.MOVING);
            movingGuard.setPosition(movingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                  movingGuardPos[1] * LevelGenerator.TILE_SIZE);
            
            initialStandingGuardX = standingGuardPos[0] * LevelGenerator.TILE_SIZE;
            initialStandingGuardY = standingGuardPos[1] * LevelGenerator.TILE_SIZE;
            initialMovingGuardX = movingGuardPos[0] * LevelGenerator.TILE_SIZE;
            initialMovingGuardY = movingGuardPos[1] * LevelGenerator.TILE_SIZE;
        }
        
        initialPlayerX = player.getX();
        initialPlayerY = player.getY();
    }
    
    public void pauseGame() {
        paused = true;
        SoundManager.stopBGM();
        new PauseMenu((Stage)canvas.getScene().getWindow(), this).show();
    }
    
    public void resumeGame() {
        paused = false;
        SoundManager.playBGM();
    }
    
    public void exitToMainMenu() {
        SoundManager.stopBGM();
        new MainMenu((Stage)canvas.getScene().getWindow());
    }
}