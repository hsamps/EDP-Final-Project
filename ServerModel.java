/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.server;

import javafx.concurrent.Task;
import javafx.application.Platform;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *
 * @author hsamp
 */

/**
 * Model class for the server side of Lecture Scheduler.
 * Manages the lecture schedule data and handles network connections.
 * It spawns a thread to listen for client connections and creates a new thread per client.
 * All modifications to the shared schedule (a HashMap) are synchronized to prevent race conditions.
 * Also provides the "early lectures" feature which shifts lectures earlier in the day if possible, 
 * using a separate thread for each weekday (divide-and-conquer approach).
 */
public class ServerModel {
    private static final int PORT = 1234;
    private final Map<String, String> schedule = new HashMap<>();  //shared lecture schedule: key "YYYY-MM-DD hh:mm", value "Room,Module"
    private ServerSocket serverSocket;
    private volatile boolean running = false;      //server running flag for the accept loop
    private Consumer<String> logCallback;          //callback to send log messages to the UI
    private final AtomicInteger clientCount = new AtomicInteger(0);  //counter to label client threads

    //date formatter for schedule keys (dates in "YYYY-MM-DD" format)
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ServerModel() {
        //load initial schedule data from CSV file into the HashMap
        loadScheduleCSV();
    }

    //set a callback to log messages which is appending to a GUI text area. 
    public void setLogCallback(Consumer<String> logCallback) {
        this.logCallback = logCallback;
    }

    //helper to log a message to the server GUI log area in a thread safe way
    private void serverMessage(String message) {
        if (logCallback != null) {
            //ensures UI updates happen on JavaFX Application Thread
            Platform.runLater(() -> logCallback.accept(message));
        }
    }

