package io.gitee.zerowsh.actable.demo.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.gitee.zerowsh.actable.annotation.Column;
import io.gitee.zerowsh.actable.annotation.Index;
import io.gitee.zerowsh.actable.annotation.Table;
import io.gitee.zerowsh.actable.annotation.Unique;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * @author zero
 */
@Getter
@Setter
@TableName("t_zero")
@Table(name = "t_zero", comment = "测试")
public class ZeroEntity extends Model<ZeroEntity> {
    @Column(name = "id", comment = "主键", isKey = true, isAutoIncrement = true)
    private Long id;

    @Column(name = "name", comment = "名称", length = 20, isNull = false, isKey = true)
    private String name;

    @Column(name = "create_time", comment = "创建时间", length = 5, defaultValue = "CURRENT_TIMESTAMP(5)")
    @Unique
    @Index(columns={"update_time","create_time"})
    private Timestamp createTime;

    @Column(name = "update_time", comment = "修改时间")
    @Index
    private Timestamp updateTime;

    @Column(exclude = true)
    private Long zero;


}
