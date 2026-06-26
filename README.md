# 项目介绍

此项目基于VUE和springboot创建的全栈ai项目

# 环境介绍

## IDE使用

前端使用VScode加claude code

后端使用IDEA

## 前端

## node.js

https://nodejs.org/zh-cn/download，选择24版本以上

终端查看node版本

```shell
node -v
```

## vite：

项目终端运行命令

```shell
npm creat vite@latest
```

按操作等待安装

### 前端项目运行

终端运行

```shell
npm run dev
```

## 后端

### JDK

Java JDK使用的21

### 项目创建

选择springboot,使用maven管理，依赖项选择spring web,mysql driver,open ai

### 后端项目运行

右上角运行TravelJavaApplication.java

## 数据库

mysql创建数据库travel_ai

# docker部署

拉取镜像到本地仓库，详情参照docker部署文档

# 公网访问

由于使用的VM虚拟机，设备IP地址无法被公网访问，因此需要使用内网穿透，这里介绍使用樱花内网穿透

## 隧道创建

进入樱花内网穿透官网https://www.natfrp.com/，创建账号，完成实名认证，下载软件，完成安装，在软件内创建隧道，因为http(s)隧道需要备案才能创建，所以我们使用TCP隧道，本地IP选本地主机127.0.0，本地端口选80HTTP,回到官网，在面板管理，服务，选择修道列表，找到刚才创建的隧道，点击编辑，打开自动HTTPS，再次点击服务，子域绑定，选择子域创建

## Ubuntu虚拟机安装樱花

```shell
# 下载amd64版本客户端
sudo wget -O /usr/local/bin/frpc https://nya.globalslb.net/natfrp/client/frpc/0.51.0-sakura-12.3/frpc_linux_amd64
# 添加执行权限
sudo chmod +x /usr/local/bin/frpc
# 验证安装成功
frpc -v
```

测试启动隧道

```shell
frpc -f abc123token:11111,22222
```

公网访问方式

https://你申请的子域名.nyat.app:端口号

查看日志 

```shell
tail -f frp.log
```

由于樱花是免费frp，因此这个网站会非常慢

## 服务器访问

通过购买腾讯云，阿里云的服务器，将项目安装好

服务器会有一个公网IP，可以通过购买域名来进行绑定，打开服务器的端口允许访问，即可通过域名访问的服务器
