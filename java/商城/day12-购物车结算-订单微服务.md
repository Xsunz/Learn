# 学习目标

- 了解订单结算页面
- 创建订单微服务
- 理解雪花算法生成唯一ID
- 使用RequestInterceptor
- 了解MySQL的无符号类型
- 完成查询订单接口

# 1.订单结算页（了解）

在购物车页面，用户会点击`去结算`按钮:

![1527990452791](assets/1527990452791.png)

随后就会进入订单结算页getOrderInfo.html，展示用户正在购买的商品，并且需要用户选择收货人地址、付款方式等信息：

![1527998515652](assets/1527998515652.png)

这个页面需要完成的功能如下：

- 收件人信息展示、选择
- 支付方式选择
- 商品清单展示



## 1.1.收货人信息

![1528011713709](assets/1528011713709.png)

这里的收货人信息肯定是当前登录用户的收货地址。所以需要根据当前登录用户去查询，目前我们在页面是写的假数据：

 ![1528011802610](assets/1528011802610.png)

大家可以在在后台提供地址的增删改查接口，然后页面加载时根据当前登录用户查询，而后赋值给addresses即可。

## 1.2.支付方式

支付方式有2种：

- 微信支付
- 货到付款

与我们订单数据中的`paymentType`关联：

![1528012065388](assets/1528012065388.png)

所以我们可以在Vue实例中定义一个属性来记录支付方式：

 ![1535897554691](assets/1535897554691.png)

然后在页面渲染时与这个变量关联：

![1535897599041](assets/1535897599041.png)

效果：

 ![11](assets/1.gif)

## 1.3.商品清单

商品清单是通过localstorage从购物车页面传递过来的，到了本页从localstorage取出并且记录在data中：

![1535897715255](assets/1535897715255.png)

随后在页面渲染完成：



## 1.4.提交订单

当点击`提交订单`按钮，会看到控制台发起请求：

![1555471364438](assets/1555471364438.png)

参数说明：

- addressId：收货人地址信息的id，需要去用户中心查询收货人地址
- carts：购物车中的商品数据，可以有多个对象
  - num：购物车中指定商品的购买数量
  - skuId：购物车中的某商品的id
- paymentType：付款方式：1 在线支付，2 货到付款

对应的JS代码：

![1555471464247](assets/1555471464247.png)

可以看到返回的提交订单成功，返回的应该是订单的编号id。

# 2.创建订单微服务

加入购物车后，自然就要完成下单，我们接下来创建订单微服务：

## 2.1.搭建服务

### 创建ly-order父工程

maven工程：

![1555383905869](assets/1555383905869.png)

选择位置：

![1555383926370](assets/1555383926370.png)

pom.xml

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

    <artifactId>ly-order</artifactId>
    <packaging>pom</packaging>


</project>
```



### 创建ly-order-service子工程

### 依赖



```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ly-order</artifactId>
        <groupId>com.leyou</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ly-order-service</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- MyBatisPlus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        <!-- mysql驱动 -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-item-interface</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-order-pojo</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-user-interface</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.wxpay</groupId>
            <artifactId>wxpay-sdk</artifactId>
            <version>3.0.9</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-xml</artifactId>
            <version>2.9.6</version>
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



### 配置文件

application.yaml:

```yaml
server:
  port: 8090
spring:
  application:
    name: order-service
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    username: root
    password: 123456
    url: jdbc:mysql:///leyou?characterEncoding=UTF-8
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
    registry-fetch-interval-seconds: 10
  instance:
    prefer-ip-address: true
    ip-address: 127.0.0.1
mybatis-plus:
  configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath*:mapper/**Mapper.xml
ly:
  jwt:
    user:
      cookieName: LY_TOKEN # cookie名称
      cookieDomain: leyou.com # cookie的域
rocketmq:
  name-server: 127.0.0.1:9876
```

### 启动类

```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.leyou.order.mapper")
public class LyOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyOrderApplication.class, args);
    }
}
```



### 路由

在ly-gateway中添加路由：

```yaml
zuul:
  routes:
    order-service:
      path: /order/**
      serviceId: order-service
      strip-prefix: false
```

这里选择了`strip-prefix`为false，因此路径中的`/order`会作为真实请求路径的一部分

### 创建ly-order-pojo工程

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ly-order</artifactId>
        <groupId>com.leyou</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>ly-order-pojo</artifactId>
</project>
```



## 2.2.用户登录信息获取

订单业务也需要知道当前登录的用户信息，如同购物车一样，我们添加一个SpringMVC的拦截器，用于获取用户信息：

### 2.2.1.拦截器

```java
package com.leyou.order.interceptor;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.threadlocals.UserHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class UserInterceptor implements HandlerInterceptor {

    private static final String COOKIE_NAME = "LY_TOKEN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 获取cookie中的token
            String token = CookieUtils.getCookieValue(request, COOKIE_NAME);
            // 解析token中的用户
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, UserInfo.class);
            // 保存用户
            UserHolder.setUser(payload.getUserInfo().getId());
            return true;
        } catch (Exception e) {
            // 解析失败，不继续向下
            log.error("【购物车服务】解析用户信息失败！", e);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.remove();
    }
}
```



### 2.2.2.配置拦截器

```java
package com.leyou.order.config;

