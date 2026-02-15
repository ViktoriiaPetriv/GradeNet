create table org (
    id bigserial primary key,
    name varchar(255) not null unique,
    type varchar(255) not null,
    parent_id bigint,
    constraint fk_org_parent foreign key (parent_id) references org(id) on delete set null
);