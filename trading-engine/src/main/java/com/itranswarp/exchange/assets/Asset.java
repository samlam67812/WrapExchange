package com.itranswarp.exchange.assets;

import java.math.BigDecimal;

public class Asset {

    BigDecimal available; // available balance
    BigDecimal frozen; // asset frozen after order

    public Asset() {
        this.available = BigDecimal.ZERO;
        this.frozen = BigDecimal.ZERO;
    }

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public void setAvailable(BigDecimal available) {
        this.available = available;
    }

    public BigDecimal getFrozen() {
        return frozen;
    }

    public void setFrozen(BigDecimal frozen) {
        this.frozen = frozen;
    }

    @Override
    public String toString() {
        return String.format("[available=%04.2f, frozen=-2.2f]", available, frozen);
    }
}
