# JDBC

## 概述

JDBC: Java DataBase Connectity ---- java数据库的连接

是一种专门用于执行SQL语句的Java API，可以为多种关系数据库提供统一的访问，它由一组用Java语言编写的接口组成

java代码要想操作各种数据库就得使用JDBC

![jdbc的引入](img\jdbc的引入.png)

## JDBC的由来

JDBC规范定义接口，具体的实现由各大数据库厂商来实现 ，JDBC是Java访问数据库的标准规范。真正怎么操作
数据库还需要具体的实现类，也就是数据库驱动（第三方JAR包）。每个数据库厂商根据自家数据库方式编写好自己数据库的驱动。所以我们只需要会调用JDBC接口中的方法即可。数据库驱动由数据库厂商提供。

我们在用java代码操作数据库时只需要会使用JDBC接口中的方法即可，使用简单

![jdbc的概述](img\jdbc的概述.png)



## 入门案例-使用java代码对数据库进行数据插入

前提：操作哪个数据库就需要导入哪个数据库的驱动包

jdbc的使用步骤：

1.加载驱动

2.获取连接

3.获取语句的执行者

4.编写sql

5.执行sql语句

6.释放资源

~~~java
@Test
    public void test1() throws SQLException {
        // 使用java代码调用jdbc提供的api操作数据库的数据
        // 前提：操作那个数据库就需要导入哪个数据库的驱动

        // 1 加载驱动
        DriverManager.registerDriver(new Driver());
        // 2 获取连接
        Connection connection = 	DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root");
        // 3 获取操作sql的执行对象
        Statement statement = connection.createStatement();
        // 4 编写sql
        String sql="insert into user values(null,'中文',123456)";
        // 5 执行语句  i:影响的行数
        int i = statement.executeUpdate(sql);
        System.out.println("共"+i+"行受到影响");
        // 6 释放资源
        statement.close();
        connection.close();	

    }
~~~



## JDBC API详解

### DriverManager:类

~~~
1 加载驱动 用反射代替了
2 获取数据库连接的对象	
~~~



方法作用：

1 加载驱动 

 static void registerDriver(Driver driver)  实际开发中不会选择  因为加载了2次驱动

​		 Class.forName(Driver的全限定名) 反射方式 加载一次驱动  适用开发

2 获取数据库连接的对象	

static Connection getConnection(String url, String user, String password);

Url:数据库的地址   固定格式：jdbc:要连接的是那个数据库://数据库地址:端口号/哪个数据库

user：数据库的用户名

Password：数据库的密码

如果数据出现乱码需要加上参数: ?characterEncoding=utf8，表示让数据库以UTF-8编码来处理数据。 如:

jdbc:要连的数据库://数据库地址:端口号/哪个数据库?characterEncoding=utf8		

### Connection:接口

~~~
1 可以获取执行SQL语句的对象（语句执行者)
2 可以做事务
~~~

方法作用：

1 可以获取执行SQL语句的对象（语句执行者)

Statement createStatement();获取语句的执行者   

2 操作事务	

void setAutoCommit(boolean autoCommit) ;    --start transaction;
false：开启事务， ture：关闭事务（默认）

void commit();   ---commit;
提交事务

void rollback();  ---rollback;
回滚事务

### Statement:接口

~~~
执行sql语句
~~~



方法作用：

1 执行sql语句

 executeUpdate()   执行的是增删改语句

返回值是int类型 代表受影响的行数

executeQuery()  执行的是查询语句

返回的值是ResultSet类型对象 称为：结果集对象	



### Resultset:接口

 方法作用：

1 封装查询语句的结果集

next() 可以让指针向下移动一行 默认在第一行之前  返回值是boolean  true:代表有数据  false: 代表没数据

2 获取结果集中每一行的数据

 get类型(int 列的位置)

 get类型(String 列名)

![结果集的概述](img\结果集的概述.png)

~~~
@Test  //查询实现
public void test1() throws Exception {

            Class.forName("com.mysql.jdbc.Driver");

            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/bookdb", "root", "root");

            Statement statement = connection.createStatement();

            String sql="select * from user";

            // resultSet: 结果集对象 （代表的就是查出来的表）
            ResultSet resultSet = statement.executeQuery(sql);

            while(resultSet.next()){
                // 获取数据打印
                int id = resultSet.getInt("id");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");

                System.out.println(id+":"+username+":"+password);
            }

            resultSet.close();
            statement.close();
            connection.close();

        }
~~~

## 编写JDBC的工具类实现数据的增删改查

工具类：可以将一些重复的代码进行抽取封装，以便于复用，简化代码开发

通过上面代码的编写会发现每次去执行SQL语句都需要注册驱动，获取连接，以及释放资源。
发现很多重复的劳动，我们可以将重复的代码抽取出来定义一个工具类，直接调用方法，可以简化代码。

