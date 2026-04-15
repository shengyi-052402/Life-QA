package com.forum.common.constant;

/**
 * 提示信息常量
 */
public class MessageConstant {

    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_DISABLED = "账号已被禁用";
    public static final String USERNAME_EXISTS = "用户名已存在";
    public static final String EMAIL_EXISTS = "邮箱已被注册";
    public static final String REGISTER_SUCCESS = "注册成功";
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String PASSWORD_CHANGED = "密码修改成功";
    public static final String PROFILE_UPDATED = "资料修改成功";

    public static final String POST_NOT_FOUND = "帖子不存在";
    public static final String POST_CREATED = "发布成功";
    public static final String POST_UPDATED = "编辑成功";
    public static final String POST_DELETED = "删除成功";
    public static final String NO_PERMISSION = "没有操作权限";

    public static final String COMMENT_CREATED = "评论成功";
    public static final String COMMENT_DELETED = "评论已删除";
    public static final String COMMENT_NOT_FOUND = "评论不存在";

    public static final String CATEGORY_NOT_FOUND = "分类不存在";
    public static final String CATEGORY_NAME_EXISTS = "分类名称已存在";
    public static final String CATEGORY_HAS_POSTS = "该分类下存在帖子，无法删除";

    public static final String UPLOAD_SUCCESS = "上传成功";
    public static final String UPLOAD_FAILED = "上传失败";
    public static final String FILE_TOO_LARGE = "文件大小超过限制";
    public static final String FILE_TYPE_NOT_ALLOWED = "不支持的文件类型";
}
