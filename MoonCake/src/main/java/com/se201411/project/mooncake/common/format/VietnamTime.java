package com.se201411.project.mooncake.common.format;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class VietnamTime {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private VietnamTime() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(VIETNAM));
    }

    public static String dateTime(LocalDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        return utcTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DATE_TIME);
    }

    public static String date(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE);
    }

    public static String date(LocalDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        return utcTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DATE);
    }
}
