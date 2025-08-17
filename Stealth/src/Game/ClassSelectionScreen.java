package Game;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ClassSelectionScreen {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final Font TITLE_FONT = new Font("Arial", 48);
    private static final Font CLASS_FONT = new Font("Arial", 24);
    private static final Font DESC_FONT = new Font("Arial", 16);
    
    private Canvas canvas;
    private GraphicsContext gc;
    private final Stage stage;
    private final GameApp gameApp;
    
    public ClassSelectionScreen(Stage stage, GameApp gameApp) {
        this.stage = stage;
        this.gameApp = gameApp;
        
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);
        
        canvas.setOnMouseClicked(this::handleMouseClick);
        
        stage.setScene(scene);
        render();
        stage.show();
    }
    
    private void render() {
        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Title
        gc.setFont(TITLE_FONT);
        gc.setFill(Color.WHITE);
        gc.fillText("Choose Your Class", 200, 80);
        
        // Sneaky Class
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(100, 150, 250, 300);
        gc.setFont(CLASS_FONT);
        gc.setFill(Color.BLACK);
        gc.fillText("Sneaky", 180, 180);
        
        gc.setFont(DESC_FONT);
        gc.fillText("Specializes in stealth and", 110, 220);
        gc.fillText("avoiding detection.", 110, 240);
        gc.fillText("- Shadow Cloak", 110, 280);
        gc.fillText("- Silent Boots", 110, 300);
        
        // Agile Class
        gc.setFill(Color.LIGHTCORAL);
        gc.fillRect(450, 150, 250, 300);
        gc.setFont(CLASS_FONT);
        gc.setFill(Color.BLACK);
        gc.fillText("Agile", 530, 180);
        
        gc.setFont(DESC_FONT);
        gc.fillText("Specializes in movement and", 460, 220);
        gc.fillText("quick traversal.", 460, 240);
        gc.fillText("- Swift Boots", 460, 280);
        gc.fillText("- Acrobat's Belt", 460, 300);
    }
    
    private void handleMouseClick(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        
        if (x >= 100 && x <= 350 && y >= 150 && y <= 450) {
            showConfirmation("Sneaky", "stealth and avoiding detection");
        }
        else if (x >= 450 && x <= 700 && y >= 150 && y <= 450) {
            showConfirmation("Agile", "movement and quick traversal");
        }
    }
    
    private void showConfirmation(String className, String classDescription) {
        if (gameApp == null) {
            System.err.println("Error: gameApp is null in ClassSelectionScreen");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Class Selection");
        confirmation.setHeaderText("Choose " + className + " Class?");
        confirmation.setContentText("You're about to select the " + className + 
                                 " class which specializes in " + classDescription + 
                                 ".\nAre you sure?");
        
        confirmation.initOwner(stage);
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                gameApp.startGame(className, stage);
                showSuccessAlert(className);
            }
        });
    }
    
    
    private void showSuccessAlert(String className) {
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Class Selected");
        success.setHeaderText("You've chosen the " + className + " class!");
        success.setContentText("Your adventure begins now...");
        success.initOwner(stage);
        success.showAndWait();
    }
}