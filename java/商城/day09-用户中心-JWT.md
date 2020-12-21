# 学习目标

- 实现数据校验功能
- 实现短信发送功能
- 实现根据用户名和密码查询用户功能
- 了解BCryptPasswordEncoder
- 了解Hibernate Validator
- 了解Swagger-UI
- 理解有状态和无状态
- 了解加密技术、RSA非对称加密
- 了解JWT原理

# 1.数据验证功能

## 1.1.接口说明：

### 接口路径

```
GET /check/{data}/{type}
```

### 参数说明：

| 参数 | 说明                                 | 是否必须 | 数据类型 | 默认值 |
| ---- | ------------------------------------ | -------- | -------- | ------ |
| data | 要校验的数据                         | 是       | String   | 无     |
| type | 要校验的数据类型：1，用户名；2，手机 | 是       | Integer  | 无     |

### 返回结果：

返回布尔类型结果：

- true：可用
- false：不可用

状态码：

- 200：校验成功
- 400：参数有误
- 500：服务器内部异常



## 1.2.controller

因为有了接口，我们可以不关心页面，所有需要的东西都一清二楚：

- 请求方式：GET
- 请求路径：/check/{param}/{type}
- 请求参数：param,type
- 返回结果：true或false

```java
package com.leyou.user.web;

import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 校验数据是否可用
     * @param data
     * @param type
     * @return
     */
    @GetMapping("/check/{data}/{type}")
    public ResponseEntity<Boolean> checkUserData(@PathVariable("data") String data, @PathVariable(value = "type") Integer type) {
        return ResponseEntity.ok(userService.checkData(data, type));
    }
}
```

## 1.3.Service

```java
package com.leyou.user.service;

@Service
public class TbUserServiceImpl extends ServiceImpl<TbUserMapper, TbUser> implements TbUserService {
    /**
     * 校验数据是否可用
     * @param data 用户输入的数据
     * @param type 1- 用户名  2 手机号
     * @return
     */
	@Override
    public Boolean checkData(String data, Integer type) {
       QueryWrapper<TbUser> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1:
                queryWrapper.lambda().eq(TbUser::getUsername,data);
                break;
            case 2:
                queryWrapper.lambda().eq(TbUser::getPhone,data);
                break;
            default:
                throw  new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }

        int count = this.count(queryWrapper);
        return count==0;
    }
}
```

## 1.4.测试

我们在数据库插入一条假数据：

 ![1527231417047](assets/1527231417047.png)

然后在浏览器调用接口，测试：

 ![1527231449211](assets/1527231449211.png)

 ![1527231475691](assets/1527231475691.png)



# 2.发送短信功能

短信微服务已经准备好，我们就可以继续编写用户中心接口了。

## 2.1.接口说明

![1527238127932](assets/1527238127932.png)



这里的业务逻辑是这样的：

- 1）我们接收页面发送来的手机号码
- 2）生成一个随机验证码
- 3）将验证码保存在服务端
- 4）发送短信，将验证码发送到用户手机



那么问题来了：验证码保存在哪里呢？

验证码有一定有效期，一般是5分钟，我们可以利用Redis的过期机制来保存。

## 2.2.controller

```java
/**
     * 发送短信验证码
     * @return
     */
@PostMapping("/code")
public ResponseEntity<Void> sendCode(@RequestParam("phone") String phone){
    userService.sendCode(phone);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```

## 2.3.service

这里的逻辑会稍微复杂：

- 生成随机验证码
- 将验证码保存到Redis中，用来在注册的时候验证
- 发送验证码到`ly-sms-service`服务，发送短信

因此，我们需要引入Redis和RocketMQ：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.0.2</version>
</dependency>
```

添加RocketMQ和Redis配置：

```yaml
spring:
  redis:
    host: 127.0.0.1
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: ${spring.application.name}
    send-message-timeout: 10000
```

Service代码：

```java
//redis中的key前缀
private static final String  KEY_PREFIX = "ly:user:verify:phone:";

@Autowired
private StringRedisTemplate redisTemplate;
@Autowired
private RocketMQTemplate rocketMQTemplate;

       /**
     * 发送短信验证码
     * @param phone
     * @return
     */
    @Override
    public void sendCode(String phone) {
//        判断手机号格式
        if(!RegexUtils.isPhone(phone)){
            throw new LyException(ExceptionEnum.INVALID_PHONE_NUMBER);
        }
//        判断这个手机号是否可以发送验证码
        if(!this.checkData(phone,2)){
            throw new LyException(ExceptionEnum.INVALID_PHONE_NUMBER);
        }
//        TODO 这个手机号发送次数
        String redisKey = PRE_FIX+phone;
        log.info("redisKey="+redisKey);
//        验证码短信不能频繁发送
        String cacheCode = redisTemplate.opsForValue().get(redisKey);
        if(!StringUtils.isBlank(cacheCode)){
            throw new LyException(ExceptionEnum.INVALID_PHONE_NUMBER);
        }
//        生成验证码,随机数
        String code = RandomStringUtils.randomNumeric(6);
//        存储短信验证码，redis中，使用string结构
//        key-ly:user:verify:phone:手机号 val-code,过期时间
        redisTemplate.opsForValue().set(redisKey,code,5, TimeUnit.MINUTES);
//        发送RocketMQ消息，消息内容是json字符串 {"phone":12312,"code":"12312323"}
        String dest = SMS_TOPIC_NAME +":"+ VERIFY_CODE_TAGS;
        Map<String,String> msg = new HashMap();
        msg.put("phone",phone);
        msg.put("code",code);
        log.info("发送短信的内容={}",msg);
        rocketMQTemplate.convertAndSend(dest, JsonUtils.toString(msg));
    }
