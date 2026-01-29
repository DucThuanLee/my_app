create extension if not exists pgcrypto;
create table if not exists notifications (
    id uuid primary key,
    type varchar(40) not null,
    channel varchar(20) not null,
    recipient varchar(320) not null,
    order_id uuid null,
    status varchar(20) not null,
    attempts int not null,
    next_attempt_at timestamp not null,
    last_error varchar(500),
    created_at timestamp not null,
    sent_at timestamp null,
    payload text
    );

create index if not exists idx_notifications_status_next on notifications(status, next_attempt_at);
create index if not exists idx_notifications_order on notifications(order_id);