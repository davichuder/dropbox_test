package com.demo.dropbox_test.services;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.ThumbnailFormat;
import com.dropbox.core.v2.files.ThumbnailSize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DropboxService {

    private final DbxClientV2 dropboxClient;

    public DropboxService(@Value("${dropbox.access.token}") String accessToken) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/spring-boot-example").build();
        this.dropboxClient = new DbxClientV2(config, accessToken);
    }

    public FileMetadata uploadFile(String path, InputStream inputStream) throws DbxException, IOException {
        return dropboxClient.files().uploadBuilder(path)
                .uploadAndFinish(inputStream);
    }

    public FolderMetadata createFolder(String path) throws CreateFolderErrorException, DbxException {
        return dropboxClient.files().createFolderV2(path).getMetadata();
    }

    public List<Metadata> listFolder(String path) throws DbxException {
        ListFolderResult result = dropboxClient.files().listFolder(path);
        return result.getEntries();
    }

    public String getTemporaryLink(String path) throws DbxException {
        return dropboxClient.files().getTemporaryLink(path).getLink();
    }

    public byte[] getThumbnail(String path) throws DbxException, IOException {
        ThumbnailSize size = ThumbnailSize.W256H256;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dropboxClient.files().getThumbnailBuilder(path)
                .withFormat(ThumbnailFormat.JPEG)
                .withSize(size)
                .download(outputStream);
        return outputStream.toByteArray();
    }

    public String getThumbnailLink(String path) throws DbxException, IOException {
        byte[] thumbnail = getThumbnail(path);
        String filename = Paths.get(path).getFileName().toString();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(thumbnail);
        FileMetadata metadata = uploadFile("/temp/" + filename, inputStream);
        return getTemporaryLink(metadata.getPathLower());
    }

    // Otros métodos para interactuar con Dropbox
}
