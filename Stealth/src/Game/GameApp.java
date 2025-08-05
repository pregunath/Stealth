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
import java.util.Random;

public class GameApp extends Application {
	
    // Game objects
    private Player player;
    private GuardVariant standingGuard;
    private GuardVariant movingGuard;
    private LevelGenerator level;
    
    
    
 // Add this new state variable at the top with other game states
    private boolean levelComplete = false;
    
    // Game state
    private boolean spotted = false;
    private boolean paused = false;
    
    // Input handling
    private final Map<KeyCode, Boolean> keys = new HashMap<>();
    
    // Display
    private Canvas canvas;
    private GraphicsContext gc;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    
    // Store initial state for reset
    private int[][] initialLevelState;
    private double initialPlayerX, initialPlayerY;
    private double initialStandingGuardX, initialStandingGuardY;
    private double initialMovingGuardX, initialMovingGuardY;
    
    	
    
    private double detectionLevel = 0;
	private boolean isPlatformerLevel;
    private static final double MAX_DETECTION = 100;
    private static final double DETECTION_RATE = 0.5; // per frame when spotted
    private static final double DETECTION_DECAY = 0.2; // per frame when not spotted
    
    
    // Constants
    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 600;
    private static final Font MESSAGE_FONT = new Font("Arial", 24);
    
    

    public GameApp(boolean isPlatformerLevel) {
        this.isPlatformerLevel = isPlatformerLevel;
    }

    @Override
    public void start(Stage stage) {
        // Initialize game objects
        initializeGame();
        
        // Setup canvas and root pane
        canvas = new Canvas(BASE_WIDTH, BASE_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        
        // Scene setup
        Scene scene = new Scene(root);
        setupEventHandlers(scene);
        
        // Stage configuration
        configureStage(stage, scene);
        
        // Start game loop
        startGameLoop();
    }

    private void initializeGame() {
        
        if (isPlatformerLevel) {
            level = new PlatformLevelGenerator();
        } else {
            level = new LevelGenerator();
        }
        
        // Store initial level state
        initialLevelState = new int[LevelGenerator.WIDTH][LevelGenerator.HEIGHT];
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                initialLevelState[x][y] = level.getTile(x, y);
            }
        }
        
        // Get guard positions first
        int[] standingGuardPos = level.getRandomFloorPosition();
        int[] movingGuardPos = level.getRandomFloorPosition();
        List<int[]> guardPositions = Arrays.asList(standingGuardPos, movingGuardPos);
        
        // Get valid player spawn
        int[] playerPos = level.getValidPlayerSpawn(guardPositions);
        
        player = new Player(level);
        player.setPosition(playerPos[0] * LevelGenerator.TILE_SIZE, 
                          playerPos[1] * LevelGenerator.TILE_SIZE);
        
        standingGuard = new GuardVariant(level, GuardVariant.GuardType.STANDING);
        standingGuard.setPosition(standingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                 standingGuardPos[1] * LevelGenerator.TILE_SIZE);
        
        movingGuard = new GuardVariant(level, GuardVariant.GuardType.MOVING);
        movingGuard.setPosition(movingGuardPos[0] * LevelGenerator.TILE_SIZE,
                               movingGuardPos[1] * LevelGenerator.TILE_SIZE);
        
        // Store initial positions
        initialPlayerX = player.getX();
        initialPlayerY = player.getY();
        initialStandingGuardX = standingGuardPos[0] * LevelGenerator.TILE_SIZE;
        initialStandingGuardY = standingGuardPos[1] * LevelGenerator.TILE_SIZE;
        initialMovingGuardX = movingGuardPos[0] * LevelGenerator.TILE_SIZE;
        initialMovingGuardY = movingGuardPos[1] * LevelGenerator.TILE_SIZE;
        
