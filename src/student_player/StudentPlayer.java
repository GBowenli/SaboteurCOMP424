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
        ArrayList<SaboteurMove> allLegalMoves = boardState.getAllLegalMoves();

        // copy all information from the boardState
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

        int playerMalus = boardState.getNbMalus(boardState.getTurnPlayer());

        boolean [] playerHiddenRevealed = new boolean[3];

        for(int h=0;h<3;h++){ // set player1hiddenRevealed based on whether the tiles at hiddenPos are "8" or not
            if (board[hiddenPos[h][0]][hiddenPos[h][1]].getName().equals("Tile:8")) {
                playerHiddenRevealed[h] = false;
            } else {
                playerHiddenRevealed[h] = true;
            }
        }

        // set hiddenCards to the cards we know, the rest of them randomize them
        ArrayList<String> list =new ArrayList<>();
        list.add("hidden1");
        list.add("hidden2");
        list.add("nugget");

        SaboteurTile[] hiddenCards = new SaboteurTile[3];
        boolean knowNuggetLocation = false;

        for (int i = 0; i < 3; i++) {
            if (playerHiddenRevealed[i]) {
                if (board[hiddenPos[i][0]][hiddenPos[i][1]].getName().substring(5).equals("nugget")) {
                    knowNuggetLocation = true;
                    hiddenCards[i] = new SaboteurTile("nugget");
                }
            }
        }

        boolean firstHiddenSet = false;
        if (knowNuggetLocation) {
            for (int i = 0; i < 3; i++) {
                if (hiddenCards[i] == null) {
                    if (!firstHiddenSet) {
                        firstHiddenSet = true;
                        hiddenCards[i] = new SaboteurTile("hidden1");
                    } else {
                        hiddenCards[i] = new SaboteurTile("hidden2");
                    }
                }
            }
        } else {
            hiddenCards = new SaboteurTile[]{new SaboteurTile("hidden1"), new SaboteurTile("nugget"), new SaboteurTile("hidden2")};
        }

        SimulatedBoard simulatedBoard = new SimulatedBoard(board, intBoard, playerMalus, playerHiddenRevealed, hiddenCards, allLegalMoves);

        //Move myMove = simulatedBoard.getIdealMove();
        System.out.println("!!!" + simulatedBoard.getAllLeaves().size());
        for (TileNodeBFS node : simulatedBoard.getAllLeaves()) {
            System.out.println("!!!" + node.getPosition()[0] + "???" + node.getPosition()[1]);
        }


        Move myMove = boardState.getRandomMove();

        // Return your move to be processed by the server.
        return myMove;
    }
}