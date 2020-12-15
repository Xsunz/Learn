##### 什么是框架？

~~~
把重复的代码工作抽取出来，让程序员把精力专注在核心的业务代码实现上。
框架可以理解为半成品软件，框架做好以后，接下来在它基础上进行开发。
~~~

##### 为什么使用框架？

~~~
因为学了它之后，会简化开发代码，缩短开发周期，节省开发成本
企业开发中都在用，不会它，你就无法正常进入企业进行开发工作。
~~~

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/%E6%A1%86%E6%9E%B6%E6%A6%82%E8%BF%B0.png)

## 一 框架简介

软件开发常用的架构是三层架构，之所以流行是因为有着清晰的任务划分。一般包括以下三层：

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/mybatis%E6%A1%86%E6%9E%B6%E7%9A%84%E4%BD%9C%E7%94%A8.png)

Mybatis框架:对JDBC的封装，在dao层和数据库交互



## 二 Mybatis简介

**ORM（Object Relational Mapping）对象关系映射**

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/orm.png)

****

一句话说：就是ORM可以让我们以面向对象的形式操作数据库，mybatis是一款优秀的持久层（ORM）框架



## 三 Mybatis快速入门

**需求**
	查询数据库user表的所有记录，封装到User对象中。

**步骤分析**

```markdown
1. 导入数据库和表数据（今天的资料中）

2. 创建java工程，导入jar包（MySQL驱动、mybatis、log4j日志）
	
3. 编写User实体类

4. 编写xml映射文件（ORM思想）

5. 编写SqlMapConfig.xml核心配置文件
	数据库环境配置
	映射关系配置

6. 编写测试代码	
	// 1.加载核心配置文件
	// 2.获取sqlSessionFactory工厂对象
	// 3.获取sqlSession会话对象
	// 4.执行sql
	// 5.释放资源
```

1. 导入数据库和表数据

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571621056627.png)



2. 创建java工程，导入jar包（MySQL驱动、mybatis、log4j日志、junit）

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571621231606.png)



3. 编写User实体类

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571621331280.png)



4. 编写xml映射文件

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571621606910.png)

5. 编写SqlMapConfig.xml核心配置文件

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571621974532.png)

6. 编写测试代码

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571622462298.png)


## 四 Mybatis映射文件概述

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571623550945.png)





## 五 Mybatis增删改

**新增**

```markdown
1. 编写UserMapper.xml映射文件

2. 编写测试代码

* 注意：
	1.新增使用 insert 标签
	2.接收的参数类型通过 parameterType属性设置
	3. ？占位符 使用 #{属性名} 代替
	4. 使用API：insert("命名空间.id",保存实体对象);
	5. 执行dml（增删改）类型，需要手动提交事务
```

**修改**

```markdown
1. 编写UserMapper.xml映射文件

2. 编写测试代码

* 注意：
	1.修改使用 update 标签
	2.使用API： update("命名空间.id",修改实体对象);
	3.执行dml类型，需要手动提交事务
```

**删除**

```markdown
1. 编写UserMapper.xml映射文件

2. 编写测试代码

* 注意：
	1.删除使用 delete 标签
	2.#{任意名称} 接收的是简单数据类型，内容可以随便写
	3.使用API：delete("命名空间.id",删除主键);
	4.执行dml类型，需要手动提交事务
```

**知识小结：**

```markdown
#{} 接收简单数据类型内容随便写
				int double string #{随便写}
#{} 接收引用数据类型内容只能是属性名
				 对象  #{属性名}
```





## 六 抽取工具类

**将测试代码中的公共部分进行抽取，封装到 MybatisUtils中**

```java
/**
 * mybatis工具类
 */
public class MybatisUtils {

    private static SqlSessionFactory sqlSessionFactory = null;

    static {
        try {
            // 加载配置文件
            InputStream in = Resources.getResourceAsStream("SqlMapConfig.xml");
            // 获取sqlSessionFactory会话工厂对象
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(in);
        } catch (IOException e) {
            throw new RuntimeException("初始化mybatis失败...");
        }
    }

    // 提供获取sqlSession会话对象方法
    public static SqlSession openSession() {
        return sqlSessionFactory.openSession();
    }
}
```



## 七 Mybatis核心文件概述

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571626720833.png)



**1）environments标签**

```markdown
* 数据库环境配置
```

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571626854457.png)


~~~
1. 其中，事务管理器（transactionManager）类型有两种：
* JDBC：
这个配置就是直接使用了JDBC 的提交和回滚设置，由mybatis自己手动控制事务
* MANAGED：
这个配置几乎没做什么。它从来不提交或回滚一个连接，而是让容器来管理事务的整个生命周期。
例如：mybatis与spring整合后，事务交给spring容器管理。

