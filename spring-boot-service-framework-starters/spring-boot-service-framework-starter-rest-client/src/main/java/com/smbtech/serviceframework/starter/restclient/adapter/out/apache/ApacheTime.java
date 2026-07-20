package com.smbtech.serviceframework.starter.restclient.adapter.out.apache;

import java.time.Duration;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

final class ApacheTime {

    private ApacheTime() {}

    static Timeout timeout(Duration duration) {
        return Timeout.of(duration);
    }

    static TimeValue timeValue(Duration duration) {
        return TimeValue.of(duration);
    }
}
