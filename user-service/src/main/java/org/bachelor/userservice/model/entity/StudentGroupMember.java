package org.bachelor.userservice.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "student_group_member")
public class StudentGroupMember {

    @Id
    private Long bookNumberId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "book_number_id")
    private BookNumber bookNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_group_id", nullable = false)
    private StudentGroup studentGroup;
}
