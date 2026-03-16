USE `flashmall`;

INSERT INTO `t_user` (`username`, `password`, `nickname`, `phone`, `email`, `status`) VALUES
('admin', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGM/TEZyj9C6', '管理员', '13800000001', 'admin@flashmall.com', 0),
('testuser1', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGM/TEZyj9C6', '测试用户1', '13800000002', 'test1@flashmall.com', 0),
('testuser2', '$2a$10$N.ZOn9G6/YLFixAOPMg/h.z7pCu6v2XyFDtC4q.jeeGM/TEZyj9C6', '测试用户2', '13800000003', 'test2@flashmall.com', 0);

INSERT INTO `t_goods` (`goods_name`, `goods_img`, `goods_detail`, `goods_price`, `status`) VALUES
('iPhone 15 Pro Max', 'https://example.com/iphone15.jpg', 'Apple iPhone 15 Pro Max 256GB 原色钛金属', 9999.00, 1),
('MacBook Pro 14', 'https://example.com/macbook14.jpg', 'Apple MacBook Pro 14英寸 M3 Pro芯片', 14999.00, 1),
('AirPods Pro 2', 'https://example.com/airpods.jpg', 'Apple AirPods Pro 第二代 USB-C接口', 1799.00, 1),
('iPad Air 5', 'https://example.com/ipadair5.jpg', 'Apple iPad Air 5 64GB WiFi版', 4399.00, 1),
('Apple Watch S9', 'https://example.com/watchs9.jpg', 'Apple Watch Series 9 GPS 45mm', 3299.00, 1);

INSERT INTO `t_stock` (`goods_id`, `stock`, `version`) VALUES
(1, 1000, 0),
(2, 500, 0),
(3, 2000, 0),
(4, 800, 0),
(5, 1500, 0);

INSERT INTO `t_seckill_goods` (`goods_id`, `seckill_price`, `stock_count`, `start_time`, `end_time`, `status`) VALUES
(1, 7999.00, 100, '2026-03-05 10:00:00', '2026-03-05 12:00:00', 0),
(3, 999.00, 200, '2026-03-05 14:00:00', '2026-03-05 16:00:00', 0);