        SoundManager.initialize();
        SoundManager.playBGM();
    }

    private void setupEventHandlers(Scene scene) {
        // Keyboard input
        scene.setOnKeyPressed(e -> {
            keys.put(e.getCode(), true);
            handleSpecialKeys(e);
        });
        
        scene.setOnKeyReleased(e -> {
            keys.put(e.getCode(), false);
            if (e.getCode() == KeyCode.P) paused = !paused;
        });
        
        // Window resize handling
        canvas.widthProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> resizeCanvas());
    }

    private void handleSpecialKeys(KeyEvent e) {
        if (e.getCode() == KeyCode.F11) {
            toggleFullscreen();
        }
        if (e.getCode() == KeyCode.R && spotted) {
            resetLevel();
        }
        if (levelComplete && e.getCode() == KeyCode.ENTER) {
            generateNewLevel();
            levelComplete = false;
            spotted = false;
            SoundManager.playBGM();
        }
        if (e.getCode() == KeyCode.ESCAPE && !spotted && !levelComplete) {
            pauseGame();
        }
    }

    private void configureStage(Stage stage, Scene scene) {
        scene.setFill(Color.DARKSLATEGRAY); // This matches your game's background color
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
                
                // Clear screen
                gc.setFill(Color.DARKSLATEGRAY);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                
                // Calculate scaling
                scaleX = canvas.getWidth() / BASE_WIDTH;
                scaleY = canvas.getHeight() / BASE_HEIGHT;
                
                // Save graphics context
                gc.save();
                gc.scale(scaleX, scaleY);
                
             // Modify the game loop to handle platformer controls
                if (!spotted) {
                    // Platformer controls
                    if (isPlatformerLevel) {
                        player.handleInput(KeyCode.A, keys.getOrDefault(KeyCode.A, false));
                        player.handleInput(KeyCode.D, keys.getOrDefault(KeyCode.D, false));
                        
                        if (keys.getOrDefault(KeyCode.SPACE, false)) {
                            player.jump();
                        }
                        
                        // Handle grappling
                        if (keys.getOrDefault(KeyCode.G, false)) {
                            // Check for nearby grapple points
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
                    // Original stealth controls
                    else {
                        player.handleInput(KeyCode.W, keys.getOrDefault(KeyCode.W, false));
                        player.handleInput(KeyCode.S, keys.getOrDefault(KeyCode.S, false));
                        player.handleInput(KeyCode.A, keys.getOrDefault(KeyCode.A, false));
                        player.handleInput(KeyCode.D, keys.getOrDefault(KeyCode.D, false));
                        player.handleInput(KeyCode.H, keys.getOrDefault(KeyCode.H, false));
                    }
                    
                    player.update(level);
                    movingGuard.update(level);
                    
                    // Only check for spotting in stealth levels
                    if (!isPlatformerLevel && !player.isHidden() && 
                        (standingGuard.canSee(player) || movingGuard.canSee(player))) {
                        spotted = true;
                        SoundManager.playAlert();
                    }
                }
             // Modify the game loop section that checks for exit
             // Change this in the game loop (around line 160):
                if (level.isAtExit((int)(player.getX() / LevelGenerator.TILE_SIZE), 
                        (int)(player.getY() / LevelGenerator.TILE_SIZE))) {
          if (!levelComplete && keys.getOrDefault(KeyCode.E, false)) {
              levelComplete();
          }
      }
                
                // Render game
                renderMap();
                player.render(gc);
                standingGuard.render(gc);
                movingGuard.render(gc);
                
                // Render UI
                renderUI();
                
                // Restore graphics context
                gc.restore();
            }
        }.start();
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
                    default: // FLOOR
                        gc.setFill(Color.LIGHTGRAY);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                }
            }
        }
    }

    private void renderUI() {
        // Player status
        player.renderHUD(gc, BASE_WIDTH);
        
        
        if (isPlatformerLevel) {
            // Platformer controls hint
            renderCenteredText(gc, "[A/D] MOVE  [SPACE] JUMP  [G] GRAPPLE", 
                              Color.WHITE, BASE_WIDTH, 30);
        } else {
            // Original stealth UI
            player.renderHUD(gc, BASE_WIDTH);
        }
        
        if (level.isAtExit((int)(player.getX() / LevelGenerator.TILE_SIZE), 
                (int)(player.getY() / LevelGenerator.TILE_SIZE)) && !levelComplete) {
  renderCenteredText(gc, "PRESS [E] TO ESCAPE", Color.GREEN, BASE_WIDTH, 50);
}
        
        if (levelComplete) {
            gc.setFont(MESSAGE_FONT);
            gc.setFill(Color.GREEN);
            String message = "ESCAPED! PRESS [ENTER] FOR NEXT LEVEL";  // Changed message
            double textWidth = gc.getFont().getSize() * message.length() * 0.6;
            gc.fillText(message, (BASE_WIDTH - textWidth) / 2, BASE_HEIGHT / 2);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeText(message, (BASE_WIDTH - textWidth) / 2, BASE_HEIGHT / 2);
        }
        
        
        
        // Game over message
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
        
        // Pause message
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
            // When exiting fullscreen, reset to base dimensions
            canvas.setWidth(BASE_WIDTH);
            canvas.setHeight(BASE_HEIGHT);
        }
        resizeCanvas();
    }

    private void resizeCanvas() {
        Stage stage = (Stage) canvas.getScene().getWindow();
        if (stage.isFullScreen()) {
            // In fullscreen mode, use all available space
            canvas.setWidth(stage.getWidth());
            canvas.setHeight(stage.getHeight());
        } else {
            // In windowed mode, maintain aspect ratio
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
        
        // Restore level state
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                level.setTile(x, y, initialLevelState[x][y]);
            }
        }
        
        // Reset player and guards to their initial positions
        player.setPosition(initialPlayerX, initialPlayerY);
        standingGuard.setPosition(initialStandingGuardX, initialStandingGuardY);
        movingGuard.setPosition(initialMovingGuardX, initialMovingGuardY);
        
        // Reset guard paths
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
			
			// Text outline
			gc.setStroke(Color.BLACK);
			gc.setLineWidth(2);
			gc.strokeText(text, xPos, yPos);
			
			// Main text
			gc.setFill(color);
			gc.fillText(text, xPos, yPos);
}
    
    private void levelComplete() {
        levelComplete = true;
        SoundManager.stopBGM();
        SoundManager.playAlert();
    }
    
    private void generateNewLevel() {
        level = new LevelGenerator(); // This creates a new random level
        
        // Store initial level state
        initialLevelState = new int[LevelGenerator.WIDTH][LevelGenerator.HEIGHT];
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                initialLevelState[x][y] = level.getTile(x, y);
            }
        }
        
        // Get guard positions first
        int[] standingGuardPos = level.getRandomFloorPosition();
        int[] movingGuardPos = level.getRandomFloorPosition();
        List<int[]> guardPositions = Arrays.asList(standingGuardPos, movingGuardPos);
        
        // Get valid player spawn
        int[] playerPos = level.getValidPlayerSpawn(guardPositions);
        
        player.setPosition(playerPos[0] * LevelGenerator.TILE_SIZE, 
                          playerPos[1] * LevelGenerator.TILE_SIZE);
        
        standingGuard = new GuardVariant(level, GuardVariant.GuardType.STANDING);
        standingGuard.setPosition(standingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                 standingGuardPos[1] * LevelGenerator.TILE_SIZE);
        
        movingGuard = new GuardVariant(level, GuardVariant.GuardType.MOVING);
        movingGuard.setPosition(movingGuardPos[0] * LevelGenerator.TILE_SIZE,
                               movingGuardPos[1] * LevelGenerator.TILE_SIZE);
        
        // Store initial positions
        initialPlayerX = player.getX();
        initialPlayerY = player.getY();
        initialStandingGuardX = standingGuardPos[0] * LevelGenerator.TILE_SIZE;
        initialStandingGuardY = standingGuardPos[1] * LevelGenerator.TILE_SIZE;
        initialMovingGuardX = movingGuardPos[0] * LevelGenerator.TILE_SIZE;
        initialMovingGuardY = movingGuardPos[1] * LevelGenerator.TILE_SIZE;
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