# Redis（缓存技术）

## 概述

~~~
Redis是用C语言开发的一个开源的高性能键值对（key-value）数据库 
mysql oracle db2  ---关系型数据库 数据都在硬盘上
redis  ---非关系型数据库  数据都在内存中
~~~



为什么要使用redis?

~~~
场景：
在企业级开发，需要完成导航条的数据，但是导航条的数据一般都是来源于数据库，只要每个人过来看到导航条的数据，都意味着连接了一次数据库，如果有1w人，需要连1w次数据库，在这里，我们发现所有人连接数据库拿到的数据都是一样的，也就意味着这个导航条的数据不是时时更新的数据，既然所有人看到的都一样，那有没有什么更好的方式去解决频繁操作数据库带来效率低的问题
方案：
让第一个访问的人去mysql数据库先获取一份，然后放入内存中，剩下所有的人再去内存中获取即可
解决技术：使用nosql数据库（redis）解决
好处：解决用户频繁访问数据库带来效率低的问题
~~~

面试题：非关系数据库和关系型数据库的区别？

~~~
关系型数据库：mysql  oracle  db2 
特点：数据都在硬盘上，有着表的概念，表数据之间有复杂的关系（1vsN N对N）
优点：数据的安全性和完整性高
缺点：高并发情况下查询效率极慢

非关系型数据库：redis 
特点：数据都在内存中 没有表的概念  只有键值对的概念 数据没有复杂的关系   key=String  value=5种
优点：查询效率高
缺点：数据的安全性和完整性没有保证 只要服务一关闭 数据全部丢失

redis:解决用户频繁访问数据库带来效率低的问题
if(redis){
   从redis中取数据
}else{
  从mysql中取数据
  放入redis中
}

企业开发：关系型数据库和非关系型数据库是相辅相成的
对于时时刻刻发生改变的数据 我们可以存入关系型数据库
对于时时刻刻不发生改变的数据 我们可以存入非关系型数据库
~~~

redis的应用场景？

~~~~
1  新闻内容
2  聊天室好友  
3  12306
~~~~

## 安装

~~~
官网下载地址：http://redis.io/download
window版：redis-2.8.9.zip解压即可
~~~

## redis的数据类型

redis数据支持的值类型: key键都是字符串   value值是5种 

~~~
1 字符串类型(掌握)   string     String=String   s1="hello"
2 散列类型(了解)     map        String=map      s2=username jack  sex nv ...
3 列表类型(了解)     list       String=list     s3=a b c d  a a a
4 集合类型(了解)     set	      String=set      s4=c a b d     
5 有序集合类型(了解) zset	    String=zset     s5=a b c d
~~~

## 字符串类型(掌握)

~~~
设置
	set key value   		
获取
	get key         		
删除
	del key        			
~~~

## 散列类型(了解)

把值看成map集合(适合存储对象的属性值)  例如: user1  username jack   

举个栗子: User  username=jack  sex=男   age=18

~~~
设置单个
hset  key  subkey subvalue :设置一个键值对   
获取单个
hget  key  subkey:获取一个子键的值

设置多个
hmset key subkey1 subvalue1  subkey2 subvalue2 ...:设置多个键值对	
获取多个
hmget key subkey1 subkey2...:获取多个子键的值

获取所有属性以属性值
hgetall key:获取指定key值的所有信息
		   

删除-子键
hdel key subkey1 subkey2 ...										
删除全部
del key
~~~

## redis视图化工具的安装

~~~
资料里面安装包--一路下一步
~~~

## 列表类型(了解) 

~~~~
两端的设置: 
lpush key member1 member2.. : 往左边开始插入
      lpush l1 a b c d e f
            list=f,e,d,c,b,a
      
rpush key member1 member2.. :往右边开始插入
	rpush l2 a b c d e f
	       list=a,b,c,d,e,f

 查看：
lrange key startindex endindex：查看  例如：lrange key 0 -1 :全查
   
两端的删除:    
lpop key :左边弹出一个	
rpop key :右边弹出一个