```

注意：

- 手机号校验使用了ly-common中定义的正则工具类

- 要设置短信验证码在Redis的缓存有效时间



## 2.4.测试

通过RestClient发送请求试试：

 ![1527240486327](assets/1527240486327.png)

查看Redis中的数据：

 ![1527240610713](assets/1527240610713.png)

查看短信：

 ![1527240770470](assets/1527240770470.png)



# 3.注册功能

## 3.1.接口说明

![1527240855176](assets/1527240855176.png)



## 3.2.controller

```java
@PostMapping("/register")
public ResponseEntity<Void> register(User user, @RequestParam("code") String code){
    userService.register(user, code);
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

## 3.3.service

基本逻辑：

- 1）校验短信验证码
- 2）对密码加密
- 3）写入数据库

密码加密：

密码加密使用传统的MD5加密并不安全，这里我们使用的是Spring提供的BCryptPasswordEncoder加密算法，分成加密和验证两个过程：

- 加密：算法会对明文密码随机生成一个salt，使用salt结合密码来加密，得到最终的密文。

- 验证密码：需要先拿到加密后的密码和要验证的密码，根据已加密的密码来推测出salt，然后利用相同的算法和salt对要验证码的密码加密，与已加密的密码对比即可。

为了防止有人能根据密文推测出salt，我们需要在使用BCryptPasswordEncoder时配置随即密钥：

```java
package com.leyou.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;


@Data
@Configuration
@ConfigurationProperties(prefix = "ly.encoder.crypt")
public class PasswordConfig {

    private int strength;
    private String secret;

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        // 利用密钥生成随机安全码
        SecureRandom secureRandom = new SecureRandom(secret.getBytes());
        // 初始化BCryptPasswordEncoder
        return new BCryptPasswordEncoder(strength, secureRandom);
    }
}
```

在配置文件中配置属性：

```yaml
ly:
  encoder:
    crypt:
      secret: ${random.uuid} # 随机的密钥，使用uuid
      strength: 10 # 加密强度4~31，决定了密码和盐加密时的运算次数，超过10以后加密耗时会显著增加
```



代码：

```java
@Autowired
    private BCryptPasswordEncoder encoder;
    /**
     * 用户注册
     * @param tbUser
     * @param code
     * @return
     */
    @Override
    public void register(TbUser tbUser, String code) {
        if(StringUtils.isBlank(code)){
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }
//        校验用户,判断数据是否可用
        if(!this.checkData(tbUser.getPhone(),2)){
            throw new LyException(ExceptionEnum.INVALID_PHONE_NUMBER);
        }
        if(!this.checkData(tbUser.getUsername(),1)){
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
//        校验短信验证码，从redis中获取
        String cacheCode = redisTemplate.opsForValue().get(PRE_FIX + tbUser.getPhone());
        if(!StringUtils.equals(cacheCode,code)){
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }
//      用户密码加密
        String encodePwd = encoder.encode(tbUser.getPassword());
//        覆盖原始密码
        tbUser.setPassword(encodePwd);
//        把tbuser对象保存在数据
        boolean b = this.save(tbUser);
        if(!b){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }
```

## 3.4.测试

我们通过RestClient测试：

![1527241936160](assets/1527241936160.png)

查看数据库：

 ![1527241966575](assets/1527241966575.png)

## 🎗经验分享-获取配置报错

### 1.用户微服务密码加密功能实现

> yml文件增加配置属性

```properties
ly:
  encoder:
    crypt:
      secret: ${random.uuid} # 随机的密钥，使用uuid
      strength: 10 # 加密强度4~31，决定了密码和盐加密时的运算次数，超过10以后加密耗时会显著增加
```



> 配置BCryptPasswordEncoder加密类

```java
package com.leyou.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;

@Configuration
@ConfigurationProperties(prefix = "ly.encoder.crypt")
public class PasswordConfig {

    private int strength;
    private String secret;

    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        // 利用密钥生成随机安全码
        SecureRandom secureRandom = new SecureRandom(secret.getBytes());
        // 初始化BCryptPasswordEncoder
        return new BCryptPasswordEncoder(strength, secureRandom);
    }
}
```



> 注册时通过加密类将用户密码进行加密处理

```java
public void register(User user, String code) {
        try {
            //密码进行加密处理
            String encodePassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodePassword);
            //将用户新增到数据库
            userMapper.insert(user);
        } catch (LyException e) {
            e.printStackTrace();
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }

```



> 用户查询时通过加密类将用户输入密码和数据库密码进行验证

```java
public UserDTO queryUserByUsernameAndPassword(String username, String password) {

        //1.根据用户名查询数据库中用户对象
        User user = new User();
        user.setUsername(username);

        QueryWrapper<User> wrapper = new QueryWrapper<>(user);

        User userDB = userMapper.selectOne(wrapper);
        // 2判断是否存在
        if (userDB == null) {
            // 用户名错误
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }

        //3.校验密码 1.用户输入的密码  2.数据库中加密后的密码
        boolean matches = passwordEncoder.matches(password, userDB.getPassword());
        if(!matches){  //如果密码不一致，抛异常
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }

        return BeanHelper.copyProperties(userDB, UserDTO.class);
    }

```



### 2.出现的问题

程序运行出现错误：

![image-20200710210515334](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/image-20200710210515334.png)

### 3.问题的分析

> 还是紧盯控制台报的错误，PasswordConfig该类的20行有空值异常，也就是
>
> SecureRandom secureRandom = new SecureRandom(secret.getBytes());该代码出现问题

> 确定secret是否正常的获取到属性，这里考核属性注入。
>
> 像本次的Bug可以先通过补全@Data注解解决该问题



### 4.问题解决办法

针对于上述的错误，有两种可能会导致set值出现问题，一个是yml格式问题

> 问题解决思路：首先确定yml文件中的属性赋值，一定要注意缩进格式！！！



还有一种是因为缺少@Data 注解导致

> 问题解决思路：要注意提供的配置文件中是否正常获取到yml中属性并赋值给对应属性

```java
@Data				//注意1：生成getter/setter方法，属性注入的关键方法
@Configuration      //配置文件
@ConfigurationProperties("ly.encoder.crypt")  //注意2：指定配置文件此处的name以及配置文件中属性都是容易出错的地方


```



## 3.5.服务端数据校验

刚才虽然实现了注册，但是服务端并没有进行数据校验，而前端的校验是很容易被有心人绕过的。所以我们必须在后台添加数据校验功能：

我们这里会使用Hibernate-Validator框架完成数据校验：

而SpringBoot的web启动器中已经集成了相关依赖：

 ![1527244265451](assets/1527244265451.png)

### 3.5.1.什么是Hibernate Validator

Hibernate Validator是Hibernate提供的一个开源框架，使用注解方式非常方便的实现服务端的数据校验。

官网：http://hibernate.org/validator/

![1527244393041](assets/1527244393041.png)



**hibernate Validator** 是 Bean Validation 的参考实现 。

Hibernate Validator 提供了 JSR 303 规范中所有内置 constraint（约束） 的实现，除此之外还有一些附加的 constraint。

在日常开发中，Hibernate Validator经常用来验证bean的字段，基于注解，方便快捷高效。

### 3.5.2.Bean校验的注解

常用注解如下：

| **Constraint**                                     | **详细信息**                                                 |
| -------------------------------------------------- | ------------------------------------------------------------ |
| **@Valid**                                         | 被注释的元素是一个对象，需要检查此对象的所有字段值           |
| **@Null**                                          | 被注释的元素必须为 null                                      |
| **@NotNull**                                       | 被注释的元素必须不为 null                                    |
| **@AssertTrue**                                    | 被注释的元素必须为 true                                      |
| **@AssertFalse**                                   | 被注释的元素必须为 false                                     |
| **@Min(value)**                                    | 被注释的元素必须是一个数字，其值必须大于等于指定的最小值     |
| **@Max(value)**                                    | 被注释的元素必须是一个数字，其值必须小于等于指定的最大值     |
| **@DecimalMin(value)**                             | 被注释的元素必须是一个数字，其值必须大于等于指定的最小值     |
| **@DecimalMax(value)**                             | 被注释的元素必须是一个数字，其值必须小于等于指定的最大值     |
| **@Size(max,   min)**                              | 被注释的元素的大小必须在指定的范围内                         |
| **@Digits   (integer, fraction)**                  | 被注释的元素必须是一个数字，其值必须在可接受的范围内         |
| **@Past**                                          | 被注释的元素必须是一个过去的日期                             |
| **@Future**                                        | 被注释的元素必须是一个将来的日期                             |
| **@Pattern(value)**                                | 被注释的元素必须符合指定的正则表达式                         |
| **@Email**                                         | 被注释的元素必须是电子邮箱地址                               |
| **@Length**                                        | 被注释的字符串的大小必须在指定的范围内                       |
| **@NotEmpty**                                      | 被注释的字符串的必须非空                                     |
| **@Range**                                         | 被注释的元素必须在合适的范围内                               |
| **@NotBlank**                                      | 被注释的字符串的必须非空                                     |
| **@URL(protocol=,host=,   port=,regexp=, flags=)** | 被注释的字符串必须是一个有效的url                            |
| **@CreditCardNumber**                              | 被注释的字符串必须通过Luhn校验算法，银行卡，信用卡等号码一般都用Luhn计算合法性 |

### 3.5.3.给TbUser添加校验

我们在TbUser对象的部分属性上添加注解：

```java
package com.leyou.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.leyou.common.utils.RegexUtils;
import com.leyou.common.utils.constants.RegexPatterns;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author HM
 * @since 2020-02-07
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbUser extends Model<TbUser> {

private static final long serialVersionUID=1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @Length(min = 4,max = 32,message = "用户名必须为4-32位")
    private String username;

    /**
     * 密码，加密存储
     */
    @Length(min = 6,max = 18,message = "密码必须为6-18位")
    private String password;

    /**
     * 注册手机号
     */
    @Pattern(regexp = RegexPatterns.PHONE_REGEX,message = "手机号码格式不正确")
    private String phone;

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



### 3.5.4.在controller上进行控制

在controller中只需要给User添加 @Valid注解即可。

 ![1527247334410](assets/1527247334410.png)



### 3.5.5.测试

我们故意填错：

 ![1527247422251](assets/1527247422251.png)

然后SpringMVC会自动返回错误信息：

 ![1527247492172](assets/1527247492172.png)



如果需要自定义返回结果，可以这么写：

```java
 /**
     * 用户注册
     * 使用服务端验证数据框架
     * @param user
     * @param code
     * @return
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid TbUser user,
                                         BindingResult result,
                                         @RequestParam(name = "code")String code){
        if(result.hasErrors()){
            String errorsMsg = result.getAllErrors().stream().map(ObjectError::getDefaultMessage).collect(Collectors.joining(","));
            throw new LyException(400,errorsMsg);
        }
        userService.register(user,code);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
```

我们在User参数后面跟一个BindingResult参数，不管校验是否通过，都会进入方法内部。如何判断校验是否通过呢？

BindingResult中会封装错误结果，我们通过result.hashErrors来判断是否有错误，然后荣光result.getFieldErrors来获取错误信息。

再次测试：

![1554547576246](assets/1554547576246.png)

结果：

![1554547592656](assets/1554547592656.png)



# 4.根据用户名和密码查询用户

## 4.1.接口说明

### 功能说明

查询功能，根据参数中的用户名和密码查询指定用户

### 接口路径

```
GET /query
```

### 参数说明：

form表单格式

| 参数     | 说明                                     | 是否必须 | 数据类型 | 默认值 |
| -------- | ---------------------------------------- | -------- | -------- | ------ |
| username | 用户名，格式为4~30位字母、数字、下划线   | 是       | String   | 无     |
| password | 用户密码，格式为4~30位字母、数字、下划线 | 是       | String   | 无     |

### 返回结果：

用户的json格式数据

```json
{
    "id": 6572312,
    "username":"test",
    "phone":"13688886666",
}
```



状态码：

- 200：查询成功
- 400：用户名或密码错误
- 500：服务器内部异常，查询失败

这里要返回的结果与数据库字段不一致，需要在`ly-user-pojo`中定义一个dto：

```java
package com.leyou.user.dto;

import lombok.Data;


@Data
public class UserDTO {
    private Long id;
    private String username;
    private String phone;
}
```





## 4.2.controller

```java
/**
     * 根据用户名和密码查询用户
     * @param userName
     * @param passWord
     * @return
     */
    @GetMapping("/query")
    public ResponseEntity<UserDTO> queryUser(@RequestParam(name = "username")String userName,
                                             @RequestParam(name = "password")String passWord){
        return ResponseEntity.ok(userService.queryUser(userName,passWord));
    }
```

## 4.3.service

```java
@Override
    public UserDTO queryUser(String userName, String passWord) {

        QueryWrapper<TbUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TbUser::getUsername,userName);
        TbUser user = this.getOne(queryWrapper);
        if(user == null){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        boolean b = passwordEncoder.matches(passWord, user.getPassword());
        if(!b){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        return BeanHelper.copyProperties(user, UserDTO.class);
    }
```

要注意，查询时也要对密码进行加密后判断是否一致。

## 4.4.测试

我们通过RestClient测试：

 ![1554548239554](assets/1554548239554.png)



# 5.Swagger-UI 

完成了用户中心接口的开发，接下来我们就要测试自己的接口了，而且为了方便前段调用和参考，我们最好提供一份更直观的api文档，这里我们介绍一个工具，叫做swagger-ui

什么是swagger呢？swagger是对Open-API的一种实现。那么，什么是OpenAPI呢？

## 5.1.什么是OpenAPI

随着互联网技术的发展，现在的网站架构基本都由原来的后端渲染，变成了：前端渲染、前后端分离的形态，而且前端技术和后端技术在各自的道路上越走越远。  前端和后端的唯一联系，变成了API接口；API文档变成了前后端开发人员联系的纽带，变得越来越重要。

没有API文档工具之前，大家都是手写API文档的，在什么地方书写的都有，而且API文档没有统一规范和格式，每个公司都不一样。这无疑给开发带来了灾难。

OpenAPI规范（OpenAPI Specification 简称OAS）是Linux基金会的一个项目，试图通过定义一种用来描述API格式或API定义的语言，来规范RESTful服务开发过程。目前V3.0版本的OpenAPI规范已经发布并开源在github上 。

官网：https://github.com/OAI/OpenAPI-Specification

## 5.2.什么是swagger？



OpenAPI是一个编写API文档的规范，然而如果手动去编写OpenAPI规范的文档，是非常麻烦的。而Swagger就是一个实现了OpenAPI规范的工具集。

官网：https://swagger.io/

看官方的说明：



Swagger包含的工具集：

- **Swagger编辑器：** Swagger Editor允许您在浏览器中编辑YAML中的OpenAPI规范并实时预览文档。
- **Swagger UI：** Swagger UI是HTML，Javascript和CSS资产的集合，可以从符合OAS标准的API动态生成漂亮的文档。
- **Swagger Codegen：**允许根据OpenAPI规范自动生成API客户端库（SDK生成），服务器存根和文档。
- **Swagger Parser：**用于解析来自Java的OpenAPI定义的独立库
- **Swagger Core：**与Java相关的库，用于创建，使用和使用OpenAPI定义
- **Swagger Inspector（免费）：** API测试工具，可让您验证您的API并从现有API生成OpenAPI定义
- **SwaggerHub（免费和商业）：** API设计和文档，为使用OpenAPI的团队构建。

## 5.3.快速入门

SpringBoot已经集成了Swagger，使用简单注解即可生成swagger的API文档。

### 5.3.1.引入依赖

```xml
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.8.0</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.8.0</version>
</dependency>
```

### 5.3.2.编写配置

```java
package com.leyou.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .host("localhost:8085")
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.leyou.user.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("乐优商城用户中心")
                .description("乐优商城用户中心接口文档")
                .version("1.0")
                .build();
    }
}
```

### 5.3.3.启动测试

重启服务，访问：http://localhost:8085/swagger-ui.html

就能看到swagger-ui为我们提供的API页面了：

![1554549137117](assets/1554549137117.png)

可以看到我们编写的4个接口，任意点击一个，即可看到接口的详细信息：

![1554554238765](assets/1554554238765.png)

可以看到详细的接口声明，包括：

- 请求方式：
- 请求路径
- 请求参数
- 响应等信息

点击右上角的`try it out!`还可以测试接口：

![1554554464170](assets/1554554464170.png)

填写参数信息，点击execute，可以发起请求并测试：

![1554554533108](assets/1554554533108.png)



## 5.4.自定义接口说明

刚才的文档说明中，是swagge-ui根据接口自动生成，不够详细。如果有需要，可以通过注解自定义接口声明。常用的注解包括：

```java
/**
 @Api：修饰整个类，描述Controller的作用
 @ApiOperation：描述一个类的一个方法，或者说一个接口
 @ApiParam：单个参数描述
 @ApiModel：用对象来接收参数
 @ApiProperty：用对象接收参数时，描述对象的一个字段
 @ApiResponse：HTTP响应其中1个描述
 @ApiResponses：HTTP响应整体描述
 @ApiIgnore：使用该注解忽略这个API
 @ApiError ：发生错误返回的信息
 @ApiImplicitParam：一个请求参数
 @ApiImplicitParams：多个请求参数
 */
