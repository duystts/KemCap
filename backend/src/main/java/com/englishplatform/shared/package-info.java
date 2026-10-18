/**
 * Hạ tầng dùng chung (security, xử lý lỗi, clock, persistence). Module mở: mọi module được dùng,
 * nhưng module này không chứa nghiệp vụ và không phụ thuộc module nghiệp vụ nào.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.englishplatform.shared;

import org.springframework.modulith.ApplicationModule;
