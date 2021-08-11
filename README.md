# zero-acTable

#### 介绍
zero-acTable是基于实体类创建表的开源项目。您只需要在实体类上标记相关注解就能达到创建数据库的操作，指定初始化脚本就能执行数据初始化，内置测试demo。

#### 数据库支持
mysql、sql_server

#### 功能介绍
1、兼容MP 排除字段逻辑（static、transient修饰字段和@TableField注解exist=false）
2、兼容MP @TableField、@TableName,@TableId注解，并且优先使用MP注解值
3、兼容MP @TableId注解设置自增
4、兼容hibernate @Table、@Column、@Id、@Transient注解
5、兼容hibernate @GeneratedValue注解设置自增
6、兼容swagger @ApiModel、@ApiModelProperty注解设置（表、字段）备注
7、字段上没有任何注解，支持属性名转数据库列名（默认驼峰下划线），字段类型转数据库类型
8、支持约束（主键、唯一键、索引、默认值（sql_server才具有））的创建、修改、删除
9、支持数据初始化（数据库连接需要配置allowMultiQueries=true）
10、忽略表@IgnoreTable，自动建表时指定包下面需要忽略的表
11、排除父类字段@ExcludeSuperField，自动建表时排除父类相关字段
###您只需三步即可集成
#### 1、Maven依赖

```
<dependency>
	<groupId>io.gitee.zero-wsh</groupId>
	<artifactId>acTable</artifactId>
	<version>2.0.0</version>
</dependency>
```

#### 2、配置说明

```
#配置实体类的包名，多个用逗号隔开
zero.ac-acTable.entity-package=io.gitee.zerowsh.actable.demo.entity.mysql
#支持的模式（默认NONE）
zero.ac-acTable.model=ADD_OR_UPDATE_OR_DEL
#初始化脚本位置resources文件夹下
zero.ac-acTable.script=db/*.sql
```

#### 3、注入Bean

```
@Bean
 public AcTableService acTableService(DataSource dataSource, AcTableProperties acTableProperties) {
     return new AcTableService(dataSource, acTableProperties);
 }
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
|name   |列名称   |   |   |
|comment   |列注释   |   |   |
|length   |字段长度   |255   |   |
|decimalLength   |小数位数   |0   |   |
|isNull   |是否为空   |true   |true/false   |
|isKey   |是否主键   |false   |true/false   |
|isAutoIncrement   |是否自增   |false   |true/false   |
|defaultValue   |默认值   |default_value   |   |
|type   |字段类型   |ColumnTypeEnums.DEFAULT   |ColumnTypeEnums   |

3、索引@Index，设置表索引
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |索引名后缀   |   |   |
|columns   |列名   |   |   |

4、唯一键@Unique，设置表唯一键
|属性名   |描述   |默认值   |取值范围   |
|---|---|---|---|
|value   |唯一键后缀   |   |   |
|columns   |列名   |   |   |

#### 注意事项
1、注入acTableService时，请保证业务系统查询数据前执行，否则将导致查询不到表
2、初始化脚本必须保证可重复执行，多个插入语句使用逗号隔开；使用druid配置了filters=wall时，需要设置允许执行多语句

#### 联系方式
QQ：254353372
