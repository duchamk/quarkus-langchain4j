package io.quarkiverse.langchain4j.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface LanguageModelBuild {
    /**
     * The model provider to use
     */
    Optional<ModelProvider> provider();
}
