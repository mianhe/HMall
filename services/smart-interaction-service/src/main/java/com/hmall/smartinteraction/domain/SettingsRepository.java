package com.hmall.smartinteraction.domain;

import java.util.Optional;

public interface SettingsRepository {

    Optional<Settings> find();

    Settings save(Settings settings);
}
