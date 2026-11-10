package com.lambda.example.servicea.enums;

import lombok.Getter;
import org.lambda.framework.common.exception.ExceptionEnumFunction;

@Getter
public enum ExampleError implements ExceptionEnumFunction {
    PRODUCT_NOT_FOUND("B0100001", "找不到该商品"),
    INVALID_CURSOR("B0100002", "分页位置无效，请重新查询"),
    PRODUCT_CHANGED("B0100003", "商品已变更，请刷新后重试"),
    STOCK_UNAVAILABLE("B0100004", "商品库存不足或已下架"),
    PURCHASE_REQUEST_CONFLICT("B0100005", "此请求已用于另一笔订单"),
    INVALID_PAGE("B0100006", "页码或每页条数不正确"),
    CACHE_NOT_FOUND("B0200001", "找不到这条缓存"),
    INVALID_CACHE_KEY("B0200002", "缓存名称不正确"),
    INVALID_CACHE_TTL("B0200003", "缓存有效期不正确"),
    INVALID_VECTOR("B0300001", "请输入三个有效且不全为零的数值"),
    LOGIN_FAILED("B0400001", "用户名或口令错误"),
    SESSION_MISSING("B0400002", "请先登录"),
    INVALID_CHAT_MESSAGE("B0500001", "请填写消息编号和正文"),
    INVALID_DELAY("B0600001", "延时时间不能超过一分钟"),
    RECEIPT_NOT_FOUND("B0600002", "回执尚未生成，请稍后查看"),
    INVALID_NOTIFICATION("B0600003", "通知内容不完整"),
    EVENT_NOT_FOUND("B0700001", "事件尚未生成回执"),
    ZOOKEEPER_DISABLED("B0800001", "所选方式暂不可用");

    private final String code;
    private final String message;

    ExampleError(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
