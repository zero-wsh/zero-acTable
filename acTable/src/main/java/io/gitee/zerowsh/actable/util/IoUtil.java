package io.gitee.zerowsh.actable.util;

import cn.hutool.core.util.ArrayUtil;

/**
 * 释放资源
 *
 * @author zero
 */
public class IoUtil {

    /**
     * close Closeable
     *
     * @param closeables the closeables
     */
    public static void close(AutoCloseable... closeables) {
        if (ArrayUtil.isNotEmpty(closeables)) {
            for (AutoCloseable closeable : closeables) {
                close(closeable);
            }
        }
    }

    /**
     * close Closeable
     *
     * @param closeable the closeable
     */
    public static void close(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

}