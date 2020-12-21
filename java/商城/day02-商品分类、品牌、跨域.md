# 学习目标

- 独立完成商品分类查询功能
- 了解跨域问题，以及解决方案
- 了解cors解决跨域的原理
- 掌握Spring的CorsFilter使用
- 独立完成品牌查询后台代码
- 掌握axios的使用
- 了解vue中watch的使用
- 实现品牌的新增
- 实现品牌的修改

# 1.分类的查询

### 重点：清楚分类、品牌的表关系，独立完成分类查询的后端代码，并返回正确结果

商城的核心自然是商品，而商品多了以后，肯定要进行分类，并且不同的商品会有不同的品牌信息

>分类 与 商品 是【一对多】关系
>
>分类 与 品牌 是【多对多】关系，需要一张中间表来维护
>
>品牌 与 商品 是【一对多】关系

简单理解：

- 一个商品分类下有很多商品 （手机分类下有很多手机商品）
- 一个商品分类下有很多品牌（手机分类下有苹果、华为、小米）
- 一个品牌可以属于不同的分类 （华为品牌可以属于手机分类、也可以属于笔记本电脑分类）
- 一个品牌下也会有很多商品 （华为品牌下可以P30、Meta30、P40 多种商品）

## 1.1.导入数据

首先导入课前资料提供的sql：

![1556525503017](assets/1556525503017.png)

我们先看商品分类表：

 ![1525999774439](assets/1525999774439.png)

```mysql
CREATE TABLE `tb_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类目id',
  `name` varchar(20) NOT NULL COMMENT '类目名称',
  `parent_id` bigint(20) NOT NULL COMMENT '父类目id,顶级类目填0',
  `is_parent` tinyint(1) NOT NULL COMMENT '是否为父节点，0为否，1为是',
  `sort` int(4) NOT NULL COMMENT '排序指数，越小越靠前',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '数据更新时间',
  PRIMARY KEY (`id`),
  KEY `key_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1424 DEFAULT CHARSET=utf8 COMMENT='商品类目表，类目和商品(spu)是一对多关系，类目与品牌是多对多关系';
```

因为商品分类会有层级关系，因此这里我们加入了`parent_id`字段，对本表中的其它分类进行自关联。



## 1.2.页面实现

### 1.2.1.页面分析

首先我们看下要实现的效果：

![1525999250932](assets/1525999250932.png)

商品分类之间是会有层级关系的，采用树结构去展示是最直观的方式。

一起来看页面，对应的是/pages/item/Category.vue：

 ![1526000313361](assets/1526000313361.png)

页面模板：

```html
<v-card>
    <v-flex xs12 sm10>
        <v-tree url="/item/category/list"
                :isEdit="isEdit"
                @handleAdd="handleAdd"
                @handleEdit="handleEdit"
                @handleDelete="handleDelete"
                @handleClick="handleClick"
                />
    </v-flex>
</v-card>
```

- `v-card`：卡片，是vuetify中提供的组件，提供一个悬浮效果的面板，一般用来展示一组数据。

  ![1526000692741](assets/1526000692741.png)

- `v-flex`：布局容器，用来控制响应式布局。与BootStrap的栅格系统类似，整个屏幕被分为12格。我们可以控制所占的格数来控制宽度：

  ![1526001573140](assets/1526001573140.png)

  本例中，我们用`sm10`控制在小屏幕及以上时，显示宽度为10格

- `v-tree`：树组件。Vuetify并没有提供树组件，这个是我们自己编写的自定义组件：

     ![1526001762446](assets/1526001762446.png) 

  里面涉及一些vue的高级用法，大家暂时不要关注其源码，会用即可。

### 1.2.2.树组件的用法

也可参考课前资料中的：《自定义Vue组件的用法.md》



这里我贴出树组件的用法指南。

> 属性列表：

| 属性名称 | 说明                             | 数据类型 | 默认值 |
| :------- | :------------------------------- | :------- | :----- |
| url      | 用来加载数据的地址，即延迟加载   | String   | -      |
| isEdit   | 是否开启树的编辑功能             | boolean  | false  |
| treeData | 整颗树数据，这样就不用远程加载了 | Array    | -      |

这里推荐使用url进行延迟加载，**每当点击父节点时，就会发起请求，根据父节点id查询子节点信息**。

当有treeData属性时，就不会触发url加载

远程请求返回的结果格式：

```json
[
    { 
        "id": 74,
        "name": "手机",
        "parentId": 0,
        "isParent": true,
        "sort": 2
	},
     { 
        "id": 75,
        "name": "家用电器",
        "parentId": 0,
        "isParent": true,
        "sort": 3
	}
]
```



> 事件：

| 事件名称     | 说明                                       | 回调参数                                         |
| :----------- | :----------------------------------------- | :----------------------------------------------- |
| handleAdd    | 新增节点时触发，isEdit为true时有效         | 新增节点node对象，包含属性：name、parentId和sort |
| handleEdit   | 当某个节点被编辑后触发，isEdit为true时有效 | 被编辑节点的id和name                             |
| handleDelete | 当删除节点时触发，isEdit为true时有效       | 被删除节点的id                                   |
| handleClick  | 点击某节点时触发                           | 被点击节点的node对象,包含全部信息                |

> 完整node的信息

回调函数中返回完整的node节点会包含以下数据：

```json
{
    "id": 76, // 节点id
    "name": "手机", // 节点名称
    "parentId": 75, // 父节点id
    "isParent": false, // 是否是父节点
    "sort": 1, // 顺序
    "path": ["手机", "手机通讯", "手机"] // 所有父节点的名称数组
}
```

## 1.3.实现功能

### 1.3.1.url异步请求

给大家的页面中已经配置了url，我们刷新页面看看会发生什么：

```html
<v-tree url="/item/category/of/parent"
        :isEdit="isEdit"
        @handleAdd="handleAdd"
        @handleEdit="handleEdit"
        @handleDelete="handleDelete"
        @handleClick="handleClick"
        />
```

刷新页面，可以看到：

 ![1552992441349](assets/1552992441349.png)

页面发起了一条请求：

> http://api.leyou.com/api/item/category/of/parent?pid=0 



大家可能会觉得很奇怪，我们明明是使用的相对路径，讲道理发起的请求地址应该是：

http://manage.leyou.com/item/category/of/parent

但实际却是：

http://api.leyou.com/api/item/category/of/parent?pid=0 

这是因为，我们有一个全局的配置文件，对所有的请求路径进行了约定：

![1552922145908](assets/1552922145908.png) 

 ![1551274252988](assets/1551274252988.png)

路径是http://api.leyou.com/api，因此页面发起的一切请求都会以这个路径为前缀。

在config/http.js中，使用了这个属性

```
axios.defaults.baseURL = config.api; // 设置axios的基础请求路径
```

而我们的Nginx又完成了反向代理，将这个地址代理到了http://127.0.0.1:10010，也就是我们的Zuul网关，最终再被zuul转到微服务，有微服务来完成请求处理并返回结果。

因此接下来，我们要做的事情就是编写后台接口，返回对应的数据即可。



### 1.3.2.实体类

在`ly-item-service`中添加category实体类，放到entity包下，代表与数据库交互的实体类：

![1551274420088](assets/1551274420088.png) 

类的代码：

```java
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbCategory extends Model<TbCategory> {

private static final long serialVersionUID=1L;

    /**
     * 类目id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类目名称
     */
    private String name;

    /**
     * 父类目id,顶级类目填0
     */
    private Long parentId;

    /**
     * 是否为父节点，0为否，1为是
     */
    private Boolean isParent;

    /**
     * 排序指数，越小越靠前
     */
    private Integer sort;

    /**
     * 数据创建时间
     */
    private Date createTime;

    /**
     * 数据更新时间
     */
    private Date updateTime;
```



### 1.3.3.controller

编写一个controller一般需要知道四个内容：

- 请求方式：决定我们用GetMapping还是PostMapping
- 请求路径：决定映射路径
- 请求参数：决定方法的参数
- 返回值结果：决定方法的返回值

在刚才页面发起的请求中，我们就能得到绝大多数信息：

 ![1552992441349](assets/1552992441349.png)

