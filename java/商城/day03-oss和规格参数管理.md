# 学习目标

- 创建图片微服务
- 实现本地上传文件
- 实现OSS文件上传
- 了解商品规格数据结构设计思路



文件的上传并不只是在品牌管理中有需求，以后的其它服务也可能需要，因此我们创建一个独立的微服务，专门处理各种上传。

# 1.搭建图片上传服务

## 1.1.创建module

![1552144636424](assets/1552144636424.png)

保存路径：

![1552144664438](/assets/1552144664438.png)

## 1.2.依赖

我们需要EurekaClient和web依赖：

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

    <artifactId>ly-upload</artifactId>

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

## 1.3.编写配置

```yaml
server:
  port: 8082
spring:
  application:
    name: upload-service
  servlet:
    multipart:
      max-file-size: 5MB # 限制文件上传的大小
# Eureka
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
    registry-fetch-interval-seconds: 15
  instance:
    ip-address: 127.0.0.1
    prefer-ip-address: true
```

需要注意的是，我们应该添加了限制文件大小的配置

## 1.4.启动类

```java
@SpringBootApplication
@EnableDiscoveryClient
public class LyUploadApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyUploadApplication.class, args);
    }
}

```

## 1.5.在网关的配置文件增加ly-upload的路由映射

```yml
zuul:
  prefix: /api
  routes:
    item-service: /item/**     #商品微服务的映射
    upload-service: /upload/**  #上传微服务的映射 新增
```



结构：

![1552144792255](/assets/1552144792255.png)



# 2.实现本地图片上传

## 2.1.编写上传功能

### 2.1.1.controller

页面的上传组件，需要做一个简单的修改，去除一个属性need-signature：

![1556611899176](assets/1556611899176.png)

另外，地址修改成`/upload/image`

点击新增品牌页面的上传图片按钮，即可看到上传图片请求：

![1552144440922](assets/1552144440922.png)



从图中很容易看出编写controller需要知道4个内容：

- 请求方式：上传肯定是POST
- 请求路径：/upload/image
- 请求参数：文件，参数名是file，SpringMVC会封装为一个接口：MultipleFile
- 返回结果：这里上传与表单分离，文件不是跟随表单一起提交，而是单独上传并得到结果，随后把结果跟随表单提交。因此上传成功后需要返回一个可以访问的文件的url路径

代码如下：

```java
@RestController
public class UploadController {

    @Autowired
    private UploadService uploadService;

    /**
     * 上传图片功能
     * @param file
     * @return url路径
     */
    @PostMapping("image")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 返回200，并且携带url路径
        return ResponseEntity.ok(this.uploadService.upload(file));
    }
}
```



### 2.1.2.service

在上传文件过程中，我们需要对上传的内容进行校验：

1. 校验文件大小
2. 校验文件的媒体类型
3. 校验文件的内容

文件大小在Spring的配置文件中设置，因此已经会被校验，我们不用管。

具体代码：

```java
@Service
public class UploadService {

    // 支持的文件类型
    private static final List<String> suffixes = Arrays.
        asList("image/png", "image/jpeg", "image/bmp");

   /**
     * 本地上传
     * @param file
     * @return
     */
    public String image(MultipartFile file) {
//         检查文件类型是否支持
        String contentType = file.getContentType();
        if(!suffixes.contains(contentType)){
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }
//         检查当前文件是否是图片，使用java提供的ImageIO工具
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if(bufferedImage == null){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }
//        原文件名
        String originalFilename = file.getOriginalFilename();
//        获取文件的后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
//        构造新的文件名，名字不重复
        String newFileName = UUID.randomUUID().toString()+suffix;
//        上传文件，存在nginx下
        String filePath = "C:\\workspace\\heima-jee123\\nginx-1.12.2\\html";
        File dir = new File(filePath);
//        检查上传目录 是否存在
        if(!dir.exists()){
//            如果目录不存在，创建目录
            dir.mkdir();
        }
//        把文件上传到本地目录,使用IO流
        try {
            file.transferTo(new File(dir,newFileName));
        } catch (IOException e) {
            e.printStackTrace();
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        }
//      http://image.leyou.com/XXXXXX.jpg
        String url = "http://image.leyou.com/"+newFileName;
        return url;
    }
}
```



这里有一个问题：为什么图片地址需要使用另外的url？

- 图片不能保存在服务器内部，这样会对服务器产生额外的加载负担
- 一般静态资源都应该使用独立域名，这样访问静态资源时不会携带一些不必要的cookie，减小请求的数据量



当然，为了能够通过域名`http://image.leyou.com`来访问我们的图片，我们还需要结合hosts和nginx来实现。

首先将`http://image.leyou.com`的地址指向本机：

![1552264759518](assets/1552264759518.png)

然后修改nginx，反向代理到本地的html目录：

```nginx
server {
	listen       80;
	server_name  image.leyou.com;
	location / {
		root	html;
	}
}
```





### 2.1.3.测试上传

我们通过RestClient工具来测试：

 ![1526196967376](assets/1526196967376.png)

结果：

 ![1526197027688](assets/1526197027688.png)



## 2.2. Nginx的请求大小限制

我们上传一个超过1M大小的文件

![1552145659243](assets/1552145659243.png)

发现收到错误响应：

![1552145695272](assets/1552145695272.png)



这是nginx对文件大小的限制，我们`leyou.conf`中的server的外面，设置大小限制为5M：

```nginx
#设置 body内容大小为5M
client_max_body_size 5m;	

server {
    listen       80;
    server_name  api.leyou.com;

    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    location / {
        proxy_pass http://127.0.0.1:10010;
        proxy_connect_timeout 600;
        proxy_read_timeout 600;
    }
}
```

