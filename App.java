/* 
 assumptions:
 -all lectures are only one hour long 
 -room number accepts any input as room numbers vary greatly and have no outline format
 -displays the schedule for the current week of servers system 
 -classes take place from 9-6 (final class starts at 5 as the college day ends at 6)
 */
package com.mycompany.hellofx;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * main class for the client app, starts GUI and initializes MVC components.
 */
public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        //initialize the MVC components for the client
        ClientModel model = new ClientModel();
        ClientView view = new ClientView();
        new ClientController(model, view);

        primaryStage.setTitle("Lecture Scheduler Client");
        primaryStage.setScene(view.getScene());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