- 请求方式：Get

- 请求路径：/api/item/category/of/parent。其中/api是网关前缀，/item是网关的路由映射，真实的路径应该是/category/of/parent

- 请求参数：pid=0，根据tree组件的说明，应该是父节点的id，第一次查询为0，那就是查询一级类目

- 返回结果：

  根据前面tree组件的用法我们知道，返回的应该是json数组：

  ```json
  [
      { 
          "id": 74,
          "name": "手机",
          "parentId": 0,
          "isParent": true,
          "sort": 2
  	},
       { 
          "id": 75,
          "name": "家用电器",
          "parentId": 0,
          "isParent": true,
          "sort": 3
  	}
  ]
  ```

  对应的java类型可以是List集合，里面的元素就是与Category数据一致的对象了。

这里返回结果中并不包含createTime和updateTime字段，所以为了符合页面需求，我们需要给页面量身打造一个用来传递的数据转移对象（Data Transfer Object），简称为DTO。

我们在`ly-item-pojo`中来创建这样的对象：

![1551275119791](assets/1551275119791.png) 

代码：

```java
@Data
public class CategoryDTO {
	private Long id;
	private String name;
	private Long parentId;
	private Boolean isParent;
	private Integer sort;
}
```

因此我们查询的返回值就是`List<CategoryDTO>`

controller代码：

```java
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private TbCategoryService categoryService;

    /**
     * 根据父类id查询 集合信息
     * @param parentId
     * @return
     */
    @GetMapping("/of/parent")
    public ResponseEntity<List<CategoryDTO>> findCategoryListByPid(@RequestParam(name = "pid")Long parentId){
        return ResponseEntity.ok(categoryService.findCategoryListByPid(parentId));
    }
}
```



### 1.3.4.service

service层实现类：

```java
/**
     * 根据父类id查询 集合信息
     * @param parentId
     * @return
     */
    @Override
    public List<CategoryDTO> findCategoryListByPid(Long parentId) {
//        select * from tb_category where parent_id = ?
//        构造查询条件
        QueryWrapper<TbCategory> queryWrapper = new QueryWrapper<>();
//        添加查询条件
        queryWrapper.eq("parent_id",parentId);
//        查询集合数据
        List<TbCategory> tbCategoryList = this.list(queryWrapper);
//        判断是否为空
        if(CollectionUtils.isEmpty(tbCategoryList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
//        转换对象并返回
        return BeanHelper.copyWithCollection(tbCategoryList,CategoryDTO.class);
    }
```



### 1.3.5.mapper

注意，我们在启动类上添加一个扫描包功能,@MapperScan：

```java
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.leyou.item.mapper") // 扫描mapper包
public class LyItem {
    public static void main(String[] args) {
        SpringApplication.run(LyItem.class, args);
    }
}
```



项目结构：

 ![1527483458604](assets/1527483458604.png)



### 1.3.6.启动并测试

我们不经过网关，直接访问：

 ![1526009785484](assets/1526009785484.png)

然后试试网关是否畅通：

  ![1526017422684](assets/1526017422684.png)

一切OK！

然后刷新页面查看：

![1526017362418](assets/1526017362418.png)

发现报错了！

浏览器直接访问没事，但是这里却报错，什么原因？

因为出现了 跨域问题！



# 2.跨域问题和解决方案

### 重点：了解什么是跨域，跨域的解决方案

### 2.1.什么是跨域

跨域是指跨域名的访问，以下情况都属于跨域：

| 跨域原因说明       | 示例                                   |
| ------------------ | -------------------------------------- |
| 域名不同           | `www.jd.com` 与 `www.taobao.com`       |
| 域名相同，端口不同 | `www.jd.com:8080` 与 `www.jd.com:8081` |
| 二级域名不同       | `item.jd.com` 与 `miaosha.jd.com`      |

注意：如果**域名和端口都相同，但是请求路径不同**，不属于跨域，如：

`www.jd.com/item` 

`www.jd.com/goods`



而我们刚才是从`manage.leyou.com`去访问`api.leyou.com`，这属于二级域名不同，跨域了。



### 2.2.为什么有跨域问题？

跨域不一定会有跨域问题。

因为跨域问题是浏览器对于ajax请求的一种安全限制：**一个页面发起的ajax请求，只能是于当前页同域名的路径**，这能有效的阻止跨站攻击。

因此：**跨域问题 是针对ajax的一种限制**。

但是这却给我们的开发带来了不变，而且在实际生成环境中，肯定会有很多台服务器之间交互，地址和端口都可能不同，怎么办？

### 2.3.解决跨域问题的方案

目前比较常用的跨域解决方案有3种：

#### 1）Jsonp

最早的解决方案，利用script标签可以跨域的原理实现。

限制：

- 需要服务的支持
- 只能发起GET请求

#### 2）nginx反向代理

思路是：利用nginx反向代理把跨域为不跨域，支持各种请求方式

缺点：需要在nginx进行额外配置，语义不清晰

#### 3）CORS

规范化的跨域请求解决方案，安全可靠。

优势：

- 在服务端进行控制是否允许跨域，可自定义规则
- 支持各种请求方式

缺点：

- 会产生额外的请求

我们这里会采用cors的跨域方案。

# 3.cors解决跨域

### 重点：

- ### 了解CORS的原理，使用Spring的CorsFilter解决跨域

- ### 会使用java配置类，并从配置类中读取配置参数

### 3.1.什么是cors

CORS是一个W3C标准，全称是"跨域资源共享"（Cross-origin resource sharing）。

它允许浏览器向跨源服务器，发出[`XMLHttpRequest`](http://www.ruanyifeng.com/blog/2012/09/xmlhttprequest_level_2.html)请求，从而克服了AJAX只能[同源](http://www.ruanyifeng.com/blog/2016/04/same-origin-policy.html)使用的限制。

CORS需要浏览器和服务器同时支持。目前，所有浏览器都支持该功能，IE浏览器不能低于IE10。

- 浏览器端：

  目前，所有浏览器都支持该功能（IE10以下不行）。整个CORS通信过程，都是浏览器自动完成，不需要用户参与。

- 服务端：

  CORS通信与AJAX没有任何差别，因此你不需要改变以前的业务逻辑。只不过，浏览器会在请求中携带一些头信息，我们需要以此判断是否运行其跨域，然后在响应头中加入一些信息即可。这一般通过过滤器完成即可。

### 3.2.cors原理

浏览器会将ajax请求分为两类，其处理方案略有差异：简单请求、特殊请求。

#### 简单请求

只要同时满足以下两大条件，就属于简单请求。：

（1) 请求方法是以下三种方法之一：

- HEAD
- GET
- POST

（2）HTTP的头信息不超出以下几种字段：

- Accept
- Accept-Language
- Content-Language
- Last-Event-ID
- Content-Type：只限于三个值`application/x-www-form-urlencoded`、`multipart/form-data`、`text/plain`



当浏览器发现发现的ajax请求是简单请求时，会在请求头中携带一个字段：`Origin`.

 ![1526019242125](assets/1526019242125.png)

Origin中会指出当前请求属于哪个域（协议+域名+端口）。服务会根据这个值决定是否允许其跨域。

如果服务器允许跨域，需要在返回的响应头中携带下面信息：

```http
Access-Control-Allow-Origin: http://manage.leyou.com
Access-Control-Allow-Credentials: true
```

- Access-Control-Allow-Origin：可接受的域，是一个具体域名或者*，代表任意
- Access-Control-Allow-Credentials：是否允许携带cookie，默认情况下，cors不会携带cookie，除非这个值是true

注意：

如果跨域请求要想操作cookie，需要满足3个条件：

- 服务的响应头中需要携带Access-Control-Allow-Credentials并且为true。
- 浏览器发起ajax需要指定withCredentials 为true
- 响应头中的Access-Control-Allow-Origin一定不能为*，必须是指定的域名

#### 特殊请求

不符合简单请求的条件，会被浏览器判定为特殊请求,，例如请求方式为PUT。

> 预检请求

特殊请求会在正式通信之前，增加一次HTTP查询请求，称为"预检"请求（preflight）。

