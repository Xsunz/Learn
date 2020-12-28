# Linux搭建web环境手册

## 说明

~~~
说明：该笔记是在linux上安装jdk,mysql,tomcat,redis，nginx软件，以便于建立一个linux系统下的web环境
建议：保留该安装笔记 以便于企业复用
~~~

## 软件安装命令：rpm

~~~
作用：相当于软件助手，可以查询已安装的软件、卸载软件和安装软件。
安装：rpm -ivh 软件名  例如：rpm -ivh mysql
卸载：rpm -e --nodeps 软件名 rpm -e --nodeps mysql
查看所有安装的软件：rpm -qa 	例如查所有的安装软件中是否有tomcat软件： rpm -qa | grep tomcat
~~~

## 安装准备

~~~
在/usr/local目录下创建jdk文件夹，mysql文件夹，tomcat文件夹，redis文件夹存放安装内容
~~~

## jdk安装

~~~~
步骤1：查看当前Linux系统是否已经安装jdk
命令 rpm -qa | grep java    注意:在7中没有 在6中有
步骤2：将jdk压缩包放入/usr/local/jdk目录下
上传工具选定文件夹上传
步骤3：进入 /usr/local/jdk目录下,解压jdk
命令 cd /usr/local/jdk
命令 tar -zxvf jdk-8u181-linux-x64.tar.gz
测试 java -version(不成功,得配置环境变量)
步骤4：配置环境变量
命令 vi /etc/profile
在文件的最后面添加如下代码  
export JAVA_HOME=/usr/local/jdk/jdk1.8.0_181
export PATH=$JAVA_HOME/bin:$PATH
ps:JAVA_HOME这个路径配置的是自己jdk解压之后的路径
步骤5：重新加载配置文件
命令 source /etc/profile
~~~~

最终测试: java -version

	java version "1.8.0_181"
	Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
	Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)
	出现这些东西,证明安装全部成功
## tomcat安装

	步骤1：查看当前Linux系统是否已经安装tomcat
	命令rpm -qa | grep tomcat
	步骤2：将tomcat压缩包放入/usr/local/tomcat目录下
	上传工具选定文件夹上传
	步骤3：进入/usr/local/tomcat目录下,解压tomcat
	命令 cd /usr/local/tomcat
	命令 tar -zxvf apache-tomcat-8.5.27.tar.gz
	步骤4: 进入/usr/local/tomcat/apache-tomcat-8.5.27/bin 启动tomcat
	命令 ./startup.sh
	测试: 通过浏览器访问8080端口(不成功,得配置8080端口开放)
	步骤5: 修改防火墙的配置文件
	命令 vi /etc/sysconfig/iptables
	复制(yy)	
	-A INPUT -m state --state NEW -m tcp -p tcp --dport 22 -j ACCEPT
	粘贴(p)
	-A INPUT -m state --state NEW -m tcp -p tcp --dport 8080 -j ACCEPT
	步骤6:重启防火墙服务					
	service iptables restart
## mysql安装

~~~
步骤1：查看当前Linux系统是否已经安装mysql
命令 rpm -qa | grep mysql
虽然没有安装mysql,但是有自带的数据库：mariadb
步骤2：查看并卸载自带的数据库
查看命令 rpm -qa | grep mariadb
卸载命令 rpm -e --nodeps mariadb-libs-5.5.56-2.el7.x86_64
步骤3：将mysql压缩包放入/usr/local/mysql目录下,解压mysql
上传工具选定文件夹上传
进入mysql文件夹：cd /usr/local/mysql
命令 tar -xvf MySQL-5.5.49-1.linux2.6.i386.rpm-bundle.tar
步骤4：先安装解压后的服务器端(MySQL-server-5.6.22-1.el6.i686.rpm)
命令 rpm -ivh MySQL-server-5.5.49-1.linux2.6.i386.rpm
ps：会缺依赖,安装mysql服务器端软件需要依赖如下(4个依赖包)
libaio.so.1
libc.so.6
libgcc_s.so.1(这个版本有冲突，需要先卸载再安装)
libstdc++.so.6（这个版本有冲突，需要先卸载在安装）
步骤5：安装依赖
命令 yum install libaio.so.1
命令 yum install libc.so.6
步骤6：先卸载 libgcc 再安装 libgcc
卸载命令
rpm -qa|grep libgcc
rpm -e --nodeps libgcc-4.8.5-16.el7.x86_64
安装命令
yum install libgcc_s.so.1
步骤7：先卸载libstdc++再安装libstdc++
卸载命令
rpm -qa|grep libstdc
rpm -e --nodeps libstdc++-4.8.5-16.el7.x86_64
安装命令
yum install libstdc++.so.6
步骤8：重新执行安装服务器端命令
命令 rpm -ivh  MySQL-server-5.5.49-1.linux2.6.i386.rpm
ps:在安装的过程中,记得复制这段设置密码的格式文本---用来后面设置登录密码的
设置登录密码的格式：/usr/bin/mysqladmin -u 用户名 password '要设置的登录密码'

