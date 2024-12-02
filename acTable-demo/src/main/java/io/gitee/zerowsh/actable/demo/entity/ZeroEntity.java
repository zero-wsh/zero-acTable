package io.gitee.zerowsh.actable.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author zero
 */
@Getter
@Setter
@TableName("t_zero")
@AcTable(name = "t_zero", comment = "测试")
//@UpdateColumnName(value = {"test1->test4"})
@IndexArr({
        @Index(type = Index.IndexEnums.IDX, value = "realA",
                columnArr = {@IndexColumn(value = "realName", asc = false)
                }),
        @Index(type = Index.IndexEnums.UK, value = "realB", columnArr = {@IndexColumn("realName"), @IndexColumn("zero")}),
        @Index(type = Index.IndexEnums.UK, value = "realD", columnArr = {@IndexColumn("zero")}),
        @Index(type = Index.IndexEnums.UK_IDX, value = "realC", columnArr = {@IndexColumn("realName")})
})
@Index(type = Index.IndexEnums.UK, value = "realNameZero2", columnArr = {@IndexColumn("test1")})
public class ZeroEntity extends BaseEntity {

    @AcColumn(comment = "名称",
            length = 20, order = 1,
            type = ColumnTypeConstants.VARCHAR)
    private String realName;


    @AcColumn(type = ColumnTypeConstants.INT, length = 4, isNull = false,isKey = true)
    private Integer zero;

    @AcColumn(oldName = "test1", value = "test1", defaultValue = "'test'")
    private String test1;
    @AcColumn(value = "test2",length = 1000)
    private String test2;

    @AcColumn(oldName = "test3", value = "test3")
    private Short test3;

    @AcColumn(type = ColumnTypeConstants.BIT, defaultValue = "1")
    private Short test222;


    @AcColumn(comment = "测试1", type = ColumnTypeConstants.DECIMAL, length = 10, decimalLength = 2)
    private BigDecimal ddd;


    @AcColumn(comment = "测试2", type = "DECIMAL(10,3)", typeLimit = false)
    private BigDecimal cc;


    @AcColumn(comment = "测试3", type = ColumnTypeConstants.DATE)
    private Date date;


    @AcColumn(comment = "测试4", type = ColumnTypeConstants.DATETIME)
    private Date dateTime;


    @AcColumn(comment = "测试5", type = ColumnTypeConstants.TEXT)
    private Date text;


    @AcColumn(comment = "测试6", type = ColumnTypeConstants.LONGTEXT)
    private Date longtext;

    @AcColumn(comment = "测试5", type = ColumnTypeConstants.JSON)
    private String test6;

    @AcColumn(comment = "测试7", length = 1)
    private Boolean isPass;

    @AcColumn(comment = "退款时间", defaultValue = "CURRENT_TIMESTAMP")
    private Timestamp refundTime; // 退款时间

}
