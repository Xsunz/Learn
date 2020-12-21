# 学习目标

- 了解SPU和SKU的概念
- 了解SPU和SKU表结构的关键字段
- 独立实现商品查询
- 独立实现商品新增、修改
- 完成前台系统的搭建

# 1.SPU和SKU

## 重点：了解SPU和SKU的概念和表结构

## 1.1.什么是SPU和SKU

SPU：Standard Product Unit （标准产品单位） ，一组具有共同属性的商品集

SKU：Stock Keeping Unit（库存量单位），SPU商品集因具体特性不同而细分的每个商品

以图为例来看：

![1526085541996](assets/1526085541996.png)

- 本页的 华为Mate10 就是一个商品集（SPU）
- 因为颜色、内存等不同，而细分出不同的Mate10，如亮黑色128G版。（SKU）

可以看出：

- SPU是一个抽象的商品集概念，为了方便后台的管理。
- SKU才是具体要销售的商品，每一个SKU的价格、库存可能会不一样，用户购买的是SKU而不是SPU



## 1.2.数据库设计分析

### 1.2.1.思考分析

弄清楚了SPU和SKU的概念区分，接下来我们一起思考一下该如何设计数据库表。

首先来看SPU，大家一起思考下SPU应该有哪些字段来描述？

```
id:主键
title：标题
description：描述
specification：规格
packaging_list：包装
after_service：售后服务
comment：评价
category_id：商品分类
brand_id：品牌
```

似乎并不复杂.

再看下SKU，大家觉得应该有什么字段？

```
id：主键
spu_id：关联的spu
price：价格
images：图片
stock：库存
颜色？
内存？
硬盘？
```

sku的特有属性也是变化的，不同商品，特有属性不一定相同，那么我们的表字段岂不是不确定？

sku的这个特有属性该如何设计呢？



### 1.2.2.SKU的特有属性

SPU中会有一些特殊属性，用来区分不同的SKU，我们称为SKU特有属性。如华为META10的颜色、内存属性。

不同种类的商品，一个手机，一个衣服，其SKU属性不相同。

同一种类的商品，比如都是衣服，SKU属性基本是一样的，都是颜色、尺码等。

这样说起来，似乎SKU的特有属性也是与分类相关的？事实上，仔细观察你会发现，**SKU的特有属性是商品规格参数的一部分**：

![1526088981953](assets/1526088981953.png)



也就是说，我们没必要单独对SKU的特有属性进行设计，它可以看做是规格参数中的一部分。这样规格参数中的属性可以标记成两部分：

- spu下所有sku共享的规格属性（称为通用属性）
- spu下每个sku不同的规格属性（称为特有属性）

回一下之前我们设计的tb_spec_param表，是不是有一个字段，名为generic，标记通用和特有属性。就是为了这里使用。

这样以来，商品SKU表就只需要设计规格属性以外的其它字段了，规格属性由之前的规格参数表`tb_spec_param`来保存。



但是，规格属性的值依然是需要与商品相关联的。

## 1.3.SPU表

### 1.3.1.表结构

SPU表：

```mysql
CREATE TABLE `tb_spu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'spu id',
  `name` varchar(256) NOT NULL DEFAULT '' COMMENT '商品名称',
  `sub_title` varchar(256) DEFAULT '' COMMENT '副标题，一般是促销信息',
  `cid1` bigint(20) NOT NULL COMMENT '1级类目id',
  `cid2` bigint(20) NOT NULL COMMENT '2级类目id',
  `cid3` bigint(20) NOT NULL COMMENT '3级类目id',
  `brand_id` bigint(20) NOT NULL COMMENT '商品所属品牌id',
  `saleable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否上架，0下架，1上架',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='spu表，该表描述的是一个抽象性的商品，比如 iphone8';
```

与我们前面分析的基本类似，但是似乎少了一些字段，比如商品描述。

我们做了表的垂直拆分，将SPU的详情放到了另一张表：tb_spu_detail

```mysql
CREATE TABLE `tb_spu_detail` (
  `spu_id` bigint(20) NOT NULL,
  `description` text COMMENT '商品描述信息',
  `generic_spec` varchar(2048) NOT NULL DEFAULT '' COMMENT '通用规格参数数据，json格式',
  `special_spec` varchar(1024) NOT NULL COMMENT '特有规格参数及可选值信息，json格式',
  `packing_list` varchar(1024) DEFAULT '' COMMENT '包装清单',
  `after_service` varchar(1024) DEFAULT '' COMMENT '售后服务',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`spu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

这张表中的数据都比较大，为了不影响主表的查询效率我们拆分出这张表。

需要注意的是这两个字段：generic_spec和special_spec。

### 1.3.2.spu中的规格参数

前面讲过规格参数与商品分类绑定，同一分类的商品，会有一套相同的规格参数key（规格参数模板），但是这个分类下每个商品的规格参数值都不相同，因此要满足下面几点：

- 我们有一个规格参数表，跟分类关联，保存的就是某分类下的规格参数模板。

- 我们还需要表，跟商品关联，保存某个商品，相关联的规格参数的值。
- 规格参数因为分成了通用规格参数和特有规格参数，因此规格参数值也需要分别于SPU和SKU关联：
  - 通用的规格参数值与SPU关联。
  - 特有规格参数值与SKU关联。

但是我们并没有增加新的表，来看下我们的 表如何存储这些信息：

#### 1.3.2.1.generic_spec字段

如果要设计一张表，来表示spu中的通用规格属性的值，至少需要下面的字段：

```
spu_id：与哪个商品关联
param_id：是商品的哪个规格参数
value：具体的值
```

我们并没有这么设计。而是把与某个商品相关的规格属性值，直接保存到这个商品spu表中，因此这些规格属性关联的商品就一目了然，那么上述3个属性中的`spu_id`就无需保存了，而剩下的就是`param_id`和规格参数值了。两者刚好是一一对应关系，组成一个键值对。我们刚好可以用一个json结构来标示。

是也就是spuDetail表中的`generic_spec`，其中保存通用规格参数信息的值：

> 整体来看：

 ![1529554390912](assets/1529554390912.png)

json结构，其中都是键值对：

- key：对应的规格参数的`spec_param`的id
- value：对应规格参数的值



#### 1.3.2.2.special_spec字段

我们说spu中只保存通用规格参数，那么为什么有多出了一个`special_spec`字段呢？



