package org.bachelor.gradeservice.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AcademicCalendar {

    public int getCurrentSemester() {
        int month = LocalDate.now().getMonthValue();
        return (month >= 9 || month == 1) ? 1 : 2;
    }

    public String getCurrentAcademicYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        if (month >= 9) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }
}