2. 其中，数据源（dataSource）常用类型有二种：
* UNPOOLED：
这个数据源的实现只是每次被请求时打开和关闭连接。
* POOLED：
这种数据源的实现利用“池”的概念将 JDBC 连接对象组织起来。使用的是mybatis自带的
~~~

 

**2）properties标签**

```markdown
* 第三方属性配置
	 实际开发中，习惯将数据源的配置信息单独抽取成一个properties文件
	 <properties resource="jdbc.properties"></properties>	
	 
	 <environments default="mysql">
        <!--mysql数据库环境-->
        <environment id="mysql">
            <!-- 使用JDBC类型事务管理器 -->
            <transactionManager type="JDBC"></transactionManager>
            <!-- 数据源（连接池）配置  POOLED-->
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"></property>
                <property name="url" value="${jdbc.url}"></property>
                <property name="username" value="${jdbc.username}"></property>
                <property name="password" value="${jdbc.password}"></property>
            </dataSource>
        </environment>
    </environments>
```



**3）typeAliases标签**

```markdown
* 实体类型别名配置
	<typeAliases>
        <!--指定一个实体类和对应的别名-->
        <!--<typeAlias type="com.itheima.domain.User" alias="user"></typeAlias>-->


        <!--指定当前包下所有的实体设置别名  默认： 别名（类名） -->
        <package name="com.itheima.domain"></package>

    </typeAliases>
```

**4）mappers标签**

```markdown
* 映射关系配置（加载映射配置文件）
	1.加载指定的src目录下的映射文件【今天快速入门】
	<mapper resource="org/mybatis/builder/AuthorMapper.xml"/>
```



**核心配置文件标签顺序**

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571628218147.png)





## 八 Mybatis的API概述

**1）Resources**

```markdown
* 专门用于加载mybatis核心配置文件，返回的 io流
InputStream in = Resources.getResourceAsStream("SqlMapConfig.xml");
```



**2）SqlSessionFactoryBuilder**

```markdown
* 专门用于生产SqlSessionFactory工厂对象
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(in);
```



**3）SqlSessionFactory**

```markdown
* 一个项目只有一个工厂对象，生产sqlSession对象

// 需要手动提交事务，DML语句才会持久化到数据库中
	SqlSession openSession();【推荐】
	
// 设置是否开启自动提交，如果设置为true，开启自动提交事务
	SqlSession openSession(boolean autoCommit);
	参数说明
		true：每一条DML类型语句都会自动提交事务
		false（默认值）：需要手动提交事务
```



**4）SqlSession**

```markdown
* 是mybatis核心对象，可以对数据库进行CRUD基本操作

* 方法
	<T> T selectOne(String statement, Object parameter);
    <E> List<E> selectList(String statement, Object parameter);
    int insert(String statement, Object parameter);
    int update(String statement, Object parameter);
    int delete(String statement, Object parameter);
    
* 事务
	void commit();
	void rollback();
```



## 九 Mybatis实现Dao层 【今日重点】

**接口开发规范**

```markdown
1. 接口全限定名与映射文件的namespace属性一致

2. 接口的方法名与statement（子标签）的id属性一致

3. 接口方法参数类型与statement（子标签）的parameterType属性一致

4. 接口方法返回值类型与statement（子标签）的resultType属性一致
```

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/接口开发.bmp)





**步骤分析**

```markdown
1. 创建UserMapper接口

2. 编写UserMapper.xml映射文件

3. 编写测试代码
```

**UserMapper接口**

```java
public interface UserMapper {

    public List<User> findAll();
}

```

**UserMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.itheima.mapper.UserMapper">

    <select id="findAll" resultType="user">
        select * from user
    </select>

</mapper>
```

### 10.1 ResutlMap属性

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/细节1.png)

```markdown
* resultType
		如果实体的属性名与表中字段名一致，将查询结果自动封装到实体类中
		
* ResutlMap
		如果实体的属性名与表中字段名不一致，可以使用ResutlMap实现手动封装到实体类中
```

```xml
<!--
        实现手动映射封装
            resultMap
                id="userResultMap" 此标签唯一标识
                type="user" 封装后的实体类型
           <id column="uid" property="id"></id>  表中主键字段封装
                column="uid"  表中的字段名
                property="id" user实体的属性名
           <result column="NAME" property="username"></result>  表中普通字段封装
                column="NAME"  表中的字段名
                property="username" user实体的属性名

                补充：如果有查询结果有 字段与属性是对应的，可以省略手动封装 【了解】
    -->
    <resultMap id="userResultMap" type="user">
        <id column="uid" property="id"></id>
        <result column="NAME" property="username"></result>
        <result column="bir" property="birthday"></result>
        <result column="gender" property="sex"></result>
        <result column="address" property="address"></result>
    </resultMap>

    <select id="findAllResultMap" resultMap="userResultMap">
       SELECT id AS uid,username AS NAME,birthday AS bir,sex AS gender ,address FROM USER
    </select>
