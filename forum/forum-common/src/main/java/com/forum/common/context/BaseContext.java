package com.forum.common.context;

/**
 * BaseContext - 使用 ThreadLocal 存储当前登录用户的ID
 * 在 JWT 拦截器中设置，在 Controller/Service 中获取
 */
public class BaseContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    /**
     * 移除当前线程数据 (防止内存泄漏)
     */
    public static void removeCurrentId() {
        threadLocal.remove();
    }
}