    /**
     * Starts the server by opening a ServerSocket and listening for client connections in a background thread.
     * Each client connection is handled on its own thread (ClientHandler).
     */
    public void startServer() {
        if (running) {
            return;//hence already running
        }
        //otherwise it will open new server socket
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            serverMessage("Error: Could not start server on port " + PORT + " - " + e.getMessage());
            return;
        }
        running = true;
        serverMessage("Server started on port " + PORT + ". Waiting for clients...");
        //background thread to accept client connections
        Thread acceptThread = new Thread(() -> {
            try {
                while (running) {
                    //accept incoming client socket, blocks until a client connects
                    Socket clientSocket = serverSocket.accept();
                    //label this client and increment counter
                    int clientId = clientCount.incrementAndGet();
                    String clientName = "Client-" + clientId + " (" + clientSocket.getInetAddress().getHostAddress() + ")";
                    serverMessage("Connection accepted from " + clientName);
                    //handle this client on a new thread
                    Thread clientThread = new Thread(new ClientHandler(clientSocket, clientName));
                    clientThread.setDaemon(true);
                    clientThread.start();
                }
            } catch (IOException e) {
                if (running) {
                    //if an exception occurs while running
                    serverMessage("Error: Server accept loop interrupted - " + e.getMessage());
                }
            } finally {
                running = false;
                //clean up server socket if not already closed
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException ex) {
                    serverMessage("Error closing server socket: " + ex.getMessage());
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * Stops the server by closing the ServerSocket and halting the accept loop.
     * Already connected client threads if there are any will finish processing their requests.
     */
    public void stopServer() {
        if (!running) {
            return;
        }
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();  //this will cause the accept loop to exit
            }
        } catch (IOException e) {
            serverMessage("Error: Could not close server socket - " + e.getMessage());
        }
        serverMessage("Server stopped.");
    }

    /**
     * Inner class that handles an individual client connection in a separate thread.
     * It reads one request from the client, processes it, and sends back a response.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientName;
        
        //constructor
        public ClientHandler(Socket clientSocket, String clientName) {
            this.socket = clientSocket;
            this.clientName = clientName;
        }

        @Override
        public void run() {
            try (
                //initalize in and out streams
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) { 
                //read a single line command from the client
                String request = in.readLine();
                if (request == null) {
                    return;  //check no request means client disconnected
                }
                serverMessage(clientName + " >> " + request);  //log received command

                //process the request and generate a response
                String response;
                try {
                    //calls handle request method
                    response = handleRequest(request);
                } catch (IncorrectActionException e) {
                    //catches the incorrectActionException as per biref
                    response = "Exception: " + e.getMessage();
                }

                //send the response back to the client
                out.println(response);
                serverMessage(clientName + " << " + response.replace("\n", " | "));
            } catch (IOException e) {
                serverMessage("Error handling " + clientName + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignore) {}
                serverMessage(clientName + " disconnected.");
            }
        }
    }

    /**
     * Parses and handles a single client request command.
     * Recognized commands: add, remove, displayschedule, earlylectures, stop.
     * Returns the result string to send back to the client.
     * @throws IncorrectActionException if the action command is not supported.
     */
    private String handleRequest(String request) throws IncorrectActionException {
        if (request == null || request.isBlank()) {
            return "Error: Empty request.";
        }
        //commands are expected in CSV format: action, date, time, room, module (depending on action)
        String[] parts = request.split(",", -1);
        String action = parts[0].trim().toLowerCase();

        switch (action) {
            case "add":
                return addLecture(parts);
            case "remove":
                return removeLecture(parts);
            case "displayschedule":
                return displaySchedule();
            case "earlylectures":
                return earlyLectures();
            case "stop":
                //stop command indicates the client wishes to terminate its session.
                //the server will respond with "TERMINATE", and the client should interpret it as a signal to close.
                return "TERMINATE";
            default:
                //unrecognizzed command throws inccorrect action exception
                throw new IncorrectActionException("Unsupported action: '" + action + "'"); // [ADDED]
        }
    }

    /**
     * Adds a lecture to the schedule if possible.
     * Expected format: add,date,time,room,module
     * Checks for scheduling conflicts like same time and room, or same time and different module indicating a timetable conflict.
     * Synchronized to prevent concurrent modifications to the schedule.
     * @return confirmation or error message.
     */
    private String addLecture(String[] parts) {
        if (parts.length < 5) {
            return "Error: Invalid format. Use add,date,time,room,module";
        }
        String date = parts[1].trim();
        String time = parts[2].trim();
        String room = parts[3].trim();
        String module = parts[4].trim();
        String newKey = date + " " + time;

        //synchronize on the schedule for thread safe check and insert
        synchronized (schedule) {
            //check for any conflict at the given date and time
            for (Map.Entry<String, String> entry : schedule.entrySet()) {
                String[] keyParts = entry.getKey().split(" ");
                String entryDate = keyParts[0];
                String entryTime = keyParts[1];
                String[] valueParts = entry.getValue().split(",");
                String entryRoom = valueParts[0];
                String entryModule = valueParts[1];

                if (entryDate.equals(date) && entryTime.equals(time)) {
                    //a lecture is already scheduled at the exact same date and time
                    if (entryRoom.equalsIgnoreCase(room)) {
                        //same room conflict
                        return "Clash: Room already booked at " + time + " on " + date;
                    }
                    if (!entryModule.equalsIgnoreCase(module)) {
                        //different module at the same time assume 
                        return "Clash: Lecture already Scheduled (" + entryModule + ") at " + time + " on " + date;
                    }
                }
            }
            //no conflict so add the new lecture
            schedule.put(newKey, room + "," + module);
            //add changes to the CVS file
            saveScheduleCSV();  
        }
        return "Lecture scheduled: " + module + " at " + time + " on " + date + " in " + room;
    }

    /**
     * Removes a lecture from the schedule.
     * Expected format: remove,date,time,room,module
     * It will only remove the lecture if the date, time, room, and module all match an existing entry.
     * Synchronized to prevent concurrent modifications.
     * @return confirmation or error message.
     */
    private String removeLecture(String[] parts) {
        if (parts.length < 5) {
            return "Error: Invalid format. Use remove,date,time,room,module";
        }
        String date = parts[1].trim();
        String time = parts[2].trim();
        String room = parts[3].trim();
        String module = parts[4].trim();
        String key = date + " " + time;

        synchronized (schedule) {
            if (!schedule.containsKey(key)) {
                return "Error: No lecture found at " + time + " on " + date;
            }
            //check that the room and module match the stored entry
            String[] valueParts = schedule.get(key).split(",");
            String entryRoom = valueParts[0];
            String entryModule = valueParts[1];
            if (!entryRoom.equalsIgnoreCase(room) || !entryModule.equalsIgnoreCase(module)) {
                return "Error: No matching lecture found at " + time + " on " + date + " in room " + room;
            }
            //remove the lecture
            schedule.remove(key);
            //rewrite new timetable to CSV
            saveScheduleCSV();  
        }
        return "Lecture removed: " + module + " at " + time + " on " + date + " in room " + room;
    }

    /**
     * Displays the schedule for the current week (Monday through Friday).
     * Retrieves all lectures between this weeks Monday and Friday and formats them in a table.
     * Synchronized to prevent concurrent read and write issues while iterating the schedule.
     * @return a multi line string listing the week’s lectures, or a message if none.
     */
    private String displaySchedule() {
        //use a synchronized block to safely go over the schedule
        synchronized (schedule) {
            if (schedule.isEmpty()) {
                return "No scheduled lectures.";
            }
            //getting current week to get schedule for the current week
            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDate friday = today.with(DayOfWeek.FRIDAY);
            //init string builder and build a schedule for a selected week
            StringBuilder sb = new StringBuilder("Week Schedule:\nDATE       | TIME  | ROOM   | MODULE\n");
            schedule.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        String[] keyParts = entry.getKey().split(" ");
                        String entryDate = keyParts[0];
                        String entryTime = keyParts[1];
                        LocalDate entryDay = LocalDate.parse(entryDate, dateFormatter);
                        if (!entryDay.isBefore(monday) && !entryDay.isAfter(friday)) {
                            String[] values = entry.getValue().split(",");
                            String room = values[0];
                            String module = values[1];
                            sb.append(entryDate).append(" | ").append(entryTime)
                              .append(" | ").append(room).append(" | ").append(module).append("\n");
                        }
                    });
            return sb.toString().trim();
        }
    }

    /**
     * Shifts all lectures earlier in the day if earlier time slots are available implementation of Early Lectures button.
     * This uses a divide and conquer approach by spawning a separate thread to handle rescheduling for each weekday (Monday Friday).
     * The operation runs in the background so that the server GUI remains responsive.
     * @return a message indicating the operation is complete, along with the updated weekly schedule.
     */
    private String earlyLectures() {
        //define the range of days which is current weeks Monday to Friday to process
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate friday = today.with(DayOfWeek.FRIDAY);

        serverMessage("Early lectures command received. Rescheduling lectures to earlier slots...");
        //javaFX Task to perform the rescheduling in background
        Task<Void> rescheduleTask = new Task<Void>() {
            @Override
            protected Void call() {
                List<Thread> dayThreads = new ArrayList<>();
                //iterate Monday through Friday
                for (LocalDate date = monday; !date.isAfter(friday); date = date.plusDays(1)) {
                    final String dateStr = date.format(dateFormatter);
                    //create a thread to shift lectures for this specific date
                    Thread t = new Thread(() -> shiftLecturesForDay(dateStr));
                    t.setDaemon(true);
                    dayThreads.add(t);
                    t.start();
                }
                //wait for all day threads to finish
                for (Thread t : dayThreads) {
                    try {
                        t.join();
                    } catch (InterruptedException ie) {
                        //if interrupted log and continue waiting for others
                        serverMessage("Warning: Early lectures thread interrupted: " + ie.getMessage());
                    }
                }
                return null;
            }
        };

        //when the task finishes, update the log indicating completion
        rescheduleTask.setOnSucceeded(e -> serverMessage("Early lectures rescheduling completed for the week."));
        //start the background task
        new Thread(rescheduleTask).start();

        try {
            //block until the rescheduling task is complete so we only respond to the client after finishing.
            rescheduleTask.get();  //waits for completion
        } catch (InterruptedException | ExecutionException ex) {
            //this catch handles InterruptedException or ExecutionException from task.get()
            serverMessage("Error during early lectures rescheduling: " + ex.getMessage());
        }

        //save the updated schedule to CSV after all rescheduling is done
        saveScheduleCSV();

        //return a confirmation and the updated week schedule to the client
        String resultMessage = "All lectures shifted to earlier slots where possible.\n";
        resultMessage += displaySchedule();
        return resultMessage;
    }

    /**
     * Helper method to shift all lectures for a given date to the earliest possible time slots.
     * This method runs in a separate thread for each day. It uses synchronization on the schedule map to avoid conflicts.
     * @param dateStr the date (YYYY-MM-DD) for which to shift lectures earlier.
     */
    private void shiftLecturesForDay(String dateStr) {
        //list of standard lecture hours in a day 9:00 through 17:00
        String[] possibleTimes = { "09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00" };

        //collect all lectures for this date and sort them by time
        List<String> times;
        synchronized (schedule) {
            times = new ArrayList<>();
            for (String key : schedule.keySet()) {
                if (key.startsWith(dateStr + " ")) {
                    //extract the "hh:mm" part of the key
                    String time = key.substring(dateStr.length() + 1);
                    times.add(time);
                }
            }
        }
        if (times.isEmpty()) {
            //no lectures on this date, nothing to shift
            return;
        }
        Collections.sort(times);  //sort lecture times 

        //iterate through the days lectures shifting each to the earliest available slot
        String nextSlot = "09:00";  //start with the earliest possible time which is 9:00
        for (String time : times) {
            //use string comparison since format HH:MM
            if (time.compareTo(nextSlot) > 0) {
                //an earlier slot is available before 'time': move this lecture to 'nextSlot'
                String oldKey = dateStr + " " + time;
                String newKey = dateStr + " " + nextSlot;
                //synchronize modifications to the schedule map
                synchronized (schedule) {
                    //double check the lecture still exists at oldKey 
                    String value = schedule.remove(oldKey);
                    if (value != null) {
                        schedule.put(newKey, value);
                    }
                }
                //log the move room and module moved from one time to another
                serverMessage("Moved lecture on " + dateStr + " from " + time + " to " + nextSlot);
                //set nextSlot to the next hour after this one using method to increase made by myself as it is in HH:MM format
                nextSlot = incrementHour(nextSlot);
            } else {
                //no gap before this lecture; it occupies the current earliest slot
                nextSlot = incrementHour(time);
            }
        }
    }

    /**
     * Method used to increment an hour time string (HH:00) by one hour.
     * If given "09:00", returns "10:00"; if "17:00", returns "18:00" (which is beyond normal schedule hours).
     */
    private String incrementHour(String time) {
        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            int nextHour = hour + 1;
            //format back to HH:00 
            return (nextHour < 10 ? "0" + nextHour : nextHour) + ":00";
        } catch (NumberFormatException e) {
            //precaution if parsing fails, just return the same time to avoid altering schedule incorrectly
            return time;
        }
    }

    /** Loads the lecture schedule from a CSV file into the schedule map. */
    private void loadScheduleCSV() {
        File file = new File("SCHEDULE.csv");
        if (!file.exists()) {
            //No existing schedule file
            return;  
        }
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String[] line = scanner.nextLine().split(",", -1);
                if (line.length == 4) {
                    String date = line[0].trim();
                    String time = line[1].trim();
                    String room = line[2].trim();
                    String module = line[3].trim();
                    String key = date + " " + time;
                    String value = room + "," + module;
                    schedule.put(key, value);
                }
            }
        } catch (FileNotFoundException e) {
            serverMessage("Error loading schedule CSV: " + e.getMessage());
        }
    }

    /** Saves the current lecture schedule to a CSV file (overwriting the file with current data). */
    private void saveScheduleCSV() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("SCHEDULE.csv"))) {
            //synchronized iteration to avoid concurrent modification while writing
            synchronized (schedule) {
                for (Map.Entry<String, String> entry : schedule.entrySet()) {
                    String[] keyParts = entry.getKey().split(" ");
                    String date = keyParts[0];
                    String time = keyParts[1];
                    String[] valParts = entry.getValue().split(",");
                    String room = valParts[0];
                    String module = valParts[1];
                    //write each lecture as CSV line: date,time,room,module
                    writer.println(String.join(",", date, time, room, module));
                }
            }
        } catch (IOException e) {
            serverMessage("Error: Could not save schedule to CSV - " + e.getMessage());
        }
    }

    /** Custom exception for unsupported or malformed actions/commands. */
    //custom exception for unsupported actions required in brief.
    public static class IncorrectActionException extends Exception {
        private String message;
        //no arg constructor
        public IncorrectActionException (){
            this.message = "Error! Incorrect Action!";
        }
        //arg constructor
        public IncorrectActionException(String message) {
            super(message);
            this.message = message;
        }
        //getter
        public String getIncorrectAction(){
            return this.message;
        }
    }
}
