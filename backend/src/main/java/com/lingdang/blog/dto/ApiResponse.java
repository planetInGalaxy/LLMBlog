package com.lingdang.blog.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应
 */
@Data
@NoArgsConstructor
public class ApiResponse<T> {

    public static final int SUCCESS_CODE = 0;
    public static final int ERROR_CODE = 1;

    private int code;
    private String message;
    private T data;
    // 兼容旧客户端，过渡期保留 success 字段
    private boolean success;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = code == SUCCESS_CODE;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(SUCCESS_CODE, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ERROR_CODE, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
