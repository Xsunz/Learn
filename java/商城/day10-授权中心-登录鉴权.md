# 学习目标

- 实现登录，生成token功能
- 了解Zuul的敏感头过滤
- 实现登录状态判断
- 实现token刷新
- 实现登出操作，理解黑名单
- 理解网关统一登录验证流程
- 使用zuul自定义过滤器，并理解白名单机制

# 1.授权中心

授权中心的主要职责：

- 用户登录鉴权：
  - 接收用户的登录请求，
  - 通过用户中心的接口校验用户名密码
  - 使用私钥生成JWT并返回
- 用户登录状态校验
  - 判断用户是否登录，其实就是token的校验
- 用户登出
  - 用户选择退出登录后，要让token失效
- 用户登录状态刷新
  - 用户登录一段时间后，JWT可能过期，需要刷新有效期

接下来，我们逐一完成上述功能

## 1.1.创建授权中心

### 1.1.1.搭建项目

创建maven工程：

![1554559656259](assets/1554559656259.png)

选择目录：

![1554559691774](assets/1554559691774.png)



### 1.1.2.引入依赖

pom文件：

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

    <artifactId>ly-auth</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-user-interface</artifactId>
            <version>1.0-SNAPSHOT</version>
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



创建启动类：

```java
package com.leyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class LyAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyAuthApplication.class, args);
    }
}
```

### 1.1.3.配置

application.yml

```yaml
server:
  port: 8087
spring:
  application:
    name: auth-service
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
    registry-fetch-interval-seconds: 5
  instance:
    ip-address: 127.0.0.1
    prefer-ip-address: true
```



添加路由规则：

在api-gateway中添加路由规则：

```yaml
zuul:
  prefix: /api
  routes:
    auth-service: /auth/**
```



项目结构：

![1554559998875](assets/1554559998875.png) 



## 1.2.登录功能

接下来，我们需要在`ly-auth`编写一个接口，对外提供登录授权服务。

登录授权流程我们上面已经分析过，基本流程如下：：

- 1、用户请求登录，携带用户名密码到授权中心
- 2、授权中心携带用户名密码，到用户中心查询用户
- 3、查询如果正确，生成JWT凭证，查询错误则返回400,
- 4、返回JWT给用户



上面的步骤有几个步骤需要我们去解决：

- 校验用户名密码必须到用户中心去做，因此**用户中心必须对外提供的接口**，根据用户名和密码查询用户。
- 生成JWT的过程需要私钥，验证签名需要公钥，因此**需要在授权中心启动时加载公钥和私钥**
- 返回JWT给用户，需要在以后的请求中携带jwt，那么**客户端该把这个JWT保存在哪里呢**？



### 1.2.1.读取公钥和私钥

#### 1）编写配置

我们需要在授权中心完成授权，肯定要用到公钥、私钥、还有JWT工具，必须知道公钥、私钥文件的位置，另外生成token的有效期等信息，这些可以配置到`application.yml`中：

```yaml
ly:
  jwt:
    pubKeyPath: C:/develop/ssh/id_rsa.pub # 公钥地址
    priKeyPath: C:/develop/ssh/id_rsa # 私钥地址
```



#### 2）属性读取

然后编写属性类，加载这些数据：

```java
package com.leyou.auth.config;

import com.leyou.common.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@Slf4j
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties implements InitializingBean {

    private String pubKeyPath;
    private String priKeyPath;
}
```

为了让属性类生效，在启动类上添加注解，启用这个属性：

![1554619534656](assets/1554619534656.png)



#### 3）加载公钥和私钥

思考一下：这个属性类只帮我们读取了公钥和私钥的地址，那么每次使用公钥我们都需要从硬盘读取，效率是不是太低了，能不能在这个类中，直接读取公钥和私钥，保存起来，供以后使用呢？



我们来试一下。

那么问题来了，**加载公钥和私钥的代码应该写在哪里呢？构造函数可以吗？**

显然不行，因为构造函数执行时，Spring还没有完成属性注入，此时pubKeyPath和priKeyPath都没有值，我们**`必须在Spring完成属性初始化后再加载密钥`**。

那么，我们如何知道Spring完成了属性初始化呢？

这就必须要知道Spring的Bean初始化生命周期了，如图：

![1554617122160](assets/1554617122160.png)

Spring Bean在Spring Bean Factory Container中完成其整个生命周期：以下是完成其生命周期所需的各种内容：

1. Spring容器从XML文件或@Configuration中bean的定义中实例化bean。
2. Spring依据配置中指定的属性，为bean填充所有属性。
3. 如果bean实现BeanNameAware接口，spring调用setBeanName()方法，并传递bean的id。
4. 如果bean实现BeanFactoryAware接口，spring将调用setBeanFactory()方法，并把自己作为参数。
5. 如果bean实现ApplicationContextAware接口，spring将调用setApplicationContext()方法，并把ApplicationContext实例作为参数。
6. 如果存在与bean关联的任何BeanPostProcessors（后处理器），则调用preProcessBeforeInitialization()方法。比如Autowired等依赖注入功能是在此时完成。
7. 如果Bean实现了InitializingBean接口，则调用bean的afterPropertiesSet()方法。
8. 如果为bean指定了init-method，那么将调用bean的init方法。
9. 最后，如果存在与bean关联的任何BeanPostProcessors，则将调用postProcessAfterInitialization（）方法。

