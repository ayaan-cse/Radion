package com.radion.domain.models;

import com.radion.service.integration.oauth.TokenEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TokenEncryptionConverter implements AttributeConverter<String, String> {

    private static volatile TokenEncryptionService encryptionService;

    public static void setEncryptionService(TokenEncryptionService service) {
        encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (encryptionService != null) {
            return encryptionService.encrypt(attribute);
        }
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (encryptionService != null) {
            return encryptionService.decrypt(dbData);
        }
        return dbData;
    }
}
