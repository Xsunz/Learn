# day11-配置中心-消息总线-购物车

# 学习目标

- 了解SpringBoot配置文件和多环境设置
- 了解SpringConfig的作用，配置SpringConfig
- 会使用配置SpringConfig
- 了解SpringCloudBus的作用，使用SpringCloudBus
- 会使用SpringCloudBus
- 了解购物车功能流程
- 了解未登录购物车功能
- 实现已登录购物车功能
- 理解ThradLocal的使用

# 1.SpringBoot知识了解

## 1.1、bootstrap.yml 和 application.yml

bootstrap.yml（bootstrap.properties）用来程序引导时执行，应用于更加早期配置信息读取，如可以使用来配置application.yml中使用到参数等

application.yml（application.properties) 应用程序特有配置信息，可以用来配置后续各个模块中需使用的公共参数等。

**bootstrap.yml 先于 application.yml 加载**

bootstrap.yml 是被一个父级的 Spring ApplicationContext 加载的。

可以通过设置`spring.cloud.bootstrap.enabled=false`来禁用`bootstrap`。

## 1.2、Spring Boot多环境配置切换

一般在一个项目中，总是会有好多个环境。

比如：开发环境 -> 测试环境 -> 预发布环境 -> 生产环境。

每个环境上的配置文件总是不一样的，配置读取需要解决的问题。

Spring Boot提供了一种优先级配置读取的机制来帮助我们从这种困境中走出来。

常规情况下，我们都知道Spring Boot的配置会从`application.yml或.properties`中读取

根据Spring Boot的文档,配置使用的优先级从高到低的顺序，具体如下所示：

```
1. 命令行参数。 java -jar XXX.jar 
2. 通过 System.getProperties() 获取的 Java 系统参数。
3. 操作系统环境变量。
4. 从 java:comp/env 得到的 JNDI 属性。
5. 通过 RandomValuePropertySource 生成的“random.*”属性。
6. 应用 Jar 文件之外的属性文件(application.properties/yml)。
7. 应用 Jar 文件内部的属性文件(application.properties/yml)。
8. 在应用配置 Java 类（包含“@Configuration”注解的 Java 类）中通过“@PropertySource”注解声明的属性文件。
9. 通过“SpringApplication.setDefaultProperties”声明的默认属性。
```

这意味着，如果Spring Boot在优先级更高的位置找到了配置，那么它就会无视低级的配置。

方法一、不在配置文件写上配置环境定义，而是通过执行时使用不同环境文件来区分。如：

```shell
java -jar demo.jar --spring.config.location=/path/test_evn.properties
```

方法二、在配置文件写上【环境定义】，在执行时传递环境名称来区分。

一般情况下我们这样定义环境：dev :开发，test：测试环境，prod：生产环境

在application-prod.yml 文件中配置的话，写法如下：

```yaml
spring:
  profiles:
    active: prod #生产环境
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/test
    username: root
    password: root
```

启动Jar包的时候：Java -jar xxxxxx.jar spring.profiles.active=prod 也可以这样启动设置配置文件

# 2. 集中配置组件SpringCloudConfig 

## 2.1 Spring Cloud Config简介 

​	在分布式系统中，由于服务数量巨多，为了方便服务配置文件统一管理，实时更新，所以需要分布式配置中心组件。

在Spring Cloud中，有分布式配置中心组件spring cloud config ，它支持配置服务放在配置服务的内存中（即本地），也支持放在远程Git仓库中。

在spring cloud config 组件中，分两个角色，

一是config server，独立的微服务

二是config client，每一个微服务都需要依赖的内容

​	Config Server是一个可横向扩展、集中式的配置服务器，它用于集中管理应用程序各个环境下的配置，默认使用Git存储配置文件内容，也可以使用SVN存储，或者是本地文件存储。

​	Config Client是Config Server的客户端，用于操作存储在Config Server中的配置内容。微服务在启动时会请求Config Server获取配置文件的内容，请求到后再启动容器。

详细内容看在线文档： https://springcloud.cc/spring-cloud-config.html

## 2.2 配置服务端 

### 2.2.1 将配置文件提交到码云 

​	使用GitHub时，国内的用户经常遇到的问题是访问速度太慢，有时候还会出现无法连接的情况。如果我们希望体验Git飞一般的速度，可以使用国内的Git托管服务——[码云](https://gitee.com/)（[gitee.com](https://gitee.com/)）。

​	和GitHub相比，码云也提供免费的Git仓库。此外，还集成了代码质量检测、项目演示等功能。对于团队协作开发，码云还提供了项目管理、代码托管、文档管理的服务。

步骤：

（1）浏览器打开gitee.com，注册用户 ，注册后登陆码云管理控制台

![](assets/8_21.png)



（2）创建仓库  leyou-config  (点击右上角的加号 ，下拉菜单选择创建项目)



（3）上传配置文件，将ly-item-service工程的application.yml改名为item-dev.yml后上传

![](assets/8_25.png)

可以通过拖拽的方式将文件上传上去

![](assets/8_26.png)

上传成功后列表可见

![](assets/8_23.png)

可以再次编辑此文件

![](assets/8_22.png)

文件命名规则：

{application}-{profile}.yml或{application}-{profile}.properties

application为应用名称 profile指的开发环境（用于区分开发环境，测试环境、生产环境等）

（4）复制git地址 ,备用

![](assets/8_24.png)

地址为：https://gitee.com/chuanzhiliubei/tensquare-config.git

### 2.2.2 配置中心微服务 

（1）创建工程模块 配置中心微服务  ly-config   ,pom.xml引入依赖

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-config-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
```

（2）创建启动类ConfigServerApplication

```java
@EnableConfigServer //开启配置服务
@SpringBootApplication
public class LyConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyConfigServerApplication.class, args);
    }
}
```

（3）编写配置文件application.yml

```yaml
server:
  port: 12000
