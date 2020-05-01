package com.example.demo.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author: gaokun
 * @date: 2020/2/14 12:32 下午
 * @description: WXWebConfig  配置文件配置信息
 */

@Data
@Configuration
@ConfigurationProperties(prefix="order.applepay")
public class ApplePayConfig {

    public String api;
    public String sandboxApi;
    public String applePayNotifyUrl;
    /**
     * 如果用券的话走这个productId
     */
    public String appleSubscribeProductId;
    /**
     * 后台配置的商品code
     */
    public String offerIdentifier;
    /**
     * 后台配置第一步中的密钥ID
     */
    public String keyIdentifier;
    /**
     * 密钥内容
     */
    public String keyIdentifierContent;
    public String appBundleID;

}
