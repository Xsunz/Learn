# 学习目标

- 了解Thymeleaf的基本使用
- 实现商品详情页动态页面
- 实现商品详情页面静态化
- 了解RocketMQ的基本使用
- 了解RocketMQ的消息
- 了解SpringBoot整合RocketMQ



# 1、商品详情-Thymeleaf入门

### 重点：了解Thymeleaf使用，实现静态页面的生成

## 1.1 商品详情实现思路

当用户搜索到商品，肯定会点击查看，就会进入商品详情页，接下来我们完成商品详情页的展示，商品详情页在leyou-portal中对应的页面是：item.html

但是不同的商品，到达item.html需要展示的内容不同，该怎么做呢？

- 思路1：统一跳转到item.html页面，然后异步加载商品数据，渲染页面
- 思路2：将请求交给tomcat处理，在服务端完成数据渲染，给不同商品生成不同页面后，返回给用户

我们该选哪一种思路？

思路1：

- 优点：页面加载快，异步处理，用户体验好
- 缺点：会向服务端发起多次数据请求，增加服务端压力

思路2：

- 优点：服务端处理页面后返回，用户拿到是最终页面，不会再次向服务端发起数据请求。
- 缺点：在服务端处理页面，服务端压力过大，tomcat并发能力差



对于大型电商网站而言，必须要考虑的就是服务的高并发问题，因此要尽可能减少服务端压力，提高服务响应速度，所以这里我们两个方案都不会用，我们采用方案3：

方案3：页面静态化

页面静态化：顾名思义，就是把本来需要动态渲染的页面提前渲染完成，生成静态的HTML，当用户访问时直接读取静态HTML，提高响应速度，减轻服务端压力。

这种方式与方案2类似，都是要为每个商品生成独立页面，不同之处在于方案2需要每次都重新渲染，而静态化不需要这么频繁。不过，其中还有很多细节问题需要解决。

我们可以先实现方式2的方式，再来对比两者区别，改造为页面静态化方案。



后两种方案都是在服务端完成页面渲染，以前服务端渲染我们都使用的JSP，不过在SpringBoot中已经不推荐使用Jsp了，因此我们会使用另外的模板引擎技术：Thymeleaf

与其类似的模板渲染技术有：Freemarker、Velocity等都可以。

## 1.2 Thymeleaf入门

在商品详情页中，我们会使用到Thymeleaf来渲染页面，如果需要先了解Thymeleaf的语法。详见课前资料中《Thymeleaf语法入门.md》



## 1.3 商品详情页服务

我们创建一个微服务，用来完成商品详情页的渲染。

### 1.3.1 创建module

商品的详情页服务，命名为：`ly-page`

![1553221580631](assets/1553221580631.png)

目录：

![1553221600298](assets/1553221600298.png) 

### 1.3.2 pom依赖

```XML
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

    <artifactId>ly-page</artifactId>

    <dependencies>
        <!--eureka-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <!--web-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!--thymeleaf-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <!--feign-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <!-- 商品服务接口 -->
        <dependency>
            <groupId>com.leyou</groupId>
            <artifactId>ly-item-interface</artifactId>
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

### 1.3.3 编写启动类

```java
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class LyPageApplication {
    public static void main(String[] args) {
        SpringApplication.run(LyPageApplication.class, args);
    }
}
```

### 1.3.4 application.yml

```yaml
server:
  port: 8084
spring:
  application:
    name: page-service
    #把thymeleaf 缓存关闭
  thymeleaf:
    cache: false
eureka:
  client:
    service-url:
      defaultZone: http://127.0.0.1:10086/eureka
```



### 1.3.5 页面模板

我们从课前资料中复制item.html模板：

 ![1535356585076](assets/1535356585076.png)

到当前项目resource目录下的template中：

  ![1535356618281](assets/1535356618281.png)



## 1.4 页面跳转

### 1.4.1 修改页面跳转路径

首先我们需要修改搜索结果页的商品地址，目前所有商品的地址都是：http://www.leyou.com/item.html

 ![1526955707685](assets/1526955707685.png)

我们应该跳转到对应的商品的详情页才对。

那么问题来了：商品详情页是一个SKU？还是多个SKU的集合？

![1526955852490](assets/1526955852490.png)

通过详情页的预览，我们知道它是多个SKU的集合，即SPU。

所以，页面跳转时，我们应该携带SPU的id信息。

例如：http://www.leyou.com/item/2314123.html

这里就采用了路径占位符的方式来传递spu的id，我们打开`search.html`，修改其中的商品路径：

 ![1526972476737](assets/1526972476737.png)

刷新页面后在看：

 ![1526972581134](assets/1526972581134.png)

### 1.4.2 nginx反向代理

接下来，我们要把这个地址指向我们刚刚创建的服务：`ly-page`，其端口为8084

我们在nginx.conf中添加一段逻辑：

```nginx
server {
	listen       80;
	server_name  www.leyou.com;
	location /item {
		proxy_pass	http://127.0.0.1:8084;
	}
	location / {
		proxy_pass   http://leyou-portal;
	}
    
}
```

把以/item开头的请求，代理到我们的8084端口

### 1.4.3 编写跳转controller

在`ly-page`中编写controller，接收请求，并跳转到商品详情页：

```java
@Controller
public class PageController {

    /**
     * 跳转到商品详情页
     * @param model
     * @param id
     * @return
     */
    @GetMapping("item/{id}.html")
    public String toItemPage(Model model, @PathVariable("id")Long id){

        return "item";
    }
}
```



### 1.4.4 测试

启动`ly-page`，点击搜索页面商品，看是能够正常跳转：

![1535422129984](assets/1535422129984.png)

现在看到是500，因为Thymeleaf模板渲染失败了，缺少模板渲染需要的Model数据：

![1535422177254](assets/1535422177254.png)

因此接下来，必须提供Model数据，完成页面渲染。

## 1.5 完成页面渲染

### 1.5.1 页面数据分析

首先我们一起来分析一下，在这个页面中需要哪些数据，这个可以查看`item.html`中的Thymeleaf语法得知，例如有下面的部分：

![1553245845879](assets/1553245845879.png)

逐个查看，发现需要下面的变量：

- categories：商品分类对象集合
- brand：品牌对象
- spuName：应该 是spu表中的name属性
- subTitle：spu中 的副标题
- detail：商品详情SpuDetail
- skus：商品spu下的sku集合
- specs：规格参数这个比较 特殊：
  - ![1553246049965](assets/1553246049965.png)
  - 通过上述代码，可以知道specs是规格组的集合，组内要包含params参数，就是该组下的规格参数的集合



我们已知的条件是传递来的spu的id，因此我们需要下面的一些查询接口：

- 根据id查询spu，最好同时查询出spuDetail和skus，这样比分三次查询效率较高。（没有这样的接口）
- 根据品牌id查询品牌（有）
- 根据分类id集合查询商品分类集合（有）
- 根据分类id查询规格组及组内规格参数没有这样的接口）

因此接下来，我们需要在商品微服务补充2个接口。

### 1.5.2 商品微服务提供接口

#### 查询spu接口

以上所需数据中，查询spu的接口目前还没有，我们需要在商品微服务中提供这个接口：

首先在`ly-item-interface`中对外提供的Feign客户端（`ItemClient`）中定义接口：

> ItemClient

```java
/**
 * 根据spu的id查询spu
 * @param id
 * @return
 */
@GetMapping("spu/{id}")
SpuDTO findSpuById(@PathVariable("id") Long id);
```

> GoodsController

```java
/**
     * 根据spu的id查询spu
     * @param id
     * @return
     */
