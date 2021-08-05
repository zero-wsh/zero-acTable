package io.gitee.zerowsh.actable.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 表和字段信息
 *
 * @author zero
 */
@Getter
@Setter
public class TableColumnInfo {
    private String tableName;
    private String tableComment;
    private String columnName;
    private String columnComment;
    private boolean isKey;
    private String typeStr;
    private int length;
    private int decimalLength;
    private boolean isNull;
    private boolean isAutoIncrement;
    private String defaultValue;
}