import com.leyou.order.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInterceptor()).addPathPatterns("/order/**");
    }
}
```



## 2.3.数据结构

这里我们增加一个 b_type 字段，用来标识当前的订单是普通订单还是秒杀订单

订单表：

```sql
CREATE TABLE `tb_order` (
  `order_id` bigint(20) NOT NULL COMMENT '订单id',
  `total_fee` bigint(20) NOT NULL COMMENT '总金额，单位为分',
  `actual_fee` bigint(20) NOT NULL COMMENT '实付金额。单位:分。如:20007，表示:200元7分',
  `promotion_ids` varchar(256) COLLATE utf8_bin DEFAULT '' COMMENT '优惠活动id，多个以，隔开',
  `payment_type` tinyint(1) unsigned zerofill NOT NULL COMMENT '支付类型，1、微信支付，2、货到付款',
  `b_type` tinyint(1) NOT NULL COMMENT '订单业务类型1- 商城订单 2、秒杀订单',
  `post_fee` bigint(20) NOT NULL COMMENT '邮费。单位:分。如:20007，表示:200元7分',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `invoice_type` int(1) DEFAULT '0' COMMENT '发票类型(0无发票1普通发票，2电子发票，3增值税发票)',
  `source_type` int(1) DEFAULT '2' COMMENT '订单来源：1:app端，2：pc端，3：微信端',
  `status` tinyint(1) DEFAULT NULL COMMENT '订单的状态，1、未付款 2、已付款,未发货 3、已发货,未确认 4、确认收货，交易成功 5、交易取消，订单关闭 6、交易结束，已评价',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` timestamp NULL DEFAULT NULL COMMENT '支付时间',
  `consign_time` timestamp NULL DEFAULT NULL COMMENT '发货时间',
  `end_time` timestamp NULL DEFAULT NULL COMMENT '交易完成时间',
  `close_time` timestamp NULL DEFAULT NULL COMMENT '交易关闭时间',
  `comment_time` timestamp NULL DEFAULT NULL COMMENT '评价时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `buyer_nick` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
```



物流信息表：

```mysql
CREATE TABLE `tb_order_logistics` (
  `order_id` bigint(20) NOT NULL COMMENT '订单id，与订单表一对一',
  `logistics_number` varchar(18) DEFAULT '' COMMENT '物流单号',
  `logistics_company` varchar(18) DEFAULT '' COMMENT '物流公司名称',
  `addressee` varchar(32) NOT NULL COMMENT '收件人',
  `phone` varchar(11) NOT NULL COMMENT '收件人手机号码',
  `province` varchar(16) NOT NULL COMMENT '省',
  `city` varchar(32) NOT NULL COMMENT '市',
  `district` varchar(32) NOT NULL COMMENT '区',
  `street` varchar(256) NOT NULL COMMENT '街道',
  `postcode` int(6) DEFAULT '0' COMMENT '邮编',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```



订单详情：

```sql
CREATE TABLE `tb_order_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '订单详情id ',
  `order_id` bigint(20) NOT NULL COMMENT '订单id',
  `sku_id` bigint(20) NOT NULL COMMENT 'sku商品id',
  `num` int(4) NOT NULL COMMENT '购买数量',
  `title` varchar(256) NOT NULL COMMENT '商品标题',
  `own_spec` varchar(1024) DEFAULT '' COMMENT '商品动态属性键值集',
  `price` int(16) NOT NULL COMMENT '价格,单位：分',
  `image` varchar(256) DEFAULT '' COMMENT '商品图片',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `key_order_id` (`order_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COMMENT='订单详情表';
```

## 2.4 实体类

### 创建TbOrder.java 订单实体类

```java
package com.leyou.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author HM
 * @since 2019-11-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbOrder extends Model<TbOrder> {

private static final long serialVersionUID=1L;

    /**
     * 订单id
     */
    @TableId(value = "order_id",type= IdType.INPUT)
    private Long orderId;

    /**
     * 总金额，单位为分
     */
    private Long totalFee;

    /**
     * 实付金额。单位:分。如:20007，表示:200元7分
     */
    private Long actualFee;

    /**
     * 优惠活动id，多个以，隔开
     */
    private String promotionIds;

    /**
     * 支付类型，1、在线支付，2、货到付款
     */
    private Integer paymentType;

    /**
     * 业务类型 1- 商城订单 2、秒杀订单
     */
    private Integer bType;

    /**
     * 邮费。单位:分。如:20007，表示:200元7分
     */
    private Long postFee;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 发票类型(0无发票1普通发票，2电子发票，3增值税发票)
     */
    private Integer invoiceType;

    /**
     * 订单来源：1:app端，2：pc端，3：微信端
     */
    private Integer sourceType;

    /**
     * 订单的状态，1、未付款 2、已付款,未发货 3、已发货,未确认 4、确认收货，交易成功 5、交易取消，订单关闭 6、交易结束，已评价
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 发货时间
     */
    private Date consignTime;

    /**
     * 交易完成时间
     */
    private Date endTime;

    /**
     * 交易关闭时间
     */
    private Date closeTime;

    /**
     * 评价时间
     */
    private Date commentTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.orderId;
    }

}
```

### 创建TbOrderDetail

```java
package com.leyou.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 订单详情表
 * </p>
 *
 * @author HM
 * @since 2019-11-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbOrderDetail extends Model<TbOrderDetail> {

private static final long serialVersionUID=1L;

    /**
     * 订单详情id 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单id
     */
    private Long orderId;

    /**
     * sku商品id
     */
    private Long skuId;

    /**
     * 购买数量
     */
    private Integer num;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品动态属性键值集
     */
    private String ownSpec;

    /**
     * 价格,单位：分
     */
    private Long price;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}
```

### 创建TbOrderLogistics.java

```java
package com.leyou.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author HM
 * @since 2019-11-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbOrderLogistics extends Model<TbOrderLogistics> {

private static final long serialVersionUID=1L;

    /**
     * 订单id，与订单表一对一
     */
    @TableId(value = "order_id",type= IdType.INPUT)
    private Long orderId;

    /**
     * 物流单号
     */
    private String logisticsNumber;

    /**
     * 物流公司名称
     */
    private String logisticsCompany;

    /**
     * 收件人
     */
    private String addressee;

    /**
     * 收件人手机号码
     */
    private String phone;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 街道
     */
    private String street;

    /**
     * 邮编
     */
    private Integer postcode;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.orderId;
    }

}
```



## 2.5.枚举类

### 订单状态枚举

```java
package com.leyou.order.enums;
public enum  OrderStatusEnum {
    INIT(1, "初始化，未付款"),
    PAY_UP(2, "已付款，未发货"),
    DELIVERED(3, "已发货，未确认"),
    CONFIRMED(4, "已确认,未评价"),
    CLOSED(5, "已关闭"),
    RATED(6, "已评价，交易结束")
    ;
    private Integer value;
    private String msg;
    OrderStatusEnum(Integer value, String msg) {
        this.value = value;
        this.msg = msg;
    }
    public Integer value(){
        return this.value;
    }
    public String msg(){
        return msg;
    }
}
```

###  业务类型枚举类

```java
package com.leyou.order.enums;

public enum BusinessTypeEnum {
    MALL(1,"商城"),
    SEC_KILL(2,"秒杀")

    ;

    /**
     * 业务类型
     */
    private Integer type;
    /**
     * 描述
     */
    private String desc;

    BusinessTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer value(){
        return this.type;
    }
    public String desc(){
        return this.desc;
    }
}
```



# 3.创建订单API

订单信息共有3张表，内容很多，但是前台提交的数据却只很少，也就是说我们需要自己填充很多的数据。

在**ly-order-pojo**中创建OrderDTO.java对象

```java
package com.leyou.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private Long addressId;
    private List<CartDTO> carts;
    private Integer paymentType;

}

```

其中的购物车数据再次封装对象,创建CartDTO.java

```java
package com.leyou.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartDTO {
    private Long skuId;
    private Integer num;
}

```



## 3.1.Controller

请求分析：

- 请求方式：POST

- 请求路径：/order

- 请求参数：包含收货人地址id、付款方式、购物车商品数据集合的json内容，我们使用OrderDTO.java

- 返回结果：订单id

具体代码：

```java
@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody @Valid OrderDTO orderDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(this.orderService.createOrder(orderDTO));
    }
}
```



## 3.2.Service

创建订单逻辑比较复杂，需要组装订单数据，基本步骤如下：

- 组织Order数据，完成新增，包括：
  - 订单编号
  - 用户id
  - 订单金额相关数据，需要查询商品信息后逐个运算并累加获取
  - 订单状态数据
- 组织OrderDetail数据，完成新增
  - 需要查询商品信息后，封装为OrderDetail集合，然后新增
- 组织OrderLogistics数据，完成新增
  - 需要查询到收货人地址
  - 然后根据收货人地址，封装OrderLogistics后，完成新增
- 下单成功后，商品对应库存应该减掉



我们来看其中的几个关键点：

### 3.2.1.生成订单编号

> 订单id的特殊性

订单数据非常庞大，将来一定会做分库分表。那么这种情况下， 要保证id的唯一，就不能靠数据库自增，而是自己来实现算法，生成唯一id。

> 雪花算法

这里的订单id是通过一个工具类生成的：

 ![1528728840023](assets/1528728840023.png)

而工具类所采用的生成id算法，是由Twitter公司开源的snowflake（雪花）算法。

> 简单原理

雪花算法会生成一个64位的二进制数据，为一个Long型。(转换成字符串后长度最多19) ，其基本结构：

 ![1528729105237](assets/1528729105237.png)

第一位：为未使用

第二部分：41位为毫秒级时间(41位的长度可以使用69年)

第三部分：5位datacenterId和5位workerId(10位的长度最多支持部署1024个节点）

第四部分：最后12位是毫秒内的计数（12位的计数顺序号支持每个节点每毫秒产生4096个ID序号）

snowflake生成的ID整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞（由datacenter和workerId作区分），并且效率较高。经测试snowflake每秒能够产生26万个ID。

> 配置

为了保证不重复，我们在application.yaml中给每个部署的节点都配置机器id：

```yaml
ly:
  worker:
    workerId: 1
    dataCenterId: 1
```

加载属性：

```java
@Data
@ConfigurationProperties(prefix = "ly.worker")
public class IdWorkerProperties {

    private long workerId;// 当前机器id

    private long dataCenterId;// 序列号
}
```

编写配置类：创建雪花算法的工具类

```java
@Configuration
@EnableConfigurationProperties(IdWorkerProperties.class)
public class IdWorkerConfig {

    @Bean
    public IdWorker idWorker(IdWorkerProperties prop) {
        return new IdWorker(prop.getWorkerId(), prop.getDataCenterId());
    }
}
```



### 3.2.2.查询sku的接口

创建订单过程中，肯定需要查询sku信息，因此我们需要在商品微服务提供根据skuId查询sku的接口：

在`ly-item-interface`的`ItemClient`中添加接口：

```java
/**
  * 根据id批量查询sku
  * @param ids skuId的集合
  * @return sku的集合
  */
@GetMapping("sku/list")
List<SkuDTO> querySkuByIds(@RequestParam("ids") List<Long> ids);
```

对应的业务实现之前已经写过了，可以不用写了。

### 3.2.3.准备物流数据

我们前端页面传来的是addressId，我们需要根据id查询物流信息，但是因为还没做物流地址管理。所以我们准备一些假数据。

首先是实体类：

在ly-user中添加用户地址的实体类

sql语句如下

```sql
CREATE TABLE `tb_user_address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '地址id',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `addressee` varchar(64) NOT NULL COMMENT '收货人名称',
  `phone` varchar(20) DEFAULT '' COMMENT '收货人电话',
  `province` varchar(20) DEFAULT '' COMMENT '收货人省份',
  `city` varchar(20) DEFAULT '' COMMENT '收货人市',
  `district` varchar(20) DEFAULT '' COMMENT '收货人区',
  `street` varchar(100) DEFAULT '' COMMENT '收货人街道',
  `address` varchar(100) DEFAULT '' COMMENT '收货人详细地址',
  `postcode` varchar(10) DEFAULT '' COMMENT '收货人邮编',
  `is_default` tinyint(2) DEFAULT '0' COMMENT '是否默认 0-不是  1-是',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='用户收货地址表'


insert  into `tb_user_address`(`id`,`user_id`,`addressee`,`phone`,`province`,`city`,`district`,`street`,`address`,`postcode`,`is_default`,`create_time`,`update_time`) values 
(1,29,'张三','1389393939','北京市','北京','顺义区','马坡','传智播客教学楼101','11111',1,'2019-10-17 23:26:36','2019-10-17 23:26:36'),
(2,29,'小李四','1397837838','上海市','上海','浦东新区','航头镇','航头镇航头路18号传智播客3号楼','222222',0,'2019-10-17 23:27:34','2019-10-17 23:27:34');

```

创建实体类如下：

```java
package com.leyou.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 用户收货地址表
 * </p>
 *
 * @author HM
 * @since 2019-10-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbUserAddress extends Model<TbUserAddress> {

private static final long serialVersionUID=1L;

    /**
     * 地址id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 收货人名称
     */
    private String addressee;

    /**
     * 收货人电话
     */
    private String phone;

    /**
     * 收货人省份
     */
    private String province;

    /**
     * 收货人市
     */
    private String city;

    /**
     * 收货人区
     */
    private String district;

    /**
     * 收货人街道
     */
    private String street;

    /**
     * 收货人详细地址
     */
    private String address;

    /**
     * 收货人邮编
     */
    private String postcode;

    /**
     * 是否默认 0-不是  1-是
     */
    private Boolean isDefault;

    private Date createTime;

    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}

```

我们在ly-user-pojo中

#### 创建用户地址DTO类：

```java
package com.leyou.user.dto;

import lombok.Data;

@Data
public class UserAddressDTO {
    private Long id;
    private Long userId;
    private String addressee;// 收件人姓名
    private String phone;// 电话
    private String province;// 省份
    private String city;// 城市
    private String district;// 区
    private String street;// 街道地址
    private String  postcode;// 邮编
    private Boolean isDefault;
}
```

#### 然后在ly-user-interface的UserClient中添加方法：

```java
   /**
     * 根据
     * @param id 地址id
     * @return 地址信息
     */
    @GetMapping("/address/byId")
    UserAddressDTO queryAddressById(@RequestParam("id") Long id);
```

然后在ly-user-service中编写controller：

```java
package com.leyou.user.controller;

import com.leyou.common.threadlocals.UserHolder;
import com.leyou.common.utils.BeanHelper;
import com.leyou.user.DTO.AddressDTO;
import com.leyou.user.DTO.UserAddressDTO;
import com.leyou.user.entity.TbUserAddress;
import com.leyou.user.service.TbUserAddressService;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private TbUserAddressService userAddressService;

    @GetMapping("/list")
    public ResponseEntity<List<UserAddressDTO>> findAddressList(){
        return ResponseEntity.ok( userAddressService.findAddressList());
    }
    /**
     * 根据 地址id 获取地址信息
     * @param id 地址id
     * @return 地址信息
     */
    @GetMapping("/byId")
    public ResponseEntity<UserAddressDTO> queryAddressById(@RequestParam("id") Long id){
        UserAddressDTO userAddressDTO = userAddressService.findById(id);
        return ResponseEntity.ok(userAddressDTO);
    }
}
```

#### 在TbUserAddressService增加方法

```java
package com.leyou.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leyou.user.DTO.UserAddressDTO;
import com.leyou.user.entity.TbUserAddress;

import java.util.List;

/**
 * <p>
 * 用户收货地址表 服务类
 * </p>
 *
 * @author HM
 * @since 2019-10-17
 */
public interface TbUserAddressService extends IService<TbUserAddress> {

    List<UserAddressDTO> findAddressList();

    UserAddressDTO findById(Long id);
}

```

#### 在TbUserAddressServiceImpl中添加方法

```java
package com.leyou.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leyou.common.threadlocals.UserHolder;
import com.leyou.common.utils.BeanHelper;
import com.leyou.user.DTO.UserAddressDTO;
import com.leyou.user.entity.TbUserAddress;
import com.leyou.user.mapper.TbUserAddressMapper;
import com.leyou.user.service.TbUserAddressService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 用户收货地址表 服务实现类
 * </p>
 *
 * @author HM
 * @since 2019-10-17
 */
@Service
public class TbUserAddressServiceImpl extends ServiceImpl<TbUserAddressMapper, TbUserAddress> implements TbUserAddressService {

    @Override
    public List<UserAddressDTO> findAddressList() {
        Long userId = UserHolder.getUserId();
        QueryWrapper<TbUserAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TbUserAddress::getUserId,userId);
        List<TbUserAddress> tbUserAddressList = this.list(queryWrapper);
        return BeanHelper.copyWithCollection(tbUserAddressList,UserAddressDTO.class);
    }

    @Override
    public UserAddressDTO findById(Long id) {
        Long userId = UserHolder.getUserId();
        QueryWrapper<TbUserAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TbUserAddress::getUserId,userId);
        queryWrapper.lambda().eq(TbUserAddress::getId,id);
        TbUserAddress tbUserAddress = this.getOne(queryWrapper);
        return BeanHelper.copyProperties(tbUserAddress,UserAddressDTO.class);
    }
}