@GetMapping("spu/{id}")
public ResponseEntity<SpuDTO> findSpuById(@PathVariable("id") Long id){
    return ResponseEntity.ok(goodsService.findSpuById(id));
}
```

> GoodsService

```java
public SpuDTO findSpuById(Long spuId) {
        TbSpu tbSpu = spuService.getById(spuId);
        if(tbSpu == null){
            throw new LyException(ExceptionEnum.GOODS_NOT_FOUND)
        }
        return BeanHelper.copyProperties(tbSpu,SpuDTO.class);
    }
```

#### 查询规格参数组

我们在页面展示规格时，需要按组展示：

 ![1535423451465](assets/1535423451465.png)

组内有多个参数，为了方便展示。我们提供一个接口，查询规格组，同时在规格组中持有组内的所有参数。

> 拓展`SpecGroupDTO`类：

我们在`SpecGroupDTO`中添加一个`SpecParamDTO`的集合，保存改组下所有规格参数

```java
@Data
public class SpecGroupDTO {
    private Long id;

    private Long cid;

    private String name;
    
    private List<SpecParamDTO> params;
}
```

在`ly-item-interface`中对外提供的Feign客户端（`ItemClient`）中定义接口：

> ItemClient

```java
/**
     * 查询规格参数组，及组内参数
     * @param id 商品分类id
     * @return 规格组及组内参数
     */
@GetMapping("/spec/of/category")
List<SpecGroupDTO> findSpecsByCid(@RequestParam("id") Long id);
```

> SpecController

```java
/**
     * 查询规格参数组，及组内参数
     * @param id 商品分类id
     * @return 规格组及组内参数
     */
@GetMapping("/of/category")
public ResponseEntity<List<SpecGroupDTO>> findSpecsByCid(@RequestParam("id") Long id){
    return ResponseEntity.ok(specService.findSpecsByCid(id));
}
```

> SpecificationService

```java
public List<SpecGroupDTO> findSpecsByCid(Long cid) {
        // 查询规格组
        List<SpecGroupDTO> groupList = findSpecGroupByCategory(cid);
        // 查询分类下所有规格参数
        List<SpecParamDTO> params = findSpecParmams(null, cid, null);
        // 将规格参数按照groupId进行分组，得到每个group下的param的集合
        Map<Long, List<SpecParamDTO>> paramMap = params.stream()
                .collect(Collectors.groupingBy(SpecParamDTO::getGroupId));
        // 填写到group中
        for (SpecGroupDTO groupDTO : groupList) {
            groupDTO.setSpecParamDTOList(paramMap.get(groupDTO.getId()));
        }
        return groupList;
    }
```

在service中，我们调用之前编写过的方法，查询规格组，和规格参数，然后封装返回。

### 1.5.3 完善controller

在之前的页面跳转controller中，缺乏模型数据，导致页面渲染失败 。我们需要准备好数据，存入Model中，传递给页面模板。

修改controller代码：

```java
@Controller
public class PageController {
    
    @Autowired
    private PageService pageService;

    /**
     * 跳转到商品详情页
     * @param model
     * @param id
     * @return
     */
    @GetMapping("item/{id}.html")
    public String toItemPage(Model model, @PathVariable("id")Long id){
        // 查询模型数据
        Map<String,Object> itemData = pageService.loadItemData(id);
        // 存入模型数据，因为数据较多，直接存入一个map
        model.addAllAttributes(itemData);
        return "item";
    }
}
```



注意我们把数据的查询和加载放到了一个PageService中去完成，数据较多，因此这里的PageService方法返回的是一个Map。



### 1.5.4 封装Model数据

我们创建一个PageService，在里面来封装数据模型。

> Service代码

```java
package com.leyou.page.service;

