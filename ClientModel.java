/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.hellofx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author hsamp
 */

/**
 * Model class for the client side. Manages sending requests to the server and receiving responses.
 * It opens a socket connection to the server for each request, sends the message, and collects the response.
 */
public class ClientModel {
    private static final int PORT = 1234;
    private static final String HOST = "localhost";

    /**
     * Sends a request message to the server and returns the server's response.
     * Opens a new TCP connection for the request and closes it after the response is received.
     * @param message the request command to send (e.g., "add,...", "remove,...", "earlylectures", etc.)
     * @return the response from the server as a String.
     */
    public String sendMessage(String message) {
        StringBuilder response = new StringBuilder();
        try (
            Socket socket = new Socket(HOST, PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            //send the request to the server
            out.println(message);
            //read all response lines from the server until the connection is closed or a terminate signal is received
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                //if the server signals termination break out
                if (line.contains("TERMINATE")) {
                    break;
                }
            }
        } catch (IOException e) {
            return "Connection error: " + e.getMessage();
        }
        return response.toString().trim();
    }
}
