create table specialty (
    id bigserial primary key,
    code varchar(10) not null unique,
    name_ua varchar(255) not null unique,
    name_en varchar(255) not null unique,
    study_program_ua varchar(255) not null,
    study_program_en varchar(255) not null,
    edu_program_ua varchar(255) not null,
    edu_program_en varchar(255) not null,
    org_id bigint not null,
    degree varchar(255) not null,
    edu_type varchar(255) not null,
    start_date timestamp not null,
    end_date timestamp,
    constraint fk_specialty_org foreign key (org_id) references org(id) on delete cascade
);