```

示例：

```java
/**
     * 校验数据是否可用
     * @param data
     * @param type
     * @return
     */
@GetMapping("/check/{data}/{type}")
@ApiOperation(value = "校验用户名数据是否可用，如果不存在则可用")
@ApiResponses({
    @ApiResponse(code = 200, message = "校验结果有效，true或false代表可用或不可用"),
    @ApiResponse(code = 400, message = "请求参数有误，比如type不是指定值")
})
public ResponseEntity<Boolean> checkUserData(
    @ApiParam(value = "要校验的数据", example = "lisi") @PathVariable("data") String data,
    @ApiParam(value = "数据类型，1：用户名，2：手机号", example = "1") @PathVariable(value = "type") Integer type) {
    return ResponseEntity.ok(userService.checkData(data, type));
}
```

查看文档：

![1554555057087](assets/1554555057087.png)

# 6.无状态登录原理

## 6.1.什么是有状态？

有状态服务，即服务端需要记录每次会话的客户端信息，从而识别客户端身份，根据用户身份进行请求的处理，典型的设计如tomcat中的session。

例如登录：用户登录后，我们把登录者的信息保存在服务端session中，并且给用户一个cookie值，记录对应的session。然后下次请求，用户携带cookie值来，我们就能识别到对应session，从而找到用户的信息。

缺点是什么？

- 服务端保存大量数据，增加服务端压力
- 服务端保存用户状态，无法进行水平扩展
- 客户端请求依赖服务端，多次请求必须访问同一台服务器



## 6.2.什么是无状态

微服务集群中的每个服务，对外提供的都是Rest风格的接口。而Rest风格的一个最重要的规范就是：服务的无状态性，即：

- 服务端不保存任何客户端请求者信息
- 客户端的每次请求必须具备自描述信息，通过这些信息识别客户端身份

带来的好处是什么呢？

- 客户端请求不依赖服务端的信息，任何多次请求不需要必须访问到同一台服务
- 服务端的集群和状态对客户端透明
- 服务端可以任意的迁移和伸缩
- 减小服务端存储压力

## 6.3.如何实现无状态

无状态登录的流程：

- 当客户端第一次请求服务时，服务端对用户进行信息认证（登录）
- 认证通过，将用户信息进行加密形成token，返回给客户端，作为登录凭证
- 以后每次请求，客户端都携带认证的token
- 服务的对token进行解密，判断是否有效。

流程图：

 	![1527300483893](/assets/1527300483893.png)



整个登录过程中，最关键的点是什么？

**token的安全性**

token是识别客户端身份的唯一标示，如果加密不够严密，被人伪造那就完蛋了。

采用何种方式加密才是安全可靠的呢？

我们将采用`JWT + RSA非对称加密`

# 7.加密技术

## 7.1.加密技术分类

加密技术是对信息进行编码和解码的技术，编码是把原来可读信息（又称明文）译成代码形式（又称密文），其逆过程就是解码（解密），加密技术的要点是加密算法，加密算法可以分为三类：  

- 对称加密，如AES
  - 基本原理：将明文分成N个组，然后使用密钥对各个组进行加密，形成各自的密文，最后把所有的分组密文进行合并，形成最终的密文。
  - 优势：算法公开、计算量小、加密速度快、加密效率高
  - 缺陷：双方都使用同样密钥，安全性得不到保证 
- 非对称加密，如RSA
  - 基本原理：同时生成两把密钥：私钥和公钥，私钥隐秘保存，公钥可以下发给信任客户端
    - 私钥加密
    - 公钥解密
  - 优点：安全，难以破解
  - 缺点：算法比较耗时
- 不可逆加密，如MD5，SHA
  - 基本原理：加密过程中不需要使用[密钥](https://baike.baidu.com/item/%E5%AF%86%E9%92%A5)，输入明文后由系统直接经过加密算法处理成密文，这种加密后的数据是无法被解密的，无法根据密文推算出明文。



RSA算法历史：

1977年，三位数学家Rivest、Shamir 和 Adleman 设计了一种算法，可以实现非对称加密。这种算法用他们三个人的名字缩写：RSA

## 7.2.RSA工具类

RSA工具类负责对RSA密钥的创建、读取功能：

```java
package com.leyou.common.auth.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


