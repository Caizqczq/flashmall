package com.flashmall.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCodeEnum {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),

    // 参数相关 4xx
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),

    // 用户相关 1xxx
    USER_NOT_EXIST(1001, "用户不存在"),
    USER_ALREADY_EXIST(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USERNAME_OR_PASSWORD_ERROR(1004, "用户名或密码错误"),

    // 商品相关 2xxx
    GOODS_NOT_EXIST(2001, "商品不存在"),
    GOODS_OFF_SHELF(2002, "商品已下架"),

    // 库存相关 3xxx
    STOCK_NOT_ENOUGH(3001, "库存不足"),
    STOCK_NOT_EXIST(3002, "库存记录不存在"),

    // 订单相关 4xxx
    ORDER_NOT_EXIST(4001, "订单不存在"),
    ORDER_ALREADY_EXIST(4002, "重复下单"),
    ORDER_CREATE_FAIL(4003, "订单创建失败"),

    // 秒杀相关 5xxx
    SECKILL_NOT_START(5001, "秒杀活动未开始"),
    SECKILL_ALREADY_END(5002, "秒杀活动已结束"),
    SECKILL_REPEAT(5003, "不可重复秒杀"),
    SECKILL_FAIL(5004, "秒杀失败"),

    // 系统相关 9xxx
    SYSTEM_ERROR(9999, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String message;
}
