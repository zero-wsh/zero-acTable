package io.gitee.zerowsh.actable.demo.entity.sqlserver;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.gitee.zerowsh.actable.annotation.AcColumn;
import io.gitee.zerowsh.actable.annotation.AcTable;
import io.gitee.zerowsh.actable.annotation.Index;
import io.gitee.zerowsh.actable.annotation.Unique;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * @author zero
 */
@Getter
@Setter
@TableName("t_zero")
@AcTable(name = "t_zero", comment = "测试")
public class ZeroEntity extends Model<ZeroEntity> {
    @AcColumn(name = "id", comment = "主键", isKey = true, isAutoIncrement = true)
    private Long id;

    @AcColumn(name = "name", comment = "名称", length = 20)
    private String name;

    @AcColumn(name = "create_time", comment = "创建时间", length = 5, defaultValue = "getdate()")
    @Unique
    @Index(columns = {"update_time", "create_time"})
    private Timestamp createTime;

    @AcColumn(name = "update_time", comment = "修改时间")
    @Index
    private Timestamp updateTime;

    @AcColumn(type = ColumnTypeEnums.VARBINARY_MAX)
    private byte[] zero;
}
