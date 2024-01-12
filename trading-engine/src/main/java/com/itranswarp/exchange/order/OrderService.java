package com.itranswarp.exchange.order;

import com.itranswarp.exchange.assets.AssetService;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.model.trade.OrderEntity;
import jakarta.persistence.criteria.Order;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OrderService {
    final AssetService assetService;

    @Autowired
    public OrderService(AssetService assetService) {
        this.assetService = assetService;
    }
    // track all active orders:
    final ConcurrentMap<Long, OrderEntity> activeOrders = new ConcurrentHashMap<>();

    // track al user orders:
    final ConcurrentMap<Long, ConcurrentMap<Long, OrderEntity>> userOrders = new ConcurrentHashMap<>();

    /**
     *  Create order , failed return null:
     */
    public OrderEntity createOrder(long sequenceId, long ts, Long orderId, Long userId, Direction direction,
                                   BigDecimal price, BigDecimal quantity) {
        switch (direction) {
            case BUY -> {
                // buy, freeze USD:
                if(!assetService.tryFreeze(userId, AssetEnum.USD, price.multiply(quantity))) {
                    return null;
                }
            }
            case SELL ->  {
                // sell, freeze BTC:
                if(!assetService.tryFreeze(userId, AssetEnum.BTC, price.multiply(quantity))) {
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("Invalid Direction");
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
        // add to ActiveOrders:
        this.activeOrders.put(order.id, order);
        // add to UserOrders:
        ConcurrentMap<Long, OrderEntity> uOrders = this.userOrders.get(userId);
        if(uOrders == null) {
            uOrders = new ConcurrentHashMap<>();
            this.userOrders.put(userId, uOrders);
        }
        uOrders.put(order.id, order);
        return order;
    }

    public ConcurrentMap<Long, OrderEntity> getActiveOrders() {
        return this.activeOrders;
    }

    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }

    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    // delete active order
    public void removeOrder(Long orderId) {
        // remove from ActiveOrders
        OrderEntity removed = this.activeOrders.remove(orderId);
        if(removed == null) {
            throw new IllegalArgumentException("Order not found by orderId in active orders:" + orderId);
        }
        // remove from UserOrders
        ConcurrentMap<Long, OrderEntity> uOrders = userOrders.get(removed.userId);
        if(uOrders == null) {
            throw new IllegalArgumentException("Order not found by orderId by userId:" + removed.userId);
        }
        if(uOrders.remove(orderId) == null) {
            throw new IllegalArgumentException("Order not found by orderId in user orders: " + orderId);
        }
    }

    public void debug() {
        System.out.println("------ orders -------");
        List<OrderEntity> orders = new ArrayList<>(this.activeOrders.values());
        Collections.sort(orders);
        for (OrderEntity order : orders) {
            System.out.println("  " + order.id + " " + order.direction + " price: " + order.price + " unfilled: "
                    + order.unfilledQuantity + " quantity: " + order.quantity + " sequenceId: " + order.sequenceId
                    + " userId: " + order.userId);
        }
        System.out.println("----- // orders -----");
    }
}