public class RsaUtils {

    private static final int DEFAULT_KEY_SIZE = 2048;
    /**
     * 从文件中读取公钥
     *
     * @param filename 公钥保存路径，相对于classpath
     * @return 公钥对象
     * @throws Exception
     */
    public static PublicKey getPublicKey(String filename) throws Exception {
        byte[] bytes = readFile(filename);
        return getPublicKey(bytes);
    }

    /**
     * 从文件中读取密钥
     *
     * @param filename 私钥保存路径，相对于classpath
     * @return 私钥对象
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(String filename) throws Exception {
        byte[] bytes = readFile(filename);
        return getPrivateKey(bytes);
    }

    /**
     * 获取公钥
     *
     * @param bytes 公钥的字节形式
     * @return
     * @throws Exception
     */
    private static PublicKey getPublicKey(byte[] bytes) throws Exception {
        bytes = Base64.getDecoder().decode(bytes);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    /**
     * 获取密钥
     *
     * @param bytes 私钥的字节形式
     * @return
     * @throws Exception
     */
    private static PrivateKey getPrivateKey(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        bytes = Base64.getDecoder().decode(bytes);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    /**
     * 根据密文，生存rsa公钥和私钥,并写入指定文件
     *
     * @param publicKeyFilename  公钥文件路径
     * @param privateKeyFilename 私钥文件路径
     * @param secret             生成密钥的密文
     */
    public static void generateKey(String publicKeyFilename, String privateKeyFilename, String secret, int keySize) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(secret.getBytes());
        keyPairGenerator.initialize(Math.max(keySize, DEFAULT_KEY_SIZE), secureRandom);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        // 获取公钥并写出
        byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
        publicKeyBytes = Base64.getEncoder().encode(publicKeyBytes);
        writeFile(publicKeyFilename, publicKeyBytes);
        // 获取私钥并写出
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        privateKeyBytes = Base64.getEncoder().encode(privateKeyBytes);
        writeFile(privateKeyFilename, privateKeyBytes);
    }

    private static byte[] readFile(String fileName) throws Exception {
        return Files.readAllBytes(new File(fileName).toPath());
    }

    private static void writeFile(String destPath, byte[] bytes) throws IOException {
        File dest = new File(destPath);
        if (!dest.exists()) {
            dest.createNewFile();
        }
        Files.write(dest.toPath(), bytes);
    }
}
```

## 7.3测试生成公钥和私钥

```java
package com.leyou.common;

import com.leyou.common.auth.utils.RsaUtils;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;


public class AuthTest {

