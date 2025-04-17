package gui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneSwitcher {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(Parent view, String title) {
        Scene scene = new Scene(view, 500, 400);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
    }
}