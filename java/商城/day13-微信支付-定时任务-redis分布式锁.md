# 学习目标

- 了解微信支付的流程
- 完成微信支付调用
- 实现支付回调,了解内网穿透
- 实现支付状态查询
- 使用定时任务
- 了解redis分布式锁
- 了解redisson
- 完成超时订单清理

# 1.微信支付简介

## 1.1.介绍

微信支付官方文档：https://pay.weixin.qq.com/index.php/core/home/login?return_url=%2F

![1555642281129](assets/1555642281129.png)

我们选择开发文档，而后进入选择页面：

![1555642332763](assets/1555642332763.png)

选择native支付，就是扫码支付：

![1527848368179](assets/1527848368179.png)

此处我们使用模式二来开发：

## 1.2.开发流程

模式二与模式一相比，流程更为简单，不依赖设置的回调支付URL。

商户后台系统先调用微信支付的统一下单接口，微信后台系统返回链接参数code_url；

商户后台系统将code_url值生成二维码图片，用户使用微信客户端扫码后发起支付。

注意：code_url有效期为2小时，过期后扫码不能再发起支付。 

流程图：

![2wa23131](assets/chapter6_5_1.png)

这里我们把商户（我们）要做的事情总结一下：

- 1、商户生成订单
- 2、商户调用微信下单接口，获取预交易的链接
- 3、商户将链接生成二维码图片，展示给用户；
- 4、支付结果通知：
  - 微信异步通知商户支付结果，商户告知微信支付接收情况
  - 商户如果没有收到通知，可以调用接口，查询支付状态
- 5、如果支付成功，发货，修改订单状态



在前面的业务中，我们已经完成了：

- 1、生成订单

接下来，我们需要做的是：

- 2、调用微信下单接口，生成链接。
- 3、根据链接生成二维码图片
- 4、支付成功后修改订单状态



# 2.微信统一下单API（生成支付链接）

按照上面的步骤分析，第一步是要生成支付链接。我们查看下微信官方文档

## 2.1.API说明

在微信支付文档中，可以查询到下面的信息：

> 请求路径

URL地址：https://api.mch.weixin.qq.com/pay/unifiedorder



> 请求参数

| 字段名     | 变量名           | 必填 | 类型        | 示例值                                 | 描述                                                         |
| :--------- | ---------------- | ---- | ----------- | -------------------------------------- | ------------------------------------------------------------ |
| 公众账号ID | appid            | 是   | String(32)  | wxd678efh56                            | 微信支付分配的公众账号ID                                     |
| 商户号     | mch_id           | 是   | String(32)  | 1230000109                             | 微信支付分配的商户号                                         |
| 随机字符串 | nonce_str        | 是   | String(32)  | 5K8264ILT                              | 随机字符串，长度要求在32位以内。推荐[随机数生成算法](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_3) |
| 签名       | sign             | 是   | String(32)  | C380BEC2B                              | 通过签名算法计算得出的签名值，详见[签名生成算法](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_3) |
| 商品描述   | body             | 是   | String(128) | 乐优手机                               | 商品简单描述，该字段请按照规范传递，具体请见[参数规定](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_2) |
| 商户订单号 | out_trade_no     | 是   | String(32)  | 20150806125                            | 商户系统内部订单号，要求32个字符内，只能是数字、大小写字母_-\|* 且在同一个商户号下唯一。详见[商户订单号](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_2) |
| 标价金额   | total_fee        | 是   | Int         | 88                                     | 订单总金额，单位为分，详见[支付金额](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_2) |
| 终端IP     | spbill_create_ip | 是   | String(16)  | 123.12.12.123                          | APP和网页支付提交用户端ip，Native支付填调用微信支付API的机器IP。 |
| 通知地址   | notify_url       | 是   | String(256) | http://www.weixin.qq.com/wxpay/pay.php | 异步接收微信支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。 |
| 交易类型   | trade_type       | 是   | String(16)  | JSAPI                                  | JSAPI 公众号支付；NATIVE 扫码支付；APP APP支付说明详见[参数规定](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_2) |

这些参数大致分成3类：

- appid、mch_id、spbill_create_ip、notify_url、trade_type：是商家自己的信息或固定数据，可以提前配置，因此无需每次请求单独配置，而是统一设置好即可，

- nonce_str、sign：是为了保证数据安全而添加的验证数据，根据算法去生成，每次请求自动生成即可。

- body、out_trade_no、total_fee：订单相关信息，需要我们自己填写。



## 2.2.微信SDK

### 2.2.1.下载

虽然请求参数比较复杂，但官方已经提供了SDK，供我们使用：![1535979973318](assets/1535979973318.png)

我也已经在课前资料提供：

 ![1535980012593](assets/1535980012593.png)

微信没有提供maven仓库坐标，因此我们必须下载使用，建议使用课前资料中，我提供给大家的SDK，其中做了一些必要的设置：

![1555646853775](assets/1555646853775.png) 

### 2.2.2.WXPay工具

微信SDK提供了一个统一的微信支付工具类：WXPay：

![1555646424254](assets/1555646424254.png)

其中包含这样一些方法：

com.github.wxpay.sdk.WXPay类下提供了对应的方法：

| 方法名           | 说明             |
| ---------------- | ---------------- |
| microPay         | 刷卡支付         |
| `unifiedOrder`   | **统一下单**     |
| orderQuery       | 查询订单         |
| reverse          | 撤销订单         |
| closeOrder       | 关闭订单         |
| refund           | 申请退款         |
| refundQuery      | 查询退款         |
| downloadBill     | 下载对账单       |
| report           | 交易保障         |
| shortUrl         | 转换短链接       |
| authCodeToOpenid | 授权码查询openid |

- 注意:
  - 参数为`Map<String, String>`对象，返回类型也是`Map<String, String>`
  - 方法内部会将参数转换成含有`appid`、`mch_id`、`nonce_str`、`sign_type`和`sign`的XML
  - 通过HTTPS请求得到返回数据后会对其做必要的处理（例如验证签名，签名错误则抛出异常）

我们主要关注其中的unifiedOrder方法，统一下单：

```java
/**
     * 作用：统一下单<br>
     * 场景：公共号支付、扫码支付、APP支付
     * @param reqData 向wxpay post的请求数据
     * @return API返回数据
     * @throws Exception
     */
public Map<String, String> unifiedOrder(Map<String, String> reqData) throws Exception {
    return this.unifiedOrder(reqData, config.getHttpConnectTimeoutMs(), this.config.getHttpReadTimeoutMs());
}
```

这里的请求参数是：Map<String, String> reqData，就是官方API说明中的请求参数了，不过并不需要我们填写所有参数，而只需要下面的：

- body：商品描述
- out_trade_no：订单编号
- total_fee：订单应支付金额
- spbill_create_ip：设备IP
- notify_url：回调地址
- trade_type：交易类型

剩下的：`appid`、`mch_id`、`nonce_str`、`sign_type`和`sign`参数都有WXPay对象帮我们设置，那么问题来了：这些参数数据WXPay是怎么拿到的呢？

其中，

- nonce_str：是随机字符串，因此由WXPay随机生成，
- sign_type：是签名算法，由WXPay指定，默认是HMACSHA256；
- sign：是签名，有签名算法结合密钥加密而来，因此这里的关键是密钥：key
- appid、mch_id是商家信息，需要配置

也就是说，这例需要配置的包括：appid、mch_id、密钥key。这些从哪里来呢？

看下WXPay的构造函数：

```java
public WXPay(final WXPayConfig config) throws Exception {
    this(config, null, true, false);
}
```

这里需要一个WXPayConfig对象，显然是配置对象。



### 2.2.3..WXPayConfig配置

WXPay依赖于WXPayConfig进行配置，那么WXPayConfig是什么呢？

看下源码中的关键部分：

```java
public abstract class WXPayConfig {
    /**
     * 获取 App ID
     *
     * @return App ID
     */
    abstract String getAppID();
    /**
     * 获取 Mch ID
     *
     * @return Mch ID
     */
    abstract String getMchID();
    /**
     * 获取 API 密钥
     *
     * @return API密钥
     */
    abstract String getKey();
    
    // 。。。省略
}
```

这不就是WXPay中需要配置的3个属性嘛，当我们实现这个类，并且给出其中的值，把WXPayConfig传递给WXPay时，WXPay就会获取到这些数据:

![1555647829543](assets/1555647829543.png) 

当我们利用WXPay发送请求时，WXPay就会帮我们封装到请求参数中：

![1555647879979](assets/1555647879979.png)



而在我提供给大家的SDK中，就编写了一个WXPayConfig的实现：

```java
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
```

将来我们只需要new出这个实现类对象，并且给这3个参数赋值即可。

## 2.3.整合到项目中

### 2.3.1.打包SDK

首先，把我提供的SDK打包并安装到本地的maven仓库，方便在项目中使用。

进入我提供的SDK的项目目录，然后打开黑窗口，输入命令：

```
mvn source:jar install -Dmaven.test.skip=true
```

![1555648127151](assets/1555648127151.png)

然后进入本地仓库查看：

![1555648204589](assets/1555648204589.png) 



### 2.3.2.配置WXPay

在ly-order中引入坐标：

```xml
<dependency>
    <groupId>com.github.wxpay</groupId>
    <artifactId>wxpay-sdk</artifactId>
    <version>3.0.9</version>
</dependency>
```



我们将这些WXPayConfig中的属性定义到application.yml中

```yaml
ly:
  pay:
    wx:
      appID: wx8397f8696b538317
      mchID: 1473426802
      key: T6m9iK73b0kn9g5v426MKfHQH7X8rKwb
      payType: NATIVE
      notifyUrl: http://api.leyou.com/api/pay/wx/notify
```

将这些属性注入到PayConfig中：

```java
package com.leyou.order.config;

import com.github.wxpay.sdk.WXPayConfigImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "ly.pay.wx")
    public WXPayConfigImpl payConfig(){
        return new WXPayConfigImpl();
    }
}
```



