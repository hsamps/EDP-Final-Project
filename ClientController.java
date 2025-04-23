/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.hellofx;

/**
 *
 * @author hsamp
 */

/**
 * Controller class for the client GUI. It handles user interactions: 
 * building command strings from input fields and invoking the model to send requests to the server.
 * The responses from the server are then displayed in the view's text area.
 */
public class ClientController {
    private final ClientModel model;
    private final ClientView view;

    public ClientController(ClientModel model, ClientView view) {
        this.model = model;
        this.view = view;

        //attach event handlers for each button
        view.addButton.setOnAction(e -> handleAdd());
        view.removeButton.setOnAction(e -> handleRemove());
        view.displayButton.setOnAction(e -> handleDisplay());
        view.earlyButton.setOnAction(e -> handleEarlyLectures());
        view.otherButton.setOnAction(e -> handleOther()); 
        view.stopButton.setOnAction(e -> handleStop());
    }

    //handle Add Lecture button send an "add" command with the form data to the server. 
    private void handleAdd() {
        String date   = view.datePicker.getValue().toString();
        String time   = view.timeBox.getValue();
        String room   = view.roomField.getText().trim();
        String module = view.moduleBox.getValue();
        if (time.isEmpty() || room.isEmpty() || module.isEmpty()) {
            view.responseArea.setText("Please enter Time, Room, and Module for adding a lecture.");
            return;
        }
        String message = "add," + date + "," + time + "," + room + "," + module;

        //send to server and display response
        view.responseArea.setText(model.sendMessage(message));
    }

    //handle Remove Lecture button send a "remove" command to the server with the given date, time, and room
    private void handleRemove() {
        String date   = view.datePicker.getValue().toString();
        String time   = view.timeBox.getValue();
        String room   = view.roomField.getText().trim();
        String module = view.moduleBox.getValue();
        if (time.isEmpty() || room.isEmpty()) {
            view.responseArea.setText("Please enter Time, Room, and Module for removing a lecture.");
            return;
        }
        String message = "remove," + date + "," + time + "," + room + "," + module;
        view.responseArea.setText(model.sendMessage(message));
    }

    //handle Display Schedule button request the current week schedule from the server
    private void handleDisplay() {
        view.responseArea.setText(model.sendMessage("displayschedule"));
    }

    //handle Early Lectures button request the server to shift lectures to earlier slots
    private void handleEarlyLectures() {
        //send the 'earlylectures' command. The server will perform the operation and return a result.
        view.responseArea.setText(model.sendMessage("earlylectures"));
    }

    //handle Other button send an unsupported command
    private void handleOther() {
        //send a fixed unknown command to test server exception handling
        view.responseArea.setText(model.sendMessage("unknownaction"));
    }

    //handle Stop button send the stop command to terminate the server session and disable client controls
    private void handleStop() {
        //the server will respond with "TERMINATE" which signals the client to close
        view.responseArea.setText(model.sendMessage("stop") + "\nConnection closed by client.");
        //disable all action buttons after stopping
        view.addButton.setDisable(true);
        view.removeButton.setDisable(true);
        view.displayButton.setDisable(true);
        view.earlyButton.setDisable(true);
        view.otherButton.setDisable(true); 
        view.stopButton.setDisable(true);
    }
}