spring:
  application:
    name: config-service
  cloud:
    config:
      server:
        git:
          uri: https://gitee.com/xxx/leyou-config.git #这里要写自己的git地址
```

（4）浏览器测试：http://localhost:12000/item-dev.yml 可以看到配置内容

## 2.3 配置客户端 

（1）在ly-item-service工程添加依赖

```xml
		<dependency>
		  <groupId>org.springframework.cloud</groupId>
		  <artifactId>spring-cloud-starter-config</artifactId>
		</dependency>
```

（2）添加bootstrap.yml ,删除application.yml 

```yaml
spring:
  cloud:
    config:
      name: item
      profile: dev
      label: master
      uri: http://127.0.0.1:12000
```

（3）测试： 启动工程ly-registry、ly-config、ly-item-service，看是否可以正常运行

# 3 消息总线组件SpringCloudBus 

## 3.1 SpringCloudBus简介 

​	如果我们更新码云中的配置文件，那客户端工程是否可以及时接受新的配置信息呢？我们现在来做有一个测试，修改一下码云中的配置文件中mysql的端口 或者连接不同mysql服务器 ，然后测试数据依然可以查询出来，证明修改服务器中的配置并没有更新立刻到工程，只有重新启动程序才会读取配置。 那我们如果想在不重启微服务的情况下更新配置如何来实现呢? 

 我们使用SpringCloudBus来实现配置的自动更新。

SpringCloudBus需要rabbitmq来支持。

## 3.2 代码实现

### 3.2.1 配置服务端 

（1）修改ly-config工程的pom.xml，引用依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

（2）修改application.yml ，添加配置

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    virtual-host: /leyou
    username: leyou
    password: leyou
management: #暴露触发消息总线的地址
  endpoints:
    web:
      exposure:
        include: bus-refresh
```

### 3.2.2 配置客户端 

我们还是以商品微服务模块为例，加入消息总线

（1）修改ly-item-service工程 ，引入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

（2）在码云的配置文件中配置rabbitMQ的地址：

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    virtual-host: /leyou
    username: leyou
    password: leyou
```

（3）启动ly-registry、ly-config、ly-item-service  看是否正常运行

（4）修改码云上的配置文件 ，将数据库连接IP  改为127.0.0.1  ，在本地部署一份数据库。

（5）使用postman测试    Url: http://127.0.0.1:12000/actuator/bus-refresh   Method: post  

（6）再次观察输出的数据是否是读取了本地的mysql数据。

### 3.2.3 自定义配置的读取 

（1）修改码云上的配置文件，增加自定义配置

```
sms:
  ip: 127.0.0.1
```

（2）在ly-item-service工程中新建controller

```java
@RestController
public class TestController {
    @Value("${sms.ip}")
    private String ip;

    @RequestMapping(value = "/ip", method = RequestMethod.GET)
    public String ip() {
        return ip;
    }
}
```

（3）运行测试看是否能够读取配置信息  ，OK.

（4）修改码云上的配置文件中的自定义配置

```
sms:
  ip: 192.168.100.8
```

（5）通过postman测试    Url: http://127.0.0.1:12000/actuator/bus-refresh   Method: post    

测试后观察,发现并没有更新信息。

这是因为我们的 controller少了一个注解@RefreshScope  此注解用于刷新配置

```java
@RefreshScope
@RestController
public class TestController {
    ///......
}
```

添加后再次进行测试。

### 3.2.4 完成乐优商城工程的配置集中管理 

步骤：

（1）将每一个工程的配置文件提取出来，重命名

![](assets/8_31.png)

（2）将这些文件上传到码云

（3）修改每一个微服务工程，pom.xml中添加依赖

```xml
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

（4）删除每一个微服务的application.yml

（5）为每一个微服务添加bootstrap.yml 

（6）修改码云上的配置文件添加rabbitmq地址

```
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    virtual-host: /leyou
    username: leyou
    password: leyou
```

# 4.购物车功能分析

## 4.1.需求

需求描述：

- 用户可以在登录状态下将商品添加到购物车
- 用户可以在未登录状态下将商品添加到购物车
- 用户可以使用购物车一起结算下单
- 用户可以查询自己的购物车
- 用户可以在购物车中可以修改购买商品的数量。
- 用户可以在购物车中删除商品。
- *在购物车中展示商品优惠信息*
- 提示购物车商品价格变化
- 提示商品是否下架
- 提示商品库存是否充足
- 对商品结算下单



## 4.2.业务分析

在需求描述中，不管用户是否登录，都需要实现加入购物车功能，那么已登录和未登录下，购物车数据应该存放在哪里呢？

