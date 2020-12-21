# 学习目标

- 了解乐优商城基本内容
- 了解乐优项目基本结构
- 独立搭建后台管理系统
- 了解@ControllerAdvice注解，了解通用异常处理方式
- 完成nginx配置反向代理

# 1.了解电商行业

学习电商项目，自然要先了解这个行业，所以我们首先来聊聊电商行业

## 1.1.项目分类

主要从需求方、盈利模式、技术侧重点这三个方面来看它们的不同

### 1.1.1.传统项目

各种企业里面用的管理系统（ERP、HR、OA、CRM、物流管理系统。。。。。。。）

- 需求方：公司、企业内部
- 盈利模式：项目本身卖钱
- 技术侧重点：业务功能

### 1.1.2.互联网项目

门户网站、电商网站：baidu.com、qq.com、taobao.com、jd.com  ...... 

- 需求方：广大用户群体
- 盈利模式：虚拟币、增值服务、广告收益......
- 技术侧重点：网站性能、业务功能

而我们今天要聊的就是互联网项目中的重要角色：**电商**。



## 1.2.电商行业的特点

### 1.2.1.谈谈双十一

双十一是电商行业的一个典型代表，从里面我们能看出电商的普遍特点，2019年双11阿里的数据如下：

![1575336556266](assets/1575336556266.png)



2019年11月11日零点刚过96秒，天猫双11成交总额突破100亿元，而订单创建峰值创下新的世界纪录，达到54.4万笔/秒，是2009年第一届双11的1360倍。

![1575338259060](assets/1575338259060.png)

### 1.2.2.技术特点

从上面的数据我们不仅要看到钱，更要看到背后的技术实力。正是得益于电商行业的高强度并发压力，促使了BAT等巨头们的技术进步。电商行业有些什么特点呢？

- 技术范围广:一个电商公司，使用的技术很多，语言、开发、测试、运维、前端
- 技术新 ：vue 、运维、spring cloud
- 要求双高：
  - 高并发（分布式、静态化技术（html，nginx）、CDN服务（内容分发网络，有专门的运营商）、缓存技术（本地缓存、redis）、异步(多线程、MQ)，并发、池化、队列Queue）
  - 高可用（集群、负载均衡（ribbon、nginx）、限流（保护服务的提供方）、降级、熔断（保护服务的调用方）） 性能不够，机器凑
- 数据量大，用户量大、业务数据量大、日志数据量大
- 业务复杂，



## 1.3.常见电商模式

电商行业的一些常见模式：

- B2C：商家对个人，如：亚马逊、当当等
- C2C平台：个人对个人，如：闲鱼、拍拍网、ebay
- B2B平台：商家对商家，如：阿里巴巴、八方资源网等
- O2O：线上和线下结合，如：饿了么、电影票、团购等
- P2P：在线金融，贷款，如：网贷之家、人人聚财等。
- B2C平台：天猫、京东、一号店等



# 2.乐优商城介绍

## 2.1.项目介绍

- 乐优商城是一个全品类的电商购物网站（B2C）。 
- 用户可以在线购买商品、加入购物车、下单、秒杀商品
- 管理员可以在后台管理商品的上下架、促销活动
- 管理员可以监控商品销售状况
- 客服可以在后台处理退款操作
- 希望未来3到5年可以支持千万用户的使用



## 2.2.系统架构

### 2.2.1.架构图

乐优商城架构缩略图，大图请参考课前资料：

![lysc](assets/lysc.png)



### 2.2.2.系统架构解读

> #### 前端页面

整个乐优商城从用户角度来看，可以分为两部分：后台管理、前台门户。

- 后台管理：

  - 后台系统主要包含以下功能：
    - 商品管理，包括商品分类、品牌、商品规格等信息的管理
    - 销售管理，包括订单统计、订单退款处理、促销活动生成等
    - 用户管理，包括用户控制、冻结、解锁等
    - 权限管理，整个网站的权限控制，采用JWT鉴权方案，对用户及API进行权限控制
    - 统计，各种数据的统计分析展示
    - ...
  - 后台系统会采用前后端分离开发，而且整个后台管理系统会使用Vue.js框架搭建出单页应用（SPA）。
  - 预览图：

  ![1528098418861](assets/1528098418861.png)

- 前台门户

  - 前台门户面向的是客户，包含与客户交互的一切功能。例如：
    - 搜索商品
    - 加入购物车
    - 下单
    - 评价商品等等
  - 前台系统我们会使用Nuxt(服务端渲染)结合Vue完成页面开发。出于SEO优化的考虑，我们将不采用单页应用。

  ![1525704277126](assets/1525704277126.png)



无论是前台门户、还是后台管理页面，都是前端页面，我们的系统采用前后端分离方式，因此前端会独立部署，不会在后端服务出现静态资源。



> #### 后端微服务

无论是前台还是后台系统，都共享相同的微服务集群，包括：

- 商品微服务：商品及商品分类、品牌、库存等的服务
- 搜索微服务：实现搜索功能
- 订单微服务：实现订单相关
- 购物车微服务：实现购物车相关功能
- 用户服务：用户的登录注册、用户信息管理等功能
- 短信服务：完成各种短信的发送任务
- 支付服务：对接各大支付平台
- 授权服务：完成对用户的授权、鉴权等功能
- Eureka注册中心
- Zuul网关服务
- ...



# 3.商城管理系统 前端页面(了解)

我们的后台管理系统采用前后端分离开发，而且整个后台管理系统会使用Vue.js框架搭建出单页应用（SPA）。

前端技术：

