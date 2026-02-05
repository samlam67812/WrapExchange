package src.main.enums;

public enum Direction {
    BUY(1),
    SELL(0);

    public final int value;

    /**
     * GET negate direction
     */
    public Direction negate() {
        return this == BUY ? SELL : BUY;
    }

    Direction(int value) {
        this .value = value;
    }

    public static Direction of(int intValue) {
        if (intValue == 1) {
            return BUY;
        }

        if (intValue == 0) {
            return SELL;
        }
        throw new IllegalArgumentException("invalid direction value");
    }

}
