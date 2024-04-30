package Project.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.TextFX;
import Project.Common.TimedEvent;
import Project.Common.TextFX.Color;

public class GameRoom extends Room {

    private ConcurrentHashMap<Long, ServerPlayer> players = new ConcurrentHashMap<>();
    private TimedEvent readyCheckTimer = null;
    private TimedEvent turnTimer = null;
    private Phase currentPhase = Phase.READY;
    private ServerPlayer currentPlayer = null;
    private List<Long> turnOrder = new ArrayList<>();

    public GameRoom(String name) {
        super(name);
    }

    @Override
    protected synchronized void addClient(ServerThread client) {
        super.addClient(client);
        if (!players.containsKey(client.getClientId())) {
            ServerPlayer sp = new ServerPlayer(client);
            players.put(client.getClientId(), sp);
            System.out.println(TextFX.colorize(client.getClientName() + " join GameRoom " + getName(), Color.WHITE));

            sp.sendPhase(currentPhase);

            players.values().forEach(p -> {
                sp.sendReadyState(p.getClientId(), p.isReady());
                sp.sendPlayerTurnStatus(p.getClientId(), p.didTakeTurn());
            });
            if (currentPlayer != null) {
                sp.sendCurrentPlayerTurn(currentPlayer.getClientId());
            }
        }
    }

    @Override
    protected synchronized void removeClient(ServerThread client) {
        super.removeClient(client);
        if (players.containsKey(client.getClientId())) {
            players.remove(client.getClientId());
            System.out.println(TextFX.colorize(client.getClientName() + " left GameRoom " + getName(), Color.WHITE));
        }
    }

    public synchronized void setReady(ServerThread client) {
        if (currentPhase != Phase.READY) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Can't initiate ready check at this time");
            return;
        }
        long playerId = client.getClientId();
        if (players.containsKey(playerId)) {
            players.get(playerId).setReady(true);
            syncReadyState(players.get(playerId));
            System.out.println(TextFX.colorize(players.get(playerId).getClientName() + " marked themselves as ready ", Color.YELLOW));
            readyCheck();
        } else {
            System.err.println(TextFX.colorize("Player doesn't exist: " + client.getClientName(), Color.RED));
        }
    }

    public synchronized void doTurn(ServerThread client, String choice) {
        if (currentPhase != Phase.TURN) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, "You can't do turns just yet");
            return;
        }

        long clientId = client.getClientId();
        if (players.containsKey(clientId)) {
            ServerPlayer sp = players.get(clientId);
            if (!sp.isReady()) {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Sorry, you weren't ready in time and can't participate");
                return;
            }
            if (!sp.didTakeTurn()) {
                sp.setChoice(choice);
                sp.sendChoice(choice);
                sp.setTakenTurn(true);
                sendMessage(ServerConstants.FROM_ROOM, String.format("%s completed their turn", sp.getClientName()));
                syncUserTookTurn(sp);
                if (currentPlayer != null && currentPlayer.didTakeTurn()) {
                    handleEndOfTurn();
                    nextTurn();
                }
            } else {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "You already completed your turn, please wait");
            }
        }
    }
        //pd438 4/19/2024 Displays timer when user hits ready for the first time.
    private synchronized void readyCheck() {
        if (readyCheckTimer == null) {
            readyCheckTimer = new TimedEvent(30, () -> {
                long numReady = players.values().stream().filter(Player::isReady).count();
                boolean meetsMinimum = numReady >= Constants.MINIMUM_REQUIRED_TO_START;
                int totalPlayers = players.size();
                boolean everyoneIsReady = numReady >= totalPlayers;
                if (meetsMinimum || (everyoneIsReady && meetsMinimum)) {
                    start();
                } else {
                    sendMessage(ServerConstants.FROM_ROOM, "Minimum players not met during ready check, please try again");
                    players.values().forEach(p -> {
                        p.setReady(false);
                        syncReadyState(p);
                    });
                }
                readyCheckTimer.cancel();
                readyCheckTimer = null;
            });
            readyCheckTimer.setTickCallback((time) -> System.out.println("Ready Countdown: " + time));
        }

        long numReady = players.values().stream().filter(Player::isReady).count();
        int totalPlayers = players.size();
        boolean everyoneIsReady = numReady >= totalPlayers;
        if (everyoneIsReady) {
            if (readyCheckTimer != null) {
                readyCheckTimer.cancel();
                readyCheckTimer = null;
            }
            System.out.println(TextFX.colorize("Everyone is Here!! Lets play", Color.GREEN));
            start();
        }
    }

    private void start() {
        if (currentPhase != Phase.READY) {
            System.err.println("Invalid phase called during start()");
            return;
        }
        changePhase(Phase.TURN);
        setupTurns();
        startTurnTimer();
    }
        // 4/19/2024 pd438 displays randomized turn
    private void setupTurns() {
        turnOrder = new ArrayList<>(players.keySet());
        Collections.shuffle(turnOrder);
        currentPlayer = players.get(turnOrder.get(0));
        System.out.println(TextFX.colorize("First person is " + currentPlayer.getClientName(), Color.YELLOW));
        sendCurrentPlayerTurn();
    }

    private void nextTurn() {
        int index = turnOrder.indexOf(currentPlayer.getClientId());
        index = (index + 1) % turnOrder.size();
        currentPlayer = players.get(turnOrder.get(index));
        System.out.println(TextFX.colorize("Next person is " + currentPlayer.getClientName(), Color.YELLOW));
        sendCurrentPlayerTurn();
    }
        //pd438 4/19/2024 This displays the timer for the person to let them know how much time they have left. 
    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        if (turnTimer == null) {
            turnTimer = new TimedEvent(30, this::handleEndOfTurn);
            turnTimer.setTickCallback(this::checkEarlyEndTurn);
            sendMessage(ServerConstants.FROM_ROOM, "Pick your actions");
        }
    }

    private void checkEarlyEndTurn(int timeRemaining) {
        if (currentPlayer != null && currentPlayer.didTakeTurn()) {
            handleEndOfTurn();
            nextTurn();
        }
    }
