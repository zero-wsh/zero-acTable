package io.gitee.zerowsh.actable.dto;

import lombok.*;

import java.util.List;

/**
 * 表信息
 *
 * @author zero
 */
@Getter
@Setter
@Builder
public class TableInfo {
    private String name;
    private String comment;
    //表编码 暂时没用
    private String characterSetName;
    /**
     * 主键集合
     */
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
     * 唯一索引信息
     */
    private List<UniqueIndexInfo> uniqueIndexInfoList;
    /**
     * 唯一约束信息
     */
    private List<UniqueInfo> uniqueInfoList;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyInfo {
        private String tableName;
        //表注释
        private String tableComment;
        //表编码 暂时没用
        private String tableCharacterSetName;
        //字段名称
        private String columnName;
        private String columnComment;
        //字段编码 暂时没用
        private String columnCharacterSetName;
        private String oldColumnName;
        private boolean isKey;
        private int order;
        private String typeStr;
        private boolean typeLimit;
        private long length;
        private long decimalLength;
        private boolean isNull;
        private boolean isAutoIncrement;
        private String defaultValue;
    }

    @Getter
    @Setter
    @Builder
    public static class Index {
        /**
         * 要建立索引的字段名
         */
        String column;

        /**
         * true正序 false倒序，默认正序
         */
        boolean asc;
    }

    @Getter
    @Setter
    @Builder
    public static class IndexInfo {
        //索引名称
        private String value;
        private List<Index> columns;
    }

    @Getter
    @Setter
    @Builder
    public static class UniqueIndexInfo {
        private String value;
        private List<Index> columns;
    }

    @Getter
    @Setter
    @Builder
    public static class UniqueInfo {
        private String value;
        private List<Index> columns;
    }
}
