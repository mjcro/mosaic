CREATE TABLE `commonString`
(
    `id`     int          not null auto_increment primary key,
    `linkId` bigint       not null,
    `typeId` smallint     not null,
    `value`  varchar(100) not null
)