    private String privateFilePath = "C:\\develop\\ssh\\id_rsa";
    private String publicFilePath = "C:\\develop\\ssh\\id_rsa.pub";

    @Test
    public void testRSA() throws Exception {
        // 生成密钥对
        RsaUtils.generateKey(publicFilePath, privateFilePath, "hello", 2048);

        // 获取私钥
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateFilePath);
        System.out.println("privateKey = " + privateKey);
        // 获取公钥
        PublicKey publicKey = RsaUtils.getPublicKey(publicFilePath);
        System.out.println("publicKey = " + publicKey);
    }
}
```



# 8.JWT入门

## 8.1.简介

JWT，全称是Json Web Token， 是JSON风格轻量级的授权和身份认证规范，可实现无状态、分布式的Web应用授权；官网：https://jwt.io

![1527301027008](assets/1527301027008.png)

GitHub上jwt的java客户端：https://github.com/jwtk/jjwt



## 8.2.数据格式

JWT包含三部分数据：

- Header：头部，通常头部有两部分信息：

  - 声明类型，这里是JWT
  - 签名算法，自定义

  我们会对头部进行base64加密（可解密），得到第一部分数据

- Payload：载荷，就是有效数据，一般包含下面信息：

  - 用户身份信息（注意，这里因为采用base64加密，可解密，因此不要存放敏感信息）
  - tokenID：当前这个JWT的唯一标示
  - 注册声明：如token的签发时间，过期时间，签发人等

  这部分也会采用base64加密，得到第二部分数据

- Signature：签名，是整个数据的认证信息。一般根据前两步的数据，再加上服务的的密钥（secret）（不要泄漏，最好周期性更换），通过加密算法生成。用于验证整个数据完整和可靠性

生成的数据格式：

![1527322512370](/assets/1527322512370.png)

可以看到分为3段，每段就是上面的一部分数据



## 8.3.JWT交互流程

流程图：

![1554558044477](/assets/1554558044477.png)

- 授权流程：
  - 1、用户请求登录，携带用户名密码到授权中心
  - 2、授权中心携带用户名密码，到用户中心查询用户
  - 3、查询如果正确，生成JWT凭证
  - 4、返回JWT给用户
- 鉴权流程：
  - 1、用户请求某微服务功能，携带JWT
  - 2、微服务将jwt交给授权中心校验
  - 3、授权中心返回校验结果到微服务
  - 4、微服务判断校验结果，成功或失败
  - 5、失败则直接返回401
  - 6、成功则处理业务并返回



因为JWT签发的token中已经包含了用户的身份信息，并且每次请求都会携带，这样服务的就无需保存用户信息，甚至无需去数据库查询，完全符合了Rest的无状态规范。



不过，这个过程是不是就完美了呢？

可以发现，用户访问我们的网站，一次授权后，以后访问微服务都需要鉴权，那么**每次鉴权都需要访问授权中心**，一个用户请求，被分解为2次请求才能完成，效率比较低。

能不能直接在微服务的完成鉴权，不去找授权中心呢？

如果这样，就可以减少一次网络请求，效率提高了一倍。但是，**`微服务并没有鉴定JWT的能力`**，因为鉴定需要通过密钥来完成。我们不能把密钥交给其它微服务，存在安全风险。

怎么办？

这就要用到RSA非对称加密技术了。

## 8.4.使用非对称加密签名和验签

有了非对称加密，我们就可以改变签名和验签的方式了：

- 生成RSA密钥对，私钥存放在授权中心，公钥下发给微服务
- 在授权中心利用私钥对JWT签名
- 在微服务利用公钥验证签名有效性

因为非对称加密的特性，不用担心公钥泄漏问题，因为公钥是无法伪造签名的，但要**确保私钥的安全和隐秘**。

非对称加密后的授权和鉴权流程：

![1554559241052](assets/1554559241052.png)

鉴权部分简化了非常多：

![1554558999402](assets/1554558999402.png) 

用户只需要与微服务交互，不用访问授权中心，效率大大提高！

接下来让我们撸起袖子，开始写代码吧！

# 9.JWT工具

因为生成jwt，解析jwt这样的行为以后在其它微服务中也会用到，因此我们会抽取成工具，放到`ly-comon`中。

![1554611761743](assets/1554611761743.png) 

## 9.1.JWT工具类

### 9.1.1依赖

我们需要先在`ly-common`中引入JWT依赖：

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.10.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.10.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.10.5</version>
    <scope>runtime</scope>
</dependency>
```

