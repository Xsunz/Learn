## maven

## 概述

~~~
简单说：Maven是一个项目管理工具，专门用来管理web项目
企业开发：一个开发好的成型web项目，最终都要交给maven管理
~~~

## maven如何去管理项目（作用）

~~~
1 maven会统一管理项目jar包
			
					
2 maven为项目提供了大量的命令功能  
			 maven提供了对项目生命周期管理的命令：：编译、测试、打包、部署、运行
			 提高开发效率
			 

3 maven对工程分模块构建，提高开发效率（项目一第一天高级课程讲）


以前：mybatis_demo01>lib>mysql
以后：mybatis_demo01>配置文件pom.xml>maven>本地仓库>mysql
		1 maven是如何管理操作仓库的？ 仓库
		2 项目是如何告知mybaits自己需要的jar包？ 坐标
~~~

## maven的仓库

1 本地仓库

~~~
工程第一次从本地仓库没找到jar包会自动联网从远程中央仓库（互联网）去下载jar 包，将jar包存在本地仓库
第二次不需要从远程中央仓库去下载,先从本地仓库找，如果找不到才会去远程仓库找。
PS:本地仓库
课程中：我给你们准备好了
工作中：企业给你准备好了
~~~

2 远程中央仓库

	就是远程仓库，仓库中jar由专业团队（maven团队）统一维护。
	中央仓库的地址：http://repo1.maven.org/maven2/

![仓库](img\仓库.png)



## maven的坐标

~~~
为了能够准确的找到仓库中的jar包，maven需要对这些jar包做唯一标识（文件夹名称）
这些唯一标识就叫做坐标，方便我们在pom文件中快速找到仓库的jar包
~~~

Pom.xml 坐标的定义元素如下：

	groupId：定义当前在Maven项目中的名称
	artifactId：定义项目模块
	version：定义当前项目的当前版本
	例如：要引入junit的jar包，
	只需要在pom.xml配置文件中定义这些坐标元素，就可以将仓库junit的jar包引入到自己的项目中


## maven的安装

	将maven包解压即安装成功
	目录：
	     bin  maven的二进制命令
	     boot maven加载第三方jar包的支撑
	     conf maven的配置文件(核心用settings.xml)
	     lib  maven运行时候需要的jar包
	
	注意事项：
	1 环境变量的配置：黑窗口
	     1需要配置环境变量MAVEN_HOME
				MAVEN_HOME=maven的解压包在硬盘的位置
	     2将MAVEN_HOME添加到path中
				%MAVEN_HOME%\bin;
	测试：mvn -version
	
	2 需要在配置文件中告诉maven本地仓库的位置 （一定得配置）
	      conf/settings.xml: 53行的位置
		 <localRepository>本地仓库在硬盘的地址</localRepository>	
## idea集成maven

~~~
file---settings--搜索maven:
	1 配置maven的安装路径
	2 配置maven配置文件路径
	3 配置本地仓库的路径
4--重要 配置所有资源都优先从本地仓库查找，没有再去网络
		maven
		---runner
		     VM Options: -DarchetypeCatalog=internal
~~~

![maven的配置1](img\maven的配置1.png)

![maven配置2](img\maven配置2.png)



## maven构建项目

~~~
maven管理普通项目（了解）

maven管理javaweb项目并发布访问（掌握）
		webapp: 放的都是页面 例如：html jsp css js img...
		src-main-蓝色的java:放的都是java代码
		src-main-文件色的resources:放的都是配置文件
~~~

![maven结构](img\maven结构.bmp)

## maven统一命令

~~~
编译、测试、打包（.war包）
~~~

## maven的依赖管理

~~~
导入坐标依赖（servlet 网站坐标介绍）坐标查询：http://mvnrepository.com/
scope标签依赖范围
~~~

![1](img\11.bmp)

## lombok插件

lombok 提供了简单的注解的形式来帮助我们简化消除一些必须有但显得很臃肿的 java 代码。

通过使用对应的注解，可以在编译源码的时候生成对应的方法，所以不会影响任何运行效率。

常用的 lombok 注解：

~~~java
* @NoArgsConstructor：注解在类上；为类提供一个无参的构造方法
* @AllArgsConstructor：注解在类上；为类提供一个全参的构造方法
* @Data：注解在类上；生成toString，getter/setter、无参构造
~~~

实体

~~~java
@Data
public class Emp {
    private Integer id;
    private String ename;
    private String joindate;
    private Double salary;
    private Double bonus;
    private Integer dept_id;
}
//注解在类上；编译源码的时候会自动生成toString，getter/setter、无参构造
~~~

坐标

~~~xml
<!--lombok-->
<dependency>
<groupId>org.projectlombok</groupId>
<artifactId>lombok</artifactId>
<version>1.16.20</version>
</dependency>
~~~



#### 注意：需要在IDEA中配置lombok插件：

1 在设置setting 中找到plugins。在检索框中检索lom：

![1](img\1.bmp)

2 在setting中找到 下图界面，在右侧红框出打钩：

![2](img\2.bmp)

3 重启idea













​		   	   

