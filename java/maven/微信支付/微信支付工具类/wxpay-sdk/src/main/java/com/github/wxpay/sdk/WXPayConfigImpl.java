package com.github.wxpay.sdk;

import lombok.Data;

import java.io.InputStream;


@Data
public class WXPayConfigImpl extends WXPayConfig {
    /**
     * 公众账号ID
     */
    private String appID;
    /**
     * 商户号
     */
    private String mchID;
    /**
     * 生成签名的密钥
     */
    private String key;
    /**
     * 支付回调地址
     */
    private String notifyUrl;
    /**
     * 支付方式
     */
    private String payType;

    public InputStream getCertStream(){
        return null;
    }

    public IWXPayDomain getWXPayDomain(){
        return WXPayDomainSimpleImpl.instance();
    }
}