因此，我们加载公钥、私钥可以再7或8的两个位置来完成。比如我们在7的位置，需要两步：

- 实现InitializingBean接口
- 实现afterPropertiesSet方法，并在方法内加载密钥

示例：

```java
package com.leyou.auth.config;

import com.leyou.common.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@Slf4j
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties implements InitializingBean {
    /**
     * 公钥地址
     */
    private String pubKeyPath;
    /**
     * 私钥地址
     */
    private String priKeyPath;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 获取公钥和私钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            log.error("初始化公钥和私钥失败！", e);
            throw new RuntimeException(e);
        }
    }
}
```

### 🎗经验分享-配置类初始化

#### 1.授权中心配置类设置公钥私钥



> yml中配置的公钥私钥地址

```properties
ly:
  jwt:
    pubKeyPath: C:/develop/ssh/id_rsa.pub # 公钥地址
    priKeyPath: C:/develop/ssh/id_rsa # 私钥地址
```



> LyAuthApplication入口类代码

```java
@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
public class LyAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(LyAuthApplication.class,args);
    }
}
```



> 加载公钥私钥的JwtProperties类

```java
@Data
@ConfigurationProperties("ly.jwt")
@Component
@Slf4j
public class JwtProperties implements InitializingBean{
    private String pubKeyPath;  //公钥路径
    private String priKeyPath;  //私钥路径
    private PublicKey publicKey;  //公钥对象
    private PrivateKey privateKey; //私钥对象
    
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            publicKey = RsaUtils.getPublicKey(pubKeyPath);
            privateKey = RsaUtils.getPrivateKey(priKeyPath);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("初始化公钥和私钥失败！", e);
            throw new RuntimeException(e);
        }
    }
}
```



> AuthService类的登录方法，注入了userClient远程调用和prop类

```java
@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties prop;

    public void login(String username, String password, HttpServletResponse response) {
        try {
            //1.通过feign远程调用获取用户信息
            UserDTO userDTO = userClient.queryUserByUsernameAndPassword(username, password);

            //2.通过jwt工具类生成token信息
            UserInfo userInfo = new UserInfo(userDTO.getId(),userDTO.getUsername(),"user_role");

            //用户信息，RAS私钥加密，过期时间30分钟
            String token = JwtUtils.generateTokenExpireInMinutes(userInfo, prop.getPrivateKey(), prop.getUser().getExpire());

            System.out.println("token="+token);

            //3.将token存入cookie中
            CookieUtils.newCookieBuilder()
                    .value(token)                           //存入token
                    .name(prop.getUser().getCookieName())   //token在cookie只存的名称
                    .response(response)
                    .httpOnly(true)                         //防止xssf攻击用
                    .domain(prop.getUser().getCookieDomain())   //域名
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            //不论业务中出现任何问题，都提示密码错误
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }

    }
}
```



#### 2.出现的问题

启动项目出现错误：

![image-20200715225320453](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/image-20200715225320453.png)

#### 3.问题的分析

> 这里的错误信息提示在AuthService类中的字段prop需要一个bean，但是找到了2个，这就是考察是否对@EnableConfigurationProperties(JwtProperties.class)是教案上提到自动注入JwtProperties类的方法
>
> 但是开发中找个注解可以被@Component注解替代掉，两个注解不能同时使用。会造成容器中存在两个同类型的Bean无法区分的问题



#### 4.问题解决办法

解决本次的Bug，建议用以下的思路去解决

> 首先解决方案一：可以去掉入口类的@EnableConfigurationProperties(JwtProperties.class)

> 如果还不行，可以尝试方案二：或者去掉JwtProperties类的@Component注解

> 最后再尝试方案三：JwtProperties中的Component设置别名
>
> ```java
> @Data
> @ConfigurationProperties("ly.jwt")
> @Component("jwtTest")
> @Slf4j
> public class JwtProperties
> ```
>
> 在AuthService类中自动注入后通过Qualifier指定名称
>
> ```java
> @Autowired
> @Qualifier("jwtTest")
> private JwtProperties prop;
> ```





### 1.2.2.查询用户接口

用户中心必须对外提供查询接口，方便ly-auth做用户名密码校验。

首先在`ly-user-interface`定义接口：

引入Feign依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-openfeign-core</artifactId>
</dependency>
```

声明接口：

```java
package com.leyou.user.client;

