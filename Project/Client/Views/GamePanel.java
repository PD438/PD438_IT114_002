package Project.Client.Views;

import java.awt.CardLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import Project.Client.CardView;
import Project.Client.Client;
import Project.Client.ICardControls;
import Project.Client.IGameEvents;


import Project.Common.Constants;
import Project.Common.Phase;

public class GamePanel extends JPanel implements IGameEvents {
    private JPanel gridPanel;
    private CardLayout cardLayout;

    /**
     * @param controls
     */
    public GamePanel(ICardControls controls) {
        super(new CardLayout());
        cardLayout = (CardLayout) this.getLayout();
        this.setName(CardView.GAME_SCREEN.name());
        Client.INSTANCE.addCallback(this);

        createReadyPanel();
        gridPanel = new JPanel();
        gridPanel.setName("GRID");
        

        JButton rockButton = new JButton();
        rockButton.setText("Rock");
        rockButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("Rock");
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        });
        gridPanel.add(rockButton);
        this.add(gridPanel);

        JButton PaperButton = new JButton();
        PaperButton.setText("Paper");
        PaperButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("Paper");
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        });
        gridPanel.add(PaperButton);
        this.add(gridPanel);

        
        JButton scissorButton = new JButton();
        scissorButton.setText("Scissor");
        scissorButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("Scissor");
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        });
        gridPanel.add(scissorButton);
        this.add(gridPanel);
        

        JButton swordButton = new JButton();
        swordButton.setText("Sword");
        swordButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("Sword");
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        });
        gridPanel.add(swordButton);
        this.add(gridPanel);
        
        JButton shieldButton = new JButton();
        shieldButton.setText("Shield");
        shieldButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendTakeTurn("Shield");
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        });
        gridPanel.add(shieldButton);
        this.add(gridPanel);

        add("GRID", gridPanel);
        setVisible(false);
        // don't need to add this to ClientUI as this isn't a primary panel(it's nested
        // in ChatGamePanel)
        // controls.addPanel(Card.GAME_SCREEN.name(), this);
    }



    private void createReadyPanel() {
        JPanel readyPanel = new JPanel();
        JButton readyButton = new JButton();
        readyButton.setText("Ready");
        readyButton.addActionListener(l -> {
            try {
                Client.INSTANCE.sendReadyCheck();
            } catch (IOException e1) {

                e1.printStackTrace();
            }
        });
        readyPanel.add(readyButton);
        this.add(readyPanel);
    }

    private void resetView() {
        if (gridPanel == null) {
            return;
        }
        if (gridPanel.getLayout() != null) {
            gridPanel.setLayout(null);
        }

        gridPanel.removeAll();
        gridPanel.revalidate();
        gridPanel.repaint();
    }


    @Override
    public void onClientConnect(long id, String clientName, String message) {
    }

    @Override
    public void onClientDisconnect(long id, String clientName, String message) {
    }

    @Override
    public void onMessageReceive(long id, String message) {
    }

    @Override
    public void onReceiveClientId(long id) {
    }

    @Override
    public void onSyncClient(long id, String clientName) {
    }

    @Override
    public void onResetUserList() {
    }

    @Override
    public void onRoomJoin(String roomName) {
        if (Constants.LOBBY.equals(roomName)) {
            setVisible(false);// TODO along the way to hide game view when you leave
        }
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {

    }

    @Override
    public void onReceivePhase(Phase phase) {
        // I'll temporarily do next(), but there may be scenarios where the screen can
        // be inaccurate
        System.out.println("Received phase: " + phase.name());
        if (phase == Phase.READY) {
            if (!isVisible()) {
                setVisible(true);
                this.getParent().revalidate();
                this.getParent().repaint();
                System.out.println("GamePanel visible");
            } else {
                cardLayout.next(this);
                System.out.println("GamePanel Grid (hopefully)");
            }
        } 
        else if (phase == Phase.TURN) {
            cardLayout.show(this, "GRID");
        }
    }

    @Override
    public void onReceiveReady(long clientId) {
    }

    @Override
    public void onReceiveGrid(int rows, int columns) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onReceiveGrid'");
    }
}