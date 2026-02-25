package com.example.travelaibackend.common;

public enum ResultCode {
    SUCCESS("200", "操作成功"),
    UNAUTHORIZED("401", "未登录或Token已过期"),
    FORBIDDEN("403", "权限不足，拒绝访问"),
    VALIDATE_FAILED("404", "参数检验失败"),
    FAILED("500", "系统异常，请稍后重试");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}