# Clash配置管理服务

> Across the Great Wall, we can reach every corner in the world

## 运行条件：
* jdk8+
* MySQL数据库 [application.properties](src%2Fmain%2Fresources%2Fapplication.properties) 配置连接信息

## 本服务实现了以下几种功能

* 支持将多个机场订阅地址、个人自建节点, 进行统一配置管理, 定时更新订阅配置, 并聚合为一个clash订阅使用
* 支持自由配置`rules`分流规则、`proxies`节点、`Proxy Groups `策略组、`tun`代理模式, 存储在数据库, 实现一处配置多设备使用
* 支持自动通过 [CloudflareSpeedTest](https://github.com/XIU2/CloudflareSpeedTest) 优选ip保存至数据库, 并在获取配置时自动将ip添加到配置中, 实现自动化的优选ip

## 如何使用

### 1.[数据库表结构说明](DB_TABLE.MD)
因为本项目相关配置均需要直接修改表数据, 所以每个表的数据、字段含义需要有所了解

### 2.[建表语句.sql](src%2Fmain%2Fresources%2Fschema.sql) [数据初始化.sql](src%2Fmain%2Fresources%2Fdata.sql)
需要在自己数据库中执行sql进行创建表与初始化数据, 或者配置[application.properties](src%2Fmain%2Fresources%2Fapplication.properties)的`spring.sql.init.mode=always`, 在启动项目时这两个sql文件会自动执行, 但注意执行后删去这段配置, **否则每次启动项目都会自动初始化数据库**

注意`clash_basic`、`clash_subscribe_config`、`cdn_speed`表中的数据为演示数据, 建议清空后按照自身情况保存数据

### 3.获取订阅配置
调用接口 `/api/client/subscribe?ver=1,2&refresh=1` 获取配置内容

`ver=1,2`代表获取`clash_rules`中`ver in (0, 1, 2)`的组合配置, 此入参可以不传, 默认只查询ver=0数据

`refresh=1`代表先更新`clash_subscribe_config`表订阅配置到`clash_proxies`, 再生成配置内容返回,
因为更新配置需要几秒的时间, 所以可以通过[定时任务](src%2Fmain%2Fjava%2Fclash%2Fproxies%2Fmanage%2Fcontroller%2FScheduledTask.java)每天定时更新配置,
此入参可以不传, 默认不更新订阅配置

### 4.更新cdn优选IP

此功能依赖 [CloudflareSpeedTest](https://github.com/XIU2/CloudflareSpeedTest) 完成对cdn节点的测速, 推荐在同一网络环境或网络运营商测速, 结果更准确。
测速完成后通过调用`/api/upload/cdn/speed?name=cf`api接口更新测速结果到`cdn_speed`表, `name`为标注这是什么cdn运营商的节点, 且需要在`cdn_config`表配置, 可以通过以下shell脚本,进行自动测速与结果上传
```shell
#测速
./CloudflareST -f ./ip.txt -o ./result.csv -n 1000  -dn 10
#上传结果
curl -L -F 'resultCsv=@"./result.csv"' "http://127.0.0.1:8080/api/upload/cdn/speed?name=cf"  --insecure
```

### 5.接口安全校验
以上提到的接口提供了拦截器进行token校验, 需要在`interceptor.authorization.token=`中配置, 便会对接口请求进行安全校验

### 6.如何自定义策略组 路由规则
>如果你不知道想要配置的网站请求的域名是什么, 可以打开clash的运行日志, 并再次请求网站, 日志中有具体记录

假如需要将`apple.com`设置需要走代理, `apple.cn`需要进行直连, 那么可以进行以下配置
```sql
INSERT INTO `clash_rules`(`sort`, `type`, `host`, `remark`, `proxies_group_code`, `ver`, `extend_config`) 
VALUES 
    (NULL, 'DOMAIN-SUFFIX', 'apple.com', NULL, '节点选择组', 0, NULL),
    (NULL, 'DOMAIN-SUFFIX', 'apple.cn', NULL, '直连组', 0, NULL);
```
假如需要将`哔哩哔哩`切换到港澳台地区, 那么可以进行以下配置

```sql
-- 第一步添加bilibili的策略组，并设定组内的节点需要包含(香港|澳门|台湾)关键字
INSERT INTO `clash_proxy_groups`( `code`, `name`, `url`, `interval`, `type`, `other_group_code`, `extend_config`, `subscribe_info`, `match_proxy_node_expression`) 
VALUES ('bilibili组', 'bilibili', NULL, NULL, 'select', '直连组', NULL, NULL, '#name matches \'.*(香港|澳门|台湾).*\'');
-- 第二步，设置bilibili的相关域名走bilibili的策略组
INSERT INTO `clash_rules`(`sort`, `type`, `host`, `remark`, `proxies_group_code`, `ver`, `extend_config`) 
VALUES 
    (NULL, 'DOMAIN-SUFFIX', 'bilibili.com', NULL, 'bilibili组', 0, NULL),
    (NULL, 'DOMAIN-SUFFIX', 'biliapi.net', NULL, 'bilibili组', 0, NULL);
```


## 适用范围
本项目主要基于[Clash Premium](https://clash.wiki/)内核文档进行开发, 因此对基于`Clash Meta` `Stash`内核的工具可能会存在不兼容的情况


## 问题排查

### cdn加速配置
如[clash_proxies表cdn_name字段说明](DB_TABLE.MD#clash_proxies), 此种实现CDN加速的方式仅是针对于vmess+ws协议, 其他协议是否支持cdn以及如何配置需要自己确认并对源码进行修改

### 配置文件报错
通过报错信息, 再结合接口响应的Yaml配置文件, 基本可以定位出哪里的配置有问题。一般是`clash_basic`的`value`字段配置有误, 尤其是冒号(:)后注意需要有空格

### 路由配置没有生效
首先需要通过clash的日志确认请求命中了哪一条路由规则并走了哪个策略组。
一般是因为`clash_rules`配置的顺序错误, 因为路由规则匹配是从上到下, 所以需要确保自己的想要命中的路由规则应该尽可能位于上方, 可以通过`sort`字段进行调整

另外`GEOIP-CN`这种配置也可能被需要代理的域名命中, 所以需要根据[自定义路由规则](#6如何自定义策略组-路由规则)配置上这些域名

# 结尾
本服务是为了通过数据库配置, 更方便自由的生成自己需要的配置文件, 因此如果有配置相关问题, 请查阅官方文档

* [Clash](https://clash.wiki/configuration/outbound.html#proxy-groups-策略组)
* [Stash](https://stash.wiki/proxy-protocols/proxy-types)



