package student_player;

import Saboteur.SaboteurMove;
import Saboteur.cardClasses.SaboteurTile;

import java.util.*;

public class SimulatedBoard {
    private SaboteurTile[][] board;
    private int[][] intBoard;
    private int playerMalus;
    private boolean[] playerHiddenRevealed;
    private SaboteurTile[] hiddenTiles;
    private ArrayList<SaboteurMove> allLegalMoves;
    private Random rand;

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

    public SimulatedBoard(SaboteurTile[][] board, int[][] intBoard, int playerMalus, boolean[] playerHiddenRevealed, SaboteurTile[] hiddenTiles, ArrayList<SaboteurMove> allLegalMoves) {
        this.board = board;
        this.intBoard = intBoard;
        this.playerMalus = playerMalus;
        this.playerHiddenRevealed = playerHiddenRevealed;
        this.hiddenTiles = hiddenTiles;
        this.allLegalMoves = allLegalMoves;
        this.rand = new Random(2019);
    }

    public SaboteurMove getIdealMove() {
        if (playerMalus > 0) { // play bonus card if player has it or drop a random card
            ArrayList<SaboteurMove> dropCardMoves = new ArrayList<>();

            for (SaboteurMove move : allLegalMoves) {
                if (move.getCardPlayed().getName().contains("Bonus")) { // prioritize playing Bonus card
                    return move;
                }

                if (move.getCardPlayed().getName().equals("Drop")) {
                    dropCardMoves.add(move);
                }
            }

            return dropCardMoves.get(rand.nextInt(dropCardMoves.size())); // drop a random card
        } else {

        }
        return null;
    }

    // this method returns all the leaves of the board (ends that can still be expanded upon)
    // this method uses breadth first search algorithm
    public ArrayList<SaboteurTile> getAllLeaves() {
        ArrayList<SaboteurTile> leaves = new ArrayList<>();
        Queue<TileNodeBFS> linkedList = new LinkedList<>();
        linkedList.add(new TileNodeBFS(board[5][5], new int[]{5, 5})); // add the initial piece to the queue

        // this 2d array keeps track of which tiles are already visited
        boolean[][] visited = new boolean[BOARD_SIZE][BOARD_SIZE];
        visited[5][5] = true; // initialize visited

        while(linkedList.size() > 0) {
            TileNodeBFS tileNode = linkedList.poll();
            int[] tileNodePositions = tileNode.getPosition();

            ArrayList<Integer> connectedEnds = checkConnectedEnds(tileNode.getTile());

            boolean addedNodes = false;
            for (Integer end : connectedEnds) {
                if (end == UP) {
                    if (tileNodePositions[0] - 1 >= 0) {
                        int[] newPositions = new int[]{tileNodePositions[0] - 1, tileNodePositions[1]};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (board[newPositions[0]][newPositions[1]].getPath()[1][1] != 0) { // checks if the tile is connected in the middle
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                addedNodes = true;
                            }
                        }
                    }
                } else if (end == DOWN) {
                    if (tileNodePositions[0] + 1 <= BOARD_SIZE) {
                        int[] newPositions = new int[]{tileNodePositions[0] + 1, tileNodePositions[1]};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (board[newPositions[0]][newPositions[1]].getPath()[1][1] != 0) { // checks if the tile is connected in the middle
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                addedNodes = true;
                            }
                        }
                    }
                } else if (end == LEFT) {
                    if (tileNodePositions[1] - 1 >= 0) {
                        int[] newPositions = new int[]{tileNodePositions[0], tileNodePositions[1] - 1};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (board[newPositions[0]][newPositions[1]].getPath()[1][1] != 0) { // checks if the tile is connected in the middle
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                addedNodes = true;
                            }
                        }
                    }
                } else if (end == RIGHT) {
                    if (tileNodePositions[1] + 1 <= BOARD_SIZE) {
                        int[] newPositions = new int[]{tileNodePositions[0], tileNodePositions[1] + 1};

                        if (board[newPositions[0]][newPositions[1]] != null) {
                            if (board[newPositions[0]][newPositions[1]].getPath()[1][1] != 0) { // checks if the tile is connected in the middle
                                linkedList.add(new TileNodeBFS(board[newPositions[0]][newPositions[1]], newPositions));
                                addedNodes = true;
                            }
                        }
                    }
                }
            }

            // if we did not add any tiles, this means that the node is a leaf
            if (!addedNodes) {
                leaves.add(tileNode.getTile());
            }
        }

        return leaves;
    }

    // apply A* search from the x and y positions to to nugget to find the distance to the goal
    // apply SaboteurTile to board before calling this method if testing a move
    public int distanceToGoal(int[] currentPosition) {
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

        while (!priorityQueue.peek().getPosition().equals(nuggetPosition)) {
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
                            if (board[newPositions[0]][newPositions[1]] == null) {
                                priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                            }
                        }
                    } else if (end == DOWN) {
                        if (headPositions[0] + 1 <= BOARD_SIZE) {
                            int[] newPositions = new int[]{headPositions[0] + 1, headPositions[1]};
                            if (board[newPositions[0]][newPositions[1]] == null) {
                                priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                            }
                        }
                    } else if (end == LEFT) {
                        if (headPositions[1] - 1 >= 0) {
                            int[] newPositions = new int[]{headPositions[0], headPositions[1] - 1};
                            if (board[newPositions[0]][newPositions[1]] == null) {
                                priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                            }
                        }
                    } else if (end == RIGHT) {
                        if (headPositions[1] + 1 <= BOARD_SIZE) {
                            int[] newPositions = new int[]{headPositions[0], headPositions[1] + 1};
                            if (board[newPositions[0]][newPositions[1]] == null) {
                                priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                            }
                        }
                    }
                }
            } else {
                if (headPositions[0] - 1 >= 0) {
                    int[] newPositions = new int[]{headPositions[0] - 1, headPositions[1]};
                    if (board[newPositions[0]][newPositions[1]] == null) {
                        priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                    }
                }

                if (headPositions[0] + 1 <= BOARD_SIZE) {
                    int[] newPositions = new int[]{headPositions[0] + 1, headPositions[1]};
                    if (board[newPositions[0]][newPositions[1]] == null) {
                        priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                    }
                }

                if (headPositions[1] - 1 >= 0) {
                    int[] newPositions = new int[]{headPositions[0], headPositions[1] - 1};
                    if (board[newPositions[0]][newPositions[1]] == null) {
                        priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                    }
                }

                if (headPositions[1] + 1 <= BOARD_SIZE) {
                    int[] newPositions = new int[]{headPositions[0], headPositions[1] + 1};
                    if (board[newPositions[0]][newPositions[1]] == null) {
                        priorityQueue.add(new TileNodeAStar(newPositions, head.getPathCost() + 1, calculateHeuristic(newPositions, nuggetPosition)));
                    }
                }
            }
        }

        return priorityQueue.peek().getPathCost();
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
