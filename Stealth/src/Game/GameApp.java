package Game;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
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
    private boolean initialized = false;
    
    private final Map<KeyCode, Boolean> keys = new HashMap<>();
    
    private Canvas canvas;
    private GraphicsContext gc;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    
    private int[][] initialLevelState;
    private double initialPlayerX, initialPlayerY;
    private double initialStandingGuardX, initialStandingGuardY;
    private double initialMovingGuardX, initialMovingGuardY;
    
    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 600;
    private static final Font MESSAGE_FONT = new Font("Arial", 24);
    
    private String selectedClass;

    private List<CherryBombEffect> activeCherryBombs= new ArrayList<>();
    
    private Stage primaryStage;
    
    
    public GameApp(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Stealth Roguelike");
        showMainMenu();
    }
    
    public void showMainMenu() {
        new MainMenu(primaryStage, this);
        primaryStage.show();
    }

    private void initializeGame() {
        level = new LevelGenerator();
        
        initialLevelState = new int[LevelGenerator.WIDTH][LevelGenerator.HEIGHT];
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                initialLevelState[x][y] = level.getTile(x, y);
            }
        }
        
        int[] standingGuardPos = level.getRandomFloorPosition();
        int[] movingGuardPos = level.getRandomFloorPosition();
        List<int[]> guardPositions = Arrays.asList(standingGuardPos, movingGuardPos);
        
        int[] playerPos = level.getValidPlayerSpawn(guardPositions);
        
        player = new Player(level);
        player.setPosition(playerPos[0] * LevelGenerator.TILE_SIZE, 
                         playerPos[1] * LevelGenerator.TILE_SIZE);
        player.setClassType(selectedClass);
        
        standingGuard = new GuardVariant(level, GuardVariant.GuardType.STANDING);
        standingGuard.setPosition(standingGuardPos[0] * LevelGenerator.TILE_SIZE,
                                standingGuardPos[1] * LevelGenerator.TILE_SIZE);
        
        movingGuard = new GuardVariant(level, GuardVariant.GuardType.MOVING);
        movingGuard.setPosition(movingGuardPos[0] * LevelGenerator.TILE_SIZE,
                              movingGuardPos[1] * LevelGenerator.TILE_SIZE);
        
        initialPlayerX = player.getX();
        initialPlayerY = player.getY();
        initialStandingGuardX = standingGuardPos[0] * LevelGenerator.TILE_SIZE;
        initialStandingGuardY = standingGuardPos[1] * LevelGenerator.TILE_SIZE;
        initialMovingGuardX = movingGuardPos[0] * LevelGenerator.TILE_SIZE;
        initialMovingGuardY = movingGuardPos[1] * LevelGenerator.TILE_SIZE;
        
        SoundManager.initialize();
        SoundManager.playBGM();
        
        initialized = true;
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

    private void configureStage(Scene scene) {
        scene.setFill(Color.DARKSLATEGRAY);
        primaryStage.setScene(scene); // Use primaryStage
        primaryStage.setTitle("Stealth Roguelike - " + selectedClass + " Class");
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(450);
    }
    
    private void startGameLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!initialized || paused || gc == null) return;
                
                try {
                    gc.setFill(Color.DARKSLATEGRAY);
                    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    
                    scaleX = canvas.getWidth() / BASE_WIDTH;
                    scaleY = canvas.getHeight() / BASE_HEIGHT;
                    
                    gc.save();
                    gc.scale(scaleX, scaleY);
                    
                    if (!spotted) {
                        handleStealthControls();
                        player.update(level);
                        movingGuard.update(level);
                        
                        if (!player.isHidden()) {
                            boolean seenByStanding = standingGuard != null && 
                                                   standingGuard.canSee(player, activeCherryBombs);
                            boolean seenByMoving = movingGuard != null && 
                                                 movingGuard.canSee(player, activeCherryBombs);
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
                    if (standingGuard != null) standingGuard.render(gc);
                    if (movingGuard != null) movingGuard.render(gc);
                    
                    // Update and render cherry bombs
                    activeCherryBombs.removeIf(effect -> {
                        effect.update();
                        return !effect.isActive();
                    });
                    
                    for (CherryBombEffect effect : activeCherryBombs) {
                        effect.render(gc);
                    }
                    
                    renderUI();
                    gc.restore();
                } catch (Exception e) {
                    System.err.println("Rendering error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void handleStealthControls() {
        player.handleInput(KeyCode.W, keys.getOrDefault(KeyCode.W, false));
        player.handleInput(KeyCode.S, keys.getOrDefault(KeyCode.S, false));
        player.handleInput(KeyCode.A, keys.getOrDefault(KeyCode.A, false));
        player.handleInput(KeyCode.D, keys.getOrDefault(KeyCode.D, false));
        player.handleInput(KeyCode.H, keys.getOrDefault(KeyCode.H, false));
        
        if (keys.getOrDefault(KeyCode.Q, false)) {
            player.useCherryBomb(activeCherryBombs);
        }
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
                    default:
                        gc.setFill(Color.LIGHTGRAY);
                        gc.fillRect(px, py, LevelGenerator.TILE_SIZE, LevelGenerator.TILE_SIZE);
                }
            }
        }
    }

    private void renderUI() {
        player.renderHUD(gc, BASE_WIDTH);
        
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
    
    private void levelComplete() {
        levelComplete = true;
        SoundManager.stopBGM();
        SoundManager.playAlert();
    }
    
    private void generateNewLevel() {
        level = new LevelGenerator();
        
        initialLevelState = new int[LevelGenerator.WIDTH][LevelGenerator.HEIGHT];
        for (int x = 0; x < LevelGenerator.WIDTH; x++) {
            for (int y = 0; y < LevelGenerator.HEIGHT; y++) {
                initialLevelState[x][y] = level.getTile(x, y);
            }
        }
        
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
    
//    public void exitToMainMenu() {
//        SoundManager.stopBGM();
//        new MainMenu((Stage)canvas.getScene().getWindow());
//    }
    
   
    public void startGame(String classType, Stage stage) {
        this.selectedClass = classType;
        this.primaryStage = stage;

        new Thread(() -> {
            // Background work
            initializeGame();
            
            // Switch to FX thread for UI operations
            javafx.application.Platform.runLater(() -> {
                try {
                    setupCanvas();
                    startGameLoop();
                    primaryStage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }
    
    private void setupCanvas() {
        canvas = new Canvas(BASE_WIDTH, BASE_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        
        setupEventHandlers(scene);
        configureStage(scene);
    }

}