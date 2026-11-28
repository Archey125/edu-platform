package com.example.eduplatform.service;

import com.example.eduplatform.dto.TagRequest;
import com.example.eduplatform.dto.TagResponse;
import com.example.eduplatform.entity.Tag;
import com.example.eduplatform.exception.ConflictException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// теги курсов. на связи course <-> tag каскада нет, тег который юзается в курсах удалить нельзя
@Service
@Transactional(readOnly = true)
public class TagService {

    private static final Logger log = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<TagResponse> findAll() {
        return tagRepository.findAll().stream().map(TagService::toResponse).toList();
    }

    public TagResponse findById(Long id) {
        return toResponse(getTag(id));
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        if (tagRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Тег '" + request.name() + "' уже существует");
        }
        Tag saved = tagRepository.save(new Tag(request.name()));
        log.info("Создан тег '{}'", saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = getTag(id);
        // читаем ленивую коллекцию внутри транзакции
        if (!tag.getCourses().isEmpty()) {
            throw new ConflictException("Нельзя удалить тег '" + tag.getName()
                    + "': он используется в " + tag.getCourses().size() + " курс(ах).");
        }
        tagRepository.delete(tag);
        log.info("Удалён тег '{}'", tag.getName());
    }

    private Tag getTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Тег с id=" + id + " не найден"));
    }

    public static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