步骤9：安装客户端(MySQL-client-5.5.49-1.linux2.6.i386.rpm)
命令 rpm -ivh MySQL-client-5.5.49-1.linux2.6.i386.rpm
ps：会缺依赖,安装mysql客户端端也需要依赖如下软件
libncurses.so.5
执行安装依赖命令:yum install libncurses.so.5
再次执行：rpm -ivh MySQL-client-5.5.49-1.linux2.6.i386.rpm
步骤10：启动mysql服务
命令 service mysql start
步骤11：将mysql设置开机自动启动服务
命令 systemctl enable mysql
步骤12: 设置密码操作
/usr/bin/mysqladmin -u root password '密码'
步骤13：登录mysql
命令 mysql -u用户名 -p密码


使用远程sqlyog图形化界面方式
步骤14：修改防火墙的配置文件(放行3306端口号)
命令 vi /etc/sysconfig/iptables
复制(yy)	
-A INPUT -m state --state NEW -m tcp -p tcp --dport 22 -j ACCEPT
粘贴(p)
-A INPUT -m state --state NEW -m tcp -p tcp --dport 3306 -j ACCEPT
重启防火墙服务:service iptables restart

步骤15: 允许远程连接linux下的mysql
默认情况下mysql为安全起见,不支持远程登录mysql,所以需要设置开启远程登录mysql的权限登录mysql
进入mysql输入命令
权限设置命令：grant all privileges on *.* to '用户名'@'%' identified by '密码';
刷新权限命令：flush privileges;
~~~

## linux下安装的mysql中文乱码解决问题

~~~~
查看服务器的编码：show variables like '%char%'; 字符集编码为latin1 不是utf-8
解决：
步骤1：停止mysql服务器  
service mysql stop 
步骤2：将/usr/share/mysql/my-medium.cnf 复制到/etc目录下,且重命名为my.cnf
			命令：cp /usr/share/mysql/my-medium.cnf /etc/my.cnf
步骤3：编辑my.cnf,在[mysqld]下添加一行"character-set-server=utf8" 保存退出
步骤4：重启mysql服务器 新建数据库新建表查看编码
			命令：service mysql start
~~~~

## redis安装

~~~
步骤1: yum install gcc-c++  //因为是C语言编写的,需要C语言的环境
步骤2: 将redis压缩包放入/usr/local/redis目录下
上传工具选定文件夹上传
步骤3：进入/usr/local/redis目录下,解压redis
命令 cd /usr/local/redis		
     tar -zxvf redis-3.0.7.tar.gz	
步骤4: 进入解压文件编译并安装redis
命令 cd redis-3.0.7
     make
     make PREFIX=/usr/local/redis install
安装成功之后 在redis目录下多出来一个bin目录
cd /usr/local/redis 
ll
步骤5：启动(服务器)
将redis-3.0.7目录下的redis.conf文件复制到 /usr/local/redis/bin 下
命令 cp redis.conf /usr/local/redis/bin/		 
修改redis.conf配置文件 设置为启动服务加载配置文件
命令 vi redis.conf
按键盘问号：搜索/daemonize
将 daemonize 值改成yes即可
启动: ./redis-server redis.conf     
步骤6：使用redis的客户端连接redis服务器			
连接指定主机 指定端口号			
命令 ./redis-cli -h ip -p 端口 	

步骤7：如果要使用可视化图形工具连接 要修改防火墙的配置文件
命令 vi /etc/sysconfig/iptables
复制(yy)	
-A INPUT -m state --state NEW -m tcp -p tcp --dport 22 -j ACCEPT
粘贴(p)
-A INPUT -m state --state NEW -m tcp -p tcp --dport 6379 -j ACCEPT
步骤7:重启防火墙服务					
service iptables restart
步骤8:关闭服务器
./redis-cli -h ip -p 端口 shutdown  或kill -9 pid
~~~

## Nginx的安装

~~~
1安装C语言环境（gcc）
命令：yum install gcc-c++   安装期间有提示，一律选y（如果安装过 可以跳过该步骤）

2安装Nginx依赖环境，‐y表示所有提示默认选择y
yum -y install pcre pcre-devel
yum -y install zlib zlib-devel
yum -y install openssl openssl-devel

3放入/usr/local下解压并进入解压目录
上传工具选定文件夹上传
tar -zxvf nginx-1.13.9.tar.gz

4编译并安装
cd nginx-1.13.9
./configure
make
make install
ps:安装成功之后,就会在/usr/local下多出了一个nginx目录.

5启动/停止 Nginx
进入nginx的sbin目录:cd /usr/local/nginx/sbin
./nginx  启动
./nginx -s stop 停止 
./nginx -s reload 重启 

6 在防火墙配置文件中开放80端口
vi /etc/sysconfig/iptables
重启防火墙服务：service iptables restart

7 查看是否有nginx的线程是否存在
命令 ps -ef | grep nginx
~~~

## 配置Nginx的反向代理

~~~
步骤一：修改nginx的配置文件
cd /usr/local/nginx/conf
命令 vim nginx.conf
步骤二：增加或修改如下内容
server(服务器)上增加代理

upstream tomcat{server localhost:8080;} #为谁代理服务
server {
　　　　listen 80;
　　　　server_name localhost;
　　　　location / {
           # root   html;
           # index  index.html index.htm;
           
           proxy_pass http://tomcat; # 访问tomcat
        }
}
步骤三：重启nginx
命令 
./nginx
./nginx -s stop
./nginx -s reload
~~~
