package student_player;

import Saboteur.SaboteurMove;
import Saboteur.cardClasses.SaboteurMap;
import Saboteur.cardClasses.SaboteurTile;

import java.util.*;

public class SimulatedBoard {
    private SaboteurTile[][] board;
    private int[][] intBoard;
    private int player1Malus;
    private int player2Malus;
    private boolean[] playerHiddenRevealed;
    private SaboteurTile[] hiddenTiles;
    private ArrayList<SaboteurMove> allLegalMoves;
    private Random rand;
    private boolean certainNuggetLocation;

    public static final int BOARD_SIZE = 14;
    public static final int originPos = 5;
    public static final int[][] hiddenPos = {{originPos+7,originPos-2},{originPos+7,originPos},{originPos+7,originPos+2}};
    public static final int EMPTY = -1;
    public static final int TUNNEL = 1;
    public static final int WALL = 0;

    // these four variables are used to tell which ends of the tile are continued
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;

    public SimulatedBoard(SaboteurTile[][] board, int[][] intBoard, int player1Malus, int player2Malus, boolean[] playerHiddenRevealed, SaboteurTile[] hiddenTiles, ArrayList<SaboteurMove> allLegalMoves, boolean certainNuggetLocation) {
        this.board = board;
        this.intBoard = intBoard;
        this.player1Malus = player1Malus;
        this.player2Malus = player2Malus;
        this.playerHiddenRevealed = playerHiddenRevealed;
        this.hiddenTiles = hiddenTiles;
        this.allLegalMoves = allLegalMoves;
        this.certainNuggetLocation = certainNuggetLocation;
        this.rand = new Random(2019);
    }

    public SaboteurMove getIdealMove() {
        if (player1Malus > 0) { // play bonus card if player has it or drop a random card
            ArrayList<SaboteurMove> dropCardMoves = new ArrayList<>();

            for (SaboteurMove move : allLegalMoves) {
                if (move.getCardPlayed().getName().contains("Bonus")) { // prioritize playing Bonus card
                    return move;
                }

                if (move.getCardPlayed().getName().contains("Drop")) {
                    dropCardMoves.add(move);
                }
            }

            return dropCardMoves.get(rand.nextInt(dropCardMoves.size())); // drop a random card
        } else {
            if (certainNuggetLocation) { // if we are certain of the nugget's location
                int currentDistanceToGoal = findCurrentShortestDistanceToGoal(); // this is the current shortest distance to the nugget

                if (currentDistanceToGoal == 3) { // winnable in 2 moves, here we only try to shorten the distance if player 2 malus is > 0
                    if (player2Malus > 0) {
                        SaboteurMove move = shortenDistanceToGoal(currentDistanceToGoal); // this is will return a move if there is a move that can shorten the distance to the nugget

                        if (move != null) {
                            return move;
                        } else {
                            return allLegalMoves.get(rand.nextInt(allLegalMoves.size()));
                        }
                    } else { // do not try to shorten the distance
                        SaboteurMove move = getNonShorteningMove(currentDistanceToGoal);

                        if (move != null) {
                            return move;
                        } else {
                            return allLegalMoves.get(rand.nextInt(allLegalMoves.size()));
                        }
                    }
                } else if (currentDistanceToGoal == 2) { // winnable in 1 move, here we try to win, if we can't we try to sabotage
                    SaboteurMove move = shortenDistanceToGoal(currentDistanceToGoal); // this is will return a move if there is a move that can shorten the distance to the nugget

                    if (move != null) {
                        return move;
                    } else {
                        move = getSabotageMove(currentDistanceToGoal);
                        if (move != null) {
                            return move;
                        } else {
                            return allLegalMoves.get(rand.nextInt(allLegalMoves.size()));
                        }
                    }
                } else {
                    SaboteurMove move = shortenDistanceToGoal(currentDistanceToGoal); // this is will return a move if there is a mvoe that can shorten the distance to the nugget

                    if (move != null) {
                        return move;
                    } else {
                        return allLegalMoves.get(rand.nextInt(allLegalMoves.size()));
                    }
                }
            } else { // if we are not certain of the nugget's location, play a map card if possible
                SaboteurMove move = playRandomMapCard();

                if (move != null) {
                    return move;
                } else {
                    int currentDistanceToGoal = findCurrentShortestDistanceToGoal(); // this is the current shortest distance to the nugget

                    move = shortenDistanceToGoal(currentDistanceToGoal); // this is will return a move if there is a move that can shorten the distance to the nugget

                    if (move != null) {
                        return move;
                    } else {
                        return allLegalMoves.get(rand.nextInt(allLegalMoves.size()));
                    }
                }
            }
        }
    }

