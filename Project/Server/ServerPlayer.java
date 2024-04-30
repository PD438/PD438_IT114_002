package Project.Server;

import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

public class ServerPlayer extends Player {
    private ServerThread serverThread;
    private String choice; // Add this field to store player's choice
//pd438 4/19/2024
    public ServerPlayer(ServerThread t) {
        serverThread = t;
        System.out.println(TextFX.colorize("Wrapped ServerThread " + t.getClientName(), Color.CYAN));
    }

    public long getClientId() {
        if (serverThread == null) {
            return Constants.DEFAULT_CLIENT_ID;
        }
        return serverThread.getClientId();
    }

    public String getClientName() {
        if (serverThread == null) {
            return "";
        }
        return serverThread.getClientName();
    }

    // Getter and setter for the choice field
    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public void sendPhase(Phase phase) {
        if (serverThread == null) {
            return;
        }
        serverThread.sendPhase(phase.name());
    }

    public void sendReadyState(long clientId, boolean isReady) {
        if (serverThread == null) {
            return;
        }
        serverThread.sendReadyState(clientId, isReady);
    }

    public void sendPlayerTurnStatus(long clientId, boolean didTakeTurn) {
        if (serverThread == null) {
            return;
        }
        serverThread.sendPlayerTurnStatus(clientId, didTakeTurn);
    }

    public void sendResetLocalTurns() {
        if (serverThread == null) {
            return;
        }
        serverThread.sendResetLocalTurns();
    }

    public void sendResetLocalReadyState() {
        if (serverThread == null) {
            return;
        }
        serverThread.sendResetLocalReadyState();
    }

    public void sendCurrentPlayerTurn(long clientId) {
        if (serverThread == null) {
            return;
        }
        serverThread.sendCurrentPlayerTurn(clientId);
    }

    public synchronized void sendChoice(String playerChoice){
        if (serverThread == null) {
            return;
        }
        serverThread.sendChoice(playerChoice);
    }

    public void sendElimination(boolean isEliminated){
        if (serverThread == null){
            return;
        }
        serverThread.sendElimination(isEliminated);
    }

    public ServerThread getServerThread() {
        return serverThread;
    }
}