> 未登录购物车

用户如果未登录，将数据保存在服务端存在一些问题：

- 无法确定用户身份，需要借助与客户端存储识别身份
- 服务端数据存储压力增加，而且可能是无效数据

那么我们应该用把数据保存在客户端，这样每个用户保存自己的数据，就不存在身份识别的问题了，而且也解决了服务端数据存储压力问题。

> 已登录购物车

用户登录时，数据保存在哪里呢？

大家首先想到的应该是数据库，不过购物车数据比较特殊，读和写都比较频繁，存储数据库压力会比较大。因此我们可以考虑存入Redis中。

不过大家可能会担心Redis存储空间问题，我们可以效仿淘宝，限制购物车最多只能添加99件商品，或者更少。

# 5.未登录购物车

## 5.1.数据结构

首先分析一下未登录购物车的数据结构。

我们看下页面展示需要什么数据：

![1535944920655](assets/1535944920655.png)

因此每一个购物车信息，都是一个对象，包含：

```js
{
    skuId:2131241,
    title:"小米6",
    image:"",
    price:190000,
    num:1,
    ownSpec:"{"机身颜色":"陶瓷黑尊享版","内存":"6GB","机身存储":"128GB"}"
}
```

另外，购物车中不止一条数据，因此最终会是对象的数组。即：

```js
[
    {...},{...},{...}
]
```



## 5.2.web本地存储

知道了数据结构，下一个问题，就是如何保存购物车数据。前面我们分析过，可以使用Localstorage来实现。Localstorage是web本地存储的一种，那么，什么是web本地存储呢？

### 5.2.1.什么是web本地存储？

![1527587496457](assets/1527587496457.png)



web本地存储主要有两种方式：

- LocalStorage：localStorage 方法存储的数据没有时间限制。第二天、第二周或下一年之后，数据依然可用。 
- SessionStorage：sessionStorage 方法针对一个 session 进行数据存储。当用户关闭浏览器窗口后，数据会被删除。 



### 5.2.2.LocalStorage的用法

语法非常简单：

 ![1527587857321](assets/1527587857321.png)

```js
localStorage.setItem("key","value"); // 存储数据
localStorage.getItem("key"); // 获取数据
localStorage.removeItem("key"); // 删除数据
```

注意：**localStorage和SessionStorage都只能保存字符串**。

不过，在我们的common.js中，已经对localStorage进行了简单的封装：

 ![1527588011623](assets/1527588011623.png)



示例：

 ![1527588112975](assets/1527588112975.png)

## 5.3.添加购物车

购物车的前端js和页面都已经实现好了，我们在商品详情页面，点击加入购物车按钮：

 ![1535969897212](assets/1535969897212.png)

即可将数据加入localstorage中：

![1535632873353](assets/1535632873353.png)

同时，页面会跳转到购物车列表页面，不过，现在看不到任何商品：

![1535633026048](assets/1535633026048.png)



## 5.4.查询购物车

> 业务分析

我们进入购物车列表页，会看到控制台记录了一次请求：

 ![1554947234083](assets/1554947234083.png)

这其实是在渲染前，要查询sku信息。

可能大家会问，之前的购物车数据中，已经有了图片、价格等信息，为什么这里还要查询sku数据呢？

还记得之前我们的需求吗，我们要做价格对比，而且购物车中的商品很可能已经下架了，需要去查询并且做出判断，在页面中渲染。



> 商品sku查询接口

分析请求：

- 请求方式：Get
- 请求路径：/api/item/sku/list，证明是商品微服务，商品查询都是以/api/item开头。
- 请求参数：ids，是购物车中多个sku的id以`,`分割的字符串
- 返回结果：sku的集合，里面需要有价格、库存，用来提示用户

接下来我们就在`ly-item-service`中的来实现查询代码

首先是`GoodsController`中，添加新的接口 ：

```java
/**
     * 根据id批量查询sku
     * @param ids skuId的集合
     * @return sku的集合
     */
@GetMapping("sku/list")
public ResponseEntity<List<SkuDTO>> querySkuByIds(@RequestParam("ids") List<Long> ids) {
    return ResponseEntity.ok(this.goodsService.querySkuListByIds(ids));
}
```

Service：

```java
public List<SkuDTO> querySkuListByIds(List<Long> ids) {
    // 查询sku
    List<Sku> skuList = skuMapper.selectByIdList(ids);
    if(CollectionUtils.isEmpty(skuList)){
        throw new LyException(ExceptionEnum.GOODS_NOT_FOUND);
    }
    return BeanHelper.copyWithCollection(skuList, SkuDTO.class);
}
```

刷新购物车页面，查看：

![1535635900602](assets/1535635900602.png)



## 5.5.修改和删除购物车

页面JS已经实现，都是直接使用localstorage实现，离线即可完成，无需与后台服务端交互。

另外，清除下架商品，删除选中商品等按钮功能暂时没有完成，大家可以作为作业实现。

# 6.搭建购物车服务

接下来是已登录的购物车，我们需要创建独立微服务，实现购物车功能。

## 6.1.创建module

![1554950436166](assets/1554950436166.png)

![1554950452738](assets/1554950452738.png)

