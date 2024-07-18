# zero-acTable

#### 介绍
zero-acTable是基于实体类创建表的开源项目。您只需要在实体类上标记相关注解就能达到创建数据库的操作，指定初始化脚本就能执行数据初始化，内置测试demo。

#### 数据库支持
mysql、达梦
#### 功能介绍
- 兼容MP 排除字段逻辑（static、transient修饰字段和@TableField注解exist=false）
- 兼容MP @TableField、@TableName,@TableId注解
- 兼容MP @TableId注解设置自增
- 兼容hibernate @Table、@Column、@Id、@Transient注解
- 兼容hibernate @GeneratedValue注解设置自增
- 兼容swagger @ApiModel、@ApiModelProperty注解设置（表、字段）备注
- 字段上没有任何注解，支持属性名转数据库列名（默认驼峰下划线），字段类型转数据库类型
- 支持约束（主键、唯一键、索引、默认值（sql_server才具有））的创建、修改、删除
- 支持数据初始化
- 忽略表@IgnoreTable，自动建表时指定包下面需要忽略的表
- 排除父类字段@ExcludeSuperField，自动建表时排除父类相关字段
- 增加修改字段逻辑，可指定在类上或者字段上，字段优先于类上
- 增加typeLimit属性，可自定义未支持的类型
- 调整唯一键、索引标记位置，统一在类上标记

### 您只需两步即可集成
#### 1、Maven依赖

```
<dependency>
	<groupId>io.gitee.zero-wsh</groupId>
	<artifactId>acTable</artifactId>
	<version>3.0.4</version>
</dependency>
```

#### 2、配置

```
#配置实体类的包名，多个用逗号隔开
zero.ac-acTable.entity-package=io.gitee.zerowsh.actable.demo.entity.mysql
#支持的模式（默认NONE）
zero.ac-acTable.model=ADD_OR_UPDATE_OR_DEL
#建表之前执行的sql脚本
zero.ac-acTable.before-script=db/*.sql
#建表之后执行的sql脚本
zero.ac-acTable.after-script=db/*.sql
```

#### 注解说明


1、类注解@AcTable，设置表相关信息
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|name   |表名称   |   |   |
|comment   |表注释   |   |   |
|turn   |当字段没有标记@AcColumn注解时，java转数据库的方式   |TurnEnums.DEFAULT   |TurnEnums   |

2、字段注解@AcColumn，设置列名相关信息
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|exclude   |排除该字段   |false   |true/false   |
|value|列名称   |   |   |
|order   |字段排序   |0   |整数   |
|comment   |列注释   |   |   |
|length   |字段长度   |255   |   |
|decimalLength   |小数位数   |0   |   |
|isNull   |是否为空   |true   |true/false   |
|isKey   |是否主键   |false   |true/false   |
|isAutoIncrement   |是否自增   |false   |true/false   |
|defaultValue   |默认值   |default_value   |   |
|type   |字段类型   |ColumnTypeEnums.DEFAULT   |ColumnTypeEnums   |
|oldName|数据库以前字段名   |||
|typeLimit|是否限制字段类型为支持的类型，如果不限制可自定义类型  |true|true/false|

3、索引@Index，设置表索引
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |索引名后缀，前缀固定idx_   |   |   |
|type|类型|  IndexEnums.IDX | IndexEnums  |
|columnArr|索引字段和排序   |   |   |
|message|冲突时提示信息，结和业务代码使用|  | |

4、索引字段@IndexColumn
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |索引字段名，该值和实体类属性保持一致|   |   |
|asc|排序，true正序 false倒序|  false| true/false|

5、唯一键@Unique，设置表唯一键
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |唯一键后缀，前缀固定uk_   |   |   |
|columns   |列名   |   |   |

6、@ExcludeSuperField，排除父级字段
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |排除父类相关字段   |   |   |

7、@IgnoreTable，忽略表的注解

8、@UpdateColumnName，修改列名
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |需要修改的列名，采用`->`进行分隔，左边是以前的字段，右边是改过后的字段   |   |   |

#### 其他说明
1、支持的模式
|模式名   |描述   |
|---|---|
|NONE|啥也不做|
|ADD_OR_UPDATE|只会新增、修改表结构，不会删除多余的字段|
|ADD_OR_UPDATE_OR_DEL|表结构和实体类保持一致，可能会删除表中字段|
|DEL_AND_ADD|先删除表（只删除实体类对应表），再新增，注意数据备份|
|DEL_ALL_AND_ADD|先删除库中所有表，再新增，注意数据备份|
#### 注意事项
- 有初始化脚本时，必须保证可重复执行，多个插入语句默认使用endFlag隔开
- 有初始化脚本时，在字符串和注释中不要出现sql分割符字样
- 有初始化脚本时，并且使用了druid连接池filters不要配置wall
- 2.*和3.*版本存在兼容问题，并且有些注解进行了调整：①唯一键、索引统一放置在类上；②@AcColumn注解取消name属性，改为value
- 如果有默认值，需自己判断是否为字符串，如果是字符串需要自己加上单引号

#### 联系方式
QQ：254353372