### 2.3.4.支付工具类

我们先初始化WXPay对象，并注入到Spring容器中：

```java
package com.leyou.order.config;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConfigImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "ly.pay.wx")
    public WXPayConfigImpl payConfig(){
        return new WXPayConfigImpl();
    }

    /**
     * 注册WXPay对象
     * @param payConfig 支付相关配置
     * @return WXPay对象
     * @throws Exception 连结WX失败时用到
     */
    @Bean
    public WXPay wxPay(WXPayConfigImpl payConfig) throws Exception {
        return new WXPay(payConfig);
    }
}
```



我们定义支付工具类，完成后续操作：

```java
package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConfigImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PayHelper {

    @Autowired
    private WXPay wxPay;

    @Autowired
    private WXPayConfigImpl payConfig;

    public String createOrder(Long orderId, Long totalPay, String desc){
        Map<String, String> data = new HashMap<>();
        // 商品描述
        data.put("body", desc);
        // 订单号
        data.put("out_trade_no", orderId.toString());
        //金额，单位是分
        data.put("total_fee", totalPay.toString());
        //调用微信支付的终端IP
        data.put("spbill_create_ip", "127.0.0.1");
        //回调地址
        data.put("notify_url", payConfig.getNotifyUrl());
        // 交易类型为扫码支付
        data.put("trade_type", payConfig.getPayType());

        // 利用wxPay工具,完成下单
        Map<String, String> result = null;
        try {
            result = wxPay.unifiedOrder(data);
        } catch (Exception e) {
            log.error("【微信下单】创建预交易订单异常失败", e);
            throw new RuntimeException("微信下单失败", e);
        }
        // 校验业务状态
        checkResultCode(result);

        // 下单成功，获取支付链接
        String url = result.get("code_url");
        if (StringUtils.isBlank(url)) {
            throw new RuntimeException("微信下单失败，支付链接为空");
        }
        return url;
    }

    public void checkResultCode(Map<String, String> result) {
        // 检查业务状态
        String resultCode = result.get("result_code");
        if ("FAIL".equals(resultCode)) {
            log.error("【微信支付】微信支付业务失败，错误码：{}，原因：{}", result.get("err_code"), result.get("err_code_des"));
            throw new RuntimeException("【微信支付】微信支付业务失败");
        }
    }
}
```



## 2.4.下单并生成支付链接

在订单支付页面，会向后台发起请求，查询支付的URL地址：

![1555651440971](assets/1555651440971.png) 

我们需要编写controller，来实现这个功能：

- 请求方式：GET
- 请求路径：/order/url/{id}
- 请求参数：id，订单的编号
- 返回结果：url地址

代码如下：

controller：

```java
@GetMapping("url/{id}")
public ResponseEntity<String> getPayUrl(@PathVariable("id") Long orderId) {
    return ResponseEntity.ok(orderService.getUrl(orderId));
}
```

service，订单支付url的有效期是2小时，因此我们可以获取url后存入redis缓存：

- 先检查redis是否已经有url，有则返回
- 没有，则查询订单信息，校验订单状态是否为已经支付，是则抛出异常
- 如果没有支付，调用PayHelper，生成url
- 将url存入redis，设置有效期为2小时

引入redis依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

配置：

```yaml
spring:
  redis:
    host: 192.168.150.101
```



代码：

```java
@Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PayHelper payHelper;
    /**
     * 获取 支付链接
     * @param orderId
     * @return
     */
    private String payUrlKey = "ly:pay:orderid:";
    @Override
    public String getUrl(Long orderId) {

        String redisKey = payUrlKey + orderId;
        //1、从redis中获取 支付url key--orderId value = url
        String url = redisTemplate.opsForValue().get(redisKey);
        //2、如果url不是null，直接返回url
        if(!org.springframework.util.StringUtils.isEmpty(url)){
            return url;
        }
//        redis中不存在order对应的 url
        TbOrder tbOrder = this.getById(orderId);
        if(tbOrder == null){
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
//        订单状态不是 1 的时候不能支付
        if(tbOrder.getStatus().intValue() != OrderStatusEnum.INIT.value().intValue()){
            throw new LyException(ExceptionEnum.INVALID_ORDER_STATUS);
        }
        //调用wxpay ，获取url
        Long totalFee = 1L;//tbOrder.getActualFee();
        String desc = "乐优商城商品支付";
        String payUrl = payHelper.createOrder(orderId, totalFee, desc);
        //payUrl 有2小时的 有效期，放入redis中
        redisTemplate.opsForValue().set(redisKey,payUrl,2, TimeUnit.HOURS);
        return payUrl;
    }
```

页面响应结果：

 ![1536017643922](assets/1536017643922.png)



## 🎗经验分享-微信支付参数

### 1.用户下单后，调用微信支付平台获取支付链接代码如下

```java
 @Override
public String getPayUrl(Long orderId) {
    try {
        //尝试从redis中获取支付链接
        String code_url = redisTemplate.boundValueOps(PAY_URL + orderId).get();
        if(StringUtils.isNotBlank(code_url)){
            return code_url;
        }
        //1、组装微信支付平台所需要的必填参数  map集合
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("appid",payProperties.getAppID());
        paramMap.put("mch_id",payProperties.getMchID());
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
        paramMap.put("body","乐优商城");
        paramMap.put("out_trade_no",orderId.toString());
        //支付金额，就是实付金额，基于订单id查询订单获取
        TbOrder order = this.getById(orderId);
        //paramMap.put("total_fee",order.getActualFee().toString());
        paramMap.put("total_fee","1");//修改成支付1分钱
        paramMap.put("spbill_create_ip","127.0.0.1");
        paramMap.put("notify_url",payProperties.getNotifyurl());
        paramMap.put("trade_type","Native");
        paramMap.put("product_id","1");

        //2、将map转为xml格式字符串
        String paramXml = WXPayUtil.generateSignedXml(paramMap, payProperties.getKey());
        //3、调用微信支付平台请求地址，完成获取支付链接操作  可以基于RestTemplate完成请求调用
        FiltratHttpsUtils.doFiltra();//过滤https安全协议校验
        String resultXml = restTemplate.postForObject("https://api.mch.weixin.qq.com/pay/unifiedorder", paramXml, String.class);
        //4、解析返回结果，并获取支付链接
        Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);
        code_url = resultMap.get("code_url");
        //将微信支付平台返回的支付链接缓存redis 2个小时
        redisTemplate.boundValueOps(PAY_URL + orderId).set(code_url,2L, TimeUnit.HOURS);
        return code_url;
    } catch (Exception e) {
        e.printStackTrace();
        log.info(e.getMessage());
        throw new LyException(ExceptionEnum.GET_PAY_URL_ERROR);
    }
}
```

### 2.出现的问题

> 页面提交订单后，跳转到支付页面微信支付二维码未生成，并且idea控制台报如下错误

![59444925741](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594449257414.png)



### 3.问题的分析

> 从idea控制台错误信息看出，TbOrderServiceImpl.java的230行报错。具体错误原因是往redis缓存数据时，value值不允许为null。说明调用微信支付平台获取支付链接时返回的code_url为null。可以debug断点调试一下错误的具体原因。调试截图如下：

![59444961872](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594449618726.png)

查看调用微信支付平台时，返回的结果数据：

![59444967179](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594449671796.png)

发现微信支付平台返回了“trade_type参数格式错误”，导致调用微信支付平台失败。

经查看封装参数的代码和微信支付平台参数列表对比，发现如下问题：

![59444995240](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594449952401.png)

### 4.问题解决办法

> 修改封装支付方式参数的代码，有“Native“改为”NATIVE“即可。修改后代码如下

```java
@Override
public String getPayUrl(Long orderId) {
    try {
        //尝试从redis中获取支付链接
        String code_url = redisTemplate.boundValueOps(PAY_URL + orderId).get();
        if(StringUtils.isNotBlank(code_url)){
            return code_url;
        }
        //1、组装微信支付平台所需要的必填参数  map集合
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("appid",payProperties.getAppID());
        paramMap.put("mch_id",payProperties.getMchID());
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
        paramMap.put("body","乐优商城");
        paramMap.put("out_trade_no",orderId.toString());
        //支付金额，就是实付金额，基于订单id查询订单获取
        TbOrder order = this.getById(orderId);
        //paramMap.put("total_fee",order.getActualFee().toString());
        paramMap.put("total_fee","1");//修改成支付1分钱
        paramMap.put("spbill_create_ip","127.0.0.1");
        paramMap.put("notify_url",payProperties.getNotifyurl());
        paramMap.put("trade_type","NATIVE");
        paramMap.put("product_id","1");

        //2、将map转为xml格式字符串
        String paramXml = WXPayUtil.generateSignedXml(paramMap, payProperties.getKey());
        //3、调用微信支付平台请求地址，完成获取支付链接操作  可以基于RestTemplate完成请求调用
        FiltratHttpsUtils.doFiltra();//过滤https安全协议校验
        String resultXml = restTemplate.postForObject("https://api.mch.weixin.qq.com/pay/unifiedorder", paramXml, String.class);
        //4、解析返回结果，并获取支付链接
        Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);
        code_url = resultMap.get("code_url");
        //将微信支付平台返回的支付链接缓存redis 2个小时
        redisTemplate.boundValueOps(PAY_URL + orderId).set(code_url,2L, TimeUnit.HOURS);
        return code_url;
    } catch (Exception e) {
        e.printStackTrace();
        log.info(e.getMessage());
        throw new LyException(ExceptionEnum.GET_PAY_URL_ERROR);
    }
}
```

修改后，debug查看返回结果参数，截图如下：

![59445029401](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594450294018.png)













# 3.生成支付二维码（了解）

## 3.1.什么是二维码（了解）

二维码又称QR Code，QR全称Quick Response，是一个近几年来移动设备上超流行的一种编码方式，它比传统的Bar Code条形码能存更多的信息，也能表示更多的数据类型。

