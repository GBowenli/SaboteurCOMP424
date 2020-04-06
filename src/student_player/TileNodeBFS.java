package student_player;

import Saboteur.cardClasses.SaboteurTile;

public class TileNodeBFS {
    private SaboteurTile tile;
    private int[] position;

    public TileNodeBFS(SaboteurTile tile, int[] position) {
        this.tile = tile;
        this.position = position;
    }

    public SaboteurTile getTile() {
        return tile;
    }

    public void setTile(SaboteurTile tile) {
        this.tile = tile;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }
}
