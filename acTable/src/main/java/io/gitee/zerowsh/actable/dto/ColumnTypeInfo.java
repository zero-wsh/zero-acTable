package io.gitee.zerowsh.actable.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 类型信息，用于构建建表中的类型
 * 如：varchar(10)
 *
 * @author zero
 */
@Getter
@Setter
@Accessors(chain = true)
public class ColumnTypeInfo {
    private Long length;
    private String typeStr;
    //标记是否找到支持的类型  true为没找到
    private boolean flag;
    private Long decimalLength;
}
