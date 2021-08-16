IF NOT EXISTS (SELECT 1 FROM t_zero WHERE name = '111')
INSERT INTO [dbo].[t_zero] ([name], [create_time], [update_time]) VALUES ( N'111', '2021-07-07 17:25:17', '2021-07-07 17:25:20');
IF NOT EXISTS (SELECT 1 FROM t_zero WHERE name = '222')
INSERT INTO [dbo].[t_zero] ([name], [create_time], [update_time]) VALUES ( N'222', '2021-07-07 17:25:18', '2021-07-07 17:25:21');
