package com.hmall.activity.domain;

import java.time.LocalDate;

public record DailyActivityStats(LocalDate date, ActivityStats stats) {}
