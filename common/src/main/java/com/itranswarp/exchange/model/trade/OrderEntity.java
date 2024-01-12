package com.itranswarp.exchange.model.trade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.trade.support.EntitySupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.criteria.Order;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class OrderEntity implements EntitySupport, Comparable<OrderEntity>{

    /**
     * Primary key: assigned order id.
     */
    public Long id;

    /**
     * event id (a.k.a sequenceId) that create this order. ASC only.
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Order direction: buy / sell
     */
    public Direction direction;

    /**
     * Order status.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public OrderStatus status;

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updatedAt) {
        this.version++;
        this.unfilledQuantity = unfilledQuantity;
        this.status = status;
        this.updatedAt = updatedAt;
        this.version++;
    }

    /**
     * User id of this order.
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * The limit-order price. MUST NOT change after insert.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    /**
     * Updated time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long updatedAt;

    private int version;

    @JsonIgnore
    @Transient
    public int getVersion() {
        return this.version;
    }

    /**
     * The order quantity. MUST NOT change after insert.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * How much unfilled during match.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal unfilledQuantity;

    @Nullable
    public OrderEntity copy() {
        OrderEntity entity = new OrderEntity();
        int ver = this.version;
        entity.status = this.status;
        entity.unfilledQuantity = this.unfilledQuantity;
        entity.updatedAt = this.updatedAt;
        if(ver != this.version) {
            return null;
        }

        entity.createdAt = this.createdAt;
        entity.updatedAt = this.updatedAt;
        entity.id = this.id;
        entity.price = this.price;
        entity.quantity = this.quantity;
        entity.sequenceId = this.sequenceId;
        entity.userId = this.userId;
        return entity;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj instanceof OrderEntity) {
            OrderEntity e = (OrderEntity) obj;
            return this.id.longValue() == e.id.longValue();
        }
        return super.equals(obj);
    }

    @Override
    public int compareTo(@NotNull OrderEntity o) {
        return 0;
    }
}
