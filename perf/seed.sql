-- Dữ liệu quy mô thiết kế cho T141: 1.000 học viên, 50 giáo viên (NFR-PERF-01/02).
-- Mỗi giáo viên: 20 quy tắc rảnh/tuần, 60 buổi BOOKED trong 30 ngày tới, 180 buổi đã học.
-- Mỗi giáo viên có thêm 1 buổi mở phòng trong giờ tới cho bài `join`.
-- Chỉ chạy trên CSDL thử tải: mật khẩu mọi tài khoản là chuỗi công khai trong k6.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

CREATE TEMP TABLE perf_hash AS SELECT crypt('Perf@1234', gen_salt('bf', 10)) AS h;

CREATE FUNCTION pg_temp.perf_id(k TEXT) RETURNS UUID LANGUAGE SQL IMMUTABLE AS
$$ SELECT md5('perf-' || k)::uuid $$;

INSERT INTO user_account (id, email, password_hash, full_name, role, date_of_birth, status,
                          created_at, updated_at)
SELECT pg_temp.perf_id('t' || i), format('perf-t%s@test.local', i), (SELECT h FROM perf_hash),
       'Giáo viên ' || i, 'TEACHER', NULL, 'ACTIVE', now(), now()
FROM generate_series(1, 50) i;

INSERT INTO user_account (id, email, password_hash, full_name, role, date_of_birth, status,
                          created_at, updated_at)
SELECT pg_temp.perf_id('s' || i), format('perf-s%s@test.local', i), (SELECT h FROM perf_hash),
       'Học viên ' || i, 'STUDENT', DATE '2000-01-01', 'ACTIVE', now(), now()
FROM generate_series(1, 1000) i;

INSERT INTO policy_consent (id, user_id, policy_version, accepted_at, created_at, updated_at)
SELECT gen_random_uuid(), id, :'policy_version', now(), now(), now()
FROM user_account
WHERE email LIKE 'perf-%';

INSERT INTO teacher_profile (user_id, created_at, updated_at)
SELECT pg_temp.perf_id('t' || i), now(), now()
FROM generate_series(1, 50) i;

INSERT INTO student_profile (user_id, created_at, updated_at)
SELECT pg_temp.perf_id('s' || i), now(), now()
FROM generate_series(1, 1000) i;

INSERT INTO course (id, name, status, created_at, updated_at)
VALUES (pg_temp.perf_id('course'), 'Khóa thử tải', 'ACTIVE', now(), now());

INSERT INTO package (id, course_id, name, session_count, session_minutes, validity_days, price_vnd,
                     status, created_at, updated_at)
VALUES (pg_temp.perf_id('package'), pg_temp.perf_id('course'), 'Gói thử tải', 400, 60, 365, 0,
        'ACTIVE', now(), now());

-- Học viên i học giáo viên ((i - 1) % 50) + 1, nên mỗi giáo viên có 20 học viên
INSERT INTO enrollment (id, student_id, package_id, teacher_id, sessions_total, session_minutes,
                        validity_days, price_vnd, activated_at, expires_at, status, created_at,
                        updated_at)
SELECT pg_temp.perf_id('e' || i), pg_temp.perf_id('s' || i), pg_temp.perf_id('package'),
       pg_temp.perf_id('t' || ((i - 1) % 50 + 1)), 400, 60, 365, 0, now(), now() + INTERVAL '365 days',
       'ACTIVE', now(), now()
FROM generate_series(1, 1000) i;

-- 20 quy tắc/tuần: 3 khung/ngày trong 6 ngày và 2 khung ngày thứ 7
INSERT INTO availability_rule (id, teacher_id, day_of_week, start_time, end_time, created_at,
                               updated_at)
SELECT gen_random_uuid(), pg_temp.perf_id('t' || t), d, w.s, w.e, now(), now()
FROM generate_series(1, 50) t
     CROSS JOIN generate_series(1, 7) d
     CROSS JOIN (VALUES (TIME '08:00', TIME '11:00'),
                        (TIME '13:00', TIME '17:00'),
                        (TIME '18:00', TIME '22:00')) AS w(s, e)
