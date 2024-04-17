package Project.Server;

import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

public class ServerPlayer extends Player {
    private ServerThread client;
    private String choice; // Add this field to store player's choice

    public ServerPlayer(ServerThread t) {
        client = t;
        System.out.println(TextFX.colorize("Wrapped ServerThread " + t.getClientName(), Color.CYAN));
    }

    public long getClientId() {
        if (client == null) {
            return Constants.DEFAULT_CLIENT_ID;
        }
        return client.getClientId();
    }

    public String getClientName() {
        if (client == null) {
            return "";
        }
        return client.getClientName();
    }

    // Getter and setter for the choice field
    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public void sendPhase(Phase phase) {
        if (client == null) {
            return;
        }
        client.sendPhase(phase.name());
    }

    public void sendReadyState(long clientId, boolean isReady) {
        if (client == null) {
            return;
        }
        client.sendReadyState(clientId, isReady);
    }

    public void sendPlayerTurnStatus(long clientId, boolean didTakeTurn) {
        if (client == null) {
            return;
        }
        client.sendPlayerTurnStatus(clientId, didTakeTurn);
    }

    public void sendResetLocalTurns() {
        if (client == null) {
            return;
        }
        client.sendResetLocalTurns();
    }

    public void sendResetLocalReadyState() {
        if (client == null) {
            return;
        }
        client.sendResetLocalReadyState();
    }

    public void sendCurrentPlayerTurn(long clientId) {
        if (client == null) {
            return;
        }
        client.sendCurrentPlayerTurn(clientId);
    }
    //pd438 
    public synchronized void sendChoice(String playerChoice){
        if (client == null) {
            return;
        }
        
        
    }

    public void sendElimination(boolean isEliminated){
        if (client == null){
            return;
        }
        client.sendElimination(isEliminated);
    }
}