    public SaboteurMove getNonShorteningMove(int currentDistanceToGoal) {
        ArrayList<SaboteurMove> malusMoves = new ArrayList<>();
        ArrayList<SaboteurMove> tilesMovesNotShortenDistance = new ArrayList<>();
        ArrayList<SaboteurMove> dropMoves = new ArrayList<>();

        for (SaboteurMove move : allLegalMoves) {
            if (move.getCardPlayed().getName().contains("Malus")) {
                malusMoves.add(move);
            } else if (move.getCardPlayed() instanceof SaboteurTile) {
                int position[] = move.getPosPlayed();
                board[position[0]][position[1]] = (SaboteurTile) move.getCardPlayed(); // temporarily add the tile to the board

                int newDistanceToGoal = findCurrentShortestDistanceToGoal(); // find the distance to the goal with the tile deleted

                if (newDistanceToGoal == currentDistanceToGoal) {
                    tilesMovesNotShortenDistance.add(move);
                }
                board[position[0]][position[1]] = null; // reset the board to its initial state
            } else if (move.getCardPlayed().getName().contains("Drop")) {
                dropMoves.add(move);
            }
        }

        if (malusMoves.size() > 0) {
            return malusMoves.get(rand.nextInt(malusMoves.size()));
        } else if (tilesMovesNotShortenDistance.size() > 0) {
            return tilesMovesNotShortenDistance.get(rand.nextInt(tilesMovesNotShortenDistance.size()));
        } else if (dropMoves.size() > 0) {
            return dropMoves.get(rand.nextInt(dropMoves.size()));
        }

        return null;
    }

    // this method tries to sabotage the game
    public SaboteurMove getSabotageMove(int currentDistanceToGoal) {
        ArrayList<SaboteurMove> malusMoves = new ArrayList<>();
        ArrayList<SaboteurMove> destroyMoves = new ArrayList<>();
        ArrayList<SaboteurMove> tilesMovesNotShortenDistance = new ArrayList<>();
        ArrayList<SaboteurMove> dropMoves = new ArrayList<>();

        for (SaboteurMove move : allLegalMoves) {
            if (move.getCardPlayed().getName().contains("Malus")) {
                malusMoves.add(move);
            } else if (move.getCardPlayed().getName().contains("Destroy")) {
                int position[] = move.getPosPlayed();
                SaboteurTile deletedTile = board[position[0]][position[1]];

                board[position[0]][position[1]] = null; // temporarily delete the tile on the board
                int newDistanceToGoal = findCurrentShortestDistanceToGoal(); // find the distance to the goal with the tile deleted

                if (newDistanceToGoal > currentDistanceToGoal) {
                    destroyMoves.add(move);
                }
                board[position[0]][position[1]] = deletedTile; // reset the board to its initial state
            } else if (move.getCardPlayed() instanceof SaboteurTile) {
                int position[] = move.getPosPlayed();
                board[position[0]][position[1]] = (SaboteurTile) move.getCardPlayed(); // temporarily add the tile to the board

                int newDistanceToGoal = findCurrentShortestDistanceToGoal(); // find the distance to the goal with the tile deleted

                if (newDistanceToGoal == currentDistanceToGoal) {
                    tilesMovesNotShortenDistance.add(move);
                }
                board[position[0]][position[1]] = null; // reset the board to its initial state
            } else if (move.getCardPlayed().getName().contains("Drop")) {
                dropMoves.add(move);
            }
        }

        if (malusMoves.size() > 0) {
            return malusMoves.get(rand.nextInt(malusMoves.size()));
        } else if (destroyMoves.size() > 0) {
            return destroyMoves.get(rand.nextInt(destroyMoves.size()));
        } else if (tilesMovesNotShortenDistance.size() > 0) {
            return tilesMovesNotShortenDistance.get(rand.nextInt(tilesMovesNotShortenDistance.size()));
        } else if (dropMoves.size() > 0) {
            return dropMoves.get(rand.nextInt(dropMoves.size()));
        }

        return null;
    }

