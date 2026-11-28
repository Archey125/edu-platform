package com.example.eduplatform.web;

import com.example.eduplatform.dto.LessonRequest;
import com.example.eduplatform.dto.LessonResponse;
import com.example.eduplatform.dto.ModuleRequest;
import com.example.eduplatform.dto.ModuleResponse;
import com.example.eduplatform.service.ModuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// rest api модулей и уроков /api/modules
@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping("/{id}")
    public ModuleResponse findById(@PathVariable Long id) {
        return moduleService.findById(id);
    }

    @PutMapping("/{id}")
    public ModuleResponse update(@PathVariable Long id, @Valid @RequestBody ModuleRequest request) {
        return moduleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        moduleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // добавить урок в модуль
    @PostMapping("/{id}/lessons")
    public ResponseEntity<LessonResponse> addLesson(@PathVariable Long id,
                                                    @Valid @RequestBody LessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleService.addLesson(id, request));
    }
}