import com.leyou.item.client.ItemClient;
import com.leyou.item.dto.BrandDTO;
import com.leyou.item.dto.CategoryDTO;
import com.leyou.item.dto.SpecGroupDTO;
import com.leyou.item.dto.SpuDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class PageService {

    @Autowired
    private ItemClient itemClient;

/**
     * 获取模板页面需要的动态数据
     * @param spuId
     * @return
     */
    public Map<String, Object> loadData(Long spuId) {
        Map<String, Object> map = new HashMap<>();
//        根据spuid 获取spu对象
        SpuDTO spuDTO = itemClient.findSpuById(spuId);
//      categories 分类的集合
        List<CategoryDTO> categoryDTOList = itemClient.findCategoryListByIds(spuDTO.getCategoryIds());
//      brand 品牌对象
        BrandDTO brandDTO = itemClient.findBrandById(spuDTO.getBrandId());
//      detail 商品详情对象  spu_detail的数据
        SpuDetailDTO spuDetailDTO = itemClient.findSpuDetailBySpuId(spuId);
//        skus spu对应的sku集合
        List<SkuDTO> skuDTOList = itemClient.findSkuListBySpuId(spuId);
//        specs 商品对应的规格参数组，名字，值
        List<SpecGroupDTO> specGroupDTOList = itemClient.findSpecGorupList(spuDTO.getCid3());
        map.put("categories",categoryDTOList);
        map.put("brand",brandDTO);
        map.put("spuName",spuDTO.getName());
        map.put("subTitle",spuDTO.getSubTitle());
        map.put("detail",spuDetailDTO);
        map.put("skus",skuDTOList);
        map.put("specs",specGroupDTOList);
        return map;
    }
```



### 1.5.5 页面测试数据

页面的数据渲染代码已经帮大家写好了，因此启动项目可以直接看到最终的效果：

![1535357366958](assets/1535357366958.png)



## 🎗经验分享-模板报错

### 1、需求

> 在ly-page工程，导入商品详情模板item.html。并编写controller，编写service远程调用item服务获取商品详情信息。代码如下：

controller代码：

```java
    /***
     * 获取商品信息
     * @param spuId
     * @param model
     * @return
     */
    @GetMapping("/item/{id}.html")
    public String loadItemData(@PathVariable(name = "id") Long spuId, Model model){
//        获取spuid 对应的商品信息
        Map<String,Object> itemData = pageService.loadData(spuId);
        //把商品信息放入model
        model.addAllAttributes(itemData);
        //使用item.html模板
        return "item";
    }
```

service代码：

```java
 /**
     * 获取商品信息
     * @param spuId
     * @return
     */
    public Map<String, Object> loadData(Long spuId) {
//        根据spuid 获取spu对象
        SpuDTO spuDTO = itemClient.findSpuById(spuId);
//        远程调用，item服务，获取商品对应分类的3级对象
        List<CategoryDTO> categoryListByIds = itemClient.findCategoryListByIds(spuDTO.getCategoryIds());
//        获取品牌对象
        BrandDTO brandDTO = itemClient.findBrandById(spuDTO.getBrandId());
//        根据spuid 查询spuDetail的数据
        SpuDetailDTO spuDetailDTO = itemClient.findSpuDetailBySpuId(spuId);
//        根据spuid 查询sku集合
        List<SkuDTO> skuDTOList = itemClient.findSkuListBySpuId(spuId);
//        获取规格参数组和组内的规格参数
        List<SpecGroupDTO> specGroupList = itemClient.findSpecGroup(spuDTO.getCid3());

        Map<String,Object> data = new HashMap<>();
        data.put("categories",categoryListByIds);
        data.put("brand",brandDTO);
        data.put("spuName",spuDTO.getName());
        data.put("subTitle",spuDTO.getSubTitle());
        data.put("detail",spuDetailDTO);
        data.put("skus",skuDTOList);
        data.put("specs",specGroupList);
        return data;
    }
```

### 2、问题描述

> 然后访问controller，去看页面，发现页面出现错误，**价格不显示，特有规格属性**

如图：

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1595751022.png)



> 然后在看规格参数，发现规格参数也不显示

如图：

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1595751401.png)

### 3、问题分析

> 我们发现页面可以显示，但是价格和特有规格属性，以及规格包装内容都不显示。
>
> 我们来分析原因：
>
> 首先如果页面可以显示，说明controller是可以访问的。显示的页面是模板，页面能显示说明模板也没问题。
>
> 那么有可能就是数据有问题。



> 现在我们来看chrome浏览器的信息，发现有forEach的报错

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1595752208.png)

> 然后我们点击这个错误，可以看到在源代码的错误

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1595752310.png)

> 可以发现 group.params.forEach() 这个方法报错了，也就是params.forEach有错，params是null。
>
> 这个params 是group里面的params，也就是从规格组中获取的规格参数。
>
> 那我们就来看看哪个params是null
>
> 我们来看const specs 这个变量里面的内容：

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1595753037.png)

> 找到问题了！原来是 id=14 的specgroup 对应的params 内容是null。

### 4、解决问题

> 我们来到数据库，查询groupid=14 的规格属性内容，sql如下：

```sql
SELECT * FROM tb_spec_param WHERE group_id=14;
```

结果如下：

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1595753236.png)

> 问题确定了，就是没有获取到规格参数，导致页面在处理数据的时候报错了。 
>
> 原来：组id是14的这条数据是在测试后台添加规格组功能时添加的数据，但是没有给组下添加规格参数

### 5、验证

> 我们把id=14的规格组删除，发现页面内容可以正常显示了

![](assets/1595753504.png)



# 2、Thymeleaf页面静态化

## 2.1.简介

### 2.1.1.问题分析

现在，我们的页面是通过Thymeleaf模板引擎渲染后返回到客户端。在后台需要大量的数据查询，而后渲染得到HTML页面。会对数据库造成压力，并且请求的响应时间过长，并发能力不高。

大家能想到什么办法来解决这个问题？

- 缓存，后端可以对要查询的数据进行缓存，提高查询效率，减小数据库压力。
  - 优点：以后无论是页面查询数据、移动的查询数据，都走缓存，更通用
  - 缺点：请求依然会到达服务端，造成服务端压力。
- 静态化，把页面渲染后写入HTML文件，以后不再查询服务端数据
  - 优点：不访问服务端，效率更高
  - 缺点：适应于H5，APP中需要另外的静态化方案

### 2.1.2.什么是静态化

静态化是指把动态生成的HTML页面变为静态内容保存，以后用户的请求到来，直接访问静态页面，不再经过服务的渲染。

而静态的HTML页面可以部署在nginx中，从而大大提高并发能力，减小tomcat压力。



### 2.1.3.如何实现静态化

目前，静态化页面都是通过模板引擎来生成，而后保存到nginx服务器来部署。常用的模板引擎比如：

- Freemarker
- Velocity
- Thymeleaf

我们之前就使用的Thymeleaf，来渲染html返回给用户。Thymeleaf除了可以把渲染结果写入Response，也可以写到本地文件，从而实现静态化。

## 2.2.Thymeleaf生成静态页

### 2.2.1.概念

先说下Thymeleaf中的几个概念：

- Context：运行上下文
- TemplateResolver：模板解析器
- TemplateEngine：模板引擎

> Context

上下文： 用来保存模型数据，当模板引擎渲染时，可以从Context上下文中获取数据用于渲染。

当与SpringBoot结合使用时，我们放入Model的数据就会被处理到Context，作为模板渲染的数据使用。

> TemplateResolver

模板解析器：用来读取模板相关的配置，例如：模板存放的位置信息，模板文件名称，模板文件的类型等等。

当与SpringBoot结合时，TemplateResolver已经由其创建完成，并且各种配置也都有默认值，比如模板存放位置，其默认值就是：templates。比如模板文件类型，其默认值就是html。

> TemplateEngine

模板引擎：用来解析模板的引擎，需要使用到上下文、模板解析器。分别从两者中获取模板中需要的数据，模板文件。然后利用内置的语法规则解析，从而输出解析后的文件。来看下模板引起进行处理的函数：

```java
templateEngine.process("模板名", context, writer);
```

三个参数：

- 模板名称
- 上下文：里面包含模型数据
- writer：输出目的地的流

在输出时，我们可以指定输出的目的地，如果目的地是Response的流，那就是网络响应。如果目的地是本地文件，那就实现静态化了。

而在SpringBoot中已经自动配置了模板引擎，因此我们不需要关心这个。现在我们做静态化，就是把输出的目的地改成本地文件即可！

### 2.2.2.具体实现

我们在PageService中，编写方法，实现静态化功能：

```java
@Slf4j
@Service
public class PageService {

    @Autowired
    private ItemClient itemClient;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${ly.static.itemDir}")
    private String itemDir;
    @Value("${ly.static.itemTemplate}")
    private String itemTemplate;

    /**
     * 获取模板页面需要的动态数据
     * @param spuId
     * @return
     */
    public Map<String, Object> loadData(Long spuId) {
        Map<String, Object> map = new HashMap<>();
//        根据spuid 获取spu对象
        SpuDTO spuDTO = itemClient.findSpuById(spuId);
//      categories 分类的集合
        List<CategoryDTO> categoryDTOList = itemClient.findCategoryListByIds(spuDTO.getCategoryIds());
//      brand 品牌对象
        BrandDTO brandDTO = itemClient.findBrandById(spuDTO.getBrandId());
//      detail 商品详情对象  spu_detail的数据
        SpuDetailDTO spuDetailDTO = itemClient.findSpuDetailBySpuId(spuId);
//        skus spu对应的sku集合
        List<SkuDTO> skuDTOList = itemClient.findSkuListBySpuId(spuId);
//        specs 商品对应的规格参数组，名字，值
        List<SpecGroupDTO> specGroupDTOList = itemClient.findSpecGorupList(spuDTO.getCid3());
        map.put("categories",categoryDTOList);
        map.put("brand",brandDTO);
        map.put("spuName",spuDTO.getName());
        map.put("subTitle",spuDTO.getSubTitle());
        map.put("detail",spuDetailDTO);
        map.put("skus",skuDTOList);
        map.put("specs",specGroupDTOList);
        return map;
    }

    public void createItemHtml(Long id) {
        // 上下文，准备模型数据
        Context context = new Context();
        // 调用之前写好的加载数据方法
        context.setVariables(loadItemData(id));
        // 准备文件路径
        File dir = new File(itemDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                // 创建失败，抛出异常
                log.error("【静态页服务】创建静态页目录失败，目录地址：{}", dir.getAbsolutePath());
                throw new LyException(ExceptionEnum.DIRECTORY_WRITER_ERROR);
            }
        }
        File filePath = new File(dir, id + ".html");
        // 准备输出流
        try (PrintWriter writer = new PrintWriter(filePath, "UTF-8")) {
            templateEngine.process(itemTemplate, context, writer);
        } catch (IOException e) {
            log.error("【静态页服务】静态页生成失败，商品id：{}", id, e);
            throw new LyException(ExceptionEnum.FILE_WRITER_ERROR);
        }
    }
}
```

在application.yml中配置生成静态文件的目录：

```yaml
ly:
  static:
    itemDir: C:\\develop\\nginx-1.12.2\\html\\item
    itemTemplate: item
```



### 2.2.4.单元测试：

我们编写一个单元测试：

```java
package com.leyou.page.test;

