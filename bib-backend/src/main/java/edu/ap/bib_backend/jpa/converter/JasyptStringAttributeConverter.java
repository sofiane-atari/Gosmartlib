package edu.ap.bib_backend.jpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

@Converter
public class JasyptStringAttributeConverter implements AttributeConverter<String, String> {

    private static final StringEncryptor ENCRYPTOR = buildEncryptor();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return ENCRYPTOR.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return ENCRYPTOR.decrypt(dbData);
        } catch (Exception e) {
            // Fallback for existing unencrypted data to not break functionality
            return dbData;
        }
    }

    private static StringEncryptor buildEncryptor() {
        String password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (password == null || password.isBlank()) {
            password = System.getProperty("JASYPT_ENCRYPTOR_PASSWORD");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("JASYPT_ENCRYPTOR_PASSWORD is required for encryption at rest");
        }

        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("2");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setConfig(config);
        return encryptor;
    }
}
