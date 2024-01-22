package io.gitee.zerowsh.actable.demo.entity;

import io.gitee.zerowsh.actable.annotation.AcColumn;
import io.gitee.zerowsh.actable.annotation.Index;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * @author zero
 */
@Getter
@Setter
@Index(columnArr = {})
public class BaseEntity {
    @AcColumn(value = "id", comment = "主键", isKey = true, isAutoIncrement = true, order = 0)
    private Long id;

    @AcColumn(value = "create_time", comment = "创建时间", length = 5, defaultValue = "CURRENT_TIMESTAMP(5)", order = 100)
    private Timestamp createTime;

    @AcColumn(value = "update_time", comment = "修改时间", order = 101)
    private Timestamp updateTime;
}