```

创建TbUserAddressMapper.java

```java
package com.leyou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leyou.user.entity.TbUserAddress;

/**
 * <p>
 * 用户收货地址表 Mapper 接口
 * </p>
 *
 * @author HM
 * @since 2019-10-17
 */
public interface TbUserAddressMapper extends BaseMapper<TbUserAddress> {

}

```

创建TbUserAddressMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.leyou.user.mapper.TbUserAddressMapper">

    <!-- 通用查询映射结果 -->
    <resultMap id="BaseResultMap" type="com.leyou.user.entity.TbUserAddress">
        <id column="id" property="id" />
        <result column="name" property="name" />
        <result column="phone" property="phone" />
        <result column="province" property="province" />
        <result column="city" property="city" />
        <result column="district" property="district" />
        <result column="street" property="street" />
        <result column="address" property="address" />
        <result column="postcode" property="postcode" />
        <result column="is_default" property="isDefault" />
        <result column="create_time" property="createTime" />
        <result column="update_time" property="updateTime" />
    </resultMap>

</mapper>

```



#### 增加拦截器配置

```java
package com.leyou.user.config;

import com.leyou.user.interceptors.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    /**
     * 注意：这里只拦截 address的请求
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInterceptor()).
                addPathPatterns("/address/list/**").
                addPathPatterns("/address/byId/**");
    }
}

```



