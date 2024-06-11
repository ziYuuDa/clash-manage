## clash_basic

> 必用到的表,此表数据全部手动维护

配置clash的 `port` `dns` `tun`等基本配置,属于极少改动的固定配置

| 字段          | 类型        | 说明                                                                                    | 必填 | 默认值    |
|-------------|-----------|---------------------------------------------------------------------------------------|----|--------|
| id          | int       | 主键                                                                                    | 是  |        |
| sort        | int       | 排序,为空则将id作为值,越小越靠前                                                                    | 否  |        |
| parent_code | varchar   | 父code,一级为0；二级为一级code；三级为一级code,二级code；以此类推                                            | 是  | 0      |
| code        | varchar   | key值,当是list元素时可以为空                                                                    | 否  |        |
| value_type  | varchar   | value类型,不同类型解析数据时做对应的序列化处理,<br/> int（数字）boolean（布尔）string（字符串）object（键值对象）list（字符串数组） | 是  | string |
| value       | varchar   | value值,value_type=object、list时可为空                                                     | 否  |        |
| desc        | text      | 备注字段,程序中不使用                                                                           | 否  |        |
| del         | tinyint   | 1-有效 0-无效                                                                             | 是  | 1      |
| update_time | timestamp | 更新时间,程序中不使用                                                                           | 是  | 当前时间   |
| create_time | timestamp | 创建时间,程序中不使用                                                                           | 是  | 当前时间   |

## clash_proxies

> 必用到的表,此表数据subscribe_name有值代表是订阅链接的节点配置,由定时任务维护,其他数据手动维护

