package io.gitee.zerowsh.actable.demo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.gitee.zerowsh.actable.annotation.AcColumn;
import io.gitee.zerowsh.actable.constant.ColumnTypeConstants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;


/**
 * 实体基类
 *
 * @author zero
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @AcColumn(comment = "主键", type = ColumnTypeConstants.INT, order = -10)
    @TableId(type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Integer id;

    @AcColumn(comment = "创建人ID", order = 100)
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty("创建人ID")
    private Integer createUserId;

    @AcColumn(comment = "创建时间", order = 100)
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private Date createTime;

    @AcColumn(comment = "修改人ID", length = 50, order = 100)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("修改人ID")
    private Integer updateUserId;

    @AcColumn(comment = "修改时间", order = 100)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("修改时间")
    private Date updateTime;

    @AcColumn(comment = "逻辑删除（F 否，空删除）", length = 1, type = ColumnTypeConstants.CHAR, defaultValue = "'F'", order = 100)
    private String logicDel;

}
