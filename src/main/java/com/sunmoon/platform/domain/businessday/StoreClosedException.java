package com.sunmoon.platform.domain.businessday;

/**
 * Thrown when an order arrives for a store that has not 개점'd. The order
 * channel turns this into "매장이 준비중입니다" rather than a failure —
 * a closed shop is a normal state, not an error in the caller.
 */
public class StoreClosedException extends RuntimeException {

    public StoreClosedException(String storeId) {
        super("Store is not open: " + storeId);
    }
}