以手机为例，品牌、操作系统等肯定是通用规格属性，内存、颜色等肯定是特有属性。

当你确定了一个SPU，比如小米的：红米4X，因为颜色内存等不同，会形成多个sku。如果把每个sku的颜色、内存等信息都整理一下，会形成下面的结果：

```
颜色：[香槟金, 樱花粉, 磨砂黑]
内存：[2G, 3G]
机身存储：[16GB, 32GB]
```

也就是说这里把一个spu下的每个sku的特有规格属性值聚合在了一起！这个就是special_spec字段了。

来看数据格式：

 ![1529554916252](assets/1529554916252.png)

也是json结构：

- key：规格参数id
- value：spu属性的数组



那么问题来：为什么要在spu中把所有sku的规格属性聚合起来保存呢？



因为我们有时候需要把所有规格参数都查询出来，而不是只查询1个sku的属性。比如，商品详情页展示可选的规格参数时：

   ![1526267828817](assets/1526267828817.png)

刚好符合我们的结构，这样页面渲染就非常方便了。



综上所述，spu与商品规格参数模板的关系如图所示：

![1552792606155](assets/1552792606155.png)

## 1.4.SKU表

### 1.4.1.表结构

```mysql
CREATE TABLE `tb_sku` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'sku id',
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `title` varchar(256) NOT NULL COMMENT '商品标题',
  `images` varchar(1024) DEFAULT '' COMMENT '商品的图片，多个图片以‘,’分割',
  `stock` int(8) DEFAULT '9999' COMMENT '库存',
  `price` bigint(16) NOT NULL DEFAULT '0' COMMENT '销售价格，单位为分',
  `indexes` varchar(32) DEFAULT '' COMMENT '特有规格属性在spu属性模板中的对应下标组合',
  `own_spec` varchar(1024) DEFAULT '' COMMENT 'sku的特有规格参数键值对，json格式，反序列化时请使用linkedHashMap，保证有序',
  `enable` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否有效，0无效，1有效',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  PRIMARY KEY (`id`),
  KEY `key_spu_id` (`spu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=27359021554 DEFAULT CHARSET=utf8 COMMENT='sku表,该表表示具体的商品实体,如黑色的 64g的iphone 8';
```



特别需要注意的是sku表中的`indexes`字段和`own_spec`字段。sku中应该保存特有规格参数的值，就在这两个字段中。



### 1.4.2.sku中的特有规格参数

#### 1.4.2.1.indexes字段

在SPU表中，已经对特有规格参数及可选项进行了保存，结构如下：

```json
{
    "4": [
        "香槟金",
        "樱花粉",
        "磨砂黑"
    ],
    "12": [
        "2GB",
        "3GB"
    ],
    "13": [
        "16GB",
        "32GB"
    ]
}
```

这些特有属性如果排列组合，会产生12个不同的SKU，而不同的SKU，其属性就是上面备选项中的一个。

比如：

- 红米4X，香槟金，2GB内存，16GB存储
- 红米4X，磨砂黑，2GB内存，32GB存储

你会发现，每一个属性值，对应于SPUoptions数组的一个选项，如果我们记录下角标，就是这样：

- 红米4X，0,0,0
- 红米4X，2,0,1

既然如此，我们是不是可以将不同角标串联起来，作为SPU下不同SKU的标示。这就是我们的indexes字段。

 ![1526266901335](assets/1526266901335.png)

这个设计在商品详情页会特别有用：

 ![1526267180997](assets/1526267180997.png)

当用户点击选中一个特有属性，你就能根据 角标快速定位到sku。

#### 1.4.2.2.own_spec字段

看结构：

```json
{"4":"香槟金","12":"2GB","13":"16GB"}
```

保存的是特有属性的键值对。

SPU中保存的是可选项，但不确定具体的值，而SKU中的保存的就是具体的值。



## 1.5.导入图片信息

现在商品表中虽然有数据，但是所有的图片信息都是无法访问的，看看数据库中给出的图片信息：

![1552812056893](assets/1552812056893.png)

现在这些图片的地址指向的是`http://image.leyou.com`这个域名。而之前我们在nginx中做了反向代理，把图片地址指向了阿里巴巴的OSS服务。

我们可以在课前资料中找到这些商品的图片：

![1552812290689](assets/1552812290689.png)

并且把这些图片放到nginx目录，然后由nginx对其加载即可。

首先，我们需要把图片放到nginx/html目录，并且解压缩：

![1552812370270](assets/1552812370270.png)



修改Nginx中，有关`image.leyou.com`的监听配置，使nginx来加载图片地址：

```nginx
server {
	listen       80;
	server_name  image.leyou.com;
	location /images { # 监听以/images打头的路径，
		root	html;
	}
	location / {
		proxy_pass   https://ly-images.oss-cn-shanghai.aliyuncs.com;
	}
}
```

再次测试：

![1552812690883](assets/1552812690883.png)



# 2.商品查询

## 重点：完成java代码，并返回正确分页结果

## 2.1.效果预览

接下来，我们实现商品管理的页面，先看下我们要实现的效果：

![1526268595873](assets/1526268595873.png)

可以看出整体是一个table，然后有新增按钮。是不是跟昨天写品牌管理很像？

## 2.2.页面请求

点击页面菜单中的商品列表页面：

![1552813666829](assets/1552813666829.png) 

页面右侧是一个空白的数据表格：

![1552814807340](assets/1552814807340.png)

看到浏览器发起已经发起了查询商品数据的请求：

 ![1528082212618](assets/1528082212618.png)

因此接下来，我们编写接口即可。



## 2.3.后台提供接口

页面已经准备好，接下来在后台提供分页查询SPU的功能：

### 2.3.1.实体类

> SPU

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
 * spu表，该表描述的是一个抽象性的商品
 * </p>
 *
 * @author HeiMa
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbSpu extends Model<TbSpu> {

private static final long serialVersionUID=1L;

    /**
     * spu id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 副标题，一般是促销信息
     */
    private String subTitle;

    /**
     * 1级类目id
     */
    private Long cid1;

    /**
     * 2级类目id
     */
    private Long cid2;

    /**
     * 3级类目id
     */
    private Long cid3;

    /**
     * 商品所属品牌id
     */
    private Long brandId;

    /**
     * 是否上架，0下架，1上架
     */
    private Boolean saleable;

    /**
     * 添加时间
     */
    private Date createTime;

    /**
     * 最后修改时间
     */
    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}

