package com.englishplatform.learning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** T152 (U4 R6): đọc lộ trình không phát thêm một query cho mỗi chặng. */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class LearningPathQueryCountIT extends LearningFlowSupport {

  @Autowired EntityManagerFactory emf;

  private void createPath(Setup s, int milestones) throws Exception {
    String body =
        IntStream.range(0, milestones)
            .mapToObj(
                i ->
                    "{\"title\":\"Chặng "
                        + i
                        + "\",\"topics\":[{\"title\":\"A"
                        + i
                        + "\"},{\"title\":\"B"
                        + i
                        + "\"}]}")
            .collect(Collectors.joining(",", "{\"title\":\"Lộ trình\",\"milestones\":[", "]}"));
    call(post("/api/v1/students/" + s.student().id() + "/learning-path"), s.teacher(), body)
        .andExpect(status().isCreated());
  }

  private long statementsToRead(Setup s, int milestones) throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    // lượt đầu làm nóng cache bảo mật, chỉ đếm lượt sau
    call(get("/api/v1/students/" + s.student().id() + "/learning-path"), s.teacher());
    stats.clear();
    call(get("/api/v1/students/" + s.student().id() + "/learning-path"), s.teacher())
        .andExpect(jsonPath("$.milestones.length()").value(milestones))
        .andExpect(jsonPath("$.milestones[" + (milestones - 1) + "].topics.length()").value(2));
    return stats.getPrepareStatementCount();
  }

  @Test
  void readingPathCostsTheSameForOneOrManyMilestones() throws Exception {
    Setup small = setup();
    Setup large = setup();
    createPath(small, 1);
    createPath(large, 12);
    Thread.sleep(500); // để listener bất đồng bộ sau khi tạo lộ trình chạy xong

    long one = statementsToRead(small, 1);
    long twelve = statementsToRead(large, 12);
    assertThat(twelve).as("query khi có 12 chặng so với 1 chặng (1 chặng: %d)", one).isEqualTo(one);
  }
}
