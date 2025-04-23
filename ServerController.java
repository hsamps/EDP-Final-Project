/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

import javafx.application.Platform;

/**
 *
 * @author hsamp
 */

/**
 * Controller class for the server GUI. It connects the ServerModel
 * with the ServerView. It sets up event handlers for the start and stop buttons 
 * and ensures server response messages from the model are displayed on the view.
 */
public class ServerController {
    private final ServerModel model;
    private final ServerView view;

    public ServerController(ServerModel model, ServerView view) {
        this.model = model;
        this.view = view;
        //set up the model to use the view's server response area for logging server responses
        model.setLogCallback(message -> Platform.runLater(() -> view.appendServerResponseArea(message)));

        //event handler for start server button
        view.startButton.setOnAction(e -> {
            model.startServer();
            view.startButton.setDisable(true);
            view.stopButton.setDisable(false);
        });

        //event handler for stop server button
        view.stopButton.setOnAction(e -> {
            model.stopServer();
            view.stopButton.setDisable(true);
            view.startButton.setDisable(false);
        });
    }
}
