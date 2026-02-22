package com.hmall.smartinteraction.application;

import com.hmall.smartinteraction.domain.Skill;
import com.hmall.smartinteraction.domain.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SkillApplicationService {

    private final SkillRepository repository;

    public SkillApplicationService(SkillRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Skill create(String name, String description, String systemPrompt, List<String> allowedTools) {
        Skill skill = new Skill(name, description, systemPrompt, allowedTools);
        return repository.save(skill);
    }

    public List<Skill> list() {
        return repository.findAllOrderByCreatedAtDesc();
    }

    public Optional<Skill> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Optional<Skill> update(Long id, String name, String description, String systemPrompt, List<String> allowedTools) {
        return repository.findById(id).map(skill -> {
            skill.update(name, description, systemPrompt, allowedTools);
            return repository.save(skill);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    @Transactional
    public Optional<Skill> setDefault(Long id) {
        return repository.findById(id).map(skill -> {
            repository.findDefault().ifPresent(current -> {
                if (!current.getId().equals(id)) {
                    current.clearDefault();
                    repository.save(current);
                }
            });
            skill.setAsDefault();
            return repository.save(skill);
        });
    }
}
