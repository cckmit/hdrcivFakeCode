package com.goodwill.hdr.civ.global;

/**
 * 作为返回给前端的规范
 *
 * @param <T> 代表后端传给前端的数据类型
 * @author 余涛
 * @date 2020/12/27
 */
public class CivResult<T> {
    /**
     * 响应状态码
     */
    private Integer statusCode;
    /**
     * 响应状态描述
     */
    private String statusDescription;
    /**
     * 传给前端的数据，可以为空
     */
    private T data;
    /**
     * 传给前端的信息
     */
    private String message;

    public CivResult(Integer statusCode, String statusDescription, T data, String message) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.data = data;
        this.message = message;
    }

    public CivResult(Integer statusCode, String statusDescription, String message) {
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
        this.message = message;
    }
}
