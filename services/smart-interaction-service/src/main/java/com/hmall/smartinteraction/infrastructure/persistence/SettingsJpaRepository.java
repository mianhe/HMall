package com.hmall.smartinteraction.infrastructure.persistence;

import com.hmall.smartinteraction.domain.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsJpaRepository extends JpaRepository<Settings, Long> {
}
