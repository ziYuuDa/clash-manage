-- ----------------------------
-- Table structure for cdn_config
-- ----------------------------
DROP TABLE IF EXISTS `cdn_config`;
CREATE TABLE `cdn_config`
(
    `id`                  int(11) NOT NULL AUTO_INCREMENT,
    `name`                varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'cdn名称',
    `good_ip_save_size`   int(11) NOT NULL DEFAULT 10 COMMENT '优选ip保存条数限制',
    `good_ip_query_size`  int(11) NOT NULL DEFAULT 5 COMMENT '优选ip查询条数限制',
    `good_ip_speed_limit` decimal(6, 2)                                           NOT NULL DEFAULT 1.00 COMMENT '优选ip最小速度限制,单位MB/s',
    `update_time`         timestamp(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT '修改时间',
    `create_time`         timestamp(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `cdn_config_name_IDX`(`name`) USING BTREE
) ENGINE = InnoDB  COMMENT = 'cdn配置表';

-- ----------------------------
-- Table structure for cdn_speed
-- ----------------------------
DROP TABLE IF EXISTS `cdn_speed`;
CREATE TABLE `cdn_speed`
(
    `id`          int(11) NOT NULL AUTO_INCREMENT,
    `ip`          varchar(39) NOT NULL COMMENT 'ip地址',
    `speed`       decimal(6, 2) NULL DEFAULT NULL COMMENT '连接速度，单位MB/s',
    `count`       int(11) NOT NULL DEFAULT 1 COMMENT '出现次数',
    `cdn_name`    varchar(10) NOT NULL DEFAULT 'cf' COMMENT 'cdn名称',
    `del`         int(11) NOT NULL DEFAULT 1 COMMENT '1-有效 0-无效',
    `update_time` timestamp(0)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT '修改时间',
    `create_time` timestamp(0)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `cdn_speed_ip_IDX`(`ip`) USING BTREE,
    INDEX         `cdn_speed_update_time_IDX`(`update_time`, `cdn_name`, `speed`) USING BTREE
) ENGINE = InnoDB COMMENT = 'cdn ip节点速度记录表';

-- ----------------------------
-- Table structure for clash_basic
-- ----------------------------
DROP TABLE IF EXISTS `clash_basic`;
CREATE TABLE `clash_basic`
(
    `id`          int(11) NOT NULL AUTO_INCREMENT,
    `sort`        int(11) NULL DEFAULT NULL COMMENT '排序',
    `parent_code` varchar(255) NOT NULL DEFAULT '0' COMMENT '父节点',
    `code`        varchar(50) NULL DEFAULT NULL COMMENT 'key值',
    `value_type`  varchar(20)  NOT NULL DEFAULT 'string' COMMENT '字段类型：int（数字）;boolean（布尔）；string（字符串）；object（键值对象）;list（字符串数组）；',
    `value`       varchar(255) NULL DEFAULT NULL COMMENT '内容',
    `desc`        text NULL COMMENT '描述',
    `del`         tinyint(4) NOT NULL DEFAULT 1 COMMENT '1-有效 0-无效',
    `update_time` timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT '修改时间',
    `create_time` timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB COMMENT = 'clash 基本信息配置表';

-- ----------------------------
-- Table structure for clash_proxies
-- ----------------------------
DROP TABLE IF EXISTS `clash_proxies`;
CREATE TABLE `clash_proxies`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `name`           varchar(50) NOT NULL COMMENT '名称',
    `expired_time`   timestamp(0) NULL DEFAULT NULL COMMENT '过期时间',
    `subscribe_name` varchar(100) NULL DEFAULT NULL COMMENT '订阅名称',
    `tag`            varchar(100) NULL DEFAULT NULL COMMENT '标签，用于方便给节点分配不同的组，多个逗号隔开',
    `cdn_name`       varchar(10) NULL DEFAULT NULL COMMENT '使用的cdn名称',
    `del`            tinyint(4) NOT NULL DEFAULT 1 COMMENT '1-有效 0-无效',
    `value`          text NOT NULL COMMENT '节点内容',
    `modify_log`     text NULL COMMENT '修改记录',
    `update_time`    timestamp(0)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT '修改时间',
    `create_time`    timestamp(0)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX            `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB COMMENT = 'clash proxies 代理节点配置表';

-- ----------------------------
-- Table structure for clash_proxy_groups
-- ----------------------------
DROP TABLE IF EXISTS `clash_proxy_groups`;
CREATE TABLE `clash_proxy_groups`
(
    `id`                          int(11) NOT NULL AUTO_INCREMENT,
    `del`                         tinyint(4) NOT NULL DEFAULT 1 COMMENT '1-有效 0-无效',
    `code`                        varchar(50)   NOT NULL COMMENT 'code,配置用',
    `name`                        varchar(255)  NOT NULL COMMENT '名称,展示用',
    `url`                         varchar(255) NULL DEFAULT 'http://www.gstatic.com/generate_204' COMMENT '测试节点连通性的url,',
    `interval`                    int(11) NULL DEFAULT 60 COMMENT '测试速度间隔',
    `type`                        varchar(255)  NOT NULL COMMENT '策略组类型',
    `other_group_code`            varchar(255) NULL DEFAULT NULL COMMENT '包含的其他策略组code',
    `extend_config`               varchar(255) NULL DEFAULT NULL COMMENT '扩展配置',
    `subscribe_info`              varchar(100) NULL DEFAULT NULL COMMENT '要展示的订阅信息',
    `match_proxy_node_expression` varchar(1000) NOT NULL DEFAULT 'false' COMMENT '匹配节点spel表达式',
    `update_time`                 timestamp(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT '修改时间',
    `create_time`                 timestamp(0)                                                   NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_code`(`code`) USING BTREE
) ENGINE = InnoDB COMMENT = 'clash proxy groups 策略组配置';

-- ----------------------------
-- Table structure for clash_rules
-- ----------------------------
DROP TABLE IF EXISTS `clash_rules`;
CREATE TABLE `clash_rules`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `sort`               int(11) NULL DEFAULT NULL COMMENT '排序值，越小约靠前',
    `type`               varchar(100) NOT NULL COMMENT '规则类型',
    `host`               varchar(100) NULL DEFAULT NULL COMMENT '规则域名',
    `remark`             varchar(255) NULL DEFAULT NULL COMMENT '说明',
    `proxies_group_code` varchar(255) NULL DEFAULT NULL COMMENT '对应的代理组code',
    `ver`                int(11) NOT NULL DEFAULT 0 COMMENT '版本，0为全局版本',
    `extend_config`      varchar(100) NULL DEFAULT NULL COMMENT '扩展配置',
    `del`                int(11) NOT NULL DEFAULT 1 COMMENT '1-有效 0-无效',
    `create_time`        timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    `update_time`        timestamp(0)                                                  NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT '修改时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `clash_rules_host_IDX`(`host`, `ver`, `type`) USING BTREE
) ENGINE = InnoDB COMMENT = 'clash rules 分流规则配置';

-- ----------------------------
-- Table structure for clash_subscribe_config
-- ----------------------------
DROP TABLE IF EXISTS `clash_subscribe_config`;
CREATE TABLE `clash_subscribe_config`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `name`               varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '名称',
    `url`                varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '订阅地址',
    `website_url`        varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '官网地址',
    `flow_total`         bigint(20) NULL DEFAULT NULL COMMENT '总流量',
    `flow_used_upload`   bigint(20) NULL DEFAULT NULL COMMENT '已用上传流量',
    `flow_used_download` bigint(20) NULL DEFAULT NULL COMMENT '已用下载流量',
    `flow_surplus`       bigint(20) NULL DEFAULT NULL COMMENT '剩余流量',
    `expire_time`        timestamp(0) NULL DEFAULT NULL COMMENT '过期时间',
    `refresh_flag`       tinyint(4) NOT NULL COMMENT '是否刷新订阅，1-是 0-否',
    `filter_key_word`    varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '过滤无效节点关键词',
    `update_time`        timestamp(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP (0) COMMENT '修改时间',
    `create_time`        timestamp(0)                                            NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX                `clash_subscribe_config_name_IDX`(`name`) USING BTREE
) ENGINE = InnoDB  COMMENT = 'clash订阅链接配置表';