浏览器先询问服务器，当前网页所在的域名是否在服务器的许可名单之中，以及可以使用哪些HTTP动做和头信息字段。只有得到肯定答复，浏览器才会发出正式的`XMLHttpRequest`请求，否则就报错。

一个“预检”请求的样板：

```http
OPTIONS /cors HTTP/1.1
Origin: http://manage.leyou.com
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: X-Custom-Header
Host: api.leyou.com
Accept-Language: en-US
Connection: keep-alive
User-Agent: Mozilla/5.0...
```

与简单请求相比，除了Origin以外，多了两个头：

- Access-Control-Request-Method：接下来会用到的请求方式，比如PUT
- Access-Control-Request-Headers：会额外用到的头信息

> 预检请求的响应

服务的收到预检请求，如果许可跨域，会发出响应：

```http
HTTP/1.1 200 OK
Date: Mon, 01 Dec 2008 01:15:39 GMT
Server: Apache/2.0.61 (Unix)
Access-Control-Allow-Origin: http://manage.leyou.com
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET, POST, PUT
Access-Control-Allow-Headers: X-Custom-Header
Access-Control-Max-Age: 1728000
Content-Type: text/html; charset=utf-8
Content-Encoding: gzip
Content-Length: 0
Keep-Alive: timeout=2, max=100
Connection: Keep-Alive
Content-Type: text/plain
```

除了`Access-Control-Allow-Origin`和`Access-Control-Allow-Credentials`以外，这里又额外多出3个头：

- Access-Control-Allow-Methods：允许访问的方式
- Access-Control-Allow-Headers：允许携带的头
- Access-Control-Max-Age：本次许可的有效时长，单位是秒，**过期之前的ajax请求就无需再次进行预检了**



如果浏览器得到上述响应，则认定为可以跨域，后续就跟简单请求的处理是一样的了。

### 3.3.SpringMVC实现cors

虽然原理比较复杂，但是前面说过：

- 浏览器端都有浏览器自动完成，我们无需操心
- 服务端可以通过拦截器统一实现，不必每次都去进行跨域判定的编写。

在实现代码我们需要 做的事情如下：

```
//        1.添加cors的配置信息
//          允许访问的域
//          是否允许发送cookie
//          允许的请求方式
//          允许的头信息
//          访问有效期
//       2.添加映射路径，我们拦截一切请求
//       3.返回新的CORSFilter
```



事实上，SpringMVC已经帮我们写好了CORS的跨域过滤器：CorsFilter ,内部已经实现了刚才所讲的判定逻辑，我们直接用就好了。

在`ly-api-gateway`中编写一个配置类，并且注册CorsFilter：

```java
package com.leyou.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCORSConfig {
    @Bean
    public CorsFilter corsFilter() {
        //1.添加CORS配置信息
        CorsConfiguration config = new CorsConfiguration();
        //1) 允许的域,不要写*，否则cookie就无法使用了
        config.addAllowedOrigin("http://manage.leyou.com");
        config.addAllowedOrigin("http://www.leyou.com");
        //2) 是否发送Cookie信息
        config.setAllowCredentials(true);
        //3) 允许的请求方式
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        // 4）允许的头信息
        config.addAllowedHeader("*");
        // 5）有效期 单位秒
        config.setMaxAge(3600L);
        //2.添加映射路径，我们拦截一切请求
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);
        //3.返回新的CORSFilter
        return new CorsFilter(configSource);
    }
}
```

结构：

 ![1534343900118](assets/1534343900118.png)



4.5.4.重启测试：

访问正常：

 ![1526021419016](assets/1526021419016.png)

页面也OK了：

![1526021447335](assets/1526021447335.png)



商品分类的增删改功能暂时就不做了，页面已经预留好了事件接口，有兴趣的同学可以完成一下。



### 3.4.优化

把一些属性抽取到配置文件application.yml：

```yaml
ly:
  cors:
    allowedOrigins:
      - http://manage.leyou.com
    allowedCredentials: true
    allowedHeaders:
      - "*"
    allowedMethods:
      - GET
      - POST
      - DELETE
      - PUT
      - OPTIONS
      - HEAD
    maxAge: 3600
    filterPath: "/**"
```

然后定义类，加载这些属性：

```java
@Data
@ConfigurationProperties(prefix = "ly.cors")
public class CORSProperties {
    private List<String> allowedOrigins;
    private Boolean allowedCredentials;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private Long maxAge;
    private String filterPath;
}
```

修改CORS配置类，读取属性：

```java
package com.leyou.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 服务端配置跨域的 信息
 */
@Configuration
public class GlobalCORSConfig {

    @Bean
    public CorsFilter corsFilterConfig(CORSProperties prop){
//        1.添加cors的配置信息
        CorsConfiguration config = new CorsConfiguration();
//          允许访问的域
//        config.addAllowedOrigin("http://manage.leyou.com");
        config.setAllowedOrigins(prop.getAllowedOrigins());
//          是否允许发送cookie
//        config.setAllowCredentials(true);
        config.setAllowCredentials(prop.getAllowedCredentials());
//          允许的请求方式
//        config.addAllowedMethod("GET");
//        config.addAllowedMethod("POST");
//        config.addAllowedMethod("PUT");
        config.setAllowedMethods(prop.getAllowedMethods());
//          允许的头信息
//        config.addAllowedHeader("*");
        config.setAllowedHeaders(prop.getAllowedHeaders());
//          访问有效期
//        config.setMaxAge(3600L);
        config.setMaxAge(prop.getMaxAge());
//       2.添加映射路径，我们拦截一切请求,使用自定义配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(prop.getFilterPath(),config);
//       3.返回新的CORSFilter
        return new CorsFilter(source);
    }
}

```

# 🎗经验分享

## 1.代码

> 商品分类查询界面访问是从manage.leyou.com跨域访问接口,需要在网关ly-gateway中设置CORSFilter,但修改代码后仍然无法显示分类信息
>
> 

代码如下:

```java
package com.leyou.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableConfigurationProperties(CORSProperties.class)
public class GlobalCORSConfig {
   /**
    @Bean
    public CorsFilter corsFilter() {
        //1.new一个CorsConfiguration对象,添加CORS配置信息
        CorsConfiguration config = new CorsConfiguration();
        //1) 允许的域,不要写*，否则cookie就无法使用了
        config.addAllowedOrigin("http://manage.leyou.com");
        config.addAllowedOrigin("http://www.leyou.com");
        //2) 是否发送Cookie信息
        config.setAllowCredentials(true);
        //3) 允许的请求方式
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        // 4）允许的头信息
        config.addAllowedHeader("*");
        // 5）有效期 单位秒
        config.setMaxAge(3600L);
        //2.添加映射路径，我们拦截一切请求
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);
        //3.返回新的CORSFilter
        return new CorsFilter(configSource);
    }
 **/
    @Bean
    //优化后,通过配置文件动态读取配置文件,防止硬编码
    public CorsFilter corsFilter(CORSProperties prop){
        //接受cors配置文件的对象
        CorsConfiguration config = new CorsConfiguration();
        //允许的域名
        config.setAllowedOrigins(prop.getAllowedOrigins());
        //是否允许cookie发送,true代表允许
        config.setAllowCredentials(prop.getAllowedCredentials());
        //允许的请求get put post
       config.setAllowedMethods(prop.getAllowedMethods());
        //允许的头信息
        config.setExposedHeaders(prop.getAllowedHeaders());
        //允许的有效期,以秒为单位(在有效期内,只需要在服务端注册一次,就可以使用一段时间)
        config.setMaxAge(prop.getMaxAge());  //1小时
        //configSource:添加过滤的地址,第二接受cors的配置文件
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        //返回一个CorsFilter
        configurationSource.registerCorsConfiguration(prop.getFilterPath(), config);
        return new CorsFilter(configurationSource);
    }
}
```

## 2.访问出现的问题

> 项目启动失败

![image-20200722231206185](/assets/image-20200722231206185.png) 



> 现在定位到：控制台输出报错: * is not a valid exposed header value

  

## 3.问题的分析 和 问题解决办法