```



### 10.2 多条件查询（三种）

**需求**

​	根据id和username查询user表

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/细节2.png)

```java
// 多条件查询
// 版本一  可以将多个参数抽取的一个 pojo对象
public List<User> findByIdAndUsername3(User user);
// 版本二
public List<User> findByIdAndUsername1(Integer id,String username);
// 版本三
public List<User> findByIdAndUsername2(@Param("id") Integer id,@Param("username") String username);
```

```xml
<!--
        版本一【推荐】 封装到pojo对象中
    -->

<select id="findByIdAndUsername3" parameterType="user" resultType="user">
    select * from user where id = #{id} and username = #{username}
</select>


<!--
        多条件查询 版本二 【了解】 可读性差
        如果查询条件有多个参数  parameterType 可以省略
    -->
<select id="findByIdAndUsername1" resultType="user">
    <!--  select * from user where id = #{arg0} and username = #{arg1} -->
    select * from user where id = #{param1} and username = #{param2}
</select>


<!--
        版本三   参数过多不太方便
    -->
<select id="findByIdAndUsername2" resultType="user">
    select * from user where id = #{id} and username = #{username}
</select>


```

### 10.3 模糊查询

**需求**

​	根据username模糊查询user表

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/细节3.png)

```java
// 模糊查询
// 版本一
public List<User> findByUsername1(String username);
// 版本二
public List<User> findByUsername2(String username);
// 版本三
public List<User> findByUsername3(String username);
```



```xml
<!--模糊查询
        版本一【最简单】
            在java中出现了sql通配符，产生了耦合性问题，不太推荐使用
    -->
<select id="findByUsername1" parameterType="string" resultType="user">
    select * from user where username like #{username}
</select>

<!--
        版本二 【这种方式会出现sql注入问题，开发不能使用】
            ${} 字符串拼接 ,如果接收简单数据类型  名称只能是：${value}

    -->
<select id="findByUsername3" parameterType="string" resultType="user">
    select * from user where username like '%${value}%'
</select>

<!--
        版本三
            可以使用 concat() 函数 拼接
    -->
<select id="findByUsername4" parameterType="string" resultType="user">
    select * from user where username like concat('%',#{username},'%');
</select>
```



### 10.4 ${} 与 #{} 区别

```java
${} ：底层 Statement
	1.将sql与参数拼接在一起，会出现sql注入问题
	2.每次执行sql语句都会编译一次
	3.接收普通数据类型，命名：'${value}'
	4.接收引用数据类型，命名: '${属性名}'  ；注意：需要使用单引号‘${xxx}’

#{}：底层 PreparedStatement
	1.sql与参数分离，不会出现sql注入问题
	2.sql与只需要编译一次
	3.接收普通数据类型，命名：#{随便写}
	4.接收引用数据类型，命名：#{属性名} 
```





## 十一 Mybatis映射文件深入

**环境搭建**

```markdown
* 将 web16_mybatis03_dao_proxy 复制改名为 web16_mybatis05_mapper_config
```

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/1571647906502.png)



### 11.1 返回主键

**1）useGeneratedKeys**【简单，但不通用】

**2）selectKey**【复杂，但通用】

![Image](https://raw.githubusercontent.com/Xsunz/Learn/main/java/mybatis/mybatis.5/assets/细节4.png)

```java
// 返回主键
public void save1(User user);
public void save2(User user);
```



```xml
<!--
        返回主键 版本一 useGeneratedKeys
            useGeneratedKeys="true" 开启返回主键功能
            keyColumn="id"    user表的主键字段
            keyProperty="id"  返回到实体的指定属性中

            这种方式：仅支持主键自增类型的数据 MySQL 和 sqlServer  ，oracle不支持

    -->
<insert id="save1" parameterType="user" useGeneratedKeys="true" keyColumn="id" keyProperty="id">
    insert into user(username,birthday,sex,address) values(#{username},#{birthday},#{sex},#{address})
</insert>

<!--
        版本二
           <selectKey>
            keyColumn="id"  指定user表的主键字段名
            keyProperty="id" 指定user实体的主键属性名
            resultType="int" 指定user实体主键的类型
            order="AFTER"  本条sql语句是在insert执行之前（执行），还是之后（执行）
                after 执后执行
                before 之前执行
    -->
<insert id="save2" parameterType="user">
    <selectKey keyColumn="id" keyProperty="id" resultType="int" order="AFTER">
        SELECT LAST_INSERT_ID()
    </selectKey>
    insert into user(username,birthday,sex,address) values(#{username},#{birthday},#{sex},#{address})
</insert>
```
