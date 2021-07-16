INSERT INTO t_zero(`id`, `name`, `update_time`, `create_time`)
SELECT 1, '1', '2021-07-16 08:53:10', '2021-07-16 08:53:13' FROM DUAL WHERE NOT EXISTS(SELECT 1 FROM t_zero WHERE id = '1');
INSERT INTO t_zero(`id`, `name`, `update_time`, `create_time`)
SELECT 2, '1', '2021-07-16 08:53:10', '2021-07-16 08:53:14' FROM DUAL WHERE NOT EXISTS(SELECT 1 FROM t_zero WHERE id = '2');