## 6.2.pom依赖

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

    <artifactId>ly-cart</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
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

## 6.3.配置文件

```yaml
server:
  port: 8088
spring:
  application:
    name: cart-service
  redis:
    host: 127.0.0.1
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
    registry-fetch-interval-seconds: 10
  instance:
    prefer-ip-address: true
    ip-address: 127.0.0.1
```

## 6.4.启动类

```java
@SpringBootApplication
@EnableDiscoveryClient
public class LyCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(LyCartApplication.class, args);
    }
}
```

## 6.5.网关路由

在ly-gateway中添加路由：

![1554950690130](assets/1554950690130.png) 



## 6.6.项目结构

![1554950726614](assets/1554950726614.png) 

# 7.已登录购物车

接下来，我们完成已登录购物车。

## 7.1.获取登录用户信息

购物车系统只负责登录状态的购物车处理，因此需要想办法获取登录的用户信息，如何才能获取呢？

### 7.1.1.思路分析

要获取登录的用户信息，有以下几种方式：

- 方式一：页面直接把用户作为请求参数传递
  - 优点：简单，方便，代码量为0
  - 缺点：不安全，因为调用购物车CRUD的请求是从页面发过来的，我们不能确定这个传递来的id是不是真的是用户的id
- 方式二：自己从cookie的token中解析JWT
  - 优点：安全
  - 缺点：
    - 需要重复校验JWT，已经在网关中做过了
    - 代码麻烦
- 方案三：在网关校验用户的时候，把用户信息传递到后面的微服务
  - 优点：安全，并且微服务不需要自己解析
  - 缺点：
    - 需要在网关加入新的逻辑
    - 微服务也要写获取用户的逻辑，代码麻烦



以上三种方式各有一定的优缺点，不存在对与错，不同业务时的取舍问题。

我们选择方式二，但是方式二中的需要解析JWT，性能太差，因为token中的载荷是BASE64编码，可以不用验证jwt，直接解析载荷即可。

需要在JwtUtils中添加一个新的工具，作用是利用Base64解析载荷，而不做签名校验：

```java
private static final Decoder<String, byte[]> stringDecoder = Decoders.BASE64URL;
/**
     * 获取token中的载荷信息
     *
     * @param token     用户请求中的令牌
     * @return 用户信息
     */
public static <T> Payload<T> getInfoFromToken(String token, Class<T> userType) throws UnsupportedEncodingException {
    String payloadStr = StringUtils.substringBetween(token, ".");
    byte[] bytes = stringDecoder.decode(payloadStr);
    String json = new String(bytes, "UTF-8");
    Map<String, String> map = JsonUtils.toMap(json, String.class, String.class);
    Payload<T> claims = new Payload<>();
    claims.setId(map.get("jti"));
    claims.setExpiration(new Date(Long.valueOf(map.get("exp"))));
    claims.setUserInfo(JsonUtils.toBean(map.get("user"), userType));
    return claims;
}
```

### 7.1.2.在购物车服务获取用户

#### 思路分析

购物车中的每个业务都需要获取当前登录的用户信息，如果在每个接口中都写这样一段逻辑，显然是冗余的。我们是不是可以利用AOP的思想，拦截每一个进入controller的请求，统一完成登录用户的获取呢。

因此，这里获取登录用户有两步要操作：

- 编写AOP拦截，统一获取登录用户
  - 这个可以使用SpringMVC的通用拦截器：`HandlerInterceptor`来实现
- 把用户保存起来，方便后面的controller使用
  - 保存用户信息到哪里呢？
  - 每次请求都有不同的用户信息，在并发请求情况下，必须保证每次请求保存的用户信息互不干扰，线程独立。

#### ThreadLocal保存用户信息

在并发请求情况下，因为每次请求都有不同的用户信息，我们必须保证每次请求保存的用户信息互不干扰，线程独立。

注意：这里不是**解决多线程资源共享问题，而是要保证每个线程都有自己的用户资源，互不干扰**。

而JDK中提供的`ThreadLocal`恰好满足这个需求，那么ThreadLocal是如何实现这一需求的呢？

看下这幅图：

  ![1554964371849](assets/1554964371849.png)

关键点：

- 每个线程（`Thread`）内部都持有一个`ThreadLocalMap`对象。
- `ThreadLocalMap`的Key是某个`ThreadLocal`对象，值是任意Object。
- 不同线程，内部有自己的`ThreadLocalMap`，因此Map中的资源互相不会干扰。



数据在堆栈中的存储示意图：

![1554964522200](assets/1554964522200.png)

下面我们定义容器，存储用户数据，因为以后其它服务也可能用，我们定义到`ly-common`中：

```java
package com.leyou.common.threadlocals;

/**
 * threadlocal 的容器
 */
public class UserHolder {

    private static ThreadLocal<Long> tl = new ThreadLocal<>();

    /**
     * 设置用户id
     * @param userId
     */
    public static void setUserId(Long userId){
        tl.set(userId);
    }

    /**
     * 获取用户id
     * @return
     */
    public static Long getUserId(){
        return tl.get();
    }

    /**
     * 删除用户id
     */
    public static void removeUserId(){
        tl.remove();
    }

}

```