再次测试，发现Nginx不报错了，但是出现了新的问题：

![1552145925766](assets/1552145925766.png)

这次的错误似乎是服务端报错的。是什么原因呢？

## 2.3.绕过网关缓存

上述错误是经过查看是网关Zuul报错的。为什么呢？因为文件上传会先经过zuul，再到ly-upload。而zuul这里发现文件过大，于是就报错了。



默认情况下，所有的请求经过Zuul网关的代理，默认会通过SpringMVC预先对请求进行处理，缓存。普通请求并不会有什么影响，但是对于大文件上传，就会造成造成不必要的网络负担。在高并发时，可能导致网络阻塞，Zuul网关不可用。这样我们的整个系统就瘫痪了。

zuul里面分为  zuulServlet  springMvcServlet -Dispatcher

所以，我们上传文件的请求需要绕过SpringMVC的请求缓存，直接通过路由到达目标微服务：

> Zuul is implemented as a Servlet. For the general cases, Zuul is embedded into the Spring Dispatch mechanism. This lets Spring MVC be in control of the routing. In this case, Zuul buffers requests. If there is a need to go through Zuul without buffering requests (for example, for large file uploads), the Servlet is also installed outside of the Spring Dispatcher. By default, the servlet has an address of `/zuul`. This path can be changed with the `zuul.servlet-path` property. 



现在，查看页面的请求路径：

  ![1526196446765](assets/1526196446765.png)

我们需要修改到以`/zuul`为前缀，可以通过nginx的rewrite指令实现这一需求：



Nginx提供了rewrite指令，用于对地址进行重写，语法规则：

```
rewrite "用来匹配路径的正则" 重写后的路径 [指令];
```

我们的案例：

```nginx 
client_max_body_size 5m;	
server {
    listen       80;
    server_name  api.leyou.com;

    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    location /api/upload {		
        rewrite "^/(.*)$" /zuul/$1; 
    }

    location / {
        proxy_pass http://127.0.0.1:10010;
        proxy_connect_timeout 600;
        proxy_read_timeout 600;
    }
}
```

- 首先，我们映射路径是/api/upload，而下面一个映射路径是 / ，根据最长路径匹配原则，/api/upload优先级更高。也就是说，凡是以/api/upload开头的路径，都会被第一个配置处理

- `rewrite "^/(.*)$" /zuul/$1`，路径重写：

  - `"^/(.*)$"`：匹配路径的正则表达式，用了分组语法，把`/`以后的所有部分当做1组

  - `/zuul/$1`：重写的目标路径，这里用$1引用前面正则表达式匹配到的分组（组编号从1开始），在原始路径的基础上，添加了`/zuul`前缀。


修改完成，输入`nginx -s reload`命令重新加载配置。

然后测试：

 ![1526200606487](assets/1526200606487.png)

zuul.servletPath,其默认是/zuul，只要路径前缀是zuul.servletPath，根据Servlet映射匹配的优先级，就会绕过DispatcherServlet，通过ZuulServlet进行处理，因此，对于multipart/form请求，就略过了解析MultipartHttpServletRequest的过程，对于所有的请求，由于buffer-requests为false，在FormBodyWrapperFilter中也不会缓冲request(需要HttpServletRequestWrapper包装请求)

```java
@Bean
 
@ConditionalOnMissingBean(name = "zuulServlet")
 
public ServletRegistrationBean zuulServlet() {
 
  ServletRegistrationBean servlet = new ServletRegistrationBean(new ZuulServlet(),
 
        this.zuulProperties.getServletPattern());
 
  // The whole point of exposing this servlet is to provide a route that doesn't
 
  // buffer requests.
 
  servlet.addInitParameter("buffer-requests", "false");
 
  return servlet;
 
}

```

在ZuulProperties中可以找到这个路径的配置

```java
private String servletPath = "/zuul";
```

## 2.4.之前上传的缺陷

先思考一下，之前上传的功能，有没有什么问题？

上传本身没有任何问题，问题出在保存文件的方式，我们是保存在服务器机器，就会有下面的问题：

- 单机器存储，存储能力有限
- 无法进行水平扩展，因为多台机器的文件无法共享,会出现访问不到的情况
- 数据没有备份，有单点故障风险
- 并发能力差

这个时候，最好使用分布式文件存储来代替本地文件存储。



# 3.阿里云对象存储OSS

## 3.1.什么是分布式文件系统

分布式文件系统（Distributed File System）是指文件系统管理的物理存储资源不一定直接连接在本地节点上，而是通过计算机网络与节点相连。 

通俗来讲：

- 传统文件系统管理的文件就存储在本机。
- 分布式文件系统管理的文件存储在很多机器，这些机器通过网络连接，要被统一管理。无论是上传或者访问文件，都需要通过管理中心来访问

常见的分布式文件系统有谷歌的GFS、HDFS（Hadoop）、TFS（淘宝）、Fast DFS（淘宝）等。

不过，企业自己搭建分布式文件系统成本较高，对于一些中小型企业而言，使用云上的文件存储，是性价比更高的选择。

## 3.2.阿里云OSS

阿里的OSS就是一个文件云存储方案：

![1552265269170](assets/1552265269170.png)

简介：

> 阿里云对象存储服务（Object Storage Service，简称 OSS），是阿里云提供的海量、安全、低成本、高可靠的云存储服务。其数据设计持久性不低于99.999999999%，服务设计可用性不低于99.99%。具有与平台无关的RESTful API接口，您可以在任何应用、任何时间、任何地点存储和访问任意类型的数据。
>
> 您可以使用阿里云提供的API、SDK接口或者OSS迁移工具轻松地将海量数据移入或移出阿里云OSS。数据存储到阿里云OSS以后，您可以选择标准类型（Standard）的阿里云OSS服务作为移动应用、大型网站、图片分享或热点音视频的主要存储方式，也可以选择成本更低、存储期限更长的低频访问类型（Infrequent Access）和归档类型（Archive）的阿里云OSS服务作为不经常访问数据的备份和归档。



