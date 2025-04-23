/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author hsamp
 */

/**
 * View class for the server GUI. It sets up the JavaFX user interface 
 * with a server response display area and buttons to start/stop the server.
 */
public class ServerView {
    public Button startButton = new Button("Start Server");
    public Button stopButton  = new Button("Stop Server");
    public TextArea serverResponseArea   = new TextArea();
    private Scene scene;

    public ServerView() {
        //initially stop button is disabled becuase servers not running
        stopButton.setDisable(true);
        serverResponseArea.setEditable(false);

        //styles for the log area setting font and font size
        serverResponseArea.setStyle("-fx-font-family: Consolas, Monospace; -fx-font-size: 12;");

        //create a header label for the log area
        Label logLabel = new Label("Server Log:");
        logLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");

        //horizontal box with buttons in them 
        HBox controlBar = new HBox(10, startButton, stopButton);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setPadding(new Insets(10));

        //creating main layout with button hbox at top nad server log in center
        BorderPane layout = new BorderPane();
        layout.setTop(controlBar);
        //adding spacing between log label and log area
        layout.setCenter(new VBox(10, logLabel, serverResponseArea)); 
        layout.setPadding(new Insets(15)); 
        layout.setPrefSize(600, 400);

        //setting background and button colors
        layout.setStyle("-fx-background-color: #E8F6F3;"); 
        for (Button btn : new Button[]{startButton, stopButton}) { 
            btn.setStyle("-fx-background-color: #005335; -fx-text-fill: white;");
        }

        scene = new Scene(layout);
    }

    //appends a message to the server response text area 
    public void appendServerResponseArea(String message) {
        //each on new line
        serverResponseArea.appendText(message + "\n");
    }

    //returns the primary scene for this view for display on the Stage 
    public Scene getScene() {
        return scene;
    }
}
