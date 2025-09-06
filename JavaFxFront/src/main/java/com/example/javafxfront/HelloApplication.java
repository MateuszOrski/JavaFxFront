package com.example.javafxfront;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("modern-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 800);

        // Dodanie CSS stylów
        //Komentarz do commita o 3 pierwszych dzialajacyh punktach
        //Naprawa widzenia terminow
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        stage.setTitle("Dziennik Online - Zarządzanie Grupami");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}