package com.flashmall.common.constant;

public final class OrderStatus {

    private OrderStatus() {
    }

    /** 待支付 */
    public static final int WAIT_PAY = 0;
    /** 已支付 */
    public static final int PAID = 1;
    /** 已取消（用户主动取消） */
    public static final int CANCELED = 2;
    /** 已退款 */
    public static final int REFUNDED = 3;

    /** 库存确认中（秒杀下单后等待库存服务扣减） */
    public static final int PENDING_STOCK = 10;
    /** 库存不足取消 */
    public static final int CANCELED_NO_STOCK = 11;
    /** 支付超时取消 */
    public static final int CANCELED_TIMEOUT = 12;
}
