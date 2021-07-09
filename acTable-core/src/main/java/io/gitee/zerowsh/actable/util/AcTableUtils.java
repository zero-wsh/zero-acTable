package io.gitee.zerowsh.actable.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import io.gitee.zerowsh.actable.config.CreateTableConfig;
import io.gitee.zerowsh.actable.dao.BaseDatabaseMapper;
import io.gitee.zerowsh.actable.emnus.DatabaseTypeEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 所有数据库共用工具类
 *
 * @author zero
 */
@Component
@Slf4j
public class AcTableUtils {

    @Resource
    private BaseDatabaseMapper baseDatabaseMapper;
    @Resource
    private CreateTableConfig createTableConfig;

    /**
     * 执行脚本
     *
     * @return 是否执行成功
     */
    public void executeScript() {
        DatabaseTypeEnums databaseType = createTableConfig.getDatabaseType();
        String script = createTableConfig.getScript();
        if (StrUtil.isBlank(script)) {
            return;
        }
        log.info("执行 [{}] 初始化数据。。。", databaseType);
        List<org.springframework.core.io.Resource> list = new ArrayList<>();
        for (String s : script.split(StringPool.COMMA)) {
            try {
                list.addAll(Arrays.asList(new PathMatchingResourcePatternResolver().getResources(ResourceUtils.CLASSPATH_URL_PREFIX + s)));
            } catch (IOException e) {
                log.warn("[{}] 没找到初始化数据文件 [{}]！！！", databaseType, s);
                return;
            }
        }

        String fileUrl = null;
        try {
            for (org.springframework.core.io.Resource resource : list) {
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
            return;
        }
        log.info("执行 [{}] 初始化数据完成！！！", databaseType);
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