import com.leyou.user.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient("user-service")
public interface UserClient {
    /**
     * 根据用户名和密码查询用户
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    @GetMapping("query")
    UserDTO queryUserByUsernameAndPassword(@RequestParam("username") String username, @RequestParam("password") String password);
}
```



### 1.2.3.JWT客户端存储方案

我们把jwt返回到客户端，客户端保存到哪里呢？

目前有两种解决方案：

- 方案一：存入web存储如：LocalStorage或SessionStorage中
  - 优点：
    - 不用担心cookie禁用问题
    - 不会随着浏览器自动发送，可以减少不必要的请求头大小
  - 缺点：
    - 不会随着浏览器自动发送，需要前段额外代码，携带jwt
    - 会遭到XSS（跨站脚本）攻击
- 方案二：存入cookie
  - 优点：
    - 会随着浏览器自动发送，客户端不用任何额外代码
    - 使用httponly，避免XSS攻击风险
  - 缺点：
    - 会随着浏览器自动发送，某些时候有些多余
    - 可能遭到CSRF（跨站资源访问）攻击



这里我们采用哪一种呢？

我们采用cookie方案，cookie方案的两个缺陷我们也可以解决：

- 问题1：会随着浏览器自动发送，某些时候有些多余
  - 解决：后端服务与其它服务资源（如静态资源）采用不同域名，浏览器的同源策略会限制cookie
- 问题2：可能遭到CSRF（跨站资源访问）攻击
  - 解决：避免get请求操作服务器资源，遵循Rest风格，必要时在token中存入随机码



我们在ly-common中编写的**CookieUtils**可以帮我们快捷实现cookie的读写问题：

![1554618845357](assets/1554618845357.png) 



### 1.2.4.登录代码实现

接下来我们就在`ly-auth`编写授权接口，接收用户名和密码，校验成功后，写入cookie中。

> #### controller
>

- 请求方式：post
- 请求路径：/login
- 请求参数：username和password
- 返回结果：无，直接写入cookie

代码：

```java
package com.leyou.auth.web;

import com.leyou.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 登录授权
     *
     * @param username 用户名
     * @param password 密码
     * @return 无
     */
    @PostMapping("login")
    public ResponseEntity<Void> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletResponse response) {
        // 登录
        authService.login(username, password, response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

```



> #### service

service的基本流程：

- 查询用户
- 生成token
- 写入cookie

这里还有几个属性要配置，包括：

- token有效期
- cookie名称
- cookie的domain属性，决定cookie在哪些域名下生效

这三个属性我们也配置到配置文件，不过我们做下特殊标记，这些虽然与JWT有关，但却是用户登录相关属性，因此这样来配置：

```yaml
ly:
  jwt:
    pubKeyPath: /Users/zhanghuyi/devlop/ssh/id_rsa.pub # C:/develop/ssh/id_rsa.pub # 公钥地址
    priKeyPath: /Users/zhanghuyi/devlop/ssh/id_rsa #C:/develop/ssh/id_rsa # 私钥地址
    user:
      expire: 30 # 过期时间,单位分钟
      cookieName: LY_TOKEN # cookie名称
      cookieDomain: leyou.com # cookie的域
```

注意：cookie的domain决定了cookie作用的域名，写成"`leyou.com`"可以让leyou.com下的所有二级域名共享cookie

然后在`JwtProperties`中添加属性：

```java

@Data
@Slf4j
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties implements InitializingBean {
    /**
     * 公钥地址
     */
    private String pubKeyPath;
    /**
     * 私钥地址
     */
    private String priKeyPath;

    /**
     * 用户token相关属性
     */
    private UserTokenProperties user = new UserTokenProperties();

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @Data
    public class UserTokenProperties {
        /**
         * token过期时长
         */
        private int expire;
        /**
         * 存放token的cookie名称
         */
        private String cookieName;
        /**
         * 存放token的cookie的domain
         */
        private String cookieDomain;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 获取公钥和私钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            log.error("初始化公钥和私钥失败！", e);
            throw new RuntimeException(e);
        }
    }
}
```



完整的service代码：

```java
package com.leyou.auth.service;

import com.leyou.auth.config.JwtProperties;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.common.utils.CookieUtils;
import com.leyou.user.client.UserClient;
import com.leyou.user.dto.UserDTO;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Service
public class AuthService {
    @Autowired
    private JwtProperties prop;

    @Autowired
    private UserClient userClient;

    private static final String USER_ROLE = "role_user";

    public void login(String username, String password, HttpServletResponse response) {
        try {
            // 查询用户
            UserDTO user = userClient.queryUserByUsernameAndPassword(username, password);
            // 生成userInfo, 没写权限功能，暂时都用guest
            UserInfo userInfo = new UserInfo(user.getId(), user.getUsername(), USER_ROLE);
            // 生成token
            String token = JwtUtils.generateTokenExpireInMinutes(userInfo, prop.getPrivateKey(), prop.getUser().getExpire());
            // 写入cookie
            CookieUtils.newCookieBuilder()
                    .response(response) // response,用于写cookie
                    .httpOnly(true) // 保证安全防止XSS攻击，不允许JS操作cookie
                    .domain(prop.getUser().getCookieDomain()) // 设置domain
                    .name(prop.getUser().getCookieName()).value(token) // 设置cookie名称和值
                    .build();// 写cookie
        } catch (Exception e) {
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
    }
}
```



### 1.2.5.项目结构：

![1554619834459](assets/1554619834459.png) 



### 1.2.6.测试

![1554620493081](assets/1554620493081.png)

响应：

![1554620531912](assets/1554620531912.png)





## 1.3.解决cookie写入问题

接下来，我们看看登录页面，是否能够正确的发出请求。

我们在页面输入登录信息，然后点击登录：

 ![1527516871646](assets/1527516871646.png)

成功跳转到了首页：

![1527518012727](assets/1527518012727.png)

接下来我们查看首页cookie：

 ![1527518123592](assets/1527518123592.png)

什么都没有，为什么？

### 1.3.1.问题分析

我们在客户端工具与页面访问的区别是什么呢？

- 客户端工具访问的是：localhost:8087/login
- 页面访问的是：api.leyou.com/api/auth/login

难道URL路径不同，导致了cookie问题？

那么，为了验证这件事情，我们在客户端工具中访问一下api.leyou.com/api/auth/login试试：

![1554620784922](assets/1554620784922.png)

果然，虽然请求返回200，但是我们并没有在头中看到cookie，这是怎么回事呢？



两种访问的区别在哪里呢？仅仅是url地址不同吗？？

你应该能想到了：

- `localhost:8087`直接访问的是微服务
- `api.leyou.com/api/`则会先把请求发送到网关Zuul

那么，会不会是Zuul把cookie给弄丢了呢？



### 1.3.2.Zuul的敏感头过滤

Zuul内部有默认的过滤器，会对请求和响应头信息进行重组，过滤掉敏感的头信息：

![1527521011633](assets/1527521011633.png)

会发现，这里会通过一个名为`SensitiveHeaders`的属性，来获取敏感头列表，然后添加到`IgnoredHeaders`中，这些头信息就会被忽略。

而这个`SensitiveHeaders`来自于一个名为ZuulProperties的类，默认值就包含了`set-cookie、Cookie、Authorization`：

 ![1527521167107](assets/1527521167107.png)

因此解决方案就是覆盖这个属性，我们查看ZuulProperties这个类：

![1554621090007](assets/1554621090007.png) 

发现其属性前缀是：zuul，因此我们可以通过修改application.yml文件，覆盖默认配置。

有两种覆盖方式：

全局设置：

- `zuul.sensitive-headers=` 

会作用于所有路径。

指定路由设置：

- `zuul.routes.<routeName>.sensitive-headers=`
- `zuul.routes.<routeName>.custom-sensitive-headers=true`

此处我们采用全局配置：

![1554621221550](assets/1554621221550.png)

### 1.3.3.最后的测试

再次重启后测试，发现token成功写入了：

![1527521423469](assets/1527521423469.png)



## 1.4.判断登录状态

虽然cookie已经成功写入，但是我们首页的顶部，登录状态依然没能判断出用户信息：

 ![1527521794580](assets/1527521794580.png)

我们思考一下，应该如何判断用户是否登录呢？

### 1.4.1.步骤分析

传统登录校验的步骤：

- 1）用户请求到达服务端，会自动携带cookie
- 2）cookie中包含sessionId，tomcat根据sessionId获取session
- 3）从session中读取用户信息，判断是否存在
- 4）存在，证明已经登录；不存在，证明登录超时或未登录

我们现在使用的是无状态登录，不存在session，而是把用户身份写入了token，是否需要发请求到服务端进行校验呢？

肯定需要的，因为token需要通过公钥解析才知道是否有效。

分析一下步骤：

- 1）页面向后台发起请求，携带cookie
- 2）后台获取cookie中的LY_TOKEN
- 3）校验token是否有效
  - 无效：登录失效
  - 有效：解析出里面的用户信息，返回到页面

接下来，我们就分步实现上述功能。

### 1.4.2.页面JS代码

首先是页面发起请求，校验cookie。

页面的顶部已经被我们封装为一个独立的Vue组件，在`/js/pages/shortcut.js`中

 ![1527522039407](assets/1527522039407.png)

打开js，发现里面已经定义好了Vue组件，并且在created函数中，查询用户信息：

 ![1527552697969](assets/1527552697969.png)

查看网络控制台，发现发起了请求：

![1527553282028](assets/1527553282028.png)

因为token在cookie中，因此本次请求肯定会携带token信息在头中。



### 1.4.3.校验用户登录状态

我们在`ly-auth`中定义用户的校验接口，通过cookie获取token，然后校验通过返回用户信息。

- 请求方式：GET
- 请求路径：/verify
- 请求参数：无，不过我们需要从cookie中获取token信息
- 返回结果：UserInfo，校验成功返回用户信息；校验失败，则返回401

controller代码：

```java
/**
     * 验证用户信息
     *
     * @return 用户信息
     */
@GetMapping("verify")
public ResponseEntity<UserInfo> verifyUser(HttpServletRequest request, HttpServletResponse response) {
    // 成功后直接返回
    return ResponseEntity.ok(authService.verifyUser(request, response));
}
```

service代码：

```java
public UserInfo verifyUser(HttpServletRequest request, HttpServletResponse response) {
    try {
        // 读取cookie
        String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
        // 获取token信息
        Payload<UserInfo> payLoad = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), UserInfo.class);
        return payLoad.getUserInfo();
    } catch (Exception e) {
        // 抛出异常，证明token无效，直接返回401
        throw new LyException(ExceptionEnum.UNAUTHORIZED);
    }
}
```



### 1.4.4.测试

 ![1527553980146](assets/1527553980146.png)

页面效果：

![1527554017189](assets/1527554017189.png)



## 1.5.刷新token

JWT内部设置了token的有效期，默认是30分钟，30分钟后用户的登录信息就无效了，用户需要重新登录，用户体验不好，怎么解决呢？

JWT的缺点就凸显出来了：

- JWT是生成后无法更改，因此我们无法修改token中的有效期，也就是无法续签。

怎么办？

3种解决方案：

- 方案1：每次用户访问网站，都重新生成token。操作简单粗暴，但是token写入频率过高，效率实在不好。
- 方案2：登录时，除了生成jwt，还另外生成一个刷新token，每当token过期，如果用户持有刷新token，则为其重新生成一个token。这种方式比较麻烦，而且会增加header大小。
- 方案3：cookie即将到期时，重新生成一个token。比如token有效期为30分钟，当用户请求我们时，我们可以判断如果用户的token有效期还剩下15分钟，那么就重新生成token，可以看做上面两种的折中方案。

我们采用方案3，在验证登录的逻辑中，加入一段时间判断逻辑，如果距离有效期不足15分钟，则生成一个新token：

```java
public UserInfo verifyUser(HttpServletRequest request, HttpServletResponse response) {
    try {
        // 读取cookie
        String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
        // 获取token信息
        Payload<UserInfo> payLoad = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), UserInfo.class);
        // 获取过期时间
        Date expiration = payLoad.getExpiration();
        // 获取刷新时间
        DateTime refreshTime = new DateTime(expiration.getTime()).minusMinutes(prop.getUser().getMinRefreshInterval());
        // 判断是否已经过了刷新时间
        if (refreshTime.isBefore(System.currentTimeMillis())) {
            // 如果过了刷新时间，则生成新token
            token = JwtUtils.generateTokenExpireInMinutes(payLoad.getUserInfo(), prop.getPrivateKey(), prop.getUser().getExpire());
            // 写入cookie
            CookieUtils.newCookieBuilder()
                // response,用于写cookie
                .response(response)
                // 保证安全防止XSS攻击，不允许JS操作cookie
                .httpOnly(true)
                // 设置domain
                .domain(prop.getUser().getCookieDomain())
                // 设置cookie名称和值
                .name(prop.getUser().getCookieName()).value(token)
                // 写cookie
                .build();
        }
        return payLoad.getUserInfo();
    } catch (Exception e) {
        log.error("用户信息认证失败",e);
        // 抛出异常，证明token无效，直接返回401
        throw new LyException(ExceptionEnum.UNAUTHORIZED);
    }
}
```

代码中有用到token的刷新周期：`getMinRefreshInterval()`，这个刷新周期也配置到配置文件中：

```
minRefreshInterval: 15
```

![1554879595447](assets/1554879595447.png)

然后在属性类中读取：

![1554879663215](assets/1554879663215.png)



## 1.6.注销登录

首页左上角，登录后除了显示用户名，还有一个注销登录按钮：

![1554627778912](assets/1554627778912.png)

点击这个按钮，该如何实现退出登录呢？

### 1.6.1.思路分析

回想下以前怎么实现的：

- 用户点击退出，发起请求到服务端
- 服务端删除用户session即可

我们现在登录是无状态的，也就没有session，那该怎么办呢？

有同学会想，太简单了，直接删除cookie就可以了。

别忘了，我们设置了httponly，JS无法操作cookie。因此，删除cookie也必须发起请求到服务端，由服务端来删除cookie。



那么，是不是删除了cookie，用户就完成了退出登录呢？

设想一下，删除了cookie，只是让用户在当前浏览器上的token删除了，但是这个**token依然是有效的**！这就是JWT的另外一个缺点了，无法控制TOKEN让其失效。如果用户提前备份了token，那么重新填写到cookie后，登录状态依然有效。

所以，我们**不仅仅要让浏览器端清除cookie，而且要让这个cookie中的token失效**！

### 1.6.2.失效token黑名单

怎样才能实现这样的效果呢？

大家肯定能想到很多办法，但是无论哪种思路，都绕不可一点：JWT的无法修改特性。因此**我们不能修改token来标记token无效，而是在服务端记录token状态**，于是就违背了无状态性的特性。

如果要记录每一个token状态，会造成极大的服务端压力，我提供一种思路，可以在轻量级数据量下，解决这个问题：

- 用户进行注销类型操作时（比如退出、修改密码），校验token有效性，并解析token信息
- 把token的id存入redis，并设置有效期为token的剩余有效期
- 校验用户登录状态的接口，除了要正常逻辑外，还必须判断token的id是否存在于redis
- 如果存在，则证明token无效；如果不存在，则证明有效

等于是在Redis中记录失效token的黑名单，黑名单的时间不用太长，最长也就是token的有效期：30分钟，因此服务端数据存储压力会减少。



### 1.6.3.代码实现

步骤梳理：

- 前端页面发起请求到服务端
- 服务端校验token是否有效，并解析token
- 将token的id存入redis，并设置有效期为token的剩余有效期
- 设置cookie有效期为0，即可删除cookie



这里要用到Redis，所以先在`ly-auth`引入Redis依赖和配置：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

在application.yml中编写配置：

```yaml
spring:
  redis:
    host: 127.0.0.1
