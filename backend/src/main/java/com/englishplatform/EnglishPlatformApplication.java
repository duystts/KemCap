package com.englishplatform;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EnglishPlatformApplication {

  public static void main(String[] args) {
    // Thời gian lưu UTC (constitution); tránh múi giờ máy (vd. Asia/Saigon) lọt vào kết nối DB
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(EnglishPlatformApplication.class, args);
  }
}
