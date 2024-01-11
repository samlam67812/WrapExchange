package com.itranswarp.exchange.assets;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;

import org.apache.catalina.User;
import org.aspectj.lang.annotation.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.exchange.enums.AssetEnum;

public class AssetServiceTest {
    static final Long DEBT = 1L;
    static final Long USER_A = 2000L;
    static final Long USER_B = 3000L;
    static final Long USER_C = 4000L;

    AssetService assetService;

    @BeforeEach
    public void setUp() throws IllegalAccessException {
        assetService = new AssetService();
        init();
    }

    @AfterEach
    public void tearDown() {
        verify();
    }
    /**
     * A: USD=12300, BTC=12
     *
     * B: USD=45600
     *
     * C: BTC=34
     */
    void init() throws IllegalAccessException {
        assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, DEBT, USER_A, AssetEnum.USD,
                BigDecimal.valueOf(12300), false);
        assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, DEBT, USER_A, AssetEnum.BTC,
                BigDecimal.valueOf(12), false);

        assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, DEBT, USER_B, AssetEnum.USD,
                BigDecimal.valueOf(45600), false);
        assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, DEBT, USER_C, AssetEnum.BTC,
                BigDecimal.valueOf(34), false);

        assertBDEquals(-57900, assetService.getAsset(DEBT, AssetEnum.USD).available);
        assertBDEquals(-46, assetService.getAsset(DEBT, AssetEnum.BTC).available);
        System.out.println("USER_A USD [available]: " + assetService.getAsset(USER_A, AssetEnum.USD).available);
        System.out.println("USER_A USD [frozen]: " + assetService.getAsset(USER_A, AssetEnum.USD).frozen);
        System.out.println("USER_A BTC [available]: " + assetService.getAsset(USER_A, AssetEnum.BTC).available);

        System.out.println("USER_B USD [available]: " + assetService.getAsset(USER_B, AssetEnum.USD).available);
        System.out.println("USER_C USD [available]: " + assetService.getAsset(USER_C, AssetEnum.BTC).frozen );
        System.out.println("\n");

    }
    void assertBDEquals(long value, BigDecimal bd) {
        assertBDEquals(String.valueOf(value), bd);
    }

    void assertBDEquals(String value, BigDecimal bd) {
        assertTrue(new BigDecimal(value).compareTo(bd) == 0,
                String.format("Expected %s but actual %s", value, bd.toPlainString()));
    }
    void verify() {
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for (Long userId : assetService.userAssets.keySet()) {
            var assetUSD = assetService.getAsset(userId, AssetEnum.USD);
            if(assetUSD != null) {
                totalUSD = totalUSD.add(assetUSD.available).add(assetUSD.frozen);
            }
            var assetBTC = assetService.getAsset(userId, AssetEnum.BTC);
            if(assetBTC != null) {
                totalBTC = totalBTC.add(assetBTC.available).add(assetBTC.frozen);
            }
        }
        assertBDEquals(0, totalUSD);
        assertBDEquals(0, totalBTC);
    }

    @Test
    void tryTransfer() throws IllegalAccessException {
        // A -> B ok:
        assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, USER_A, USER_B, AssetEnum.USD,
                new BigDecimal("12000"), true);
        assertBDEquals(300, assetService.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(12000 + 45600, assetService.getAsset(USER_B, AssetEnum.USD).available);

        // A -> B failed:
        assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, USER_A, USER_B, AssetEnum.USD,
                new BigDecimal("301"), true);
        assertBDEquals(300, assetService.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(12000 + 45600, assetService.getAsset(USER_B, AssetEnum.USD).available);
    }

    @Test
    void tryFreeze() throws IllegalAccessException {
        // freeze 12000 ok:
        assetService.tryFreeze(USER_A, AssetEnum.USD, new BigDecimal("12000"));
        assertBDEquals(300, assetService.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(12000, assetService.getAsset(USER_A, AssetEnum.USD).frozen);

        // freeze 301 failed:
        assertFalse(assetService.tryFreeze(USER_A, AssetEnum.USD, new BigDecimal("301")));

        assertBDEquals(300, assetService.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(12000, assetService.getAsset(USER_A, AssetEnum.USD).frozen);
    }


    @Test
    void unFreeze() throws IllegalAccessException {
        // freeze 12000 ok:
        assetService.tryFreeze(USER_A, AssetEnum.USD, new BigDecimal("12000"));
        assertBDEquals(300, assetService.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(12000, assetService.getAsset(USER_A, AssetEnum.USD).frozen);

        // unfreeze 9000 ok:
        assetService.unFreeze(USER_A, AssetEnum.USD, new BigDecimal("9000"));
        assertBDEquals(9300, assetService.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(3000, assetService.getAsset(USER_A, AssetEnum.USD).frozen);

        // unfreeze 3001 failed:
        assertThrows(RuntimeException.class, () -> {
            assetService.unFreeze(USER_A, AssetEnum.USD, new BigDecimal("3001"));
        });
    }

    @Test
    void transfer() throws IllegalAccessException {
        // A USD -> A frozen:
        assetService.transfer(Transfer.AVAILABLE_TO_FROZEN, USER_A, USER_A, AssetEnum.USD, new BigDecimal("9000"));
        assertBDEquals(3300, assetService.getAsset(USER_A, AssetEnum.USD).available);
        assertBDEquals(9000, assetService.getAsset(USER_A, AssetEnum.USD).frozen);

        // A frozen -> C available:
        assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, USER_A, USER_C, AssetEnum.USD, new BigDecimal("8000"));
        assertBDEquals(1000, assetService.getAsset(USER_A, AssetEnum.USD).frozen);
        assertBDEquals(8000, assetService.getAsset(USER_C, AssetEnum.USD).available);

        // A frozen -> B available failed:
        assertThrows(RuntimeException.class, () -> {
            assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, USER_A, USER_B, AssetEnum.USD, new BigDecimal("1001"));
        });
    }

}
