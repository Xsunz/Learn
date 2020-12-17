# linux安装rabbitMQ

1. 进入linux系统 创建文件夹

   mkdir rabbitMQ

   ![1567655661414](imgs\1567655661414.png)

2. 进入文件夹 

   cd rabbitMQ

3.  解析主机 复制安装包

   wget http://packages.erlang-solutions.com/erlang-solutions-1.0-1.noarch.rpm

4.   安装依赖环境

   yum install epel-release   有选择全部选y

   rpm -Uvh erlang-solutions-1.0-1.noarch.rpm

   rpm --import http://packages.erlang-solutions.com/rpm/erlang_solutions.asc 

   sudo yum install erlang   安装erlang 有选择全部选y

5.  将本地的rabbitMQ的服务器程序上传到rabbitMQ目录下

   rabbitmq-server-3.7.17-1.el7.noarch.rpm

   cd /root/rabbitMQ  进入MQ目录

   yum install socat 安装依赖

   rpm -ivh rabbitmq-server-3.7.17-1.el7.noarch.rpm  安装服务器 

6. 启动停止命令

   service rabbitmq-server start
   service rabbitmq-server stop
   service rabbitmq-server restart

7.  设置开机自动启动

   chkconfig rabbitmq-server on

8.  配置文件

   cd /etc/rabbitmq  进入配置目录

   cp /usr/share/doc/rabbitmq-server-3.7.17/rabbitmq.config.example /etc/rabbitmq 复制配置文件

   修改文件名称

   mv rabbitmq.config.example rabbitmq.config

9.   开启远程访问 找到如下配置 删除前面的百分号 以及最后的逗号

   vi /etc/rabbitmq/rabbitmq.config

   ![1567657114755](imgs\1567657114755.png)

10.  启动插件管理控制台

    rabbitmq-plugins enable rabbitmq_management  启动插件

    service rabbitmq-server start 启动服务器

11.   开放端口

    firewall-cmd --zone=public --add-port=15672/tcp --permanent

    firewall-cmd --zone=public --add-port=5672/tcp --permanent

    service firewalld restart  重启服务

12.  登陆控制台访问

    ![1567657752799](imgs\1567657752799.png)
    
13. 如果使用的是提供好的虚拟机 账号/密码: root /  123456