> 在配置跨域之前启动时没有问题的，所以是在配置跨域时出现的问题

![image-20200722231741436](assets/image-20200722231741436.png)

然而在做优化的时候，用配置文件的方式配置跨域之后就出现了问题

![image-20200722231942126](/assets/image-20200722231942126.png)

找header相关的位置

可以看到是在设置允许的头信息时的方法调用错误，应该是setAllowedHeaders
错误的调用了setExposedHeaders方法造成的。

![image-20200722232004626](/assets/image-20200722232004626.png)

将方法修改为正确的,问题解决.

```java
package com.leyou.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableConfigurationProperties(CORSProperties.class)
public class GlobalCORSConfig {
   /**
    @Bean
    public CorsFilter corsFilter() {
        //1.new一个CorsConfiguration对象,添加CORS配置信息
        CorsConfiguration config = new CorsConfiguration();
        //1) 允许的域,不要写*，否则cookie就无法使用了
        config.addAllowedOrigin("http://manage.leyou.com");
        config.addAllowedOrigin("http://www.leyou.com");
        //2) 是否发送Cookie信息
        config.setAllowCredentials(true);
        //3) 允许的请求方式
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        // 4）允许的头信息
        config.addAllowedHeader("*");
        // 5）有效期 单位秒
        config.setMaxAge(3600L);
        //2.添加映射路径，我们拦截一切请求
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);
        //3.返回新的CORSFilter
        return new CorsFilter(configSource);
    }
 **/
    @Bean
    //优化后,通过配置文件动态读取配置文件,防止硬编码
    public CorsFilter corsFilter(CORSProperties prop){
        //接受cors配置文件的对象
        CorsConfiguration config = new CorsConfiguration();
        //允许的域名
        config.setAllowedOrigins(prop.getAllowedOrigins());
        //是否允许cookie发送,true代表允许
        config.setAllowCredentials(prop.getAllowedCredentials());
        //允许的请求get put post
       config.setAllowedMethods(prop.getAllowedMethods());
        //允许的头信息
        config.setAllowedHeaders(prop.getAllowedHeaders());
        //允许的有效期,以秒为单位(在有效期内,只需要在服务端注册一次,就可以使用一段时间)
        config.setMaxAge(prop.getMaxAge());  //1小时
        //configSource:添加过滤的地址,第二接受cors的配置文件
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        //返回一个CorsFilter
        configurationSource.registerCorsConfiguration(prop.getFilterPath(), config);
        return new CorsFilter(configurationSource);
    }
}
```



![image-20200707194121927](/assets/image-20200707194121927.png)



# 4.品牌查询-后端代码

### 重点：独立完成后台代码，并返回正确分页结果。

先看看我们要实现的效果：

![1526021968036](assets/1526021968036.png)



### 4.1.数据库表

```mysql
CREATE TABLE `tb_brand` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '品牌id',
  `name` varchar(64) NOT NULL COMMENT '品牌名称',
  `image` varchar(256) DEFAULT '' COMMENT '品牌图片地址',
  `letter` char(1) DEFAULT '' COMMENT '品牌的首字母',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=325403 DEFAULT CHARSET=utf8 COMMENT='品牌表，一个品牌下有多个商品（spu），一对多关系';
```

这里需要注意的是，品牌和商品分类之间是多对多关系。因此我们有一张中间表，来维护两者间关系：

```mysql
CREATE TABLE `tb_category_brand` (
  `category_id` bigint(20) NOT NULL COMMENT '商品类目id',
  `brand_id` bigint(20) NOT NULL COMMENT '品牌id',
  PRIMARY KEY (`category_id`,`brand_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品分类和品牌的中间表，两者是多对多关系';

```

但是，你可能会发现，这张表中并**没有设置外键约束**，似乎与数据库的设计范式不符。为什么这么做？

- 外键会影响数据库读写的效率
- 数据删除时会比较麻烦

在电商行业，性能是非常重要的。我们宁可在代码中通过逻辑来维护表关系，也不设置外键。



### 4.2.实体类

 ![1552922819904](assets/1552922819904.png)

### 4.3.controller

编写controller先思考四个问题，这次没有前端代码，需要我们自己来设定

- 请求方式：查询，肯定是Get

- 请求路径：分页查询，/brand/page

- 请求参数：根据API文档里的参数定义，我们需要5个参数：

  - page：当前页，int
  - rows：每页大小，int
  - sortBy：排序字段，String
  - desc：是否为降序，boolean
  - key：搜索关键词，String

- 响应结果：分页结果一般至少需要两个数据

  - total：总条数
  - items：当前页数据集合
  - totalPage：有些还需要总页数

  这里我们封装一个类，来表示分页结果：

  ```java
  package com.leyou.common.vo;
  
  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  
  import java.util.List;
  
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public class PageResult<T> {
      private List<T> items; // 返回结果
      private Long total; //总条数
      private Long totalPage; //总页数
  }
  
  ```
  
  另外，这个PageResult以后可能在其它项目中也有需求，因此我们将其抽取到`ly-common`中，提高复用性：
  
   ![1534373131667](assets/1534373131667.png)
  

而后，PageResult中的数据，应该是Brand。跟Category一样，我们需要定义BrandDTO在`ly-item-pojo`中：

  ```java
  package com.leyou.item.dto;
  
  import lombok.Data;
  import lombok.EqualsAndHashCode;
  import lombok.experimental.Accessors;
  
  /**
   * <p>
   * 品牌表，一个品牌下有多个商品（spu），一对多关系
   * </p>
   *
   * @author HM
   * @since 2019-12-23
   */
  @Data
  @EqualsAndHashCode(callSuper = false)
  @Accessors(chain = true)
  public class BrandDTO {
  
  
      /**
       * 品牌id
       */
      private Long id;
  
      /**
       * 品牌名称
       */
      private String name;
  
      /**
       * 品牌图片地址
       */
      private String image;
  
      /**
       * 品牌的首字母
       */
      private String letter;
  
  
  }
  
  ```

  

接下来，我们编写Controller

```java
package com.leyou.item.controller;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.BrandDTO;
import com.leyou.item.entity.TbBrand;
import com.leyou.item.service.TbBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brand")
public class BrandController {