WHERE d < 7 OR w.s <> TIME '13:00';

-- buổi của giáo viên t xoay vòng trong 20 học viên của họ: học viên t + 50 * k
CREATE FUNCTION pg_temp.perf_student(t INT, n INT) RETURNS INT LANGUAGE SQL IMMUTABLE AS
$$ SELECT t + 50 * (n % 20) $$;

-- 60 buổi sắp tới: 2 buổi mỗi ngày, từ ngày mai
INSERT INTO session (id, enrollment_id, teacher_id, student_id, starts_at, ends_at, status,
                     booked_by, created_at, updated_at)
SELECT gen_random_uuid(),
       pg_temp.perf_id('e' || pg_temp.perf_student(t, n)),
       pg_temp.perf_id('t' || t),
       pg_temp.perf_id('s' || pg_temp.perf_student(t, n)),
       date_trunc('day', now()) + (d || ' days')::INTERVAL + (h || ' hours')::INTERVAL,
       date_trunc('day', now()) + (d || ' days')::INTERVAL + ((h + 1) || ' hours')::INTERVAL,
       'BOOKED', pg_temp.perf_id('s' || pg_temp.perf_student(t, n)), now(), now()
FROM generate_series(1, 50) t
     CROSS JOIN generate_series(1, 30) d
     CROSS JOIN (VALUES (0, 2), (1, 12)) AS x(k, h)
     CROSS JOIN LATERAL (SELECT d * 2 + k AS n) m;

-- 180 buổi đã học: mỗi ngày một buổi, tới hôm kia
INSERT INTO session (id, enrollment_id, teacher_id, student_id, starts_at, ends_at, status,
                     booked_by, outcome_by, outcome_at, created_at, updated_at)
SELECT gen_random_uuid(),
       pg_temp.perf_id('e' || pg_temp.perf_student(t, d)),
       pg_temp.perf_id('t' || t),
       pg_temp.perf_id('s' || pg_temp.perf_student(t, d)),
       date_trunc('day', now()) - (d + 1 || ' days')::INTERVAL + INTERVAL '3 hours',
       date_trunc('day', now()) - (d + 1 || ' days')::INTERVAL + INTERVAL '4 hours',
       'COMPLETED', pg_temp.perf_id('s' || pg_temp.perf_student(t, d)),
       pg_temp.perf_id('t' || t), now(), now(), now()
FROM generate_series(1, 50) t
     CROSS JOIN generate_series(1, 180) d;

-- Phòng đã mở: học viên t với giáo viên t, bắt đầu sau 5 phút
INSERT INTO session (id, enrollment_id, teacher_id, student_id, starts_at, ends_at, status,
                     booked_by, created_at, updated_at)
SELECT gen_random_uuid(), pg_temp.perf_id('e' || t), pg_temp.perf_id('t' || t),
       pg_temp.perf_id('s' || t), now() + INTERVAL '5 minutes', now() + INTERVAL '65 minutes',
       'BOOKED', pg_temp.perf_id('s' || t), now(), now()
FROM generate_series(1, 50) t
ON CONFLICT DO NOTHING;

UPDATE enrollment e
SET sessions_reserved = c.booked,
    sessions_consumed = c.done
FROM (SELECT enrollment_id,
             count(*) FILTER (WHERE status = 'BOOKED')    AS booked,
             count(*) FILTER (WHERE status = 'COMPLETED') AS done
      FROM session
      GROUP BY enrollment_id) c
WHERE e.id = c.enrollment_id;

COMMIT;

ANALYZE;

SELECT (SELECT count(*) FROM user_account WHERE role = 'STUDENT' AND email LIKE 'perf-%') AS students,
       (SELECT count(*) FROM user_account WHERE role = 'TEACHER' AND email LIKE 'perf-%') AS teachers,
       (SELECT count(*) FROM availability_rule)                                           AS rules,
       (SELECT count(*) FROM session)                                                     AS sessions,
       (SELECT count(*) FROM session WHERE starts_at BETWEEN now() AND now() + INTERVAL '10 minutes') AS open_rooms;