- 基础的HTML、CSS、JavaScript（基于ES6标准）
- JQuery
- Vue.js 2.6
- 基于Vue的UI框架：Vuetify
- 前端构建工具：WebPack，项目编译、打包工具
- 前端安装包工具：NPM
- Vue脚手架：Vue-cli  springBoot
- Vue路由：vue-router
- ajax框架：axios 
- 基于Vue的富文本框架：quill-editor

## 3.1.什么是SPA

SPA，并不是去洗澡按摩，而是Single Page Application，即单页应用。整个后台管理系统只会出现一个HTML页面，剩余一切页面的内容都是通过Vue组件来实现。

我们的后台管理系统就是一个基于Vue的SPA的模式，其中的UI交互式通过一个名为Vuetify的框架来完成的。

## 3.2.Vuetify框架

Vuetify是一个基于Vue的UI框架，可以利用预定义的页面组件快速构建页面。有点类似BootStrap框架。

### 3.2.1.为什么要学习UI框架

Vue负责的是虽然会帮我们进行视图的渲染，但是样式是有我们自己来完成。这显然不是我们的强项，因此后端开发人员一般都喜欢使用一些现成的UI组件，拿来即用，常见的例如：

- BootStrap
- LayUI
- EasyUI
- ZUI

然而这些UI组件的基因天生与Vue不合，因为他们更多的是利用DOM操作，借助于jQuery实现，而不是MVVM的思想。

而目前与Vue吻合的UI框架也非常的多，国内比较知名的如：

- element-ui：饿了么出品
- i-view：某公司出品

然而我们都不用，我们今天推荐的是一款国外的框架：Vuetify

官方网站：https://vuetifyjs.com/zh-Hans/

![1525960652724](assets/1525960652724.png)





### 3.2.2.为什么是Vuetify

有中国的为什么还要用外国的？原因如下：

- Vuetify几乎不需要任何CSS代码，而element-ui许多布局样式需要我们来编写
- Vuetify从底层构建起来的语义化组件。简单易学，容易记住。
- Vuetify基于Material Design（谷歌推出的多平台设计规范），更加美观，动画效果酷炫，且风格统一

这是官网的说明：

![1525959769978](assets/1525959769978.png)

缺陷：

- 目前官网虽然有中文文档，但因为翻译问题，几乎不太能看。



### 3.2.3.怎么用？

基于官方网站的文档进行学习：

![1525960312939](assets/1525960312939.png)



我们重点关注`UI components`即可，里面有大量的UI组件，我们要用的时候再查看，不用现在学习，先看下有什么：

 ![1525961862771](assets/1525961862771.png)



 ![1525961875288](assets/1525961875288.png)

以后用到什么组件，就来查询即可。

## 3.3.后台管理页面

我们的后台管理系统就是基于Vue，并使用Vuetify的UI组件来编写的。

### 3.3.1.导入已有资源

后台项目相对复杂，为了有利于教学，我们不再从0搭建项目，而是直接使用课前资料中给大家准备好的源码：

 ![1534066112302](assets/1534066112302.png)

我们解压缩，放到工作目录中：

 ![1525955615381](assets/1525955615381.png)



然后在IDE中导入新的工程：

 ![1525955644681](assets/1525955644681.png)

选中我们的工程：

 ![1525955709334](assets/1525955709334.png)



这正是一个用vue-cli构建的单页应用：

 ![1556181876599](assets/1556181876599.png) 



### 3.3.2.运行一下看看

输入命令：

```
npm run serve
```

 ![1525957604219](assets/1525957604219.png)

发现默认的端口是9001。访问：

> http://localhost:9001

会自动进行跳转：

![1525958950616](assets/1525958950616.png)



### 3.3.3.项目结构

开始编码前，我们先了解下项目的结构：

#### 目录结构

首先是目录结构图：

![1556181958076](assets/1556181958076.png) 

可以看到，整个项目除了一个`index.html`外没有任何的静态页面。页面的切换都是靠一个个的vue组件的切换来完成的。这里的Vue组件称为单文件组件，就是后缀名为`.vue`的文件。

#### 单文件组件

`.vue`文件是vue组件的特殊形式，在以前我们定义一个Vue组件是这样来写的：

```js
const com = {
    template:`
		<div style="background-ground-color:red">
			<h1>hello ...</h1>
		</div>
	`,
    data(){
        return {
            
        }
    },
    methods:{
        
    }
}
Vue.component("com", com);
```

这种定义方式虽然可以实现，但是html、css、js代码混合在一起，而且在JS中编写html和css显然不够优雅。

而`.vue`文件是把三者做了分离：

![1552921477484](assets/1552921477484.png)



#### 页面菜单项

页面的左侧是我们的导航菜单：

![1552921510318](assets/1552921510318.png) 

这个菜单的文字信息，在项目的src目录下，有一个menu.js文件，是页面的左侧菜单目录：

 ![1552921550434](assets/1552921550434.png)

内容：

 ![1534066700557](assets/1534066700557.png)

#### 菜单的组件

每一个菜单都对应一个组件，这些组件在项目的`src/views/`目录下：

 ![1552921610875](assets/1552921610875.png)

那么，菜单是如何与页面组件对应的呢？src目录下有一个router.js，里面保存了页面路径与页面组件的对应关系：

 ![1552921640520](assets/1552921640520.png)

核心部分代码：

![1552921721804](assets/1552921721804.png)

当我们点击对应按钮，对应的组件就会展示在页面。



# 4.搭建基础服务

先准备后台微服务集群的基本架构

## 4.1.技术选型

后端技术：

- 基础的SpringMVC、Spring 5.0和MyBatisPlus
- Spring Boot 2.1.3版本
- Spring Cloud 版本 Greenwich.Release
- Redis-4.0
- RabbitMQ / RocketMQ
- Elasticsearch-6.2.4
- nginx-反向代理
- Thymeleaf   模板语言     生成静态页
- JWT    -- 做鉴权

