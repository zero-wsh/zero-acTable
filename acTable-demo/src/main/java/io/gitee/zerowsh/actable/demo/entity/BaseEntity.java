package io.gitee.zerowsh.actable.demo.entity;

import io.gitee.zerowsh.actable.annotation.AcColumn;
import io.gitee.zerowsh.actable.annotation.Index;
import io.gitee.zerowsh.actable.annotation.Unique;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class BaseEntity {
    @AcColumn(name = "id", comment = "主键", isKey = true, isAutoIncrement = true, order = 0)
    private Long id;

    @AcColumn(name = "create_time", comment = "创建时间", length = 5, defaultValue = "CURRENT_TIMESTAMP(5)", order = 100)
    @Unique
    @Index(columns = {"update_time", "create_time"})
    private Timestamp createTime;

    @AcColumn(name = "update_time", comment = "修改时间", order = 101)
    @Index
    private Timestamp updateTime;
}
