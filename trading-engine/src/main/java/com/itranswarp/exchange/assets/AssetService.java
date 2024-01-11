package com.itranswarp.exchange.assets;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.support.LoggerSupport;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AssetService extends LoggerSupport {
    // UserId -> Map(AssetEnum -> Assets[available/frozen]

    final ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> userAssets = new ConcurrentHashMap<>();

    public Asset getAsset(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> assets = userAssets.get(userId);
        if (assets == null) {
            return null;
        }
        return assets.get(assetId);
    }
    public  Asset initAssets(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> map = userAssets.get(userId);
        if(map == null) {
            map = new ConcurrentHashMap<>();
            userAssets.put(userId, map);
        }
        Asset zeroAsset = new Asset();
        map.put(assetId, zeroAsset);
        return zeroAsset;
    }


    public boolean tryTransfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount, boolean checkBalance)
            throws IllegalAccessException {
        if(amount.signum() == 0) {
            return true;
        }
        // transfer amount can not be negative
        if(amount.signum() < 0) {
            throw new IllegalAccessException("Negative amount");
        }
        // get User Assets
        Asset fromAsset = getAsset(fromUser, assetId);
        if(fromAsset == null) {
            // init toUser asset if assets not exists
            fromAsset = initAssets(fromUser, assetId);
        }
        Asset toAsset = getAsset(toUser, assetId);
        if (toAsset == null) {
            toAsset = initAssets(toUser, assetId);
        }
        return switch(type) {
            case AVAILABLE_TO_AVAILABLE -> {
                // Balance is not enough
                if(checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            case AVAILABLE_TO_FROZEN -> {
                // not enough balance
                if(checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield  true;
            }
            case FROZEN_TO_AVAILABLE -> {
                if(checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.frozen = fromAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            default -> {
                throw new IllegalAccessException("invalid type: " + type);
            }
        };
    }

    public void transfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount) throws IllegalAccessException {
        if(!tryTransfer(type, fromUser, toUser, assetId, amount, true)) {
            throw new RuntimeException("Transfer failed");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("transfer asset {}, from {} => {}, amount {}", assetId, fromUser, toUser, amount);
        }
    }

    public boolean tryFreeze(Long userId, AssetEnum assetId, BigDecimal amount) throws IllegalAccessException {
        boolean ok = tryTransfer(Transfer.AVAILABLE_TO_FROZEN, userId, userId, assetId, amount, true);
        if (ok && logger.isDebugEnabled()) {
            logger.debug("freezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
        return ok;
    }

    public void unFreeze(Long userId, AssetEnum assetId, BigDecimal amount) throws IllegalAccessException {
        if (!tryTransfer(Transfer.FROZEN_TO_AVAILABLE, userId, userId, assetId, amount, true)) {
            throw new RuntimeException("Unfreeze failed");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("unfreezed user {}, asset {}, amount {}", userId, assetId, amount);
        }
    }

    public void debug() {
        logger.debug("------------ assets ------------");
        List<Long> userIds = new ArrayList<>(userAssets.keySet());
        Collections.sort(userIds);
        for (Long userId: userIds) {
            logger.debug(" user: " + userId + "------------");
            Map<AssetEnum, Asset> assets = userAssets.get(userId);
            List<AssetEnum> assetIds = new ArrayList<>(assets.keySet());
            Collections.sort(assetIds);
            for(AssetEnum assetId : assetIds) {
                logger.debug("      " + assetId + ":"  + assets.get(assetId));
            }
        }
        logger.debug("------------// assets ------------");
    }
}
