package com.itranswarp.exchange.clearing;

import com.itranswarp.exchange.assets.AssetService;
import com.itranswarp.exchange.assets.Transfer;
import com.itranswarp.exchange.match.MatchDetailRecord;
import com.itranswarp.exchange.match.MatchResult;
import com.itranswarp.exchange.orders.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import src.main.enums.AssetEnum;
import src.main.model.trade.OrderEntity;
import src.main.support.LoggerSupport;

import java.math.BigDecimal;

@Component
public class ClearingService extends LoggerSupport {
    final AssetService assetService;
    final OrderService orderService;
    
    @Autowired
    public ClearingService(AssetService assetService, OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    public void clearMatchResult(MatchResult result) {
        OrderEntity taker = result.takerOrder;
        switch (taker.direction) {
            case BUY -> {
                // maker price
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    if (taker.price.compareTo(maker.price) > 0) {
                        BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matched);
                        assetService.unfreeze(taker.userId, AssetEnum.USD, unfreezeQuote);
                    }
                    // from buyer USC seller ac
                    assetService.tryTransfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.USD, maker.price.multiply(matched));
                    // from seller BTC to seller
                    assetService.tryTransfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.BTC, matched);
                    // delete fully completed maker
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            case SELL -> {
                for (MatchDetailRecord detail : result.matchDetails) {
                    OrderEntity maker = detail.makerOrder();
                    BigDecimal matched = detail.quantity();
                    // seller BTC to buyer :
                    assetService.tryTransfer(Transfer.FROZEN_TO_AVAILABLE, taker.userId, maker.userId, AssetEnum.BTC, matched);
                    // buyer USD to seller:
                    assetService.tryTransfer(Transfer.FROZEN_TO_AVAILABLE, maker.userId, taker.userId, AssetEnum.USD, maker.price.multiply(matched));
                    // delete fully complete maker:
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.id);
                    }
                }
                // delete fully complete Taker:
                if (taker.unfilledQuantity.signum() == 0) {
                    orderService.removeOrder(taker.id);
                }
            }
            default -> throw new IllegalArgumentException("Invalid direction");
        }
    }

    public void clearCancelOrder(OrderEntity order) {
        switch (order.direction) {
            case BUY -> {
                assetService.unfreeze(order.userId, AssetEnum.USD, order.price.multiply(order.unfilledQuantity));
            }
            case SELL -> {
                assetService.unfreeze(order.userId, AssetEnum.BTC, order.unfilledQuantity);
            }
            default -> throw new IllegalArgumentException("Invalid direction.");
        }
        orderService.removeOrder(order.id);
    }
}