二维条码/二维码（2-dimensional bar code）是用某种特定的几何图形按一定规律在平面（二维方向上）分布的黑白相间的图形记录数据符号信息的；在代码编制上巧妙地利用构成计算机内部逻辑基础的“0”、“1”比特流的概念，使用若干个与二进制相对应的几何形体来表示文字数值信息，通过图象输入设备或光电扫描设备自动识读以实现信息自动处理：它具有条码技术的一些共性：每种码制有其特定的字符集；每个字符占有一定的宽度；具有一定的校验功能等。同时还具有对不同行的信息自动识别功能、及处理图形旋转变化点。

## 3.2.二维码优势（了解）

- 信息容量大, 可以容纳多达1850个大写字母或2710个数字或500多个汉字

- 应用范围广, 支持文字,声音,图片,指纹等等...

- 容错能力强, 即使图片出现部分破损也能使用

- 成本低, 容易制作

## 3.3.二维码容错级别（了解）

- L级（低） 7％的码字可以被恢复。

- M级（中） 15％的码字可以被恢复。

- Q级（四分）25％的码字可以被恢复。

- H级（高）30％ 的码字可以被恢复。

## 3.4.二维码生成插件qrious（了解）

qrious是一款基于HTML5 Canvas的纯JS二维码生成插件。通过qrious.js可以快速生成各种二维码，你可以控制二维码的尺寸颜色，还可以将生成的二维码进行Base64编码。[官网](https://github.com/davidshimjs/qrcodejs)

qrious.js二维码插件的可用配置参数如下：

| 参数       | 类型   | 默认值      | 描述                               |
| ---------- | ------ | ----------- | ---------------------------------- |
| background | String | "white"     | 二维码的背景颜色。                 |
| foreground | String | "black"     | 二维码的前景颜色。                 |
| level      | String | "L"         | 二维码的误差校正级别(L, M, Q, H)。 |
| mime       | String | "image/png" | 二维码输出为图片时的MIME类型。     |
| size       | Number | 100         | 二维码的尺寸，单位像素。           |
| value      | String | ""          | 需要编码为二维码的值               |

课前资料中给出的案例可以直接生成二维码：

 ![1535987680862](assets/1535987680862.png)



## 3.5.生成二维码（了解）

我们把课前资料中的这个js脚本引入到项目中：

 ![1528362348399](assets/1528362348399.png)

然后在页面引用：

 ![1528362377494](assets/1528362377494.png)



页面定义一个div，用于展示二维码：

 ![1528362023061](assets/1528362023061.png)

然后获取到付款链接后，根据链接生成二维码：

 ![1528362420151](assets/1528362420151.png)





刷新页面，查看效果：

 ![1528362464276](assets/1528362464276.png)

此时，客户用手机扫描二维码，可以看到付款页面。



# 4.支付结果通知

支付以后，我们后台需要修改订单状态。我们怎么得知有没有支付成功呢？

在我们的请求参数中，有一个notify_url的参数，是支付的回调地址。当用户支付成功后，微信会主动访问这个地址，并携带支付结果信息。

那么，这个notify_url该怎么用呢？


## 4.1.notify_url                                  

### 1）什么是notify_url

参数中有一个非常重要的，叫做notify_url的：

![1535981510532](assets/1535981510532.png)

基于上文的介绍我们知道，这个地址是在支付成功后的异步结果通知。官网介绍如下：

支付完成后，微信会把相关支付结果和用户信息发送给商户，商户需要接收处理，并返回应答。

所以，此处的地址必须是一个外网可访问地址，而且我们要定义好回调的处理接口。

http://api.leyou.com/api/pay/wx/notify

### 2）内网穿透

此处我们肯定不能写：http://api.leyou.com/api/pay/，这个域名未经备案，是不被识别的。如何才能获取一个能够外网访问的域名呢？

我们可以通过内网穿透来实现，那么什么是内网穿透呢？ 

![1535984453478](assets/1535984453478.png)

**简单来说内网穿透的目的是：让外网能访问你本地的应用，例如在外网打开你本地http://127.0.0.1指向的Web站点。**



在这里有一篇播客，详细介绍了几种内网穿透策略：[一分钟了解内网穿透](https://blog.csdn.net/zhangguo5/article/details/77848658?utm_source=5ibc.net&utm_medium=referral)



这里我们使用一个免费的内网穿透工具：Natapp：[NATAPP官网](https://natapp.cn)

![1535984650173](assets/1535984650173.png)

详细教程在这里：[一分钟的natapp快速新手教程](https://natapp.cn/article/natapp_newbie)

启动后的样子：

 ![1555650481104](assets/1555650481104.png)

比如此处，我使用的natapp得到的域名是：http://ff7hgc.natappfree.cc，并且我设置指向到`127.0.0.1:10010`位置，也就是我的网关服务。



### 3）配置回调地址

设置内网穿透地址到配置文件application.yml：

```yaml
ly:
  pay:
    appId: wx8397f8696b538317
    mchId: 1473426802
    key: T6m9iK73b0kn9g5v426MKfHQH7X8rKwb
    notifyUrl: http://5jcy6r.natappfree.cc/api/pay/wx/notify
    payType: NATIVE
```

WxPayConfigImpl中本来就有notifyURL属性，因此会被自动注入。

### 4）网关白名单

因为异步回调是微信来访问我们的，因此不应该对登录做校验，我们把这个地址配置到白名单，修改ly-gateway中的application.yml

```yaml
ly:
  filter:
    allowPaths:
      - /api/pay
```

然后，将/api/pay映射到订单微服务：

```yaml
zuul:
  prefix: /api # 添加路由前缀
  routes:
    pay-service:
      path: /pay/**
      serviceId: order-service
      strip-prefix: false
```

## 4.2.支付结果通知API

来看官网关于结果通知的介绍：https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=9_7&index=8

> 应用场景

支付完成后，微信会把相关支付结果和用户信息发送给商户，商户需要接收处理，并返回应答。

对后台通知交互时，如果微信收到商户的应答不是成功或超时，微信认为通知失败，微信会通过一定的策略定期重新发起通知，尽可能提高通知的成功率，但微信不保证通知最终能成功。 （通知频率为15/15/30/180/1800/1800/1800/1800/3600，单位：秒）

**注意：同样的通知可能会多次发送给商户系统。商户系统必须能够正确处理重复的通知。**

推荐的做法是，当收到通知进行处理时，首先检查对应业务数据的状态，判断该通知是否已经处理过，如果没有处理过再进行处理，如果处理过直接返回结果成功。在对业务数据进行状态检查和处理之前，要采用数据锁进行并发控制，以避免函数重入造成的数据混乱。

**特别提醒：商户系统对于支付结果通知的内容一定要做`签名验证,并校验返回的订单金额是否与商户侧的订单金额一致`，防止数据泄漏导致出现“假通知”，造成资金损失。**



支付完成后，微信服务会自动向`notify_url`地址发起POST请求，请求参数是xml格式：

| 字段名     | 变量名      | 必填 | 类型        | 示例值  | 描述                                                         |
| ---------- | ----------- | ---- | ----------- | ------- | ------------------------------------------------------------ |
| 返回状态码 | return_code | 是   | String(16)  | SUCCESS | SUCCESS/FAIL此字段是通信标识，非交易标识，交易是否成功需要查看trade_state来判断 |
| 返回信息   | return_msg  | 是   | String(128) | OK      | 当return_code为FAIL时返回信息为错误原因 ，例如签名失败参数格式校验错误 |

通信成功，会返回下面信息：

| 签名           | sign           | 是   | String(32)  | C380BEC2BFD.. | 名，详见[签名算法](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_3) |
| -------------- | -------------- | ---- | ----------- | ------------- | ------------------------------------------------------------ |
| 签名类型       | sign_type      | 否   | String(32)  | HMAC-SHA256   | 签名类型，目前支持HMAC-SHA256和MD5，默认为MD5                |
| 业务结果       | result_code    | 是   | String(16)  | SUCCESS       | SUCCESS/FAIL                                                 |
| 错误代码       | err_code       | 否   | String(32)  | SYSTEMERROR   | 错误返回的信息描述                                           |
| 错误代码描述   | err_code_des   | 否   | String(128) | 系统错误      | 错误返回的信息描述                                           |
| 用户标识       | openid         | 是   | String(128) | wxd930ea54f   | 用户在商户appid下的唯一标识                                  |
| 交易类型       | trade_type     | 是   | String(16)  | JSAPI         | JSAPI、NATIVE、APP                                           |
| 订单金额       | total_fee      | 是   | Int         | 100           | 订单总金额，单位为分                                         |
| 现金支付金额   | cash_fee       | 是   | Int         | 100           | 现金支付金额订单现金支付金额，详见[支付金额](https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=4_2) |
| 微信支付订单号 | transaction_id | 是   | String(32)  | 121775250120  | 微信支付订单号                                               |
| 商户订单号     | out_trade_no   | 是   | String(32)  | 12123212112   | 商户系统内部订单号，要求32个字符内，只能是数字、大小写字母_-\|*@ ，且在同一个商户号下唯一。 |

我们需要返回给微信的结果：

```xml
<xml>
  <return_code><![CDATA[SUCCESS]]></return_code>
  <return_msg><![CDATA[OK]]></return_msg>
</xml>
```



## 4.3.编写回调接口

先分析接口需要的四个数据：

- 请求方式：官方文档虽然没有明说，但是测试得出是POST请求
- 请求路径：我们之前指定的notify_url的路径是：/pay/wx/notify
- 请求参数：是xml格式数据，包括支付的结果和状态
- 返回结果：也是xml，表明是否成功

因为要接收xml格式数据，因此我们需要引入解析xml的依赖：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <version>2.9.6</version>
</dependency>
```

然后编写controller：

```java
package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/pay")
public class PayController {


    @Autowired
    private OrderService orderService;

    /**
     * 微信支付成功回调
     * @param result
     * @return
     */
    @PostMapping(value = "/wx/notify", produces = "application/xml")
    public Map<String, String> hello(@RequestBody Map<String,String> result){
        // 处理回调
        log.info("[支付回调] 接收微信支付回调, 结果:{}", result);
        orderService.updateOrderStatus(result);

        // 返回成功
        Map<String, String> msg = new HashMap<>();
        msg.put("return_code", "SUCCESS");
        msg.put("return_msg", "OK");
        return msg;
    }
}
```



因为需要对结果的签名进行验证，所以在`PayHelper`中定义一个校验签名的算法：

```java
//校验签名的算法
void isValidSign(Map<String, String> result) throws Exception {
    boolean boo1 = WXPayUtil.isSignatureValid(result, payConfig.getKey(), WXPayConstants.SignType.MD5);
    boolean boo2 = WXPayUtil.isSignatureValid(result, payConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
    if (!boo1 && !boo2) {
        throw new RuntimeException("【微信支付回调】签名有误");
    }
}

//另外，支付是否成功，需要校验业务状态才知道，我们在PayHelper编写一个校验业务状态的方法
public void checkResultCode(Map<String, String> result) {
    // 检查业务状态
    String resultCode = result.get("result_code");
    if ("FAIL".equals(resultCode)) {
        log.error("【微信支付】微信支付业务失败，错误码：{}，原因：{}", result.get("err_code"), result.get("err_code_des"));
        throw new RuntimeException("【微信支付】微信支付业务失败");
    }
}
```



Service 代码：

service中需要完成下列代码；

- 签名校验
- 数据校验
  - 订单号码校验
  - 订单金额校验
- 更新订单状态

```java
    /**
     * 接收微信通知，修改订单状态
     * @param reqMap
     */
    public void updateOrderStatus(Map<String, String> reqMap) {
        if(reqMap.get("result_code") == null || !reqMap.get("result_code").equals(WXPayConstants.SUCCESS)){
            throw new LyException(ExceptionEnum.INVALID_NOTIFY_PARAM);
        }
        try {
            boolean bWeiChat = wxPay.isPayResultNotifySignatureValid(reqMap);
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.INVALID_NOTIFY_PARAM);
        }
//      获取系统订单号
        String orderId = reqMap.get("out_trade_no");
//        通过orderid查询订单数据
        TbOrder tbOrder = tbOrderService.getById(Long.valueOf(orderId));
        if(tbOrder == null){
            throw new LyException(ExceptionEnum.INVALID_NOTIFY_PARAM);
        }
        String totalFeeStr = reqMap.get("total_fee");
        Long totalFee = Long.valueOf(totalFeeStr);
//        TODO 测试注释代码，上线需要打开注释判断订单金额
//        if(tbOrder.getActualFee().longValue()!= totalFee.longValue()){
//            throw new LyException(ExceptionEnum.INVALID_NOTIFY_PARAM);
//        }
        if(tbOrder.getStatus().intValue() != OrderStatusEnum.INIT.value().intValue()){
            //可以抛异常，也可以回复微信已经成功
            throw new LyException(ExceptionEnum.INVALID_NOTIFY_PARAM);
        }
//        更新订单状态
//        update tb_order set status=? where order_id=? and status = 1
        UpdateWrapper<TbOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(TbOrder::getOrderId,orderId)
                .eq(TbOrder::getStatus,OrderStatusEnum.INIT.value());
        updateWrapper.lambda().set(TbOrder::getStatus,OrderStatusEnum.PAY_UP.value());
        boolean b = tbOrderService.update(updateWrapper);
        if(!b){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

    }
```





# 5.系统支付状态查询

当用户扫码支付成功，会自动调用回调接口，从而修改订单状态，完成订单支付。

但是，页面上并不知道支付是否成功。怎么办？

## 5.1.页面查询支付状态

因为不知道用户什么时候会支付，也不知道支付有没有成功，因此页面会采用定时任务，不断查询订单支付的状态：

```js
// 开启定时任务，查询付款状态
const taskId = setInterval(() => {
    ly.http.get("/order/state/" + id)
        .then(resp => {
        let i = resp.data;
        if (i !== 1) {
            // 付款成功
            clearInterval(taskId);
            // 跳转到付款成功页
            location.href = "/paysuccess.html?orderId=" + id;
        }
    }).catch((e) => {
        alert("支付状态查询失败，请刷新页面重试。");
        clearInterval(taskId);
    })
}, 3000);
// 同时设置一个定时任务，10分钟后，终止查询，认为付款失败
setTimeout(() => {
    clearInterval(taskId);
    location.href = "/payfail.html?orderId=" + id;
}, 600000)
```

每隔3秒就会查询一次，为了防止用户一直不支付的情况，又设置了一个定时任务，10分钟后跳转到支付失败页面。



## 5.2.支付状态查询接口

上面的查询请求 分析：

- 请求方式：Get
- 请求路径 ：/state/{id}
- 请求参数：订单id
- 返回结果：1或者其它，1代表未支付，其它是已经支付

controller：

```java
@GetMapping("/state/{id}")
public ResponseEntity<Integer> findPayState(@PathVariable("id") Long orderId) {
    return ResponseEntity.ok(orderService.findPayStatus(orderId));
}
```

service：

```java
/**
     * 支付状态查询
     * @param orderId
     * @return
     */
    public Integer findPayStatus(Long orderId) {
        TbOrder tbOrder = orderService.getById(orderId);
        if(tbOrder == null){
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        return tbOrder.getStatus();
    }
```

## 5.3主动查询支付结果

如果没有做接收微信支付结果通知的代码，也可以主动发起支付结果查询。具体逻辑：

  1.组装微信查询支付状态所需要的必填参数，并将参数转为xml格式

​	2.基于restTemplate调用微信支付平台，完成支付状态操作操作

​	3.将查询结果响应给浏览器端，完成页面跳转。

​	注意：当用户支付完成后，更新乐优商城订单状态为已支付。并把当前时间作为支付时间更新到数据库表中

```java
@Override
    public Integer queryPayStatus(String orderId)  {
        try {
            // 1、组装微信查询支付状态所需要的必填参数
            Map<String,String> paramMap = new HashMap<>();
            paramMap.put("appid",payProperties.getAppID());
            paramMap.put("mch_id",payProperties.getMchID());
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
            paramMap.put("out_trade_no",orderId);
            //将参数转xml
            String paramXml = WXPayUtil.generateSignedXml(paramMap, payProperties.getKey());
            //2、基于restTemplate工具类，调用微信支付平台，完成支付状态操作操作           
             String resultString = restTemplate.postForObject("https://api.mch.weixin.qq.com/pay/orderquery", paramXml, String.class);
            //3、处理响应结果
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultString);
            //支付成功，返回状态1
            if("SUCCESS".equals(resultMap.get("trade_state"))){
                //更新订单状态
                TbOrder orderStatus = new TbOrder();
                orderStatus.setStatus(OrderStatusEnum.PAY_UP.value());
                orderStatus.setOrderId(Long.parseLong(orderId));
                orderStatus.setPayTime(new Date());
                int size = this.updateById(orderStatus);
                if(size != 1){
                    throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
                }
            }
            return "SUCCESS".equals(resultMap.get("trade_state"))?1:0;

        } catch (Exception e) {
            e.printStackTrace();
            throw new LyException(ExceptionEnum.INVALID_PAY_STATUS);
        }
    }
```



# 6.SpringSchedule

我们完成了订单的创建，并且生成了支付的二维码，但是如果用户一直不支付，就会导致商品库存被占用，而不能形成有效交易。在商品资源有限时，会损害商家的利益。

比较常见的做法，是定时统计，对于超时未支付的订单，自动关闭并释放库存。

这就需要定时任务的支持了。

## 6.1.常见的定时任务框架

目前常用的定时任务实现:

| 实现方式        | cron表达式 | 固定时间执行 | 固定频率执行 | 开发难易程度 |
| --------------- | ---------- | ------------ | ------------ | ------------ |
| JDK 的TimeTask  | 不支持     | 支持         | 支持         | 复杂         |
| Spring Schedule | 支持       | 支持         | 支持         | 简单         |
| Quartz          | 支持       | 支持         | 支持         | 难           |

从以上表格可以看出，Spring Schedule框架功能完善，简单易用。对于中小型项目需求，Spring Schedule是完全可以胜任的。



## 6.2.简介

Spring Schedule是Spring  Framework的其中一部分功能：

![1555666217625](assets/1555666217625.png)

并且在SpringBoot中已经默认对Spring的Schedule实现了自动配置，使用时只需要简单注解和部分属性设置即可。

## 6.3.快速入门

### 6.3.1.开启定时任务

要开启定时任务功能，只需要在启动类上加载一个`@EnableScheduling`注解即可：

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling//开启定时任务
@MapperScan("com.leyou.order.mapper")
public class LyOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(LyOrderApplication.class, args);
    }
}
```

### 6.3.3.定义任务

定义任务，需要散步：

- 声明类，通过`@Component`注解注册到Spring容器
- 类中定义方法，方法内部编写任务逻辑
- 方法上添加注解`@Scheduled(fixedRate = 1000)`来定义任务执行频率，
  - 这里的fiexRate=1000，代表是每隔1000毫秒执行一次

![1556163899025](assets/1556163899025.png) 

```java
package com.leyou.task.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class HelloJob {

    @Scheduled(fixedRate = 1000)
    public void hello(){
        log.info("hello spring schedule!");
    }
}
```

### 6.3.4.启动测试

启动项目，可以在控制台看到任务执行情况：

![1555667399323](assets/1555667399323.png)

## 6.4.配置

定时任务有许多可以自定义的配置属性：

### 6.4.1.任务线程池大小

默认情况下，定时任务的线程池大小只有1，**当任务较多执行频繁时，会出现阻塞等待的情况**，任务调度器就会出现**时间漂移**，任务执行时间将不确定。

为了避免这样的情况发生，我们需要自定义线程池的大小：

修改application.yml即可实现：

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 10
```

重启项目后测试：

![1555667762439](assets/1555667762439.png)

可以看到执行时会切换到不同的线程执行。

### 6.4.2.定时策略

在方法上添加注解`@Scheduled`可以控制定时执行的频率，有三种策略：

- fixedRate：按照固定时间频率执行，单位毫秒，即每xx毫秒执行一次。
  - 如果上一个任务阻塞导致任务积压，则会在当前任务执行后，一次把多个积压的任务都执行完成
  - 举例：假如任务执行每秒1次，而第一个任务执行耗时4秒，会导致4个任务积压，在第一个任务执行后，积压的4个任务会立即执行，不再等待
- fixedDelay：固定延迟执行，单位毫秒，即前一个任务执行结束后xx毫秒执行第二个任务。
  - 如果上一个任务阻塞导致任务积压，则会在当前任务执行后xx毫秒执行下一个任务
- cron：知名的cron表达式，使用表达式规则来定义任务执行策略，与fixedDelay类似的。



### 6.4.3.cron表达式

什么是cron表达式呢？

Cron表达式是一个字符串，字符串包含6或7个域，每一个域代表一个含义，例如秒、分。域和域之间以空格隔开，有如下两种语法格式：

- Seconds Minutes Hours DayofMonth Month DayofWeek Year
- Seconds Minutes Hours DayofMonth Month DayofWeek

cron表达式规则：

| 域                       | 允许值                                 | 允许的特殊字符           |
| ------------------------ | -------------------------------------- | ------------------------ |
| 秒（Seconds）            | 0~59的整数                             | ,   -   *   /            |
| 分（*Minutes*）          | 0~59的整数                             | ,   -   *   /            |
| 小时（*Hours*）          | 0~23的整数                             | ,   -   *   /            |
| 日期（*DayofMonth*）     | 1~31的整数（但是你需要考虑你月的天数） | ,   -   *  ?   /  L W C  |
| 月份（*Month*）          | 1~12的整数或者 JAN-DEC                 | ,   -   *   /            |
| 星期（*DayofWeek*）      | 1~7的整数或者 SUN-SAT （1=SUN）        | ,   -   *  ?   /  L C  # |
| 年(可选，留空)（*Year*） | 1970~2099                              | ,   -   *   /            |

每个域上一般都是数字，或者指定允许的特殊字符：

| 特殊字符 | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| ?        | 只能用在DayofMonth和DayofWeek两个域中的一个。它表示不确定的值 |
| -        | 表示范围。例如在Hours域使用5-8，表示从5点、6点、7点、8点各执行一次 |
| ,        | 表示列出枚举值。例如：在week域使用FRI,SUN，表示星期五和星期日执行 |
| /        | 一般用法：x/y，从x开始，每次递增y。如果放在minutes域，5/15，表示每小时的5分钟开始，每隔15分钟一次，即：5分钟、20分钟、35分钟、50分钟时执行 |
| L        | 表示最后，只能出现在DayofWeek和DayofMonth域。如果在DayOfMonth中，代表每个月的最后一天。如果是在DayOfWeek域，表示每周最后一天（周六），但是如果是：数字+L，如6L表示每月的 最后一个周五 |
| W        | 表示最近的有效工作日(周一到周五),只能出现在DayofMonth域，系统将在离指定日期的最近的有效工作日触发事件。例如：在 DayofMonth使用5W，如果5日是星期六，则将在最近的工作日：星期五，即4日触发。如果5日是星期天，则在6日(周一)触发；如果5日在星期一到星期五中的一天，则就在5日触发。另外一点，W的最近寻找不会跨过月份 。 |
| LW       | 两个字符可以连用，表示在某个月最后一个工作日，即最后一个星期五 |
| *        | 表示匹配该域的任意值。假如在Minutes域使用, 即表示每分钟都会触发事件 |
| #        | 用在DayOfMonth中，确定每个月第几个星期几。例如在4#2，表示某月的第二个星期三（2表示当月的第二周，4表示这周的第4天，即星期三）。 |



示例：

| 表达式                     | 含义                                                 |
| -------------------------- | ---------------------------------------------------- |
| `0 0 2 1 * ?  *`           | 表示在每月的1日的凌晨2点执行任务                     |
| `0 15 10 ?  * MON-FRI`     | 表示周一到周五每天上午10:15执行作                    |
| `0 15 10 ? * 6L 2002-2020` | 表示2002-2006年的每个月的最后一个星期五上午10:15执行 |
| `0 0 9-21 * * 2-7`         | 朝九晚9工作时间  996                                 |

我们把代码修改一下：

```java
@Slf4j
@Component
public class HelloJob {

    @Scheduled(cron = "0/2 * * * * ?")
    public void hello() throws InterruptedException {
        log.info("hello spring schedule!");
    }
}
```

测试：

![1555684079046](assets/1555684079046.png)

# 7.Redis分布式锁

因为Redis具备高性能、高可用、高并发的特性，这里，我们会采用Redis来实现分布式锁。

## 7.1.Redis分布式锁原理

上面讲过，分布式锁的关键是**多进程共享的内存标记**，因此只要我们在Redis中放置一个这样的标记就可以了。不过在实现过程中，不要忘了我们需要实现下列目标：

- 多进程可见：多进程可见，否则就无法实现分布式效果
- 避免死锁：死锁的情况有很多，我们要思考各种异常导致死锁的情况，保证锁可以被释放
- 排它：同一时刻，只能有一个进程获得锁
- 高可用：避免锁服务宕机或处理好宕机的补救措施

在Redis中我们可以用下面的方式来解决上述问题：

- **多进程可见**：多进程可见，否则就无法实现分布式效果

  - redis本身就是多服务共享的，因此自然满足

- **排它**：同一时刻，只能有一个进程获得锁

  - 我们需要利用Redis的setnx命令来实现，setnx是set when not exits的意思。当多次执行setnx命令时，只有第一次执行的才会成功并返回1，其它情况返回0：
  - ![1555935393771](assets/1555935393771.png) 
  - 我们定义一个固定的key，多个进程都执行setnx，设置这个key的值，返回1的服务获取锁，0则没有获取

- **避免死锁**：死锁的情况有很多，我们要思考各种异常导致死锁的情况

  - 比如服务宕机后的锁释放问题，我们设置锁时最好设置锁的有效期，如果服务宕机，有效期到时自动删除锁。

    ![1555935852042](assets/1555935852042.png) 

- **高可用**：避免锁服务宕机或处理好宕机的补救措施

  - 利用Redis的主从、哨兵、集群，保证高可用

## 7.2.分布式锁版本1

### 7.2.1.流程

按照上面所述的理论，分布式锁的流程大概如下：

![1555942085021](assets/1555942085021.png) 

基本流程：

- 1、通过set命令设置锁
- 2、判断返回结果是否是OK
  - 1）Nil，获取失败，结束或重试（自旋锁）
  - 2）OK，获取锁成功
    - 执行业务
    - 释放锁，DEL 删除key即可
- 3、异常情况，服务宕机。超时时间EX结束，会自动释放锁

### 7.2.2.代码实现

定义一个锁接口：

```java
package com.leyou.task.utils;


public interface RedisLock {
    boolean lock(long releaseTime);
    void unlock();
}

```



先定义一个锁工具：

```java
package com.leyou.task.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;


public class SimpleRedisLock implements RedisLock{

    private StringRedisTemplate redisTemplate;
    /**
     * 设定好锁对应的 key
     */
    private String key;
    /**
     * 锁对应的值，无意义，写为1
     */
    private static final String value = "1";

    public SimpleRedisLock(StringRedisTemplate redisTemplate, String key) {
        this.redisTemplate = redisTemplate;
        this.key = key;
    }

    public boolean lock(long releaseTime) {
        // 尝试获取锁
        Boolean boo = redisTemplate.opsForValue().setIfAbsent(key, value, releaseTime, TimeUnit.SECONDS);
        // 判断结果
        return boo != null && boo;
    }

    public void unlock(){
        // 删除key即可释放锁
        redisTemplate.delete(key);
    }
}
```

在定时任务中使用锁：

```java
package com.leyou.task.job;

import com.leyou.task.utils.SimpleRedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HelloJob {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0/10 * * * * ?")
    public void hello() {
        // 创建锁对象
        RedisLock lock = new SimpleRedisLock(redisTemplate, "lock");
        // 获取锁,设置自动失效时间为50s
        boolean isLock = lock.lock(50);
        // 判断是否获取锁
        if (!isLock) {
            // 获取失败
            log.info("获取锁失败，停止定时任务");
            return;
        }
        try {
            // 执行业务
            log.info("获取锁成功，执行定时任务。");
            // 模拟任务耗时
            Thread.sleep(500);
        } catch (InterruptedException e) {
            log.error("任务执行异常", e);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
}
```

![1556163989735](assets/1556163989735.png) 



## 7.3.分布式锁版本2

刚才的锁有没有什么问题？

### 7.3.1.释放锁的问题

大家思考一下，释放锁就是用DEL语句把锁对应的key给删除，有没有这么一种可能性：

1. 三个进程：A和B和C，在执行任务，并争抢锁，此时A获取了锁，并设置自动过期时间为10s
2. A开始执行业务，因为某种原因，业务阻塞，耗时超过了10秒，此时锁自动释放了
3. B恰好此时开始尝试获取锁，因为锁已经自动释放，成功获取锁
4. A此时业务执行完毕，执行释放锁逻辑（删除key），于是B的锁被释放了，而B其实还在执行业务
5. 此时进程C尝试获取锁，也成功了，因为A把B的锁删除了。

问题出现了：B和C同时获取了锁，违反了排它性！

如何解决这个问题呢？我们应该在删除锁之前，判断这个锁是否是自己设置的锁，如果不是（例如自己的锁已经超时释放），那么就不要删除了。



那么问题来了：**如何得知当前获取锁的是不是自己**呢？

对了，我们可以在set 锁时，存入自己的信息！删除锁前，判断下里面的值是不是与自己相等，如果不等，就不要删除了。

### 7.3.2.流程图

来看下流程的变化：

![1555944884321](assets/1555944884321.png) 

在释放锁之前，多了一步根据判断，判断锁的value释放跟自己存进去的一致。

### 7.3.3.代码实现

```java
package com.leyou.task.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class SimpleRedisLock implements RedisLock{

    private StringRedisTemplate redisTemplate;
    /**
     * 设定好锁对应的 key
     */
    private String key;
    /**
     * 存入的线程信息的前缀，防止与其它JVM中线程信息冲突
     */
    private final String ID_PREFIX = UUID.randomUUID().toString();

    public SimpleRedisLock(StringRedisTemplate redisTemplate, String key) {
        this.redisTemplate = redisTemplate;
        this.key = key;
    }

    public boolean lock(long releaseTime) {
        // 获取线程信息作为值，方便判断是否是自己的锁
        String value = ID_PREFIX + Thread.currentThread().getId();
        // 尝试获取锁
        Boolean boo = redisTemplate.opsForValue().setIfAbsent(key, value, releaseTime, TimeUnit.SECONDS);
        // 判断结果
        return boo != null && boo;
    }

    public void unlock(){
        // 获取线程信息作为值，方便判断是否是自己的锁
        String value = ID_PREFIX + Thread.currentThread().getId();
        // 获取现在的锁的值
        String val = redisTemplate.opsForValue().get(key);
        // 判断是否是自己
        if(value.equals(val)) {
            // 删除key即可释放锁
            redisTemplate.delete(key);
        }
    }
}
```





## 7.4.分布式锁版本3

刚才的锁有没有什么问题？

如果我们在获取锁以后，执行代码的过程中，再次尝试获取锁，执行setnx肯定会失败，因为锁已经存在了。这样就是**不可重入锁**，有可能导致死锁。

如何解决呢？

当然是想办法改造成**可重入锁**。

### 7.4.1.重入锁

什么叫做可重入锁呢？

> 可重入锁，也叫做递归锁，指的是在同一线程内，外层函数获得锁之后，内层递归函数仍然可以获取到该锁。换一种说法：**同一个线程再次进入同步代码时，可以使用自己已获取到的锁。**



可重入锁可以避免因同一线程中多次获取锁而导致死锁发生。



那么，如何实现可重入锁呢？

其中的关键，就是**在锁已经被使用时，判断这个锁是否是自己的，如果是则再次获取**。



我们可以在set锁的值是，**存入获取锁的线程的信息**，这样下次再来时，就能知道当前持有锁的是不是自己，如果是就允许再次获取锁。



要注意，因为锁的获取是**可重入**的，因此必须记录重入的次数，这样不至于在释放锁时一下就释放掉，而是逐层释放。

因此，不能再使用简单的key-value结构，这里推荐使用hash结构：

- key：lock
- hashKey：线程信息
- hashValue：重入次数，默认1



释放锁时，每次都把**重入次数减一**，减到0说明多次获取锁的逻辑都执行完毕，才可以删除key，释放锁



### 7.4.2.流程图

这里重点是获取锁的流程：

![1556164092317](assets/1556164092317.png)

下面我们假设锁的key为“`lock`”，hashKey是当前线程的id：“`threadId`”，锁自动释放时间假设为20

获取锁的步骤：

- 1、判断lock是否存在 `EXISTS lock`
  - 存在，说明有人获取锁了，下面判断是不是自己的锁
    - 判断当前线程id作为hashKey是否存在：`HEXISTS lock threadId`
      - 不存在，说明锁已经有了，且不是自己获取的，锁获取失败，end
      - 存在，说明是自己获取的锁，重入次数+1：`HINCRBY lock threadId 1`，去到步骤3
  - 2、不存在，说明可以获取锁，`HSET key threadId 1`
  - 3、设置锁自动释放时间，`EXPIRE lock 20`

释放锁的步骤：

- 1、判断当前线程id作为hashKey是否存在：`HEXISTS lock threadId`
  - 不存在，说明锁已经失效，不用管了
  - 存在，说明锁还在，重入次数减1：`HINCRBY lock threadId -1`，获取新的重入次数
- 2、判断重入次数是否为0：
  - 为0，说明锁全部释放，删除key：`DEL lock`
  - 大于0，说明锁还在使用，重置有效时间：`EXPIRE lock 20`



### 7.4.3.实现分析

上述流程有一个最大的问题，就是有大量的判断，这样在多线程运行时，会有线程安全问题，除非能保证**执行**

**命令的原子性**。

因此，这里使用java代码无法实现，那该怎么办呢？

Redis支持一种特殊的执行方式：lua脚本执行，lua脚本中可以定义多条语句，语句执行具备原子性。



## 7.5.Redis的Lua脚本（了解）

其实实现Redis的原子操作有多种方式，比如Redis事务，但是相比而言，使用Redis的Lua脚本更加优秀，具有不可替代的好处：

- 原子性：redis会将整个脚本作为一个整体执行，不会被其他命令插入。
- 复用：客户端发送的脚本会永久存在redis中，以后可以重复使用，而且各个Redis客户端可以共用。
- 高效：Lua脚本解析后会形成缓存，不用每次执行都解析。
- 减少网络开销：Lua脚本缓存后，可以形成SHA值，作为缓存的key，以后调用可以直接根据SHA值来调用脚本，不用每次发送完整脚本，较少网络占用和时延

### 7.5.1.Redis脚本命令：

通过下面这个命令，可以看到所有脚本相关命令：

```
help @scripting
```



我们看一些常用命令

> EVAL命令：

![1556029159652](assets/1556029159652.png) 

直接执行一段脚本，参数包括：

- script：脚本内容，或者脚本地址
- numkeys：脚本中用到的key的数量，接下来的numkeys个参数会作为key参数，剩下的作为arg参数
- key：作为key的参数，会被存入脚本环境中的KEYS数组，角标从1开始
- arg：其它参数，会被存入脚本环境中的ARGV数组，角标从1开始



示例：`EVAL "return 'hello world!'" 0`，其中：

- `"return 'hello world!'"`：就是脚本的内容，直接返回字符串，没有别的命令
- `0`：就是说没有用key参数，直接返回

效果：

![1556030139226](assets/1556030139226.png) 



> SCRIPT LOAD命令

![1556029464469](assets/1556029464469.png) 

将一段脚本编译并缓存起来，生成一个SHA1值并返回，作为脚本字典的key，方便下次使用。

参数script就是脚本内容或地址。

以之前案例中的的脚本为例：

![1556030196610](assets/1556030196610.png) 

此处返回的`ada0bc9efe2392bdcc0083f7f8deaca2da7f32ec`就是脚本缓存后得到的sha1值。

在脚本字典中，每一个这样的sha1值，对应一段解析好的脚本：

![1556030293491](assets/1556030293491.png) 



> EVALSHA 命令：

![1556029524238](assets/1556029524238.png) 

与EVAL类似，执行一段脚本，区别是通过脚本的sha1值，去脚本缓存中查找，然后执行，参数：

- sha1：就是脚本对应的sha1值

我们用刚刚缓存的脚本为例：

![1556030354363](assets/1556030354363.png) 



### 7.5.2.Lua基本语法

Lua脚本遵循Lua的基本语法，这里我们简单介绍几个常用的：

> redis.call()和redis.pcall()

这两个函数是调用redis命令的函数，区别在于call执行过程中出现错误会直接返回错误；pcall则在遇到错误后，会继续向下执行。基本语法类似：

```lua
redis.call("命令名称", 参数1， 参数2 ...)
```

例如这样的脚本：`return redis.call('set', KEYS[1], ARGV[1])`

- 'set'：就是执行set 命令
- KEYS[1]：从脚本环境中KEYS数组里取第一个key参数
- ARGV[1]：从脚本环境中ARGV数组里取第一个arg参数

完整示例：

![1556031114634](assets/1556031114634.png) 

执行这段脚本时传入的参数：

- 1：声明key只有一个，接下来的第一个参数作为key参数
- name：key参数，会被存入到KEYS数组
- Jack：arg参数，会被存入ARGV数组



> 条件判断和变量

条件判断语法：`if (条件语句) then ...; else ...; end;`

变量接收语法：`local num = 123;`

示例：

```lua
local val = redis.call('get', KEYS[1]);
if (val > ARGV[1]) then 
    return 1; 
else 
	return 0; 
end;
```

基本逻辑：获取指定key的值，判断是否大于指定参数，如果大于则返回1，否则返回0

执行：

![1556032181883](assets/1556032181883.png)

- 可以看到num一开始是321。
- 我们保存脚本，
- 然后执行并传递num，400。判断num是否大于400，
- 结果返回0.

### 7.5.3.编写分布式锁脚本

这里我们假设有3个参数：

- KEYS[1]：就是锁的key
- ARGV[1]：就是线程id信息
- ARGV[2]：锁过期时长

首先是获取锁：

```lua
if (redis.call('EXISTS', KEYS[1]) == 0) then
    redis.call('HSET', KEYS[1], ARGV[1], 1);
    redis.call('EXPIRE', KEYS[1], ARGV[2]);
    return 1;
end;
if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 1) then
    redis.call('HINCRBY', KEYS[1], ARGV[1], 1);
    redis.call('EXPIRE', KEYS[1], ARGV[2]);
    return 1;
end;
return 0;
```

然后是释放锁：

```lua
if (redis.call('HEXISTS', KEYS[1], ARGV[1]) == 0) then
    return nil;
end;
local count = redis.call('HINCRBY', KEYS[1], ARGV[1], -1);
if (count > 0) then
    redis.call('EXPIRE', KEYS[1], ARGV[2]);
    return nil;
else
    redis.call('DEL', KEYS[1]);
    return nil;
end;
```



### 7.5.4.Java执行Lua脚本

`RedisTemplate`中提供了一个方法，用来执行Lua脚本：

![1556162076875](assets/1556162076875.png)

包含3个参数：

- `RedisScript<T> script`：封装了Lua脚本的对象
- `List<K> keys`：脚本中的key的值
- `Object ... args`：脚本中的参数的值

因此，要执行Lua脚本，我们需要先把脚本封装到`RedisScript`对象中，有两种方式来构建`RedisScript`对象：

> 方式1：

通过RedisScript中的静态方法：

![1556162311151](assets/1556162311151.png)

这个方法接受两个参数：

- `String script`：Lua脚本
- `Class<T> resultType`：返回值类型

需要把脚本内容写到代码中，作为参数传递，不够优雅。

> 方式二

另一种方式，就是自己去创建`RedisScript`的实现类`DefaultRedisScript`的对象：

![1556162540499](assets/1556162540499.png)

可以把脚本文件写到classpath下的某个位置，然后通过加载这个文件来获取脚本内容，并设置给`DefaultRedisScript`实例。

此处我们选择方式二。

### 7.5.5.可重入分布式锁的实现

首先在classpath中编写两个Lua脚本文件：

![1556162696842](assets/1556162696842.png) 

然后编写一个新的RedisLock实现：ReentrantRedisLock，利用静态代码块来加载脚本并初始化：

![1556163851408](assets/1556163851408.png) 

```java
public class ReentrantRedisLock {
    // 获取锁的脚本
    private static final DefaultRedisScript<Long> LOCK_SCRIPT;
    // 释放锁的脚本
    private static final DefaultRedisScript<Object> UNLOCK_SCRIPT;
    static {
        // 加载释放锁的脚本
        LOCK_SCRIPT = new DefaultRedisScript<>();
        LOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("lock.lua")));
        LOCK_SCRIPT.setResultType(Long.class);

        // 加载释放锁的脚本
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("unlock.lua")));
    }
    
    // 其它代码略
}
```



然后实现RedisLock并实现lock和unlock方法，完整代码如下：

```java
public class ReentrantRedisLock implements RedisLock {

    private StringRedisTemplate redisTemplate;
    /**
     * 设定好锁对应的 key
     */
    private String key;

    /**
     * 存入的线程信息的前缀，防止与其它JVM中线程信息冲突
     */
    private final String ID_PREFIX = UUID.randomUUID().toString();

    public ReentrantRedisLock(StringRedisTemplate redisTemplate, String key) {
        this.redisTemplate = redisTemplate;
        this.key = key;
    }

    private static final DefaultRedisScript<Long> LOCK_SCRIPT;
    private static final DefaultRedisScript<Object> UNLOCK_SCRIPT;
    static {
        // 加载释放锁的脚本
        LOCK_SCRIPT = new DefaultRedisScript<>();
        LOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("lock.lua")));
        LOCK_SCRIPT.setResultType(Long.class);

        // 加载释放锁的脚本
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("unlock.lua")));
    }
    // 锁释放时间
    private String releaseTime;

    @Override
    public boolean lock(long releaseTime) {
        // 记录释放时间
        this.releaseTime = String.valueOf(releaseTime);
        // 执行脚本
        Long result = redisTemplate.execute(
                LOCK_SCRIPT,
                Collections.singletonList(key),
                ID_PREFIX + Thread.currentThread().getId(), this.releaseTime);
        // 判断结果
        return result != null && result.intValue() == 1;
    }

    @Override
    public void unlock() {
        // 执行脚本
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(key),
                ID_PREFIX + Thread.currentThread().getId(), this.releaseTime);
    }
}
```

完成！

### 7.5.6.测试

新建一个定时任务，测试重入锁：

```java
package com.leyou.task.job;

