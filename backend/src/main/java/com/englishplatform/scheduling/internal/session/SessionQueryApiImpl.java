package com.englishplatform.scheduling.internal.session;

import com.englishplatform.scheduling.SessionQueryApi;
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
}
