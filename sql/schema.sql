-- ============================================================
-- FlashMall 闪购商城 - 数据库建表脚本
-- 数据库: flashmall
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `flashmall` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `flashmall`;

-- -----------------------------------------------------------
-- 1. 用户表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname`    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-正常 1-禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- -----------------------------------------------------------
-- 2. 商品表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_goods`;
CREATE TABLE `t_goods` (
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `goods_name`   VARCHAR(256)   NOT NULL COMMENT '商品名称',
    `goods_img`    VARCHAR(512)   DEFAULT NULL COMMENT '商品图片URL',
    `goods_detail` TEXT           DEFAULT NULL COMMENT '商品详情',
    `goods_price`  DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    `status`       TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0-下架 1-上架',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- -----------------------------------------------------------
-- 3. 库存表（独立于商品表，避免锁竞争）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_stock`;
CREATE TABLE `t_stock` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    `goods_id`    BIGINT   NOT NULL COMMENT '商品ID',
    `stock`       INT      NOT NULL DEFAULT 0 COMMENT '库存数量',
    `version`     INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- -----------------------------------------------------------
-- 4. 秒杀商品表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_goods`;
CREATE TABLE `t_seckill_goods` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '秒杀商品ID',
    `goods_id`      BIGINT         NOT NULL COMMENT '商品ID',
    `seckill_price` DECIMAL(10, 2) NOT NULL COMMENT '秒杀价格',
    `stock_count`   INT            NOT NULL DEFAULT 0 COMMENT '秒杀库存数量',
    `start_time`    DATETIME       NOT NULL COMMENT '秒杀开始时间',
    `end_time`      DATETIME       NOT NULL COMMENT '秒杀结束时间',
    `status`        TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0-未开始 1-进行中 2-已结束',
    `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀商品表';

-- -----------------------------------------------------------
-- 5. 订单表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`     VARCHAR(64)    NOT NULL COMMENT '订单编号（雪花算法）',
    `user_id`      BIGINT         NOT NULL COMMENT '用户ID',
    `goods_id`     BIGINT         NOT NULL COMMENT '商品ID',
    `goods_name`   VARCHAR(256)   NOT NULL COMMENT '商品名称（冗余）',
    `goods_price`  DECIMAL(10, 2) NOT NULL COMMENT '商品单价',
    `quantity`     INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
    `total_amount` DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    `status`       TINYINT        NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付 1-已支付 2-已取消 3-已退款',
    `pay_time`     DATETIME       DEFAULT NULL COMMENT '支付时间',
    `create_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- -----------------------------------------------------------
-- 6. 秒杀订单表（唯一索引防重复秒杀）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `t_seckill_order`;
CREATE TABLE `t_seckill_order` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '秒杀订单ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `goods_id`    BIGINT   NOT NULL COMMENT '秒杀商品ID',
    `order_id`    BIGINT   NOT NULL COMMENT '关联订单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_goods` (`user_id`, `goods_id`) COMMENT '防止重复秒杀'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀订单表';