    @Autowired
    private TbBrandService brandService;
    /**
     * 分页查询 品牌信息
     * @param key
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    @GetMapping("/page")
    public ResponseEntity<PageResult<BrandDTO>> findBrandByPage(@RequestParam(name = "key",required = false)String key,
                                                                @RequestParam(name = "page",defaultValue = "1")Integer page,
                                                                @RequestParam(name = "rows",defaultValue = "10")Integer rows,
                                                                @RequestParam(name = "sortBy",required = false)String sortBy,
                                                                @RequestParam(name = "desc",defaultValue = "false")Boolean desc){
        return ResponseEntity.ok(brandService.findBrandByPage(key,page,rows,sortBy,desc));
    }
}

```



### 4.4.Service

```java
    /**
     * 分页查询 品牌信息
     * @param key  模糊查询的关键词
     * @param page
     * @param rows
     * @param sortBy
     * @param desc
     * @return
     */
    @Override
    public PageResult<BrandDTO> findBrandByPage(String key, Integer page, Integer rows, String sortBy, Boolean desc) {
//      SELECT * FROM tb_brand WHERE NAME LIKE '%华%' OR letter LIKE '%华%' order by sortBy desc
//        构造分页的2个参数
        IPage<TbBrand> page1 = new Page<>(page,rows);
//         构造查询条件
        QueryWrapper<TbBrand> queryWrapper = new QueryWrapper<>();
//        添加条件
        if(!StringUtils.isBlank(key)){
//            queryWrapper.like("name",key).or().like("letter",key);
            queryWrapper.lambda().like(TbBrand::getName,key).or().like(TbBrand::getLetter,key);
        }
        if(!StringUtils.isBlank(sortBy)){
//            添加排序
            if(desc){
                queryWrapper.orderByDesc(sortBy);
            }else{
                queryWrapper.orderByAsc(sortBy);
            }
        }
//      分页查询
        IPage<TbBrand> brandIPage = this.page(page1, queryWrapper);
        List<TbBrand> tbBrandList = brandIPage.getRecords();
        if(CollectionUtils.isEmpty(tbBrandList)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        List<BrandDTO> brandDTOList = BeanHelper.copyWithCollection(tbBrandList, BrandDTO.class);
        return new PageResult<>(brandDTOList,brandIPage.getTotal(),brandIPage.getPages());
    }
```



### 4.5.测试

通过浏览器访问试试：http://api.leyou.com/api/item/brand/page

 ![1526047708748](assets/1526047708748.png)

接下来，我们去处理页面和ajax请求数据并渲染

# 5.品牌查询-页前端处理(了解）

## 5.1.添加一个新的VUE组件(了解)

### 了解配置左侧菜单和路由设置

为了方便看到效果，我们新建一个MyBrand.vue，从0开始搭建。

 ![1526023142926](assets/1526023142926.png)

内容初始化一下：

```vue
<template>
  <span>
    hello
  </span>
</template>

<script>
    export default {
        name: "my-brand"
    }
</script>

<style scoped>

</style>
```

改变router新的index.js，将路由地址指向MyBrand.vue

![1526023276997](assets/1526023276997.png)

打开服务器，再次查看页面：

![1526023471428](assets/1526023471428.png)



干干净净了。



## 5.2.品牌查询页面拼装(了解)

### 了解vuetifyjs的页面组件

### 5.2.1.data-tables组件

大家看到这个原型页面肯定能看出，其主体就是一个table。我们去Vuetify查看有关table的文档：

![1526023540226](assets/1526023540226.png)

仔细阅读，发现`v-data-table`中有以下核心属性：

- dark：是否使用黑暗色彩主题，默认是false

- expand：表格的行是否可以展开，默认是false

- headers：定义表头的数组，数组的每个元素就是一个表头信息对象，结构：

  ```js
  {
    text: string, // 表头的显示文本
    value: string, // 表头对应的每行数据的key
    align: 'left' | 'center' | 'right', // 位置
    sortable: boolean, // 是否可排序
    class: string[] | string,// 样式
    width: string,// 宽度
  }
  ```

- items：表格的数据的数组，数组的每个元素是一行数据的对象，对象的key要与表头的value一致

- loading：是否显示加载数据的进度条，默认是false

- no-data-text：当没有查询到数据时显示的提示信息，string类型，无默认值

- pagination.sync：包含分页和排序信息的对象，将其与vue实例中的属性关联，表格的分页或排序按钮被触发时，会自动将最新的分页和排序信息更新。对象结构：

  ```js
  {
      page: 1, // 当前页
      rowsPerPage: 5, // 每页大小
      sortBy: '', // 排序字段
      descending:false, // 是否降序
  }
  ```

- total-items：分页的总条数信息，number类型，无默认值

- select-all ：是否显示每一行的复选框，Boolean类型，无默认值

- value：当表格可选的时候，返回选中的行



我们向下翻，找找有没有看起来牛逼的案例。

找到这样一条：

![1526023837773](assets/1526023837773.png)

其它的案例都是由Vuetify帮我们对查询到的当前页数据进行排序和分页，这显然不是我们想要的。我们希望能在服务端完成对整体品牌数据的排序和分页，而这个案例恰好合适。

点击按钮，我们直接查看源码，然后直接复制到MyBrand.vue中

模板：

```vue
<template>
  <div>
    <v-data-table
      :headers="headers"
      :items="desserts"
      :pagination.sync="pagination"
      :total-items="totalDesserts"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.name }}</td>
        <td class="text-xs-right">{{ props.item.calories }}</td>
        <td class="text-xs-right">{{ props.item.fat }}</td>
        <td class="text-xs-right">{{ props.item.carbs }}</td>
        <td class="text-xs-right">{{ props.item.protein }}</td>
        <td class="text-xs-right">{{ props.item.iron }}</td>
      </template>
    </v-data-table>
  </div>
</template>
```



### 5.2.2.分析

接下来，就分析一下案例中每一部分是什么意思，搞清楚了，我们也可以自己玩了。

先看模板中table上的一些属性：

```vue
<v-data-table
              :headers="headers"
              :items="desserts"
              :pagination.sync="pagination"
              :total-items="totalDesserts"
              :loading="loading"
              class="elevation-1"
              >
</v-data-table>
```

- headers：表头信息，是一个数组

- items：要在表格中展示的数据，数组结构，每一个元素是一行

- search：搜索过滤字段，用不到，直接删除

- pagination.sync：分页信息，包含了当前页，每页大小，排序字段，排序方式等。加上.sync代表服务端排序，当用户点击分页条时，该对象的值会跟着变化。监控这个值，并在这个值变化时去服务端查询，即可实现页面数据动态加载了。

- total-items：总条数

- loading：boolean类型，true：代表数据正在加载，会有进度条。false：数据加载完毕。

  ![1526029254159](assets/1526029254159.png)



另外，在`v-data-tables`中，我们还看到另一段代码：

```vue
<template slot="items" slot-scope="props">
        <td>{{ props.item.name }}</td>
        <td class="text-xs-right">{{ props.item.calories }}</td>
        <td class="text-xs-right">{{ props.item.fat }}</td>
        <td class="text-xs-right">{{ props.item.carbs }}</td>
        <td class="text-xs-right">{{ props.item.protein }}</td>
        <td class="text-xs-right">{{ props.item.iron }}</td>
</template>
```

这段就是在渲染每一行的数据。Vue会自动遍历上面传递的`items`属性，并把得到的对象传递给这段`template`中的`props.item`属性。我们从中得到数据，渲染在页面即可。



我们需要做的事情，主要有：

- 定义表头，编写headers
  - 根据我们的表数据结构，主要是4个字段：id、name、letter、image
- 获取表内容：items 
  - 这个需要去后台查询Brand列表，可以先弄点假数据
- 获取总条数：totalItems
  - 这个也需要去后台查，先写个假的
- 定义分页对象：pagination
  - 这个值会有vuetify传给我们，我们不用管
- 数据加载进度条：loading
  - 当我们加载数据时把这个值改成true，加载完毕改成false
- 完成页面数据渲染部分 slot="items" 的那个template标签
  - 基本上把Brand的四个字段在这里渲染出来就OK了，需要跟表头（headers）对应



### 5.2.3.初步实现

我们先弄点假品牌数据，可以参考数据库表：

```json
 [
     {id: 2032, name: "OPPO", image: "1.jpg", letter: "O"},
     {id: 2033, name: "飞利浦", image: "2.jpg", letter: "F"},
     {id: 2034, name: "华为", image: "3.jpg", letter: "H"},
     {id: 2036, name: "酷派", image: "4.jpg", letter: "K"},
     {id: 2037, name: "魅族", image: "5.jpg", letter: "M"}
 ]
```

品牌中有id,name,image,letter字段。



#### 修改模板

```vue
  <div>
    <v-data-table
      :headers="headers"
      :items="brands"
      :pagination.sync="pagination"
      :total-items="totalBrands"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.name }}</td>
        <td class="text-xs-center">
          <img v-if="props.item.image" :src="props.item.image" width="130" height="40">
          <span v-else>无</span>
        </td>
        <td class="text-xs-center">{{ props.item.letter }}</td>
      </template>
    </v-data-table>
  </div>
