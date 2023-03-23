CREATE TABLE `unitTestString`
(
    `id`     int          not null auto_increment primary key,
    `linkId` bigint       not null,
    `typeId` smallint     not null,
    `value`  varchar(100) not null
);

CREATE TABLE `unitTestLong`
(
    `id`     int      not null auto_increment primary key,
    `linkId` bigint   not null,
    `typeId` smallint not null,
    `value`  bigint   not null
);


CREATE TABLE `unitTestInstant`
(
    `id`     int          not null auto_increment primary key,
    `linkId` bigint       not null,
    `typeId` smallint     not null,
    `value`  bigint       not null
);

CREATE TABLE `unitTestDiscount`
(
    `id`     int           not null auto_increment primary key,
    `linkId` bigint        not null,
    `typeId` smallint      not null,
    `value`  decimal(3, 1) not null
);

CREATE TABLE `unitTestAmount`
(
    `id`           int            not null auto_increment primary key,
    `linkId`       bigint         not null,
    `typeId`       smallint       not null,
    `active`       tinyint        not null,
    `time`         bigint         not null,
    `currencyCode` varchar(3)     not null,
    `amount`       decimal(20, 4) not null
);