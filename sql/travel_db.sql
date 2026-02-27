create table attraction
(
    spot_id      bigint auto_increment
        primary key,
    name         varchar(255)                             not null comment '景点名称',
    city         varchar(100)                             null comment '所属城市',
    description  varchar(1000)                            null comment '简介(短)',
    content_text text                                     null comment '详细介绍(用于AI RAG)',
    address      varchar(255)                             null comment '详细地址',
    tags         varchar(255)                             null comment '标签(逗号分隔)',
    open_time    varchar(100)                             null comment '开放时间',
    ticket_price decimal(10, 2) default 0.00              null comment '门票价格',
    image_url    varchar(500)                             null comment '封面图',
    rating       decimal(2, 1)  default 4.5               null comment '评分',
    latitude     decimal(10, 6)                           null comment '纬度',
    longitude    decimal(10, 6)                           null comment '经度',
    is_hot       int            default 0                 null comment '是否热门(0否1是)',
    create_time  datetime       default CURRENT_TIMESTAMP null
)
    charset = utf8mb4;

create table banner
(
    banner_id   int auto_increment
        primary key,
    image_url   varchar(500)                       not null,
    link_url    varchar(500)                       null,
    sort_order  int      default 0                 null,
    is_show     tinyint  default 1                 null,
    create_time datetime default CURRENT_TIMESTAMP null
)
    comment '首页轮播图';

create table comment
(
    comment_id  bigint auto_increment
        primary key,
    user_id     bigint                             not null,
    spot_id     bigint                             not null,
    content     text                               not null,
    score       int      default 5                 null,
    create_time datetime default CURRENT_TIMESTAMP null
)
    charset = utf8mb4;

create table itinerary
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                                 not null,
    title       varchar(100) default 'AI智能行程'      null,
    note        text                                   null comment '行程内容',
    start_date  date                                   null,
    end_date    date                                   null,
    create_time datetime     default CURRENT_TIMESTAMP null
)
    charset = utf8mb4;

create table sys_ai_note
(
    note_id     bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '用户ID',
    content     text                               not null comment 'AI回答的内容',
    create_time datetime default CURRENT_TIMESTAMP null
)
    charset = utf8mb4;

create table sys_order
(
    order_id    bigint auto_increment
        primary key,
    user_id     bigint                                not null,
    spot_id     bigint                                not null,
    spot_name   varchar(100)                          null,
    price       decimal(10, 2)                        null,
    status      varchar(20) default 'UNPAID'          null comment 'UNPAID, PAID',
    create_time datetime    default CURRENT_TIMESTAMP null
)
    charset = utf8mb4;

create table sys_user
(
    user_id     bigint auto_increment comment '用户ID'
        primary key,
    username    varchar(50)                           not null comment '用户名',
    password    varchar(100)                          not null comment '密码',
    email       varchar(100)                          null comment '邮箱',
    role        varchar(20) default 'user'            null comment '角色',
    create_time datetime    default CURRENT_TIMESTAMP null,
    tags        varchar(255)                          null comment '兴趣标签(逗号分隔)',
    name        varchar(255)                          null comment '用户昵称/姓名',
    avatar      varchar(255)                          null comment '头像URL',
    phone       varchar(20)                           null comment '联系电话',
    gender      varchar(10)                           null comment '性别',
    age         int                                   null comment '年龄',
    city        varchar(100)                          null comment '所在城市',
    status      tinyint(1)  default 1                 null comment '状态: 1正常 0禁用',
    constraint username
        unique (username)
)
    comment '用户表';

create table ai_chat_record
(
    chat_id       bigint auto_increment
        primary key,
    user_id       bigint                             not null,
    user_question text                               not null,
    ai_answer     text                               not null,
    create_time   datetime default CURRENT_TIMESTAMP null,
    constraint ai_chat_record_ibfk_1
        foreign key (user_id) references sys_user (user_id)
)
    comment 'AI问答记录表';

create index user_id
    on ai_chat_record (user_id);

create table post
(
    post_id     bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '作者ID',
    title       varchar(200)                       not null comment '标题',
    cover_img   varchar(500)                       null comment '封面图',
    content     text                               null comment '正文内容(支持HTML)',
    view_count  int      default 0                 null comment '阅读量',
    create_time datetime default CURRENT_TIMESTAMP null,
    constraint post_ibfk_1
        foreign key (user_id) references sys_user (user_id)
)
    comment '社区攻略表';

create index user_id
    on post (user_id);

create table user_favorite
(
    favorite_id bigint auto_increment
        primary key,
    user_id     bigint                             not null,
    spot_id     bigint                             not null,
    create_time datetime default CURRENT_TIMESTAMP null,
    constraint uk_user_spot
        unique (user_id, spot_id),
    constraint user_favorite_ibfk_1
        foreign key (user_id) references sys_user (user_id),
    constraint user_favorite_ibfk_2
        foreign key (spot_id) references attraction (spot_id)
)
    comment '用户收藏表';

create index spot_id
    on user_favorite (spot_id);