import com.leyou.page.service.PageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestCreatePage {

    @Autowired
    private PageService pageService;
    @Test
    public void testCreateHtml(){
        List<Long> spuIdList = Arrays.asList(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        for (Long spuId : spuIdList) {
            pageService.createHtml(spuId);
        }

    }
}

```

然后到nginx下查看：

![1575341879825](assets/1575341879825.png)





## 2.3.nginx代理静态页面

接下来，我们修改nginx，让它对商品请求进行监听，指向本地静态页面，如果本地没找到，才进行反向代理：

```nginx
server {
	listen       80;
	server_name  www.leyou.com;
	location /item {
		# 先找本地
        root html;
        if (!-f $request_filename) { #请求的文件不存在，就反向代理
            proxy_pass http://127.0.0.1:8084;
            break;
        }
	}
	location / {
		proxy_pass   http://leyou-portal;
	}
}
```

重启测试：

发现请求速度得到了极大提升：

![1553252883092](assets/1553252883092.png)



**`注意！！！`**，这里为了演示方便我们采用了if判断文件是否存在，不存在则反向代理到服务端，服务端生成页面返回。这种做法有`缓存穿透`的风险，建议不要加入if判断，如果nginx中不存在商品的静态页，直接返回404即可。

即：

```nginx
server {
	listen       80;
	server_name  www.leyou.com;
	location /item {
        root html;
	}
	location / {
		proxy_pass   http://leyou-portal;
	}
}
```



## 2.4.静态化的数据同步问题

我们思考这样几个问题：

- 商品详情页应该在什么时候生成呢？不能每次都用单元测试生成吧？
- 如果商品数据修改以后，静态页的内容与商品实际内容不符，该如何完成同步？
- 商品下架后，不应该再展示商品页面，静态页如何处理？



思考上面问题的同时，我们会想起一件事情，其实商品数据如果发生了增、删、改，不仅仅静态页面需要处理，我们的索引库数据也需要同步！！这又该如何解决？



因为商品新增后需要上架用户才能看到，商品修改需要先下架，然后修改，再上架。因此上述问题可以统一的设计成这样的逻辑处理：

- 商品上架：
  - 生成静态页
  - 新增索引库数据
- 商品下架：
  - 删除静态页，或者修改静态页
  - 删除索引库数据

这样既可保证数据库商品与索引库、静态页三者之间的数据同步。

那么，如何实现上述逻辑呢？

**结论**：最好不要使用 同步调用，最好使用**异步调用**

# 3、RocketMQ概念与安装

### 了解RocketMQ概念

### 3.1 消息中间件

消息中间件利用高效可靠的消息传递机制进行平台无关的数据交流，并基于数据通信来进行[分布式系统](https://baike.baidu.com/item/%E5%88%86%E5%B8%83%E5%BC%8F%E7%B3%BB%E7%BB%9F)的集成。通过提供消息传递和消息排队模型，它可以在分布式环境下扩展进程间的通信。对于消息中间件，常见的角色大致也就有Producer（生产者）、Consumer（消费者）

常见的消息中间件产品:

#### 1)RocketMQ

阿里系下开源的一款分布式、队列模型的消息中间件，原名Metaq，3.0版本名称改为RocketMQ，是阿里参照kafka设计思想使用java实现的一套mq。同时将阿里系内部多款mq产品（Notify、metaq）进行整合，只维护核心功能，去除了所有其他运行时依赖，保证核心功能最简化，在此基础上配合阿里上述其他开源产品实现不同场景下mq的架构，目前主要多用于订单交易系统。

具有以下特点：

- 能够保证严格的消息顺序
- 提供针对消息的过滤功能
- 提供丰富的消息拉取模式
- 高效的订阅者水平扩展能力
- 实时的消息订阅机制
- 亿级消息堆积能力

#### 2)ActiveMQ

ActiveMQ 是Apache出品，最流行的，能力强劲的开源消息总线。ActiveMQ 是一个完全支持JMS1.1和J2EE 1.4规范的 JMS Provider实现。

#### 3)RabbitMQ

AMQP协议的领导实现，支持多种场景。

交换机、队列   

#### 4)ZeroMQ

史上最快的消息队列系统，部分功能需要编码实现

#### 5)Kafka

Apache下的一个子项目 。特点：高吞吐，在一台普通的服务器上既可以达到10W/s的吞吐速率；完全的分布式系统。适合处理海量数据。

注意：在乐优商城中，我们通过引入消息中间件RocketMQ，保证数据库商品与索引库、静态页三者之间的数据同步，实现业务的解耦合。

### 3.2 RocketMQ概念介绍

#### 3.2.1 RocketMQ架构

RocketMQ作为一款纯java、分布式、队列模型的开源消息中间件，支持事务消息、顺序消息、批量消息、定时消息、消息回溯等。RocketMQ由阿里巴巴开源。

![1548877889288](assets/1548877889288.png)

如图所示为RocketMQ基本的部署结构，主要分为NameServer集群、Broker集群、Producer集群和Consumer集群四个部分。 

**大致流程**：
Broker在启动的时候会去向NameServer注册并且定时发送心跳，Producer在启动的时候会到NameServer上去拉取Topic所属的Broker具体地址，然后向具体的Broker发送消息

为了消除单点故障，增加可靠性或增大吞吐量，可以在多台机器上部署多个nameserver和broker，并且为每个broker部署1个或多个slave

#### 3.2.2  NameServer

NameServer的作用是Broker的注册中心。

**每个NameServer节点互相之间是独立的，没有任何信息交互**，也就不存在任何的选主或者主从切换之类的问题，因此NameServer是很轻量级的。单个NameServer节点中存储了活跃的Broker列表（包括master和slave），这里活跃的定义是与NameServer保持有心跳。

#### 3.2.3 Topic、Tag、Queue、GroupName

Topic 与 Tag 都是业务上用来归类的标识，区分在于 Topic 是一级分类，而 Tag 可以理解为是二级分类

##### 1)Topic

是生产者在发送消息和消费者在拉取消息的类别。Topic与生产者和消费者之间的关系非常松散。一个生产者可以发送不同类型Topic的消息。消费者组可以订阅一个或多个主题，只要该组的实例保持其订阅一致即可。

​    Topic翻译为**话题**。我们可以理解为第一级消息类型，比如一个电商系统的消息可以分为：交易消息、物流消息等，一条消息必须有一个Topic。

##### 2)Tag

标签，意思就是子主题，为用户提供了额外的灵活性。有了标签，方便RocketMQ提供的查询功能。

可以理解为第二级消息类型，交易创建消息，交易完成消息..... 一条消息可以没有Tag

##### 3)Queue

一个topic下，可以设置多个queue(消息队列)，默认4个队列。当我们发送消息时，需要要指定该消息的topic。

RocketMQ会轮询该topic下的所有队列，将消息发送出去。

在 RocketMQ 中，所有消息队列都是持久化，长度无限的数据结构，所谓长度无限是指队列中的每个存储单元都是定长，访问其中的存储单元使用 Offset 来访问，offset 为 java long 类型，64 位，理论上在 100年内不会溢出，所以认为是长度无限。

也可以认为 Message Queue 是一个长度无限的数组，Offset 就是下标。

##### 4)groupName

RocketMQ中也有组的概念。代表具有相同角色的生产者组合或消费者组合，称为生产者组或消费者组。

作用是在集群HA的情况下，一个生产者down之后，本地事务回滚后，可以继续联系该组下的另外一个生产者实例，不至于导致业务走不下去。在消费者组中，可以实现消息消费的负载均衡和消息容错目标。

有了GroupName，在集群下，动态扩展容量很方便。只需要在新加的机器中，配置相同的GroupName。启动后，就立即能加入到所在的群组中，参与消息生产或消费。

#### 3.2.4 Broker-存放消息

Broker是具体提供业务的服务器，单个Broker节点与所有的NameServer节点保持长连接及心跳，定时(每隔30s)注册Topic信息到所有Name Server。Name Server定时(每隔10s)扫描所有存活broker的连接，如果Name Server超过2分钟没有收到心跳，则Name Server断开与Broker的连接。底层的通信和连接都是基于Netty实现的。

负载均衡：Broker上存Topic信息，Topic由多个队列组成，队列会平均分散在多个Broker上，会自动轮询当前所有可发送的broker ，尽量平均分布到所有队列中，最终效果就是所有消息都平均落在每个Broker上

高可用：Broker中分**master**和**slave**两种角色，每个master可以对应多个slave，但一个slave只能对应一个master，master和slave通过指定相同的Brokername组成，其中不同的BrokerId==0 是master，非0是slave。

高可靠并发读写服务：master和slave之间的同步方式分为同步双写和异步复制，异步复制方式master和slave之间虽然会存在少量的延迟，但性能较同步双写方式要高出10%左右。

Topic、Broker、queue

![](assets/13433554456.png)

#### 3.2.5 Producer-生产消息

##### 3.2.5.1 与nameserver的关系

单个Producer和一台NameServer节点(随机选择)保持长连接，定时查询topic配置信息，如果该NameServer挂掉，生产者会自动连接下一个NameServer，直到有可用连接为止，并能自动重连。与NameServer之间没有心跳。

##### 3.2.5.2 与broker的关系

单个Producer和与其关联的所有broker保持长连接，并维持心跳。默认情况下消息发送采用轮询方式，会均匀发到对应Topic的所有queue中。



#### 3.2.6 Consumer-消费消息

##### 3.2.6.1 与nameserver的关系

单个Consumer和一台NameServer保持**长连接**，定时查询topic配置信息，如果该NameServer挂掉，消费者会自动连接下一个NameServer，直到有可用连接为止，并能自动重连。与NameServer之间没有心跳。

##### 3.2.6.2 与broker的关系

单个Consumer和与其关联的所有broker保持**长连接**，并维持心跳，失去心跳后，则关闭连接，并向该消费者分组的所有消费者发出通知，分组内消费者重新分配队列继续消费。

##### 3.2.6.3 消费者类型

###### 3.2.6.3.1 pull consume

Consumer 的一种，应用通常通过 Consumer 对象注册一个 Listener 接口，一旦收到消息，Consumer 对象立刻回调 Listener 接口方法，类似于activemq的方式

###### 3.2.6.3.2 push consume

Consumer 的一种，应用通常主动调用 Consumer 的拉消息方法从 Broker 拉消息，主动权由应用控制

#### 3.2.7 消费模式

##### 3.2.7.1 集群模式

在默认情况下，就是集群消费，此时消息发出去后将只有一个消费者能获取消息。

##### 3.2.7.2 广播模式

广播消费，一条消息被多个Consumer消费。消息会发给Consume Group中的每一个消费者进行消费。

#### 3.2.8、RocketMQ的路由

#### 3.2.9、RocketMQ的特性

##### 3.2.9.1、消息顺序

消息的顺序指的是消息消费时，能按照发送的顺序来消费。

RocketMQ是通过将“相同ID的消息发送到同一个队列，而一个队列的消息只由一个消费者处理“来实现顺序消息

##### 3.2.9.2、消息重复

###### 3.2.9.2.1、消息重复的原因

消息领域有一个对消息投递的QoS（服务质量）定义，分为：最多一次（At most once）、至少一次（At least once）、仅一次（ Exactly once）。

MQ产品都声称自己做到了At least once。既然是至少一次，就有可能发生消息重复。

有很多原因导致，比如：网络原因闪断，ACK返回失败等等故障，确认信息没有传送到消息队列，导致消息队列不知道自己已经消费过该消息了，再次将该消息分发给其他的消费者

不同的消息队列发送的确认信息形式不同：RocketMQ返回一个CONSUME_SUCCESS成功标志，RabbitMQ是发送一个ACK确认消息

###### 3.2.9.2.2、消息去重

1）、去重原则：使用业务端逻辑保持幂等性

幂等性：就是用户对于同一操作发起的一次请求或者多次请求的结果是一致的，不会因为多次点击而产生了副作用，数据库的结果都是唯一的，不可变的。

2）、只要保持幂等性，不管来多少条重复消息，最后处理的结果都一样，需要业务端来实现。

去重策略：保证每条消息都有唯一编号(比如唯一流水号)，且保证消息处理成功与去重表的日志同时出现。

#### 3.2.10、RocketMQ的应用场景

##### 1.削峰填谷

比如如秒杀等大型活动时会带来较高的流量脉冲，如果没做相应的保护，将导致系统超负荷甚至崩溃。如果因限制太过导致请求大量失败而影响用户体验，可以利用MQ 超高性能的消息处理能力来解决。

##### 2.异步解耦

通过上、下游业务系统的松耦合设计，比如：交易系统的下游子系统（如积分等）出现不可用甚至宕机，都不会影响到核心交易系统的正常运转。

##### 3.顺序消息

与FIFO原理类似，MQ提供的顺序消息即保证消息的先进先出，可以应用于交易系统中的订单创建、支付、退款等流程。

##### 4.分布式事务消息

比如阿里的交易系统、支付红包等场景需要确保数据的最终一致性，需要引入 MQ 的分布式事务，既实现了系统之间的解耦，又可以保证最终的数据一致性。

### 3.3 RocketMQ集群部署方式

#### 3.3.1、单Mater模式

优点：配置简单，方便部署

缺点：风险较大，一旦Broker重启或者宕机，会导致整个服务不可用

#### 3.3.2、多Master模式

一个集群无 Slave，全是 Master，例如 2 个 Master 或者 3 个 Master

优点：配置简单，单个Master宕机重启对应用没有影响。消息不会丢失

缺点：单台机器宕机期间，这台机器上没有被消费的消息在恢复之前不可订阅，消息实时性会受到影响。

#### 3.3.3、多Master多Slave模式（异步）

每个Master配置一个Slave，采用异步复制方式，主备有短暂消息延迟

优点：因为Master 宕机后，消费者仍然可以从 Slave消费，此过程对应用透明。不需要人工干预。性能同多 Master 模式几乎一样。

缺点：Master宕机后，会丢失少量信息

#### 3.3.4、多Master多Slave模式（同步）

每个Master配置一个Slave，采用同步双写方式，只有主和备都写成功，才返回成功

优点：数据与服务都无单点， Master宕机情况下，消息无延迟，服务可用性与数据可用性都非常高

缺点：性能比异步复制模式略低，大约低 10%左右，发送单个消息的 RT会略高。目前主宕机后，备机不能自动切换为主机，后续会支持自动切换功能

### 3.4 RocketMQ下载与安装

#### 3.4.1、下载

官方网站下载：http://rocketmq.apache.org/，我们找到 Quick Start 进入快速开始页面找到下载链接

<http://rocketmq.apache.org/docs/quick-start/>

我们这里使用的是rocketmq-all-4.4.0-bin-release.zip

![](assets/Snipaste_2019-09-05_10-22-35.png)

#### 3.4.2、安装

将下载好的文件解压到D:\coding-software目录下，然后配置环境变量ROCKETMQ_HOME=D:\coding-software\rocketmq-all-4.4.0-bin-release

![1548542074825](assets/1548542074825.png)



### 3.5 windows启动操作 

#### 3.5.1 、启动NAMESERVER

cmd命令框执行进入至‘MQ文件夹\bin’下，然后执行

```
start mqnamesrv.cmd
```



启动NAMESERVER。成功后会弹出提示框，此框勿关闭。

![1548542221063](assets/1548542221063.png)



#### 3.5.2 、启动BROKER

 cmd命令框执行进入至‘MQ文件夹\bin’下，然后执行

```
start mqbroker.cmd -n 127.0.0.1:9876 autoCreateTopicEnable=true
```



启动BROKER。成功后会弹出提示框，此框勿关闭。

![1548542388881](assets/1548542388881.png)

注意：假如弹出提示框提示‘错误: 找不到或无法加载主类 xxxxxx’。打开runbroker.cmd，然后将‘%CLASSPATH%’加上英文双引号。保存并重新执行start语句。

![1548542513490](assets/1548542513490.png)

### 3.6 RocketMQ插件部署

RocketMQ插件是一个基于SpringBoot编写的可视化插件，主要用于对RocketMQ提供了可视化的管理界面。

#### 3.6.1 、下载

下载地址：https://github.com/apache/rocketmq-externals/releases

![1548542721503](assets/1548542721503.png)

#### 3.6.2 、编译启动

先修改下配置文件的nameserver地址

修改rocketmq-console\src\main\resources\application.properties，修改如下：

![1548878397947](assets/1548878397947.png)



进入‘\rocketmq-externals\rocketmq-console’文件夹，

```
mvn clean package -Dmaven.test.skip=true
```

编译生成。 

![1548542793935](assets/1548542793935.png)

编译成功之后，Cmd进入‘target’文件夹，执行

```
java -jar rocketmq-console-ng-1.0.0.jar
```

启动‘rocketmq-console-ng-1.0.0.jar’。

#### 3.6.3 、测试

输入地址http://127.0.0.1:8080/#/ops

![1548542862391](assets/1548542862391.png)



上面显示的是英文，如果想显示中文，可以点击语言切换，选中Chinese

![1548542953245](assets/1548542953245.png)

切换后如下图：

![1548542995511](assets/1548542995511.png)



# 4、RocketMQ入门

### 重点：熟悉RocketMQ基本api的使用

### 步骤分析

RocketMQ消息消费类型有2种类型，我们先实现一次集群消息模式，再实现广播模式。集群模式也就是消息只能同时被一个消费者读取，而广播模式则可以同时被所有消费者读取。

消息发送步骤：

```properties
1.创建DefaultMQProducer
2.设置Namesrv地址
3.启动DefaultMQProducer
4.创建消息Message
5.发送消息,发送到broker
6.关闭DefaultMQProducer
```

消息消费步骤：

```properties
1.创建DefaultMQPushConsumer
2.设置namesrv地址
3.设置subscribe，这里是要读取的主题信息
4.创建消息监听MessageListener
5.启动消费
6.获取消息信息
7.返回消息读取状态
```

### 创建工程

#### 1、创建父工程 rocketMq-demo

项目结构如下：

![](assets/1571449744.png)

配置pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.itheima</groupId>
    <artifactId>rocketMq-demo</artifactId>
    <packaging>pom</packaging>
    <version>1.0-SNAPSHOT</version>
    <modules>
        <module>springBoot-demo</module>
        <module>mq-demo</module>
    </modules>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.3.RELEASE</version>
    </parent>

</project>
```



#### 2、创建子工程mq-demo

这个子工程用来演示RocketMQ的原生API的使用

加入依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>rocketMq-demo</artifactId>
        <groupId>com.itheima</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>mq-demo</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-client</artifactId>
            <version>4.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
</project>
```



创建常量类

```java
package com.itheima.mq.constants;

public class SysConstants {
	//nameserver的地址
    public static final String NAME_SERVER = "127.0.0.1:9876";
}

```



### 4.1 、普通消息

#### 4.1.1  消息生产者

创建类Producer， main方法代码如下：

```java
package com.itheima.mq.base;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;

/**
 * 普通消息生产者
 */
public class Producer {
    public static void main(String[] args) throws Exception {
//        创建一个消息发送入口对象，主要用于消息发送，指定生产者组
        DefaultMQProducer producer = new DefaultMQProducer("producerGroup");
//        设置NameServe地址，如果是集群环境，用分号隔开
        producer.setNamesrvAddr(SysConstants.NAME_SERVER);
//        启动并创建消息发送组件
        producer.start();
//        topic的名字
        String topic = "rocketDemobasic";
//        标签名
        String tag = "tag1";
//        要发送的数据
        String body = "hello,RocketMq，basic1";
        Message message = new Message(topic,tag,body.getBytes());
        // 发送消息
        SendResult result = producer.send(message,20000);
        System.out.println(result);

//        关闭消息发送对象
        producer.shutdown();


    }
}

```

执行一次main方法后，查看控制台主题信息，如下：

![](assets/Snipaste_2019-09-05_16-20-46.png)

还可以查看到消息

![](assets/Snipaste_2019-09-05_17-01-43.png)

#### 4.1.2 消息消费者

创建类Consumer，main方法代码如下：

```java
package com.itheima.mq.base;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * 普通消息消费者
 */
public class Consumer {

    public static void main(String[] args) throws Exception {
//        创建一个消费管理对象，并创建消费者组名字
        DefaultMQPushConsumer consumerGroup = new DefaultMQPushConsumer("ConsumerGroup");
//        设置NameServer地址，如果是集群环境，用逗号分隔
        consumerGroup.setNamesrvAddr(SysConstants.NAME_SERVER);
//        设置要读取的消息主题和标签
        consumerGroup.subscribe("rocketDemobasic","*");
//        设置消费者 可以消费的消息条数
        consumerGroup.setConsumeMessageBatchMaxSize(1);
//        读取消息的位置
        //ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET:从最后一个读取
        //ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET:从第一个读取
        consumerGroup.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
//      设置回调函数，处理消息
        consumerGroup.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                try{
//                    读取消息记录
                    for (MessageExt messageExt : msgs) {
//                    获取消息主题
                        String topic = messageExt.getTopic();
//                    获取消息标签
                        String tags = messageExt.getTags();
//                    获取消息体内容
                        String body = new String(messageExt.getBody(), "UTF-8");
                        System.out.println("topic："+topic+"，tags："+tags+"，body："+body);
                    }

                }catch(Exception e){
                    e.printStackTrace();
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
//      运行消息消费对象
        consumerGroup.start();
    }
}

```



执行后看到控制台输出

![](assets/Snipaste_2019-09-05_17-04-26.png)





### 4.2、 顺序消息

消息有序指的是可以按照消息的发送顺序来消费。RocketMQ是通过将“相同ID的消息发送到同一个队列，而一个队列的消息只由一个消费者处理“来实现顺序消息 。

**如何保证顺序**

```properties
在MQ的模型中，顺序需要由3个阶段去保障：
	1.消息被发送时保持顺序
	2.消息被存储时保持和发送的顺序一致
	3.消息被消费时保持和存储的顺序一致
```

发送时保持顺序意味着对于有顺序要求的消息，用户应该在同一个线程中采用同步的方式发送。存储保持和发送的顺序一致则要求在同一线程中被发送出来的消息A和B，存储时在空间上A一定在B之前。而消费保持和存储一致则要求消息A、B到达Consumer之后必须按照先A后B的顺序被处理。 

#### 4.2.1 、生产者

我们创建一个消息生产者OrderProducer

Producer端确保消息顺序唯一要做的事情就是将消息路由到特定的队列，在RocketMQ中，通过MessageQueueSelector来实现分区的选择。

比如如下实现就可以保证相同的订单的消息被路由到相同的分区：

```java
long orderId ; //订单号   
return mqs.get(orderId % mqs.size()); //用订单号 和队列数量 取模
```

准备顺序消息

```java
package com.itheima.mq.order.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    private Long orderId;
    private String desc;

    public static List<Order> buildOrders(){
        List<Order> list = new ArrayList<>();
        Order order1001a = new Order(1001L,"创建");
        Order order1004a = new Order(1004L,"创建");
        Order order1006a = new Order(1006L,"创建");
        Order order1009a = new Order(1009L,"创建");
        list.add(order1001a);
        list.add(order1004a);
        list.add(order1006a);
        list.add(order1009a);
        Order order1001b = new Order(1001L,"付款");
        Order order1004b = new Order(1004L,"付款");
        Order order1006b = new Order(1006L,"付款");
        Order order1009b = new Order(1009L,"付款");
        list.add(order1001b);
        list.add(order1004b);
        list.add(order1006b);
        list.add(order1009b);
        Order order1001c = new Order(1001L,"完成");
        Order order1006c = new Order(1006L,"完成");
        list.add(order1001c);
        list.add(order1006c);
        return list;
    }

}

```



消费者代码如下：

```java
package com.itheima.mq.order;

import com.itheima.mq.constants.SysConstants;
import com.itheima.mq.order.entity.Order;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.List;

/**
 * 顺序消息生产者
 */
public class ProducerOrder {
        //nameserver地址
        private static String namesrvaddress= SysConstants.NAME_SERVER;

        public static void main(String[] args) throws Exception {
            //创建DefaultMQProducer
            DefaultMQProducer producer = new DefaultMQProducer("order_producer");
            //设置namesrv地址
            producer.setNamesrvAddr(namesrvaddress);

            //启动Producer
            producer.start();

            List<Order> orderList = Order.buildOrders();

            for (Order order : orderList) {

                String body = order.toString();
                //创建消息
                Message message = new Message("orderTopic","order",body.getBytes());
                //发送消息
                SendResult sendResult = producer.send(
                        message,
                        new MessageQueueSelector() {
                            /**
                             *
                             * @param mqs topic中的队列集合
                             * @param msg 消息对象
                             * @param arg 业务参数
                             * @return
                             */
                            @Override
                            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                                //参数是订单id号
                                Long orderId = (Long) arg;
                                //确定选择的队列的索引
                                long index = orderId % mqs.size();
                                return mqs.get((int) index);
                            }
                        },
                        order.getOrderId(),
                        20000);
                System.out.println("发送结果="+sendResult);

            }
            //关闭Producer
            producer.shutdown();
        }
    }
