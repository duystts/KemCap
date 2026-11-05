package com.englishplatform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.englishplatform.catalog.EnrollmentApi;
import com.englishplatform.catalog.internal.course.CoursePackage;
import com.englishplatform.catalog.internal.course.CourseService;
import com.englishplatform.catalog.internal.enrollment.EnrollmentService;
import com.englishplatform.identity.Role;
import com.englishplatform.scheduling.internal.availability.AvailabilityRule;
import com.englishplatform.scheduling.internal.availability.AvailabilityRuleRepository;
import com.englishplatform.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Ma trận quyền T140 (NFR-SEC-01, NFR-SEC-02, FR-003): mọi endpoint nghiệp vụ × mỗi vai trò. Không
 * đăng nhập → 401; sai vai trò → 403; đúng vai trò nhưng không phải dữ liệu của mình → 404. Thân
 * request hợp lệ để kết quả do phân quyền quyết định, không phải do kiểm tra dữ liệu.
 *
 * <p>Endpoint mới chưa khai báo ở đây làm {@link #everyEndpointIsInTheMatrix} đỏ. Lệnh ghi được
 * phép không chạy ở đây (đổi dữ liệu dùng chung của ma trận): IT của từng module và {@link
 * OwnershipHardeningIT} phủ phần này.
 */
class AuthorizationMatrixIT extends ApiTestSupport {

  private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

  private enum Actor {
    ADMIN,
    TEACHER,
    STUDENT,
    OTHER_TEACHER,
    OTHER_STUDENT
  }

  private static final Actor A = Actor.ADMIN;
  private static final Actor T = Actor.TEACHER;
  private static final Actor S = Actor.STUDENT;
  private static final Actor OT = Actor.OTHER_TEACHER;
  private static final Actor OS = Actor.OTHER_STUDENT;

  /** {@code allowed} phải trả 2xx (chỉ chạy với GET); {@code denied}: actor → mã mong đợi. */
  private record Row(
      HttpMethod method,
      String template,
      String path,
      String body,
      Set<Actor> allowed,
      Map<Actor, Integer> denied) {

    Row ok(Actor... actors) {
      allowed.addAll(List.of(actors));
      return this;
    }

    Row forbidden(Actor... actors) {
      for (Actor a : actors) {
        denied.put(a, 403);
      }
      return this;
    }

    Row notFound(Actor... actors) {
      for (Actor a : actors) {
        denied.put(a, 404);
      }
      return this;
    }

    String key() {
      return method + " " + template;
    }
  }

  private final List<Row> rows = new ArrayList<>();

  private Row row(HttpMethod method, String template, String body, Object... ids) {
    String path = template;
    for (Object id : ids) {
      path = path.replaceFirst("\\{[^}]+}", id.toString());
    }
    Row row =
        new Row(
            method, template, path, body, EnumSet.noneOf(Actor.class), new EnumMap<>(Actor.class));
    rows.add(row);
    return row;
  }

  private Row get(String template, Object... ids) {
    return row(HttpMethod.GET, template, null, ids);
  }

  private Row post(String template, String body, Object... ids) {
    return row(HttpMethod.POST, template, body, ids);
  }

  private Row putJson(String template, String body, Object... ids) {
    return row(HttpMethod.PUT, template, body, ids);
  }

  @Autowired CourseService courseService;
  @Autowired EnrollmentService enrollmentService;
  @Autowired EnrollmentApi enrollmentApi;
  @Autowired AvailabilityRuleRepository rules;
  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager txManager;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping mappings;

  private final Map<Actor, TestUser> people = new EnumMap<>(Actor.class);

  private Instant at(int days, int hour) {
    return LocalDate.ofInstant(clock.instant(), VN)
        .plusDays(days)
        .atTime(hour, 0)
        .atZone(VN)
        .toInstant();
  }

  /** Dựng dữ liệu của cặp TEACHER–STUDENT và khai báo ma trận. */
  private void buildMatrix() throws Exception {
    TestUser admin = user(Role.ADMIN);
    TestUser teacher = user(Role.TEACHER);
    TestUser student = user(Role.STUDENT);
    people.put(A, admin);
    people.put(T, teacher);
    people.put(S, student);
    people.put(OT, user(Role.TEACHER));
    people.put(OS, user(Role.STUDENT));
    for (DayOfWeek d : DayOfWeek.values()) {
      rules.save(new AvailabilityRule(teacher.id(), d, LocalTime.of(6, 0), LocalTime.of(22, 0)));
    }
    UUID courseId =
        courseService
            .createCourse("Khóa " + UUID.randomUUID().toString().substring(0, 8), null)
            .getId();
    CoursePackage pkg =
        courseService.createPackage(
            courseId, new CourseService.PackageSpec("Gói", 12, 45, 90, 1_000_000));
    UUID enrollment =
        enrollmentService.create(student.id(), pkg.getId(), teacher.id(), null, admin.id()).getId();
    new TransactionTemplate(txManager)
        .executeWithoutResult(s -> enrollmentApi.activateOnFirstPayment(enrollment));

    String path =
        "{\"title\":\"Lộ trình\",\"milestones\":[{\"title\":\"Chặng\",\"topics\":[{\"title\":\"A\"}]}]}";
    UUID pathId =
        id(
            call(httpPost("/api/v1/students/" + student.id() + "/learning-path"), teacher, path)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    UUID goal =
        id(
            call(
                    httpPost("/api/v1/students/" + student.id() + "/goals"),
                    teacher,
                    "{\"type\":\"IELTS\",\"target\":\"6.5\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    UUID dayOff =
        id(
            call(
                    httpPost("/api/v1/teachers/" + teacher.id() + "/availability/exceptions"),
                    teacher,
                    "{\"date\":\"" + LocalDate.now(VN).plusDays(25) + "\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

    Instant doneStart = at(2, 10);
    UUID done = book(student, enrollment, doneStart);
    clock.set(doneStart.plus(Duration.ofMinutes(50)));
    call(httpPost("/api/v1/sessions/" + done + "/outcome"), teacher, "{\"outcome\":\"COMPLETED\"}")
        .andExpect(status().isOk());
    awaitReport(done);
    call(
            httpPut("/api/v1/sessions/" + done + "/report"),
            teacher,
            "{\"scores\":{\"listening\":3},\"comment\":\"Tốt\",\"assignments\":[{\"title\":\"Bài\"}]}")
        .andExpect(status().isOk());
    UUID assignment =
        UUID.fromString(
            read(
                call(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/assignments?studentId=" + student.id()),
                    student),
                "$[0].id"));
    Instant futureStart = at(3, 10);
    UUID future = book(student, enrollment, futureStart);
    UUID request =
        id(
            call(
                    httpPost("/api/v1/data-subject-requests"),
                    admin,
                    "{\"studentId\":\""
                        + student.id()
                        + "\",\"requesterName\":\"A\",\"type\":\"ACCESS\",\"receivedOn\":\""
                        + LocalDate.now(VN)
                        + "\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    UUID notification = awaitNotification(student);

    UUID t = teacher.id();
    UUID s = student.id();
    String today = LocalDate.now(VN).toString();

    // identity
    get("/api/v1/me").ok(A, T, S, OT, OS);
    row(HttpMethod.PATCH, "/api/v1/me", "{}").ok(A, T, S, OT, OS);
    post("/api/v1/me/consents", "{\"policyVersion\":\"x\"}").ok(A, T, S, OT, OS);
    get("/api/v1/users").ok(A).forbidden(T, S, OT, OS);
    get("/api/v1/users/{id}", s).ok(A).forbidden(T, S, OT, OS);
    get("/api/v1/users/{id}/policy-consents", s).ok(A).forbidden(T, S, OT, OS);
    get("/api/v1/users/{userId}/notifications", s).ok(A).forbidden(T, S, OT, OS);
    post("/api/v1/users", "{\"email\":\"m@test.local\",\"fullName\":\"M\",\"role\":\"TEACHER\"}")
        .ok(A)
        .forbidden(T, S, OT, OS);
    row(HttpMethod.PATCH, "/api/v1/users/{id}/status", "{\"status\":\"INACTIVE\"}", s)
        .ok(A)
        .forbidden(T, S, OT, OS);
    post("/api/v1/users/{id}/resend-invite", null, s).ok(A).forbidden(T, S, OT, OS);
    String consent =
        "{\"guardianName\":\"M\",\"relationship\":\"Mẹ\",\"guardianContact\":\"09\","
            + "\"consentedOn\":\"2026-01-15\"}";
    get("/api/v1/students/{studentId}/guardian-consent", s)
        .ok()
        .notFound(A)
        .forbidden(T, S, OT, OS);
    putJson("/api/v1/students/{studentId}/guardian-consent", consent, s)
        .ok(A)
        .forbidden(T, S, OT, OS);
    String dsr =
        "{\"studentId\":\""
            + s
            + "\",\"requesterName\":\"A\",\"type\":\"ACCESS\",\"receivedOn\":\""
            + today
            + "\"}";
    get("/api/v1/data-subject-requests").ok(A).forbidden(T, S, OT, OS);
    post("/api/v1/data-subject-requests", dsr).ok(A).forbidden(T, S, OT, OS);
    post("/api/v1/data-subject-requests/{id}/reject", "{\"resolutionNote\":\"x\"}", request)
        .ok(A)
        .forbidden(T, S, OT, OS);
    post("/api/v1/data-subject-requests/{id}/complete-access", null, request)
        .ok(A)
        .forbidden(T, S, OT, OS);
    post("/api/v1/data-subject-requests/{id}/anonymize", "{\"confirm\":true}", request)
        .ok(A)
        .forbidden(T, S, OT, OS);

    // catalog
    get("/api/v1/courses").ok(A, T, S, OT, OS);
    post("/api/v1/courses", "{\"name\":\"K\"}").ok(A).forbidden(T, S, OT, OS);
    putJson("/api/v1/courses/{id}", "{\"name\":\"K\",\"status\":\"ACTIVE\"}", courseId)
        .ok(A)
        .forbidden(T, S, OT, OS);
    get("/api/v1/packages").ok(A, T, S, OT, OS);
    String packageBody =
        "{\"courseId\":\""
            + courseId
            + "\",\"name\":\"G\",\"sessionCount\":6,\"sessionMinutes\":45,\"validityDays\":90,"
            + "\"priceVnd\":1000000}";
    post("/api/v1/packages", packageBody).ok(A).forbidden(T, S, OT, OS);
    putJson(
            "/api/v1/packages/{id}",
            "{\"name\":\"G\",\"sessionCount\":6,\"sessionMinutes\":45,\"validityDays\":90,"
                + "\"priceVnd\":1000000,\"status\":\"ACTIVE\"}",
            pkg.getId())
        .ok(A)
        .forbidden(T, S, OT, OS);
    post(
            "/api/v1/enrollments",
            "{\"studentId\":\""
                + s
                + "\",\"packageId\":\""
                + pkg.getId()
                + "\",\"teacherId\":\""
                + t
                + "\"}")
        .ok(A)
        .forbidden(T, S, OT, OS);
    get("/api/v1/enrollments").ok(A, T, S, OT, OS);
    get("/api/v1/enrollments/{id}", enrollment).ok(A, T, S).notFound(OT, OS);
    get("/api/v1/enrollments/{id}/teacher-history", enrollment).ok(A).forbidden(T, S, OT, OS);
    putJson(
            "/api/v1/enrollments/{id}/teacher",
            "{\"teacherId\":\"" + people.get(OT).id() + "\",\"reason\":\"x\"}",
            enrollment)
        .ok(A)
        .forbidden(T, S, OT, OS);
    post(
            "/api/v1/enrollments/{id}/extend",
            "{\"newExpiresAt\":\"" + clock.instant().plus(Duration.ofDays(30)) + "\"}",
            enrollment)
        .ok(A)
        .forbidden(T, S, OT, OS);
    post("/api/v1/enrollments/{id}/cancel", "{\"reason\":\"x\"}", enrollment)
        .ok(A)
        .forbidden(T, S, OT, OS);
    get("/api/v1/teachers/{id}/profile", t).ok(A, T).notFound(OT).forbidden(S, OS);
    putJson("/api/v1/teachers/{id}/profile", "{\"bio\":\"x\"}", t)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    get("/api/v1/students/{id}/profile", s).ok(A, T, S).notFound(OT, OS);
    putJson("/api/v1/students/{id}/profile", "{\"notes\":\"x\"}", s).ok(A).forbidden(T, S, OT, OS);
    get("/api/v1/students").ok(A, T, OT).forbidden(S, OS);

    // billing
    get("/api/v1/enrollments/{enrollmentId}/payments", enrollment)
        .ok(A, S)
        .forbidden(T, OT)
        .notFound(OS);
    post(
            "/api/v1/enrollments/{enrollmentId}/payments",
            "{\"amountVnd\":1000,\"paidAt\":\"" + today + "\",\"method\":\"CASH\"}",
            enrollment)
        .ok(A)
        .forbidden(T, S, OT, OS);
    post(
            "/api/v1/enrollments/{enrollmentId}/payments/adjustments",
            "{\"amountVnd\":-1000,\"note\":\"x\"}",
            enrollment)
        .ok(A)
        .forbidden(T, S, OT, OS);
    get("/api/v1/receivables").ok(A).forbidden(T, S, OT, OS);
    get("/api/v1/reports/revenue").ok(A).forbidden(T, S, OT, OS);

    // scheduling
    String availabilityBase = "/api/v1/teachers/{teacherId}/availability";
    get(availabilityBase, t).ok(A, T).notFound(OT).forbidden(S, OS);
    putJson(availabilityBase, "[]", t).ok(A, T).notFound(OT).forbidden(S, OS);
    get(availabilityBase + "/exceptions", t).ok(A, T).notFound(OT).forbidden(S, OS);
    post(
            availabilityBase + "/exceptions",
            "{\"date\":\"" + LocalDate.now(VN).plusDays(26) + "\"}",
            t)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    row(HttpMethod.DELETE, availabilityBase + "/exceptions/{exceptionId}", null, t, dayOff)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    get("/api/v1/enrollments/{enrollmentId}/slots", enrollment)
        .ok(A, S)
        .forbidden(T, OT)
        .notFound(OS);
    post(
            "/api/v1/sessions",
            "{\"enrollmentId\":\"" + enrollment + "\",\"startsAt\":\"" + at(4, 10) + "\"}")
        .ok(A, S)
        .forbidden(T, OT)
        .notFound(OS);
    get("/api/v1/sessions").ok(A, T, S, OT, OS);
    get("/api/v1/sessions/{id}", future).ok(A, T, S).notFound(OT, OS);
    get("/api/v1/sessions/{id}/cancel-preview", future).ok(A, S).forbidden(T, OT).notFound(OS);
    post("/api/v1/sessions/{id}/cancel", "{\"reason\":\"x\"}", future).ok(A, T, S).notFound(OT, OS);
    post("/api/v1/sessions/{id}/reschedule", "{\"newStartsAt\":\"" + at(5, 10) + "\"}", future)
        .ok(A, S)
        .forbidden(T, OT)
        .notFound(OS);
    post("/api/v1/sessions/{id}/outcome", "{\"outcome\":\"COMPLETED\"}", future)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    get("/api/v1/sessions/{id}/outcome-history", done).ok(A).forbidden(T, S, OT, OS);
    post("/api/v1/sessions/{id}/interruption-decision", "{\"decision\":\"COUNT\"}", done)
        .ok(A)
        .forbidden(T, S, OT, OS);
    post("/api/v1/sessions/{id}/join", null, future).ok(T, S).forbidden(A).notFound(OT, OS);
    get("/api/v1/reports/teacher-sessions").ok(A).forbidden(T, S, OT, OS);

    // learning
    get("/api/v1/students/{studentId}/assessments", s).ok(A, T, S).notFound(OT, OS);
    post(
            "/api/v1/students/{studentId}/assessments",
            "{\"type\":\"PLACEMENT\",\"listening\":\"B1\",\"speaking\":\"A2\",\"reading\":\"B1\","
                + "\"writing\":\"A2\",\"grammarVocab\":\"B1\"}",
            s)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    get("/api/v1/students/{studentId}/proficiency", s).ok(A, T, S).notFound(OT, OS);
    get("/api/v1/students/{studentId}/goals", s).ok(A, T, S).notFound(OT, OS);
    post("/api/v1/students/{studentId}/goals", "{\"type\":\"IELTS\",\"target\":\"7\"}", s)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    putJson("/api/v1/students/{studentId}/goals/{goalId}", "{\"status\":\"DROPPED\"}", s, goal)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    get("/api/v1/students/{studentId}/learning-path", s).ok(A, T, S).notFound(OT, OS);
    post("/api/v1/students/{studentId}/learning-path", path, s)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    putJson(
            "/api/v1/learning-paths/{pathId}",
            path.replace("}]}]}", "}]}],\"reason\":\"x\",\"version\":0}"),
            pathId)
        .ok(A, T)
        .notFound(OT)
        .forbidden(S, OS);
    get("/api/v1/learning-paths/{pathId}/revisions", pathId).ok(A, T).notFound(OT).forbidden(S, OS);
    get("/api/v1/session-reports").ok(A, T, OT).forbidden(S, OS);
    get("/api/v1/sessions/{sessionId}/report", done).ok(A, T, S).notFound(OT, OS);
    putJson(
            "/api/v1/sessions/{sessionId}/report",
            "{\"scores\":{\"listening\":3},\"comment\":\"x\"}",
            done)
        .ok(T)
        .notFound(OT)
        .forbidden(A, S, OS);
    get("/api/v1/assignments").ok(A, T, S, OT, OS);
    putJson("/api/v1/assignments/{id}/submission", "{\"textAnswer\":\"x\"}", assignment)
        .ok(S)
        .notFound(OS)
        .forbidden(A, T, OT);
    post("/api/v1/assignments/{id}/grade", "{\"score\":5}", assignment)
        .ok(T)
        .notFound(OT)
        .forbidden(A, S, OS);
    get("/api/v1/students/{studentId}/progress", s).ok(A, T, S).notFound(OT, OS);
    post("/api/v1/sessions/{sessionId}/feedback", "{\"stars\":4}", done)
        .ok(S)
        .notFound(OS)
        .forbidden(A, T, OT);
    get("/api/v1/session-feedback/mine").ok(S, OS).forbidden(A, T, OT);
    get("/api/v1/sessions/{sessionId}/feedback", done).ok().notFound(A).forbidden(T, S, OT, OS);

    // notification
    get("/api/v1/notifications").ok(A, T, S, OT, OS);
    post("/api/v1/notifications/{id}/read", null, notification).ok(S).notFound(A, T, OT, OS);
    post("/api/v1/notifications/read-all", null).ok(A, T, S, OT, OS);
  }

  private static MockHttpServletRequestBuilder httpPost(String path) {
    return request(HttpMethod.POST, path);
  }

  private static MockHttpServletRequestBuilder httpPut(String path) {
    return request(HttpMethod.PUT, path);
  }

  private static UUID id(String json) {
    return UUID.fromString(JsonPath.read(json, "$.id"));
  }

  private UUID book(TestUser student, UUID enrollment, Instant startsAt) throws Exception {
    return id(
        call(
                httpPost("/api/v1/sessions"),
                student,
                "{\"enrollmentId\":\"" + enrollment + "\",\"startsAt\":\"" + startsAt + "\"}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private void awaitReport(UUID sessionId) {
    org.awaitility.Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .until(
            () ->
                jdbc.queryForObject(
                        "select count(*) from session_report where session_id = ?",
                        Long.class,
                        sessionId)
                    == 1);
  }

  private UUID awaitNotification(TestUser student) {
    org.awaitility.Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .until(
            () ->
                jdbc.queryForObject(
                        "select count(*) from notification where user_id = ?",
                        Long.class,
                        student.id())
                    > 0);
    return jdbc.queryForList(
            "select id from notification where user_id = ?", UUID.class, student.id())
        .get(0);
  }

  private MockHttpServletRequestBuilder builder(Row row) {
    MockHttpServletRequestBuilder b = request(row.method(), row.path());
    if (row.body() != null) {
      b.contentType(MediaType.APPLICATION_JSON).content(row.body());
    }
    return b;
  }

  private Set<String> declared() throws Exception {
    buildMatrix();
    Set<String> keys = new TreeSet<>();
    rows.forEach(r -> keys.add(r.key()));
    return keys;
  }

  /** Quên khai báo endpoint mới vào ma trận thì đỏ, không âm thầm bỏ sót. */
  @Test
  void everyEndpointIsInTheMatrix() throws Exception {
    Set<String> actual = new TreeSet<>();
    mappings
        .getHandlerMethods()
        .keySet()
        .forEach(
            info ->
                info.getPatternValues().stream()
                    .filter(p -> p.startsWith("/api/v1/") && !p.startsWith("/api/v1/auth/"))
                    .forEach(
                        p -> {
                          assertThat(info.getMethodsCondition().getMethods())
                              .as("mapping phải khai method: " + p)
                              .isNotEmpty();
                          info.getMethodsCondition()
                              .getMethods()
                              .forEach(m -> actual.add(m.name() + " " + p));
                        }));

    assertThat(declared()).containsExactlyInAnyOrderElementsOf(actual);
  }

  @Test
  void everyRowCoversEveryActor() throws Exception {
    buildMatrix();
    List<String> incomplete = new ArrayList<>();
    for (Row row : rows) {
      Set<Actor> covered = EnumSet.noneOf(Actor.class);
      covered.addAll(row.allowed());
      covered.addAll(row.denied().keySet());
      if (covered.size() != Actor.values().length) {
        incomplete.add(row.key());
      }
    }
    assertThat(incomplete).isEmpty();
  }

  @Test
  void anonymousGets401Everywhere() throws Exception {
    buildMatrix();
    List<String> wrong = new ArrayList<>();
    for (Row row : rows) {
      int code = mvc.perform(builder(row)).andReturn().getResponse().getStatus();
      if (code != 401) {
        wrong.add(row.key() + " -> " + code);
      }
    }
    assertThat(wrong).isEmpty();
  }

  @Test
  void deniedActorsGetExpectedStatus() throws Exception {
    buildMatrix();
    List<String> wrong = new ArrayList<>();
    for (Row row : rows) {
      for (Map.Entry<Actor, Integer> e : row.denied().entrySet()) {
        int code =
            call(builder(row), people.get(e.getKey()), null).andReturn().getResponse().getStatus();
        if (code != e.getValue()) {
          wrong.add(row.key() + " as " + e.getKey() + ": mong " + e.getValue() + ", được " + code);
        }
      }
    }
    assertThat(wrong).isEmpty();
  }

  @Test
  void allowedActorsCanReadWithStatus2xx() throws Exception {
    buildMatrix();
    List<String> wrong = new ArrayList<>();
    for (Row row : rows) {
      if (row.method() != HttpMethod.GET) {
        continue;
      }
      for (Actor actor : row.allowed()) {
        int code =
            call(builder(row), people.get(actor), null).andReturn().getResponse().getStatus();
        if (code / 100 != 2) {
          wrong.add(row.key() + " as " + actor + ": được " + code);
        }
      }
    }
    assertThat(wrong).isEmpty();
  }

  /** Danh sách trả 200 cho mọi người nhưng chỉ dữ liệu của mình. */
  @Test
  void listsAreScopedToCaller() throws Exception {
    buildMatrix();
    for (String path : List.of("/api/v1/enrollments", "/api/v1/sessions", "/api/v1/assignments")) {
      for (Actor outsider : List.of(OT, OS)) {
        String body =
            call(request(HttpMethod.GET, path), people.get(outsider))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(JsonPath.<List<?>>read(body, "$")).as(path + " as " + outsider).isEmpty();
      }
    }
  }

  /** Tài liệu API không mở công khai ngoài môi trường dev. */
  @Test
  void apiDocsAreDisabledOutsideLocalProfile() throws Exception {
    mvc.perform(request(HttpMethod.GET, "/v3/api-docs")).andExpect(status().isNotFound());
    mvc.perform(request(HttpMethod.GET, "/swagger-ui/index.html")).andExpect(status().isNotFound());
  }
}
