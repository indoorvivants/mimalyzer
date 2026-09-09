create table jobs(
    id uuid primary key not null,
    code_before text not null,
    code_after text not null,
    created_at timestamp default now()
);

create table compilation_results(
	id uuid primary key not null default gen_random_uuid(),
    job_id uuid not null references jobs(id),
    scala_version_tag varchar(10) not null,
    mima_problems text,
    tasty_mima_problems text,
    before_compilation_errors text,
    after_compilation_errors text,
    state varchar(100) not null,
    processing_step text,
    created_at timestamp default now(),
    worker_id uuid,
    worker_checked_in_at timestamp
);

delete from jobs; delete from compilation_results;

create index idx_compilation_result_job_id on compilation_results(job_id);
create index idx_compilation_result_state on compilation_results(state);
create index idx_compilation_result_worker_id on compilation_results(worker_id);
