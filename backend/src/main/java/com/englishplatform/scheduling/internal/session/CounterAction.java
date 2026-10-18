package com.englishplatform.scheduling.internal.session;

import com.englishplatform.catalog.EnrollmentApi;
import java.util.UUID;

/** Ảnh hưởng của một lần chuyển trạng thái buổi tới bộ đếm ghi danh (research R5). */
public enum CounterAction {
  RESERVE,
  RELEASE,
  CONSUME,
  CONSUME_RELEASED,
  UNCONSUME;

  /** Gọi trong transaction đang đổi trạng thái session ({@code EnrollmentApi} là MANDATORY). */
  public void apply(EnrollmentApi api, UUID enrollmentId) {
    switch (this) {
      case RESERVE -> api.reserve(enrollmentId);
      case RELEASE -> api.release(enrollmentId);
      case CONSUME -> api.consume(enrollmentId);
      case CONSUME_RELEASED -> api.consumeReleased(enrollmentId);
      case UNCONSUME -> api.unconsume(enrollmentId);
    }
  }
}