### 9.1.2.载荷对象

JWT中，会保存载荷数据，我们计划存储3部分：

- id：jwt的id
- 用户信息：用户数据，不确定，可以是任意类型
- 过期时间：Date

为了方便后期获取，我们定义一个类来封装：

```java
package com.leyou.common.auth.entity;

import lombok.Data;

import java.util.Date;

@Data
public class Payload<T> {
    private String id;
    private T userInfo;
    private Date expiration;
}
```



### 9.1.3.工具

```java
package com.leyou.common.auth.utils;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.utils.JsonUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.joda.time.DateTime;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;


public class JwtUtils {

    private static final String JWT_PAYLOAD_USER_KEY = "user";

    /**
     * 私钥加密token
     *
     * @param userInfo   载荷中的数据
     * @param privateKey 私钥
     * @param expire     过期时间，单位分钟
     * @return JWT
     */
    public static String generateTokenExpireInMinutes(Object userInfo, PrivateKey privateKey, int expire) {
        return Jwts.builder()
                .claim(JWT_PAYLOAD_USER_KEY, JsonUtils.toString(userInfo))
                .setId(createJTI())
                .setExpiration(DateTime.now().plusMinutes(expire).toDate())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * 私钥加密token
     *
     * @param userInfo   载荷中的数据
     * @param privateKey 私钥
     * @param expire     过期时间，单位秒
     * @return JWT
     */
    public static String generateTokenExpireInSeconds(Object userInfo, PrivateKey privateKey, int expire) {
        return Jwts.builder()
                .claim(JWT_PAYLOAD_USER_KEY, JsonUtils.toString(userInfo))
                .setId(createJTI())
                .setExpiration(DateTime.now().plusSeconds(expire).toDate())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * 公钥解析token
     *
     * @param token     用户请求中的token
     * @param publicKey 公钥
     * @return Jws<Claims>
     */
    private static Jws<Claims> parserToken(String token, PublicKey publicKey) {
        return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token);
    }

    private static String createJTI() {
        return new String(Base64.getEncoder().encode(UUID.randomUUID().toString().getBytes()));
    }

    /**
     * 获取token中的用户信息
     *
     * @param token     用户请求中的令牌
     * @param publicKey 公钥
     * @return 用户信息
     */
    public static <T> Payload<T> getInfoFromToken(String token, PublicKey publicKey, Class<T> userType) {
        Jws<Claims> claimsJws = parserToken(token, publicKey);
        Claims body = claimsJws.getBody();
        Payload<T> claims = new Payload<>();
        claims.setId(body.getId());
        claims.setUserInfo(JsonUtils.toBean(body.get(JWT_PAYLOAD_USER_KEY).toString(), userType));
        claims.setExpiration(body.getExpiration());
        return claims;
    }

    /**
     * 获取token中的载荷信息
     *
     * @param token     用户请求中的令牌
     * @param publicKey 公钥
     * @return 用户信息
     */
    public static <T> Payload<T> getInfoFromToken(String token, PublicKey publicKey) {
        Jws<Claims> claimsJws = parserToken(token, publicKey);
        Claims body = claimsJws.getBody();
        Payload<T> claims = new Payload<>();
        claims.setId(body.getId());
        claims.setExpiration(body.getExpiration());
        return claims;
    }
}
```

