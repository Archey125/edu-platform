package com.example.eduplatform.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// попытка прохождения теста студентом, хранит набранный балл
// пара quiz_id + student_id уникальна, пересдать нельзя
@Entity
@Table(name = "quiz_submissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_quiz_submissions_quiz_student",
                columnNames = {"quiz_id", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class QuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // набранный балл
    @Column(nullable = false)
    private Integer score;

    // максимум, сумма points всех вопросов на момент прохождения
    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    // зачет от 60% правильных ответов
    @Column(nullable = false)
    private boolean passed;

    @Column(name = "taken_at", nullable = false, updatable = false)
    private LocalDateTime takenAt;

    // что студент выбрал
    @OneToMany(mappedBy = "quizSubmission", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuizAnswer> answers = new HashSet<>();

    public void addAnswer(QuizAnswer answer) {
        answers.add(answer);
        answer.setQuizSubmission(this);
    }

    @PrePersist
    void onCreate() {
        this.takenAt = LocalDateTime.now();
    }
}