import com.leyou.task.utils.RedisLock;
import com.leyou.task.utils.ReentrantRedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class ReentrantJob {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private int max = 2;

    @Scheduled(cron = "0/10 * * * * ?")
    public void hello() {
        // 创建锁对象
        RedisLock lock = new ReentrantRedisLock(redisTemplate, "lock");
        // 执行任务
        runTaskWithLock(lock, 1);
    }

    private void runTaskWithLock(RedisLock lock, int count) {
        // 获取锁,设置自动失效时间为50s
        boolean isLock = lock.lock(50);
        // 判断是否获取锁
        if (!isLock) {
            // 获取失败
            log.info("{}层 获取锁失败，停止定时任务", count);
            return;
        }
        try {
            // 执行业务
            log.info("{}层 获取锁成功，执行定时任务。", count);
            Thread.sleep(500);
            if(count < max){
                runTaskWithLock(lock, count + 1);
            }
        } catch (InterruptedException e) {
            log.error("{}层 任务执行失败", count, e);
        } finally {
            // 释放锁
            lock.unlock();
            log.info("{}层 任务执行完毕，释放锁", count);
        }
    }
}
```

DEBUG运行，打断点在获取锁后，执行任务前。





## 7.6.Redisson

虽然我们已经实现了分布式锁，能够满足大多数情况下的需求，不过我们的代码并不是万无一失。

某些场景下，可能需要实现分布式的不同类型锁，比如：公平锁、互斥锁、可重入锁、读写锁、红锁（redLock）等等。实现起来比较麻烦。

而开源框架Redisson就帮我们实现了上述的这些 锁功能，而且还有很多其它的强大功能。

### 7.6.1.什么是Redisson

来自官网的一段介绍：

Redisson是一个在Redis的基础上实现的Java驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式的Java常用对象，还提供了许多分布式服务。其中包括(`BitSet`, `Set`, `Multimap`, `SortedSet`, `Map`, `List`, `Queue`, `BlockingQueue`, `Deque`, `BlockingDeque`, `Semaphore`, `Lock`, `AtomicLong`, `CountDownLatch`, `Publish / Subscribe`, `Bloom filter`, `Remote service`, `Spring cache`, `Executor service`, `Live Object service`, `Scheduler service`) Redisson提供了使用Redis的最简单和最便捷的方法。Redisson的宗旨是促进使用者对Redis的关注分离（Separation of Concern），从而让使用者能够将精力更集中地放在处理业务逻辑上。

[官网地址](https://redisson.org/)：https://redisson.org/

[GitHub地址](https://github.com/redisson/redisson)：https://github.com/redisson/redisson

看看Redission能实现的功能：

![1556163395067](assets/1556163395067.png) 

![1556163419648](assets/1556163419648.png) 

 ![1556163437101](assets/1556163437101.png)

![1556163483872](assets/1556163483872.png) 

非常强大而且丰富！



### 7.6.2.使用Redisson分布式锁

Redisson中的分布式锁种类丰富，功能强大，因此使用Redisson的分布式锁功能是开发时的首选方案。我们一起来试一下。

#### 1）依赖

引入Redission依赖：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.10.6</version>
</dependency>
```

