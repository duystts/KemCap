package com.englishplatform.scheduling.internal.session;

import com.englishplatform.scheduling.internal.SchedulingAccess;
import com.englishplatform.shared.security.CurrentUser;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Tìm buổi theo quyền: admin mọi buổi; giáo viên/học viên chỉ buổi của mình — còn lại 404. */
@Component
public class SessionLookup {

  private final SessionRepository sessions;

  SessionLookup(SessionRepository sessions) {
    this.sessions = sessions;
  }

  public Session visible(CurrentUser actor, UUID sessionId) {
    Session session = sessions.findById(sessionId).orElseThrow(SchedulingAccess::notFound);
    if (!SchedulingAccess.isAdmin(actor) && !session.isParticipant(actor.id())) {
      throw SchedulingAccess.notFound();
    }
    return session;
  }
}
