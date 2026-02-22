package com.hmall.smartinteraction.domain;

import java.util.List;
import java.util.Optional;

public interface SkillRepository {
    Skill save(Skill skill);
    Optional<Skill> findById(Long id);
    List<Skill> findAllOrderByCreatedAtDesc();
    Optional<Skill> findDefault();
    void deleteById(Long id);
    boolean existsById(Long id);
}