//pd438 4/19/2024
/**
 * 
 */
private void handleEndOfTurn() {
    if (turnTimer != null) {
        turnTimer.cancel();
        turnTimer = null;
    }
    System.out.println(TextFX.colorize("Handling end of turn", Color.YELLOW));
    // option 1 - if they can only do a turn when ready
    final List<ServerPlayer> playersToProcess = players.values().stream().filter(p-> p.didTakeTurn() && p.getElimination()== false && p.getChoice() != null).toList();
    // option 2 - double check they are ready and took a turn
    // List<ServerPlayer> playersToProcess =
    // players.values().stream().filter(sp->sp.isReady() &&
    // sp.didTakeTurn()).toList();
    playersToProcess.forEach(p -> {
        sendMessage(ServerConstants.FROM_ROOM, String.format("%s did something for the game", p.getClientName()));
        
    });
    // TODO end game logic
    //pd438 
    
    for(int i = 0; i<playersToProcess.size(); i++) {
        int nextPlayer = i+1; 
        if(nextPlayer>= playersToProcess.size()) {nextPlayer= 0;}
        ServerPlayer p1= playersToProcess.get(i); 
        ServerPlayer p2= playersToProcess.get(nextPlayer);
        
        
        //pd438 4/30/2025
        //to display for debugging issues (Rock loses to Paper,Paper loses to Scissor, and Scissor Loses to Paper)
        if(p1.getChoice().equals("Rock") && p2.getChoice().equals("Scissor")){
            p2.sendElimination(true);
            p2.setElimination(true);
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p1.getClientName() + "Eliminates"+ p2.getClientName(), Color.YELLOW));
        } else if (p1.getChoice().equals("Paper") && p2.getChoice().equals("Rock")) {
            p2.sendElimination(true);
            p2.setElimination(true);
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p1.getClientName() + "Eliminates" + p2.getClientName(), Color.YELLOW));
        } else if(p1.getChoice().equals("Scissor") && p2.getChoice().equals("Paper")) {
            p2.sendElimination(true);
            p2.setElimination(true);
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p1.getClientName() + "Eliminates"+ p2.getClientName(), Color.YELLOW));
        } 
            else if (p1.getChoice().equals("Sword")&& p2.getChoice().equals("Shield")) {
                p2.sendElimination(true);
                p2.setElimination(true);
                sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p1.getClientName() + "Eliminates" + p2.getClientName(),Color.YELLOW));
            }
        else if(p2.getElimination() != true) { p2.sendElimination(false);
        p2.setElimination(false);
            
        }
        if(p2.getChoice().equals("Paper") && p1.getChoice().equals("Rock")){
            p1.sendElimination(true);
            p1.setElimination(true);
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p2.getClientName() + "Eliminates"+ p1.getClientName(), Color.YELLOW));
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize("Paper Beats Rock!!!" ,Color.CYAN));
        } else if (p2.getChoice().equals("Scissor") && p1.getChoice().equals("Paper")) {
            p1.sendElimination(true);
            p1.setElimination(true);
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p2.getClientName() + "Eliminates" + p1.getClientName(), Color.YELLOW));
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize("Scissor Beats Paper!!!" ,Color.CYAN));
        } else if(p2.getChoice().equals("Rock") && p1.getChoice().equals("Scissor")) {
            p1.sendElimination(true);
            p1.setElimination(true);
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p2.getClientName() + "Eliminates"+ p1.getClientName(), Color.YELLOW));
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize("Rock Beats Shield!!!" ,Color.CYAN));
        }else if (p2.getChoice().equals("Sword")&& p1.getChoice().equals("Shield")) {
            p1.sendElimination(true);
            p1.setElimination(true);
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(p2.getClientName() + "Eliminates" + p1.getClientName(),Color.YELLOW));
            sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize("Sword Beats Shield!!!" ,Color.CYAN));
        } 
        else if (p1.getElimination() != true) {
            p1.sendElimination(false);
            p1.setElimination(false);
        }
        
        //to display for debugging
        
        }
        List<ServerPlayer> processplayersNotEliminated = players.values().stream().filter(p-> p.getElimination()== false).toList();
        int playersnotEliminated = processplayersNotEliminated.size();
        sendMessage(ServerConstants.FROM_ROOM, playersnotEliminated+ " Players Left");
        //More than 1 player remains
        if(playersnotEliminated>1)
        {
            for(int i = 0; i<processplayersNotEliminated.size(); i++){
                ServerPlayer playersRemaining = processplayersNotEliminated.get(i);
                sendMessage(ServerConstants.FROM_ROOM,TextFX.colorize(playersRemaining.getClientName()+ ",", Color.BLACK));
            }
            sendMessage(ServerConstants.FROM_ROOM, "There are more than 2 left");
            resetTurns();
        }
        else if (playersnotEliminated == 1) {
             ServerPlayer playerThatWon = processplayersNotEliminated.get(0);
            sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(playerThatWon.getClientName()+"  We have a Winner!!!", Color.CYAN));
            end();
        }
        else if(playersnotEliminated == 0){
            sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(" We have a Tie!!!", Color.RED));
            end();
        }
            
        }



    private void resetTurns() {
        players.values().forEach(p -> p.setTakenTurn(false));
        players.values().forEach(p -> p.setChoice(null));
        sendResetLocalTurns();
        changePhase(Phase.READY);
        sendMessage(ServerConstants.FROM_ROOM, "Show Goes On!! New Round Starting");
        start();
    }

    private void end() {
        System.out.println(TextFX.colorize("Doing game over", Color.YELLOW));
        turnOrder.clear();
        players.values().forEach(p -> {
            p.setReady(false);
            p.setTakenTurn(false);
            p.setElimination(false);
            p.setChoice(null);

        });
        //depending if this is not called yet, we can clear this state
        sendResetLocalTurns();
        sendResetLocalReadyState();
        changePhase(Phase.READY);
        sendMessage(ServerConstants.FROM_ROOM,  "Game over!");
    }

    private void sendCurrentPlayerTurn() {
        Iterator<ServerPlayer> iter = players.values().iterator();
        while (iter.hasNext()) {
            ServerPlayer sp = iter.next();
            sp.sendCurrentPlayerTurn(currentPlayer == null ? Constants.DEFAULT_CLIENT_ID : currentPlayer.getClientId());
        }
    }
    private void sendResetLocalReadyState(){
        Iterator<ServerPlayer> iter = players.values().iterator();
        while(iter.hasNext()) {
            ServerPlayer sp = iter.next();
            sp.sendCurrentPlayerTurn(currentPlayer == null ? Constants.DEFAULT_CLIENT_ID : currentPlayer.getClientId());
        }
    }
    private void sendResetLocalTurns() {
        players.values().forEach(ServerPlayer::sendResetLocalTurns);
    }

    private void syncUserTookTurn(ServerPlayer isp) {
        players.values().forEach(sp -> sp.sendPlayerTurnStatus(isp.getClientId(), isp.didTakeTurn()));
    }

    private void changePhase(Phase incomingChange) {
        if (currentPhase != incomingChange) {
            currentPhase = incomingChange;
            syncCurrentPhase();
        }
    }

    private void syncCurrentPhase() {
        players.values().forEach(t -> t.sendPhase(currentPhase));
    }

    private void syncReadyState(ServerPlayer csp) {
        players.values().forEach(sp -> sp.sendReadyState(csp.getClientId(), csp.isReady()));
    }
}
