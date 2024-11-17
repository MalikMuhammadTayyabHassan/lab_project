import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final List<ServerHandlerThread2> connectedClients = new CopyOnWriteArrayList<>(); // Thread-safe list
    private static final List<Contact> contacts = new ArrayList<>(List.of(
            new Contact("Contact1", "Alice"),
            new Contact("Contact2", "Bob"),
            new Contact("Contact3", "Charlie"),
            new Contact("Contact4", "David")
    ));
    private static MessageManager manager = new MessageManager(); // Shared message manager
    private static Scanner scanner = new Scanner(System.in);
    private static boolean isRunning = true;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(5555);
            System.out.println("Server is running and waiting for connections...");

            System.out.print("Enter server ID for authorization: ");
            String serverID = scanner.nextLine();
            if (getContactByID(serverID) == null) {
                System.out.println("Invalid server ID. Exiting...");
                return;
            }

            Thread menuThread = new Thread(() -> showServerMenu(serverID));
            menuThread.start();

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                ServerHandlerThread2 clientHandler = new ServerHandlerThread2(clientSocket, serverID, contacts, manager);
                connectedClients.add(clientHandler);
                new Thread(clientHandler).start();
                System.out.println("New client connected.");
            }
        } catch (IOException e) {
            System.out.println("Error starting the server: " + e.getMessage());
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    private static void showServerMenu(String serverID) {
        while (isRunning) {
            System.out.println("\n======= Server Menu =======");
            System.out.println("1. Send a message to a client");
            System.out.println("2. View message history");
            System.out.println("3. List connected clients");
            System.out.println("4. Clear All Messages");
            System.out.println("5. Mark All Messages Read");
            System.out.println("6. Delete All Messages for Specific Contact");
            System.out.println("7. Display All Messages for a Specific Contact");
            System.out.println("8. Exit");
            System.out.println("===========================");
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine();
            handleServerMenuChoice(choice, serverID);
        }
    }

    private static void handleServerMenuChoice(String choice, String serverID) {
        switch (choice) {
            case "1" -> sendMessageToClient(serverID);
            case "2" -> manager.displayAllMessages();
            case "3" -> listConnectedClients();
            case "4" -> {
                System.out.println("Clearing all messages...");
                manager.clearAllMessages();
            }
            case "5" -> {
                System.out.println("Marking all messages as read...");
                manager.markMessagesAsRead();
            }
            case "6" -> {
                System.out.println("Deleting all messages for a specific contact...");
                System.out.print("Enter the contact ID to delete messages for: ");
                String contactID = scanner.nextLine();
                manager.deleteMessagesByContact(contactID);
            }
            case "7" -> {
                System.out.println("Displaying all messages for a specific contact...");
                System.out.print("Enter the contact ID to view messages: ");
                String contactID = scanner.nextLine();
                manager.displayMessagesForContact(contactID);
            }
            case "8" -> {
                System.out.println("Exiting server menu...");
                isRunning = false;
            }
            default -> System.out.println("Invalid choice. Try again.");
        }
    }

    private static void sendMessageToClient(String serverID) {
        try {
            listConnectedClients();
            System.out.print("Enter the client ID to send a message: ");
            String targetClientID = scanner.nextLine();
            ServerHandlerThread2 targetClient = getClientByID(targetClientID);

            if (targetClient == null) {
                System.out.println("Client not found.");
                return;
            }

            String sentMessage = "";
            while (sentMessage != null && !sentMessage.equalsIgnoreCase("over")) {
                System.out.print("Enter message to send: ");
                sentMessage = scanner.nextLine();
                targetClient.sendMessage(sentMessage);
            }

        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    private static void listConnectedClients() {
        System.out.println("\nConnected Clients:");
        synchronized (connectedClients) {
            for (int i = 0; i < connectedClients.size(); i++) {
                ServerHandlerThread2 client = connectedClients.get(i);
                System.out.println((i + 1) + ". " + client.getClientID());
            }
        }
    }

    private static Contact getContactByID(String contactID) {
        for (Contact contact : contacts) {
            if (contact.getContactID().equalsIgnoreCase(contactID)) {
                return contact;
            }
        }
        return null;
    }

    private static ServerHandlerThread2 getClientByID(String clientID) {
        synchronized (connectedClients) {
            for (ServerHandlerThread2 client : connectedClients) {
                if (client.getClientID().equalsIgnoreCase(clientID)) {
                    return client;
                }
            }
        }
        return null;
    }
}

class ServerHandlerThread2 extends Thread {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String serverID;
    private String clientID;
    private List<Contact> contacts;
    private MessageManager manager;
    private boolean isRunning = true;

    public ServerHandlerThread2(Socket socket, String serverID, List<Contact> contacts, MessageManager manager) {
        this.socket = socket;
        this.serverID = serverID;
        this.contacts = contacts;
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            authenticateClient();
            readMessagesFromClient();
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            cleanupResources();
        }
    }

    private void authenticateClient() throws IOException {
        clientID = reader.readLine();
        if (getContactByID(clientID) == null) {
            writer.println("Invalid Client ID. Disconnecting...");
            socket.close();
            return;
        }

        writer.println("Accepted");
        writer.println(getContactByID(clientID).getContactName());
        writer.println(serverID);
        writer.println(contacts.toString());
    }

    private void readMessagesFromClient() {
        try {
            String message;
            while (isRunning && (message = reader.readLine()) != null && !message.equalsIgnoreCase("over")) {
                if (message.equalsIgnoreCase("over")) {
                    System.out.println("\t-------| Client ended the conversation !!!!!");
                    break;
                }
                System.out.println("\n\t\t=====>Message from client [" + clientID + "]: " + message);
                manager.addMessage(new Message(clientID, serverID, message));
            }
        } catch (IOException e) {
            if (isRunning) {
                System.out.println("Error reading message from client: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    private Contact getContactByID(String contactID) {
        for (Contact contact : contacts) {
            if (contact.getContactID().equalsIgnoreCase(contactID)) {
                return contact;
            }
        }
        return null;
    }

    private void cleanupResources() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        } catch (IOException e) {
            System.out.println("Error closing resources: " + e.getMessage());
        }
    }

    public String getClientID() {
        return clientID;
    }
}
