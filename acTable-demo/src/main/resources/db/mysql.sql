INSERT IGNORE INTO t_zero ( `id`, `name`, `update_time`, `create_time` ) VALUES ( 2, '''', '2021-07-16 08:53:10', '2021-07-16 08:53:14' );-- fffjjj
/*
测试"
dsadsa
INSERT IGNORE INTO t_zero ( `id`, `name`, `update_time`, `create_time` ) VALUES ( 2, '''', '2021-07-16 08:53:10', '2021-07-16 08:53:14' );
dasdasdsa
*//**//**//**/
INSERT IGNORE INTO t_zero ( `id`, `name`, `update_time`, `create_time` ) VALUES ( 2, "''", '2021-07-16 08:53:10', '2021-07-16 08:53:14' );-- fffjjj/**//**//*

INSERT IGNORE  INTO t_zero(`id`, `name`, `update_time`, `create_time`) values(2, '         \'-- ddd      /*dd*/;/*dd*/', '2021-07-16 08:53:10', '2021-07-16 08:53:14');-- fff
INSERT IGNORE INTO t_zero ( `id`, `name`, `update_time`, `create_time` ) VALUES ( 2, '''', '2021-07-16 08:53:10', '2021-07-16 08:53:14' );
