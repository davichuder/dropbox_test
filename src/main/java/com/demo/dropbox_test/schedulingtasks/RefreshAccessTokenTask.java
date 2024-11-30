package com.demo.dropbox_test.schedulingtasks;

import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.demo.dropbox_test.services.DropboxService;

@Component
public class RefreshAccessTokenTask {
    @Autowired
    private DropboxService dropboxService;

    private final long additionalTime = 1000 * 60 * 10;

    @Scheduled(fixedRate = additionalTime)
    public void refreshAccessTokenAsk() {
        if (dropboxService.isAccessTokenExpired(additionalTime, ChronoUnit.MILLIS)) {
            dropboxService.refreshAccessToken();
        }
    }
}