    // this method returns a random map move from allLegalMoves
    public SaboteurMove playRandomMapCard() {
        ArrayList<SaboteurMove> mapMoves = new ArrayList<>();
        for (SaboteurMove move : allLegalMoves) {
            if (move.getCardPlayed() instanceof SaboteurMap) {
                mapMoves.add(move);
            }
        }

        if (mapMoves.size() > 0) {
            return mapMoves.get(rand.nextInt(mapMoves.size()));
        } else {
            return null;
        }
    }

    // this method goes through every single move that plays a SaboterTile and returns a move that shortens the distance to the goal if there exists one
    // if there does not exist a move to shorten the distance this method returns null
    public SaboteurMove shortenDistanceToGoal(int currentDistanceToGoal) {
        int[] positionOfTile;
        SaboteurTile tile;
        Integer distanceToGoalFromMove;

        for (SaboteurMove move : allLegalMoves) {
            if (move.getCardPlayed() instanceof SaboteurTile) {
                positionOfTile = move.getPosPlayed();
                tile = (SaboteurTile) move.getCardPlayed();

                if (tile.getPath()[1][1] == 1) { // ensure the move does not break the graph
                    board[positionOfTile[0]][positionOfTile[1]] = tile; // position the tile on the board temporarily

                    distanceToGoalFromMove = distanceToGoal(positionOfTile);

                    if (distanceToGoalFromMove != null) {
                        if (distanceToGoalFromMove < currentDistanceToGoal) {
                            board[positionOfTile[0]][positionOfTile[1]] = null; // reset the tile on the board
                            return move;
                        }
                    }

                    board[positionOfTile[0]][positionOfTile[1]] = null; // reset the tile on the board
                }
            }
        }

        return null;
    }

    // this method finds the shortest distance to the goal from each of the leaves in the board right now
    public int findCurrentShortestDistanceToGoal() {
        Integer shortestDistance = BOARD_SIZE * 2;
        Integer currentDistance;

        ArrayList<TileNodeBFS> leaves = getAllLeaves();

        for (TileNodeBFS tile : leaves) {
            currentDistance = distanceToGoal(tile.getPosition());

            if (currentDistance != null) {
                if (currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                }
            }
        }

        return shortestDistance;
    }

    // this method returns all the leaves of the board (ends that can still be expanded upon)
    // this method uses breadth first search algorithm
    public ArrayList<TileNodeBFS> getAllLeaves() {
        ArrayList<TileNodeBFS> leaves = new ArrayList<>();
        Queue<TileNodeBFS> linkedList = new LinkedList<>();
        linkedList.add(new TileNodeBFS(board[5][5], new int[]{5, 5})); // add the initial piece to the queue

        // this 2d array keeps track of which tiles are already visited
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        visited[5][5] = true; // initialize visited

        while(linkedList.size() > 0) {
            TileNodeBFS tileNode = linkedList.poll();

            int[] tileNodePositions = tileNode.getPosition();

            ArrayList<Integer> connectedEnds = checkConnectedEnds(tileNode.getTile());

            boolean hasUnusedEnds = false;
            for (Integer end : connectedEnds) {
                if (end == UP) {
                    if (tileNodePositions[0] - 1 >= 0) {
                        int[] newPositions = new int[]{tileNodePositions[0] - 1, tileNodePositions[1]};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (!visited[newPositions[0]][newPositions[1]]) {
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        } else {
                            hasUnusedEnds = true;
                        }
                    }
                } else if (end == DOWN) {
                    if (tileNodePositions[0] + 1 < BOARD_SIZE) {
                        int[] newPositions = new int[]{tileNodePositions[0] + 1, tileNodePositions[1]};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (!visited[newPositions[0]][newPositions[1]]) {
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        } else {
                            hasUnusedEnds = true;
                        }
                    }
                } else if (end == LEFT) {
                    if (tileNodePositions[1] - 1 >= 0) {
                        int[] newPositions = new int[]{tileNodePositions[0], tileNodePositions[1] - 1};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (!visited[newPositions[0]][newPositions[1]]) {
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        } else {
                            hasUnusedEnds = true;
                        }
                    }
                } else if (end == RIGHT) {
                    if (tileNodePositions[1] + 1 < BOARD_SIZE) {
                        int[] newPositions = new int[]{tileNodePositions[0], tileNodePositions[1] + 1};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (!visited[newPositions[0]][newPositions[1]]) {
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        } else {
                            hasUnusedEnds = true;
                        }
                    }
                }
            }


            // if there are any unused ends, that means it is a leaf
            if (hasUnusedEnds) {
                leaves.add(tileNode);
            }
        }

        return leaves;
    }

