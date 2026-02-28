package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    private int clicks = 0;

    @Override
    public void start(Stage stage) {
        Label title = new Label("Hello JavaFX");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label counter = new Label("Clicks: 0");

        Button button = new Button("Click me");
        button.setOnAction(event -> counter.setText("Clicks: " + (++clicks)));

        VBox content = new VBox(12, title, counter, button);
        content.setPadding(new Insets(20));

        BorderPane root = new BorderPane(content);
        Scene scene = new Scene(root, 420, 220);

        stage.setTitle("My First JavaFX App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
