package com.demo.dropbox_test.services;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.demo.dropbox_test.utilities.DropboxToken;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class JsonService {
    private final ObjectMapper objectMapper;

    private final String accessKeyFile = "src/main/resources/tokens/dropbox.json";

    public JsonService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public DropboxToken readJson(String filePath) {
        try {
            return objectMapper.readValue(new File(filePath), DropboxToken.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeJson(String filePath, DropboxToken dropboxToken) {
        try {
            objectMapper.writeValue(new File(filePath), dropboxToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DropboxToken readToken() {
        try {
            return objectMapper.readValue(new File(accessKeyFile), DropboxToken.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveToken(DropboxToken dropboxToken) throws StreamWriteException, DatabindException, IOException {
        objectMapper.writeValue(new File(accessKeyFile), dropboxToken);
    }
}
