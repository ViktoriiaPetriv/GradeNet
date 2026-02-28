package org.bachelor.userservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "book_number")
public class BookNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false, unique = true, length = 20)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @CreationTimestamp
    @Column(name = "reg_start_date", nullable = false, updatable = false)
    private Instant regStartDate;

    @Column(name = "reg_end_date")
    private Instant regEndDate;

    @Column(name = "handed_date")
    private Instant handedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookNumberStatus status;

    @Column(name = "specialty_id", nullable = false)
    private Long specialtyId;
}