#### 2）配置

配置Redisson客户端：

```java
package com.leyou.task.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties prop) {
        String address = "redis://%s:%d";
        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format(address, prop.getHost(), prop.getPort()));
        return Redisson.create(config);
    }
}
```

注意：这里读取了一个名为RedisProperties的属性，因为我们引入了SpringDataRedis，Spring已经自动加载了RedisProperties，并且读取了配置文件中的Redis信息。

#### 3）常用API

RedissClient中定义了常见的锁：

![1556169332323](assets/1556169332323.png) 

```java
// 创建锁对象，并制定锁的名称
RLock lock = redissonClient.getLock("taskLock");
```



获取锁对象后，可以通过`tryLock()`方法获取锁：

![1556169690541](assets/1556169690541.png)

有3个重载的方法：

- 三个参数：获取锁，设置锁等待时间`waitTime`、释放时间`leaseTime`，时间单位`unit`。
  - 如果获取锁失败后，会在`waitTime  `减去获取锁用时的剩余时间段内继续尝试获取锁，如果依然获取失败，则认为获取锁失败；
  - 获取锁后，如果超过`leaseTime`未释放，为避免死锁会自动释放。
- 两个参数：获取锁，设置锁等待时间`time`、时间单位`unit`。释放时间`leaseTime`按照默认的30s
- 空参：获取锁，`waitTime`默认0s，即获取锁失败不重试，`leaseTime`默认30s



