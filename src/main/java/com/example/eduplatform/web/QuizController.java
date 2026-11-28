package com.example.eduplatform.web;

import com.example.eduplatform.dto.CreateQuizRequest;
import com.example.eduplatform.dto.QuestionRequest;
import com.example.eduplatform.dto.QuestionResponse;
import com.example.eduplatform.dto.QuizResponse;
import com.example.eduplatform.dto.QuizSubmissionResponse;
import com.example.eduplatform.dto.TakeQuizRequest;
import com.example.eduplatform.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// rest api тестов:
// post /api/modules/{moduleId}/quiz - создать тест для модуля
// get /api/quizzes/{id} - тест с вопросами (без правильных ответов)
// post /api/quizzes/{id}/questions - добавить вопрос
// post /api/quizzes/{id}/take - пройти тест
// get /api/quizzes/{id}/results - результаты, опционально ?studentId=
@RestController
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/api/modules/{moduleId}/quiz")
    public ResponseEntity<QuizResponse> createForModule(@PathVariable Long moduleId,
                                                        @Valid @RequestBody CreateQuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.createForModule(moduleId, request));
    }

    @GetMapping("/api/quizzes/{id}")
    public QuizResponse findById(@PathVariable Long id) {
        return quizService.findById(id);
    }

    @PostMapping("/api/quizzes/{id}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(@PathVariable Long id,
                                                        @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.addQuestion(id, request));
    }

    @PostMapping("/api/quizzes/{id}/take")
    public ResponseEntity<QuizSubmissionResponse> take(@PathVariable Long id,
                                                       @Valid @RequestBody TakeQuizRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.take(id, request));
    }

    @GetMapping("/api/quizzes/{id}/results")
    public List<QuizSubmissionResponse> getResults(@PathVariable Long id,
                                                   @RequestParam(required = false) Long studentId) {
        return quizService.getResults(id, studentId);
    }
}
