# Linux

## ip地址设置--自动配置/手动配置命令(了解)

需求：之前我们linux的ip地址都是安装的时候自动分配获取的，能不能设置一个固定的ip地址

```
查看网络配置：ifconfig
设置固定ip：
编辑ifcfg-ens33网卡文件 vi /etc/sysconfig/network‐scripts/ifcfg‐ens33
```

信息如下：

```
DEVICE=ens33 #网卡名称
TYPE=Ethernet #网卡类型 以太网
ONBOOT=yes #是否开机就使用此网卡 
BOOTPROTO=dhcp #启动网卡时指定获取IP地址的方式(只需要修改这一个内容)
常用取值 : dhcp （自动获取ip地址,网关,子网掩码等信息无需设置）
常用取值：static （静态ip,如需要访问网络,需要自己设置ip地址等信息）
IPADDR=192.168.50.128 #ip地址
GATEWAY=192.168.50.2 #网关
NETMASK=255.255.255.0 #子网掩码
```

注意：修改完毕后要重启网卡服务：service network restart

~~~
总结：linux的ip地址2种配置方式
linux的主机自动获取ip：BOOTPROTO=dhcp
会自动给当前linux主机分配ip地址    
					 ---------自动获取ip地址

linux的主机手动设置ip：BOOTPROTO=static
手动自己配置ip 网关 子网掩码
IPADDR=192.168.50.128 #ip地址
GATEWAY=192.168.50.2 #网关
NETMASK=255.255.255.0 #子网掩码
					-----------手动设置ip地址	

还需要重启网卡服务 加载修改过后的ifcfg‐ens33配置文件
~~~



## 防火墙设置（iptables防火墙）

注意：conos7没有iptables防火墙了 用的是firewall防火墙

为了适应企业 我们依然把iptables防火墙还原回来 不用firewall防火墙

~~~
1 安装iptables作为防火墙(企业用)
yum install iptables-services    #通过yum install 命令可以从网上下载安装iptables

2 停止firewall及其开机不启动
systemctl stop firewalld.service   #停止firewall
systemctl disable firewalld.service   #禁止firewall开机启动

3 启用iptables,配置开机启动
systemctl start iptables.service   #启动iptables 
systemctl enable iptables.service  #将iptables设置为开机启动

4 查看iptables是否启动成功：systemctl status iptables
关闭iptables防火墙：systemctl stop iptables  #严重不建议
重启iptables防火墙：systemctl restart iptables

ps:只要安装好了iptable这个防火墙，这个防火墙的配置文件在/etc/sysconfig/iptabls
iptabls配置文件：允许某个端口可以被外界访问
后期我们要安装软件 比如tomcat 8080
我们可以通过修改iptables文件设置8080端口可以被外界访问
修改配置文件：vi /etc/sysconfig/iptables  复制指定端口
ps:iptables是iptable防火墙的配置文件 所以必须得安装iptables防火墙才有该配置文件
~~~





