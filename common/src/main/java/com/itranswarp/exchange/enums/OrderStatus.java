package com.itranswarp.exchange.enums;

public enum OrderStatus {

    /**
     *  (unfilledQuantity == quantity)
     */
    PENDING(false),

    /**
     *  (unfilledQuantity = 0)
     */
    FULL_FILLED(true),

    /**
     *  (quantity > unfilledQuantity > 0)
     */
    PARTIAL_FILLED(false),

    /**
     *  (quantity > unfilledQuantity > 0)
     */
    PARTIAL_CANCELLED(true),

    /**
     *  (unfilledQuantity == quantity)
     */
    FULLY_CANCELLED(true);

    public final boolean isFinalStatus;
    OrderStatus(boolean isFinalStatus) {
        this.isFinalStatus = isFinalStatus;
    }
}