![1554965501195](assets/1554965501195.png) 



#### 获取用户信息并保持

最后一步，定义SpringMVC的拦截器，在拦截器中获取用户信息，并保存到ThreadLocal中即可。

首先是定义拦截器：

```java
package com.leyou.cart.interceptors;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.threadlocals.UserHolder;
import com.leyou.common.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截器
 * 拦截请求，获取用户id
 */
@Slf4j
public class UserInterceptor implements HandlerInterceptor {
    /**
     * 拦截请求
     * 获取token
     * 获取userid
     * 放入容器
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try{
            String token = CookieUtils.getCookieValue(request, "LY_TOKEN");
//            获取payload
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, UserInfo.class);
//            获取用户id
            Long userId = payload.getUserInfo().getId();
            log.info("获取到用户id，{}",userId);
//            放入容器,把userid 放入threadlocal中
            UserHolder.setUserId(userId);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            // 解析失败，不继续向下
            log.error("【购物车服务】解析用户信息失败！", e);
            return false;
        }
    }

    /**
     * 从threadlocal中删除用户id
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        log.info("删除threadlocal的userId");
        UserHolder.removeUserId();
    }
}

```

然后配置到SpringMVC中：

```java
package com.leyou.cart.config;

import com.leyou.cart.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author HeiMa
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserInterceptor()).addPathPatterns("/**");
    }
}
```

![1554965981476](assets/1554965981476.png) 



## 7.2.后台购物车设计

### 数据结构设计

当用户登录时，我们需要把购物车数据保存到后台，可以选择保存在数据库。但是购物车是一个读写频率很高的数据。因此我们这里选择读写效率比较高的Redis作为购物车存储。

Redis有5种不同数据结构，这里选择哪一种比较合适呢？

- 首先不同用户应该有独立的购物车，因此购物车应该以用户的作为key来存储，Value是用户的所有购物车信息。这样看来基本的`k-v`结构就可以了。
- 但是，我们对购物车中的商品进行增、删、改操作，基本都需要根据商品id进行判断，为了方便后期处理，我们的购物车也应该是`k-v`结构，key是商品id，value才是这个商品的购物车信息。

综上所述，我们的购物车结构是一个双层Map：Map<String,Map<String,String>>

- 第一层Map，Key是用户id
- 第二层Map，Key是购物车中商品id，值是购物车数据

### 实体类

后台的购物车结构与前台是一样的：

```java
package com.leyou.cart.entity;

import lombok.Data;

@Data
public class Cart {
    private Long skuId;// 商品id
    private String title;// 标题
    private String image;// 图片
    private Long price;// 加入购物车时的价格
    private Integer num;// 购买数量
    private String ownSpec;// 商品规格参数
}
```



## 7.3.添加商品到购物车

### 7.3.1.页面发起请求：

我们再次回到商品详情页，登录以后，点击加入购物车，发现控制台发起了请求：

 ![1535636545217](assets/1535636545217.png)

这里发起的是Json格式数据。那么我们后台也要以json接收。

### 7.3.2.后台添加购物车

#### controller

先分析一下：

- 请求方式：新增，肯定是Post
- 请求路径：/cart ，这个其实是Zuul路由的路径，我们可以不管
- 请求参数：Json对象，包含购物车的所有属性，我们可以用购物车对象介绍
- 返回结果：无

```java
@RestController
public class CartController {

    @Autowired
    private CartService cartService;

 /**
     * 添加购物车
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> saveCart(@RequestBody CartDTO cartDTO){
        cartService.saveCart(cartDTO);
        return ResponseEntity.noContent().build();
    }
}
```

#### Service

这里我们不访问数据库，而是直接操作Redis。基本思路：

- 先查询之前的购物车数据
- 判断要添加的商品是否存在
  - 存在：则直接修改数量后写回Redis
  - 不存在：新建一条数据，然后写入Redis

代码：

```java
package com.leyou.cart.service;

import com.leyou.cart.entity.Cart;
import com.leyou.common.threadlocals.UserHolder;
import com.leyou.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "ly:cart:uid:";
/**
     * 新增购物车数据
     * 先获取购物车数据，如果当前数据已经存在，需要累加
     * @param cartDTO
     * @return
     */
    public void saveCart(CartDTO cartDTO) {

        try{
//            从threadlocal 中获取uid
            Long userId = UserHolder.getUserId();
//            组成redisKey
            String redisKey = PRE_FIX+userId; //ly:cart:uid:123
            BoundHashOperations<String, String, String> boundHashOps = redisTemplate.boundHashOps(redisKey);
//            获取hashKey - skuId
            String hashKey = cartDTO.getSkuId().toString();
//            先获取购物的数据
            String cartJson = boundHashOps.get(hashKey);
            if(!StringUtils.isBlank(cartJson)){
//                把json转对象
                CartDTO cart = JsonUtils.toBean(cartJson, CartDTO.class);
//                获取数量
                Integer num = cart.getNum();
                cartDTO.setNum(num + cartDTO.getNum());
            }
//            把购物车对象转json字符串 放入redis中
            boundHashOps.put(hashKey, JsonUtils.toString(cartDTO));
        }catch (Exception e){
            e.printStackTrace();
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }

    }
}
```



