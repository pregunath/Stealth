package Game;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setTitle("Stealth Roguelike");
            GameApp gameApp = new GameApp(primaryStage);  // Pass stage to constructor
            new MainMenu(primaryStage, gameApp);
            primaryStage.show();
        } catch (Exception e) {
            showErrorAlert("Failed to start game", e.getMessage());
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle(title);
        error.setHeaderText(null);
        error.setContentText(message);
        error.showAndWait();
    }
}