## 9.2.测试

### 9.2.1.用户信息

这里我们假设用户信息包含3部分：

- id：用户id
- username：用户名
- role：角色（权限中会使用）

载荷:UserInfo

```java
package com.leyou.common.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private Long id;

    private String username;
    
    private String role;
}
```



### 9.2.2.测试类

我们在`ly-common`中编写测试类：

![1554612391707](assets/1554612391707.png) 

```java
package com.leyou.common;

import com.leyou.common.auth.entity.Payload;
import com.leyou.common.auth.entity.UserInfo;
import com.leyou.common.auth.utils.JwtUtils;
import com.leyou.common.auth.utils.RsaUtils;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;


public class AuthTest {

    private String privateFilePath = "C:\\develop\\ssh\\id_rsa";
    private String publicFilePath = "C:\\develop\\ssh\\id_rsa.pub";

    @Test
    public void testRSA() throws Exception {
        // 生成密钥对
        RsaUtils.generateKey(publicFilePath, privateFilePath, "hello", 2048);

        // 获取私钥
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateFilePath);
        System.out.println("privateKey = " + privateKey);
        // 获取公钥
        PublicKey publicKey = RsaUtils.getPublicKey(publicFilePath);
        System.out.println("publicKey = " + publicKey);
    }

    @Test
    public void testJWT() throws Exception {
        // 获取私钥
        PrivateKey privateKey = RsaUtils.getPrivateKey(privateFilePath);
        // 生成token
        String token = JwtUtils.generateTokenExpireInMinutes(new UserInfo(1L, "Jack", "guest"), privateKey, 5);
        System.out.println("token = " + token);

        // 获取公钥
        PublicKey publicKey = RsaUtils.getPublicKey(publicFilePath);
        // 解析token
        Payload<UserInfo> info = JwtUtils.getInfoFromToken(token, publicKey, UserInfo.class);

        System.out.println("info.getExpiration() = " + info.getExpiration());
        System.out.println("info.getUserInfo() = " + info.getUserInfo());
        System.out.println("info.getId() = " + info.getId());
    }
}
```