### 7.3.3.结果：

 ![1527776569221](assets/1527776569221.png)



## 🎗经验分享-购物车数量

### 1.用户登录后，添加商品到购物车功能代码如下

```java
@Override
public void addCart(Cart cart) {
    //1、从线程中获取用户id
    String userId = UserHolder.getUserId();
    //2、基于用户id获取redis中当前用户购物车列表数据
    BoundHashOperations<String, String, String> boundHashOps = redisTemplate.boundHashOps(REFIX_KEY + userId);
    //3、添加购物车数据到redis
    //获取提交的购物车商品id
    String skuId = cart.getSkuId().toString();
    //3、判断当前添加的购物车商品是否存在于购物车列表中
    Boolean boo = boundHashOps.hasKey(skuId);

    if(boo){
        //3.1 如果存在当前商品，购物车商品数量相加
        //获取页面提交的商品数据
        Integer num = cart.getNum();
        //获取redis原有的购物车商品数据
        String cartStr = boundHashOps.get(skuId);
        Cart redisCart = JsonUtils.toBean(cartStr, Cart.class);
        //购物车商品数量相加
	    redisCart.setNum(redisCart.getNum()+num);
    }else {
        //3.2 如果不存在当前商品，添加商品到购物车
        boundHashOps.put(skuId,JsonUtils.toString(cart));
    }
}
```

### 2.出现的问题

> 页面提交添加商品到购物车时，购物车中如果已经存在该商品，但是购物车中该商品数据没有改变。操作过程如下：

#### 2.1 redis中“华为（HUAWEI） nova 手机”商品数据截图：

![59410839155](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594108391557.png)



#### 2.2 登录后，添加商品到购物车

![59410850294](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594108502949.png)



#### 2.3 发现redis中，“华为（HUAWEI） nova 手机”商品数量没变

![59410866967](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594108669677.png)



### 3.问题的分析

> 整个操作过程，程序没有报错。那就是业务逻辑代码可能出问题了。查看用户登录后，添加商品到购物车功能时，发现代码虽然将页面提交的购物车商品数据与redis中原来的购物车商品数量相加，但是并没有将相加后的结果再次存入redis。导致redis中还是原来的购物车数据。

![59410901215](../../../%E8%AF%BE%E7%A8%8B/%E7%BB%8F%E9%AA%8C%E5%80%BC/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E9%A1%B9%E7%9B%AE%E4%BA%8C_day11_%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83-%E6%B6%88%E6%81%AF%E6%80%BB%E7%BA%BF-%E8%B4%AD%E7%89%A9%E8%BD%A6%E7%BB%8F%E9%AA%8C%E5%80%BC-%E5%BC%A0%E5%AE%AA/assets/1594109012151.png)

### 4.问题解决办法

> 添加一行代码，将相加后的购物车商品数据再次存入redis即可

![59410927604](../../../%E8%AF%BE%E7%A8%8B/%E7%BB%8F%E9%AA%8C%E5%80%BC/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E9%A1%B9%E7%9B%AE%E4%BA%8C_day11_%E9%85%8D%E7%BD%AE%E4%B8%AD%E5%BF%83-%E6%B6%88%E6%81%AF%E6%80%BB%E7%BA%BF-%E8%B4%AD%E7%89%A9%E8%BD%A6%E7%BB%8F%E9%AA%8C%E5%80%BC-%E5%BC%A0%E5%AE%AA/assets/1594109276040.png)

再次添加商品到购物车时，已经在购物车存在的商品数量累加了

![59410916001](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594109160014.png)





## 7.4.查询购物车

### 7.4.1.页面发起请求

我们进入购物车列表页面，然后刷新页面，查看控制台的请求：

 ![1535636942868](assets/1535636942868.png)

### 7.4.2.后台实现

请求分析：

- 请求方式：Get
- 请求路径：/list
- 请求参数：无
- 返回结果：当前用户的购物车集合



> Controller

```java
    /**
     * 查询购物车集合
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity<List<CartDTO>> findCartList(){
        return ResponseEntity.ok(cartService.findCartList());
    }

```

> Service

```java
   /**
     * 查询用户的购物车数据
     * @return
     */
    public List<CartDTO> findCartList() {

        try{
//            从threadlocal 中获取uid
            Long userId = UserHolder.getUserId();
//            构造redisKey
            String redisKey = PRE_FIX + userId;
//            判断 redis中是否包含用户的购物车数据
            Boolean b = redisTemplate.hasKey(redisKey);
            if(b == null || !b){
                throw new LyException(ExceptionEnum.CARTS_NOT_FOUND);
            }
            BoundHashOperations<String, String, String> boundHashOps = redisTemplate.boundHashOps(redisKey);
//            获取用户的所有购物车数据
            List<String> values = boundHashOps.values();
            if(CollectionUtils.isEmpty(values)){
                throw new LyException(ExceptionEnum.CARTS_NOT_FOUND);
            }
            List<CartDTO> cartDTOList = values.stream().map(cartJSon -> {
                return JsonUtils.toBean(cartJSon, CartDTO.class);
            }).collect(Collectors.toList());
            return cartDTOList;
        }catch(Exception e){
            e.printStackTrace();
            throw new LyException(ExceptionEnum.CARTS_NOT_FOUND);
        }
    }
```



