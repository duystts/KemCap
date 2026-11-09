-- Module scheduling: lưu quyết định của giáo vụ cho buổi Gián đoạn (T147, FR-014). Trước đây "xếp
-- bù" không được lưu nên buổi đã xếp bù vẫn bị "tính là đã học" được. Buổi Gián đoạn cũ để NULL vì
-- không biết đã xếp bù hay chưa.

ALTER TABLE session
  ADD COLUMN interruption_decision VARCHAR(10)
    CONSTRAINT session_interruption_decision_chk CHECK (interruption_decision IN ('COUNT', 'MAKE_UP'));
