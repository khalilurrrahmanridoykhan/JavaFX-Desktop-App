package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        MapView mapView = new MapView();
        new MapController(mapView);

        Label title = new Label("Bangladesh Map Explorer");
        title.getStyleClass().add("app-title");

        ToggleButton darkToggle = new ToggleButton("Dark mode");
        darkToggle.getStyleClass().add("mode-toggle");

        HBox header = new HBox(16, title, darkToggle);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 22, 12, 22));
        header.getStyleClass().add("app-header");

        BorderPane root = new BorderPane(mapView);
        root.setTop(header);
        root.getStyleClass().add("app-root");

        darkToggle.selectedProperty().addListener((obs, wasOn, isOn) -> {
            if (isOn) {
                if (!root.getStyleClass().contains("dark")) {
                    root.getStyleClass().add("dark");
                }
            } else {
                root.getStyleClass().remove("dark");
            }
        });

        Scene scene = new Scene(root, 1100, 760);
        scene.getStylesheets().add(Main.class.getResource("/styles.css").toExternalForm());
        stage.setTitle("My First JavaFX Map App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
