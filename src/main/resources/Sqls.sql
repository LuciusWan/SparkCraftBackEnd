# 数据库初始化

-- 创建库
create database if not exists spark_craft;

-- 切换库
use spark_craft;

-- 用户表
-- 以下是建表语句


-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

create table chat_history
(
    id          bigint auto_increment comment 'id'
        primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)
    comment '对话历史' collate = utf8mb4_unicode_ci;

create index idx_appId
    on chat_history (appId);

create index idx_appId_createTime
    on chat_history (appId, createTime);

create index idx_createTime
    on chat_history (createTime);

create table if not exists image_project(
    id bigint auto_increment comment 'id' primary key,
    useId bigint not null comment '用户id',
    projectName varchar(256) not null comment '项目名称',
    projectDesc varchar(1024) not null comment '项目描述',
    projectImageUrl text null comment '生成图片地址',
    ProductionProcess text null comment '制造流程',
    projectStatus varchar(32) default 'waiting' not null comment '项目状态',
    3DModelUrl text null comment '3D模型地址',
    priority     int      default 0                 not null comment '优先级',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
)