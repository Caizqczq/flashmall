package com.flashmall.common.event;

public final class EventType {

    private EventType() {
    }

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String STOCK_DEDUCTED = "STOCK_DEDUCTED";
    public static final String STOCK_DEDUCT_FAILED = "STOCK_DEDUCT_FAILED";
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String ORDER_CANCELED = "ORDER_CANCELED";
    public static final String ORDER_CLOSE_DELAY = "ORDER_CLOSE_DELAY";
}