## 4.2.开发环境

- IDE：我们使用Idea 2019.1 版本
- JDK：统一使用JDK1.8.151
- 项目构建：maven3.5.x以上版本即可
- MySQl    5.7以上版本

## 4.3.域名

我们在开发的过程中，为了保证以后的生产、测试环境统一。尽量都采用域名来访问项目。

一级域名：leyou.com

二级域名：manage.leyou.com , api.leyou.com，image.leyou.com

但是我们并没申请这些域名，那么只需要在本机增加配置，就可以使用域名进行访问。

修改 C:\Windows\System32\drivers\etc 下的 hosts文件，加入以下内容：

127.0.0.1  api.leyou.com        #api接口的地址
127.0.0.1  manage.leyou.com   #后台管理平台
127.0.0.1  www.leyou.com     # 门户网站
127.0.0.1  image.leyou.com     #图片域名

## 4.4.创建父工程

创建统一的父工程：leyou，用来管理依赖及其版本，注意是创建project，而不是moudle

![1551239776553](assets/1551239776553.png)

填写工程信息：

 ![1551239758795](assets/1551239758795.png)

保存的位置信息：

![1551239769186](assets/1551239769186.png)



然后将pom文件修改：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.leyou</groupId>
    <artifactId>leyou</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.3.RELEASE</version>
        <relativePath/>
    </parent>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
        <spring-cloud.version>Greenwich.RELEASE</spring-cloud.version>
        <mysql.version>5.1.47</mysql.version>
        <mybaitsPlus.version>3.1.1</mybaitsPlus.version>
        <leyou.latest.version>1.0-SNAPSHOT</leyou.latest.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- springCloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybaitsPlus.version}</version>
            </dependency>
            <!-- mysql驱动 -->
            <dependency>
                <groupId>mysql</groupId>
                <artifactId>mysql-connector-java</artifactId>
                <version>${mysql.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.4</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

</project>
```

可以发现，我们在父工程中引入了SpringCloud等很多以后需要用到的依赖，以后创建的子工程就不需要自己引入了。

如果是一个需要运行和启动的子工程，需要加上插件：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```





## 4.5.创建EurekaServer

### 4.5.1.创建工程

这个大家应该比较熟悉了。

我们的注册中心，起名为：ly-registry，直接创建maven项目，自然会继承父类的依赖：

选择新建module：

![1551239817387](assets/1551239817387.png)



选择maven安装，但是不要选择骨架：

![1551239800177](assets/1551239800177.png)

然后填写项目坐标，我们的项目名称为ly-registry:

![1551239836496](assets/1551239836496.png)

选择安装目录，因为是聚合项目，目录应该是在父工程leyou的下面：

![1551239848532](assets/1551239848532.png)



### 4.5.2.添加依赖

添加EurekaServer的依赖：

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

    <artifactId>ly-registry</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
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

### 4.5.4.编写启动类

创建一个包：com.leyou，然后新建一个启动类：

```java
@SpringBootApplication
@EnableEurekaServer
public class LyRegistry {
    public static void main(String[] args) {
        SpringApplication.run(LyRegistry.class, args);
    }
}
```

### 4.5.4.配置文件

```yaml
server:
  port: 10086
spring:
  application:
    name: ly-registry #微服务的serviceID
eureka:
  client:
    fetch-registry: false
    register-with-eureka: false
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
```

### 4.5.5.项目的结构：

目前，整个项目的结构如图：

![1551239918685](assets/1551239918685.png)



## 4.6.创建Zuul网关

### 4.6.1.创建工程

与上面类似，选择maven方式创建Module，然后填写项目名称，我们命名为：ly-gateway

![1551239931675](assets/1551239931675.png)

填写保存的目录：

![1551239938112](assets/1551239938112.png)

### 4.6.2.添加依赖

这里我们需要添加Zuul和EurekaClient的依赖：

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

    <artifactId>ly-gateway</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
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

### 4.6.4.编写启动类

创建一个包：com.leyou.gateway，然后新建一个启动类：

```java
@SpringCloudApplication
@EnableZuulProxy
public class LyGateway {
    public static void main(String[] args) {
        SpringApplication.run(LyGateway.class, args);
    }
}
```



### 4.6.4.配置文件

```yaml
server:
  port: 10010
spring:
  application:
    name: ly-gateway
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
    registry-fetch-interval-seconds: 5
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 6000 # 熔断超时时长：6000ms
ribbon:
  ConnectTimeout: 500 # ribbon链接超时时长
  ReadTimeout: 2000 # ribbon读取超时时长
  MaxAutoRetries: 0  # 当前服务重试次数
  MaxAutoRetriesNextServer: 1 # 切换服务重试次数
  OkToRetryOnAllOperations: false # 是否对所有的请求方式都重试，只对get请求重试
zuul:
  prefix: /api
```

### 4.6.5.项目结构

目前，leyou下有两个子模块：

- ly-registry：服务的注册中心（EurekaServer）
- ly-api-gateway：服务网关（Zuul）

目前，服务的结构如图所示：

 	![1551240009768](assets/1551240009768.png)



截止到这里，我们已经把基础服务搭建完毕。

## 4.7.通用工具模块

有些工具或通用的约定内容，我们希望各个服务共享，因此需要创建一个工具模块：`ly-common`

### 4.7.1.创建common工程

使用maven来构建module：

![1551241061737](assets/1551241061737.png)

位置信息：

![1551241076840](assets/1551241076840.png)



结构：

 ![1551241113648](assets/1551241113648.png)

### 4.7.2.引入工具类

然后把课前资料提供的工具类引入：

![1556356508809](assets/1556356508809.png) 

 ![1529377719351](assets/1529377719351.png)

然后引入依赖：

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

    <groupId>com.leyou</groupId>
    <artifactId>ly-common</artifactId>

<dependencies>
    <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-beans</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>3.1.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.9.8</version>
            <scope>compile</scope>
        </dependency>
            <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
        </dependency>    
</dependencies>
</project>
```



每个工具类的作用：

- BeanHelper：实现Bean属性的拷贝，把一个Bean的属性拷贝到另一个Bean，前提是其属性名一致或部分一致
- CookieUtils：实现cookie的读和写
- IdWorker：生成Id
- JsonUtils：实现实体类与Json的转换
- RegexUtils：常用正则校验
- RegexPattern：常用正则的字符串



### 4.7.3.Json工具类

我们在项目中，经常有需求对对象进行JSON的序列化或者反序列化，所以我们定义了一个工具类，JsonUtils：

```java
public class JsonUtils {

    public static final ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj.getClass() == String.class) {
            return (String) obj;
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("json序列化出错：" + obj, e);
            return null;
        }
    }

    public static <T> T toBean(String json, Class<T> tClass) {
        try {
            return mapper.readValue(json, tClass);
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    public static <E> List<E> toList(String json, Class<E> eClass) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, eClass));
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }

    public static <K, V> Map<K, V> toMap(String json, Class<K> kClass, Class<V> vClass) {
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructMapType(Map.class, kClass, vClass));
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }
    
    public static <T> T nativeRead(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            logger.error("json解析出错：" + json, e);
            return null;
        }
    }
}

