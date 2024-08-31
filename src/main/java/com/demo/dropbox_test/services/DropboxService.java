package com.demo.dropbox_test.services;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FolderMetadata;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DropboxService {

    private final DbxClientV2 dropboxClient;

    public DropboxService(@Value("${dropbox.access.token}") String accessToken) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/spring-boot-example").build();
        this.dropboxClient = new DbxClientV2(config, accessToken);
    }

    public void uploadFile(String path, InputStream inputStream) throws DbxException, IOException {
        dropboxClient.files().uploadBuilder(path)
                .uploadAndFinish(inputStream);
    }

    public FolderMetadata createFolder(String path) throws CreateFolderErrorException, DbxException {
        return dropboxClient.files().createFolderV2(path).getMetadata();
    }

    // Otros métodos para interactuar con Dropbox
}

