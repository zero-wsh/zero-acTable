package io.gitee.zerowsh.actable.demo.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableName;
import io.gitee.zerowsh.actable.annotation.AcColumn;
import io.gitee.zerowsh.actable.annotation.AcTable;
import io.gitee.zerowsh.actable.annotation.Unique;
import io.gitee.zerowsh.actable.demo.entity.BaseEntity;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;
import lombok.Getter;
import lombok.Setter;

/**
 * @author zero
 */
@Getter
@Setter
@TableName("t_zero")
@AcTable(name = "t_zero", comment = "测试")
public class ZeroEntity extends BaseEntity {
    @AcColumn(name = "name", comment = "名称",
            length = 20, isNull = false,
            isKey = true, order = 1,type = ColumnTypeEnums.NVARCHAR)
    private String name;


    @AcColumn(type=ColumnTypeEnums.LONGTEXT)
    private String zero;

    @AcColumn
    @Unique(value = "test_test2",columns = {"test","test2"})
    private String test;

    @AcColumn
    private String test2;


}
