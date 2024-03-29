package Project;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket client;
    private String clientName;
    private boolean isRunning = false;
    private ObjectOutputStream out;
    private Room currentRoom;

    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", getClientName(), message));
    }

    public ServerThread(Socket myClient, Room room) {
        info("Thread created");
        this.client = myClient;
        this.currentRoom = room;
    }

    protected void setClientName(String name) {
        if (name == null || name.isBlank()) {
            System.err.println("Invalid client name being set");
            return;
        }
        clientName = name;
    }

    protected String getClientName() {
        return clientName;
    }

    protected synchronized Room getCurrentRoom() {
        return currentRoom;
    }

    protected synchronized void setCurrentRoom(Room room) {
        if (room != null) {
            currentRoom = room;
        } else {
            info("Passed in room was null, this shouldn't happen");
        }
    }

    public void disconnect() {
        info("Thread being disconnected by server");
        isRunning = false;
        cleanup();
    }

    public boolean sendMessage(String from, String message) {
        try {
            Payload payload = new Payload();
            payload.setPayloadType(PayloadType.MESSAGE);
            payload.setClientName(from);
            payload.setMessage(message);
            out.writeObject(payload);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendConnectionStatus(String clientName, boolean isConnected) {
        try {
            Payload payload = new Payload();
            payload.setPayloadType(isConnected ? PayloadType.CONNECT : PayloadType.DISCONNECT);
            payload.setClientName(clientName);
            payload.setMessage(isConnected ? "connected" : "disconnected");
            out.writeObject(payload);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {
            this.out = out;
            isRunning = true;
            Payload fromClient;
            while (isRunning && (fromClient = (Payload) in.readObject()) != null) {
                info("Received from client: " + fromClient);
                processMessage(fromClient);
            }
        } catch (Exception e) {
            e.printStackTrace();
            info("Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection");
            cleanup();
        }
    }

    void processMessage(Payload p) {
        switch (p.getPayloadType()) {
            case CONNECT:
                setClientName(p.getClientName());
                break;
            case DISCONNECT:
                break;
            case MESSAGE:
                if (currentRoom != null) {
                    if (p.getMessage().equalsIgnoreCase("start game")) {
                        int result = flipCoin();
                        String outcome = (result == 0) ? "Heads" : "Tails";
                        currentRoom.broadcast("Coin flip result: " + outcome);
                    } else {
                        currentRoom.sendMessage(this, p.getMessage());
                    }
                } else {
                    Room.joinRoom("lobby", this);
                }
                break;
            default:
                break;
        }
    }

    private int flipCoin() {
        return (int) (Math.random() * 2); // 0 for heads, 1 for tails
    }

    private void cleanup() {
        info("Thread cleanup() start");
        try {
            client.close();
        } catch (IOException e) {
            info("Client already closed");
        }
        info("Thread cleanup() complete");
    }
}