    // apply A* search from the x and y positions to to nugget to find the distance to the goal
    // apply SaboteurTile to board before calling this method if testing a move
    public Integer distanceToGoal(int[] currentPosition) {
        int[] nuggetPosition = new int[2];

        // find the position of the nugget
        for (int i = 0; i < 3; i++) {
            if (hiddenTiles[i].getName().contains("nugget")) {
                nuggetPosition[0] = hiddenPos[i][0];
                nuggetPosition[1] = hiddenPos[i][1];
            }
        }

        // set up priority que for the search algorithm
        Comparator<TileNodeAStar> comparator = new ValueComparator();
        PriorityQueue<TileNodeAStar> priorityQueue = new PriorityQueue<>(comparator);
        priorityQueue.add(new TileNodeAStar(currentPosition, calculateHeuristic(currentPosition, nuggetPosition), 0));

        // this 2d array keeps track of which tiles are already visited
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        visited[currentPosition[0]][currentPosition[1]] = true; // initialize visited

        while (priorityQueue.size() > 0) {
            if (!Arrays.equals(priorityQueue.peek().getPosition(), nuggetPosition)) {
                // dequeue top of priority queue
                TileNodeAStar head = priorityQueue.poll();
                int[] headPositions = head.getPosition();

                // this will only run on the first iteration of the algorithm
                // this makes sure we don't go down paths that are not possible due to the tile's configuration
                // in both the if and else segments we enqueue reachable nodes
                if (board[headPositions[0]][headPositions[1]] != null) {
                    ArrayList<Integer> connectedEnds = checkConnectedEnds(board[headPositions[0]][headPositions[1]]);

                    for (Integer end : connectedEnds) {
                        if (end == UP) {
                            if (headPositions[0] - 1 >= 0) {
                                int[] newPositions = new int[]{headPositions[0] - 1, headPositions[1]};

                                if (!visited[newPositions[0]][newPositions[1]]) {
                                    // check if newPositions is equal to one of hiddenPos
                                    boolean isHiddenTile = false;
                                    for (int i = 0; i < 3; i++) {
                                        if (Arrays.equals(newPositions, hiddenPos[i])) {
                                            isHiddenTile = true;
                                        }
                                    }

                                    if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                        priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                        visited[newPositions[0]][newPositions[1]] = true;
                                    }
                                }
                            }
                        } else if (end == DOWN) {
                            if (headPositions[0] + 1 < BOARD_SIZE) {
                                int[] newPositions = new int[]{headPositions[0] + 1, headPositions[1]};

                                if (!visited[newPositions[0]][newPositions[1]]) {
                                    // check if newPositions is equal to one of hiddenPos
                                    boolean isHiddenTile = false;
                                    for (int i = 0; i < 3; i++) {
                                        if (Arrays.equals(newPositions, hiddenPos[i])) {
                                            isHiddenTile = true;
                                        }
                                    }
                                    if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                        priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                        visited[newPositions[0]][newPositions[1]] = true;
                                    }
                                }
                            }
                        } else if (end == LEFT) {
                            if (headPositions[1] - 1 >= 0) {
                                int[] newPositions = new int[]{headPositions[0], headPositions[1] - 1};

                                if (!visited[newPositions[0]][newPositions[1]]) {
                                    // check if newPositions is equal to one of hiddenPos
                                    boolean isHiddenTile = false;
                                    for (int i = 0; i < 3; i++) {
                                        if (Arrays.equals(newPositions, hiddenPos[i])) {
                                            isHiddenTile = true;
                                        }
                                    }
                                    if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                        priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                        visited[newPositions[0]][newPositions[1]] = true;
                                    }
                                }
                            }
                        } else if (end == RIGHT) {
                            if (headPositions[1] + 1 < BOARD_SIZE) {
                                int[] newPositions = new int[]{headPositions[0], headPositions[1] + 1};

                                if (!visited[newPositions[0]][newPositions[1]]) {
                                    // check if newPositions is equal to one of hiddenPos
                                    boolean isHiddenTile = false;
                                    for (int i = 0; i < 3; i++) {
                                        if (Arrays.equals(newPositions, hiddenPos[i])) {
                                            isHiddenTile = true;
                                        }
                                    }
                                    if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                        priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                        visited[newPositions[0]][newPositions[1]] = true;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (headPositions[0] - 1 >= 0) {
                        int[] newPositions = new int[]{headPositions[0] - 1, headPositions[1]};

                        if (!visited[newPositions[0]][newPositions[1]]) {
                            // check if newPositions is equal to one of hiddenPos
                            boolean isHiddenTile = false;
                            for (int i = 0; i < 3; i++) {
                                if (Arrays.equals(newPositions, hiddenPos[i])) {
                                    isHiddenTile = true;
                                }
                            }
                            if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        }
                    }

                    if (headPositions[0] + 1 < BOARD_SIZE) {
                        int[] newPositions = new int[]{headPositions[0] + 1, headPositions[1]};

                        if (!visited[newPositions[0]][newPositions[1]]) {
                            // check if newPositions is equal to one of hiddenPos
                            boolean isHiddenTile = false;
                            for (int i = 0; i < 3; i++) {
                                if (Arrays.equals(newPositions, hiddenPos[i])) {
                                    isHiddenTile = true;

                                }
                            }
                            if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        }
                    }

                    if (headPositions[1] - 1 >= 0) {
                        int[] newPositions = new int[]{headPositions[0], headPositions[1] - 1};

                        if (!visited[newPositions[0]][newPositions[1]]) {
                            // check if newPositions is equal to one of hiddenPos
                            boolean isHiddenTile = false;
                            for (int i = 0; i < 3; i++) {
                                if (Arrays.equals(newPositions, hiddenPos[i])) {
                                    isHiddenTile = true;

                                }
                            }
                            if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        }
                    }

                    if (headPositions[1] + 1 < BOARD_SIZE) {
                        int[] newPositions = new int[]{headPositions[0], headPositions[1] + 1};

                        if (!visited[newPositions[0]][newPositions[1]]) {
                            // check if newPositions is equal to one of hiddenPos
                            boolean isHiddenTile = false;
                            for (int i = 0; i < 3; i++) {
                                if (Arrays.equals(newPositions, hiddenPos[i])) {
                                    isHiddenTile = true;
                                }
                            }
                            if (board[newPositions[0]][newPositions[1]] == null || isHiddenTile) {
                                priorityQueue.add(new TileNodeAStar(newPositions, calculateHeuristic(newPositions, nuggetPosition), head.getPathCost() + 1));
                                visited[newPositions[0]][newPositions[1]] = true;
                            }
                        }
                    }
                }
            } else {
                break;
            }
        }

        if (priorityQueue.size() > 0) {
            return priorityQueue.peek().getPathCost();
        } else {
            return null;
        }
    }

    // this method calculates an admissible heuristic for the distance between currentPosition and nuggetPosition
    public int calculateHeuristic(int[] currentPosition, int[] nuggetPosition) {
        return Math.abs(currentPosition[0] - nuggetPosition[0]) + Math.abs(currentPosition[1] - nuggetPosition[1]);
    }

    // this method returns all the ends that can be connected by the saboteurTile
    public ArrayList<Integer> checkConnectedEnds (SaboteurTile saboteurTile) {
        int[][] tilePath = saboteurTile.getPath();
        ArrayList<Integer> connectedEnds = new ArrayList<>();

        if (tilePath[1][1] == 1) {
            if (tilePath[1][2] == 1) {
                connectedEnds.add(UP);
            }

            if (tilePath[1][0] == 1) {
                connectedEnds.add(DOWN);
            }

            if (tilePath[0][1] == 1) {
                connectedEnds.add(LEFT);
            }

            if (tilePath[2][1] == 1) {
                connectedEnds.add(RIGHT);
            }
        }

        return connectedEnds;
    }

    // this class compares two TileNodeAStar objects used in priority queue
    public class ValueComparator implements Comparator<TileNodeAStar> {
        @Override
        public int compare(TileNodeAStar o1, TileNodeAStar o2) {
            return o1.getTotalCost() - o2.getTotalCost();
        }
    }
}
