package Module4.Part3HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Server {
    int port = 3001;
    // connected clients
    private List<ServerThread> clients = new ArrayList<>();
    // Map to store username to ServerThread mapping for private messaging
    private Map<String, ServerThread> usernameThreadMap = new HashMap<>();

    private void start(int port) {
        this.port = port;
        // server listening
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Socket incoming_client = null;
            System.out.println("Server is listening on port " + port);
            do {
                System.out.println("waiting for next client");
                if (incoming_client != null) {
                    System.out.println("Client connected");
                    ServerThread sClient = new ServerThread(incoming_client, this);

                    clients.add(sClient);
                    sClient.start();
                    incoming_client = null;

                }
            } while ((incoming_client = serverSocket.accept()) != null);
        } catch (IOException e) {
            System.err.println("Error accepting connection");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    protected synchronized void disconnect(ServerThread client) {
        long id = client.getId();
        client.disconnect();
        broadcast("Disconnected", id);
    }

    protected synchronized void broadcast(String message, long id) {
        if (processCommand(message, id)) {
            return;
        }
        // let's temporarily use the thread id as the client identifier to
        // show in all client's chat. This isn't good practice since it's subject to
        // change as clients connect/disconnect
        message = String.format("User[%d]: %s", id, message);
        // end temp identifier

        // loop over clients and send out the message
        Iterator<ServerThread> it = clients.iterator();
        while (it.hasNext()) {
            ServerThread client = it.next();
            boolean wasSuccessful = client.send(message);
            if (!wasSuccessful) {
                System.out.println(String.format("Removing disconnected client[%s] from list", client.getId()));
                it.remove();
                broadcast("Disconnected", id);
            }
        }
    }

    private boolean processCommand(String message, long clientId) {
        System.out.println("Checking command: " + message);
        if (message.equalsIgnoreCase("disconnect")) {
            Iterator<ServerThread> it = clients.iterator();
            while (it.hasNext()) {
                ServerThread client = it.next();
                if (client.getId() == clientId) {
                    it.remove();
                    disconnect(client);
                }
                break;
            }
        } else if (message.startsWith("@")) { // Check if it's a private message
            // Extract recipient's username pd438 02/19/2024 private message attempt
            int spaceIndex = message.indexOf(" ");
            if (spaceIndex != -1) {
                String recipientUsername = message.substring(1, spaceIndex); // Extract username
                String privateMessage = message.substring(spaceIndex + 1); // Extract message content

                ServerThread recipientThread = usernameThreadMap.get(recipientUsername);
                if (recipientThread != null) {
                    recipientThread.send("Private message from " + clientId + ": " + privateMessage);
                    return true;
                } else {
                    // If recipient not found, send a notification to the sender
                    ServerThread sender = findClientById(clientId);
                    if (sender != null) {
                        sender.send("User '" + recipientUsername + "' not found or offline.");
                    }
                }
            }
        }

        return false;
    }

    private ServerThread findClientById(long clientId) {
        for (ServerThread client : clients) {
            if (client.getId() == clientId) {
                return client;
            }
        }
        return null;
    }

  

    public static void main(String[] args) {
        System.out.println("Starting Server");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