```

我们修改了以下部分：

- items：指向一个brands变量，等下在js代码中定义
- total-items：指向了totalBrands变量，等下在js代码中定义
- template模板中，渲染了四个字段：
  - id：
  - name
  - image，注意，我们不是以文本渲染，而是赋值到一个`img`标签的src属性中，并且做了非空判断
  - letter

#### 编写数据

接下来编写要用到的数据：

```js
{
	data() {
      return {
        totalBrands: 0, // 总条数
        brands: [], // 当前页品牌数据,目前没有，下面模拟假数据
        loading: true, // 是否在加载中
        pagination: {}, // 分页信息
        headers: [ // 头信息
          {text: 'id', align: 'center', value: 'id'},
          {text: '名称', align: 'center', sortable: false, value: 'name'},
          {text: 'LOGO', align: 'center', sortable: false, value: 'image'},
          {text: '首字母', align: 'center', value: 'letter', sortable: true,}
        ]
      }
  }
}
```



#### 编写函数，初始化数据

接下来就是对brands和totalBrands完成赋值动作了。

我们编写一个函数来完成赋值，提高复用性：

```js
methods:{
    getDataFromServer(){ // 从服务的加载数据的方法。
        // 打印一句话，证明在加载
        console.log("开始加载了。。。。")
        // 开启加载
        this.loading = true;
        // 模拟延迟一段时间，随后进行赋值
        setTimeout(() => {
            // 然后赋值给brands
            this.brands = [
              {"id": 2032,"name": "OPPO", "image": "1.jpg","letter": "O"},
              {"id": 2033, "name": "飞利浦","image": "2.jpg", "letter": "F"},
              {"id": 2034,"name": "华为（HUAWEI）","image": "3.jpg","letter": "H"},
              {"id": 2036,"name": "酷派（Coolpad）","image": "4.jpg","letter": "K"},
              {"id": 2037,"name": "魅族（MEIZU）","image": "5.jpg","letter": "M"}
            ];
            // 总条数暂时写死
            this.totalBrands = 20;
            // 完成赋值后，把加载状态赋值为false
            this.loading = false;
        },400)
    }
}
```

然后使用生命周期钩子函数，在Vue实例初始化完毕后调用这个方法，这里使用created函数：

```js
created(){ // 渲染后执行
    // 查询数据
    this.getDataFromServer();
}
```



#### 完整代码

```vue
<template>
  <div>
    <v-data-table
      :headers="headers"
      :items="brands"
      :pagination.sync="pagination"
      :total-items="totalBrands"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.name }}</td>
        <td class="text-xs-center"><img :src="props.item.image"></td>
        <td class="text-xs-center">{{ props.item.letter }}</td>
      </template>
    </v-data-table>
  </div>
</template>

<script>
  export default {
    name: "my-brand",
    data() {
      return {
        totalBrands: 0, // 总条数
        brands: [], // 当前页品牌数据
        loading: true, // 是否在加载中
        pagination: {}, // 分页信息
        headers: [
          {text: 'id', align: 'center', value: 'id'},
          {text: '名称', align: 'center', sortable: false, value: 'name'},
          {text: 'LOGO', align: 'center', sortable: false, value: 'image'},
          {text: '首字母', align: 'center', value: 'letter', sortable: true,}
        ]
      }
    },
    mounted(){ // 渲染后执行
      // 查询数据
      this.getDataFromServer();
    },
    methods:{
      getDataFromServer(){ // 从服务的加载数据的方法。
        console.log(this.pagination);
        // 开启加载
        this.loading = true;
        // 模拟延迟一段时间，随后进行赋值
        setTimeout(() => {
          // 然后赋值给brands
          this.brands = [
            {"id": 2032,"name": "OPPO", "image": "1.jpg","letter": "O"},
            {"id": 2033, "name": "飞利浦","image": "2.jpg", "letter": "F"},
            {"id": 2034,"name": "华为","image": "3.jpg","letter": "H"},
            {"id": 2036,"name": "酷派","image": "4.jpg","letter": "K"},
            {"id": 2037,"name": "魅族","image": "5.jpg","letter": "M"}
          ];
          this.totalBrands = brands.length; 
          // 完成赋值后，把加载状态赋值为false
          this.loading = false;
        },400)
      }
    }
  }
</script>
```



刷新页面查看：

![1526029445561](assets/1526029445561.png)

注意，**我们上面提供的假数据，因此大家的页面可能看不到图片信息！**





### 5.2.4.优化页面

#### 编辑和删除按钮

我们将来要对品牌进行增删改，需要给每一行数据添加 修改删除的按钮，一般放到改行的最后一列：

![1526029907794](assets/1526029907794.png)

其实就是多了一列，只是这一列没有数据，而是两个按钮而已。

我们先在头（headers）中添加一列：

```js
headers: [
    {text: 'id', align: 'center', value: 'id'},
    {text: '名称', align: 'center', sortable: false, value: 'name'},
    {text: 'LOGO', align: 'center', sortable: false, value: 'image'},
    {text: '首字母', align: 'center', value: 'letter', sortable: true,},
    {text: '操作', align: 'center', value: 'id', sortable: false}
]
```

然后在模板中添加按钮：

```vue
<template slot="items" slot-scope="props">
  <td>{{ props.item.id }}</td>
  <td class="text-xs-center">{{ props.item.name }}</td>
  <td class="text-xs-center"><img :src="props.item.image"></td>
  <td class="text-xs-center">{{ props.item.letter }}</td>
  <td class="justify-center">
    编辑/删除
  </td>
</template>
```

因为不知道按钮怎么写，先放个普通文本看看：

![1526030236992](assets/1526030236992.png)

然后在官方文档中找到按钮的用法：

![1526030329303](assets/1526030329303.png)

修改我们的模板：

```vue
<template slot="items" slot-scope="props">
    <td>{{ props.item.id }}</td>
    <td class="text-xs-center">{{ props.item.name }}</td>
    <td class="text-xs-center"><img :src="props.item.image"></td>
    <td class="text-xs-center">{{ props.item.letter }}</td>
    <td class="justify-center layout">
        <v-btn color="info">编辑</v-btn>
        <v-btn color="warning">删除</v-btn>
    </td>
</template>
```

![1526030431704](assets/1526030431704.png)

#### 新增按钮

因为新增根某个品牌无关，是独立的，因此我们可以放到表格的外面：

 ![1526030663178](assets/1526030663178.png)

效果：

![1526030540341](assets/1526030540341.png)



#### 卡片（card）

为了不让按钮显得过于孤立，我们可以将按`新增按钮`和`表格`放到一张卡片（card）中。

我们去官网查看卡片的用法：

![1526031159242](assets/1526031159242.png)

卡片`v-card`包含四个基本组件：

- v-card-media：一般放图片或视频
- v-card-title：卡片的标题，一般位于卡片顶部
- v-card-text：卡片的文本（主体内容），一般位于卡片正中
- v-card-action：卡片的按钮，一般位于卡片底部

我们可以把`新增的按钮`放到`v-card-title`位置，把`table`放到下面，这样就成一个上下关系。

```vue
  <v-card>
    <!-- 卡片的头部 -->
    <v-card-title>
      <v-btn color="primary">新增</v-btn>
    </v-card-title>
    <!-- 分割线 -->
    <v-divider/>
    <!--卡片的中部-->
    <v-data-table
      :headers="headers"
      :items="brands"
      :pagination.sync="pagination"
      :total-items="totalBrands"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.name }}</td>
        <td class="text-xs-center"><img :src="props.item.image"></td>
        <td class="text-xs-center">{{ props.item.letter }}</td>
        <td class="justify-center layout">
          <v-btn color="info">编辑</v-btn>
          <v-btn color="warning">删除</v-btn>
        </td>
      </template>
    </v-data-table>
  </v-card>
```

效果：

![1526031720583](assets/1526031720583.png)

#### 添加搜索框

我们还可以在卡片头部添加一个搜索框，其实就是一个文本输入框。

查看官网中，文本框的用法：

![1526031897445](assets/1526031897445.png)

- name：字段名，表单中会用到
- label：提示文字
- value：值。可以用v-model代替，实现双向绑定



修改模板，添加输入框：

```html
<v-card-title>
    <v-btn color="primary">新增品牌</v-btn>
    <!--搜索框，与search属性关联-->
     <v-text-field label="输入关键字搜索" append-icon="search" v-model="search"/>
</v-card-title>
```

需要在data中添加属性：search

 ![1534374295695](assets/1534374295695.png)

效果：

![1526032177687](assets/1526032177687.png)

发现输入框变的超级长！！！

这个时候，我们可以使用Vuetify提供的一个空间隔离工具：

![1526032321057](assets/1526032321057.png)

修改代码：

```html
<v-card-title>
        <v-btn color="info">新增品牌</v-btn>
         <v-spacer/>
         <v-flex xs3>
            <v-text-field label="输入关键字搜索" append-icon="search" v-model="search"/>
        </v-flex>
        </v-card-title>
