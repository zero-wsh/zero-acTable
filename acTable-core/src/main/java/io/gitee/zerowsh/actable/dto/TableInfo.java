package io.gitee.zerowsh.actable.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import io.gitee.zerowsh.actable.emnus.ColumnTypeEnums;

import java.util.List;

/**
 * 表信息
 * @author zero
 */
@Getter
@Setter
@Builder
public class TableInfo {
    private String name;
    private String comment;
    //主键集合，可能用到
    private List<String> keyList;
    /**
     * 字段信息
     */
    private List<PropertyInfo> propertyInfoList;
    /**
     * 索引信息
     */
    private List<IndexInfo> indexInfoList;
    /**
     * 唯一键信息
     */
    private List<UniqueInfo> uniqueInfoList;

    @Getter
    @Setter
    @Builder
    public static class PropertyInfo {
        private String columnName;
        private boolean isKey;
        private ColumnTypeEnums type;
        private int length;
        private int decimalLength;
        private boolean isNull;
        private boolean isAutoIncrement;
        private String defaultValue;
        private String columnComment;
    }

    @Getter
    @Setter
    @Builder
    public static class IndexInfo {
        private String value;
        private String[] columns;
    }

    @Getter
    @Setter
    @Builder
    public static class UniqueInfo {
        private String value;
        private String[] columns;
    }
}
