package src.main.enums;

public enum OrderStatus {

    /**
     * Pending for order complete (unfilledQuantity = quantity)
     */
    PENDING(false),

    /**
     * Fully order complete (unfilledQuantity = 0)
     */
    FULLY_FILLED(true),

    /**
     * Partial order complete (quantity > unfilledQuantity > 0)
     */
    PARTIAL_FILLED(false),

    /**
     * Partial order complete and then cancel (quantity > unfilledQuantity > 0)
     */
    PARTIAL_CANCELLED(true),

    /**
     * Fully order cancel (quantity = unfilledQuantity)
     */
    FULLY_CANCELLED(true);

    public final boolean isFinalStatus;

    OrderStatus(boolean isFinalStatus) {
        this.isFinalStatus = isFinalStatus;
    }
}