```

#### 4.2.2 、消费者

创建一个消息消费者OrderConsumer,消息监听用MessageListenerOrderly来实现顺序消息，

代码如下：

```java
package com.itheima.mq.order;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 顺序消息 消费者
 */
public class ConsumerOrder {
    public static void main(String[] args) throws Exception {

        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("order_consumer");
        consumer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //从第一个开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        consumer.subscribe("orderTopic","*");

        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {

                    System.out.println("当前队列："+context.getMessageQueue().getQueueId()+",接收消息："+new String(msg.getBody()));
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }

        });


//        consumer.registerMessageListener(new MessageListenerOrderly() {
//
//            AtomicLong consumerTimes = new AtomicLong(0);
//
//            @Override
//            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
//                context.setAutoCommit(false);
//                System.out.printf(Thread.currentThread().getName() + "Receive New Message:"+msgs +"%n");
//                this.consumerTimes.incrementAndGet();
//                System.out.println(this.consumerTimes);
//                if((this.consumerTimes.get() % 2) == 0){
//                    return ConsumeOrderlyStatus.SUCCESS;
//                }else if((this.consumerTimes.get() % 3) ==  0){
//                    return ConsumeOrderlyStatus.ROLLBACK;
//                }else if((this.consumerTimes.get() % 4)== 0){
//                    return ConsumeOrderlyStatus.COMMIT;
//                }else if((this.consumerTimes.get() % 5)==0){
//                    context.setSuspendCurrentQueueTimeMillis(3000);
//                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
//                }
//                return ConsumeOrderlyStatus.SUCCESS;
//            }
//        });
        consumer.start();
        System.out.printf("消费者启动成功.%n");
    }
}

