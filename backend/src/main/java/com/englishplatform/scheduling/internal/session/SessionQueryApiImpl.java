package com.englishplatform.scheduling.internal.session;

import com.englishplatform.scheduling.SessionQueryApi;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SessionQueryApiImpl implements SessionQueryApi {

  private final SessionRepository sessions;

  SessionQueryApiImpl(SessionRepository sessions) {
    this.sessions = sessions;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SessionView> find(UUID sessionId) {
    return sessions.findById(sessionId).map(Session::toView);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SessionView> findByEnrollment(UUID enrollmentId) {
    return sessions.findByEnrollmentId(enrollmentId).stream().map(Session::toView).toList();
  }
}