### 7.4.3.测试

![1554971700099](assets/1554971700099.png) 



## 7.5.修改商品数量

### 7.5.1.页面发起请求

 ![1535637474109](assets/1535637474109.png)

### 7.5.2.后台实现

> Controller

```java
/**
     * 修改数量
     * @param skuId
     * @param num
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> updateNum(@RequestParam(name = "id") Long skuId,
                                            @RequestParam(name = "num")Integer num){
        cartService.updateNum(skuId,num);
        return ResponseEntity.noContent().build();
    }
```

> Service

```java
 /**
     * 修改数量
     * @param skuId
     * @param num
     * @return
     */
    public void updateNum(Long skuId, Integer num) {
//        获取用户id
        Long userId = UserHolder.getUserId();
//        构造redisKey
        String redisKey = PRE_FIX + userId;
        Boolean b = redisTemplate.hasKey(redisKey);
        if(b == null || !b){
            log.error("购物车商品不存在，用户：{}, 商品：{}", userId, skuId);
            throw new LyException(ExceptionEnum.CARTS_NOT_FOUND);
        }
//        获取已经存在的购物车
        BoundHashOperations<String, String, String> boundHashOps = redisTemplate.boundHashOps(redisKey);
        String hashKey = skuId.toString();
        String cartJson = boundHashOps.get(hashKey);
        if(StringUtils.isBlank(cartJson)){
            log.error("购物车商品不存在，用户：{}, 商品：{}", userId, skuId);
            throw new LyException(ExceptionEnum.CARTS_NOT_FOUND);
        }
        CartDTO cartDTO = JsonUtils.toBean(cartJson, CartDTO.class);
//        修改数量
        cartDTO.setNum(num);
//        保存redis
        boundHashOps.put(hashKey,JsonUtils.toString(cartDTO));
    }
```

## 7.6.删除购物车商品

### 7.6.1.页面发起请求

 ![1535637499692](assets/1535637499692.png)

### 7.6.2.后台实现

> Controller

```java
/**
     * 删除购物车数据
     * @param skuId
     * @return
     */
    @DeleteMapping("/{skuId}")
    public ResponseEntity<Void> deleteCart(@PathVariable(name = "skuId") Long skuId){
        cartService.deleteCart(skuId);
        return ResponseEntity.noContent().build();
    }
```



> Service

```java
 /**
     * 删除购物车数据
     * @param skuId
     * @return
     */
    public void deleteCart(Long skuId) {
        Long userId = UserHolder.getUserId();
        String redisKey = PRE_FIX+userId;
        String hashKey = skuId.toString();
        redisTemplate.opsForHash().delete(redisKey,hashKey);
    }
```

# 8.登录后购物车合并

用户登录后，如果未登录下添加有购物车，则需要把未登录的购物车数据添加到已登录购物车列表中。

## 8.1.思路分析

基本流程如下：

- 当跳转到购物车页面，查询购物车列表前，需要判断用户登录状态
- 如果登录：
  - 首先检查用户的LocalStorage中是否有购物车信息，
  - 如果有，则提交到后台保存，
  - 清空LocalStorage
- 如果未登录，直接查询即可



## 8.2.前端页面

先看一下现在的加载购物车的逻辑：

![1554976355461](assets/1554976355461.png)



我们需要在加载已登录购物车数据之前，新增一段判断逻辑：

![1554976478290](assets/1554976478290.png)

部分代码如下：

```js
// 判断本地未登录购物车是否存在，
const carts = ly.store.get("carts") || [];
if (carts.length > 0) {
    // 如果存在，发到后台，添加到redis，删除本地购物车
    ly.http.post("/cart/list", carts).then(() => {
        // 查询购物车
        this.handleLoginCarts();
        // 删除本地购物车
        ly.store.del("carts");
    }).catch(() => {
        alert("购物车数据更新失败！")
    })
} else {
    // 查询购物车
    this.handleLoginCarts();
}
```



## 8.3.批量新增购物车

刷新页面，可以看到请求已经发出：

![1554976629597](assets/1554976629597.png)

### 8.3.1.controller

分析一下请求：

- 请求方式：POST
- 请求路径：/cart/list
- 请求参数：json数组，里面是cart对象
- 返回结果，应该是void

代码：

```java
/**
     * 批量保存合并 购物车数据
     * @param cartDTOList
     * @return
     */
    @PostMapping("/list")
    public ResponseEntity<Void> saveCartBatch(@RequestBody List<CartDTO> cartDTOList){
        cartService.saveCartBatch(cartDTOList);
        return ResponseEntity.noContent().build();
    }
```



### 8.3.2.service

批量新增，其实就是循环把集合中的每个购物车商品添加到redis。

```java
/**
     * 批量保存合并 购物车数据
     * @param cartDTOList
     * @return
     */
    public void saveCartBatch(List<CartDTO> cartDTOList) {
        Long userId = UserHolder.getUserId();
        String redisKey = PRE_FIX+userId;
        BoundHashOperations<String, String, String> boundHashOps = redisTemplate.boundHashOps(redisKey);
        for (CartDTO cartDTO : cartDTOList) {
            String hashKey = cartDTO.getSkuId().toString();
            String cartJson = boundHashOps.get(hashKey);
            if(!StringUtils.isBlank(cartJson)){
//                合并购物车数据
                CartDTO cacheCart = JsonUtils.toBean(cartJson, CartDTO.class);
//                合并数量
                cartDTO.setNum(cartDTO.getNum() + cacheCart.getNum());
            }
//            保存数据
            boundHashOps.put(hashKey,JsonUtils.toString(cartDTO));
        }
    }
```