```

我们来查看控制台的输出,可以返现 同一个orderId，被同一个线程，有顺序的消费了

![](assets/orderconsumerlog.png)



### 4.3 、延迟消息

#### 使用限制

```java
 rocketmq支持固定延迟等级
 //"1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h"
```



#### 4.3.1、生产者

```java
package com.itheima.mq.delay;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * 延迟消息 生产者
 */
public class ProducerDelay {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("delay_producer");
        producer.setSendMsgTimeout(10000);
        //设置nameserver
        producer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //生产者开启
        producer.start();
        //创建消息对象
        Message message = new Message("delayTopic","delay","hello world".getBytes());
        //设置延迟时间级别
        message.setDelayTimeLevel(2);
        //发送消息
        SendResult sendResult = producer.send(message,20000);
        System.out.println(sendResult);
        //生产者关闭
        producer.shutdown();
    }
}

```



#### 4.3.2、消费者

```java
package com.itheima.mq.delay;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.Date;
import java.util.List;

/**
 * 延迟消息 消费者
 */
public class ConsumerDelay {

    public static void main(String[] args) throws Exception {
        //创建消费者对象
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("delay_consumer");
        //设置nameserver
        consumer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //设置主题和tag
        consumer.subscribe("delayTopic","*");
        //注册消息监听
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    long now = System.currentTimeMillis();
                    long storeTime = msg.getStoreTimestamp();
                    long dealy = now - storeTime;
                    Date date = new Date(storeTime);
                    Date nowdate = new Date(now);
                    System.out.println("now="+now);
                    System.out.println("storeTime="+storeTime);
                    System.out.println("delay="+dealy);
                    System.out.println("date="+date);
                    System.out.println("nowdate="+nowdate);
                    System.out.println("消息ID："+msg.getMsgId()+"，延迟时间："+dealy);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //开启消费者
        consumer.start();
        System.out.println("消费者启动");
    }
}

