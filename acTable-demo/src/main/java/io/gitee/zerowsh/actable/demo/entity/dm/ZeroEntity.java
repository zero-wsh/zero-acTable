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
@UpdateColumnName(value = {"test1->test5"})
@IndexArr({
        @Index(type = Index.IndexEnums.IDX, value = "realA",
                columnArr = {@IndexColumn(value = "realName", asc = false)
                }),
        @Index(type = Index.IndexEnums.UK, value = "realB", columnArr = {@IndexColumn("realName"), @IndexColumn("zero")}),
        @Index(type = Index.IndexEnums.UK, value = "realD", columnArr = {@IndexColumn("zero")}),
        @Index(type = Index.IndexEnums.UK_IDX, value = "realC", columnArr = {@IndexColumn("realName")})
})
//@Index(type = Index.IndexEnums.UK, value = "realNameZero2", columnArr = {@IndexColumn("test1")})
public class ZeroEntity extends BaseEntity {

    @AcColumn(comment = "名称",
            length = 20, order = 1,
            type = ColumnTypeConstants.VARCHAR)
    private String realName;


    @AcColumn(type = ColumnTypeConstants.INT, length = 4, isNull = false)
    private Integer zero;

    @AcColumn(value = "test2")
    private String test1;

//    @AcColumn(oldName = "test1", value = "test3")
    private Short test3;

    @AcColumn(type = ColumnTypeConstants.BIT, defaultValue = "1")
    private Short test222;


    @AcColumn(comment = "测试", type = ColumnTypeConstants.DECIMAL, length = 10, decimalLength = 2)
    private BigDecimal ddd;


    @AcColumn(comment = "测试2", type = "DECIMAL(10,3)", typeLimit = false)
    private BigDecimal cc;

}
