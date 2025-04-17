package gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        SceneSwitcher.setPrimaryStage(primaryStage);

        BuildingSelector selector = new BuildingSelector();
        Scene scene = new Scene(selector.getView(), 400, 300);

        primaryStage.setTitle("Property Manager Sample");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
//mvn clean javafx:run