### 3.2.1 开通oss访问

首先登陆阿里云，然后找到对象存储的产品：

![1552307302707](assets/1552307302707.png)

点击进入后，开通服务：

![1552307241886](assets/1552307241886.png)



随后即可进入管理控制台：

![1552307724634](assets/1552307724634.png)

### 3.2.2.基本概念

OSS中包含一些概念，我们来认识一下：

- 存储类型（Storage Class）

  OSS提供标准、低频访问、归档三种存储类型，全面覆盖从热到冷的各种数据存储场景。其中标准存储类型提供高可靠、高可用、高性能的对象存储服务，能够支持频繁的数据访问；低频访问存储类型适合长期保存不经常访问的数据（平均每月访问频率1到2次），存储单价低于标准类型；归档存储类型适合需要长期保存（建议半年以上）的归档数据，在三种存储类型中单价最低。详情请参见[存储类型介绍](https://help.aliyun.com/document_detail/51374.html#concept-fcn-3xt-tdb)。

- `存储空间（Bucket）`

  存储空间是您用于存储对象（Object）的容器，所有的对象都必须隶属于某个存储空间。您可以设置和修改存储空间属性用来控制地域、访问权限、生命周期等，这些属性设置直接作用于该存储空间内所有对象，因此您可以通过灵活创建不同的存储空间来完成不同的管理功能。

- 对象/文件（Object）

  对象是 OSS 存储数据的基本单元，也被称为OSS的文件。对象由元信息（Object Meta），用户数据（Data）和文件名（Key）组成。对象由存储空间内部唯一的Key来标识。对象元信息是一组键值对，表示了对象的一些属性，比如最后修改时间、大小等信息，同时您也可以在元信息中存储一些自定义的信息。

- 地域（Region）

  地域表示 OSS 的数据中心所在物理位置。您可以根据费用、请求来源等综合选择数据存储的地域。详情请参见[OSS已开通的Region](https://help.aliyun.com/document_detail/31837.html#concept-zt4-cvy-5db)。

- `访问域名（Endpoint`）

  Endpoint 表示OSS对外服务的访问域名。OSS以HTTP RESTful API的形式对外提供服务，当访问不同地域的时候，需要不同的域名。通过内网和外网访问同一个地域所需要的域名也是不同的。具体的内容请参见[各个Region对应的Endpoint](https://help.aliyun.com/document_detail/31837.html#concept-zt4-cvy-5db)。

- `访问密钥（AccessKey）`

  AccessKey，简称 AK，指的是访问身份验证中用到的AccessKeyId 和AccessKeySecret。OSS通过使用AccessKeyId 和AccessKeySecret对称加密的方法来验证某个请求的发送者身份。AccessKeyId用于标识用户，AccessKeySecret是用户用于加密签名字符串和OSS用来验证签名字符串的密钥，其中AccessKeySecret 必须保密。

以上概念中，跟我们开发中密切相关的有三个：

- 存储空间（Bucket）
- 访问域名（Endpoint）
- 访问密钥（AccessKey）：包含了AccessKeyId 和AccessKeySecret。

### 3.2.3.创建一个bucket

在控制台的右侧，可以看到一个`新建Bucket`按钮：

![1552308905874](assets/1552308905874.png)

点击后，弹出对话框，填写基本信息：

![1552309049398](assets/1552309049398.png)

注意点：

- bucket：存储空间名称，名字只能是字母、数字、中划线
- 区域：即服务器的地址，这里选择了上海
- Endpoint：选中区域后，会自动生成一个Endpoint地址，这将是我们访问OSS服务的域名的组成部分
- 存储类型：默认
- 读写权限：这里我们选择公共读，否则每次访问都需要额外生成签名并校验，比较麻烦。敏感数据不要请都设置为私有！
- 日志：不开通

### 3.2.4.创建AccessKey

有了bucket就可以进行文件上传或下载了。不过，为了安全考虑，我们给阿里云账户开通一个子账户，并设置对OSS的读写权限。

点击屏幕右上角的个人图像，然后点击访问控制：

![1552309424324](assets/1552309424324.png)

在跳转的页面中，选择用户，并新建一个用户：

![1552309517332](assets/1552309517332.png)

然后填写用户信息：

![1552309580867](assets/1552309580867.png)

然后会为你生成用户的AccessKeyID和AccessKeySecret：

![1552309726968](assets/1552309726968.png)

妥善保管，不要告诉任何人！

接下来，我们需要给这个用户添加对OSS的控制权限。

进入这个新增的用户详情页面：

![1552309892306](assets/1552309892306.png)

点击添加权限，会进入权限选择页面，输入oss进行搜索，然后选择`管理对象存储服务（OSS）`权限：

![1552309962457](assets/1552309962457.png)

### 3.2.5 设置跨域规则

之前我们在 ly-gateway中设置过项目的跨域规则，这里设置的阿里云的跨域规则

参考阿里的[官方文档](https://help.aliyun.com/document_detail/91868.html?spm=a2c4g.11186623.2.15.53186e28cSOlxg#concept-ahk-rfz-2fb)

![1556548616524](assets/1556548616524.png)

特别需要注意这里的**步骤3**，我们向阿里提交的请求属于跨域请求，因此需要配置跨域许可：

![1556549242811](assets/1556549242811.png)

设置我们的域名，请求方式是POST，因为是文件上传。

![1556549324534](assets/1556549324534.png)



## 3.3.上传文件最佳实践

在控制台的右侧，点击`开发者指南`按钮，即可查看帮助文档：

![1552310900485](assets/1552310900485.png)

然后在弹出的新页面的左侧菜单中找到开发者指南：

![1552310990458](assets/1552310990458.png) 

可以看到上传文件中，支持多种上传方式，并且因为提供的Rest风格的API，任何语言都可以访问OSS实现上传。



我们可以直接使用java代码来实现把图片上传到OSS，不过这样以来文件会先从客户端浏览器上传到我们的服务端tomcat，然后再上传到OSS，效率较低，如图：

![1552311281042](assets/1552311281042.png)

以上方法有三个缺点：

- 上传慢。先上传到应用服务器，再上传到OSS，网络传送比直传到OSS多了一倍。如果直传到OSS，不通过应用服务器，速度将大大提升，而且OSS采用BGP带宽，能保证各地各运营商的速度。
- 扩展性差。如果后续用户多了，应用服务器会成为瓶颈。
- 费用高。需要准备多台应用服务器。由于OSS上传流量是免费的，如果数据直传到OSS，不通过应用服务器，那么将能省下几台应用服务器。

在阿里官方的最佳实践中，推荐了更好的做法：

![1552311136676](assets/1552311136676.png) 

直接从前端（客户端浏览器）上传文件到OSS。

### 3.3.1.web前端直传分析

阿里官方文档中，对于web前端直传又给出了3种不同方案：

- [JavaScript客户端签名直传](https://help.aliyun.com/document_detail/31925.html?spm=a2c4g.11186623.2.10.6c5762121wgIAS#concept-frd-4gy-5db)：客户端通过JavaScript代码完成签名，然后通过表单直传数据到OSS。
- [服务端签名后直传](https://help.aliyun.com/document_detail/31926.html?spm=a2c4g.11186623.2.11.6c5762121wgIAS#concept-en4-sjy-5db)：客户端上传之前，由服务端完成签名，前端获取签名，然后通过表单直传数据到OSS。
- [服务端签名直传并设置上传回调](https://help.aliyun.com/document_detail/31927.html?spm=a2c4g.11186623.2.12.6c5762121wgIAS#concept-qp2-g4y-5db)：服务端完成签名，并且服务端设置了上传后回调，然后通过表单直传数据到OSS。OSS回调完成后，再将应用服务器响应结果返回给客户端。

各自有一些优缺点：

- JavaScript客户端签名直传：
  - 优点：在客户端通过JavaScript代码完成签名，无需过多配置，即可实现直传，非常方便。
  - 问题：客户端通过JavaScript把AccesssKeyID 和AccessKeySecret写在代码里面有泄露的风险
- 服务端签名，JavaScript客户端直传：
  - 优点：Web端向服务端请求签名，然后直接上传，不会对服务端产生压力，而且安全可靠
  - 问题：服务端无法实时了解用户上传了多少文件，上传了什么文件
- 服务端签名直传并设置上传回调：
  - 优点：包含服务端签名后客户端直传的所有优点，并且可以设置上传回调接口路径。上传结束后，OSS会主动将上传的结果信息发送给回调接口。

经过一番分析，大家觉得我们会选哪种方案？

这里我们选择第二种，因为我们并不需要了解用户上传的文件的情况。

### 3.3.2.服务端签名后直传流程

服务端签名后直传的原理如下：

1. 用户发送上传Policy请求到应用服务器（我们的微服务）。
2. 应用服务器返回上传Policy和签名给用户。
3. 用户直接上传数据到OSS。

流程图：

![1552311833528](assets/1552311833528.png)

根据上面的流程，我们需要做的事情包括：

- 改造文件上传组件，达成下面的目的：
  - 上传文件前，向服务端发起请求，获取签名
  - 上传时，修改上传目的地到阿里OSS服务，并携带上传的签名
- 编写服务端接口，接收请求，生成签名后返回



## 3.4.改造上传组件

文件上传组件是我们自定义组件，用法可以参考课前资料的：《自定义组件用法》。

而且，我们的上传组件内部已经实现了对阿里OSS的支持了：

![1552314430650](assets/1552314430650.png)

所以我们只需要在`BrandForm`页面中，给`v-upload`组件，修改url的内容，把image 改成signature。加上`need-signature`属性，并且指定一个获取签名的url地址即可：

![1552314746644](assets/1552314746644.png)

自定义的上传组件中的核心代码：

![1552315238995](assets/1552315238995.png)

这段代码的核心思路：

- 判断needSignature是否为true，true代表需要签名，false代表不需要
- 如果false，直接上传文件到url
- 如果为true，则把url作为签名接口路径，访问获取签名
- 拿到响应结果后，其中可不仅仅包括签名，而是包括：
  - host：上传到阿里的url路径（**bucket + endpoint**）
  - policy：上传的策略
  - signature：签名
  - accessId：就是AccessKeyID
  - dir：在oss的bucket中的子文件夹的名称，可以不写。

因此我们需要在签名返回时，携带这些参数。



刷新页面，可以看到请求已经发出：

![1552315014021](assets/1552315014021.png)

## 3.5.编写签名接口

我们依然在ly-upload项目中完成签名接口编写。

给出了一段示例代码：

```java
protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

    String accessId = "<yourAccessKeyId>"; // 请填写您的AccessKeyId。
    String accessKey = "<yourAccessKeySecret>"; // 请填写您的AccessKeySecret。
    String endpoint = "oss-cn-hangzhou.aliyuncs.com"; // 请填写您的 endpoint。
    String bucket = "bucket-name"; // 请填写您的 bucketname 。
    String host = "http://" + bucket + "." + endpoint; // host的格式为 bucketname.endpoint
    // callbackUrl为 上传回调服务器的URL，请将下面的IP和Port配置为您自己的真实信息。
    String callbackUrl = "http://88.88.88.88:8888";
    String dir = "user-dir-prefix/"; // 用户上传文件时指定的前缀。

    OSSClient client = new OSSClient(endpoint, accessId, accessKey);
    
    try {
        long expireTime = 30;
        long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
        Date expiration = new Date(expireEndTime);
        PolicyConditions policyConds = new PolicyConditions();
        policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
        policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

        String postPolicy = client.generatePostPolicy(expiration, policyConds);
        byte[] binaryData = postPolicy.getBytes("utf-8");
        String encodedPolicy = BinaryUtil.toBase64String(binaryData);
        String postSignature = client.calculatePostSignature(postPolicy);

        Map<String, String> respMap = new LinkedHashMap<String, String>();
        respMap.put("accessid", accessId);
        respMap.put("policy", encodedPolicy);
        respMap.put("signature", postSignature);
        respMap.put("dir", dir);
        respMap.put("host", host);
        respMap.put("expire", String.valueOf(expireEndTime / 1000));
        // respMap.put("expire", formatISO8601Date(expiration));

        JSONObject jasonCallback = new JSONObject();
        jasonCallback.put("callbackUrl", callbackUrl);
        jasonCallback.put("callbackBody",
                          "filename=${object}&size=${size}&mimeType=${mimeType}&height=${imageInfo.height}&width=${imageInfo.width}");
        jasonCallback.put("callbackBodyType", "application/x-www-form-urlencoded");
        String base64CallbackBody = BinaryUtil.toBase64String(jasonCallback.toString().getBytes());
        respMap.put("callback", base64CallbackBody);

        JSONObject ja1 = JSONObject.fromObject(respMap);
        // System.out.println(ja1.toString());
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST");
        response(request, response, ja1.toString());

    } catch (Exception e) {
        // Assert.fail(e.getMessage());
        System.out.println(e.getMessage());
    }
}
```

可以看到代码分4部分：

- 初始化OSSClient，这是阿里SDK提供的工具
- 生成文件上传策略policy（文件大小、文件存储目录等）
- 生成许可签名
- 封装结果并返回

接下来，我们逐个完成。

首先要做的是引入阿里sdk的依赖：

```xml
<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.4.2</version>
    <scope>compile</scope>
</dependency>
```



### 3.5.2.配置OSSClient

OSSClient底层使用的是HttpClient，如果每次访问都创建新的客户端，资源浪费较多。我们可以把其注册到Spring，单例来使用。

首先，把需要用到的数据统一抽取的配置文件：application.yml中

```yaml
ly:
  oss:
    accessKeyId: LTAI4GECXLNqMYLULWMAWm4U
    accessKeySecret: IgT4T7uyd5lLl4Xtg2oYLKGb6YuI52
    host: http://leyou-shunyi.oss-cn-beijing.aliyuncs.com # 访问oss的域名，很重要bucket + endpoint
    endpoint: oss-cn-beijing.aliyuncs.com # 你的服务的端点，不一定跟我一样
    dir: "" # 保存到bucket的某个子目录
    expireTime: 120 # 过期时间，单位是S
    maxFileSize: 5242880 #文件大小限制，这里是5M
```

在com.leyou.upload.config 包下，创建OSSProperties.java,然后通过配置类加载：

```java
package com.leyou.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("ly.oss")
public class OSSProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String bucket;
    private String host;
    private String endpoint;
    private String dir;
    private long expireTime;
    private long maxFileSize;
}
```

在com.leyou.upload.config 包下，创建OSSConfig.java,并通过一个Bean来配置：

```java
package com.leyou.upload.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OSSConfig {

    @Bean
    public OSS ossClient(OSSProperties prop){
        return new OSSClientBuilder()
                .build(prop.getEndpoint(), prop.getAccessKeyId(), prop.getAccessKeySecret());
    }
}
```



### 3.5.2.编写web层

上面的页面请求已经清楚的说明的请求接口的要素：

- 请求方式：GET
- 请求路径：signature
- 请求参数：无
- 返回结果：应该是一个对象，包含下列属性：
  - host：上传到阿里的url路径（bucket + endpoint）
  - policy：上传的策略
  - signature：签名
  - accessId：就是AccessKeyID
  - dir：在oss的bucket中的子文件夹的名称，可以不写。

在UploadController.java 中的代码：

```java
    @GetMapping("signature")
    public ResponseEntity<Map<String,Object>> getAliSignature(){
        return ResponseEntity.ok(uploadService.getSignature());
    }
```

### 3.5.3.service

业务代码：

```java
package com.leyou.upload.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.upload.config.OSSProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UploadService {

    @Autowired
    private OSSProperties prop;

    @Autowired
    private OSS client;

 // 支持的文件类型
    private static final List<String> suffixes = Arrays.
        asList("image/png", "image/jpeg", "image/bmp");

    /**
     * 本地上传
     * @param file
     * @return
     */
    public String image(MultipartFile file) {
//         检查文件类型是否支持
        String contentType = file.getContentType();
        if(!suffixes.contains(contentType)){
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }
//         检查当前文件是否是图片，使用java提供的ImageIO工具
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if(bufferedImage == null){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
        }
//        原文件名
        String originalFilename = file.getOriginalFilename();
//        获取文件的后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
//        构造新的文件名，名字不重复
        String newFileName = UUID.randomUUID().toString()+suffix;
//        上传文件，存在nginx下
        String filePath = "C:\\workspace\\heima-jee123\\nginx-1.12.2\\html";
        File dir = new File(filePath);
//        检查上传目录 是否存在
        if(!dir.exists()){
//            如果目录不存在，创建目录
            dir.mkdir();
        }
//        把文件上传到本地目录,使用IO流
        try {
            file.transferTo(new File(dir,newFileName));
        } catch (IOException e) {
            e.printStackTrace();
            throw new LyException(ExceptionEnum.FILE_UPLOAD_ERROR);
        }
//      http://image.leyou.com/XXXXXX.jpg
        String url = "http://image.leyou.com/"+newFileName;
        return url;
    }

    /**
    * 获取OSS签名
    */
    public Map<String, Object> getSignature() {
        try {
            long expireTime = prop.getExpireTime();
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, prop.getMaxFileSize());
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, prop.getDir());

            String postPolicy = client.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = client.calculatePostSignature(postPolicy);

            Map<String, Object> respMap = new LinkedHashMap<>();
            respMap.put("accessId", prop.getAccessKeyId());
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", prop.getDir());
            respMap.put("host", prop.getHost());
            respMap.put("expire", expireEndTime);
            return respMap;
        }catch (Exception e){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
    }
}
```

### 3.5.4.测试

签名：

![1552317644809](assets/1552317644809.png)

结果：

![1552317675905](assets/1552317675905.png)

上传测试结果：

![1552317761492](assets/1552317761492.png)

在页面访问：

![1552317847336](assets/1552317847336.png)



## 3.6.隐藏阿里服务地址（了解）

在刚才的业务中，我们直接把我们在阿里的域名对外暴露了，如果要隐藏域名信息，我们可以使用一个自定义域名。

另外，最好保持与我们自己的域名一致，我们可以使用：`http://image.leyou.com`，然后使用nginx反向代理，最终再指向阿里服务器域名。

首先，修改服务器端返回的请求域名，修改ly-upload中的application.yml文件：

```yaml
ly:
  oss:
    host: http://image.leyou.com
```

然后在nginx中设置反向代理：

```nginx
server {
	listen       80;
	server_name  image.leyou.com;
	location / {
		proxy_pass   http://leyou-shunyi.oss-cn-beijing.aliyuncs.com;
	}
}
```

重启后测试，一切OK

![1552480202164](assets/1552480202164.png)



# 4.商品规格数据结构

商品中都有属性，不同商品，属性往往不同，这一部分数据很重要，我们一起来看看：

## 4.1.规格属性内容

我们看下京东中商品的规格属性：

一款华为手机的属性：

![1526087063700](assets/1526087063700.png)

一款空调的属性：

![1534633813140](assets/1534633813140.png)

我们发现，不同商品的属性名称竟然不同，假如我们要把属性放入一张表去保存，表字段该如何设计？

别着急，我们再看另一个手机的属性：

三星手机的规格属性：

![1526087142454](assets/1526087142454.png)

我们发现，虽然不同商品，规格不同。但是同一分类的商品，比如都是手机，其规格参数名称是一致的，但是值不一样。

也就是说，商品的规格参数应该是与分类绑定的。**每一个分类都有统一的规格参数模板，但不同商品其参数值可能不同**。

因此：

- 规格参数的名称（key）与值（value）应该分开来保存
- 一个分类，对应一套规格参数模板，只有规格参数key，没有值
- 一个分类对应多个商品，每个商品的规格值不同，每个商品对应一套规格的值



## 4.2.规格与规格组

值我们暂且不管，新增商品时，再来填写规格参数值即可，我们先思考**规格参数模板**（key）该如何设计。

来看下规格参数的结构：

​	 ![1534634716761](assets/1534634716761.png)

- 规格数据首先要分组，组内再有不同的规格参数
- 一个分类规格模板中，有多个规格组
- 每个规格组中，包含多个规格参数

从面向对象的思想来看，我们规格参数和规格组分别是两类事务，并且组与组内参数成**一对多关系**，因此可以有两个类分别描述他们，那么从数据库设计来看，也就对应两张不同的表：



- 规格组：tb_spec_group
  - 一个商品分类下有多个规格组
- 规格参数：tb_spec_param
  - 一个规格组下，有多个规格参数

如图：

![1534636185452](assets/1534636185452.png)

大家接下来要思考的就是：

- 描述规格组需要哪些属性？
- 描述规格参数需要哪些属性？

想清楚上面的问题，就知道表该怎么设计了。



## 4.3.表结构

### 4.3.1.规格组

规格参数分组表：tb_spec_group

```mysql
CREATE TABLE `tb_spec_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cid` bigint(20) NOT NULL COMMENT '商品分类id，一个分类下有多个规格组',
  `name` varchar(32) NOT NULL COMMENT '规格组的名称',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `key_category` (`cid`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8 COMMENT='规格参数的分组表，每个商品分类下有多个规格参数组';
```

规格组有3个字段：

- id：主键
- cid：商品分类id，一个分类下有多个模板
- name：该规格组的名称。



### 4.3.2.规格参数

规格参数表：tb_spec_param

```mysql
CREATE TABLE `tb_spec_param` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cid` bigint(20) NOT NULL COMMENT '商品分类id',
  `group_id` bigint(20) NOT NULL,
  `name` varchar(128) NOT NULL COMMENT '参数名',
  `is_numeric` tinyint(1) NOT NULL COMMENT '是否是数字类型参数，true或false',
  `unit` varchar(128) DEFAULT '' COMMENT '数字类型参数的单位，非数字类型可以为空',
  `generic` tinyint(1) NOT NULL COMMENT '是否是sku通用属性，true或false',
  `searching` tinyint(1) NOT NULL COMMENT '是否用于搜索过滤，true或false',
  `segments` varchar(1024) DEFAULT '' COMMENT '数值类型参数，如果需要搜索，则添加分段间隔值，如CPU频率间隔：0.5-1.0',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `key_group` (`group_id`),
  KEY `key_category` (`cid`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8 COMMENT='规格参数组下的参数名';
```

按道理来说，我们的规格参数就只需要记录参数名、组id、商品分类id即可。但是这里却多出了很多字段，为什么？

#### 4.3.2.1数值类型

某些规格参数可能为数值类型，我们需要标记出来，并且记录单位：

​	 ![1534636623281](assets/1534636623281.png)

我们有两个字段来描述：

- is_numeric：是否为数值类型
  - true：数值类型
  - false：不是数值类型
- unit：参数的单位

#### 4.3.2.2搜索过滤

打开一个搜索页，我们来看看过滤的条件：

![1526090072535](assets/1526090072535.png)

你会发现，过滤条件中的屏幕尺寸、运行内存、网路、机身内存、电池容量、CPU核数等，在规格参数中都能找到：

 ![1526090228171](assets/1526090228171.png)

也就是说，规格参数中的数据，将来会有一部分作为搜索条件来使用。我们可以在设计时，将这部分属性标记出来，将来做搜索的时候，作为过滤条件。

与搜索相关的有两个字段：

- searching：标记是否用作过滤
  - true：用于过滤搜索
  - false：不用于过滤
- segments：某些数值类型的参数，在搜索时需要按区间划分，这里提前确定好划分区间
  - 比如电池容量，0~2000mAh，2000mAh~3000mAh，3000mAh~4000mAh

#### 4.3.2.3通用属性

有一个generic属性，代表通用属性，我们在商品数据结构时再聊。

# 5.商品规格参数管理

接下来，我们一起来看下页面中对规格组和规格参数的管理。

## 5.1.规格组查询

首先是规格组管理，我们点击菜单中的`规格参数`：

![1552659123797](assets/1552659123797.png) 

可以看到页面发生了变化

### 5.1.1页面请求

打开规格参数页面，看到如下内容：

![1552659142800](assets/1552659142800.png)

可以看到规格参数页面的左侧是一个商品分类的树，右侧暂时是空白。那么问题来了：

我们这里是规格管理，为什么会显示商品分类信息呢？



因为规格是跟商品分类绑定的，我们在看到规格参数时，肯定希望知道接下来要管理的是哪个分类下的规格参数。

所以首先会展现商品分类树，并且提示你要选择商品分类，才能看到规格参数的模板。



此时，我们点击分类，当点击到某个3级分类时，页面发生了变化：

![1552659334510](assets/1552659334510.png)

页面右侧出现了数据表格，不过还没有数据。同时，我们查看页面控制台，可以看到请求已经发出：

![1552488184332](assets/1552488184332.png)

这个请求查询的应该就是当前分类下的规格参数组信息了。



### 5.1.2.后端代码

> 实体类

在`ly-item-service`中添加实体类：

```java
/**
 * <p>
 * 规格参数的分组表，每个商品分类下有多个规格参数组
 * </p>
 *
 * @author HeiMa
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbSpecGroup extends Model<TbSpecGroup> {

private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品分类id，一个分类下有多个规格组
     */
    private Long cid;

    /**
     * 规格组的名称
     */
    private String name;

    private Date createTime;

    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}

```



在`ly-item-service`中编写业务：

> controller

先分析下需要的东西，在页面的ajax请求中可以看出：

- 请求方式：查询，肯定是get

- 请求路径：/spec/groups/of/category ，这里通过路径占位符传递商品分类的id

- 请求参数：商品分类id

- 返回结果：我们的业务是查询当前分类下的所有规格组，而一个分类下有多个规格组信息，因此查询的结果应该是规格组的集合，同以前一样，我们也定义一个DTO在`ly-item-pojo`中：

  ```java
  package com.leyou.item.dto;
  
  import lombok.Data;
  
  @Data
  public class SpecGroupDTO {
      private Long id;
      private Long cid;
      private String name;
  }
  ```
  
  

代码：

```java
package com.leyou.item.controller;

import com.leyou.item.service.SpecService;
import com.leyou.pojo.SpecGroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理 规格组 和 规格参数
 */
@RestController
public class SpecController {

    @Autowired
    private SpecService specService;


    /**
     * 根据分类id 查询规格组集合
     * @param cid
     * @return
     */
    @GetMapping("/spec/groups/of/category")
    public ResponseEntity<List<SpecGroupDTO>> findSpecGroupListByCid(@RequestParam(name = "id")Long cid){
        return ResponseEntity.ok(specService.findSpecGroupListByCid(cid));
    }
}

```



> service:

```java
package com.leyou.item.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leyou.common.BeanHelper;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exceptions.LyException;
import com.leyou.item.entity.TbSpecGroup;
import com.leyou.pojo.SpecGroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class SpecService {

    @Autowired
    private TbSpecGroupService tbSpecGroupService;
    /**
     * 根据分类id 查询规格组集合
     * @param cid
     * @return
     */
    public List<SpecGroupDTO> findSpecGroupListByCid(Long cid) {
//        select * from tb_spec_group where cid=?
        QueryWrapper<TbSpecGroup> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("cid",cid);
//        面向对象
        queryWrapper.lambda().eq(TbSpecGroup::getCid,cid);
//        查询集合
        List<TbSpecGroup> tbSpecGroupList = tbSpecGroupService.list(queryWrapper);
//        判断是否为空
        if(CollectionUtils.isEmpty(tbSpecGroupList)){
            throw new LyException(ExceptionEnum.SPEC_GROUP_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(tbSpecGroupList,SpecGroupDTO.class);
    }
}

```



> 页面访问测试：

我们访问：http://api.leyou.com/api/item/spec/groups/of/category/?id=76

 ![1552489634505](assets/1552489634505.png)

然后在后台系统中测试：

![1529552321813](assets/1529552321813.png)



## 5.2.规格参数查询

在刚才的表格中，只显示了规格组信息，但是却没有看到组内的规格参数，那么规格参数信息又保存在哪里呢？

### 5.2.1.页面请求

我们在页面点击某个规格组信息，发现页面发生了变化：

![1552660899113](assets/1552660899113.png)

可以看到当我们选中`主体`这个规格组时，其它规格组信息都隐藏了，表格变成了一个空白表格。

通过表头可以看出，这个表格显示的应该是`主体`这个规格组内的规格参数信息。

此时查看控制台，发现页面发起了新的请求：

![1552661146177](assets/1552661146177.png)

可以看出，这个是根据规格组的id，查询规格参数的请求



### 5.2.2.后台实现

接下来，我们就编写后台接口，完成规格参数查询

> 实体类：

```java
package com.leyou.item.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 规格参数组下的参数名
 * </p>
 *
 * @author HeiMa
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbSpecParam extends Model<TbSpecParam> {

private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品分类id
     */
    private Long cid;

    private Long groupId;

    /**
     * 参数名
     */
    private String name;

    /**
     * 是否是数字类型参数，true或false
     */
    private Boolean isNumeric;

    /**
     * 数字类型参数的单位，非数字类型可以为空
     */
    private String unit;

    /**
     * 是否是sku通用属性，true或false
     */
    private Boolean generic;

    /**
     * 是否用于搜索过滤，true或false
     */
    private Boolean searching;

    /**
     * 数值类型参数，如果需要搜索，则添加分段间隔值，如CPU频率间隔：0.5-1.0
     */
    private String segments;

    private Date createTime;

    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}

```

> controller

分析：

- 请求方式：GET

- 请求路径：/spec/params

- 请求参数：gid，分组id

- 返回结果：该分组下的规格参数集合，不过我们依然需要定义一个DTO:

  ```java
  @Data
  public class SpecParamDTO {
      private Long id;
      private Long cid;
      private Long groupId;
      private String name;
      private Boolean isNumeric;
      private String unit;
      private Boolean generic;
      private Boolean searching;
      private String segments;
  }
  ```

  返回结果就是`List<SpecParamDTO>`


代码：

```java
/**
     * 查询 规格参数信息
     * @param gid  分组id
     * @return
     */
    @GetMapping("/params")
    public ResponseEntity<List<SpecParamDTO>> findSpecParam(
            @RequestParam(name = "gid",required = false) Long gid,
            @RequestParam(name = "cid",required = false) Long cid){
        List<SpecParamDTO> specParamDTOList = specServcie.findSpecParam(gid,cid);
        return ResponseEntity.ok(specParamDTOList);
    }
```



> service

```java
public List<SpecParamDTO> findSpecParam(Long gid,Long cid) {
        QueryWrapper<TbSpecParam> queryWrapper = new QueryWrapper<>();
        if(gid != null && gid != 0){
            queryWrapper.lambda().eq(TbSpecParam::getGroupId,gid);
        }
        if(cid != null && cid != 0){
            queryWrapper.lambda().eq(TbSpecParam::getCid,cid);
        }
        List<TbSpecParam> specParamList = specParamService.list(queryWrapper);
        if (CollectionUtils.isEmpty(specParamList)) {
            throw new LyException(ExceptionEnum.SPEC_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(specParamList,SpecParamDTO.class);

    }
```

# 🎗经验分享-添加规格组

## 1.在后台管理平台，添加规格组

![](assets/1595041898.png)

## 2.点击保存出现问题

### 1）在浏览器查看错误信息

发现POST请求返回的是500错误

![](assets/1595042224.png)

### 2）点击Preview选项

查看服务端返回的详细信息

![](assets/1595042286.png)

内容如下：

```json
error: "Internal Server Error"
message: "
↵### Error updating database.  Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near '' at line 1
↵### The error may exist in com/leyou/item/mapper/TbSpecGroupMapper.java (best guess)
↵### The error may involve com.leyou.item.mapper.TbSpecGroupMapper.insert-Inline
↵### The error occurred while setting parameters
↵### SQL: INSERT INTO tb_spec_group    VALUES
↵### Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near '' at line 1↵; bad SQL grammar []; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near '' at line 1"
path: "/spec/group"
status: 500
timestamp: "2020-07-18T03:16:22.241+0000"
```

## 3.问题的分析

### 1） 从服务端找日志

从浏览器的报错来看，状态码是500，说明是服务端的错误。

我们要到服务的日志找问题。

服务端的日志如下：

![](assets/1595042355.png)

内容如下：

```verilog
### Error updating database.  Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near '' at line 1
### The error may exist in com/leyou/item/mapper/TbSpecGroupMapper.java (best guess)
### The error may involve com.leyou.item.mapper.TbSpecGroupMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO tb_spec_group    VALUES
### Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near '' at line 1
; bad SQL grammar []; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near '' at line 1] with root cause

```

### 2） 分析服务端日志

> 分析：
>
> 看服务端的日志可以发现：
>
> insert语句   INSERT INTO tb_spec_group    VALUES   没有字段名字和保存的值，这是什么原因呢？
>
> 我们通过Debug来找，发现一个情况：  controller接收到的tbSpecGroup是null

![](assets/1595044011.png)

### 3）查看前端页面传值

发现前端传递的是 json数据

![](assets/1595042575.png)

## 4.问题解决办法

如果前端传递JSON数据，服务端Controller需要使用@RequestBody 来接收参数。

如图：

![](assets/1595044442.png)

> 修改后，再次执行保存操作发现可以正常保存数据



