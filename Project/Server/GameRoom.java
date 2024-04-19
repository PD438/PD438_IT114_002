package Project.Server;

import java.util.ArrayList;
import java.util.Arrays;
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
        }
    }
//pd438 4/19/2024
    private void handleEndOfTurn() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        System.out.println(TextFX.colorize("Handling end of turn", Color.YELLOW));

        List<ServerPlayer> playerList = new ArrayList<>(players.values());
        ServerPlayer eliminatedPlayer = resolveGame(playerList);

        if (eliminatedPlayer != null) {
            removeClient(eliminatedPlayer.getServerThread());
            eliminatedPlayer.getServerThread().sendMessage(Constants.DEFAULT_CLIENT_ID, "You have been eliminated!");
            sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(eliminatedPlayer.getClientName() + " is eliminated!", Color.RED));
            if (players.size() == 1) {
                end();
                return;
            }
        }

        resetTurns();
        nextTurn();

        if (currentPhase != Phase.READY && players.size() > 2) {
            startTurnTimer();
        } else if (currentPhase != Phase.READY && players.size() == 2) {
            handleEndOfGame(); // Immediately resolve the game when there are only two players left
        }
    }
    //pd438 4/19/2024 This displays what happens when end of game occurs. 
    private void handleEndOfGame() {
        System.out.println(TextFX.colorize("Handling end of game", Color.YELLOW));

        // Resolve the game based on the choices of the two players
        List<ServerPlayer> playerList = new ArrayList<>(players.values());
        ServerPlayer eliminatedPlayer = resolveGame(playerList);

        if (eliminatedPlayer != null) {
            removeClient(eliminatedPlayer.getServerThread());
            eliminatedPlayer.getServerThread().sendMessage(Constants.DEFAULT_CLIENT_ID, "You have been eliminated!");
            sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(eliminatedPlayer.getClientName() + " is eliminated!", Color.RED));
            end(); // End the game
        }
    }
        
    private ServerPlayer resolveGame(List<ServerPlayer> players) {
        String[] choices = players.stream().map(ServerPlayer::getChoice).toArray(String[]::new);
        String[] uniqueChoices = Arrays.stream(choices).distinct().toArray(String[]::new);

        if (uniqueChoices.length == 1) {
            sendMessage(ServerConstants.FROM_ROOM, "It's a tie!");
            return null;
        }

        if (uniqueChoices.length == 3 || uniqueChoices.length == 1) {
            return null; // No elimination if everyone chose differently or if everyone chose the same
        }

        String choice1 = uniqueChoices[0];
        String choice2 = uniqueChoices[1];

        String winnerChoice = getWinnerChoice(choice1, choice2);

        ServerPlayer eliminatedPlayer = null;
        for (ServerPlayer player : players) {
            if (!player.getChoice().equalsIgnoreCase(winnerChoice)) {
                eliminatedPlayer = player;
                break;
            }
        }

        return eliminatedPlayer;
    }
        //pd438 4/19/2024
    private String getWinnerChoice(String choice1, String choice2) {
        if (choice1 == null || choice2 == null) {
            // Handle null choices here, return a default winner or null
            return null;
        }

        if (choice1.equalsIgnoreCase("rock") && choice2.equalsIgnoreCase("scissors")) {
            return "rock";
        } else if (choice1.equalsIgnoreCase("scissors") && choice2.equalsIgnoreCase("paper")) {
            return "scissors";
        } else if (choice1.equalsIgnoreCase("paper") && choice2.equalsIgnoreCase("rock")) {
            return "paper";
        } else {
            // If not explicitly defined, second choice wins
            return choice2;
        }
    }

    private void resetTurns() {
        players.values().forEach(p -> p.setTakenTurn(false));
        sendResetLocalTurns();
    }

    private void end() {
        System.out.println(TextFX.colorize("Doing game over", Color.YELLOW));
        turnOrder.clear();
        players.clear();
        changePhase(Phase.READY);
        sendMessage(ServerConstants.FROM_ROOM, "You Win!!! Game over! Start a new game by setting up players and issuing ready checks.");
    }

    private void sendCurrentPlayerTurn() {
        Iterator<ServerPlayer> iter = players.values().iterator();
        while (iter.hasNext()) {
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
