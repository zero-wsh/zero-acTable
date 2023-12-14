package io.gitee.zerowsh.actable.demo.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableName;
import io.gitee.zerowsh.actable.annotation.AcColumn;
import io.gitee.zerowsh.actable.annotation.AcTable;
import io.gitee.zerowsh.actable.annotation.UpdateColumnName;
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
@UpdateColumnName(value = {"test4->test2"})
public class ZeroEntity extends BaseEntity {

    @AcColumn(name = "name", comment = "名称",
            length = 20, isNull = false,
            isKey = true, order = 1, type = ColumnTypeEnums.NVARCHAR)
    private String name;


    @AcColumn(type = ColumnTypeEnums.LONGTEXT)
    private String zero;

    @AcColumn(value = "test1", oldName = "test")
    private String test1;

    @AcColumn(type = ColumnTypeEnums.YEAR)
    private Short test3;

    @AcColumn(type = ColumnTypeEnums.BIT, defaultValue = "1")
    private Short test222;


    @AcColumn(comment = "测试", type = ColumnTypeEnums.DOUBLE, length = 10, decimalLength = 2)
    private Double ddd;

}
