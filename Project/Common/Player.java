package Project.Common;

/**
 * For chatroom projects, you can call this "User"
 */
public class Player {
    private boolean isReady;


    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    private boolean takenTurn;

    public boolean didTakeTurn() {
        return takenTurn;
    }

    public void setTakenTurn(boolean takenTurn) {
        this.takenTurn = takenTurn;
    }

    private boolean isMyTurn;

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public void setMyTurn(boolean isMyTurn) {
        this.isMyTurn = isMyTurn;
    }

    //pd438 Send choice to ServerThread 4/8/2024
    private String choice; 
    private boolean isEliminated;

    public String getChoice(){
        return choice;
    }

    public void setChoice(String choice){
        this.choice = choice;
    }

    public boolean getElimination(){
        return isEliminated;
    }

    public void setElimination(boolean isEliminated){
        this.isEliminated = isEliminated;
    }

}
