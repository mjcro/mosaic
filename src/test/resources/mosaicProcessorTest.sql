CREATE TABLE `dataProviderUnitString`
(
    `id`     int          not null auto_increment primary key,
    `linkId` bigint       not null,
    `typeId` smallint     not null,
    `value`  varchar(100) not null
);

CREATE TABLE `dataProviderUnitInstant`
(
    `id`     int          not null auto_increment primary key,
    `linkId` bigint       not null,
    `typeId` smallint     not null,
    `value`  bigint       not null
);

CREATE TABLE `dataProviderUnitAmount`
(
    `id`           int            not null auto_increment primary key,
    `linkId`       bigint         not null,
    `typeId`       smallint       not null,
    `active`       tinyint        not null,
    `time`         bigint         not null,
    `currencyCode` varchar(3)     not null,
    `amount`       decimal(20, 4) not null
);