```





#### 1）发起请求

点击页面，发现请求已经发出：

![1554630843747](assets/1554630843747.png) 

#### 2）业务接口

controller分析：

- 请求方式：POST
- 请求路径：/logout
- 请求参数：无，但要cookie中的token，因此需要request和response
- 返回值：无，但要删除cookie

```java
/**
     * 退出登录
     */
@PostMapping("logout")
public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response){
    authService.logout(request, response);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```

service代码：

```java
@Autowired
private StringRedisTemplate redisTemplate;

/**
     * 用户登出操作
     * @param request
     * @param response
     * @return
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try{
//        1、获取request中的token
            String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
//        2、获取token的id 和有效期
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), UserInfo.class);
//        获取tokenid
            String jti = payload.getId();
//        获取过期时间
            Date expiration = payload.getExpiration();
//        计算过期时间
            long timeout = expiration.getTime() - System.currentTimeMillis();
//        把jti 存入黑名单
            redisTemplate.opsForValue().set(jti,"1",timeout, TimeUnit.MILLISECONDS);
        }catch(Exception e){
            e.printStackTrace();
            log.info("用户登出操作出现异常！");
        }finally {
//        删除cookie
            CookieUtils.deleteCookie(prop.getUser().getCookieName(),prop.getUser().getCookieDomain(),response);
        }

    }
```



### 1.6.4.修改登录校验逻辑

登录校验不仅仅要看JWT是否有效，还要检查redis中是否已经存在

![1554631340775](assets/1554631340775.png)

完整代码：

```java
public UserInfo verifyUser(HttpServletRequest request, HttpServletResponse response) {
    try {
        // 读取cookie
        String token = CookieUtils.getCookieValue(request, prop.getUser().getCookieName());
        // 获取token信息
        Payload<UserInfo> payLoad = JwtUtils.getInfoFromToken(token, prop.getPublicKey(), UserInfo.class);
        // 获取token的id，校验黑名单
        String id = payLoad.getId();
        Boolean boo = redisTemplate.hasKey(id);
        if (boo != null && boo) {
            // 抛出异常，证明token无效，直接返回401
            throw new LyException(ExceptionEnum.UNAUTHORIZED);
        }
        // 获取过期时间
        Date expiration = payLoad.getExpiration();
        // 获取刷新时间
        DateTime refreshTime = new DateTime(expiration.getTime()).plusMinutes(prop.getUser().getMinRefreshInterval());
        // 判断是否已经过了刷新时间
        if (refreshTime.isBefore(System.currentTimeMillis())) {
            // 如果过了刷新时间，则生成新token
            token = JwtUtils.generateTokenExpireInMinutes(payLoad.getUserInfo(), prop.getPrivateKey(), prop.getUser().getExpire());
            // 写入cookie
            CookieUtils.newCookieBuilder()
                // response,用于写cookie
                .response(response)
                // 保证安全防止XSS攻击，不允许JS操作cookie
                .httpOnly(true)
                // 设置domain
                .domain(prop.getUser().getCookieDomain())
                // 设置cookie名称和值
                .name(prop.getUser().getCookieName()).value(token)
                // 写cookie
                .build();
        }
        return payLoad.getUserInfo();
    } catch (Exception e) {
        log.error("用户信息认证失败",e);
        // 抛出异常，证明token无效，直接返回401
        throw new LyException(ExceptionEnum.UNAUTHORIZED);
    }
}
```

# 2.网关的登录控制

我们实现了登录相关的几个功能，也就是给用户授权。接下来，用户访问我们的系统，我们还需要根据用户的身份，判断是否有权限访问微服务资源，就是鉴权。

大部分的微服务都必须做这样的权限判断，但是如果在每个微服务单独做权限控制，每个微服务上的权限代码就会有重复，如何更优雅的完成权限控制呢？

我们可以在整个服务的入口完成服务的权限控制，这样微服务中就无需再做了，如图：

![1554643791047](assets/1554643791047.png)



接下来，我们在Zuul编写拦截器，对用户的token进行校验，完成初步的权限判断。

## 2.1.流程分析

权限控制，一般有粗粒度、细粒度控制之分，但不管哪种，前提是用户必须先登录。知道访问者是谁，才能知道这个人具备怎样的权限，可以访问那些服务资源（也就是微服务接口）。

因此，权限控制的基本流程是这样：

- 1）获取用户的登录凭证jwt
- 2）解析jwt，获取用户身份
  - 如果解析失败，证明没有登录，返回401
  - 如果解析成功，继续向下
- 3）根据身份，查询用户权限信息
- 4）获取当前请求资源（微服务接口路径）
- 5）判断是否有访问资源的权限



一般权限信息会存储到数据库，会对应角色表和权限表：

- 角色：就是身份，例如普通用户，金钻用户，黑钻用户，商品管理员
- 权限：就是可访问的访问资源，如果是URL级别的权限控制，包含请求方式、请求路径、等信息

一个角色一般会有多个权限，一个权限也可以属于多个用户，属于多对多关系。根据角色可以查询到对应的所有权限，再根据权限判断是否可以访问当前资源即可。

在我们的功能中，因为还没有写权限功能，所以暂时只有一个角色，就是普通用户，可以访问的是商品及分类品牌等的查询功能，以及自己的信息。以后编写权限服务时，再补充相关业务。

## 2.2.加载公钥

权限控制的第一部分，就是获取cookie，并解析jwt，那么肯定需要公钥。我们在`ly-api-gateway`中配置公钥信息，并在服务启动时加载。

首先引入所需要的依赖：

```xml
<dependency>
    <groupId>com.leyou</groupId>
    <artifactId>ly-common</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

然后编写属性文件：

```yaml
ly:
  jwt:
    pubKeyPath: /Users/zhanghuyi/devlop/ssh/id_rsa.pub # C:/develop/ssh/id_rsa.pub # 公钥地址
    user:
      cookieName: LY_TOKEN # cookie名称
```

编写属性类，读取公钥：

```java
/**
 * @author 黑马程序员
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties implements InitializingBean {
    /**
     * 公钥地址
     */
    private String pubKeyPath;
    
    private PublicKey publicKey;
    /**
     * 用户token相关属性
     */
    private UserTokenProperties user = new UserTokenProperties();

    @Data
    public class UserTokenProperties {
        /**
         * 存放token的cookie名称
         */
        private String cookieName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 获取公钥和私钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            log.error("初始化公钥失败！", e);
            throw new RuntimeException(e);
        }
    }
}
```



## 2.3.编写过滤器逻辑

有了公钥，就可以编写权限控制逻辑了，权限验证通过，放行到微服务；不通过，则拦截并返回401给用户。因此拦截的逻辑需要在请求被路由之前执行，你能想到用什么实现吗？

没错，就是ZuulFilter。

ZuulFilter是Zuul的过滤器，其中pre类型的过滤器会在路由之前执行，刚好符合我们的需求。接下来，我们自定义pre类型的过滤器，并在过滤器中完成权限校验逻辑。



基本逻辑：

- 获取cookie中的token
- 通过JWT对token进行解析
  - 解析通过，继续权限校验
  - 解析不通过，返回401
- 根据用户身份获取权限信息
- 获取当前请求路径，判断权限
- 通过：则放行；不通过：则返回401

 ![1554647164661](assets/1554647164661.png)

```java
package com.leyou.gateway.filters;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 黑马程序员
 */
