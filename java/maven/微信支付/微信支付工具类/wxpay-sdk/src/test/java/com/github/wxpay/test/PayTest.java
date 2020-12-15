package com.github.wxpay.test;

import com.github.wxpay.sdk.PayConfig;
import com.github.wxpay.sdk.WXPay;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class PayTest {

    private WXPay wxPay;

    @Before
    public void setup() throws Exception{
        PayConfig payConfig = new PayConfig();
        payConfig.setAppID("wx8397f8696b538317"); // 公众账号ID
        payConfig.setMchID("1473426802");// 商户号
        payConfig.setKey("T6m9iK73b0kn9g5v426MKfHQH7X8rKwb");// 生成签名的密钥
        this.wxPay = new WXPay(payConfig);
    }

    @Test
    public void testUnifiedOrder(){
        // 请求参数：
        Map<String, String> data = new HashMap<String, String>();
        data.put("body", "腾讯充值中心-QQ会员充值");
        data.put("out_trade_no", "2018414151234000012");
        data.put("total_fee", "1");
        data.put("spbill_create_ip", "127.0.0.1");
        data.put("notify_url", "http://www.example.com/wxpay/notify");
        data.put("trade_type", "NATIVE");  // 此处指定为扫码支付

        try {
            Map<String, String> resp = wxPay.unifiedOrder(data);
            System.out.println(resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
