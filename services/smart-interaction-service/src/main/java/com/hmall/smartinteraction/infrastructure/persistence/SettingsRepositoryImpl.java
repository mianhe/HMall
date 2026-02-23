package com.hmall.smartinteraction.infrastructure.persistence;

import com.hmall.smartinteraction.domain.Settings;
import com.hmall.smartinteraction.domain.SettingsRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SettingsRepositoryImpl implements SettingsRepository {

    private final SettingsJpaRepository jpa;

    public SettingsRepositoryImpl(SettingsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Settings> find() {
        return jpa.findById(1L);
    }

    @Override
    public Settings save(Settings settings) {
        return jpa.save(settings);
    }
}
