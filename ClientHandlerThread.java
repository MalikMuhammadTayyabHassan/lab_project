import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandlerThread extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Scanner scanner;
    private String clientID;
    private String serverRecipientID;
    private String contactList;
    private MessageManager manager;
    private volatile boolean isRunning;

    public ClientHandlerThread(Socket socket) {

        this.socket = socket;
        scanner = new Scanner(System.in);
        manager = new MessageManager();
        isRunning = true;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error initializing streams: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            authenticateClient();


            new Thread(this::startReaderThread).start();

            // Main thread handles menu and user input
            while (isRunning) {
                showMenu();
                System.out.print("=======> Enter Your Choice: ");
                String choice = scanner.nextLine();

                if (!isValidMenuChoice(choice)) {
                    System.out.println("Invalid choice. Please enter a number between 1 and 7.");
                    continue;
                }

                if ("7".equals(choice)) {
                    exitApplication();
                    break;
                }

                handleMenuChoice(choice);
            }
        } catch (IOException e) {
            System.out.println("Communication error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void authenticateClient() throws IOException {
        System.out.print("=========> Enter Your Client ID: ");
        clientID = scanner.nextLine();
        System.out.println("================================>> Waiting for response from Server........");
        writer.println(clientID);

        String serverResponse = reader.readLine();
        if ("Accepted".equals(serverResponse)) {
            String ClientName=reader.readLine();
            serverRecipientID = reader.readLine();
            contactList = reader.readLine();
            System.out.println("\t---------------->(Verification Completed....)");
            System.out.println("\t===========|WELCOME MR "+ClientName);


        } else {
            System.out.println("Authentication failed. Closing connection.");
            socket.close();
            System.exit(0);
        }
    }

    private void startReaderThread() {
        try {
            while (isRunning) {
                String receivedMessage = reader.readLine();
                if (receivedMessage != null && !receivedMessage.equalsIgnoreCase("Over")) {
                    Message message = new Message(serverRecipientID, clientID, receivedMessage);
                    manager.addMessage(message);
                    System.out.println("\nNew message received: " + message);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("Error in reader thread: " + e.getMessage());
            }
        }
    }

    private void showMenu() {
        System.out.println("\n============== Menu ==============");
        System.out.println("1. Start Conversation");
        System.out.println("2. View Message History");
        System.out.println("3. Clear All Messages");
        System.out.println("4. Mark All Messages Read");
        System.out.println("5. Delete All Messages for Specific Contact");
        System.out.println("6. Display All Messages for a Specific Contact");
        System.out.println("7. Exit");
        System.out.println("==================================");
    }

    private boolean isValidMenuChoice(String choice) {
        try {
            int menuChoice = Integer.parseInt(choice);
            return menuChoice >= 1 && menuChoice <= 7;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void handleMenuChoice(String choice) {
        switch (choice) {
            case "1":
                startNewConversation();
                break;
            case "2":
                manager.displayAllMessages();
                break;
            case "3":
                System.out.println("Clearing all messages...");
                manager.clearAllMessages();
                break;
            case "4":
                System.out.println("Marking all messages as read...");
                manager.markMessagesAsRead();
                break;
            case "5":
                System.out.println("Deleting all messages for a specific contact...");
                System.out.print("Enter the contact ID to delete messages for: ");
                String contactID = scanner.nextLine();
                manager.deleteMessagesByContact(contactID);
                break;
            case "6":
                System.out.println("Displaying all messages for a specific contact...");
                System.out.print("Enter the contact ID to view messages: ");
                contactID = scanner.nextLine();
                manager.displayMessagesForContact(contactID);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }

    private void startNewConversation() {
        System.out.println("\t\t------------(Starting a new conversation...)------------");
        System.out.println(contactList);
        System.out.print("\t=========> Enter the Contact ID to start a conversation: ");
        String contactID = scanner.nextLine();
        if (serverRecipientID.equalsIgnoreCase(contactID)) {
            System.out.println("\t====>(Successfully started a new conversation.)<====");
            sendMessages(contactID);
        } else {
            System.out.println("!!!!!!!! (This Person is not currently available on the server.) !!!!!!!!");
        }
    }

    private void sendMessages(String contactID) {
        try {
            while (true) {
                System.out.print("--> Enter your message: ");
                String sentMessage = scanner.nextLine();
                writer.println(sentMessage);

                if ("over".equalsIgnoreCase(sentMessage)) {
                    System.out.println("======> (Ending Chat .......) <=======");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error while sending messages: " + e.getMessage());
        }
    }

    private void exitApplication() {
        System.out.println("Exiting the application...");
        isRunning = false; // Stop the reader thread
        closeConnection();
        System.exit(0);
    }

    private void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error while closing the socket: " + e.getMessage());
        }
        System.out.println("Connection closed.");
    }
}