任务执行完毕，使用`unlock()`方法释放锁：

![1556170353278](assets/1556170353278.png) 



#### 4）完整测试代码

```java
package com.leyou.task.job;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Component
public class RedsssionJob {

    @Autowired
    private RedissonClient redissonClient;

    @Scheduled(cron = "0/10 * * * * ?")
    public void hello() {
        // 创建锁对象，并制定锁的名称
        RLock lock = redissonClient.getLock("taskLock");
        // 获取锁,设置自动失效时间为50s
        boolean isLock = lock.tryLock();
        // 判断是否获取锁
        if (!isLock) {
            // 获取失败
            log.info("获取锁失败，停止定时任务");
            return;
        }
        try {
            // 执行业务
            log.info("获取锁成功，执行定时任务。");
            // 模拟任务耗时
            Thread.sleep(500);
        } catch (InterruptedException e) {
            log.error("任务执行异常", e);
        } finally {
            // 释放锁
            lock.unlock();
            log.info("任务执行完毕，释放锁");
        }
    }
}
```



# 8.定时清理订单

回到我们今天开始时提出的需求：我们需要定时清理那些超时未支付的订单，并且保证多个订单微服务执行任务时不要重复，而是只有一个服务执行。用我们刚刚学习的分布式任务调度方案，恰好可以实现。



