package Project.Common;

/**
 * For chatroom projects, you can call this "User"
 */
public class Player {
    private boolean isReady;
    private String previousChoice;

    public boolean isReady() {
        return isReady;
    }

    //pd438 04/30/2024
    public String getpreviousChoice(){
        return previousChoice;
    }

    public void setpreviousChoice(String previousChoice){
        this.previousChoice = previousChoice;
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