```



![1526032398630](assets/1526032398630.png)



#### 给搜索框添加搜索图标

查看textfiled的文档，发现：

 ![1526033007616](assets/1526033007616.png)

通过append-icon属性可以为 输入框添加后置图标，所有可用图标名称可以到 [material-icons官网](https://material.io/tools/icons/)去查看。

修改我们的代码：

```html
<v-text-field label="输入关键字搜索" v-model="search" append-icon="search"/>
```

![1526033167381](assets/1526033167381.png)



#### 把文本框变紧凑

搜索框看起来高度比较高，页面不够紧凑。这其实是因为默认在文本框下面预留有错误提示空间。通过下面的属性可以取消提示：

![1526033439890](assets/1526033439890.png)

修改代码：

```html
<v-text-field label="输入关键字搜索" v-model="search" append-icon="search" hide-details/>
```

效果：

![1526033500219](assets/1526033500219.png)

几乎已经达到了原来一样的效果了吧！

## 5.3.异步查询工具axios(回顾)

### 重点：回忆axios的使用方式

异步查询数据，自然是通过ajax查询，这里我们是axios来做ajax请求。

axios的说明文档：

https://www.kancloud.cn/yunye/axios/234845

### 5.3.1.axios回顾

axios是Vue官方推荐的ajax请求框架，看下demo：

 ![1526033988251](assets/1526033988251.png)

axios支持Http的所有7种请求方式，并且有对应的方法如：Get、POST与其对应。另外这些方法最终返回的是一个Promise，对异步调用进行封装。因此，我们可以用.then() 来接收成功时回调，.catch()完成失败时回调



axios的Get请求语法：

```js
axios.get("/item/category/list?pid=0") // 请求路径和请求参数拼接
    .then(function(resp){
    	// 成功回调函数
	})
    .catch(function(){
    	// 失败回调函数
	})
// 参数较多时，可以通过params来传递参数
axios.get("/item/category/list", {
        params:{
            pid:0
        }
	})
    .then(function(resp){})// 成功时的回调
    .catch(function(error){})// 失败时的回调
```

axios的POST请求语法：

比如新增一个用户

```js
axios.post("/user",{
    	name:"Jack",
    	age:21
	})
    .then(function(resp){})
    .catch(function(error){})
```

- 注意，POST请求传参，不需要像GET请求那样定义一个对象，在对象的params参数中传参。post()方法的第二个参数对象，就是将来要传递的参数

PUT和DELETE请求与POST请求类似

### 5.3.2.axios的全局配置

而在我们的项目中，已经引入了axios，并且进行了简单的封装，在src下的http.js中：

![1552922899940](assets/1552922899940.png) 

http.js中对axios进行了一些默认配置：

```js
import Vue from 'vue'
import axios from 'axios'
import config from './config'
// config中定义的基础路径是：http://api.leyou.com/api
axios.defaults.baseURL = config.api; // 设置axios的基础请求路径
axios.defaults.timeout = 2000; // 设置axios的请求时间

Vue.prototype.$http = axios;// 将axios赋值给Vue原型的$http属性，这样所有vue实例都可使用该对象

```

- http.js中导入了config的配置，还记得吗？

  ![1551274252988](assets/1551274252988.png)

- http.js对axios进行了全局配置：`baseURL=config.api`，即`http://api.leyou.com/api`。因此以后所有用axios发起的请求，都会以这个地址作为前缀。

- 通过`Vue.property.$http = axios`，将`axios`赋值给了 Vue原型中的`$http`。这样以后所有的Vue实例都可以访问到$http，也就是访问到了axios了。

### 5.3.3.测试一下：

我们在组件`MyBrand.vue`的getDataFromServer方法，通过$http发起get请求，测试查询品牌的接口，看是否能获取到数据：

   ![1526048221750](assets/1526048079191.png)

网络监视：

 ![1526048143014](assets/1526048143014.png)

控制台结果：

![1526048275064](assets/1526048275064.png)

可以看到，在请求成功的返回结果response中，有一个data属性，里面就是真正的响应数据。

响应结果中与我们设计的一致，包含3个内容：

- total：总条数，目前是165
- items：当前页数据
- totalPage：总页数，我们没有返回



## 5.4.异步加载品牌数据

### 重点：使用axios发送ajax的请求，并接收处理返回内容

虽然已经通过ajax请求获取了品牌数据，但是刚才的请求没有携带任何参数，这样显然不对。我们后端接口需要5个参数：

- page：当前页，int
- rows：每页大小，int
- sortBy：排序字段，String
- desc：是否为降序，boolean
- key：搜索关键词，String

而页面中分页信息应该是在pagination对象中，我们通过浏览器工具，查看pagination中有哪些属性：

 ![](assets/1526042136135.png)

分别是：

- descending：是否是降序，对应请求参数的desc
- page：当前页，对应参数的page
- rowsPerpage：每页大小，对应参数中的rows
- sortBy：排序字段，对应参数的sortBy

缺少一个搜索关键词，这个应该是通过v-model与输入框绑定的属性：search。这样，所有参数就都有了。

另外，不要忘了把查询的结果赋值给brands和totalBrands属性，Vuetify会帮我们渲染页面。

接下来，我们在`getDataFromServer`方法中完善请求参数：

```js
// 发起请求
this.$http.get("/item/brand/page",{
        params:{
            key: this.search, // 搜索条件
            page: this.pagination.page,// 当前页
            rows: this.pagination.rowsPerPage,// 每页大小
            sortBy: this.pagination.sortBy,// 排序字段
            desc: this.pagination.descending// 是否降序
        }
    }).then(resp => { // 这里使用箭头函数
        // 将得到的数据赋值给本地属性
        this.brands = resp.data.items;
        this.totalBrands = resp.data.total;
        // 完成赋值后，把加载状态赋值为false
        this.loading = false;
    })
```

查看网络请求：

 ![1526049810351](assets/1526049810351.png)

效果：

![1526049139244](assets/1526049139244.png)



## 5.5.完成分页和过滤

### 重点：使用watch和深度监控

### 5.5.1.分页

现在我们实现了页面加载时的第一次查询，你会发现你点击分页并没发起请求，怎么办？

虽然点击分页，不会发起请求，但是通过浏览器工具查看，会发现pagination对象的属性一直在变化：

 ![](assets/9.gif)

我们可以利用Vue的监视功能：watch，来监视pagination 对象。

当pagination对象中的属性发生改变时，调用我们的回调函数，我们在回调函数中进行数据的查询即可！

由于我们要监控的是pagination对象内的属性，这里需要使用【深度监控】

具体实现：

![1526049643506](assets/1526049643506.png)



刷新页面测试，成功实现分页功能：

![1526049720200](assets/1526049720200.png)



### 5.5.2.过滤

分页实现了，过滤也很好实现了。过滤字段对应的是search属性，我们只要监视这个属性即可:

 ![1526049939985](assets/1526049939985.png)

查看网络请求：

 ![1526050032436](assets/1526050032436.png)

页面结果：

![1526050071442](assets/1526050071442.png)



## 3.6.完整代码