## 8.1.业务分析

业务主要有下列部分组成：

- 定时任务：每隔一定时间，尝试执行清理订单的任务
- 清理订单任务：
  - 1）设置一个超时时间限制，例如1小时，超过1小时未付款则取消订单
  - 2）查询当前以及超时未支付的订单（状态为1，创建时间是1小时以前）
  - 3）修改这些订单的状态为5，交易取消并关闭
  - 4）查询这些订单对应的商品信息
  - 5）恢复库存

其中，1~4的步骤都是在订单微服务完成，而步骤5是在商品微服务完成，需要远程调用。



## 8.2.商品服务加库存接口

我们先完成商品微服务的恢复库存接口，与减库存非常相似。

> 定义API接口

在`ly-item-interface`中的`ItemClient`中添加新的接口：

```java
/**
     * 加库存
     * @param skuMap 商品id及数量的map
     */
@PutMapping("/stock/plus")
void plusStock(@RequestBody Map<Long, Integer> skuMap);
```

> 业务实现

然后在GoodsController中定义handler：

注意，因为一个订单中有多个商品，因此这里接收的参数是一个map，key是商品的id，值是对应的数量num

```java
/**
     * 加库存
     * @param skuMap 商品id及数量的map
     */
@PutMapping("/stock/plus")
public ResponseEntity<Void> plusStock(@RequestBody Map<Long, Integer> skuMap){
    goodsService.plusStock(skuMap);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```

