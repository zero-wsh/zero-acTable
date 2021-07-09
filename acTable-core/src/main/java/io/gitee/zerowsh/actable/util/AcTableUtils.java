package io.gitee.zerowsh.actable.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dao.BaseDatabaseMapper;
import io.gitee.zerowsh.actable.dao.SqlServerMapper;
import io.gitee.zerowsh.actable.emnus.DatabaseTypeEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static io.gitee.zerowsh.actable.emnus.DatabaseTypeEnums.SQL_SERVER;

/**
 * 所有数据库共用工具类
 *
 * @author zero
 */
@Component
@Slf4j
public class AcTableUtils {
    /**
     * 执行脚本
     *
     * @return 是否执行成功
     */
    public boolean executeScript(CreateTableConfig createTableConfig, BaseDatabaseMapper baseDatabaseMapper) {
        DatabaseTypeEnums databaseType = createTableConfig.getDatabaseType();
        String script = createTableConfig.getScript();
        if (StrUtil.isBlank(script)) {
            return false;
        }
        log.info("执行 [{}] 初始化数据。。。", databaseType);
        org.springframework.core.io.Resource[] resources;
        try {
            resources = new PathMatchingResourcePatternResolver().getResources(ResourceUtils.CLASSPATH_URL_PREFIX + script);
        } catch (IOException e) {
            log.warn("[{}] 没找到初始化数据文件 [{}]！！！", databaseType, script);
            return false;
        }
        String fileUrl = null;
        try {
            for (org.springframework.core.io.Resource resource : resources) {
                File file = resource.getFile();
                if (file.isFile()) {
                    fileUrl = file.getAbsolutePath();
                    String sql = FileUtil.readUtf8String(file);
                    if (StrUtil.isNotBlank(sql)) {
                        log.info(sql);
                        baseDatabaseMapper.executeSql(sql);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("执行 [{}] 初始化数据失败！！！读取文件异常 [{}]", databaseType, fileUrl);
            return false;
        }
        return true;
    }

    /**
     * 处理关键字
     *
     * @param var
     * @param databaseType
     * @return
     */
    public static String handleKeyword(String var, DatabaseTypeEnums databaseType) {
        switch (databaseType) {
            case SQL_SERVER:
                if (var.startsWith(StringPool.LEFT_SQ_BRACKET) && var.endsWith(StringPool.RIGHT_SQ_BRACKET)) {
                    var = var.replace(StringPool.LEFT_SQ_BRACKET, "")
                            .replace(StringPool.RIGHT_SQ_BRACKET, "");
                }
                break;
            case MYSQL:
                if (var.startsWith(StringPool.BACKTICK) && var.endsWith(StringPool.BACKTICK)) {
                    var = var.replace(StringPool.BACKTICK, "");
                }
                break;
            default:
        }
        return var;
    }
}