### 9.2.3.测试生成公钥和私钥

我们运行`testRSA()`，然后到指定的目录中查看：

![1554612495009](assets/1554612495009.png)

 打开公钥看看：

![1554612523179](assets/1554612523179.png)

公钥和私钥已经生成了！



### 9.2.4.测试生成token

运行`testJWT()`方法，查看控制台：

```
token = eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoie1wiaWRcIjoxLFwidXNlcm5hbWVcIjpcIkphY2tcIixcInJvbGVcIjpcImd1ZXN0XCJ9IiwianRpIjoiTkRnMlpUUXhaall0TUdNMFl5MDBNREU0TFdGaFpUWXRZVEUzT1Rjelpqa3hOVFEzIiwiZXhwIjoxNTU0NjEwNTMxfQ.FH_b4uBqgEYBTfLFaTnFNTrKNrm4n8e6clvBr1FiVMZirEinpjJdWUZc8NDNuJdSVA_FXd3G0aPAYgbTqPUXQ0QF3DC6BWB05lbXC2KGeJKHaKUSVw1KdIC2xjg5gOv-5QohjjVgXDRg3_p_s6zZeU6IMoao-6L5dZdYt4j60QP-4fp8uKn40HAiWh7KtKTQGbVn6w0sJNV17r2V5vmm1NplDUCJkDbfL7cEAkrszauB6qGEiw_vPe7sDydYAPUvIWkz85pJIUUs1ZbcZj4uw6xDjpiXKen3Xu8erV30buCFuJPbxg3pSHl5f-mvjyY7zF90TQOkA-Co580tlpUOhQ

info.getExpiration() = Sun Apr 07 12:15:31 CST 2019
info.getUserInfo() = UserInfo(id=1, username=Jack, role=guest)
info.getId() = NDg2ZTQxZjYtMGM0Yy00MDE4LWFhZTYtYTE3OTczZjkxNTQ3

Process finished with exit code 0
```