```



> SPU详情

```java
package com.leyou.item.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbSpuDetail extends Model<TbSpuDetail> {

private static final long serialVersionUID=1L;

    /**
    *注意这里 使用 IdType.INPUT
    *表示 主键id 不是自增，而是由用户输入
    **/
    @TableId(type = IdType.INPUT)
    private Long spuId;

    /**
     * 商品描述信息
     */
    private String description;

    /**
     * 通用规格参数数据
     */
    private String genericSpec;

    /**
     * 特有规格参数及可选值信息，json格式
     */
    private String specialSpec;

    /**
     * 包装清单
     */
    private String packingList;

    /**
     * 售后服务
     */
    private String afterService;

    private Date createTime;

    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.spuId;
    }

}

```

### 2.3.2.请求业务分析

先分析：

- 请求方式：GET

- 请求路径：/spu/page

- 请求参数：

  - page：当前页
  - rows：每页大小
  - key：过滤条件
  - saleable：上架或下架

- 返回结果：通过页面能看出来，查询要展示的数据都是SPU数据，而且因为是分页查询，我们可以返回与之前品牌查询一样的PageResult。

  - 要注意，页面展示的中需要是商品分类和品牌名称，而SPU表中中保存的是id，我们需要在DTO中处理这些字段。

    我们可以对Spu拓展categoryName和brandName属性：

    ```java
    @Data
    public class SpuDTO {
        private Long id;
        private Long brandId;
        private Long cid1;// 1级类目
        private Long cid2;// 2级类目
        private Long cid3;// 3级类目
        private String name;// 名称
        private String subTitle;// 子标题
        private Boolean saleable;// 是否上架
        private Date createTime;// 创建时间
        private String categoryName; // 商品分类名称拼接
        private String brandName;// 品牌名称
    
        /**
         * 方便同时获取3级分类
         * @return
         */
        @JsonIgnore //序列化时，不会对这个方法进行序列化
        public List<Long> getCategoryIds(){
            return Arrays.asList(cid1, cid2, cid3);
        }
    }
    ```
    
    需要注意的是，这里通过`@JsonIgnore`注解来忽略这个`getCategoryIds`方法，避免将其序列化到JSON结果
    

因此这里需要在`ly-item-pojo`中引入Jackson依赖：
    
```xml
    <dependencies>
    <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>2.9.0</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
```

​    

### 2.3.3 分类和品牌查询功能

在SpuDTO中，需要的categoryName和brandName属性。而Spu中只有多级分类id：cid1、cid2、cid3，另外还有brandId。所以我们必须增加新的功能：

- 根据分类id的集合查询商品分类
- 根据品牌id查询品牌

### 2.3.5.编写controller代码：

我们把与商品相关的一切业务接口都放到一起，起名为GoodsController，业务层也是这样

```java
package com.leyou.item.web;

