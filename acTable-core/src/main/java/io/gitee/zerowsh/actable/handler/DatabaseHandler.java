package io.gitee.zerowsh.actable.handler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.annotation.Table;
import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dao.SqlServerMapper;
import io.gitee.zerowsh.actable.dto.ConstraintInfo;
import io.gitee.zerowsh.actable.dto.TableColumnInfo;
import io.gitee.zerowsh.actable.dto.TableInfo;
import io.gitee.zerowsh.actable.emnus.ModelEnums;
import io.gitee.zerowsh.actable.util.CreateTableUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 数据库处理类
 *
 * @author zero
 */
@Component
@Slf4j
public class DatabaseHandler {
    @Resource
    private SqlServerMapper sqlServerMapper;

    @Autowired
    private CreateTableConfig createTableConfig;

    @Transactional(rollbackFor = Exception.class)
    public void createTable() {
        if (Objects.isNull(createTableConfig.getModel())
                || Objects.equals(createTableConfig.getModel(), ModelEnums.NONE)) {
            log.info("自动建表不做任何操作");
            return;
        }

        log.info("开始执行数据库自动建表操作。。。");
        String entityPackage = createTableConfig.getEntityPackage();
        if (StrUtil.isBlank(entityPackage)) {
            log.warn("请设置实体类包路径");
            return;
        }
        String[] split = entityPackage.split(StringPool.COMMA);
        List<TableInfo> tableInfoList = new ArrayList<>();
        List<String> tableList = new ArrayList<>();
        for (String s : split) {
            Set<Class<?>> tableClass = ClassUtil.scanPackageByAnnotation(s, Table.class);
            Set<Class<?>> tableNameClass = ClassUtil.scanPackageByAnnotation(s, TableName.class);
            Set<Class<?>> tableSet = new HashSet<>();
            tableSet.addAll(tableClass);
            tableSet.addAll(tableNameClass);
            for (Class<?> aClass : tableSet) {
                CreateTableUtils.getTableInfoByClass(aClass, tableList, tableInfoList, createTableConfig.getTurn());
            }
        }
        if (CollectionUtil.isEmpty(tableInfoList)) {
            log.warn("没有找到@Table或@TableName标记的类，entityPackage={}", entityPackage);
            return;
        }

        /*
         * 1.判断表是否存在
         *    存在--判断是否需要修改
         *    不存在--新建
         */
        for (TableInfo tableInfo : tableInfoList) {
            String name = tableInfo.getName();
            int existTable = sqlServerMapper.isExistTable(name);
            if (existTable == 0) {
                //不存在--建表
                List<String> createTableSql = CreateTableUtils.getCreateTableSql(tableInfo);
                for (String sql : createTableSql) {
                    log.info(sql);
                    sqlServerMapper.executeSql(sql);
                }
            } else {
                /*
                 * 存在--改表
                 *
                 * 1.获取表结构
                 * 2.获取约束（唯一键、主键、索引）
                 * 3.获取默认值约束
                 * 4.修改表
                 * 5.顺序执行sql
                 */
                List<TableColumnInfo> tableColumnInfoList = sqlServerMapper.getTableStructure(name);
                List<ConstraintInfo> constraintInfoList = sqlServerMapper.getConstraintInfo(name);
                List<ConstraintInfo> defaultInfoList = sqlServerMapper.getDefaultInfo(name);
                List<String> updateTableSql = CreateTableUtils.getUpdateTableSql(tableInfo,
                        tableColumnInfoList,
                        constraintInfoList,
                        defaultInfoList,
                        createTableConfig.getModel());
                for (String sql : updateTableSql) {
                    log.info(sql);
                    sqlServerMapper.executeSql(sql);
                }
            }
        }
        log.info("数据库自动建表操作完成！");
    }
}
