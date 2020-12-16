

# Redis服务端配置

解压Redis-x64-5.0.9.zip

在解压后的目录打开命令行窗口

![image-20200920165928989](redis启动.assets/image-20200920165928989.png)

在控制台输入启动命令:

```shell
redis-server.exe redis.windows.conf
```

看到下面界面说明启动成功

![image-20200920170012863](redis启动.assets/image-20200920170012863.png)

# Redis客户端

使用redis-cli连接

直接双击

![image-20200920170104369](redis启动.assets/image-20200920170104369.png)

输入命令进行测试



```shell
# 设置
set name tom
# 获取
get name
```

![image-20200920170153031](redis启动.assets/image-20200920170153031.png)