【[配置文档](https://stash.wiki/proxy-protocols/proxy-types)】配置clash节点(
配置文件的proxies),[订阅链接](#clash_subscribe_config)自动获取的节点、自己的手动录入的节点,全部保存于这里

| 字段             | 类型        | 说明                                                                                                              | 必填 | 默认值  | 
|----------------|-----------|-----------------------------------------------------------------------------------------------------------------|----|------|
| id             | int       | 主键                                                                                                              | 是  |      | 
| name           | varchar   | 名称                                                                                                              | 是  |      | 
| expired_time   | timestamp | 过期时间,已过期的不会使用                                                                                                   | 否  |      | 
| subscribe_name | varchar   | 订阅名称,对应clash_subscribe_config表的name                                                                             | 否  |      | 
| tag            | varchar   | 标签,通过手动打标,方便分组一些节点                                                                                              | 否  |      | 
| cdn_name       | varchar   | cdn名称,有值则会查询cdn_speed表的ip套上cdn(会裂变为多个节点,具体看[cdn_speed](#cdn_speed)说明),即自动更改value字段内的server、ws-opts.headers.Host | 否  |      | 
| del            | tinyint   | 1-有效 0-无效,订阅任务自动更新时,如果节点没有返回,会自动置为0                                                                             | 是  | 1    | 
| value          | text      | 订阅内容,其中的name值会被name字段覆盖,特别注意内容为Yaml格式时,冒号(:)后需要有空格                                                              | 是  |      | 
| modify_log     | text      | 修改记录,订阅任务自动更新时,如果配置发生更改会在这里记录                                                                                   | 否  |      | 
| update_time    | timestamp |                                                                                                                 | 是  | 当前时间 | 
| create_time    | timestamp |                                                                                                                 | 是  | 当前时间 | 

## clash_proxy_groups

> 必用到的表,此表数据全部手动维护

【[配置文档](https://stash.wiki/proxy-protocols/proxy-groups)】配置clash策略组(配置文件的proxy-groups),
即clash GUI界面的每个分组,以及记录每个分组中要包含哪些节点,

| 字段                          | 类型        | 说明                                                                                                              | 必填 | 默认值                                 | 
|-----------------------------|-----------|-----------------------------------------------------------------------------------------------------------------|----|-------------------------------------|
| id                          | int       | 主键,生成的配置中越小越靠前                                                                                                  | 是  |                                     | 
| del                         | tinyint   | 1-有效 0-无效                                                                                                       | 是  | 1                                   | 
| code                        | varchar   | 外表关联的外键                                                                                                         | 是  |                                     | 
| name                        | varchar   | 展示的名称                                                                                                           | 是  |                                     | 
| type                        | varchar   | 策略组类型,如：select、url-test、load-balance、fallback                                                                   | 是  |                                     | 
| url                         | varchar   | 测试节点延迟的网站url，type为select、load-balance、fallback必填                                                                | 否  | http://www.gstatic.com/generate_204 | 
| interval                    | int       | 测试时间间隔,单位：秒                                                                                                     | 否  | 60                                  | 
| other_group_code            | varchar   | 包含的其他组的code,逗号分割                                                                                                | 否  |                                     | 
| extend_config               | varchar   | 扩展配置,格式为: key:value, 比如load-balance的额外配置:strategy:round-robin                                                   | 是  |                                     | 
| subscribe_info              | varchar   | 如果此group专门存储某个订阅的节点,可以将节点名称写在这里,生成配置文件时会自动通过[clash_subscribe_config](#clash_subscribe_config)获取订阅剩余流量、过期时间,进行展示 | 是  |                                     | 
| match_proxy_node_expression | varchar   | 用于匹配包含哪些节点的SpEL表达式,具体使用说明见[说明](#match_proxy_node_expression配置说明)                                                | 是  |                                     | 
| update_time                 | timestamp |                                                                                                                 | 是  | 当前时间                                | 
| create_time                 | timestamp |                                                                                                                 | 是  | 当前时间                                | 

### match_proxy_node_expression配置说明

目前代码中定义了这几个参数用于判断是否要包含此节点

| 字段            | 类型             | 定义       | 说明                                          | 
|---------------|----------------|----------|---------------------------------------------|
| subscribeFlag | boolean        | 是否是订阅节点  | clash_proxies表subscribe_name有值为ture         |
| subscribeName | String         | 订阅名称     | clash_proxies表subscribe_name                |
| cdnFlag       | boolean        | 是否是cdn节点 | clash_proxies表cdn_name有值为ture               |
| cdnName       | String         | cdn名称    | clash_proxies表cdn_name                      |
| name          | String         | 节点名称     | clash_proxies表name                          |
| id            | int            | 节点id     | clash_proxies表id                            |
| tagList       | List\<String\> | 标签列表     | clash_proxies表tag,通过逗号切换后的 List\<String\>数组 |

然后通过以下举例的表达式将进行判断此节点是否包含在此组,如果有问题可以查询相关SpEL说明文档。[SpEL 表达式使用文档](https://www.baeldung.com/spring-expression-language)

| 例子                              | 说明                     | 
|---------------------------------|------------------------|
| true                            | 所有节点                   |
| !#subscribeFlag                 | 非订阅的节点                 |
| #cdnFlag                        | 套了CDN的节点               |
| '机场A'.equals(#subscribeName)    | 订阅名称是'机场A'的节点          |
| #name matches '.\*(日本\|新加坡).\*' | 订阅名称中有'日本'或'新加坡'关键词的节点 |

## clash_rules

> 必用到的表,此表数据全部手动维护

【[配置文档](https://stash.wiki/rules/rule-types)】配置clash路由规则(配置文件的rules)
,即客户端访问不同网站时,每个url走哪些策略组,实现不同网站的不同路由规则

需要特别注意的是,路由匹配是从上到下匹配到符合的便结束,所以先后顺序十分重要,要通过 `id` `sort`
进行好控制,尤其是 `CN` `MATCH` 这两个规则应该确保顺序在最后

| 字段                 | 类型        | 说明                                               | 必填 | 默认值  | 
|--------------------|-----------|--------------------------------------------------|----|------|
| id                 | int       | 主键                                               | 是  |      | 
| sort               | int       | 排序,为空则将id作为值,越小越靠前                               | 否  |      | 
| type               | varchar   | 规则类型,如：DOMAIN-SUFFIX 、DOMAIN-KEYWORD、GEOIP、MATCH | 是  |      | 
| host               | varchar   | 域名内容                                             | 否  |      | 
| remark             | varchar   | 备注说明                                             | 否  |      | 
| proxies_group_code | varchar   | 策略组的code,也可以是DIRECT、REJECT这两个规则                  | 否  |      | 
| ver                | int       | 版本号,用于区分不同的组,生成订阅时可根据传值获取不同组的配置                  | 是  |      | 
| extend_config      | varchar   | 扩展配置,如：no-resolve,多个逗号分割                         | 否  |      | 
| del                | tinyint   | 1-有效 0-无效                                        | 是  | 1    | 
| create_time        | timestamp |                                                  | 是  | 当前时间 | 
| update_time        | timestamp |                                                  | 是  | 当前时间 | 

## cdn_config

> cdn功能表,如果不使用cdn功能则此表数据可为空,此表数据全部手动维护

配置cdn的一些规则

| 字段                  | 类型        | 说明                                        | 必填 | 默认值  | 
|---------------------|-----------|-------------------------------------------|----|------|
| id                  | int       | 主键                                        | 是  |      | 
| name                | varchar   | 名称                                        | 是  |      | 
| good_ip_save_size   | int       | 优选ip保存到cdn_speed表时限制最大多少条                 | 是  | 5    | 
| good_ip_query_size  | int       | 从cdn_speed表中查询多少条优选ip                     | 是  | 5    | 
| good_ip_speed_limit | decimal   | 保存cdn_speed表数据时,要求的speed最小值,防止保存速度为0的无效节点 | 否  | 1    | 
| update_time         | timestamp |                                           | 是  | 当前时间 | 
| create_time         | timestamp |                                           | 是  | 当前时间 | 

## cdn_speed

> 与cdn_config配套使用的表,此表数据由CloudflareSpeedTest脚本生成维护

记录cdn的优秀ip的连接速度,用于当作优选ip套在proxies节点中

```
生成配置时,查询此表id>0,通过update_time倒叙排序的ip数据,并且限制会通过`cdn_config`表`good_ip_size`的条数,防止查出很多条数据,这种数据叫做优选ip

如果需要某些ip表现优选需要恒定查出来,那么可以设置id<0,有多少条会查出多少条,这种数据叫做最佳ip

最后原本的节点,会被 原始域名+优选ip+最佳ip 覆盖为多条,如原本的节点名称叫 aws_cf, 那么覆盖后的节点为：
目标host为cdn域名的节点,即cdn厂商自动为你分配ip：aws_cf_origin
目标host为最佳ip的节点：aws_cf_b1、aws_cf_b2、aws_cf_b3、aws_cf_b{x}……
目标host为优选ip的节点：aws_cf_g1、aws_cf_g2、aws_cf_g3、aws_cf_g{x}……
```

| 字段          | 类型        | 说明                         | 必填 | 默认值  | 
|-------------|-----------|----------------------------|----|------|
| id          | int       | 主键                         | 是  |      | 
| ip          | varchar   | 优选ip                       | 是  |      | 
| speed       | decimal   | 速度,单位MB/s                  | 否  |      | 
| count       | int       | 计数,ip多次出现时次数累加,也代表了这个是优质ip | 是  | 1    | 
| cdn_name    | varchar   | cdn_config的name            | 是  | cf   | 
| del         | tinyint   | 1-有效 0-无效                  | 是  | 1    | 
| update_time | timestamp | 修改时间,查询时通过此时间倒序排序          | 是  | 当前时间 | 
| create_time | timestamp |                            | 是  | 当前时间 | 

## clash_subscribe_config

> 如不需要获取订阅链接中节点的功能,此表数据可为空,此表数据全部手动维护

配置clash订阅(机场)信息,用于定时任务自动更新订阅中的节点

| 字段                 | 类型        | 说明                       | 必填 | 默认值  | 值填充方式                                          |
|--------------------|-----------|--------------------------|----|------|------------------------------------------------|
| id                 | int       | 主键                       | 是  |      | 手动录入                                           |
| name               | varchar   | 名称                       | 是  |      | 手动录入                                           |
| url                | varchar   | 订阅链接                     | 是  |      | 手动录入                                           |
| website_url        | varchar   | 官网链接                     | 否  |      | 自动更新<br/>header的profile-web-page-url           |
| flow_total         | bigint    | 总流量,单位byte               | 否  |      | 自动更新<br/>header的subscription-userinfo的total    |
| flow_used_upload   | bigint    | 已用上传流量                   | 否  |      | 自动更新<br/>header的subscription-userinfo的upload   |
| flow_used_download | bigint    | 已用下载流量                   | 否  |      | 自动更新<br/>header的subscription-userinfo的download |
| flow_surplus       | bigint    | 剩余流量                     | 否  |      | 自动更新<br/>upload+download的和                     |
| expire_time        | timestamp | 订阅过期时间                   | 否  |      | 自动更新<br/>header的subscription-userinfo的expire   |
| refresh_flag       | tinyint   | 是否通过定时任务刷新订阅,1-是 0-否     | 是  |      | 手动录入                                           |
| filter_key_word    | varchar   | 过滤节点关键词,用于过滤'剩余流量'这种无效节点 | 否  |      | 手动录入                                           |
| update_time        | timestamp |                          | 是  | 当前时间 | 自动更新                                           |
| create_time        | timestamp |                          | 是  | 当前时间 | 自动更新                                           |
