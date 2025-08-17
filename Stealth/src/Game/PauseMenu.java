package Game;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

public class PauseMenu {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 300;
    private static final Font TITLE_FONT = new Font("Arial", 36);
    private static final Font MENU_FONT = new Font("Arial", 24);
    private static final Font SELECTED_FONT = new Font("Arial Bold", 24);
    
    private Canvas canvas;
    private GraphicsContext gc;
    private Stage stage;
    private GameApp gameApp;
    private int selectedOption = 0;
    private final String[] options = {"Resume", "Settings", "Exit"};
    
    public PauseMenu(Stage primaryStage, GameApp gameApp) {
        this.stage = new Stage();
        this.gameApp = gameApp;
        
        // Configure the pause menu stage
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(primaryStage);
        stage.setTitle("Game Paused");
        
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        
        // Handle keyboard input
        scene.setOnKeyPressed(this::handleKeyPress);
        
        stage.setScene(scene);
        render();
    }
    
    private void render() {
        // Clear screen with semi-transparent background
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw title
        gc.setFont(TITLE_FONT);
        gc.setFill(Color.WHITE);
        String title = "GAME PAUSED";
        double titleWidth = gc.getFont().getSize() * title.length() * 0.6;
        gc.fillText(title, (WIDTH - titleWidth) / 2, 60);
        
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
            gc.fillText(options[i], (WIDTH - textWidth) / 2, 120 + i * 50);
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
            case ESCAPE:
                // ESC also resumes the game
                selectOption(0);
                break;
        }
    }
    
    private void selectOption(int option) {
        switch (option) {
            case 0: // Resume
                stage.close();
                gameApp.resumeGame();
                break;
            case 1: // Settings
                showSettingsDialog();
                break;
            case 2: // Exit
                showExitConfirmation();
                break;
        }
    }
    
    private void showSettingsDialog() {
        Alert settings = new Alert(AlertType.INFORMATION);
        settings.setTitle("Settings");
        settings.setHeaderText("Game Settings");
        settings.setContentText("Volume controls and other settings coming soon!");
        settings.initOwner(stage);
        settings.showAndWait();
    }

	private void showExitConfirmation() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirm Exit");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("All progress will be lost.");
        
        alert.initOwner(stage);
        
        // Customize button text
        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == yesButton) {
                stage.close();
                gameApp.showMainMenu();
            }
        });
    }
    
    public void show() {
        stage.show();
    }
}