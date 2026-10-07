package com.examly.springapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The clock used for the trip rules (a trip cannot end before it starts, it cannot be cancelled in the last
 * 24 hours). Trip dates and times are entered without a time zone, so they are read in the zone of this clock:
 * the server's own zone, or app.time-zone (e.g. Asia/Kolkata) when set.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone:}") String timeZone) {
        ZoneId zone = timeZone == null || timeZone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(timeZone.trim());
        return Clock.system(zone);
    }
}
