package org.example.collectfocep.services.interfaces;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DateTimeService {
    LocalDateTime toStartOfDay(LocalDate date);
    LocalDateTime toEndOfDay(LocalDate date);
    LocalDate getCurrentDate();
    LocalDateTime getCurrentDateTime();
}
