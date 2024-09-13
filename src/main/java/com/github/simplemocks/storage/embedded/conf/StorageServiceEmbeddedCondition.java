package com.github.simplemocks.storage.embedded.conf;


import com.github.simplemocks.storage.embedded.EnableStorageServiceEmbedded;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author sibmaks
 * @since 0.1.5
 */
public class StorageServiceEmbeddedCondition implements Condition {
    @Override
    public boolean matches(@Nonnull ConditionContext context,
                           @Nonnull AnnotatedTypeMetadata metadata) {
        return context.getBeanFactory()
                .getBeanNamesForAnnotation(EnableStorageServiceEmbedded.class).length > 0;

    }
}