那么我们接下来定义一个JDBCUtils 类。把注册驱动，获取连接，以及释放资源的代码放到这个类的方法
中。以后直接调用该工具类的方法即可。

## JDBC操作事务

~~~~java
Connection:	
	void setAutoCommit(boolean autoCommit) ;
	false：开启事务 ture：关闭事务（默认的）
	void commit();
	提交事务
	void rollback();
	回滚事务

ps:在使用手动事务时,必须要抓取异常,在catch块中进行回滚事务.
   只要有事务，就必须try,catch捕捉处理  查询有没有事务都行
~~~~



## 模拟用户登录功能

```java
需求:
	输入用户名和密码后,获取输入的值,使用jdbc操作数据库,完成登录功能.
    找到了：登录成功   找不到：用户名或密码错误  
技术分析:
	jdbc
创建用户表:
    create table user (
      id int primary key auto_increment,
      name varchar(20),
      password varchar(20)
  );
  insert into user values(null,"tom","tom"),(null,"rose","rose"),(null,"jerry","jerry");

```

Statement编译对象：拼接什么样的sql就执行什么样的sql语句

问题：使用Statement对象来操作sql语句会有缺陷：会造成数据的安全隐患

比如登录中的sql拼接：select * from user where username='jack' #' and password='123';

相当于注释掉了密码的校验，这种安全隐患我们称为：Sql注入

Sql注入:将用户输入的内容作为了sql语句的一部分 改变了原有sql语句的含义

解决：就不能使用当前的Statement对象来操作SQL语句了，得使用另一个对象--preparedStatement对象

## PreparedStatement: 预编译对象

作用：也是用来执行sql语句的 

语句执行者：Statement编译对象     preparedStatement预编译对象

区别：

Statement编译对象是有什么样的sql就执行什么样的sql语句，每次执行任何一条sql语句都得让数据库去编译执行   如果执行一万条同样的查询语句 数据库要编译一万次 效率低

preparedStatement预编译对象是先将sql格式传递给数据库做预编译 其后拿着预编译结果传递参数执行sql，执行一万条同样的查询语句 数据库只编译一次 根据不同的参数做不用的执行

预编译的好处：

1 只需要编译一次，效率高

2  能让数据库提前知晓要执行的sql语句格式，只负责给数据库传参即可

语法格式：

Statement编译对象： select * from 表 where 字段1=值1 and 字段2=值2;

preparedStatement：select * from 表 where   name=？ and  password=？;

	?:占位符 所有的实际参数都用占位符替换了 不在是直接设值了
	通过外部方法来设置实际参数的值：set字段类型(占位符的序号，要设置的值)；
	占位符的序号是从1开始的 
	setString(1,"jack");
	setString(2,"1234");

## preparedStatement对象操作数据库的增删改查

### 	使用步骤:

1.编写sql
   	  String sql = "insert into user values(null,?,?...) ";
2.创建预编译对象--提前编译sql语句
​	  PreparedStatement pst = conn.preparedStatement(sql);
3.设置具体的参数
​	  pst.set字段类型(int a,值);
​	  a: 第几个 ? (占位符)  默认从左向右第1个开始
4.执行sql即可	
​	ResultSet rs = pst.executeQuery(); // 执行查询,返回 resultSet
​	int i = pst.executeUpdate(); // 执行 增 删 改 返回的是影响的行数



# 连接池（数据源）

连接池：存放数据库连接的容器（集合）

## 连接池出现的目的

~~~
之前在使用jdbc操作数据库数据的时候，有一个步骤是获取连接（创建连接）
连接用完，还需要释放连接（销毁连接），这2个步骤太消耗资源了
创建连接=0.1
销毁连接=0.1
10000000*0.2=2000000
连接池就可以帮助我们解决创建连接和销毁连接带来的资源消耗问题
连接池就是用来优化jdbc的2个步骤的
		   1 优化的是jdbc的创建连接部分
		   1 优化的是jdbc的销毁连接部分
~~~

## 连接池优化原理

~~~
在连接池一初始话的时候，就在容器中创建一定量的连接
当用户要连数据库的时候，就从容器中拿一个连接使用
使用完毕之后，不再是销毁，而是把使用后的连接还放回容器 供下一个用户去循环使用
~~~

在企业中使用的都是已经成熟并且性能很高的提供好的连接池

常见的连接池：

![01](img\01.bmp)

![连接池](img\连接池.png)

## druid连接池的使用

导入druid连接池的jar包

