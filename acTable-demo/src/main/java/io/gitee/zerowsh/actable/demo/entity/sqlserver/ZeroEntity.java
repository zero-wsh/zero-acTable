package io.gitee.zerowsh.actable.demo.entity.sqlserver;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.gitee.zerowsh.actable.annotation.AcColumn;
import io.gitee.zerowsh.actable.annotation.AcTable;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
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
    @AcColumn(value = "id", comment = "主键", isKey = true, isAutoIncrement = true)
    private Long id;

    @AcColumn(value = "name", comment = "名称", length = 20)
    private String name;

    @AcColumn(value = "create_time", comment = "创建时间", length = 5, defaultValue = "getdate()")
    private Timestamp createTime;

    @AcColumn(value = "update_time", comment = "修改时间")
    private Timestamp updateTime;

    @AcColumn(type = ColumnTypeConstants.VARBINARY_MAX)
    private byte[] zero;
}