在ly-order-service的pom.xml中添加ly-user的依赖：

```xml
<dependency>
    <groupId>com.leyou.service</groupId>
    <artifactId>ly-user-interface</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 3.2.4 使用RequestInterceptor 增加头信息

我们调用userclient获取用户收货信息时，没有传递userid

原因是在userservice服务中，会从当前请求的头中获取cookie中的token，并解析获取用户信息

而当我们发起feign远程调用时，是一次新的请求，在头中并不包含cookie。

所以我们要在 feign发起远程调用时，在feign的请求头中增加cookie。

我们可以使用RequestInterceptor 

```java
package com.leyou.order.interceptors;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * feign的拦截器
 */
@Slf4j
@Component
public class FeignInterceptor implements RequestInterceptor {
    /**
     * 把前端传过来的request中的cookie拿出来
     * 放在请求user的request头中
     * @param requestTemplate
     */
    @Override
    public void apply(RequestTemplate requestTemplate) {
//        获取 新的请求的url
        String path = requestTemplate.path();
//        如果请求不是以/address开头，后面业务也不用做了
        if(!path.startsWith("/address")){
            return;
        }
//        获取请求的上下文
        ServletRequestAttributes servletRequestAttributes=
                (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
//        获取前端传过来的request
        HttpServletRequest request = servletRequestAttributes.getRequest();
//        获取前端传过来的头信息
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            //        从头中找到cookie
            if(headerName.equals("cookie")){
                String value = request.getHeader(headerName);
                log.info("cookie的内容是：{}",value);
                //        把cookie放入requestTemplate
                requestTemplate.header(headerName,value);
            }
        }


    }
}

