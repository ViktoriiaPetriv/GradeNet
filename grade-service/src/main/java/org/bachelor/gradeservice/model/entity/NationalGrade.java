package org.bachelor.gradeservice.model.entity;

public enum NationalGrade {
    FIVE, FOUR, THREE, TWO, PASSED, NOT_PASSED;

    public String getDisplayValue() {
        return switch (this) {
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case PASSED -> "passed";
            case NOT_PASSED -> "not_passed";
        };
    }
}
