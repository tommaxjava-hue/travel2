package com.example.travelaibackend.common;

import lombok.Data;

@Data
public class Result<T> {
    private String code;
    private String msg;
    private T data;

    protected Result() {
    }

    protected Result(String code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> error(ResultCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> error(String code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCode.FAILED.getCode(), msg, null);
    }
}