```



里面包含四个方法：

- toString：把一个对象序列化为String类型，包含1个参数：

  - `Object obj`：原始java对象

- toList：把一个json反序列化为List类型，需要指定集合中元素类型，包含两个参数：

  - `String json`：要反序列化的json字符串
  - `Class eClass`：集合中元素类型

- toMap：把一个json反序列化为Map类型，需要指定集合中key和value类型，包含三个参数：

  - `String json`：要反序列化的json字符串
  - `Class kClass`：集合中key的类型
  - `Class vClass`：集合中value的类型

- nativeRead：把json字符串反序列化，当反序列化的结果比较复杂时，通过这个方法转换，参数：

  - `String json`：要反序列化的json字符串

  - `TypeReference<T> type`：在传参时，需要传递TypeReference的匿名内部类，把要返回的类型写在TypeReference的泛型中，则返回的就是泛型中类型

  - 例如：

    ```java
    List<User> users = JsonUtils.nativeRead(json, new TypeReference<List<User>>() {});
    ```



测试代码：

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
static class User{
    String name;
    Integer age;
}
public static void main(String[] args) {
    User user = new User("Jack", 21);
    // toString
    //        String json = toString(user);
    //        System.out.println("json = " + json);

    // 反序列化
    //        User user1 = toBean(json, User.class);
    //        System.out.println("user1 = " + user1);

    // toList
    //        String json = "[20, -10, 5, 15]";
    //        List<Integer> list = toList(json, Integer.class);
    //        System.out.println("list = " + list);

    // toMap
    //        String json = "{\"name\":\"Jack\", \"age\": \"21\"}";
    //
    //        Map<String, String> map = toMap(json, String.class, String.class);
    //        System.out.println("map = " + map);

    String json = "[{\"name\":\"Jack\", \"age\": \"21\"}, {\"name\":\"Rose\", \"age\": \"18\"}]";

    List<Map<String, String>> maps = nativeRead(json, new TypeReference<List<Map<String, String>>>() {
    });

    for (Map<String, String> map : maps) {
        System.out.println("map = " + map);
    }
}
```





## 4.8.创建商品微服务

既然是一个全品类的电商购物平台，那么核心自然就是商品。因此我们要搭建的第一个服务，就是商品微服务。其中会包含对于商品相关的一系列内容的管理，包括：

- 商品分类管理
- 品牌管理
- 商品规格参数管理
- 商品管理

我们先完成项目的搭建：

### 4.8.1.微服务的结构

因为与商品的品类相关，我们的工程命名为`ly-item`.

需要注意的是，我们的ly-item是一个微服务，那么将来肯定会有其它系统需要来调用服务中提供的接口，因此肯定也会使用到接口中关联的实体类。

因此这里我们需要使用聚合工程，将要提供的接口及相关实体类放到独立子工程中，以后别人引用的时候，只需要知道坐标即可。

我们会在ly-item中创建两个子工程：

- ly-item-pojo：主要是相关实体类，供数据传输

  Pojo  、DTO、VO、BO （一个BO包含多个pojo）

- ly-item-service：所有业务逻辑及内部使用接口

调用关系如图所示：

![1543896420669](assets/1543896420669.png)

### 4.8.2.创建父工程ly-item

依然是使用maven构建：

![1551240063940](assets/1551240063940.png)

保存的位置：

![1551240086739](assets/1551240086739.png)

不需要任何依赖，我们可以把项目打包方式设置为pom

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
    <artifactId>ly-item</artifactId>
    <!-- 打包方式为pom -->
    <packaging>pom</packaging>
    
