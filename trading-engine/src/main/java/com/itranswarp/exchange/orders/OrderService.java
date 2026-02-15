package com.itranswarp.exchange.orders;

import com.itranswarp.exchange.assets.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import src.main.enums.AssetEnum;
import src.main.enums.Direction;
import src.main.model.trade.OrderEntity;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class OrderService {
    final AssetService assetService;

    // trace all active orders: Order ID => OrderEntity
    final ConcurrentHashMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    // trace users active orders: User ID => Map(Order ID => OrderEntity)
    final ConcurrentHashMap<Long, ConcurrentHashMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    @Autowired
    public OrderService(AssetService assetService) {
        this.assetService = assetService;
    }

    public OrderEntity createOrder(long sequenceId, long ts, Long orderId,
                                   Long userId, Direction direction, BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                if (!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL -> {
                if (!assetService.tryFreeze(userId, AssetEnum.BTC, quantity)) {
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction");
        }

        OrderEntity order = new OrderEntity();
        order.id = orderId;
        order.sequenceId = sequenceId;
        order.userId = userId;
        order.direction = direction;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createdAt = order.updatedAt = ts;
        this.activeOrders.put(order.id, order);

        ConcurrentHashMap<Long, OrderEntity> uOrders = this.userOrders.get(userId);
        if (uOrders == null) {
            uOrders = new ConcurrentHashMap<>();
            this.userOrders.put(userId, uOrders);
        }
        uOrders.put(order.id, order);
        return order;
    }

    public void removeOrder(Long orderId) {
        OrderEntity removed = this.activeOrders.remove(orderId);
        if (removed == null) {
            throw new IllegalArgumentException("Order not found by orderId in active orders: " + orderId);
        }
        ConcurrentHashMap<Long, OrderEntity> uOrders = userOrders.get(removed.userId);
        if (uOrders == null) {
            throw new IllegalArgumentException("User orders not found by userId: " + removed.userId);
        }
        if (uOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }

    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentHashMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

}
