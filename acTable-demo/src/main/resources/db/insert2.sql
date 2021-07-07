IF NOT EXISTS (SELECT 1 FROM t_zero WHERE name = '111')
INSERT INTO [dbo].[t_zero] ([name], [create_time], [update_time], [zero1]) VALUES ( N'111', '2021-07-07 17:25:17.00000', '2021-07-07 17:25:20.0000000', N'1');
