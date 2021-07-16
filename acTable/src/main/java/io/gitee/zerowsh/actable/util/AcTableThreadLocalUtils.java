package io.gitee.zerowsh.actable.util;

/**
 * @author zero
 */
public class AcTableThreadLocalUtils {
    private static final ThreadLocal<String> DATABASE_TYPE = new ThreadLocal<>();

    public static String getDatabaseType() {
        return DATABASE_TYPE.get();
    }

    public static void setDatabaseType(String databaseType) {
        DATABASE_TYPE.set(databaseType);
    }

    public static void remove() {
        DATABASE_TYPE.remove();
    }
}