```



### 4.4 、批量发送消息

上面发送消息，我们测试的时候，可以发现消息只有一个消费者能收到，如果我们想实现消息广播，让每个消费者都能收到消息也是可以实现的。而且上面发送消息的时候，每次都是发送单条Message对象，能否批量发送呢？答案是可以的。

#### 4.4.1 、生产者

创建消息生产者，代码如下：

```java
package com.itheima.mq.batch;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量 生产者
 */
public class ProducerBatch {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("batch_producer");
        //设置nameserver
        producer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //生产者开启
        producer.start();
        //创建消息对象  集合
        String topic = "batchTopic";
        String tag = "batchTag";
        List<Message> messageList = new ArrayList<>();
        Message message1 = new Message(topic,tag,"hello world1".getBytes());
        Message message2 = new Message(topic,tag,"hello world2".getBytes());
        Message message3 = new Message(topic,tag,"hello world3".getBytes());
        messageList.add(message1);
        messageList.add(message2);
        messageList.add(message3);
        //发送消息
        SendResult sendResult = producer.send(messageList,20000);
        System.out.println(sendResult);
        //生产者关闭
        producer.shutdown();
    }
}

```

注意：发送的总消息长度 不能大于4M

#### 4.4.2 、消费者 

消费者代码没有特殊的地方，代码如下：

```java
package com.itheima.mq.batch;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * 批量消费者
 */