# ThreadLocal

**ThreadLocal的接口方法**

ThreadLocal类接口很简单，只有4个方法，我们先来了解一下：

### **public void set(T value)**

设置当前线程的线程局部变量的值。

```java
public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
```

### **public T get()**

该方法返回当前线程所对应的线程局部变量。

```java
public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
```

### **public void remove()**

```java
public void remove() {
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }
```

将当前线程局部变量的值删除，目的是为了减少内存的占用，该方法是JDK 5.0新增的方法。需要指出的是，当线程结束后，对应该线程的局部变量将自动被垃圾回收，所以显式调用该方法清除线程的局部变量并不是必须的操作，但它可以加快内存回收的速度。

### **protected T initialValue()**

返回该线程局部变量的初始值，该方法是一个protected的方法，显然是为了让子类覆盖而设计的。这个方法是一个延迟调用方法，在线程第1次调用get()或set(Object)时才执行，并且仅执行1次。ThreadLocal中的缺省实现直接返回一个null。

# ThreadLocalMap

ThreadLocalMap是ThreadLocal的内部类，没有实现Map接口，用独立的方式实现了Map的功能，其内部的Entry也独立实现。

在ThreadLocalMap中，也是用Entry来保存K-V结构数据的。但是Entry中key只能是ThreadLocal对象，这点被Entry的构造方法已经限定死了。

```java
static class Entry extends WeakReference<ThreadLocal> {
    /** The value associated with this ThreadLocal. */
    Object value;

    Entry(ThreadLocal k, Object v) {
        super(k);
        value = v;
    }
}
```

Entry继承自WeakReference（弱引用，生命周期只能存活到下次GC前），但只有Key是弱引用类型的，Value并非弱引用。

**弱引用**：当一个对象仅仅被weak reference（弱引用）指向, 而没有任何其他strong reference（强引用）指向的时候, 如果这时GC运行, 那么这个对象就会被回收，不论当前的内存空间是否足够，这个对象都会被回收。

ThreadLocalMap的成员变量：

```java
static class ThreadLocalMap {
    /**
     * The initial capacity -- MUST be a power of two.
     */
    private static final int INITIAL_CAPACITY = 16;

    /**
     * The table, resized as necessary.
     * table.length MUST always be a power of two.
     */
    private Entry[] table;

    /**
     * The number of entries in the table.
     */
    private int size = 0;

    /**
     * The next size value at which to resize.
     */
    private int threshold; // Default to 0
}
```

### Hash冲突怎么解决

和HashMap的最大的不同在于，ThreadLocalMap结构非常简单，没有next引用，也就是说ThreadLocalMap中解决Hash冲突的方式并非链表的方式，而是采用线性探测的方式，所谓线性探测，就是根据初始key的hashcode值确定元素在table数组中的位置，如果发现这个位置上已经有其他key值的元素被占用，则利用固定的算法寻找一定步长的下个位置，依次判断，直至找到能够存放的位置。

ThreadLocalMap解决Hash冲突的方式就是简单的步长加1或减1，寻找下一个相邻的位置。

```
/**
 * Increment i modulo len.
 */
private static int nextIndex(int i, int len) {
    return ((i + 1 < len) ? i + 1 : 0);
}

/**
 * Decrement i modulo len.
 */
private static int prevIndex(int i, int len) {
    return ((i - 1 >= 0) ? i - 1 : len - 1);
}
```

显然ThreadLocalMap采用线性探测的方式解决Hash冲突的效率很低，如果有大量不同的ThreadLocal对象放入map中时发送冲突，或者发生二次冲突，则效率很低。

**所以这里引出的良好建议是：每个线程只存一个变量，这样的话所有的线程存放到map中的Key都是相同的ThreadLocal，如果一个线程要保存多个变量，就需要创建多个ThreadLocal，多个ThreadLocal放入Map中时会极大的增加Hash冲突的可能。**

### ThreadLocalMap的问题

由于ThreadLocalMap的key是弱引用，而Value是强引用。这就导致了一个问题，ThreadLocal在没有外部对象强引用时，发生GC时弱引用Key会被回收，而Value不会回收，如果创建ThreadLocal的线程一直持续运行，那么这个Entry对象中的value就有可能一直得不到回收，发生内存泄露。

**如何避免泄漏**
 既然Key是弱引用，那么我们要做的事，就是在调用ThreadLocal的get()、set()方法时完成后再调用remove方法，将Entry节点和Map的引用关系移除，这样整个Entry对象在GC Roots分析后就变成不可达了，下次GC的时候就可以被回收。

如果使用ThreadLocal的set方法之后，没有显示的调用remove方法，就有可能发生内存泄露，所以养成良好的编程习惯十分重要，使用完ThreadLocal之后，记得调用remove方法。

