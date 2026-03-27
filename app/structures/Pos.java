package structures;

import java.util.Objects;

/**
 * Immutable board coordinate (x,y) with distance utilities.
 * Board is 9x5, 1-based indexing (x in [1..9], y in [1..5]).
 */
public final class Pos {
    public final int x;
    public final int y;

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Pos add(int dx, int dy) {
        return new Pos(this.x + dx, this.y + dy);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pos)) return false;
        Pos other = (Pos) o;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Pos(" + x + "," + y + ")";
    }
}
