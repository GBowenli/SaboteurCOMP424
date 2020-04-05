package student_player;

import Saboteur.SaboteurMove;
import Saboteur.cardClasses.SaboteurCard;
import Saboteur.cardClasses.SaboteurTile;
import boardgame.Move;

import Saboteur.SaboteurPlayer;
import Saboteur.SaboteurBoardState;

import java.util.ArrayList;
import java.util.Random;

/** A player file submitted by a student. */
public class StudentPlayer extends SaboteurPlayer {
    private boolean firstExecution = true;

    public static final int originPos = 5;
    public static final int[][] hiddenPos = {{originPos+7,originPos-2},{originPos+7,originPos},{originPos+7,originPos+2}};

    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260787692");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(SaboteurBoardState boardState) {
        long startTimeMillis = System.currentTimeMillis();
        ArrayList<SaboteurMove> allLegalMoves = boardState.getAllLegalMoves();

        // copy all information from the boardState
        ArrayList<SaboteurCard> myHand = new ArrayList<>(boardState.getCurrentPlayerCards());
        SaboteurTile[][] board = new SaboteurTile[14][14];
        for(int i=0; i<board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                board[i][j] = boardState.getHiddenBoard()[i][j];
            }
        }
        int[][] intBoard = new int[42][42];
        for (int i = 0; i < intBoard.length; i++) {
            for (int j = 0; j < intBoard[0].length; j++) {
                intBoard[i][j] = boardState.getHiddenIntBoard()[i][j];
            }
        }

        int playerNum = boardState.getTurnPlayer();
        int player1Malus = boardState.getNbMalus(playerNum);
        int player2Malus = boardState.getNbMalus(1 - playerNum);

        boolean [] player1hiddenRevealed = new boolean[3];

        for(int h=0;h<3;h++){ // set player1hiddenRevealed based on whether the tiles at hiddenPos are "8" or not
            if (board[hiddenPos[h][0]][hiddenPos[h][1]].getName().equals("Tile:8")) {
                player1hiddenRevealed[h] = false;
            } else {
                player1hiddenRevealed[h] = true;
            }
        }

        // randomize player2hideenRealed to have fair simulations where player 2 can have randomized hidden revealed values
        boolean[] player2hiddenRevealed = new boolean[3];
        Random random = new Random();
        int randomValue;
        for (int i = 0; i < 3; i++) {
            randomValue = random.nextInt(2);
            if (randomValue == 0) {
                player2hiddenRevealed[i] = true;
            } else { // equals 1
                player2hiddenRevealed[i] = false;
            }
        }

        // set hiddenCards to the cards we know, the rest of them randomize them
        ArrayList<String> list =new ArrayList<>();
        list.add("hidden1");
        list.add("hidden2");
        list.add("nugget");

        SaboteurTile[] hiddenCards = new SaboteurTile[3];
        for (int i = 0; i < 3; i++) {
            if (player1hiddenRevealed[i]) {
                hiddenCards[i] = board[hiddenPos[i][0]][hiddenPos[i][1]];
                list.remove(board[hiddenPos[i][0]][hiddenPos[i][1]].getName().substring(5));
            }
        }

        for (int i = 0; i < 3; i++) {
            int index = random.nextInt(list.size());
            if (hiddenCards[i] == null) {
                hiddenCards[i] = new SaboteurTile(list.remove(index));
            }
        }

        SimulatedBoardState simulatedBoardState = new SimulatedBoardState(board, intBoard, myHand, player1Malus, player2Malus, player1hiddenRevealed, player2hiddenRevealed, hiddenCards);

        ArrayList<MCTSNode> possibleMovesChildren = new ArrayList<>();

        for (SaboteurMove move : allLegalMoves) {
            possibleMovesChildren.add(new MCTSNode(move));
        }

        MCTSTree tree = new MCTSTree(possibleMovesChildren, simulatedBoardState);
        //if (firstExecution) {
        //    while (System.currentTimeMillis() - startTimeMillis < 29998) { // 30 seconds

        //    }
         //   firstExecution = false;
        //} else {
            //while (System.currentTimeMillis() - startTimeMillis < 1998) { // 2 seconds

            //}
        //}

        while (System.currentTimeMillis() - startTimeMillis < 1999) {
            tree.performSimulation();
        }

        Move myMove = tree.getBestMove();

        // Return your move to be processed by the server.
        return myMove;
    }
}