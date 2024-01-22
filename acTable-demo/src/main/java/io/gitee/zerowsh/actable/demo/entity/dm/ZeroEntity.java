package io.gitee.zerowsh.actable.demo.entity.dm;

import com.baomidou.mybatisplus.annotation.TableName;
import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.gitee.zerowsh.actable.demo.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author zero
 */
@Getter
@Setter
@TableName("t_zero")
@AcTable(name = "t_zero", comment = "测试")
@UpdateColumnName(value = {"test4->test2"})
@IndexArr({
        @Index(type = Index.IndexEnums.IDX, value = "realNameZero",
                columnArr = {@IndexColumn("realName")
                }),
        @Index(type = Index.IndexEnums.UK, value = "realNameZero", columnArr = {@IndexColumn("realName")}),
        @Index(type = Index.IndexEnums.UK_IDX, value = "realNameZero", columnArr = {@IndexColumn("realName")})
})
@Index(type = Index.IndexEnums.UK_IDX, value = "realNameZero2", columnArr = {@IndexColumn("id")})
public class ZeroEntity extends BaseEntity {

    @AcColumn(comment = "名称",
            length = 20, order = 1,
            type = ColumnTypeConstants.VARCHAR)
    private String realName;


    @AcColumn(type = ColumnTypeConstants.INT, length = 4)
    private Integer zero;

    @AcColumn(value = "test1", oldName = "test")
    private String test1;

    @AcColumn
    private Short test3;

    @AcColumn(type = ColumnTypeConstants.BIT, defaultValue = "1")
    private Short test222;


    @AcColumn(comment = "测试", type = ColumnTypeConstants.DECIMAL, length = 10, decimalLength = 2)
    private BigDecimal ddd;

}
