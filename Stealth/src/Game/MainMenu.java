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
    private final Stage stage;
    private final GameApp gameApp;
    private int selectedOption = 0;
    private final String[] options = {"Start Game", "Exit"};
    
    public MainMenu(Stage stage, GameApp gameApp) {
        this.stage = stage; 
        this.gameApp = gameApp;
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        
        canvas.setOnMouseClicked(this::handleMouseClick);
        scene.setOnKeyPressed(this::handleKeyPress);
        
        stage.setScene(scene);
        render();
    }
    
    private void render() {
        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        gc.setFont(TITLE_FONT);
        gc.setFill(Color.WHITE);
        String title = "STEALTH: ROGUELIKE";
        double titleWidth = gc.getFont().getSize() * title.length() * 0.6;
        gc.fillText(title, (WIDTH - titleWidth) / 2, 100);
        
        double boxWidth = 300;
        double boxHeight = 150;
        double boxX = (WIDTH - boxWidth) / 2;
        double boxY = 150;
        
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.strokeRect(boxX, boxY, boxWidth, boxHeight);
        
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
        
        gc.setFont(VERSION_FONT);
        gc.setFill(Color.GRAY);
        gc.fillText("Version 1.0", WIDTH - 80, HEIGHT - 20);
    }
    
    private void handleMouseClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        
        double boxX = (WIDTH - 300) / 2;
        double boxY = 150;
        
        if (x >= boxX && x <= boxX + 300) {
            if (y >= boxY + 50 && y <= boxY + 90) {
                selectOption(0);
            } else if (y >= boxY + 110 && y <= boxY + 150) {
                selectOption(1);
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
            case 0:
                // Pass the existing gameApp instance to ClassSelectionScreen
                new ClassSelectionScreen(stage, gameApp);
                break;
            case 1:
                System.exit(0);
                break;
        }
    }
}