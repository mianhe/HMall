package com.hmall.smartinteraction.infrastructure.persistence;

import com.hmall.smartinteraction.domain.Skill;
import com.hmall.smartinteraction.domain.SkillRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SkillRepositoryImpl implements SkillRepository {

    private final SkillJpaRepository jpa;

    public SkillRepositoryImpl(SkillJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Skill save(Skill skill) {
        return jpa.save(skill);
    }

    @Override
    public Optional<Skill> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<Skill> findAllOrderByCreatedAtDesc() {
        return jpa.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<Skill> findDefault() {
        return jpa.findByIsDefaultTrue();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }
}
