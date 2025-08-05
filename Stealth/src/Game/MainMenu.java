package Game;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainMenu {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final Font TITLE_FONT = new Font("Arial", 48);
    private static final Font MENU_FONT = new Font("Arial", 32);
    private static final Font VERSION_FONT = new Font("Arial", 12);
    private static final Font SELECTED_FONT = new Font("Arial Bold", 32);
    
    private Canvas canvas;
    private GraphicsContext gc;
    private Stage stage;
    private int selectedOption = 0;
    private final String[] options = {"Stealth Mode", "Platformer Mode", "Exit"};
    
    public MainMenu(Stage stage) {
        this.stage = stage;
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        
        // Handle mouse clicks
        canvas.setOnMouseClicked(this::handleMouseClick);
        
        // Handle keyboard input
        scene.setOnKeyPressed(this::handleKeyPress);
        
        stage.setScene(scene);
        render();
    }
    
    private void render() {
        // Clear screen
        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw title
        gc.setFont(TITLE_FONT);
        gc.setFill(Color.WHITE);
        String title = "STEALTH";
        double titleWidth = gc.getFont().getSize() * title.length() * 0.6;
        gc.fillText(title, (WIDTH - titleWidth) / 2, 100);
        
        // Draw menu box
        double boxWidth = 300;
        double boxHeight = 250;
        double boxX = (WIDTH - boxWidth) / 2;
        double boxY = 150;
        
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(boxX, boxY, boxWidth, boxHeight);
        
        // Draw menu options
        for (int i = 0; i < options.length; i++) {
            if (i == selectedOption) {
                gc.setFont(SELECTED_FONT);
                gc.setFill(Color.YELLOW);
            } else {
                gc.setFont(MENU_FONT);
                gc.setFill(Color.WHITE);
            }
            
            double textWidth = gc.getFont().getSize() * options[i].length() * 0.5;
            gc.fillText(options[i], (WIDTH - textWidth) / 2, boxY + 80 + i * 60);
        }
        
        // Draw version number
        gc.setFont(VERSION_FONT);
        gc.setFill(Color.GRAY);
        gc.fillText("Version 1.0", WIDTH - 80, HEIGHT - 20);
    }
    
    private void handleMouseClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        
        // Check if click is within menu box
        double boxX = (WIDTH - 300) / 2;
        double boxY = 150;
        
        if (x >= boxX && x <= boxX + 300) {
            // Check which option was clicked
            if (y >= boxY + 50 && y <= boxY + 90) {
                selectOption(0); // Start Game
            } else if (y >= boxY + 110 && y <= boxY + 150) {
                selectOption(1); // Settings
            } else if (y >= boxY + 170 && y <= boxY + 210) {
                selectOption(2); // Exit
            }
        }
    }
    
    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
                selectedOption = (selectedOption - 1 + options.length) % options.length;
                render();
                break;
            case DOWN:
                selectedOption = (selectedOption + 1) % options.length;
                render();
                break;
            case ENTER:
                selectOption(selectedOption);
                break;
        }
    }
    
    private void selectOption(int option) {
        switch (option) {
            case 0: // Stealth Mode
                new GameApp(false).start(stage); // false for stealth mode
                break;
            case 1: // Platformer Mode
                new GameApp(true).start(stage); // true for platformer mode
                break;
            case 2: // Exit
                System.exit(0);
                break;
        }
    }

    private void showSettings() {
        Alert settings = new Alert(AlertType.INFORMATION);
        settings.setTitle("Settings");
        settings.setHeaderText("Game Settings");
        settings.setContentText("Game options will be available here");
        settings.initOwner(stage);
        settings.showAndWait();
    }
}