// BuildingSelector.java
package gui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.util.stream.IntStream;

public class BuildingSelector {
    public Parent getView() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(30));

        IntStream.rangeClosed(1, 5).forEach(i -> {
            String dbName = "property_" + i;
            Button button = new Button("Test Property " + i);
            button.setPrefWidth(250);
            button.setOnAction(e -> {
                TenantManager manager = new TenantManager(dbName);
                SceneSwitcher.switchTo(manager.getView(), "Property " + i);
            });
            root.getChildren().add(button);
        });

        return root;
    }
}