@Slf4j
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties jwtProp;

    @Autowired
    private FilterProperties filterProp;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.FORM_BODY_WRAPPER_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        // 获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest request = ctx.getRequest();
        // 获取token
        String token = CookieUtils.getCookieValue(request, jwtProp.getUser().getCookieName());
        // 校验
        try {
            // 解析token
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey(), UserInfo.class);
            // 解析没有问题，获取用户
            UserInfo user = payload.getUserInfo();
            // 获取用户角色，查询权限
            String role = user.getRole();
            // 获取当前资源路径
            String path = request.getRequestURI();
            String method = request.getMethod();
            // TODO 判断权限，此处暂时空置，等待权限服务完成后补充
            log.info("【网关】用户{},角色{}。访问服务{} : {}，", user.getUsername(), role, method, path);
        } catch (Exception e) {
            // 校验出现异常，返回403
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(403);
            log.error("非法访问，未登录，地址：{}", request.getRemoteHost(), e );
        }
        return null;
    }
}
```

登录状态时，访问商品查询接口：

![1554646907505](assets/1554646907505.png)

没有问题，可以访问。



退出登录，再次访问：

![1554646947375](assets/1554646947375.png)

证明拦截器生效了！

## 2.4.白名单

此时我们尝试再次登录：

![1554647020757](assets/1554647020757.png) 

登录接口也被拦截器拦截了！！！



要注意，并不是所有的路径我们都需要拦截，例如：

- 登录校验接口：`/auth/login`

- 注册接口：`/user/register`

  数据校验接口：`/user/check/`

- 发送验证码接口：`/user/code`

- 搜索接口：`/search/**`

另外，跟后台管理相关的接口，因为我们没有做登录和权限，因此暂时都放行，但是生产环境中要做登录校验：

- 后台商品服务：`/item/**`



所以，我们需要在拦截时，配置一个白名单，如果在名单内，则不进行拦截。

在`application.yaml`中添加规则：

```yaml
ly:
  filter:
    allowPaths:
      - /api/auth/login
      - /api/search
      - /api/user/register
      - /api/user/check
      - /api/user/code
      - /api/item
```

然后读取这些属性：

 ![1554647210954](assets/1554647210954.png)

内容：

```java
@ConfigurationProperties(prefix = "ly.filter")
public class FilterProperties {

    private List<String> allowPaths;

    public List<String> getAllowPaths() {
        return allowPaths;
    }

    public void setAllowPaths(List<String> allowPaths) {
        this.allowPaths = allowPaths;
    }
}
```

在过滤器中的`shouldFilter`方法中添加判断逻辑：

 ![1527558787803](assets/1527558787803.png)

代码：

```java
package com.leyou.gateway.filters;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.FilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 黑马程序员
 */
