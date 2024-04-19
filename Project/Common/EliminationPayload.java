package Project.Common;

public class EliminationPayload extends Payload {
    //pd438 4/29/2024
    private boolean isEliminated;

    public boolean isEliminated() {
        return isEliminated;
    }

    public void setElimination(boolean isEliminated){
        this.isEliminated = isEliminated;
    }

    public EliminationPayload() {
        setPayloadType(PayloadType.ELIMINATION);
    }
}
