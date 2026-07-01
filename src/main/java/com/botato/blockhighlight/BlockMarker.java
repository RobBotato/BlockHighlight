package com.botato.blockhighlight;

public class BlockMarker {
    public int x;
    public int y;
    public int z;
    public String dimension;
    public int hexColor;
    public boolean hidden;
    public String name = "";
    public boolean renderFilled = true;
    public boolean renderOutline = false;
    public boolean renderSeeThrough = false;

    public BlockMarker() {
        this.dimension = "";
    }

    public BlockMarker(int x, int y, int z, String dimension, int hexColor) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.hexColor = hexColor;
        this.hidden = false;
        this.name = "";
    }

    public String displayName() {
        return name == null ? "" : name;
    }
}
