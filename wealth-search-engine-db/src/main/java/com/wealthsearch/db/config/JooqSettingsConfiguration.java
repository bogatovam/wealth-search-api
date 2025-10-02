package com.wealthsearch.db.config;

import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqSettingsConfiguration {

    @Bean
    public DefaultConfigurationCustomizer jooqConfigurationCustomizer() {
        return configuration -> {
            Settings settings = configuration.settings() != null ? configuration.settings() : new Settings();
            settings.setRenderNameStyle(RenderNameStyle.LOWER);
            settings.setRenderQuotedNames(RenderQuotedNames.NEVER);
            configuration.set(settings);
        };
    }
}