import com.leyou.common.vo.PageResult;
import com.leyou.item.dto.SpuDTO;
import com.leyou.item.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    /**
     * 查询spu列表
     * @param page
     * @param rows
     * @param key
     * @param saleable
     * @return
     */
    @GetMapping("/spu/page")
    public ResponseEntity<PageResult> findSpuByPage(
        @RequestParam(name = "page",defaultValue = "1") Integer page,
                                                    
        @RequestParam(name = "rows",defaultValue = "5") Integer rows,                                         
        @RequestParam(name = "key",required = false) String key,                                    
        @RequestParam(name = "saleable",required = false) Boolean saleable){
        PageResult<SpuDTO> pageResult = goodsService.findSpuByPage(page,rows,key,saleable);
        return ResponseEntity.ok(pageResult);
    }
}
```



### 2.3.6.service

所有商品相关的业务（包括SPU和SKU）放到一个业务下：GoodsService。

```java
public PageResult<SpuDTO> findSpuByPage(Integer page, Integer rows, String key, Boolean saleable) {
       //分页信息
        IPage<TbSpu> spuPage = new Page<>(page,rows);
        //查询条件
        QueryWrapper<TbSpu> queryWrapper = new QueryWrapper<>();
        //模糊查询 商品 名称
        if(!StringUtils.isEmpty(key)){
            queryWrapper.lambda().like(TbSpu::getName,key);
        }
        //是否上下架
        if(saleable != null){
            queryWrapper.lambda().eq(TbSpu::getSaleable,saleable);
        }
        //排序
        queryWrapper.lambda().orderByDesc(TbSpu::getUpdateTime);
        //分页查询
        IPage<TbSpu> spuIPage = spuService.page(spuPage, queryWrapper);
        if(spuIPage == null || CollectionUtils.isEmpty(spuIPage.getRecords())){
            throw  new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        //转bean
        List<SpuDTO> spuDTOS = BeanHelper.copyWithCollection(spuIPage.getRecords(), SpuDTO.class);
        //处理 品牌名称和分类名称
        this.handleBrandAndCategoryName(spuDTOS);
        //包装PageResult
        PageResult<SpuDTO> pageResult = new PageResult<>(spuDTOS,spuIPage.getTotal(),spuIPage.getPages());
        return pageResult;
    }

    private void handleBrandAndCategoryName(List<SpuDTO> spuDTOList){
        for (SpuDTO spuDTO : spuDTOList) {
            //商品所属 分类
            List<Long> categoryIdList = spuDTO.getCategorys();
            Collection<TbCategory> tbCategoryCollection = categoryService.listByIds(categoryIdList);
            String categoryName = tbCategoryCollection.stream().map(TbCategory::getName).collect(Collectors.joining("/"));
            spuDTO.setCategoryName(categoryName);
            //商品品牌
            Long brandId = spuDTO.getBrandId();
            TbBrand brand = brandService.getById(brandId);
            spuDTO.setBrandName(brand.getName());

        }
    }
```



## 2.4.测试

刷新页面，查看效果：

![1552824903094](assets/1552824903094.png)



基本与预览的效果一致，OK！



# 3.商品新增

## 重点：了解商品新增处理的表，表之间的关系，并完成java代码编写，正确处理业务

## 3.1.页面预览

当我们点击新增商品按钮：

 ![1528083727447](assets/1528083727447.png)

就会出现一个弹窗：

![1528086595597](assets/1528086595597.png)

里面把商品的数据分为了4部分来填写：

- 基本信息：主要是一些简单的文本数据，包含了SPU和SpuDetail的部分数据，如
  - 商品分类：是SPU中的`cid1`，`cid2`，`cid3`属性
  - 品牌：是spu中的`brandId`属性
  - 标题：是spu中的`title`属性
  - 子标题：是spu中的`subTitle`属性
  - 售后服务：是SpuDetail中的`afterService`属性
  - 包装列表：是SpuDetail中的`packingList`属性
- 商品描述：是SpuDetail中的`description`属性，数据较多，所以单独放一个页面
- 规格参数：商品规格信息，对应SpuDetail中的`genericSpec`属性
- SKU属性：spu下的所有Sku信息

也就是说这个页面包含了商品相关的四张表中的数据：

- tb_spu
- tb_spu_detail
- tb_sku
- tb_stock

## 3.3.基本数据

我们先来看下基本数据：

![1528086595597](assets/1528086595597.png)

### 3.3.1.商品分类

商品分类的级联选框我们之前在品牌查询已经做过，是要根据分类的pid查询分类，所以这里的级联选框已经实现完成：

刷新页面，可以看到请求已经发出：

 ![1528101483023](assets/1528101483023.png)

 ![1528101622952](assets/1528101622952.png)



 ![1528101649596](assets/1528101649596.png)

需要注意的是，这里选中以后会显示3级分类，因为数据库中保存的就是商品的1~3级类目。

### 3.3.2.品牌选择-后台接口

品牌也是一个下拉选框，不过其选项是不确定的，只有当用户选择了商品分类，才会把这个分类下的所有品牌展示出来。

所以页面编写了watch函数，监控商品分类的变化，每当商品分类值有变化，就会发起请求，查询品牌列表。刷新页面，当选中一个分类时，可以看到请求发起：

![1552822964914](assets/1552822964914.png)

接下来，我们只要编写后台接口，根据商品分类id，查询对应品牌即可。

#### 后台接口

页面需要去后台查询品牌信息，我们自然需要提供：

> controller

```java
/**
     * 根据分类ID查询品牌列表
     * @param categoryId 商品分类id
     * @return 该分类下的品牌的集合
     */
@GetMapping("/of/category")
public ResponseEntity<List<BrandDTO>> queryByCategoryId(@RequestParam("id") Long categoryId) {
    return ResponseEntity.ok(this.brandService.queryByCategoryId(categoryId));
}
```

> service

```java
@Override
    public List<BrandDTO> findBrandByCategoryId(Long cid) {
        List<TbBrand> brandList = this.baseMapper.selectBrandJoinCategory(cid);
        if(CollectionUtils.isEmpty(brandList)){
            throw new LyException(ExceptionEnum.BRAND_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(brandList,BrandDTO.class);
    }
```



> mapper

根据分类查询品牌有中间表，需要自己编写Sql：

```java
@Select("SELECT b.* FROM tb_category_brand a ,tb_brand b WHERE a.`brand_id` = b.`id` AND a.`category_id`=#{cid}")
    List<TbBrand> selectBrandJoinCategory(@Param("cid") Long cid);
```



效果：

 ![1528102194745](assets/1528102194745.png)

### 3.3.3.其它文本框

剩余的几个属性：标题、子标题等都是普通文本框，我们直接填写即可，没有需要特别注意的。



## 3.4.商品描述-富文本编辑器（了解）

商品描述信息比较复杂，而且图文并茂，甚至包括视频。

这样的内容，一般都会使用富文本编辑器。

### 3.4.1.什么是富文本编辑器

百度百科：

![1526290914491](assets/1526290914491.png)

通俗来说：富文本，就是比较丰富的文本编辑器。普通的框只能输入文字，而富文本还能给文字加颜色样式等。

富文本编辑器有很多，例如：KindEditor、Ueditor。但并不原生支持vue

但是我们今天要说的，是一款支持Vue的富文本编辑器：`vue-quill-editor`



### 1.4.2.Vue-Quill-Editor

GitHub的主页：https://github.com/surmon-china/vue-quill-editor

Vue-Quill-Editor是一个基于Quill的富文本编辑器：[Quill的官网](https://quilljs.com/)

![1526291232678](assets/1526291232678.png)



### 1.4.3.使用指南

使用非常简单：

第一步：安装，使用npm命令：

```
npm install vue-quill-editor --save

```

第二步：加载，在js中引入：

全局使用：

```js
import Vue from 'vue'
import VueQuillEditor from 'vue-quill-editor'

const options = {}; /* { default global options } */

Vue.use(VueQuillEditor, options); // options可选
```



局部使用：

```js
import 'quill/dist/quill.core.css'
import 'quill/dist/quill.snow.css'
import 'quill/dist/quill.bubble.css'

import {quillEditor} from 'vue-quill-editor'

var vm = new Vue({
    components:{
        quillEditor
    }
})
```



第三步：页面引用：

```html
<quill-editor v-model="goods.spuDetail.description" :options="editorOption"/>
```



### 1.4.4.自定义的富文本编辑器

不过这个组件有个小问题，就是图片上传的无法直接上传到后台，因此我们对其进行了封装，支持了图片的上传。

 ![1526296083605.png](assets/1526296083605.png)

使用也非常简单：

```html
<v-stepper-content step="2">
    <v-editor v-model="goods.spuDetail.description" url="/upload/signature" needSignature/>
</v-stepper-content>
```

- url：是图片上传的路径或者上传阿里OSS时的签名路径，这里输入的是签名路径
- v-model：双向绑定，将富文本编辑器的内容绑定到goods.spuDetail.description



### 1.4.5.效果：

![1526297276667](assets/1526297276667.png)



## 3.5.商品规格参数

规格参数的查询我们之前也已经编写过接口，因为商品规格参数也是与商品分类绑定，所以需要在商品分类变化后去查询，我们也是通过watch监控来实现：

 

可以看到这里是根据商品分类id查询规格参数：SpecParam。我们之前写过一个根据gid（分组id）来查询规格参数的接口，我们可以对其进行扩展：

> ### 改造查询规格参数接口

我们在原来的根据 gid（规格组id)查询规格参数的接口上，添加一个参数：cid，即商品分类id。

等一下， 考虑到以后可能还会根据是否搜索(searching)字段来过滤，我们多添加一个过滤条件：

```java
   /**
     * 查询规格参数
     * @param gid 组id
     * @param cid 分类id
     * @param searching 是否用于搜索
     * @return 规格组集合
     */
    @GetMapping("/params")
    public ResponseEntity<List<SpecParamDTO>> querySpecParams(
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "searching", required = false) Boolean searching
    ) {
        return ResponseEntity.ok(specService.querySpecParams(gid, cid, searching));
    }
```

改造service：

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

如果param中有属性为null，则不会吧属性作为查询条件，因此该方法具备通用性，即可根据gid查询，也可根据cid查询。

测试：

 ![1529631805801](assets/1529631805801.png)



刷新页面测试：

 ![1529631887407](assets/1529631887407.png)



## 3.6.SKU信息

Sku属性是SPU下的每个商品的不同特征，如图：

![1529656674978](assets/1529656674978.png)

当我们填写一些属性后，会在页面下方生成一个sku表格，大家可以计算下会生成多少个不同属性的Sku呢？

当你选择了上图中的这些选项时：

- 颜色共2种：夜空黑，绚丽红
- 内存共2种：4GB，6GB
- 机身存储1种：64GB

此时会产生多少种SKU呢？ 应该是 2 * 2 * 1 = 4种，这其实就是在求笛卡尔积。

我们会在页面下方生成一个sku的表格：

![1528856353718](assets/1528856353718.png)

这个表格中就包含了以上颜色内存的所有可能组合，剩下的价格等信息就需要用户自己来完成了。

注意，页面中的sku的图片上传，默认是上传到本地，这样可能上传后就无法找到图片，需要修改nginx：

如果是上传到本地，则需要在nginx中配置图片地址的判断：

```nginx
server {
	listen       80;
	server_name  image.leyou.com;
	location /images {
		root	html;
	}
	location / {
		root 	html;
		if (!-f $request_filename) {
            proxy_pass http://heima64.oss-cn-shanghai.aliyuncs.com;
            break;
        }
	}
}
```

- `if (!-f $request_filename)`：判断是否在本地能找到图片，找不到才反向代理到阿里去找图片

 

**如果不想修改nginx**，也可以修改成上传到阿里，这样nginx就无需修改了：

![1557134693791](assets/1557134693791.png)

大概是在119行。

## 3.7.页面表单提交

在sku列表的下方，有一个提交按钮：

![1528860775149](assets/1528860775149.png)

点击提交，查看控制台提交的数据格式：

![1552827511270](assets/1552827511270.png)

- 整体是一个json格式数据，包含Spu表所有数据：

  - brandId：品牌id

  - cid1、cid2、cid3：商品分类id

  - subTitle：副标题

  - name：商品名称

  - spuDetail：是一个json对象，代表商品详情表数据

    ![1552827536439](assets/1552827536439.png)

    - afterService：售后服务
    - description：商品描述
    - packingList：包装列表
    - specialSpec：sku规格属性模板
    - genericSpec：通用规格参数

  - skus：spu下的所有sku数组，元素是每个sku对象：

    ![1552827560584](assets/1552827560584.png)

    - title：标题
    - images：图片
    - price：价格
    - stock：库存
    - ownSpec：特有规格参数
    - indexes：特有规格参数的下标

## 3.8.后台实现

### 重点：清楚新增操作3张表，了解@TableId注解的作用

### 3.8.1.实体类

> Sku

```java
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TbSku extends Model<TbSku> {

private static final long serialVersionUID=1L;

    /**
     * sku id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * spu id
     */
    private Long spuId;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品的图片，多个图片以‘,’分割
     */
    private String images;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 销售价格，单位为分
     */
    private Long price;

    /**
     * 特有规格属性在spu属性模板中的对应下标组合
     */
    private String indexes;

    /**
     * sku的特有规格参数键值对，json格式，反序列化时请使用linkedHashMap，保证有序
     */
    private String ownSpec;

    /**
     * 是否有效，0无效，1有效
     */
    private Boolean enable;

    /**
     * 添加时间
     */
    private Date createTime;

    /**
     * 最后修改时间
     */
    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}

```



然后是对应的DTO：

```java
@Data
public class SkuDTO {
    private Long id;
    private Long spuId;
    private String title;
    private String images;
    private Long price;
    private String ownSpec;// 商品特殊规格的键值对
    private String indexes;// 商品特殊规格的下标
    private Boolean enable;// 是否有效，逻辑删除用
    private Integer stock; // 库存
}
```



### 3.8.2.请求分析

四个问题：

- 请求方式：POST
- 请求路径：/goods
- 请求参数：SpuDTO的json格式的对象，但是要包含spuDetail和Sku集合。我们需要拓展SpuDTO，来接收其中的数据
- 返回类型：无

首先要定义一个SpuDetailDTO：

```java
@Data
public class SpuDetailDTO {
    private Long spuId;// 对应的SPU的id
    private String description;// 商品描述
    private String specialSpec;// 商品特殊规格的名称及可选值模板
    private String genericSpec;// 商品的全局规格属性
    private String packingList;// 包装清单
    private String afterService;// 售后服务
}
```

然后修改SpuDTO，在里面包含SpuDetail和Sku的集合

```java
package com.leyou.item.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * spu表，该表描述的是一个抽象性的商品，比如 iphone8
 * </p>
 *
 * @author HM
 * @since 2019-12-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SpuDTO {


    /**
     * spu id
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 副标题，一般是促销信息
     */
    private String subTitle;

    /**
     * 1级类目id
     */
    private Long cid1;

    /**
     * 2级类目id
     */
    private Long cid2;

    /**
     * 3级类目id
     */
    private Long cid3;

    /**
     * 商品所属品牌id
     */
    private Long brandId;

    /**
     * 是否上架，0下架，1上架
     */
    private Boolean saleable;
    /**
     * 品牌名称
     */
    private String brandName;
    /**
     * 分类名称
     * 3级分类名称都要
     */
    private String categoryName;

//    商品详情 spu_detail,spu 和 spu_detail表的关系
    private SpuDetailDTO spuDetail;
//   sku表
    private List<SkuDTO> skus;

    @JsonIgnore //当序列化json的时候，不会序列化当前的方法
    public List<Long> getCategoryIds(){
        return Arrays.asList(cid1,cid2,cid3);
    }


}

```

接下来就可以编写业务了

### 3.8.3.业务代码

> controller

代码：

```java
/**
     * 新增商品
     * @param spuDTO 页面提交商品信息
     * @return
     */
@PostMapping("/goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuDTO spuDTO){
        goodsService.saveGoods(spuDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
```

注意：通过@RequestBody注解来接收Json请求

> Service

这里的逻辑比较复杂，我们除了要对SPU新增以外，还要对SpuDetail、Sku、Stock进行保存

```java
@Transactional(rollbackFor = Exception.class)
    public void saveGoods(SpuDTO spuDTO) {
        //保存spu信息
        TbSpu tbSpu = BeanHelper.copyProperties(spuDTO, TbSpu.class);
        boolean bSpu = spuService.save(tbSpu);
        if(!bSpu){
            throw  new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
        Long spuId = tbSpu.getId();
        //详情表
        SpuDetailDTO spuDetailDTO = spuDTO.getSpuDetail();
        TbSpuDetail tbSpuDetail = BeanHelper.copyProperties(spuDetailDTO, TbSpuDetail.class);
        tbSpuDetail.setSpuId(spuId);
        //保存spudetail信息
        boolean bDetail = detailService.save(tbSpuDetail);
        if(!bDetail){
            throw  new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
        //保存sku信息
       List<SkuDTO> skuDTOList = spuDTO.getSkus();
        if(CollectionUtils.isEmpty(skuDTOList)){
            throw new LyException(ExceptionEnum.INVALID_PARAM_ERROR);
        }
        List<TbSku> tbSkuList = BeanHelper.copyWithCollection(skuDTOList, TbSku.class);
        for (TbSku tbSku : tbSkuList) {
            tbSku.setSpuId(spuId);
        }
        boolean b2 = tbSkuService.saveBatch(tbSkuList);
        if(!b2){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }

    }
```



# 4.商品上下架

### 重点：了解上下架的逻辑和要处理的表，完成代码编写，正确实现业务

在商品详情页，每一个商品后面，都会有一个编辑按钮：

 ![1528874711108](assets/1528874711108.png)

点击这个按钮，并没有打开商品编辑窗口，而是弹出了一个提示窗口：

![1552901853155](assets/1552901853155.png)

已经上架的商品用户可能正在购买，所以不能修改。必须要先下架才可以。

### 4.1.页面请求

此时打开控制台，可以看到请求已经发出了：

![1552902179096](assets/1552902179096.png)

请求方式：PUT

请求路径：/spu/saleable

参数有两个 ：

- id：应该是spu的id
- saleable：布尔值，代表上架或下架

返回结果：应该是无



### 4.2.后台实现

接下来我们在服务端接收请求，并且修改spu的saleable属性。

需要注意的是，我们在修改spu属性的同时，还需要修改sku的enable属性，因为spu下架，sku也要跟着进行下架

controller：

```java
/**
     * 修改商品上下架
     * @param id 商品spu的id
     * @param saleable true：上架；false：下架
     * @return
     */
@PutMapping("spu/saleable")
public ResponseEntity<Void> updateSpuSaleable(@RequestParam("id") Long id, @RequestParam("saleable") Boolean saleable) {
    goodsService.updateSaleable(id, saleable);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
}
```



service：

```java
@Transactional(rollbackFor = Exception.class)
    public void upateSaleable(Long id, Boolean saleable) {
        //更新spu
        TbSpu tbSpu = new TbSpu();
        tbSpu.setId(id);
        tbSpu.setSaleable(saleable);
        boolean bSpu = spuService.updateById(tbSpu);
        if(!bSpu){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
        //更新sku
        UpdateWrapper<TbSku> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(TbSku::getSpuId,id);
        updateWrapper.lambda().set(TbSku::getEnable,saleable);
        boolean bSku = skuService.update(updateWrapper);
        if(!bSku){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
    }
```



# 5.商品数据回显

### 重点：了解商品回显需要的数据，完成java代码，返回正确结果

### 5.1.编辑按钮点击事件

点击下架商品后，再次点击修改，发现这次没有弹出警告，但是编辑窗口也没有弹出，怎么回事呢？

打开控制台，发现发起了一次请求：

![1552914451933](assets/1552914451933.png)



编辑是需要数据回显的，而表格数据中只有spu信息，没有spuDetail和sku信息，需要去服务端查询。

因此，接下来我们就编写后台接口，提供查询spuDetail和sku接口。

### 5.2.查询SpuDetail接口

> controller

需要分析的内容：

- 请求方式：GET
- 请求路径：/spu/detail
- 请求参数：id，应该是spu的id
- 返回结果：SpuDetailDTO对象



```java
/**
     * 根据spuID查询spuDetail
     * @param id spuID
     * @return SpuDetail
     */
@GetMapping("/spu/detail")
public ResponseEntity<SpuDetailDTO> findSpuDetailById(@RequestParam("id") Long id) {
    return ResponseEntity.ok(goodsService.querySpuDetailById(id));
}
```

> service

```java
public SpuDetailDTO findSpuDetail(Long spuId) {
        TbSpuDetail spuDetail = detailService.getById(spuId);
        if(spuDetail == null){
            throw  new LyException(ExceptionEnum.GOODS_DETAIL_NOT_FOUND);
        }
        return BeanHelper.copyProperties(spuDetail,SpuDetailDTO.class);
    }
```

测试：

![1552915552485](assets/1552915552485.png)

### 5.3.查询sku

再次点击修改， 发现又发出了一次请求，这次应该是查询sku：

![1552915043070](assets/1552915043070.png)

> 分析

- 请求方式：Get
- 请求路径：/sku/of/spu
- 请求参数：id，应该是spu的id
- 返回结果：sku的集合

> controller

```java
/**
     * 根据spuID查询sku
     * @param id spuID
     * @return sku的集合
     */
@GetMapping("/sku/of/spu")
    public ResponseEntity<List<SkuDTO>> findSkuBySpuId(@RequestParam(name="id") Long spuId){
        List<SkuDTO> skuDTOList = goodsService.findSkuListBySpuId(spuId);
        return ResponseEntity.ok(skuDTOList);
    }
```



> service

需要注意的是，为了页面回显方便，我们一并把sku也查询出来

```java
public List<SkuDTO> findSkuListBySpuId(Long spuId) {
        QueryWrapper<TbSku> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TbSku::getSpuId,spuId);
        List<TbSku> tbSkuList = skuService.list(queryWrapper);
        if(CollectionUtils.isEmpty(tbSkuList)){
            throw  new LyException(ExceptionEnum.GOODS_NOT_FOUND);
        }
        return BeanHelper.copyWithCollection(tbSkuList,SkuDTO.class);
    }
```

> 测试：

![1552915617879](assets/1552915617879.png)



### 5.4.页面回显

随便点击一个编辑按钮，发现数据回显完成：

![1528875639346](assets/1528875639346.png)

![1528875657327](assets/1528875657327.png)





![1528875725545](assets/1528875725545.png)

 ![1528875747067](assets/1528875747067.png)

# 6.商品数据修改

### 重点：了解修改对SKU的表的修改

这里的保存按钮与新增其实是同一个，因此提交的逻辑也是一样的，这里不再赘述。

随便修改点数据，然后点击保存，可以看到浏览器已经发出请求：

![1552916650110](assets/1552916650110.png)

区别在于，这次的请求中带上了id信息，因为需要根据id来修改数据

包括spuDetail也是一样：

![1552916682788](assets/1552916682788.png)

但是再来看sku：

![1552916705801](assets/1552916705801.png)

发现sku中并没有带上sku的id信息，为什么呢？如果没有id我们又该怎样修改呢？

这是因为sku的规格参数修改或删减，可能会导致新增很多sku或者以前的sku直接消失，无法去做修改操作。因此建议的业务逻辑是先对sku进行删除，然后再进行新增。



## 6.1.后台实现

接下来，我们编写后台，实现修改商品接口。

### 6.1.1.Controller

- 请求方式：PUT
- 请求路径：/goods
- 请求参数：SpuDTO对象
- 返回结果：无

```java
/**
     * 修改商品
     * @param spu
     * @return
     */
	@PutMapping("/goods")
    public ResponseEntity<Void> updateGoods(@RequestBody SpuDTO spuDTO){
        goodsService.updateGoods(spuDTO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
```



### 6.2.2.Service

spu数据可以修改，但是SKU数据无法修改，因为有可能之前存在的SKU现在已经不存在了，或者以前的sku属性都不存在了。比如以前内存有4G，现在没了。

因此这里直接删除以前的SKU，然后新增即可。

代码：

```java
/**
     * 修改商品
     * @param spuDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateGoods(SpuDTO spuDTO) {
        Long spuId = spuDTO.getId();
//        修改spu
        TbSpu tbSpu = BeanHelper.copyProperties(spuDTO, TbSpu.class);
        boolean b = spuService.updateById(tbSpu);
        if(!b){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
//        修改spudetail
        TbSpuDetail tbSpuDetail = BeanHelper.copyProperties(spuDTO.getSpuDetail(), TbSpuDetail.class);
        boolean b1 = spuDetailService.updateById(tbSpuDetail);
        if(!b1){
            throw new LyException(ExceptionEnum.UPDATE_OPERATION_FAIL);
        }
//        删除sku
//        delete from tb_sku where spu_id=?
        QueryWrapper<TbSku> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TbSku::getSpuId,spuId);
        boolean b2 = skuService.remove(queryWrapper);
        if(!b2){
            throw new LyException(ExceptionEnum.DELETE_OPERATION_FAIL);
        }
//        新增sku
        List<SkuDTO> skuDTOList = spuDTO.getSkus();
        List<TbSku> tbSkuList = BeanHelper.copyWithCollection(skuDTOList, TbSku.class);
        for (TbSku tbSku : tbSkuList) {
            tbSku.setSpuId(spuId);
        }
//        保存sku
        boolean b3 = skuService.saveBatch(tbSkuList);
        if(!b3){
            throw new LyException(ExceptionEnum.INSERT_OPERATION_FAIL);
        }
    }
```

# 🎗经验分享-修改商品

## 1、需求

> 在商品列表中，找到下架的商品，点击编辑，来修改商品信息

操作界面：

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594127604.png)

## 2、报错信息

> 点击编辑后，发现并没有显示商品编辑页面，我们打开Chrome浏览器的开发工具，发现有报错，点击Network后信息如下：
>
> 在请求http://api.leyou.com/api/item/spu/detail?id=2 时出现错误，这个请求用spuid来查询spuDetail（商品详情）的数据。

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594127956.png)

> 然后点击 Preview，可以看到具体报错，提示商品不存在，可以我们在商品列表中是可以看到这个商品的。

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594128074.png)

## 3、问题分析

> 分析：
>
> 从客户端发出的请求来看，并不能看出什么错误。这时就需要到服务端找到对应的微服务，查看微服务的输出日志，来找问题。

去item服务查看日志

> 在item服务的日志中我们发现有这样的错误信息，如图:

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594127392-1594128652657.png)

> 在这个错误信息中，可以发现 Where null = 2  这样的语句。 这个SQL是使用spuid查询tb_spu_detail的数据，tb_spu表和tb_spu_detail表是一对一的关系，也就是spu_id也是spu_detail的主键。
>
> 那么在where语句中，应该出现spu_id这个字段的名字，可是这里却 是null。
>
> 错误正式因为这个错误的where条件引起的。当前我们使用MybaitsPlus，这个SQL是由MybaitsPlus自动为我们生成的，难道是MybaitsPlus出错了？

## 4、寻找真正的错误

> 当前我们使用的持久层框架是MybaitsPlus，在MybaitsPlus的注解中有一个在实体类中可以标识主键的注解
>
> @TableId。
>
> 我们查看tb_spu_detail表对应的实体类TbSpuDetail，发现在spuId上并没有@TableId注解。MybaitsPlus的生成器在生成实体类时，如果主键的生成策略不是自增（AUTO_INCREMENT）而是需要用户输入，那么在生成实体类的时候，就不会给这个主键对应的属性添加@TableId注解。
>
> 由于TbSpuDetail类缺少@TableId注解，导致在使用spuId查询spuDetail数据的时候不知道哪个属性是注解。SQL语句的where条件 出现  null = 2

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594129885.png)

## 5、解决错误

> 现在给spuId这个属性添加@TableId注解，标识这个属性对应的主键名字。

代码：

```java
@TableId(value = "spu_id",type = IdType.INPUT)
```

属性解释：

> value：当前属性对应的字段名字
>
> type：当前主键的生成策略。   IdType.INPUT -- 代表用户输入

## 6、验证

> 加上注解后，重启item服务验证，页面上点击编辑按钮，发现请求可以正常返回数据，编辑窗口也显示出来，如图：

![](../../../%E8%AF%BE%E7%A8%8B/%E9%A1%B9%E7%9B%AE%E4%BA%8C/%E4%B9%90%E4%BC%98%E5%95%86%E5%9F%8E-%E6%89%80%E6%9C%89%E7%AC%94%E8%AE%B0-%E5%B8%A6%E7%BB%8F%E9%AA%8C%E5%80%BC/assets/1594132653.png)





# 7.搭建前台系统

## 重点：能够完成前台系统的搭建，并运行看到页面效果

后台系统的内容暂时告一段落，有了商品，接下来我们就要在页面展示商品，给用户提供浏览和购买的入口，那就是我们的门户系统。

门户系统面向的是用户，安全性很重要，而且搜索引擎对于单页应用并不友好。因此我们的门户系统不再采用与后台系统类似的SPA（单页应用）。

依然是前后端分离，不过前端的页面会使用独立的html，在每个页面中使用vue来做页面渲染。



## 7.1.静态资源

webpack打包多页应用配置比较繁琐，项目结构也相对复杂。这里为了简化开发（毕竟我们不是专业的前端人员），我们不在使用webpack，而是直接编写原生的静态HTML。

### 7.1.1.导入静态资源

将课前资料中的leyou-portal解压，并把结果赋值到工作空间的目录

 ![1526460560069](assets/1526460560069.png)

解压缩：

 ![1526460681615](assets/1526460681615.png)

然后通过idea打开，可以看到项目结构：

 ![1526460701617](assets/1526460701617.png)



## 7.2.添加启动脚本

初始化npm

```
npm init -y
```

安装vue

```
npm install vue --save
```

配置启动脚本：

进入package.json文件，在script中添加启动脚本：

```json
{
  "name": "leyou-portal",
  "version": "1.0.0",
  "description": "",
  "main": "index.js",
  "scripts": {
    "test": "echo \"Error: no test specified\" && exit 1",
    "serve": "live-server --port=9002"
  },
  "keywords": [],
  "author": "",
  "license": "ISC",
  "dependencies": {
    "vue": "^2.6.10"
  }
}
```

以后可以用 npm run serve启动

## 7.3.live-server

没有webpack，我们就无法使用webpack-dev-server运行这个项目，实现热部署。

所以，这里我们使用另外一种热部署方式：live-server，

### 7.3.1.简介

地址；https://www.npmjs.com/package/live-server

 ![1526460917348](assets/1526460917348.png)

这是一款带有热加载功能的小型开发服务器。用它来展示你的HTML / JavaScript / CSS，但不能用于部署最终的网站。 

### 7.3.2.安装

安装live-server，这里建议全局安装，以后任意位置可用

```
npm install -g live-server
```



运行时，直接输入命令：

```
live-server
```

另外，你可以在运行命令后，跟上一些参数以配置：

- `--port=NUMBER` - 选择要使用的端口，默认值：PORT env var或8080
- `--host=ADDRESS` - 选择要绑定的主机地址，默认值：IP env var或0.0.0.0（“任意地址”）
- `--no-browser` - 禁止自动Web浏览器启动
- `--browser=BROWSER` - 指定使用浏览器而不是系统默认值
- `--quiet | -q` - 禁止记录
- `--verbose | -V` - 更多日志记录（记录所有请求，显示所有侦听的IPv4接口等）
- `--open=PATH` - 启动浏览器到PATH而不是服务器root
- `--watch=PATH` - 用逗号分隔的路径来专门监视变化（默认值：观看所有内容）
- `--ignore=PATH`- 要忽略的逗号分隔的路径字符串（[anymatch](https://github.com/es128/anymatch) -compatible definition）
- `--ignorePattern=RGXP`-文件的正则表达式忽略（即`.*\.jade`）（**不推荐使用**赞成`--ignore`）
- `--middleware=PATH` - 导出要添加的中间件功能的.js文件的路径; 可以是没有路径的名称，也可以是引用`middleware`文件夹中捆绑的中间件的扩展名
- `--entry-file=PATH` - 提供此文件（服务器根目录）代替丢失的文件（对单页应用程序有用）
- `--mount=ROUTE:PATH` - 在定义的路线下提供路径内容（可能有多个定义）
- `--spa` - 将请求从/ abc转换为/＃/ abc（方便单页应用）
- `--wait=MILLISECONDS` - （默认100ms）等待所有更改，然后重新加载
- `--htpasswd=PATH` - 启用期待位于PATH的htpasswd文件的http-auth
- `--cors` - 为任何来源启用CORS（反映请求源，支持凭证的请求）
- `--https=PATH` - 到HTTPS配置模块的路径
- `--proxy=ROUTE:URL` - 代理ROUTE到URL的所有请求
- `--help | -h` - 显示简洁的使用提示并退出
- `--version | -v` - 显示版本并退出

### 7.3.3.测试

我们进入leyou-portal目录，输入命令：

```
live-server --port=9002
```

 ![1526462494331](assets/1526462494331.png)



## 7.4.域名访问

现在我们访问只能通过：http://127.0.0.1:9002

我们希望用域名访问：http://www.leyou.com

第一步，修改hosts文件，添加一行配置：

```
127.0.0.1 www.leyou.com
```

第二步，修改nginx配置，将www.leyou.com反向代理到127.0.0.1:9002

```nginx
server {
    listen       80;
    server_name  www.leyou.com;

    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    location / {
        proxy_pass http://127.0.0.1:9002;
        proxy_connect_timeout 600;
        proxy_read_timeout 600;
    }
}
```

重新加载nginx配置：`nginx.exe -s reload`

![1526462774092](assets/1526462774092.png)





## 7.5.common.js

为了方便后续的开发，我们在前台系统中定义了一些工具，放在了common.js中：

 ![1526643361038](assets/1526643361038.png)



部分代码截图：

 ![1526643526973](assets/1526643526973.png)

- 首先对axios进行了一些全局配置，请求超时时间，请求的基础路径，是否允许跨域操作cookie等
- 定义了对象 ly ，也叫leyou，包含了下面的属性：
  - getUrlParam(key)：获取url路径中的参数
  - http：axios对象的别名。以后发起ajax请求，可以用ly.http.get()
  - store：localstorage便捷操作，后面用到再详细说明
  - formatPrice：格式化价格，如果传入的是字符串，则扩大100被并转为数字，如果传入是数字，则缩小100倍并转为字符串
  - formatDate(val, pattern)：对日期对象val按照指定的pattern模板进行格式化
  - stringify：将对象转为参数字符串
  - parse：将参数字符串变为js对象



