package com.pj.login.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(
        boolean success,
        T data,
        ErrorDetail error
) {
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, data, null);
    }

    public static <T> ApiResult<T> error(String code, String message) {
        return new ApiResult<>(false, null, new ErrorDetail(code, message));
    }
}