```vue
<template>
  <v-card>
    <v-card-title>
      <v-btn color="primary" @click="addBrand">新增品牌</v-btn>
      <!--搜索框，与search属性关联-->
      <v-spacer/>
      <v-text-field label="输入关键字搜索" v-model.lazy="search" append-icon="search" hide-details/>
    </v-card-title>
    <v-divider/>
    <v-data-table
      :headers="headers"
      :items="brands"
      :pagination.sync="pagination"
      :total-items="totalBrands"
      :loading="loading"
      class="elevation-1"
    >
      <template slot="items" slot-scope="props">
        <td>{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.name }}</td>
        <td class="text-xs-center"><img :src="props.item.image"></td>
        <td class="text-xs-center">{{ props.item.letter }}</td>
        <td class="justify-center layout">
          <v-btn color="info">编辑</v-btn>
          <v-btn color="warning">删除</v-btn>
        </td>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
  import MyBrandForm from './MyBrandForm'
  export default {
    name: "my-brand",
    data() {
      return {
        search: '', // 搜索过滤字段
        totalBrands: 0, // 总条数
        brands: [], // 当前页品牌数据
        loading: true, // 是否在加载中
        pagination: {}, // 分页信息
        headers: [
          {text: 'id', align: 'center', value: 'id'},
          {text: '名称', align: 'center', sortable: false, value: 'name'},
          {text: 'LOGO', align: 'center', sortable: false, value: 'image'},
          {text: '首字母', align: 'center', value: 'letter', sortable: true,},
          {text: '操作', align: 'center', value: 'id', sortable: false}
        ]
      }
    },
    mounted() { // 渲染后执行
      // 查询数据
      this.getDataFromServer();
    },
    watch: {
      pagination: { // 监视pagination属性的变化
        deep: true, // deep为true，会监视pagination的属性及属性中的对象属性变化
        handler() {
          // 变化后的回调函数，这里我们再次调用getDataFromServer即可
          this.getDataFromServer();
        }
      },
      search: { // 监视搜索字段
        handler() {
          this.getDataFromServer();
        }
      }
    },
    methods: {
      getDataFromServer() { // 从服务的加载数的方法。
        // 发起请求
        this.$http.get("/item/brand/page", {
          params: {
            key: this.search, // 搜索条件
            page: this.pagination.page,// 当前页
            rows: this.pagination.rowsPerPage,// 每页大小
            sortBy: this.pagination.sortBy,// 排序字段
            desc: this.pagination.descending// 是否降序
          }
        }).then(resp => { // 这里使用箭头函数
          this.brands = resp.data.items;
          this.totalBrands = resp.data.total;
          // 完成赋值后，把加载状态赋值为false
          this.loading = false;
        })
      }
    }
  }
</script>

<style scoped>

</style>

```



# 6.品牌的新增

品牌新增的功能，我们就不再编写页面了，使用课前资料中给出的页面即可。

## 重点：独立完成品牌新增的后台代码。

## 6.1 页面请求

点击页面`品牌管理`按钮，可以看到品牌的列表页面：

![1552138315445](assets/1552138315445.png)

此时点击新增品牌按钮，即可看到品牌新增的页面：

![1552139501851](assets/1552139501851.png)

填写基本信息，此时，点击提交按钮，可以看到页面已经发出请求：

![1552139565844](assets/1552139565844.png)



## 6.2.后台实现新增

### 6.2.1.controller

还是一样，先分析四个内容：

- 请求方式：刚才看到了是POST
- 请求路径：/brand
- 请求参数：brand对象的三个属性，可以用BrandDTO接收，外加商品分类的id数组cids
- 返回值：无

代码：

```java
/**
     * 新增品牌
     * @param brand
     * @param ids
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> saveBrand(TbBrand brand,@RequestParam(name = "cids") List<Long> ids){
        brandService.saveBrand(brand,ids);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
```



### 6.2.2.Service

这里要注意，我们不仅要新增品牌，还要维护品牌和商品分类的中间表。

```java
/**
     * 新增品牌
     * @param brand
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBrand(TbBrand brand, List<Long> ids) {
//        保存品牌表
        boolean b = this.save(brand);
        if(!b){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
//        获取品牌id
        Long brandId = brand.getId();
//        保存中间表

        List<TbCategoryBrand> categoryBrandList = new ArrayList<>();
        for (Long categoryId : ids) {
            TbCategoryBrand tbCategoryBrand = new TbCategoryBrand();
            tbCategoryBrand.setBrandId(brandId);
            tbCategoryBrand.setCategoryId(categoryId);
            categoryBrandList.add(tbCategoryBrand);
        }
        boolean b1 = categoryBrandService.saveBatch(categoryBrandList);
        if(!b1){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }
```

# 7.品牌的修改

## 重点：独立完成品牌回显和修改

## 7.1.数据回显

修改一般都需要先回显，我们来看看品牌的修改如何完成

### 7.1.1.页面请求

点击品牌页面后面的 `编辑`按钮：

![1552480477888](assets/1552480477888.png)

可以看到并没有弹出修改品牌的页面，而是在控制台发起了一个请求：

![1552992682525](assets/1552992682525.png) 

这里显然是要查询品牌下对应的分类信息，因为品牌的table中已经有了品牌数据，缺少的恰好是品牌相关联的商品分类信息。



### 7.1.2.后台查询分类

> controller实现

首先分析一下四个条件：

- 请求方式：Get
- 请求路径：/category/list/of/brand
- 请求参数：id，这里的值应该是品牌的id，因为是根据品牌查询分类
- 返回结果：一个品牌对应多个分类， 应该是分类的集合List<Category>

具体代码CategoryController.java：

```java
/**
     * 根据品牌id 查询分类集合
     * @param brandId
     * @return
     */
    @GetMapping("/of/brand")
    public ResponseEntity<List<CategoryDTO>> findCategoryListByBrandId(@RequestParam(name = "id")Long brandId){
        return ResponseEntity.ok(categoryService.findCategoryListByBrandId(brandId));
    }
```



> Service

业务层代码没有什么特殊的，只是调用的mapper方法是自定义方法，因为有中间表：

```java
    /**
     * 根据品牌id 查询分类集合
     * @param brandId
     * @return
     */
    @Override
    public List<CategoryDTO> findCategoryListByBrandId(Long brandId) {
//        2表联查，根据品牌id 返回分类集合
        List<TbCategory> tbCategoryList = this.getBaseMapper().selectCategoryByBrandId(brandId);
        if(CollectionUtils.isEmpty(tbCategoryList)){
            throw new LyException(ExceptionEnum.CATEGORY_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(tbCategoryList,CategoryDTO.class);
    }
```



> mapper

这里定义mapper没有采用xml定义，而是用了注解方式：

```java
/**
     * 根据品牌id  ，2表联查获取分类集合
     * 分类表、品牌分类对照表
     * @param brandId
     * @return
     */
    @Select("SELECT a.* FROM tb_category a , tb_category_brand  b WHERE a.id = b.category_id AND b.brand_id=#{brandId}")
    List<TbCategory> selectCategoryByBrandId(@Param("brandId") Long brandId);
```



再次点击回显按钮，查看效果，发现回显成功：

![1552482952384](assets/1552482952384.png)

## 7.2.修改品牌

### 7.2.1.页面请求

我们修改一些数据，然后再次点击提交，看看页面的反应：

![1552483902099](/assets/1552483902099.png)



分析：

- 请求方式：PUT
- 请求路径：brand
- 请求参数：与新增时类似，但是多了id属性
- 返回结果：新增应该是void

### 7.2.2.后端代码

首先是controller

```java
/**
     * 修改品牌
     * @param brand
     * @param ids
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> updateBrand(TbBrand brand,@RequestParam(name = "cids")List<Long> ids){
        brandService.updateBrand(brand,ids);
        return ResponseEntity.noContent().build();
    }
```

几乎与新增的controller一样。

然后是service，需要注意的是，业务逻辑并不是简单的修改就可以了，因为还有中间表要处理，此处中间表因为没有其它数据字段，只包含分类和品牌的id，建议采用先删除，再新增的策略

```java
/**
     * 修改品牌
     * @param brand
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBrand(TbBrand brand, List<Long> ids) {
        Long brandId = brand.getId();
//      修改品牌表
        boolean b = this.updateById(brand);
        if(!b){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
//        删除中间表数据
//        delete from tb_category_brand where brand_id=?'
        QueryWrapper<TbCategoryBrand> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TbCategoryBrand::getBrandId,brandId);
        boolean b1 = categoryBrandService.remove(queryWrapper);
        if(!b1){
            throw new LyException(ExceptionEnum.DELETE_OPERATION_FAIL);
        }
//        新增中间表数据
        List<TbCategoryBrand> categoryBrandList = new ArrayList<>();
        for (Long categoryId : ids) {
            TbCategoryBrand tbCategoryBrand = new TbCategoryBrand();
            tbCategoryBrand.setCategoryId(categoryId);
            tbCategoryBrand.setBrandId(brandId);
            categoryBrandList.add(tbCategoryBrand);
        }
        boolean b2 = categoryBrandService.saveBatch(categoryBrandList);
        if(!b2){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }
```
