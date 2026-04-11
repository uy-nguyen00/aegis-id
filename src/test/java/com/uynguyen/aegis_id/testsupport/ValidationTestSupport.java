package com.uynguyen.aegis_id.testsupport;

import com.uynguyen.aegis_id.validation.EmailDomainValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

public final class ValidationTestSupport {

    private static final String TEST_CONFIG_FILE = "application-dev.yml";
    private static final String DISPOSABLE_EMAIL_DOMAINS_KEY =
        "app.security.disposable-email-domains";
    private static final List<String> DISPOSABLE_EMAIL_DOMAINS =
        loadDisposableEmailDomains();

    private static final Validator VALIDATOR = buildValidator();

    private ValidationTestSupport() {}

    public static Validator createValidatorWithDisposableEmailSupport() {
        return VALIDATOR;
    }

    private static List<String> loadDisposableEmailDomains() {
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource(TEST_CONFIG_FILE));
        Properties properties = yamlFactory.getObject();

        if (properties == null) {
            throw new IllegalStateException(
                "Unable to load test configuration from " + TEST_CONFIG_FILE
            );
        }

        String domains = properties.getProperty(DISPOSABLE_EMAIL_DOMAINS_KEY);
        if (domains == null || domains.isBlank()) {
            throw new IllegalStateException(
                "Missing required property: " + DISPOSABLE_EMAIL_DOMAINS_KEY
            );
        }

        return Arrays.stream(domains.split(","))
            .map(String::trim)
            .filter(domain -> !domain.isEmpty())
            .toList();
    }

    private static Validator buildValidator() {
        ValidatorFactory defaultFactory =
            Validation.buildDefaultValidatorFactory();
        ConstraintValidatorFactory delegate =
            defaultFactory.getConstraintValidatorFactory();

        ValidatorFactory customFactory = Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(
                new DisposableEmailAwareConstraintValidatorFactory(delegate)
            )
            .buildValidatorFactory();

        return customFactory.getValidator();
    }

    private static final class DisposableEmailAwareConstraintValidatorFactory
        implements ConstraintValidatorFactory {

        private final ConstraintValidatorFactory delegate;

        private DisposableEmailAwareConstraintValidatorFactory(
            ConstraintValidatorFactory delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance(
            Class<T> key
        ) {
            if (EmailDomainValidator.class.equals(key)) {
                return key.cast(
                    new EmailDomainValidator(DISPOSABLE_EMAIL_DOMAINS)
                );
            }
            return delegate.getInstance(key);
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> instance) {
            delegate.releaseInstance(instance);
        }
    }
}
