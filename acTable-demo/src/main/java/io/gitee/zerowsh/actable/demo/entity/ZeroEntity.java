package io.gitee.zerowsh.actable.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.gitee.zerowsh.actable.annotation.Column;
import io.gitee.zerowsh.actable.annotation.ExcludeSuperField;
import io.gitee.zerowsh.actable.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@TableName("t_zero")
@Table(name = "t_zero", comment = "测试")
@ExcludeSuperField
public class ZeroEntity extends Model<ZeroEntity> {
    @Column(name = "id", comment = "主键", isKey = true, isAutoIncrement = true)
    private Long id;

    @Column(name = "name", comment = "名称", isKey = true, length = 20)
    private String name;

    @Column(name = "create_time", comment = "创建时间", defaultValue = "getdate()", length = 5)
    private Timestamp createTime;

    @Column(name = "update_time", comment = "修改时间")
    private Timestamp updateTime;

    @Column(exclude = true)
    private Long zero;

    private String zero1;
}
