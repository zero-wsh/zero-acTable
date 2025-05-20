package io.gitee.zerowsh.actable.dto;

import io.gitee.zerowsh.actable.annotation.*;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * sql脚本历史记录表
 *
 * @author zero
 */
@Getter
@Setter
@AcTable(name = "tb_ac_history", comment = "sql脚本历史记录表")
@IndexArr({
        @Index(type = Index.IndexEnums.UK, columnArr = {@IndexColumn("execScript"), @IndexColumn("fileName")}),
})
public class AcHistoryTable {

    @AcColumn(comment = "主键", isKey = true, isAutoIncrement = true, type = ColumnTypeConstants.BIGINT)
    private Long id;

    @AcColumn(comment = "脚本文件名称")
    private String fileName;

    @AcColumn(comment = "脚本文件md5")
    private String fileMd5;

    @AcColumn(comment = "创建时间", length = 0)
    private Date createTime;

    @AcColumn(comment = "执行脚本时机", length = 6)
    private String execScript;

    @AcColumn(comment = "修改时间", length = 0)
    private Date updateTime;

}
