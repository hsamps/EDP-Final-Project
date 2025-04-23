/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.hellofx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 *
 * @author hsamp
 */

/**
 * View class for the client application. Provides the GUI for scheduling lectures: 
 * date picker, time choice, room input, module selection, and action buttons.
 */
public class ClientView {
    //input controls
    public DatePicker datePicker = new DatePicker();
    public ChoiceBox<String> timeBox = new ChoiceBox<>();
    public TextField roomField = new TextField();
    public ChoiceBox<String> moduleBox = new ChoiceBox<>();
    //action buttons
    public Button addButton = new Button("Add Lecture");
    public Button removeButton = new Button("Remove Lecture");
    public Button displayButton = new Button("Display Schedule");
    public Button earlyButton = new Button("Early Lectures");
    public Button otherButton = new Button("Other");
    public Button stopButton = new Button("Stop");
    //area to display server responses
    public TextArea responseArea = new TextArea();
    private Scene scene;

    public ClientView() {
        //initialize all inputs with default controls
        datePicker.setValue(LocalDate.now());
        datePicker.setDayCellFactory(getWeekdayCellFactory());  //limit selection to weekdays
        timeBox.getItems().addAll("09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00");
        timeBox.setValue("09:00");
        moduleBox.getItems().addAll(
                "Event Driven Programming", 
                "Data Structures and Algorithms", 
                "Intelligent System", 
                "Statistics", 
                "Computer Graphics"
        );
        moduleBox.setValue("Event Driven Programming");
        roomField.setPromptText("Room Number");

        //arrange input labels and fields in a grid pane
        Label dateLabel = new Label("Date:");
        Label timeLabel = new Label("Time:");
        Label roomLabel = new Label("Room:");
        Label moduleLabel = new Label("Module:");
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.setPadding(new Insets(10));
        inputGrid.add(dateLabel,   0, 0); inputGrid.add(datePicker, 1, 0);
        inputGrid.add(timeLabel,   0, 1); inputGrid.add(timeBox,    1, 1);
        inputGrid.add(roomLabel,   0, 2); inputGrid.add(roomField,  1, 2);
        inputGrid.add(moduleLabel, 0, 3); inputGrid.add(moduleBox,  1, 3);

        //arrange buttons in a horizontal box
        HBox buttonBox = new HBox(10, addButton, removeButton, displayButton, earlyButton, otherButton); // [MODIFIED] added otherButton to button bar
        buttonBox.setAlignment(Pos.CENTER);
        stopButton.setMaxWidth(Double.MAX_VALUE);
        //use a vertical layout for buttons, input form, stop button, and response area
        VBox mainLayout = new VBox(15, buttonBox, inputGrid, stopButton, responseArea);
        mainLayout.setPadding(new Insets(15));

        //styles for a nicer UI
        mainLayout.setStyle("-fx-background-color: #E8F6F3;");  //background color
        for (Button btn : new Button[]{addButton, removeButton, displayButton, earlyButton, otherButton, stopButton}) { // [MODIFIED] added otherButton to style loop
            btn.setStyle("-fx-background-color: #005335; -fx-text-fill: white;");
        }
        for (Label lbl : new Label[]{dateLabel, timeLabel, roomLabel, moduleLabel}) {
            lbl.setStyle("-fx-font-weight: bold;");
        }
        responseArea.setEditable(false);
        responseArea.setWrapText(true);

        scene = new Scene(mainLayout, 600, 500);
    }

    //limit the DatePicker to weekdays only making weekend days unselectable and red
    private Callback<DatePicker, DateCell> getWeekdayCellFactory() {
        return picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;"); //mark weekends in red
                }
            }
        };
    }

    //returns the main scene for the client UI
    public Scene getScene() {
        return scene;
    }
}
