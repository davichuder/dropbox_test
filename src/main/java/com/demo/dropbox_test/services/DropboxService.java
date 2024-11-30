package com.demo.dropbox_test.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.demo.dropbox_test.utilities.DropboxToken;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.oauth.DbxRefreshResult;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.filerequests.FileRequest;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.ThumbnailFormat;
import com.dropbox.core.v2.files.ThumbnailSize;

import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class DropboxService {
    private JsonService jsonService = new JsonService();

    private final DbxRequestConfig requestConfig;
    private final DbxAppInfo appInfo;
    private final DbxWebAuth webAuth;
    private DbxClientV2 dropboxClient;
    private DropboxToken dropboxToken;

    private final String urlAuthStart = "http://localhost:8080/api/dropbox/dropbox-auth-start";

    public DropboxService(@Value("${dropbox.app_key}") String appKey,
            @Value("${dropbox.secret_key}") String secretKey) {
        requestConfig = new DbxRequestConfig("dropbox-authorize");
        appInfo = new DbxAppInfo(appKey, secretKey);
        webAuth = new DbxWebAuth(requestConfig, appInfo);
        dropboxToken = jsonService.readToken();
        if (isValidToken()) {
            dropboxClient = new DbxClientV2(requestConfig, dropboxToken.getAccessToken());
        }
    }

    public boolean isRefreshTokenExpired() {
        return LocalDateTime.now()
                .isAfter(dropboxToken.getAccessTokenExpiredTime());
    }

    public boolean isRefreshTokenExpired(long aditionalTime, TemporalUnit temporalUnit) {
        return LocalDateTime.now()
                .plus(aditionalTime, temporalUnit)
                .isAfter(dropboxToken.getAccessTokenExpiredTime());
    }

    public boolean isAccessTokenExpired() {
        return LocalDateTime.now()
                .isAfter(dropboxToken.getRefreshTokenExpiredTime());
    }

    public boolean isAccessTokenExpired(long aditionalTime, TemporalUnit temporalUnit) {
        return LocalDateTime.now()
                .plus(aditionalTime, temporalUnit)
                .isAfter(dropboxToken.getRefreshTokenExpiredTime());
    }

    public void refreshAccessToken() {
        DbxCredential dbxCredential = new DbxCredential(dropboxToken.getAccessToken(),
                dropboxToken.getAccessTokenExpiredTimeTimestamp(),
                dropboxToken.getRefreshToken(),
                appInfo.getKey(),
                appInfo.getSecret());
        dropboxClient = new DbxClientV2(requestConfig, dbxCredential);
        try {
            DbxRefreshResult dbxRefreshResult = dropboxClient.refreshAccessToken();
            dropboxToken.updateAccessToken(dbxRefreshResult);
            jsonService.saveToken(dropboxToken);
        } catch (DbxException | IOException e) {
            redirectAuthStart();
        }
    }

    public boolean isValidToken() {
        return dropboxToken != null && !isRefreshTokenExpired() && !isAccessTokenExpired();
    }

    public void redirectAuthStart() {
        WebClient client = WebClient.create();
        client.get()
                .uri(urlAuthStart)
                .retrieve()
                .bodyToMono(String.class);
    }

    public void refreshAuth() {
        if (dropboxToken == null || isRefreshTokenExpired()) {
            redirectAuthStart();
        }
        refreshAccessToken();
    }

    public FileMetadata uploadFile(String path, InputStream inputStream) throws DbxException, IOException {
        return dropboxClient.files().uploadBuilder(path)
                .uploadAndFinish(inputStream);
    }

    public FolderMetadata createFolder(String path) throws DbxException {
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

    public String getFileRequest(String title, String destination){
        try {
            FileRequest fileRequest = dropboxClient.fileRequests().create(title, destination);
            return fileRequest.getUrl();
        } catch (DbxException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Otros métodos para interactuar con Dropbox
}