</project>
```



### 4.8.3.创建ly-item-pojo

在ly-item工程上点击右键，选择new > module:

![1551240166926](assets/1551240166926.png)

依然是使用maven构建，注意父工程是ly-item：

![1551240202115](assets/1551240202115.png)

**注意**：接下来填写的目录结构需要自己手动完成，保存到`ly-item`下的`ly-item-pojo`目录中：

![1551240220009](assets/1551240220009.png)

点击Finish完成。

此时的项目结构：

 ![1544362076716](assets/1544362076716.png) 



### 4.8.4.创建ly-item-service

与`ly-item-interface`类似，我们选择在`ly-item`上右键，新建module，然后填写项目信息：

![1551240269328](assets/1551240269328.png)

填写存储位置，是在`/ly-item/ly-item-service`目录

![1551240283037](assets/1551240283037.png)

点击Finish完成。

### 4.8.5.整个微服务结构

如图所示：

![1544362113639](assets/1544362113639.png) 



我们打开ly-item的pom查看，会发现ly-item-pojo和ly-item-service都已经称为module了：

![1551240314579](assets/1551240314579.png)



### 4.8.6.添加依赖

接下来我们给`ly-item-service`中添加依赖：

思考一下我们需要什么？

- Eureka客户端
- web启动器
- MybaitsPlus启动器
- 连接池
- mysql驱动
- 千万不能忘了，我们自己也需要`ly-item-pojo`中的实体类

这些依赖，我们在顶级父工程：leyou中已经添加好了。所以直接引入即可：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>ly-item</artifactId>
        <groupId>com.leyou</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ly-item-service</artifactId>

    <dependencies>
        <!--web启动器-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!--eureka客户端-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <!--mybatisPlus-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        <!--数据库驱动-->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        <!--实体类-->
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-item-pojo</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <!--通用工具类-->
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-common</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <!--单元测试-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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



ly-item-pojo中需要什么依赖我们暂时不清楚，所以先不管。



### 4.8.7.编写启动和配置

在整个`ly-item工程`中，只有`ly-item-service`是需要启动的。因此在其中编写启动类即可：

```java
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.leyou.item.mapper")
public class LyItemApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyItemApplication.class, args);
    }
}
```

MyBatisPlus分页配置

```java
@Configuration
public class MyBatisPlusConfig {
    /***
     * plus 的性能优化
     * 生产环境不适用
     * @return
     */
    @Bean
    public PerformanceInterceptor performanceInterceptor() {
        PerformanceInterceptor performanceInterceptor = new PerformanceInterceptor();
        /*<!-- SQL 执行性能分析，开发环境使用，线上不推荐。 maxTime 指的是 sql 最大执行时长 -->*/
        performanceInterceptor.setMaxTime(1000);
        /*<!--SQL是否格式化 默认false-->*/
        performanceInterceptor.setFormat(true);
        return performanceInterceptor;
    }

