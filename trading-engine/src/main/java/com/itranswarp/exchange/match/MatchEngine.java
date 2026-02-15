package com.itranswarp.exchange.match;

import org.springframework.stereotype.Component;
import src.main.bean.OrderBookBean;
import src.main.enums.Direction;
import src.main.enums.OrderStatus;
import src.main.model.trade.OrderEntity;

import java.math.BigDecimal;

@Component
public class MatchEngine {

    public final OrderBook buyBook = new OrderBook(Direction.BUY);
    public final OrderBook sellBook = new OrderBook(Direction.SELL);
    public BigDecimal marketPrice = BigDecimal.ZERO;
    private long sequenceId;

    public MatchResult processOrder(long sequenceId, OrderEntity order) {
        return switch (order.direction) {
            case BUY -> processOrder(sequenceId, order, this.sellBook, this.buyBook);
            case SELL -> processOrder(sequenceId, order, this.buyBook, this.sellBook);
            default -> throw new IllegalArgumentException("Invalid direction.");
        };
    }

    private MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook markerBook,
                                     OrderBook anotherBook) {
        this.sequenceId = sequenceId;
        long ts = takerOrder.createdAt;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        for (;;) {
            OrderEntity makerOrder = markerBook.getFirst();
            if (makerOrder == null) {
                break;
            }
            if (takerOrder.direction == Direction.BUY && takerOrder.price.compareTo(makerOrder.price) < 0) {
                // buy order price is lower than first sell order
                break;
            } else if (takerOrder.direction == Direction.SELL && takerOrder.price.compareTo(makerOrder.price) > 0) {
                // sell order price is higher than first buy order
                break;
            }
            // deal with maker price
            this.marketPrice= makerOrder.price;
            // lowest among two order
            BigDecimal matchedQuantity = takerUnfilledQuantity.min(makerOrder.unfilledQuantity);
            // order record
            matchResult.add(makerOrder.price, matchedQuantity, makerOrder);
            // update orders (taker, maker) after matching
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchedQuantity);
            BigDecimal makerUnfilledQuantity = makerOrder.unfilledQuantity.subtract(matchedQuantity);
            // delete from order boot if fully filled
            if (makerUnfilledQuantity.signum() == 0) {
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
            } else {
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.PARTIAL_FILLED, ts);
            }
            // Taker fully filled, exit
            if (takerUnfilledQuantity.signum() == 0) {
                takerOrder.updateOrder(takerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                break;
            }
        }
        // Taker partial filled, put in order book
        if (takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatus.PENDING
                            : OrderStatus.PARTIAL_FILLED,
                    ts);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }

    public void cancel(long ts, OrderEntity order) {
        OrderBook book = order.direction == Direction.BUY ? this.buyBook : this.sellBook;
        if (!book.remove(order)) {
            throw new IllegalArgumentException("Order not found in order book.");
        }
        OrderStatus status = order.unfilledQuantity.compareTo(order.quantity) == 0 ? OrderStatus.FULLY_CANCELLED
                : OrderStatus.PARTIAL_CANCELLED;
        order.updateOrder(order.unfilledQuantity, status, ts);
    }

    public OrderBookBean getOrderBook(int maxDepth) {
        return new OrderBookBean(this.sequenceId, this.marketPrice, this.buyBook.getOrderBook(maxDepth),
                this.sellBook.getOrderBook(maxDepth));
    }

    public void debug() {
        System.out.println("---------- match engine ----------");
        System.out.println(this.sellBook);
        System.out.println("  ----------");
        System.out.println("  " + this.marketPrice);
        System.out.println("  ----------");
        System.out.println(this.buyBook);
        System.out.println("---------- // match engine ----------");
    }


}
