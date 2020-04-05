package student_player;

import Saboteur.SaboteurBoardState;
import Saboteur.SaboteurMove;
import boardgame.Board;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;

public class MCTSTree {
    private MCTSNode head;
    private SimulatedBoardState simulatedBoardState;
    private int scalingConstant = 2;

    public MCTSTree(ArrayList<MCTSNode> children, SimulatedBoardState simulatedBoardState) {
        head = new MCTSNode(children);
        this.simulatedBoardState = simulatedBoardState;
    }

    // this method performs a single execution of the MCTS algorithm
    public void performSimulation() {
        MCTSNode child = chooseChildToSimulate();
        int gameResult = simulateRandomGame(child.getSaboteurMove());
        updateMCTSTree(child.getSaboteurMove(), gameResult);
    }

    // this method finds the most suitable child to perform a game simulation on by the best QPlusValue
    public MCTSNode chooseChildToSimulate() {
        double bestQPlusValue = 0;
        double currentQPlusValue;
        MCTSNode child = null;

        for (MCTSNode node : head.getChildren()) {
            currentQPlusValue = calculateQPlusValue(node);
            if (currentQPlusValue > bestQPlusValue) {
                bestQPlusValue = currentQPlusValue;
                child = node;
            }
        }

        return child;
    }

    // this method calculates the qPlusValue by the Upper Confidence Trees formula
    public double calculateQPlusValue(MCTSNode child) {
        if (child.getTotalSimulations() == 0) {
            return 100; // makes sure to simulate children that have not been simulated yet
        } else {
            double expoitationValue;
            double explorationValue;

            expoitationValue = (double) child.getTotalVictories() / child.getTotalSimulations();
            explorationValue = scalingConstant * Math.log(head.getTotalSimulations()) / child.getTotalSimulations();

            return expoitationValue + explorationValue;
        }
    }

    public int simulateRandomGame(SaboteurMove moveChosen) { // returns 0 if tie, 1 if win, -1 if lost
        while(simulatedBoardState.getWinner() == Board.NOBODY) {
            SaboteurMove m = simulatedBoardState.getSimulatedMove();

            simulatedBoardState.processMove(m);
        }

        if (simulatedBoardState.getWinner() == 1) { // player 1 wins here, player 1 is the current player
            return 1;
        } else if (simulatedBoardState.getWinner() == 0) { // player 2 wins here, player 2 is the opponent
            return -1;
        } else { // draw
            return 0;
        }
    }

    // this method performs the backpropagation step of the MCTS algorithm
    public void updateMCTSTree(SaboteurMove moveChosen, int gameResult) {
        MCTSNode child = head.findChild(moveChosen);

        if (gameResult == 1) { // if the simulation game resulted in a victory
            head.incrementVictories();
            head.incrementSimulations();

            child.incrementVictories();
            child.incrementSimulations();
        } else { // if the simulation game resulted in a defeat or tie
            head.incrementSimulations();

            child.incrementSimulations();
        }
    }

    // this method is called at the end of all the simulations and returns the best move by totalSimulations
    public SaboteurMove getBestMove() {
        double best = 0;
        double current;
        SaboteurMove bestMove = null;

        System.out.println("!!!" + head.getTotalSimulations());


        for (MCTSNode node : head.getChildren()) {
            current = node.getTotalSimulations();
            if (current > best) {
                best = current;
                bestMove = node.getSaboteurMove();
            }
        }

        return bestMove;
    }
}
