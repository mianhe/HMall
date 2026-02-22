package com.hmall.smartinteraction.infrastructure.persistence;

import com.hmall.smartinteraction.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SkillJpaRepository extends JpaRepository<Skill, Long> {
    List<Skill> findAllByOrderByCreatedAtDesc();
    Optional<Skill> findByIsDefaultTrue();
}