```



### 3.2.5.减库存接口

创建订单，肯定需要减库存，我们这里看2种方案

- 方案1：提交订单时减库存。用户选择提交订单，说明用户有强烈的购买欲望。生成订单会有一个支付时效，例如半个小时。超过半个小时后，系统自动取消订单，还库存。
- 方案2：支付时去减库存。支付完成需要处理的业务较多，这个时候如果减库存失败，那么数据回滚很难做。



我们还要在商品微服务提供减库存接口

在`ly-item-interface`的`GoodsApi`中添加接口：

```java
/**
     * 减库存
     * @param cartMap 商品id及数量的map
     */
@PutMapping("/stock/minus")
void minusStock(@RequestBody Map<Long, Integer> cartMap);
```

在`ly-item-service`的`GoodsController`中编写业务：

```java
/**
     * 减库存
     * @param cartMap 商品id及数量的map
     */
@PutMapping("/stock/minus")
public ResponseEntity<Void> minusStock(@RequestBody Map<Long, Integer> cartMap){
    goodsService.minusStock(cartMap);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```

在`ly-item-service`的`GoodsServicer`中编写业务

```java
@Transactional    
public void minusStock(Map<Long, Integer> cartMap) {
    for (Long skuId : cartMap.keySet()) {
            Integer num = skuMap.get(skuId);
            int count = this.baseMapper.minusStock(skuId,num);
            if(count != 1){
                throw new LyException(ExceptionEnum.STOCK_NOT_ENOUGH_ERROR);
            }
        }
}
```

此处采用了手写Sql在SkuMapper中：

```java
@Update("UPDATE tb_sku SET stock = stock - #{num} WHERE id = #{id}")
int minusStock(@Param("id") Long id, @Param("num") Integer num);

```

这里减库存并没有采用先查询库存，判断充足才减库存的方案，那样会有线程安全问题，当然可以通过加锁解决。不过我们此处为了效率，并没有使用。

而是把数据库中的库存stock列设置为：无符号类型，当库存减到0以下时，数据库会报错，从而避免超卖：

![1555474518503](assets/1555474518503.png)



### 🎗经验分享

#### 1.如下是用户下单时，商品微服务扣减商品库存的代码

**ItemClient.java接口代码**

```java
/**
     * 商品下单后，扣减商品库存 
     * @param skuMap
     */
@GetMapping("sku/minusStock")
void minusStock(@RequestBody Map<Long, Integer> skuMap);
```

**GoodsController.java相关代码**

```java
/**
     * 商品下单后，扣减商品库存
     * @param skuMap
     */
@GetMapping("sku/minusStock")
public ResponseEntity<Void> minusStock(@RequestBody Map<Long, Integer> skuMap){
    skuService.minusStock(skuMap);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```



#### 2.出现的问题

> 页面提交订单后，提示下单失败，订单微服务idea控制台报错信息如下：

![59444627008](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594446270089.png)



#### 3.问题的分析

> 从上面的问题截图上看到，订单微服务通过feign远程调用商品微服务时调用失败。并包405状态码。405状态码是因为请求方式错误。商品微服务提供的接口代码ItemClient.java可以看到，扣减商品库存使用的是get请求接收的。

![59444656320](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594446563209.png)

而扣减商品库存是对商品数量修改，基于restful风格的请求规范，应该是put请求。

#### 4.问题解决办法

> 将ItemClient.java和GoodsController.java中关于扣减商品库存的代码请求方式都改为put请求即可。修改后的代码如下：

##### **ItemClient.java接口代码**

```java
/**
     * 商品下单后，扣减商品库存
     * @param skuMap
     */
@PutMapping("sku/minusStock")
void minusStock(@RequestBody Map<Long, Integer> skuMap);
```

##### **GoodsController.java相关代码**

```java
 /**
     * 商品下单后，扣减商品库存
     * @param skuMap
     */
@PutMapping("sku/minusStock")
public ResponseEntity<Void> minusStock(@RequestBody Map<Long, Integer> skuMap){
    skuService.minusStock(skuMap);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```



### 3.2.6.创建订单代码

完整代码如下：

```java
    /**
     * 创建订单
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderDTO orderDTO) {

//        1、保存order
//        获取userid
        Long userId = UserHolder.getUser();
//        生成订单号,使用雪花算法
        long orderId = idWorker.nextId();
//        获取前端传过来的skuid的集合
        List<Long> skuIds = orderDTO.getCarts().stream().map(CartDTO::getSkuId).collect(Collectors.toList());
//        构造skuid 和num的对应 map结构  key-skuid  val - num
        Map<Long, Integer> skuNumMap = orderDTO.getCarts().stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
//        通过skuid的集合获取sku的集合
        List<SkuDTO> skuDTOList = itemClient.findSkuListByIds(skuIds);
//        计算总金额
        long totalFee = 0;
        List<TbOrderDetail> detailList = new ArrayList<>();
        for (SkuDTO skuDTO : skuDTOList) {
            Long skuId = skuDTO.getId();
            Long price = skuDTO.getPrice();
//            for (CartDTO cart : orderDTO.getCarts()) {
//                if(cart.getSkuId().longValue() == skuDTO.getId().longValue()){
//                    Integer num = cart.getNum();
//                    totalFee = price * num;
//                    break;
//                }
//            }
            Integer num = skuNumMap.get(skuId);
            totalFee += price * num;

            TbOrderDetail orderDetail = new TbOrderDetail();
            orderDetail.setSkuId(skuId);
            orderDetail.setOrderId(orderId);
            orderDetail.setNum(num);
            orderDetail.setTitle(skuDTO.getTitle());
            orderDetail.setImage(StringUtils.substringBefore(skuDTO.getImages(),","));
            orderDetail.setOwnSpec(skuDTO.getOwnSpec());
            orderDetail.setPrice(price);
            detailList.add(orderDetail);

        }
//        计算运费 包邮
        long postFee = 0;
//        计算优惠金额
        long promotion = 0;
//        计算实付金额  总金额 + 运费 - 优惠
        long actualFee = totalFee + postFee - promotion;
//        设置订单的状态
        Integer status = OrderStatusEnum.INIT.value();
//        构造tborder
        TbOrder tbOrder = new TbOrder();
        tbOrder.setOrderId(orderId);
        tbOrder.setUserId(userId);
        tbOrder.setActualFee(actualFee);
        tbOrder.setTotalFee(totalFee);
        tbOrder.setPostFee(postFee);
        tbOrder.setStatus(status);
        tbOrder.setSourceType(2);
        tbOrder.setPaymentType(orderDTO.getPaymentType());
        tbOrder.setBType(BusinessTypeEnum.MALL.value());
        boolean b = tbOrderService.save(tbOrder);
        if(!b){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
//        2、保存order_Detail
        if(CollectionUtils.isEmpty(detailList)){
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        boolean b1 = tbOrderDetailService.saveBatch(detailList);
        if(!b1){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
//        3、保存物流
//        根据收货人id查询收货人信息，
        Long addressId = orderDTO.getAddressId();
//        远程调用用户服务
        UserAddressDTO userAddressDTO = userClient.queryAddressById(addressId);
        log.info("userAddressDTO={}",userAddressDTO);
        if(userAddressDTO == null){
            throw new LyException(ExceptionEnum.USER_ADDRESS_NOT_FOUND);
        }
        //        拷贝 物流信息对象
        TbOrderLogistics tbOrderLogistics = BeanHelper.copyProperties(userAddressDTO,TbOrderLogistics.class);
//        设置订单id
        tbOrderLogistics.setOrderId(orderId);
        boolean b2 = tbOrderLogisticsService.save(tbOrderLogistics);
        if(!b2){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
//        4、减库存
//        远程调用item,参数 Map<Long,Integer> key-skuid val-num ,{key:value,key:value}
         try {
             itemClient.minusStock(skuNumMap);
        }catch(Exception e){
            e.printStackTrace();
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }

        return orderId;
    }
```



## 3.3.测试

启动项目，在页面再次点击提交订单，发现提交成功，跳转到了支付页面：

 ![1528362464276](assets/1528362464276.png)

查看数据库，发现订单已经生成：

订单

![1535985796739](assets/1535985796739.png)

订单详情：

![1535985836733](assets/1535985836733.png)

订单状态：

 ![1535985877827](assets/1535985877827.png)



# 4.查询订单接口

## 4.1.接口分析

支付页面需要展示订单信息，页面加载时，就会发起请求，查询订单信息：

 ![1556181236344](assets/1556181236344.png)

因此我们应该提供查询订单接口：

- 请求方式：Get
- 请求路径：/order/{id}
- 请求参数：路径占位符的id
- 返回结果：订单对象的json结构，包含订单状态，订单详情，需要定义对应的VO对象



## 4.2.VO对象

OrderDetailVO：

```java
package com.leyou.order.vo;

import lombok.Data;

@Data
public class OrderDetailVO {
    private Long id;
    /**
     * 订单编号
     */
    private Long orderId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 商品购买数量
     */
    private Integer num;
    /**
     * 商品标题
     */
    private String title;
    /**
     * 商品单价
     */
    private Long price;
    /**
     * 商品规格数据
     */
    private String ownSpec;
    /**
     * 图片
     */
    private String image;
}
```

OrderLogisticsVO:

```java
package com.leyou.order.vo;

import lombok.Data;


@Data
public class OrderLogisticsVO {
    private Long orderId;
    /**
     * 物流单号
     */
    private String logisticsNumber;
    /**
     * 物流名称
     */
    private String logisticsCompany;
    /**
     * 收件人
     */
    private String addressee;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 省
     */
    private String province;
    /**
     * 市
     */
    private String city;
    /**
     * 区
     */
    private String district;
    /**
     * 街道
     */
    private String street;
    /**
     * 邮编
     */
    private String postcode;
}
```

OrderVO:

```java
package com.leyou.order.vo;

import lombok.Data;

import javax.persistence.Id;
import java.util.Date;
import java.util.List;


@Data
public class OrderVO {
    /**
     * 订单编号
     */
    @JsonSerialize(using= ToStringSerializer.class)
    private Long orderId;
    /**
     * 业务类型 1- 商城订单 2、秒杀订单
     */
    private Integer bType;
    /**
     * 商品金额
     */
    private Long totalFee;
    /**
     * 邮费
     */
    private Long postFee = 0L;
    /**
     * 实付金额
     */
    private Long actualFee;
    /**
     * 付款方式：1:在线支付, 2:货到付款
     */
    private Integer paymentType;
    /**
     * 优惠促销的活动id，
     */
    private String promotionIds;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 订单状态
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 付款时间
     */
    private Date payTime;
    /**
     * 发货时间
     */
    private Date consignTime;
    /**
     * 确认收货时间
     */
    private Date endTime;
    /**
     * 交易关闭时间
     */
    private Date closeTime;
    /**
     * 评价时间
     */
    private Date commentTime;
    /**
     * 发票类型，0无发票，1普通发票，2电子发票，3增值税发票
     */
    private Integer invoiceType = 0;
    /**
     *  订单来源 1:app端，2：pc端，3：微信端
     */
    private Integer sourceType = 1;
    /**
     * 订单物流信息
     */
    private OrderLogisticsVO logistics;
    /**
     * 订单详情信息
     */
    private List<OrderDetailVO> detailList;
}
```



## 4.3.业务

OrderController：

```java
@GetMapping("{id}")
public ResponseEntity<OrderVO> findOrderById(@PathVariable("id") Long orderId){
    return ResponseEntity.ok(orderService.findOrderById(orderId));
}
```

在TbOrderService中添加方法

```java
 TbOrder findOrder(Long orderId, Long userId, Integer status);
```

在TbOrderServiceImpl添加方法

```java
@Override
    public TbOrder findOrder(Long orderId, Long userId, Integer status) {
        QueryWrapper<TbOrder> queryWrapper = new QueryWrapper();
        if(orderId != null){
            queryWrapper.lambda().eq(TbOrder::getOrderId,orderId);
        }
        queryWrapper.lambda().eq(TbOrder::getUserId,userId).
                eq(TbOrder::getStatus,status).
                orderByDesc(TbOrder::getCreateTime);
        TbOrder tbOrder = this.getOne(queryWrapper);
        if(tbOrder == null){
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        return tbOrder;
    }
```



service,添加方法：

```java
public OrderVO findOrderById(Long id) {
       // 判断用户id是否正确
        Long userId = UserHolder.getUser();
//        订单状态,初始状态
        Integer status = INIT.value();
        // 1.查询订单
        TbOrder tbOrder = tborderService.findOrder(orderId,userId,status);
        if (tbOrder == null) {
            // 不存在
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        if (!userId.equals(tbOrder.getUserId())) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        orderId = tbOrder.getOrderId();
        // 4.封装数据
        OrderVo orderVO = BeanHelper.copyProperties(tbOrder, OrderVo.class);
    

    // 2. 查询订单详情
    QueryWrapper<TbOrderDetail> queryWrapper = new QueryWrapper<>();
    queryWrapper.lambda().eq(TbOrderDetail::getOrderId,orderId);
    List<TbOrderDetail> tbOrderDetailList = orderDetailService.list(queryWrapper);
    if(CollectionUtils.isEmpty(tbOrderDetailList)){
        throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
    }
   

    // 3.查询订单状态
    TbOrderLogistics tbOrderLogistics = orderLogisticsService.getById(orderId);
    if (tbOrderLogistics == null) {
        // 不存在
        throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
    }

    // 4.封装数据
    OrderVO orderVO = BeanHelper.copyProperties(order, OrderVO.class);
    orderVO.setDetailList(BeanHelper.copyWithCollection(tbOrderDetailList, OrderDetailVO.class));
    orderVO.setLogistics(BeanHelper.copyProperties(tbOrderLogistics, OrderLogisticsVO.class));
    return orderVO;
}
```

