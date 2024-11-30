package com.demo.dropbox_test.utilities;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.oauth.DbxRefreshResult;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DropboxToken {
    private static int durationRefreshToken = 1;

    private String accessToken;
    private Long accessTokenExpiredTimeTimestamp;
    private LocalDateTime accessTokenExpiredTime;
    private String refreshToken;
    private LocalDateTime refreshTokenExpiredTime;

    public DropboxToken(DbxAuthFinish authFinish) {
        accessToken = authFinish.getAccessToken();
        accessTokenExpiredTimeTimestamp = authFinish.getExpiresAt();
        accessTokenExpiredTime = new Timestamp(accessTokenExpiredTimeTimestamp).toLocalDateTime();
        refreshToken = authFinish.getRefreshToken();
        refreshTokenExpiredTime = LocalDateTime.now().plusYears(durationRefreshToken);
    }

    public void updateAccessToken(DbxRefreshResult dbxRefreshResult) {
        accessToken = dbxRefreshResult.getAccessToken();
        accessTokenExpiredTimeTimestamp = dbxRefreshResult.getExpiresAt();
        accessTokenExpiredTime = new Timestamp(accessTokenExpiredTimeTimestamp).toLocalDateTime();
    }
}