@Slf4j
@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties jwtProp;

    @Autowired
    private FilterProperties filterProp;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.FORM_BODY_WRAPPER_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        // 获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest req = ctx.getRequest();
        // 获取路径
        String requestURI = req.getRequestURI();
        // 判断白名单
        return !isAllowPath(requestURI);
    }

    private boolean isAllowPath(String requestURI) {
        // 定义一个标记
        boolean flag = false;
        // 遍历允许访问的路径
        for (String path : this.filterProp.getAllowPaths()) {
            // 然后判断是否是符合
            if(requestURI.startsWith(path)){
                flag = true;
                break;
            }
        }
        return flag;
    }

    @Override
    public Object run() throws ZuulException {
        // 获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 获取request
        HttpServletRequest request = ctx.getRequest();
        // 获取token
        String token = CookieUtils.getCookieValue(request, jwtProp.getUser().getCookieName());
        // 校验
        try {
            // 解析token
            Payload<UserInfo> payload = JwtUtils.getInfoFromToken(token, jwtProp.getPublicKey(), UserInfo.class);
            // 解析没有问题，获取用户
            UserInfo user = payload.getUserInfo();
            // 获取用户角色，查询权限
            String role = user.getRole();
            // 获取当前资源路径
            String path = request.getRequestURI();
            String method = request.getMethod();
            // TODO 判断权限，此处暂时空置，等待权限服务完成后补充
            log.info("【网关】用户{},角色{}。访问服务{} : {}，", user.getUsername(), role, method, path);
        } catch (Exception e) {
            // 校验出现异常，返回403
            ctx.setSendZuulResponse(false);
            ctx.setResponseStatusCode(403);
            log.error("非法访问，未登录，地址：{}", request.getRemoteHost(), e );
        }
        return null;
    }
}
```

# 3.面试常见问题

- 你们使用JWT做登录凭证，如何解决token注销问题

  答：jwt的缺陷是token生成后无法修改，因此无法让token失效。只能采用其它方案来弥补，基本思路如下：

  ```
  1）适当减短token有效期，让token尽快失效
  
  2）删除客户端cookie
  
  3）服务端对失效token进行标记，形成黑名单，虽然有违无状态特性，但是因为token有效期短，因此标记 	时间也比较短。服务器压力会比较小
  ```

- 既然token有效期短，怎么解决token失效后的续签问题？

  答：在验证用户登录状态的代码中，添加一段逻辑：判断cookie即将到期时，重新生成一个token。比如token有效期为30分钟，当用户请求我们时，我们可以判断如果用户的token有效期还剩下10分钟，那么就重新生成token。因此用户只要在操作我们的网站，就会续签token

- 如何解决异地登录问题？ 

  答：在我们的应用中是允许用户异地登录的。如果要禁止用户异地登录，只能采用有状态方式，在服务端记录登录用户的信息，并且判断用户已经登录，并且在其它设备再次登录时，禁止登录请求，并要求发送短信验证。

  一个账号只能在一个客户端登录

  每一台都有个唯一编码，登录的时候可以把这个唯一编码传给服务器，服务器需要采用有状态的方式，记录下token和这个唯一编码的关系。

  每次请求的时候，带上token和唯一编码，都会判断这个唯一编码

- 如何解决cookie被盗用问题？

  答：cookie被盗用的可能性主要包括下面几种：

  - XSS攻击：这个可以再前端页面渲染时对 数据做安全处理即可，而且我们的cookie使用了Httponly为true，可以防止JS脚本的攻击。
  - CSRF攻击：
    - 我们严格遵循了Rest风格，CSRF只能发起Get请求，不会对服务端造成损失，可以有效防止CSRF攻击
    - 利用Referer头，防盗链
  - 抓包，获取用户cookie：我们采用了HTTPS协议通信，无法获取请求的任何数据
  - 请求重放攻击：对于普通用户的请求没有对请求重放做防御，而是对部分业务做好了幂等处理。运行管理系统中会对token添加随机码，认证token一次有效，来预防请求重放攻击。
  - 用户电脑中毒：这个无法防范。控制自己

jWT 

cas 需要一个登陆鉴权的服务 实现好cas服务端

- 如何解决cookie被篡改问题？
  - 答：cookie可以篡改，但是签名无法篡改，否则服务端认证根本不会通过
- 如何完成权限校验的？
  - 首先我们有权限管理的服务，管理用户的各种权限，及可访问路径等
  - 在网关zuul中利用Pre过滤器，拦截一切请求，在过滤器中，解析jwt，获取用户身份，查询用户权限，判断用户身份可以访问当前路径

