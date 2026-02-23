package com.hmall.smartinteraction.application;

import com.hmall.smartinteraction.domain.AiChatService;
import com.hmall.smartinteraction.domain.Settings;
import com.hmall.smartinteraction.domain.SettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsApplicationService {

    private final SettingsRepository repository;

    public SettingsApplicationService(SettingsRepository repository) {
        this.repository = repository;
    }

    public Settings getOrCreate() {
        return repository.find().orElseGet(() -> {
            Settings defaults = Settings.createDefault(
                    AiChatService.DEFAULT_ADMIN_BASE_PROMPT,
                    AiChatService.DEFAULT_CONSUMER_BASE_PROMPT);
            return repository.save(defaults);
        });
    }

    @Transactional
    public Settings update(String adminBasePrompt, String consumerBasePrompt) {
        Settings settings = getOrCreate();
        settings.update(adminBasePrompt, consumerBasePrompt);
        return repository.save(settings);
    }

    @Transactional
    public Settings reset() {
        Settings settings = getOrCreate();
        settings.resetToDefaults(
                AiChatService.DEFAULT_ADMIN_BASE_PROMPT,
                AiChatService.DEFAULT_CONSUMER_BASE_PROMPT);
        return repository.save(settings);
    }
}