在GoodsService中添加新业务：

```java
public void plusStock(Map<Long, Integer> skuMap) {
    skuService.plusStock(skuMap);
}
```

修改TbSkuService接口 ,增加接口

```java
void plusStock(Map<Long, Integer> skuMap);
```

修改TbSkuServiceImpl，实现接口方法

```java
@Override
    @Transactional(rollbackFor = Exception.class)
    public void plusStock(Map<Long, Integer> skuMap) {
        for (Long skuId : skuMap.keySet()) {
            Integer num = skuMap.get(skuId);
            int count = this.baseMapper.plusStock(skuId,num);
            if(count != 1){
                throw new RuntimeException("恢复库存异常！");
            }
        }
    }
```

修改TbSkuMapper接口，添加新功能：

```java
@Update("UPDATE tb_sku SET stock = stock + #{num} WHERE id = #{id}")
int plusStock(@Param("id") Long id, @Param("num") Integer num);
```



## 8.3.清理订单操作

我们在`ly-order-service`的`OrderService`中，添加一个清理订单的业务，基本流程如下：

- 查询已经超时的订单
- 更新这些订单的状态
- 查询订单详情，并获取商品相关信息
- 调用商品服务更新库存



### 8.3.1.查询超时订单

其中，查询超时订单业务的查询条件包括两点：

- 未付款
- 超过了1个小时

在TbOrderService中定义方法

```java
List<Long> getOverTimeIds(Date overDate);
```



在TbOrderServiceImpl中定义方法

```java
@Override
    public List<Long> getOverTimeIds(Date overDate) {
        return this.baseMapper.selectOverTimeIds(overDate);
    }
```



需要自己定义，我们在`TbOrderMapper`中定义接口：

```java
@Select("select order_id from tb_order where status = 1 AND create_time <= #{overDate}")
List<Long> selectOverTimeIds(Date overDate);
```

这里查询的条件是status和create_time，这两个字段在数据库中我们添加了联合索引：

![1556177384689](assets/1556177384689.png)

因此查询时依然会走索引，可以通过mysql的执行计划证实这一点。



另外，这里只查询了order_id，因为得到id后，我们再根据id更新数据库表就可以了，无需其它信息。



### 8.3.2.订单中的商品及数量

要恢复库存，必须获取订单对应的商品及数量，而这些数据都在订单详情表：`tb_order_detail`中。我们根据orderId查询出OrderDetail的集合后，还需要整理出其中的商品及数量信息。

我们需要知道订单详情中包含的商品的**`skuId`**和数量**`num`**，因此可以把数据整理成一个Map，map的key是skuId，value就是num的值。

这里我们通过JDK8中的Stream流的分组功能和求和功能来实现：

![1556180487105](assets/1556180487105.png)



### 8.3.3.清理订单

service:

```java
/**
     * 关闭过期订单
     * @param overDate
     */
    @Transactional
    public void closeOverTimeOrder(Date overDate) {
//        获取已经超时并且需要被关闭的订单号
        List<Long> orderIds = tbOrderService.getOverTimeOrderIds(overTime);
        if(CollectionUtils.isEmpty(orderIds)){
            return ;
        }
//        修改订单的状态
//        update tb_order set status = 5 where status=1 and id in (orderIds)
        UpdateWrapper<TbOrder> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().
                in(TbOrder::getOrderId,orderIds).
                set(TbOrder::getStatus,OrderStatusEnum.CLOSED.value());
        boolean b = tbOrderService.update(updateWrapper);
        if(!b){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
//        用订单号 查询 需要返回库存的商品数量
//        select * from tb_order_detail where order_id in (orderIds)
        QueryWrapper<TbOrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(TbOrderDetail::getOrderId,orderIds);
        List<TbOrderDetail> tbOrderDetailList = tbOrderDetailService.list(queryWrapper);
        if(CollectionUtils.isEmpty(tbOrderDetailList)){
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
//        构造skuid 和 数量的对应关系map  key - skuid value- num
        Map<Long, Integer> skuNumMap = tbOrderDetailList.stream().collect(Collectors.groupingBy(TbOrderDetail::getSkuId,
                Collectors.summingInt(TbOrderDetail::getNum)));
//        远程调用把skuid ，num 传到item服务中增加库存
        try{
        	itemClient.plusStock(skuNumMap);
        }catch (Exception e){
            e.printStackTrace();
            throw new LyException(ExceptionEnum.PLUS_STOCK_ERROR);
        }


    }
```

## 8.4 创建超时订单清理的消息监听

在pom.xml中增加依赖

```xml
<dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
            <version>2.0.2</version>
        </dependency>
```



在application.yml中添加rocketmq的配置

```yaml
rocketmq:
  name-server: 127.0.0.1:9876	
```



超时订单的清理，我们接收消息来完成

### 消息监听器

```java
package com.leyou.order.listener;

import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.leyou.common.constants.RocketMQConstants.CONSUMER.ORDER_OVERTIME_CONSUMER;
import static com.leyou.common.constants.RocketMQConstants.TAGS.ORDER_OVERTIME_TAGS;
import static com.leyou.common.constants.RocketMQConstants.TOPIC.ORDER_TOPIC_NAME;

/**
 * 订单业务 清理超时订单 监听
 *
 * @author
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = ORDER_TOPIC_NAME,
        selectorExpression = ORDER_OVERTIME_TAGS,
        consumerGroup = ORDER_OVERTIME_CONSUMER)
public class OverTimeOrderListener implements RocketMQListener<Date> {

    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(Date overDate) {
        log.info("接收到 overtime 消息 : 内容：{}", overDate);
        orderService.closeOverTimeOrder(overDate);
    }
}

```



# 9.创建定时任务服务

## 创建ly-task父工程

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>leyou</artifactId>
        <groupId>com.leyou</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ly-task</artifactId>
    <packaging>pom</packaging>


</project>
```



## 创建ly-task-service子工程

### pom.xml:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ly-task</artifactId>
        <groupId>com.leyou</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ly-task-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
            <version>2.0.2</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson</artifactId>
            <version>3.10.6</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 启动类

```java
package com.leyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class LyTaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyTaskApplication.class,args);
    }
}

```

### 配置文件

```yml
server:
  port: 8093
spring:
  application:
    name: task-service
  redis:
    host: 127.0.0.1
  task:
    scheduling:
      pool:
        size: 10
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
    registry-fetch-interval-seconds: 5
  instance:
    prefer-ip-address: true
    ip-address: 127.0.0.1
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    send-message-timeout: 30000
    group: ${spring.application.name}
```



### 配置Redisson

在ly-order中添加配置类：

```java
package com.leyou.task.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties prop) {
        String address = "redis://%s:%d";
        Config config = new Config();
        config.useSingleServer()
                .setAddress(String.format(address, prop.getHost(), prop.getPort()));
        return Redisson.create(config);
    }
}
```

### 编写定时任务

这里约定好任务的一些参数：

- 任务执行频率：30分钟一次
- 订单超时未付款的期限：1小时
- 任务锁的自动释放时间：2分钟
- 任务锁等待时长：0

```java
package com.leyou.task.schedule;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.leyou.common.constants.RocketMQConstants.TAGS.ORDER_OVERTIME_TAGS;
import static com.leyou.common.constants.RocketMQConstants.TOPIC.ORDER_TOPIC_NAME;

@Slf4j
@Component
public class OrderOverTimeTask {
    private static final Long FIX_DELAY_TIME = 60000L;
    private static final String LOCK_KEY = "ly:order:close:task:lock";
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    private Long TASK_LEASE_TIME = 120L;
    private int OVER_ORDER_SECONDS = 3600;

    /**
     * 定时关闭业务
     */
    @Scheduled(fixedDelay = 60000L)
    public void closeOrder() {
        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            boolean b = lock.tryLock(0, TASK_LEASE_TIME, TimeUnit.SECONDS);
            if (!b) {
                // 获取锁失败，结束任务
                log.info("【清理订单任务】未能获取任务锁，结束任务。");
                return;
            }
            try {
                log.info("【清理订单任务】任务执行开始。");
                //计算本次查询的 过期时间点
                Date overDate = DateTime.now().minusSeconds(OVER_ORDER_SECONDS).toDate();
                // 2.2清理订单

//                orderClient.closeOverTimeOrder(overDate);
                rocketMQTemplate.convertAndSend(ORDER_TOPIC_NAME + ":" + ORDER_OVERTIME_TAGS, overDate);
            } finally {
                // 任务结束，释放锁
                lock.unlock();
                log.info("【清理订单任务】任务执行完毕，释放锁。");
            }
        } catch (InterruptedException e) {
            log.error("【清理订单任务】获取任务锁异常，原因：{}", e.getMessage(), e);
        }
    }
}

```