~~~java
 @Test
    public void test2() throws Exception {
        // 创建一个配置文件对象
        Properties properties = new Properties();
        // 获取文件流  DruidTest.class.getResourceAsStream获取的是classess文件的路径  返回流
        InputStream is = DruidTest.class.getResourceAsStream("/druid.properties");
        // 配置文件对象加载配置文件
        properties.load(is);
        // 创建druid的连接
        DataSource ds = DruidDataSourceFactory.createDataSource(properties);
        // 获取连接
        Connection connection = ds.getConnection();

        String sql="select * from user";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()){
            int id = resultSet.getInt("id");
            String name = resultSet.getString("name");
            String password = resultSet.getString("password");
            System.out.println(id+":"+name+":"+password);
        }

        // 归还连接
        JDBCUtils.close(resultSet,preparedStatement,connection);
    }
~~~





## jdbc+工具类单独操作的最终版（配置文件）

配置文件：jdbc.properties

~~~properties
driver=com.mysql.jdbc.Driver
url=jdbc:mysql://localhost:3306/db88
username=root
password=root
~~~

工具类：自己创建连接 自己加载配置文件 

~~~java
public class JDBCUtils {

    private static String driver;
    private static String url;
    private static String username;
    private static String password;

    static {

        // 加载配置
        ResourceBundle bundle = ResourceBundle.getBundle("jdbc");
        driver = bundle.getString("driver");
        url = bundle.getString("url");
        username = bundle.getString("username");
        password = bundle.getString("password");
        // 1 加载驱动
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 获取连接的方法
    public static Connection getConnection() throws Exception {
        // 2 获取连接
        Connection connection = DriverManager.getConnection(url, username, password);
        return connection;
    }



    // 释放资源的方法
    public static void closeZY(ResultSet rs, Statement statement, Connection connection){
        try {

            if(rs!=null){
                rs.close();
            }

            if(statement!=null){
                statement.close();
            }

            if(connection!=null){
                connection.close();
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }
~~~

jdbc  crud一套

~~~java
@Test  //增
    public void test1() throws Exception {
        Connection connection = JDBCUtils.getConnection();
        String sql="insert into user values(null,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,"rose");
        preparedStatement.setString(2,"6666");
        int i = preparedStatement.executeUpdate();
        System.out.println(i);
        JDBCUtils.closeZY(null,preparedStatement,connection);
    }

    @Test  //删
    public void test2() throws Exception {

        Connection connection = JDBCUtils.getConnection();
        String sql="delete from user where id=?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1,6);
        int i = preparedStatement.executeUpdate();
        System.out.println(i);
        JDBCUtils.closeZY(null,preparedStatement,connection);

    }

    @Test
    public void test3() throws Exception {
        Connection connection = JDBCUtils.getConnection();
        String sql="update user set name=? where id=?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1,"rose");
        preparedStatement.setInt(2,1);
        int i = preparedStatement.executeUpdate();
        System.out.println(i);
        JDBCUtils.closeZY(null,preparedStatement,connection);
    }

    @Test
    public void test4() throws Exception {
        Connection connection = JDBCUtils.getConnection();
        String sql="select * from user";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet rs = preparedStatement.executeQuery();
        while(rs.next()){
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String password = rs.getString("password");
            System.out.println(id+"::"+name+"::"+password);
        }

        JDBCUtils.closeZY(null,preparedStatement,connection);
    }
~~~



## jdbc+druid+工具类操作的最终版（配置文件）

配置文件：druid.properties

~~~properties
driverClassName=com.mysql.jdbc.Driver
url=jdbc:mysql://localhost:3306/db88
username=root
password=root

initialSize=10
maxActive=20
minIdle=2
maxWait=2000
~~~

工具类：使用druid的方法加载的配置文件 连接从druid连接池获取的

~~~java
// jdbc+druid的工具类
public class JDBCUtils {

    private static DataSource ds;

    static {

        try {

            Properties properties=new Properties();
            // 只要获取src下的资源文件流  该方法是固定方法
            InputStream is = JDBCUtils.class.getResourceAsStream("/druid.properties");
            properties.load(is); //流
            ds = DruidDataSourceFactory.createDataSource(properties);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 获取连接的方法
    public static Connection getConnection() throws Exception {
        Connection connection = ds.getConnection();
        return connection;
    }



    // 释放资源的方法
    public static void closeZY(ResultSet rs, Statement statement, Connection connection){
        try {

            if(rs!=null){
                rs.close();
            }

            if(statement!=null){
                statement.close();
            }

            if(connection!=null){
                connection.close();
            }

        }catch (Exception e){
            e.printStackTrace();
        }

    }
~~~

jdbc+druid crud一套

~~~java
@Test
    public void test1() throws Exception {
        // 从连接池中获取连接
        Connection connection = JDBCUtils.getConnection();


        String sql="select * from user";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet rs = preparedStatement.executeQuery();
        while(rs.next()){
            int id = rs.getInt("id");
            String name = rs.getString("name");
            String password = rs.getString("password");
            System.out.println(id+"::"+name+"::"+password);
        }


        JDBCUtils.closeZY(null,preparedStatement,connection);
    }