    /**
     * @Description : mybatis-plus分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        return new PaginationInterceptor();
    }
}
```



然后是全局属性文件：

```yaml
server:
  port: 8081
spring:
  application:
    name: item-service
  datasource:
      driver-class-name: com.mysql.jdbc.Driver
      url: jdbc:mysql:///leyou?characterEncoding=UTF-8
      username: root
      password: 123456
#myBatis-plus
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl #输出sql日志
  #mapper-locations: classpath*:mapper/**Mapper.xml 可以不写
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
    registry-fetch-interval-seconds: 10
  instance:
    prefer-ip-address: true
    ip-address: 127.0.0.1
```



整个结构：

  ![1544362221352](assets/1544362221352.png)

### 4.8.8.添加商品微服务的路由规则

既然商品微服务已经创建，接下来肯定要添加路由规则到Zuul中，我们不使用默认的路由规则。

```yaml
zuul:
  prefix: /api # 添加路由前缀
  routes:
    item-service: /item/** # 将商品微服务映射到/item/**
```





### 4.8.9.启动测试

我们分别启动：ly-registry，ly-api-gateway，ly-item-service

 ![1551241034715](assets/1551241034715.png)

查看Eureka面板：

![1534081230701](assets/1534081230701.png)





# 5.通用异常处理

在项目中出现异常是在所难免的，但是出现异常后怎么处理，这就很有学问了。

默认我们需要service层的异常需要抛出，而servcie层被Spring管理，

默认Spring的事务回滚只对RunTimeException起作用，所以我们要把service层的编译器异常转换为运行时异常抛出。

只有这种异常不需要影响业务，或者通过这种异常可以执行其他业务，我们才需要try

## 5.1.场景预设

### 5.1.1.场景

我们预设这样一个场景，假如我们做新增商品，需要接收下面的参数：

>price：价格
>name：名称

然后对数据做简单校验：

- 价格不能为空

新增时，自动形成ID，然后随商品对象一起返回

### 5.1.2.代码

在ly-item-service中编写实体类：

```java
@Data
public class Item {
    private Integer id;
    private String name;
    private Long price;
}
```

在ly-item-service中编写业务：

service：

```java
@Service
public class ItemService {
    
    public Item saveItem(Item item){
        int id = new Random().nextInt(100);
        item.setId(id);
        return item;
    }
}

```

- 这里临时使用随机生成id，然后直接返回，没有做数据库操作

controller：

```java
@RestController
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PostMapping("item")
    public ResponseEntity<Item> saveItem(Item item){
        // 如果价格为空，则抛出异常，返回400状态码，请求参数有误
        if(item.getPrice() == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        Item result = itemService.saveItem(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
```

## 5.2.统一异常处理

### 5.2.1初步测试

现在我们启动项目，做下测试：

通过RestClient去访问：

![1534202307259](assets/1534202307259.png)

发现参数不正确时，返回了400。看起来没问题

### 5.2.2.问题分析

虽然上面返回结果正确，但是没有任何有效提示，这样是不是不太友好呢？

我们在返回错误信息时，使用了这样的代码：

```java
return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
```

响应体是null，因此返回结果才只有状态码。

如果我们在body中加入错误信息，会出现新的问题：

![1534202544199](assets/1534202544199.png)

这个错误显然是返回类型不匹配造成的，我们返回值类型中要的是`ResponseEntity<Item>`，因此body中必须是Item对象，不能是字符串。

如何解决？

大家可能会想到：我们把返回值改成String就好了，返回对象的时候，手动进行Json序列化。

这确实能达到目的，但是不是给开发带来了不必要的麻烦呢？



上面的问题，通过ResponseEntity是无法完美解决的，那么我们需要转换一下思考问题的方式，

既然不能统一返回一样的内容，干脆在出现问题的地方就不要返回，而是转为异常，然后再统一处理异常。这样就问题从返回值类型，转移到了异常的处理了。



那么问题来了：具体该怎么操作呢？

### 5.2.3.统一异常处理

我们先修改controller的代码，把异常抛出：

```java
@PostMapping("item")
public ResponseEntity<Item> saveItem(Item item){
    if(item.getPrice() == null){
        throw new RuntimeException("价格不能为空");
    }
    Item result = itemService.saveItem(item);
    return ResponseEntity.ok(result);
}
```

Spring 在3.2版本后面增加了一个ControllerAdvice注解。

ControllerAdvice拆分开来就是Controller Advice，关于Advice，在Spring Aop中，其是用于封装一个切面所有属性的，包括切入点和需要织入的切面逻辑。

这里ContrllerAdvice也可以这么理解，其抽象级别应该是用于对Controller进行“切面”环绕的，而具体的业务织入方式则是通过结合其他的注解来实现的。

@ControllerAdvice是在类上声明的注解，其用法主要有三点：

- 结合方法型注解@ExceptionHandler，用于捕获Controller中抛出的指定类型的异常，从而达到不同类型的异常区别处理的目的；

- 结合方法型注解@InitBinder，用于request中自定义参数解析方式进行注册，从而达到自定义指定格式参数的目的；

- 结合方法型注解@ModelAttribute，表示其标注的方法将会在目标Controller方法执行之前执行。

@ControllerAdvice的用法基本是将其声明在某个bean上，然后在该bean的方法上使用其他的注解来指定不同的织入逻辑。不过这里@ControllerAdvice并不是使用AOP的方式来织入业务逻辑的，而是Spring内置对其各个逻辑的织入方式进行了内置支持。

接下来，我们使用SpringMVC提供的统一异常拦截器，因为是统一处理，我们放到`ly-common`项目中：

新建一个类，名为：BasicExceptionAdvice

 ![1551262677854](assets/1551262677854.png)

然后代码如下：

```java
@ControllerAdvice
@Slf4j
public class BasicExceptionAdvice {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException e) {
        // 我们暂定返回状态码为400， 然后从异常中获取友好提示信息
        return ResponseEntity.status(400).body(e.getMessage());
    }
}
```

解读：

- `@ControllerAdvice`：默认情况下，会拦截所有加了`@Controller`的类

  ![1534203615380](assets/1534203615380.png)

- `@ExceptionHandler(RuntimeException.class)`：作用在方法上，声明要处理的异常类型，可以有多个，这里指定的是`RuntimeException`。被声明的方法可以看做是一个SpringMVC的`Handler`：

  - 参数是要处理的异常，类型必须要匹配
  - 返回结果可以是`ModelAndView`、`ResponseEntity`等，基本与`handler`类似

- 这里等于从新定义了返回结果，我们可以随意指定想要的返回类型。此处使用了String

此处使用了spring的注解，因此需要在ly-common中引入web依赖：

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
</dependency>
```



要想在商品服务中扫描到这个advice，需要在ly-item-service中引入ly-common依赖：

```xml
<dependency>
    <groupId>com.leyou.common</groupId>
    <artifactId>ly-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

重启项目测试：

![1534204142605](assets/1534204142605.png)

成功返回了错误信息！



### 5.2.4.异常信息及状态

刚才的处理看似完美，但是仔细想想，我们在通用的异常处理中，把返回状态码写死为400了：

 ![1534204244712](assets/1534204244712.png)

这样显然不太合理。



但是，仅凭用户抛出的异常，我们根本无法判断到底该返回怎样的状态码，可能是参数有误、数据库异常、没有权限，等等各种情况。



因此，用户抛出异常时，就必须传递两个内容：

- 异常信息
- 异常状态码

但是`RuntimeException`是无法接受状态码的，只能接受异常的消息，所以我们需要做两件事情：

- 自定义异常，来接受状态码、异常消息
- 状态码与异常消息可能会重复使用，我们通过枚举来把这些信息变为常量

### 5.2.5.自定义异常枚举

枚举：把一件事情的所有可能性列举出来。在计算机中，枚举也可以叫多例，单例是多例的一种情况 。

单例：一个类只能有一个实例。

多例：一个类只能有有限个数的实例。



单例的实现：

- 私有化构造函数
- 在成员变量中初始化本类对象
- 对外提供静态方法，访问这个对象



我们定义一个枚举，用于封装异常状态码和异常信息：

![1551262817818](assets/1551262817818.png)

```java
@Getter
public enum ExceptionEnum {
    PRICE_CANNOT_BE_NULL(400, "价格不能为空！");
    private int status;
    private String message;

    ExceptionEnum(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
```



### 5.2.6.自定义异常

然后自定义异常，来获取枚举对象。

在ly-common中定义自定义异常类：

 ![1534204572495](assets/1534204572495.png)

代码：

```java
@Getter
public class LyException extends RuntimeException {
    private int status;

    public LyException(ExceptionEnum em) {
        super(em.getMessage());
        this.status = em.getStatus();
    }

    public LyException(ExceptionEnum em, Throwable cause) {
        super(em.getMessage(), cause);
        this.status = em.getStatus();
    }
}
```

- status：响应状态码



修改controller代码：

```java
@PostMapping("item")
public ResponseEntity<Item> saveItem(Item item){
    if(item.getPrice() == null){
        throw new LyException(ExceptionEnum.PRICE_CANNOT_BE_NULL);
    }
    Item result = itemService.saveItem(item);
    return ResponseEntity.ok(result);
}
```



### 5.2.7.自定义异常结果

在刚刚编写的BasicExceptionAdvice中，我们返回的结果是String，为了让异常结果更友好，我们不在返回在ly-common中定义异常结果对象：



```java
@Getter
public class ExceptionResult {
    private int status;
    private String message;
    private String timestamp;

    public ExceptionResult(LyException e) {
        this.status = e.getStatus();
        this.message = e.getMessage();
        this.timestamp = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
    }
}
```

这里使用了日期工具类：JodaTime，我们要引入依赖在ly-common中：

```xml
<dependency>
    <groupId>joda-time</groupId>
    <artifactId>joda-time</artifactId>
</dependency>
```



修改异常处理逻辑：

```java
@ControllerAdvice
@Slf4j
public class BasicExceptionAdvice {

    @ExceptionHandler(LyException.class)
    public ResponseEntity<ExceptionResult> handleLyException(LyException e) {
        return ResponseEntity.status(e.getStatus()).body(new ExceptionResult(e));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.status(500).body(e.getMessage());
    }
}
```

再次测试：

![image-20180913140749602](assets/image-20180913140749602.png)



以后，我们无论controller还是service层的业务处理，出现异常情况，都抛出自定义异常，方便统一处理。





## 5.3.异常枚举默认值

最后，这里给大家提供一个已经写好大量异常信息的枚举类：

```java
package com.leyou.common.enums;


@Getter
public enum ExceptionEnum {
    INVALID_FILE_TYPE(400, "无效的文件类型！"),
    INVALID_PARAM_ERROR(400, "无效的请求参数！"),
    INVALID_PHONE_NUMBER(400, "无效的手机号码"),
    INVALID_VERIFY_CODE(400, "验证码错误！"),
    INVALID_USERNAME_PASSWORD(400, "无效的用户名和密码！"),
    INVALID_SERVER_ID_SECRET(400, "无效的服务id和密钥！"),
    INVALID_NOTIFY_PARAM(400, "回调参数有误！"),
    INVALID_NOTIFY_SIGN(400, "回调签名有误！"),

    CATEGORY_NOT_FOUND(400, "商品分类不存在！"),
    BRAND_NOT_FOUND(400, "品牌不存在！"),
    SPEC_NOT_FOUND(400, "规格不存在！"),
    GOODS_NOT_FOUND(400, "商品不存在！"),
    CARTS_NOT_FOUND(400, "购物车不存在！"),
    APPLICATION_NOT_FOUND(400, "应用不存在！"),
    ORDER_NOT_FOUND(400, "订单不存在！"),
    ORDER_DETAIL_NOT_FOUND(400, "订单数据不存在！"),

    DATA_TRANSFER_ERROR(500, "数据转换异常！"),
    INSERT_OPERATION_FAIL(500, "新增操作失败！"),
    UPDATE_OPERATION_FAIL(500, "更新操作失败！"),
    DELETE_OPERATION_FAIL(500, "删除操作失败！"),
    FILE_UPLOAD_ERROR(500, "文件上传失败！"),
    DIRECTORY_WRITER_ERROR(500, "目录写入失败！"),
    FILE_WRITER_ERROR(500, "文件写入失败！"),
    SEND_MESSAGE_ERROR(500, "短信发送失败！"),
    INVALID_ORDER_STATUS(500, "订单状态不正确！"),

    UNAUTHORIZED(401, "登录失效或未登录！");

    private int status;
    private String message;

    ExceptionEnum(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
```



# 6.使用域名访问本地项目

## 6.1.统一环境

我们现在访问页面使用的是：

> http://localhost:9001

有什么问题呢？

实际开发中，会有不同的环境：

- 开发环境：自己的电脑
- 测试环境：提供给测试人员使用的环境
- 预发布环境：数据是和生成环境的数据一致，运行最新的项目代码进去测试
- 生产环境：项目最终发布上线的环境

如果不同环境使用不同的ip去访问，可能会出现一些问题。为了保证所有环境的一致，我们会在各种环境下都使用域名来访问。

我们将使用以下域名：

>主域名是：www.leyou.com
>
>管理系统域名：manage.leyou.com
>
>网关域名：api.leyou.com

最终，我们希望这些域名指向的还是我们本机的某个端口。

那么，当我们在浏览器输入一个域名时，浏览器是如何找到对应服务的ip和端口的呢？

## 6.2.域名解析

一个域名一定会被解析为一个或多个ip。这一般会包含两步：

- 本地域名解析

  浏览器会首先在本机的hosts文件中查找域名映射的IP地址，如果查找到就返回IP ，没找到则进行域名服务器解析，一般本地解析都会失败，因为默认这个文件是空的。

  - Windows下的hosts文件地址：

    > C:/Windows/System32/drivers/etc/hosts

  - Linux下的hosts文件所在路径：

    > /etc/hosts 

  样式：

  ```
  # My hosts
  127.0.0.1 localhost
  127.0.0.1 www.xmind.net
  ```
  
- 域名服务器解析

  本地解析失败，才会进行域名服务器解析，域名服务器就是网络中的一台计算机，里面记录了所有注册备案的域名和ip映射关系，一般只要域名是正确的，并且备案通过，一定能找到。



## 6.3.解决域名解析问题

我们不可能去购买一个域名，因此我们可以伪造本地的hosts文件，实现对域名的解析。修改本地的host为：

```
127.0.0.1 api.leyou.com
127.0.0.1 manage.leyou.com
```

这样就实现了域名的关系映射了。

每次在C盘寻找hosts文件并修改是非常麻烦的，给大家推荐一个快捷修改host的工具，在课前资料中可以找到：

 ![1526014883706](assets/1526014883706.png)

效果：

 ![1526015022365](assets/1526015022365.png)

我们添加了两个映射关系：

>127.0.0.1 api.leyou.com     网关Zuul
>
>127.0.0.1 manage.leyou.co    后台系统地址

现在，ping一下域名试试是否畅通：

 ![1526015211298](assets/1526015211298.png)

OK！

## 6.4.nginx解决端口问题

虽然域名解决了，但是现在如果我们要访问，还得自己加上端口：`http://manage.leyou.com:9001`。

这就不够优雅了。我们希望的是直接域名访问：`http://manage.leyou.com`。这种情况下端口默认是80，如何才能把请求转移到9001端口呢？

这里就要用到反向代理工具：Nginx

Lua脚本 + nginx

### 6.4.1.什么是Nginx

![1526187409033](assets/1526187409033.png)



nginx可以作为web服务器，但更多的时候，我们把它作为网关，因为它具备网关必备的功能：

- 反向代理
- 负载均衡
- 动态路由
- 请求过滤

### 6.4.2.nginx作为web服务器

Web服务器分2类：

- web应用服务器，如：
  - tomcat
  - resin
  - jetty
- web服务器，如：
  - Apache 服务器
  - Nginx
  - IIS

区分：web服务器不能解析jsp等页面，只能处理js、css、html等静态资源。
并发：web服务器的并发能力远高于web应用服务器。

Nginx + tomcat

### 6.4.3.nginx作为反向代理

什么是反向代理？

- 代理：通过客户机的配置，实现让一台服务器代理客户机，客户的所有请求都交给代理服务器处理。
- 反向代理：用一台服务器，代理真实服务器，用户访问时，不再是访问真实服务器，而是代理服务器。

nginx可以当做反向代理服务器来使用：

- 我们需要提前在nginx中配置好反向代理的规则，不同的请求，交给不同的真实服务器处理
- 当请求到达nginx，nginx会根据已经定义的规则进行请求的转发，从而实现路由功能



利用反向代理，就可以解决我们前面所说的端口问题，如图

如果是安装在本机：

![1526016663674](assets/1526016663674.png)



### 6.4.4.安装和使用

#### windows下安装

安装非常简单，把课前资料提供的nginx直接解压即可，绿色免安装，舒服！

我们在本地安装一台nginx：

 ![1526016829605](assets/1526016829605.png)

目录结构：

 ![1526188477802](assets/1526188477802.png)



> ### 使用

nginx可以通过命令行来启动，操作命令：

- 启动：`start nginx.exe`
- 停止：`nginx.exe -s stop`
- 重新加载：`nginx.exe -s reload`

我们需要让nginx反向代理我们的服务器，因此需要自定义nginx配置。

![1551272629398](assets/1551272629398.png) 

为了看起来清爽，我们先在nginx主配置文件`nginx.conf`中使用include指令引用我们的配置：

```
	include vhost/*.conf;
```

如图所示：

![1551272608390](assets/1551272608390.png)

然后在nginx.conf所在目录新建文件夹vhost：

![1551272754312](assets/1551272754312.png) 

并在vhost中创建文件leyou.conf：

![1551272772103](assets/1551272772103.png) 

填写下面配置：

```nginx
upstream leyou-manage{
	server	127.0.0.1:9001;
}
upstream leyou-gateway{
	server	127.0.0.1:10010;
}
upstream leyou-portal{
	server	127.0.0.1:9002;
}

server {
	listen       80;
	server_name  manage.leyou.com;
	
	location / {
	    proxy_pass   http://leyou-manage;
		proxy_connect_timeout 600;
		proxy_read_timeout 5000;
	}
}
server {
	listen       80;
	server_name  www.leyou.com;
	
	location / {
	    proxy_pass   http://leyou-portal;
		proxy_connect_timeout 600;
		proxy_read_timeout 5000;
	}
}
server {
	listen       80;
	server_name  api.leyou.com;
	
	location / {
	    proxy_pass   http://leyou-gateway;
		proxy_connect_timeout 600;
		proxy_read_timeout 5000;
	}
}
```

解读：

- upstream：定义一个负载均衡集群，例如leyou-manage
  - server：集群中某个节点的ip和port信息，可以配置多个，实现负载均衡，默认轮询
- server：定义一个监听服务配置
  - listen：监听的断开
  - server_name：监听的域名
  - location：匹配当前域名下的哪个路径。例如：`/`，代表的是一切路径
    - proxy_pass：监听并匹配成功后，反向代理的目的地，可以指向某个ip和port，或者指向upstream定义的负载均衡集群，nginx反向代理时会轮询中服务列表中选择。

## 6.5.测试

启动nginx，然后用域名访问后台管理系统：

![1526017010289](assets/1526017010289.png)

现在实现了域名访问网站了，中间的流程是怎样的呢？

  ![1541388674330](assets/1541388674330.png)

1. 浏览器准备发起请求，访问http://mamage.leyou.com，但需要进行域名解析

2. 优先进行本地域名解析，因为我们修改了hosts，所以解析成功，得到地址：127.0.0.1（本机）

3. 请求被发往解析得到的ip，并且默认使用80端口：http://127.0.0.1:80

   本机的nginx一直监听80端口，因此捕获这个请求

4. nginx中配置了反向代理规则，将manage.leyou.com代理到http://127.0.0.1:9001

5. 主机上的后台系统的webpack server监听的端口是9001，得到请求并处理，完成后将响应返回到nginx

6. nginx将得到的结果返回到浏览器