~~~~

## 集合类型(无序且唯一set)(了解)   

~~~
sadd key member1 member2.... :设置   
smembers key :查看
srem key member1 member2 :删除

~~~

## 有序集合类型（有序且唯一的set）(了解)

条件：必须要有一个数值来与之关联,因为要通过这个数值来排序

~~~
zadd key score1 value1 score2 value2...: 设置添加
zcard key :展示元素的长度
zscore key value :获取成员的数字
zrem key value1 value2... :删除指定成员
~~~

## redis通用的操作（掌握）

~~~
1 keys *:查询所有的key
2 exists key:判断是否有指定的key 若有返回1,否则返回0
3 rename key 新key:重命名
4 type key:判断一个key的类型
5 del key :删除


多数据库操作（扩展）
	select index:切换库
	move key 指定数据库: 将当前库的数据移动到指定库中
	dbsize: 返回当前库中有多少个key
	flushdb:清空当前数据库数据
	flushall:清空当前实例下所有的数据库数据
~~~



## redis的特有的：持久化功能(了解)

持久化: 就是redis会将数据从内存保存到硬盘

~~~
1 rdb(快照方式) 
默认开启的  
	redis.windows.conf    查save
保存策略:
		after 900 sec (15 min) if at least 1 key changed      
		after 300 sec (5 min) if at least 10 keys changed     
		after 60 sec if at least 10000 keys changed           
~~~

~~~
2 aof(配置文件命令方式)
默认不开启

若要开启,必须修改配置文件redis.windows.conf  搜appendonly
	appendonly yes

保存策略:
	# appendfsync always  每次都写入
	# appendfsync everysec  每秒写入
	 appendfsync no 不写入
~~~

## java代码操作redis数据库数据(核心重点)

java--jdbc---mysql

java--jedis--redis

使用步骤:

	1.导入jar包(maven坐标)
	2.创建jedis对象
			new Jedis(ip,端口)
	3.通过jedis操作redis数据库 (字符串)
	4.释放资源

使用连接池优化:

		// 创建数据源的配置对象
		JedisPoolConfig config = new JedisPoolConfig();
			// 配置初始化连接
			config.setMaxTotal(1000);
			// 配置空闲时期的最大连接
			config.setMaxIdle(5);
			.......
	
		// 创建一个连接池
		JedisPool pool = new JedisPool(config, "192.168.2.132",6379);
		// 获取jedis对象
		Jedis jedis = pool.getResource();

JedisUtils工具类的封装

~~~java
package cn.itcast.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisUtils {

        private static JedisPool jedisPool =null;
    static {
        //创建jedis连接池的配置对象
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 设置连接池初始化的时候有多少个连接
        jedisPoolConfig.setMaxTotal(40);
        // 设置空闲时期释放连接的时候最少要保留到少个在连接池中
        jedisPoolConfig.setMaxIdle(10);
        // 创建jedis的连接池
        jedisPool = new JedisPool(jedisPoolConfig, "localhost", 6379);
    }

    //获取jedis
    public static Jedis getJedis() {
        // 从连接池中获取jedis
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }
}

~~~

## jedis优化短信验证码

![redis优化验证码](img\redis优化验证码.png)

UserServlet

~~~java
// 发短信
    private void sendSms(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // 1 获取用户填写的手机号
        String telephone = request.getParameter("telephone");
        // 2 调用service实现发短信的功能
        UserService userService = new UserService();
        // 发送失败（false） 发送成功（随机数）
        String value = userService.sendSms(telephone);
        // 判断
        if("false".equals(value)){
            // 告诉ajax发送失败
            response.getWriter().print(-1);
            return;
        }
        // 成功了要保存数据到session中
        //request.getSession().setAttribute("telephoneCode",value);
        // 将手机验证码放入redis中只保留1分钟
        Jedis jedis = JedisUtils.getJedis();
        jedis.setex("telephoneCode",60,value);
        jedis.close();
    }
~~~