public class ConsumerBatch {
    public static void main(String[] args) throws Exception {
        //创建消费者对象
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("batch_consumer");
        //设置nameserver
        consumer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //设置主题和tag
        consumer.subscribe("batchTopic","*");
        //注册消息监听
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    System.out.println("消息ID："+msg.getMsgId()+","+msg.getTopic()+","+msg.getTags());
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //开启消费者
        consumer.start();
        System.out.println("消费者启动");
    }
}

```



### 4.5 、广播模式和集群模式

集群模式，只有一个消费者可以收到消息

广播模式是所有的消费者都可以收到消息。

需要把【消息模式】设置为 ：广播模式，同时开启2个以上的消费者，再次运行生产者，只需要在消费方设置一下消费模式即可。

**注意：需要消费者属于不同的【消费者组】**

```
MessageModel.BROADCASTING:广播模式
MessageModel.CLUSTERING：集群模式
```

需要在消费者加入的代码如下：

```java
//设置消费模式为广播模式(BROADCASTING),默认为集群模式(CLUSTERING)
consumer.setMessageModel(MessageModel.CLUSTERING);
```

注意：2个消费者的 名字都是一样的

消费者1：

```java
package com.itheima.mq.broadcast;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import java.util.List;

/**
 * 批量消费者
 */
public class ConsumerBroadcast {
    public static void main(String[] args) throws Exception {
        //创建消费者对象
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("delay_consumer1");
        //设置nameserver
        consumer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //设置主题和tag
        consumer.subscribe("broadcastTopic","*");
        //设置消息模式 为 广播模式
        consumer.setMessageModel(MessageModel.BROADCASTING);
        //注册消息监听
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    System.out.println("消费者1：消息ID："+msg.getMsgId()+"，内容"+new String(msg.getBody()));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //开启消费者
        consumer.start();
        System.out.println("消费者启动");
    }
}

```

消费者2：

```java
package com.itheima.mq.broadcast;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import java.util.List;

/**
 * 批量消费者
 */
public class ConsumerBroadcast1 {
    public static void main(String[] args) throws Exception {
        //创建消费者对象
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("delay_consumer");
        //设置nameserver
        consumer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //设置主题和tag
        consumer.subscribe("broadcastTopic","*");
        //设置消息模式 为 广播模式
        consumer.setMessageModel(MessageModel.BROADCASTING);
        //注册消息监听
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    System.out.println("消费者2：消息ID："+msg.getMsgId()+"，内容"+new String(msg.getBody()));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //开启消费者
        consumer.start();
        System.out.println("消费者启动");
    }
}

```

生产者

```java
package com.itheima.mq.broadcast;

import com.itheima.mq.constants.SysConstants;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量 生产者
 */
public class ProducerBroadcast {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("delay_producer");
        //设置nameserver
        producer.setNamesrvAddr(SysConstants.NAME_SERVER);
        //生产者开启
        producer.start();
        //创建消息对象  集合
        String topic = "broadcastTopic";
        String tag = "broad";
        List<Message> messageList = new ArrayList<>();
        Message message1 = new Message(topic,tag,"hello world1".getBytes());
        Message message2 = new Message(topic,tag,"hello world2".getBytes());
        Message message3 = new Message(topic,tag,"hello world3".getBytes());
        messageList.add(message1);
        messageList.add(message2);
        messageList.add(message3);
        //发送消息
        SendResult sendResult = producer.send(messageList);
        System.out.println(sendResult);
        //生产者关闭
        producer.shutdown();
    }
}

```

再次发送消息和接收消息，可以发现多个消费者都能收到相同消息。





# 5、SpringBoot整合RocketMQ

### 5.1 、创建工程 

工程名：springBoot-demo

![](assets/1571471374.png)

#### 5.1.1、引入依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>rocketMq-demo</artifactId>
        <groupId>com.itheima</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>springBoot-demo</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
            <version>2.0.2</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

</project>
```

#### 5.1.2、创建启动类

```java
package com.itheima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RocketDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(RocketDemoApplication.class,args);
    }
}

```

#### 5.1.3、创建 配置文件

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    send-message-timeout: 300000
    group: productgroup
server:
  port: 8081
```

### 5.2、普通消息

#### 5.2.1、生产者

创建一个测试类，作为消息生产者

```java
package com.itheima.rocketdemo.test;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Test1 {
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 普通消息，生产者
     */
    @Test
    public void testSimpleSend(){

        rocketMQTemplate.convertAndSend(
                "testTopic",
                "这是SpringBoot测试消息！"+new Date());
    }
}

```



#### 5.2.2、消费者

消费者需要使用 

@Component

@RocketMQMessageListener ，需要配置消费者名字和topic名字

```java
package com.itheima.rocketdemo.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RocketMQMessageListener(consumerGroup = "myConsumer", topic = "testTopic")
public class RocketConsumer implements RocketMQListener<String>{
    @Override
    public void onMessage(String message) {
        System.out.println("接收到消息：="+message);
    }
}

```

### 5.3、顺序消息

#### 生产者

需要自己编写消息选择队列的代码，这里我们用 orderid 对 队列总数 取模：long index = orderId % mqs.size();

```java
/**
     * 顺序消息 生产者
     */
    @Test
    public void testOrderlySend(){
        List<Order> orderList = Order.buildOrders();
        for (Order order : orderList) {
            //发送消息
            rocketMQTemplate.setMessageQueueSelector(new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    //参数是订单id号
                    Long orderId = Long.valueOf((String)arg);
                    //确定选择的队列的索引
                    long index = orderId % mqs.size();
                    log.info("mqs is ::" + mqs.get((int) index));
                    return mqs.get((int) index);
                }
            });
            SendResult sendOrderly = rocketMQTemplate.syncSendOrderly("testTopicOrderLy",
                   new GenericMessage<>(order.toString()),order.getOrderId().toString());
            log.info("发送结果="+sendOrderly+",orderid :"+order.getOrderId());

        }

    }
```

#### 消费者

```java
package com.itheima.rocketdemo.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 顺序消息 ，消费者
 */
@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "myConsumerOrderly",
        topic = "testTopicOrderLy",
        consumeMode = ConsumeMode.ORDERLY)
public class RocketConsumerOrderly implements RocketMQListener<String>{


    @Override
    public void onMessage(String message) {

        log.info("当前线程："+Thread.currentThread().getName()+",接收到消息：="+message);
    }
}

```

### 5.4、延迟消息

生产者

```java
/**
     * 延迟消息生产者
     */
    @Test
    public void testDelaySend(){
        SendResult sendResult = rocketMQTemplate.syncSend("testTopic",
                new GenericMessage("这是延迟测试消息！"+new Date()),
                10000,
                4);
        log.info("sendResult=="+sendResult);
    }
```



消费者，和普通消息的消费者一样，无需修改。



### 5.5、广播消息

我们创建新的工程作为新的消费者，创建出一个新的工程springBoot-demo2，如图：

![](assets/1571479172.png)

这个工程中的启动类、配置文件模仿springBoot-demo工程写。在application.yml中配置server.port =8081即可



想要实现广播功能，只需要修改消费者的 消息类型选项，即：

```
messageModel = MessageModel.BROADCASTING
```

```java
package com.itheima.rocketdemo.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "myConsumer", 
                         topic = "testTopic",
                         messageModel = MessageModel.BROADCASTING)
public class RocketConsumer implements RocketMQListener<String>{


    @Override
    public void onMessage(String message) {
       log.info("RocketConsumer,当前线程："+Thread.currentThread().getName()+",接收到消息：="+message);
    }
}
```



当修改为广播模式后，这个新的消费者会收到消息





