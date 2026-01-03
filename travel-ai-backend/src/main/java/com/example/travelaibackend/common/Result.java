package com.example.travelaibackend.common;

import lombok.Data;

@Data
public class Result<T> {
    private String code; // 200成功, 其他失败
    private String msg;  // 提示信息
    private T data;      // 返回的数据

    // 成功的静态方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode("200");
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败的静态方法
    // 修改重点：这里加了 <T>，并且返回值改成了 Result<T>
    // 这样它就能根据需要，自动变成 Result<Attraction> 或 Result<User> 等任何类型
    public static <T> Result<T> error(String code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}