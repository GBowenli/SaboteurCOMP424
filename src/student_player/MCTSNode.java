package student_player;

import Saboteur.SaboteurMove;

import java.util.ArrayList;

public class MCTSNode {
    private SaboteurMove saboteurMove;
    private int totalVictories;
    private int totalSimulations;
    private ArrayList<MCTSNode> children;

    public MCTSNode(SaboteurMove saboteurMove, ArrayList<MCTSNode> children) {
        this.saboteurMove = saboteurMove;
        this.children = children;
    }

    public MCTSNode(ArrayList<MCTSNode> children) { // constructor for head
        this.saboteurMove = null;
        this.totalVictories = 1;
        this.totalSimulations = 1;
        this.children = children;
    }

    public MCTSNode(SaboteurMove saboteurMove) { // constructor for child
        this.saboteurMove = saboteurMove;
        this.totalVictories = 0;
        this.totalSimulations = 0;
        this.children = null;
    }

    public MCTSNode findChild(SaboteurMove move) { // returns child with the SaboTeurMove move
        for (MCTSNode node : children) {
            if (node.getSaboteurMove() == move) {
                return node;
            }
        }
        return null;
    }

    public SaboteurMove getSaboteurMove() {
        return saboteurMove;
    }

    public int getTotalVictories() {
        return totalVictories;
    }

    public int getTotalSimulations() {
        return totalSimulations;
    }

    public ArrayList<MCTSNode> getChildren() {
        return children;
    }

    public void incrementVictories() {
        totalVictories++;
    }

    public void incrementSimulations() {
        totalSimulations++;
    }
}
