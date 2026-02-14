create table org (
    id serial primary key,
    name varchar(255) not null unique,
    type varchar(255) not null,
    parent_id int,
    constraint fk_org_parent foreign key (parent_id) references org(id) on delete set null
);