~~~java
// 发短信
    private void sendSms(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        // 1 获取用户填写的手机号
        String telephone = request.getParameter("telephone");
        // 2 调用service实现发短信的功能
        UserService userService = new UserService();
        // 发送失败（false） 发送成功（随机数）
        String value = userService.sendSms(telephone);
        // 判断
        if("false".equals(value)){
            // 告诉ajax发送失败
            response.getWriter().print(-1);
            return;
        }
        // 成功了要保存数据到session中
        //request.getSession().setAttribute("telephoneCode",value);
        // 将手机验证码放入redis中只保留1分钟
        Jedis jedis = JedisUtils.getJedis();
        jedis.setex("telephoneCode",60,value);
        jedis.close();
    }
~~~

## jedis优化分类导航条

![分类导航条分析](img\分类导航条分析.png)

1）header.jsp

~~~jsp
<%--分类导航条数据的获取--%>
<script>
    $(function(){
       // 页面已加载就和服务器进行ajax异步交互获取数据
        var url="${pageContext.request.contextPath}/category";
        var data="action=findCategory";
        $.post(url,data,function(d){
            //遍历循环
            for(var i=0;i<d.length;i++){
                //将数据放入导航条
                $("#categoryUI").append("<li><a href=#>"+d[i].cname+"</a></li>");
            }
        },"json")
    })
</script>
~~~

2) CategoryServlet

~~~java
// 分类导航条数据的查询
    private void findCategory(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 调用service(面向接口编程--多态--先有接口后有实现类)
        CategoryService categoryService=new CategoryServiceImpl();
        //从mysql中获取数据
        //String json=categoryService.findCategory();
        //从redis中获取
        String json=categoryService.findCategoryToRedis();
        System.out.println(json);
        // 把转换的数据给ajax
        response.getWriter().print(json);
    }
~~~

3） CategoryService

~~~java
public interface CategoryService {
    // 查询分类导航条
    String findCategory() throws JsonProcessingException;
	// 从redis获取数据
    String findCategoryToRedis();
}
~~~

4) CategoryServiceImpl

~~~java
public class CategoryServiceImpl implements CategoryService {
    // 从mysql中查询分类导航条
    public String findCategory() throws JsonProcessingException {
        SqlSession sqlSession = MyBatisUtils.openSession();
        CategoryDao categoryDao = sqlSession.getMapper(CategoryDao.class);
        //调用方法
        List<Category> list=categoryDao.findCategory();
        // 将复杂数据list返回给ajax(ajax不接受复杂数据--list 对象 map 数组..)
        // 复杂数据list-->工具jackson--->json数据--->ajax
        // 工具jackson的使用
        // 导入坐标
        // 创建对象调用api ObjectMapper.writeValueAsString()
        ObjectMapper objectMapper = new ObjectMapper();
        // 转换方法--json格式的字符串 {} []  [{},{}] {key1:[]}
        String json = objectMapper.writeValueAsString(list);
        return json;
    }

    @Override  //从redis中获取数据
    public String findCategoryToRedis() {

            Jedis jedis = JedisUtils.getJedis();
        try {

            // 直接从redis中拿数据
            // 没拿到 从mysql拿一份放入redis中
            // 拿到了 直接返回
            String value = jedis.get("dht");
            // 判断
            if(value==null){
                // 从mysql中拿一份放入redis中
                value=findCategory();
                jedis.set("dht",value);
                System.out.println("第一次从数据库mysql中获取的 放入了redis中");
                return value;
            }
            System.out.println("从redis中获取返回的");
            return value;

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }


        return null;
    }
}

~~~

5）CategoryDao

~~~java
public interface CategoryDao {
    //查询分类导航条
    List<Category> findCategory();
}
~~~

6) CategoryDao.xml

~~~xml
<mapper namespace="cn.itcast.dao.CategoryDao">
    <!--查询分类导航条-->
    <select id="findCategory" resultType="category">
        select * from tab_category
    </select>
</mapper>
~~~


