package org.bachelor.integrationservice.model;

import lombok.Data;

@Data
public class ParsedDiscipline {
    private final String name;
    private final int totalHours;
    private final int ectsCredits;

    public static ParsedDiscipline of(String name, int totalHours) {
        int ects = Math.max(1, (int) Math.round(totalHours / 30.0));
        return new ParsedDiscipline(name, totalHours, ects);
    }
}
