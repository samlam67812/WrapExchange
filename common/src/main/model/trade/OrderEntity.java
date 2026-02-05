package src.main.model.trade;

import src.main.enums.Direction;
import src.main.enums.OrderStatus;

import java.math.BigDecimal;

public class OrderEntity {

    public Long id;
    public long sequenceId;
    public Long userId;

    public BigDecimal price;
    public Direction direction;
    public OrderStatus status;

    public BigDecimal quantity;
    public BigDecimal unfilledQuantity;

    public long createdAt;
    public long updatedAt;
}
