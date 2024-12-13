package com.middleware.leave_approval_system.Util;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class HolidaysUtil {
    public static List<LocalDate> getNationalHolidays(int year) {
        return Arrays.asList(
                LocalDate.of(year,1,26),
                LocalDate.of(year,8,15),
                LocalDate.of(year,10,02),
                LocalDate.of(year,12,25)
        );
    }
}
