package student_player;

import Saboteur.SaboteurBoardState;
import Saboteur.SaboteurMove;
import Saboteur.cardClasses.*;
import boardgame.Board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class SimulatedBoardState {
    public static final int BOARD_SIZE = 14;

    private SaboteurTile[][] board;
    private int[][] intBoard;

    private ArrayList<SaboteurCard> deck;
    private ArrayList<SaboteurCard> player1Cards;
    private ArrayList<SaboteurCard> player2Cards;

    private int player1nbMalus;
    private int player2nbMalus;

    private int turnPlayer;
    private int turnNumber;
    private int winner;

    private boolean[] player1hiddenRevealed;
    private boolean[] player2hiddenRevealed;

    private Random rand;

    protected SaboteurTile[] hiddenCards = new SaboteurTile[3];
    private boolean[] hiddenRevealed = {false,false,false}; //whether hidden at pos1 is revealed, hidden at pos2 is revealed, hidden at pos3 is revealed.

    public static final int originPos = 5;
    public static final int[][] hiddenPos = {{originPos+7,originPos-2},{originPos+7,originPos},{originPos+7,originPos+2}};
    public static final int EMPTY = -1;
    public static final int TUNNEL = 1;
    public static final int WALL = 0;

    public SimulatedBoardState(SaboteurTile[][] board, int[][] intBoard, ArrayList<SaboteurCard> player1Cards, int player1nbMalus, int player2nbMalus, boolean[] player1hiddenRevealed, boolean[] player2hiddenRevealed) {
        this.board = board;
        this.intBoard = intBoard;
        this.player1Cards = player1Cards;

        this.deck = SaboteurCard.getDeck();
        Collections.shuffle(this.deck);
        this.player1Cards = player1Cards;
        this.player2Cards = new ArrayList<>();

        // generate random hand for player2Hand
        for(int i=0;i<7;i++){
            this.deck.remove(0);
            this.player2Cards.add(this.deck.remove(0));
        }

        this.player1nbMalus = player1nbMalus;
        this.player2nbMalus = player2nbMalus;

        // initialize the hidden position:
        ArrayList<String> list =new ArrayList<String>();
        list.add("hidden1");
        list.add("hidden2");
        list.add("nugget");

        int totalHiddenCardsKnown = 0;

        // set hiddenCards to its actual value if player1 knows what it is
        for (int i = 0; i < 3; i++) {
            if (player1hiddenRevealed[i]) {
                totalHiddenCardsKnown++;
                list.remove(this.board[hiddenPos[i][0]][hiddenPos[i][1]].getName()); // delete the occurrence from list because we now know where the hiddenCard is
                this.board[hiddenPos[i][0]][hiddenPos[i][1]] = this.board[hiddenPos[i][0]][hiddenPos[i][1]];
                this.hiddenCards[i] = this.board[hiddenPos[i][0]][hiddenPos[i][1]];
            }
        }

        // randomize the rest of the hiddenCards as player1 does not know what they are
        Random startRand = new Random();
        for (int i = 0; i < 3; i++) {
            if (!player1hiddenRevealed[i]) {
                int idx = startRand.nextInt(list.size());
                this.board[hiddenPos[i][0]][hiddenPos[i][1]] = new SaboteurTile(list.remove(idx));
                this.hiddenCards[i] = this.board[hiddenPos[i][0]][hiddenPos[i][1]];
            }
        }

        this.rand = new Random(2019);
        this.turnPlayer = 1;
        this.turnNumber = 0;
        this.winner = Board.NOBODY;

        this.player1hiddenRevealed = player1hiddenRevealed;
        this.player2hiddenRevealed = player2hiddenRevealed;
    }

    public ArrayList<int[]> possiblePositions(SaboteurTile card) {
        // Given a card, returns all the possiblePositions at which the card could be positioned in an ArrayList of int[];
        // Note that the card will not be flipped in this test, a test for the flipped card should be made by giving to the function the flipped card.
        ArrayList<int[]> possiblePos = new ArrayList<int[]>();
        int[][] moves = {{0, -1},{0, 1},{1, 0},{-1, 0}}; //to make the test faster, we simply verify around all already placed tiles.
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (this.board[i][j] != null) {
                    for (int m = 0; m < 4; m++) {
                        if (0 <= i+moves[m][0] && i+moves[m][0] < BOARD_SIZE && 0 <= j+moves[m][1] && j+moves[m][1] < BOARD_SIZE) {
                            if (this.verifyLegit(card.getPath(), new int[]{i + moves[m][0], j + moves[m][1]} )){
                                possiblePos.add(new int[]{i + moves[m][0], j +moves[m][1]});
                            }
                        }
                    }
                }
            }
        }
        return possiblePos;
    }

    public boolean verifyLegit(int[][] path,int[] pos){
        // Given a tile's path, and a position to put this path, verify that it respects the rule of positionning;
        if (!(0 <= pos[0] && pos[0] < BOARD_SIZE && 0 <= pos[1] && pos[1] < BOARD_SIZE)) {
            return false;
        }
        if(board[pos[0]][pos[1]] != null) return false;

        //the following integer are used to make sure that at least one path exists between the possible new tile to be added and existing tiles.
        // There are 2 cases:  a tile can't be placed near an hidden objective and a tile can't be connected only by a wall to another tile.
        int requiredEmptyAround=4;
        int numberOfEmptyAround=0;

        ArrayList<SaboteurTile> objHiddenList=new ArrayList<>();
        for(int i=0;i<3;i++) {
            if (!hiddenRevealed[i]){
                objHiddenList.add(this.board[hiddenPos[i][0]][hiddenPos[i][1]]);
            }
        }
        //verify left side:
        if(pos[1]>0) {
            SaboteurTile neighborCard = this.board[pos[0]][pos[1] - 1];
            if (neighborCard == null) numberOfEmptyAround += 1;
            else if(objHiddenList.contains(neighborCard)) requiredEmptyAround -= 1;
            else {
                int[][] neighborPath = neighborCard.getPath();
                if (path[0][0] != neighborPath[2][0] || path[0][1] != neighborPath[2][1] || path[0][2] != neighborPath[2][2] ) return false;
                else if(path[0][0] == 0 && path[0][1]== 0 && path[0][2] ==0 ) numberOfEmptyAround +=1;
            }
        }
        else numberOfEmptyAround+=1;

        //verify right side
        if(pos[1]<BOARD_SIZE-1) {
            SaboteurTile neighborCard = this.board[pos[0]][pos[1] + 1];
            if (neighborCard == null) numberOfEmptyAround += 1;
            else if(objHiddenList.contains(neighborCard)) requiredEmptyAround -= 1;
            else {
                int[][] neighborPath = neighborCard.getPath();
                if (path[2][0] != neighborPath[0][0] || path[2][1] != neighborPath[0][1] || path[2][2] != neighborPath[0][2]) return false;
                else if(path[2][0] == 0 && path[2][1]== 0 && path[2][2] ==0 ) numberOfEmptyAround +=1;
            }
        }
        else numberOfEmptyAround+=1;

        //verify upper side
        if(pos[0]>0) {
            SaboteurTile neighborCard = this.board[pos[0]-1][pos[1]];
            if (neighborCard == null) numberOfEmptyAround += 1;
            else if(objHiddenList.contains(neighborCard)) requiredEmptyAround -= 1;
            else {
                int[][] neighborPath = neighborCard.getPath();
                int[] p={path[0][2],path[1][2],path[2][2]};
                int[] np={neighborPath[0][0],neighborPath[1][0],neighborPath[2][0]};
                if (p[0] != np[0] || p[1] != np[1] || p[2] != np[2]) return false;
                else if(p[0] == 0 && p[1]== 0 && p[2] ==0 ) numberOfEmptyAround +=1;
            }
        }
        else numberOfEmptyAround+=1;

        //verify bottom side:
        if(pos[0]<BOARD_SIZE-1) {
            SaboteurTile neighborCard = this.board[pos[0]+1][pos[1]];
            if (neighborCard == null) numberOfEmptyAround += 1;
            else if(objHiddenList.contains(neighborCard)) requiredEmptyAround -= 1;
            else {
                int[][] neighborPath = neighborCard.getPath();
                int[] p={path[0][0],path[1][0],path[2][0]};
                int[] np={neighborPath[0][2],neighborPath[1][2],neighborPath[2][2]};
                if (p[0] != np[0] || p[1] != np[1] || p[2] != np[2]) return false;
                else if(p[0] == 0 && p[1]== 0 && p[2] ==0 ) numberOfEmptyAround +=1; //we are touching by a wall
            }
        }
        else numberOfEmptyAround+=1;

        if(numberOfEmptyAround==requiredEmptyAround)  return false;

        return true;
    }

    public ArrayList<SaboteurMove> getAllLegalMoves() {
        // Given the current player hand, gives back all legal moves he can play.
        ArrayList<SaboteurCard> hand;
        boolean isBlocked;
        if(turnPlayer == 1){
            hand = this.player1Cards;
            isBlocked= player1nbMalus > 0;
        }
        else {
            hand = this.player2Cards;
            isBlocked= player2nbMalus > 0;
        }

        ArrayList<SaboteurMove> legalMoves = new ArrayList<>();

        for(SaboteurCard card : hand){
            if( card instanceof SaboteurTile && !isBlocked) {
                ArrayList<int[]> allowedPositions = possiblePositions((SaboteurTile)card);
                for(int[] pos:allowedPositions){
                    legalMoves.add(new SaboteurMove(card,pos[0],pos[1],turnPlayer));
                }
                //if the card can be flipped, we also had legal moves where the card is flipped;
                if(SaboteurTile.canBeFlipped(((SaboteurTile)card).getIdx())){
                    SaboteurTile flippedCard = ((SaboteurTile)card).getFlipped();
                    ArrayList<int[]> allowedPositionsflipped = possiblePositions(flippedCard);
                    for(int[] pos:allowedPositionsflipped){
                        legalMoves.add(new SaboteurMove(flippedCard,pos[0],pos[1],turnPlayer));
                    }
                }
            }
            else if(card instanceof SaboteurBonus){
                if(turnPlayer ==1){
                    if(player1nbMalus > 0) legalMoves.add(new SaboteurMove(card,0,0,turnPlayer));
                }
                else if(player2nbMalus>0) legalMoves.add(new SaboteurMove(card,0,0,turnPlayer));
            }
            else if(card instanceof SaboteurMalus){
                legalMoves.add(new SaboteurMove(card,0,0,turnPlayer));
            }
            else if(card instanceof SaboteurMap){
                for(int i =0;i<3;i++){ //for each hidden card that has not be revealed, we can still take a look at it.
                    if(! this.hiddenRevealed[i]) legalMoves.add(new SaboteurMove(card,hiddenPos[i][0],hiddenPos[i][1],turnPlayer));
                }
            }
            else if(card instanceof SaboteurDestroy){
                for (int i = 0; i < BOARD_SIZE; i++) {
                    for (int j = 0; j < BOARD_SIZE; j++) { //we can't destroy an empty tile, the starting, or final tiles.
                        if(this.board[i][j] != null && (i!=originPos || j!= originPos) && (i != hiddenPos[0][0] || j!=hiddenPos[0][1] )
                                && (i != hiddenPos[1][0] || j!=hiddenPos[1][1] ) && (i != hiddenPos[2][0] || j!=hiddenPos[2][1] ) ){
                            legalMoves.add(new SaboteurMove(card,i,j,turnPlayer));
                        }
                    }
                }
            }
        }
        // we can also drop any of the card in our hand
        for(int i=0;i<hand.size();i++) {
            legalMoves.add(new SaboteurMove(new SaboteurDrop(), i, 0, turnPlayer));
        }
        return legalMoves;
    }

    public SaboteurMove getRandomMove() {
        ArrayList<SaboteurMove> moves = getAllLegalMoves();
        return moves.get(rand.nextInt(moves.size()));
    }

    public boolean isLegal(SaboteurMove m) {
        // For a move to be legal, the player must have the card in its hand
        // and then the game rules apply.
        // Note that we do not test the flipped version. To test it: use the flipped card in the SaboteurMove object.

        SaboteurCard testCard = m.getCardPlayed();
        int[] pos = m.getPosPlayed();
        int currentPlayer = m.getPlayerID();
        if (currentPlayer != turnPlayer) return false;

        ArrayList<SaboteurCard> hand;
        boolean isBlocked;
        if(turnPlayer == 1){
            hand = this.player1Cards;
            isBlocked= player1nbMalus > 0;
        }
        else {
            hand = this.player2Cards;
            isBlocked= player2nbMalus > 0;
        }
        if(testCard instanceof SaboteurDrop){
            if(hand.size()>=pos[0]){
                return true;
            }
        }
        boolean legal = false;
        for(SaboteurCard card : hand){
            if (card instanceof SaboteurTile && testCard instanceof SaboteurTile && !isBlocked) {
                if(((SaboteurTile) card).getIdx().equals(((SaboteurTile) testCard).getIdx())){
                    return verifyLegit(((SaboteurTile) card).getPath(),pos);
                }
                else if(((SaboteurTile) card).getFlipped().getIdx().equals(((SaboteurTile) testCard).getIdx())){
                    return verifyLegit(((SaboteurTile) card).getFlipped().getPath(),pos);
                }
            }
            else if (card instanceof SaboteurBonus && testCard instanceof SaboteurBonus) {
                if (turnPlayer == 1) {
                    if (player1nbMalus > 0) return true;
                } else if (player2nbMalus > 0) return true;
                return false;
            }
            else if (card instanceof SaboteurMalus && testCard instanceof SaboteurMalus ) {
                return true;
            }
            else if (card instanceof SaboteurMap && testCard instanceof SaboteurMap) {
                int ph = 0;
                for(int j=0;j<3;j++) {
                    if (pos[0] == hiddenPos[j][0] && pos[1] == hiddenPos[j][1]) ph=j;
                }
                if (!this.hiddenRevealed[ph])
                    return true;
            }
            else if (card instanceof SaboteurDestroy && testCard instanceof SaboteurDestroy) {
                int i = pos[0];
                int j = pos[1];
                if (this.board[i][j] != null && (i != originPos || j != originPos) && (i != hiddenPos[0][0] || j != hiddenPos[0][1])
                        && (i != hiddenPos[1][0] || j != hiddenPos[1][1]) && (i != hiddenPos[2][0] || j != hiddenPos[2][1])) {
                    return true;
                }
            }
        }
        return legal;
    }

    private void draw(){
        if(this.deck.size()>0){
            if(turnPlayer==1){
                this.player1Cards.add(this.deck.remove(0));
            }
            else{
                this.player2Cards.add(this.deck.remove(0));
            }
        }
    }

    private int[][] getIntBoard() {
        //update the int board.
        //Note that this tool is not available to the player.
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if(this.board[i][j] == null){
                    for (int k = 0; k < 3; k++) {
                        for (int h = 0; h < 3; h++) {
                            this.intBoard[i * 3 + k][j * 3 + h] = EMPTY;
                        }
                    }
                }
                else {
                    int[][] path = this.board[i][j].getPath();
                    for (int k = 0; k < 3; k++) {
                        for (int h = 0; h < 3; h++) {
                            this.intBoard[i * 3 + k][j * 3 + h] = path[h][2-k];
                        }
                    }
                }
            }
        }

        return this.intBoard;
    }

    private Boolean cardPath(ArrayList<int[]> originTargets,int[] targetPos,Boolean usingCard){
        // the search algorithm, usingCard indicate weither we search a path of cards (true) or a path of ones (aka tunnel)(false).
        ArrayList<int[]> queue = new ArrayList<>(); //will store the current neighboring tile. Composed of position (int[]).
        ArrayList<int[]> visited = new ArrayList<int[]>(); //will store the visited tile with an Hash table where the key is the position the board.
        visited.add(targetPos);
        if(usingCard) addUnvisitedNeighborToQueue(targetPos,queue,visited,BOARD_SIZE,usingCard);
        else addUnvisitedNeighborToQueue(targetPos,queue,visited,BOARD_SIZE*3,usingCard);
        while(queue.size()>0){
            int[] visitingPos = queue.remove(0);
            if(containsIntArray(originTargets,visitingPos)){
                return true;
            }
            visited.add(visitingPos);
            if(usingCard) addUnvisitedNeighborToQueue(visitingPos,queue,visited,BOARD_SIZE,usingCard);
            else addUnvisitedNeighborToQueue(visitingPos,queue,visited,BOARD_SIZE*3,usingCard);
        }
        return false;
    }

    private void addUnvisitedNeighborToQueue(int[] pos,ArrayList<int[]> queue, ArrayList<int[]> visited,int maxSize,boolean usingCard){
        int[][] moves = {{0, -1},{0, 1},{1, 0},{-1, 0}};
        int i = pos[0];
        int j = pos[1];
        for (int m = 0; m < 4; m++) {
            if (0 <= i+moves[m][0] && i+moves[m][0] < maxSize && 0 <= j+moves[m][1] && j+moves[m][1] < maxSize) { //if the hypothetical neighbor is still inside the board
                int[] neighborPos = new int[]{i+moves[m][0],j+moves[m][1]};
                if(!containsIntArray(visited,neighborPos)){
                    if(usingCard && this.board[neighborPos[0]][neighborPos[1]]!=null) queue.add(neighborPos);
                    else if(!usingCard && this.intBoard[neighborPos[0]][neighborPos[1]]==1) queue.add(neighborPos);
                }
            }
        }
    }

    private boolean containsIntArray(ArrayList<int[]> a,int[] o){ //the .equals used in Arraylist.contains is not working between arrays..
        if (o == null) {
            for (int i = 0; i < a.size(); i++) {
                if (a.get(i) == null)
                    return true;
            }
        } else {
            for (int i = 0; i < a.size(); i++) {
                if (Arrays.equals(o, a.get(i)))
                    return true;
            }
        }
        return false;
    }

    private boolean pathToHidden(SaboteurTile[] objectives){
        /* This function look if a path is linking the starting point to the states among objectives.
            :return: if there exists one: true
                     if not: false
                     In Addition it changes each reached states hidden variable to true:  self.hidden[foundState] <- true
            Implementation details:
            For each hidden objectives:
                We verify there is a path of cards between the start and the hidden objectives.
                    If there is one, we do the same but with the 0-1s matrix!

            To verify a path, we use a simple search algorithm where we propagate a front of visited neighbor.
               TODO To speed up: The neighbor are added ranked on their distance to the origin... (simply use a PriorityQueue with a Comparator)
        */
        this.getIntBoard(); //update the int board.
        boolean atLeastOnefound = false;
        for(SaboteurTile target : objectives){
            ArrayList<int[]> originTargets = new ArrayList<>();
            originTargets.add(new int[]{originPos,originPos}); //the starting points
            //get the target position
            int[] targetPos = {0,0};
            int currentTargetIdx = -1;
            for(int i =0;i<3;i++){
                if(this.hiddenCards[i].getIdx().equals(target.getIdx())){
                    targetPos = SaboteurBoardState.hiddenPos[i];
                    currentTargetIdx = i;
                    break;
                }
            }
            if(!this.hiddenRevealed[currentTargetIdx]) {  //verify that the current target has not been already discovered. Even if there is a destruction event, the target keeps being revealed!

                if (cardPath(originTargets, targetPos, true)) { //checks that there is a cardPath
                    //next: checks that there is a path of ones.
                    ArrayList<int[]> originTargets2 = new ArrayList<>();
                    //the starting points
                    originTargets2.add(new int[]{originPos*3+1, originPos*3+1});
                    originTargets2.add(new int[]{originPos*3+1, originPos*3+2});
                    originTargets2.add(new int[]{originPos*3+1, originPos*3});
                    originTargets2.add(new int[]{originPos*3, originPos*3+1});
                    originTargets2.add(new int[]{originPos*3+2, originPos*3+1});
                    //get the target position in 0-1 coordinate
                    int[] targetPos2 = {targetPos[0]*3+1, targetPos[1]*3+1};
                    if (cardPath(originTargets2, targetPos2, false)) {
                        this.hiddenRevealed[currentTargetIdx] = true;
                        this.player1hiddenRevealed[currentTargetIdx] = true;
                        this.player2hiddenRevealed[currentTargetIdx] = true;
                        atLeastOnefound =true;
                    }
                }
            }
            else{
                atLeastOnefound = true;
            }
        }
        return atLeastOnefound;
    }

    private void updateWinner() {

        pathToHidden(new SaboteurTile[]{new SaboteurTile("nugget"),new SaboteurTile("hidden1"),new SaboteurTile("hidden2")});
        int nuggetIdx = -1;
        for(int i =0;i<3;i++){
            if(this.hiddenCards[i].getIdx().equals("nugget")){
                nuggetIdx = i;
                break;
            }
        }
        boolean playerWin = this.hiddenRevealed[nuggetIdx];
        if (playerWin) { // Current player has won
            winner = turnPlayer;
        } else if (gameOver() && winner== Board.NOBODY) {
            winner = Board.DRAW;
        }

    }

    public boolean gameOver() {
        return this.deck.size()==0 && this.player1Cards.size()==0 && this.player2Cards.size()==0 || winner != Board.NOBODY;
    }

    public void processMove(SaboteurMove m) throws IllegalArgumentException {
        if (!isLegal(m)) {
            throw new IllegalArgumentException("Invalid move. Move: " + m.toPrettyString());
        }

        SaboteurCard testCard = m.getCardPlayed();
        int[] pos = m.getPosPlayed();

        if(testCard instanceof SaboteurTile){
            this.board[pos[0]][pos[1]] = new SaboteurTile(((SaboteurTile) testCard).getIdx());
            if(turnPlayer==1){
                //Remove from the player card the card that was used.
                for(SaboteurCard card : this.player1Cards) {
                    if (card instanceof SaboteurTile) {
                        if (((SaboteurTile) card).getIdx().equals(((SaboteurTile) testCard).getIdx())) {
                            this.player1Cards.remove(card);
                            break; //leave the loop....
                        }
                        else if(((SaboteurTile) card).getFlipped().getIdx().equals(((SaboteurTile) testCard).getIdx())) {
                            this.player1Cards.remove(card);
                            break; //leave the loop....
                        }
                    }
                }
            }
            else {
                for (SaboteurCard card : this.player2Cards) {
                    if (card instanceof SaboteurTile) {
                        if (((SaboteurTile) card).getIdx().equals(((SaboteurTile) testCard).getIdx())) {
                            this.player2Cards.remove(card);
                            break; //leave the loop....
                        }
                        else if(((SaboteurTile) card).getFlipped().getIdx().equals(((SaboteurTile) testCard).getIdx())) {
                            this.player2Cards.remove(card);
                            break; //leave the loop....
                        }
                    }
                }
            }
        }
        else if(testCard instanceof SaboteurBonus){
            if(turnPlayer==1){
                player1nbMalus --;
                for(SaboteurCard card : this.player1Cards) {
                    if (card instanceof SaboteurBonus) {
                        this.player1Cards.remove(card);
                        break; //leave the loop....
                    }
                }
            }
            else{
                player2nbMalus --;
                for(SaboteurCard card : this.player2Cards) {
                    if (card instanceof SaboteurBonus) {
                        this.player2Cards.remove(card);
                        break; //leave the loop....
                    }
                }
            }
        }
        else if(testCard instanceof SaboteurMalus){
            if(turnPlayer==1){
                player2nbMalus ++;
                for(SaboteurCard card : this.player1Cards) {
                    if (card instanceof SaboteurMalus) {
                        this.player1Cards.remove(card);
                        break; //leave the loop....
                    }
                }
            }
            else{
                player1nbMalus ++;
                for(SaboteurCard card : this.player2Cards) {
                    if (card instanceof SaboteurMalus) {
                        this.player2Cards.remove(card);
                        break; //leave the loop....
                    }
                }
            }
        }
        else if(testCard instanceof SaboteurMap){
            if(turnPlayer==1){
                for(SaboteurCard card : this.player1Cards) {
                    if (card instanceof SaboteurMap) {
                        this.player1Cards.remove(card);
                        int ph = 0;
                        for(int j=0;j<3;j++) {
                            if (pos[0] == hiddenPos[j][0] && pos[1] == hiddenPos[j][1]) ph=j;
                        }
                        this.player1hiddenRevealed[ph] = true;
                        break; //leave the loop....
                    }
                }
            }
            else{
                for(SaboteurCard card : this.player2Cards) {
                    if (card instanceof SaboteurMap) {
                        this.player2Cards.remove(card);
                        int ph = 0;
                        for(int j=0;j<3;j++) {
                            if (pos[0] == hiddenPos[j][0] && pos[1] == hiddenPos[j][1]) ph=j;
                        }
                        this.player2hiddenRevealed[ph] = true;
                        break; //leave the loop....
                    }
                }
            }
        }
        else if (testCard instanceof SaboteurDestroy) {
            int i = pos[0];
            int j = pos[1];
            if(turnPlayer==1){
                for(SaboteurCard card : this.player1Cards) {
                    if (card instanceof SaboteurDestroy) {
                        this.player1Cards.remove(card);
                        this.board[i][j] = null;
                        break; //leave the loop....
                    }
                }
            }
            else{
                for(SaboteurCard card : this.player2Cards) {
                    if (card instanceof SaboteurDestroy) {
                        this.player2Cards.remove(card);
                        this.board[i][j] = null;
                        break; //leave the loop....
                    }
                }
            }
        }
        else if(testCard instanceof SaboteurDrop){
            if(turnPlayer==1) this.player1Cards.remove(pos[0]);
            else this.player2Cards.remove(pos[0]);
        }
        this.draw();
        this.updateWinner();
        turnPlayer = 1 - turnPlayer; // Swap player
        turnNumber++;
    }

    public int getWinner() { return winner; }
}
