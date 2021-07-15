# zero-acTable

#### 介绍
zero-acTable是基于MP（mybatis plus 后面使用简称）实现的开源项目。您只需要在实体类上标记相关注解就能达到创建数据库的操作，指定初始化脚本就能执行数据初始化，内置测试demo。

#### Maven依赖

```
<dependency>
	<groupId>io.gitee.zero-wsh</groupId>
	<artifactId>acTable</artifactId>
	<version>1.0.0</version>
</dependency>
```



#### 数据库支持
mysql、sql_server

#### 功能介绍

1.  兼容MP 排除字段逻辑（static、transient修饰字段和@TableField注解exist=false）
2.  兼容MP @TableField、@TableName,@TableId注解，并且优先使用MP注解值
3.  兼容MP @TableId注解设置自增
4.  字段没有任何注解，支持属性名转数据库列名（默认驼峰下划线）
5.  支持约束（主键、唯一键、索引、默认值（sql_server才具有））的创建、修改、删除

#### 配置说明

```
#配置实体类的包名，多个用逗号隔开
zero.ac-table.entity-package=io.gitee.zerowsh.actable.demo.entity.mysql
#支持的模式 none：啥也不做（默认值）、ADD_OR_UPDATE：只会新增、修改表结构（注意：不会删除多余的字段）、ADD_OR_UPDATE_OR_DEL：表结构和实体类保持一致（注意：可能会删除表中字段）
zero.ac-table.model=ADD_OR_UPDATE_OR_DEL
#初始化脚本位置resources文件夹下
zero.ac-table.script=db/*.sql
#数据库类型（mysql、sql_server）
zero.ac-table.database-type=mysql
```

#### 注解说明

1.类注解@Table

|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|name   |表名称   |   |   |
|comment   |表注释   |   |   |
|turn   |当字段没有标记@Column注解时，java转数据库的方式   |TurnEnums.DEFAULT   |TurnEnums   |


2.字段注解@Column

|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|exclude   |排除该字段   |false   |true/false   |
|name   |列名称   |   |   |
|comment   |列注释   |   |   |
|length   |字段长度   |255   |   |
|decimalLength   |小数位数   |0   |   |
|isNull   |是否为空   |true   |true/false   |
|isKey   |是否主键   |false   |true/false   |
|isAutoIncrement   |是否自增   |false   |true/false   |
|defaultValue   |默认值   |default_value   |   |
|sqlServerType   |sqlServer字段类型   |SqlServerColumnTypeEnums.DEFAULT   |SqlServerColumnTypeEnums   |
|mysqlType   |mysql字段类型   |MysqlColumnTypeEnums.DEFAULT   |SqlServerColumnTypeEnums   |


3.索引@Index

|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |索引名后缀   |   |   |
|columns   |列名   |   |   |


4.唯一键@Unique

|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |唯一键后缀   |   |   |
|columns   |列名   |   |   |
