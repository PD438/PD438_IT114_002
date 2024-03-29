package Project;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Room implements AutoCloseable {
    protected static Server server; // used to refer to accessible server functions
    private String name;
    private List<ServerThread> clients = new ArrayList<>();
    private boolean isRunning = false;
    private boolean isOpen = true; // Flag to indicate if the room is open for communication

    // Commands
    private final static String COMMAND_TRIGGER = "/";
    private final static String CREATE_ROOM = "createroom";
    private final static String JOIN_ROOM = "joinroom";
    private final static String DISCONNECT = "disconnect";
    private final static String LOGOUT = "logout";
    private final static String LOGOFF = "logoff";

    public Room(String name) {
        this.name = name;
        isRunning = true;
    }

    private void info(String message) {
        System.out.println(String.format("Room[%s]: %s", name, message));
    }

    public String getName() {
        return name;
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning || !isOpen) {
            return;
        }
        client.setCurrentRoom(this);
        if (clients.contains(client)) {
            info("Attempting to add a client that already exists");
        } else {
            clients.add(client);
            sendConnectionStatus(client, true);
        }
    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning || !isOpen) {
            return;
        }
        clients.remove(client);
        if (clients.size() > 0) {
            sendConnectionStatus(client, false);
        }
        checkClients();
    }

    private void checkClients() {
        if (!name.equalsIgnoreCase("lobby") && clients.size() == 0) {
            close();
        }
    }

    private boolean processCommands(String message, ServerThread client) {
        boolean wasCommand = false;
        try {
            if (message.startsWith(COMMAND_TRIGGER)) {
                String[] comm = message.split(COMMAND_TRIGGER);
                String part1 = comm[1];
                String[] comm2 = part1.split(" ");
                String command = comm2[0];
                String roomName;
                wasCommand = true;
                switch (command) {
                    case CREATE_ROOM:
                        roomName = comm2[1];
                        Room.createRoom(roomName, client);
                        break;
                    case JOIN_ROOM:
                        roomName = comm2[1];
                        Room.joinRoom(roomName, client);
                        break;
                    case DISCONNECT:
                    case LOGOUT:
                    case LOGOFF:
                        Room.disconnectClient(client, this);
                        break;
                    default:
                        wasCommand = false;
                        break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wasCommand;
    }

    protected static void createRoom(String roomName, ServerThread client) {
        if (server.createNewRoom(roomName)) {
            Room.joinRoom(roomName, client);
        } else {
            client.sendMessage("Server", String.format("Room %s already exists", roomName));
        }
    }

    protected static void joinRoom(String roomName, ServerThread client) {
        if (client.getCurrentRoom() != null) {
            client.getCurrentRoom().removeClient(client); // Remove from current room
        }
        Room room = server.getRoom(roomName); // This is where the error occurs
        if (room != null) {
            room.addClient(client);
        } else {
            client.sendMessage("Server", String.format("Room %s doesn't exist", roomName));
        }
    }

    protected static void disconnectClient(ServerThread client, Room room) {
        client.setCurrentRoom(null);
        client.disconnect();
        room.removeClient(client);
    }

    protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning || !isOpen) {
            return;
        }
        info("Sending message to " + clients.size() + " clients");
        if (sender != null && processCommands(message, sender)) {
            return;
        }

        String from = (sender == null ? "Room" : sender.getClientName());
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendMessage(from, message);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
    }

    protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected) {
        if (!isRunning || !isOpen) {
            return;
        }
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendConnectionStatus(sender.getClientName(), isConnected);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
    }

    private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
        iter.remove();
        info("Removed client " + client.getClientName());
        checkClients();
        sendMessage(null, client.getClientName() + " disconnected");
    }
	protected synchronized void broadcast(String message) {
		Iterator<ServerThread> iterator = clients.iterator();
		while (iterator.hasNext()) {
			ServerThread client = iterator.next();
			client.sendMessage(null, message);
		}
	}
    public void close() {
        server.removeRoom(this);
        server = null;
        isRunning = false;
        clients.clear();
        isOpen = false; // Set isOpen to false to indicate that the room is closed
    }
}
