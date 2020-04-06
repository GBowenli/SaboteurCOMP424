package student_player;

public class TileNodeAStar {
    private int[] position;
    private int heuristic;
    private int pathCost;
    private int totalCost;

    public TileNodeAStar(int[] position, int heuristic, int pathCost) {
        this.position = position;
        this.heuristic = heuristic;
        this.pathCost = pathCost;
        this.totalCost = heuristic + pathCost;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }


    public int getHeuristic() {
        return heuristic;
    }

    public void setHeuristic(int heuristic) {
        this.heuristic = heuristic;
    }

    public int getPathCost() {
        return pathCost;
    }

    public void setPathCost(int pathCost) {
        this.pathCost = pathCost;
    }

    public int getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(int totalCost) {
        this.totalCost = totalCost;
    }
}
