package com.demo.dropbox_test.controllers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.demo.dropbox_test.payloads.responses.DropboxAuthValidResponse;
import com.demo.dropbox_test.services.DropboxService;
import com.demo.dropbox_test.services.JsonService;
import com.demo.dropbox_test.utilities.DropboxToken;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxStandardSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.TokenAccessType;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/dropbox")
public class DropboxController {
    @Autowired
    private DropboxService dropboxService;

    @Autowired
    private JsonService jsonService;

    @Value("${dropbox.redirect_uri}")
    private String redirectUri;

    @Value("${dropbox.session_key}")
    private String sessionKey;

    @GetMapping("/dropbox-auth-start")
    public void authStart(HttpServletResponse response, HttpServletRequest request) throws IOException {
        DbxWebAuth webAuth = dropboxService.getWebAuth();

        HttpSession session = request.getSession(true);
        DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(session, sessionKey);

        DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
                .withRedirectUri(redirectUri, csrfTokenStore)
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .build();

        String authorizePageUrl = webAuth.authorize(authRequest);
        response.sendRedirect(authorizePageUrl);
    }

    @GetMapping("/dropbox-auth-finish")
    public ResponseEntity<String> authFinish(HttpServletResponse response, HttpServletRequest request)
            throws IOException {
        DbxWebAuth webAuth = dropboxService.getWebAuth();
        DbxRequestConfig requestConfig = dropboxService.getRequestConfig();

        HttpSession session = request.getSession(true);
        DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(session, sessionKey);

        DbxAuthFinish authFinish;
        try {
            authFinish = webAuth.finishFromRedirect(redirectUri, csrfTokenStore, request.getParameterMap());
            DropboxToken dropboxToken = new DropboxToken(authFinish);
            jsonService.saveToken(dropboxToken);
        } catch (DbxWebAuth.BadRequestException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("On /dropbox-auth-finish: Bad request: " + ex.getMessage());
        } catch (DbxWebAuth.BadStateException ex) {
            response.sendRedirect("http://my-server.com/dropbox-auth-start");
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Send them back to the start of the auth flow.");
        } catch (DbxWebAuth.CsrfException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("On /dropbox-auth-finish: CSRF mismatch: " + ex.getMessage());
        } catch (DbxWebAuth.NotApprovedException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("When Dropbox asked: Do you want to allow this app to access your Dropbox account? The user clicked 'No'");
        } catch (DbxWebAuth.ProviderException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("On /dropbox-auth-finish: Auth failed: " + ex.getMessage());
        } catch (DbxException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("On /dropbox-auth-finish: Error getting token: " + ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("On /dropbox-auth-finish: Error write token: " + ex.getMessage());
        }

        DbxClientV2 client = new DbxClientV2(requestConfig, authFinish.getAccessToken());
        dropboxService.setDropboxClient(client);

        return ResponseEntity.status(HttpStatus.OK)
                .body("Access token saved successfully");
    }

    @GetMapping("/dropbox-auth-valid")
    public ResponseEntity<DropboxAuthValidResponse> validAuth() {
        return dropboxService.isValidToken() ? ResponseEntity.ok(new DropboxAuthValidResponse("valid"))
                : ResponseEntity.ok(new DropboxAuthValidResponse("invalid"));
    }

    @GetMapping("/dropbox-auth-refresh")
    public ResponseEntity<String> authRefresh() {
        try {
            dropboxService.refreshAuth();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/run-test")
    public String run_test() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return "online API " + dtf.format(now);
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("path") String path, @RequestParam("file") MultipartFile file) {
        try {
            String id = dropboxService.uploadFile(path + "/" + file.getOriginalFilename(), file.getInputStream()).getId();
            return "Archivo subido exitosamente. ID=" + id;
        } catch (Exception e) {
            return "Error al subir el archivo: " + e.getMessage();
        }
    }

    @PostMapping("/upload-files")
    public String uploadFiles(@RequestParam("path") String path, @RequestParam("files") List<MultipartFile> files) {
        try {
            for (MultipartFile file : files) {
                dropboxService.uploadFile(path + "/" + file.getOriginalFilename(), file.getInputStream());
            }
            return "Archivos subidos exitosamente.";
        } catch (Exception e) {
            return "Error al subir los archivos: " + e.getMessage();
        }
    }

    @PostMapping("/create-folder")
    public String createFolder(@RequestParam("path") String path) {
        try {
            dropboxService.createFolder(path);
            return "Carpeta creada exitosamente.";
        } catch (Exception e) {
            return "Error al crear la carpeta: " + e.getMessage();
        }
    }

    @GetMapping("/list-folder")
    public ResponseEntity<List<Metadata>> listFolder(@RequestParam String path) {
        try {
            List<Metadata> files = dropboxService.listFolder(path);
            return ResponseEntity.ok(files);
        } catch (DbxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/temporary-link")
    public ResponseEntity<String> temporaryLink(@RequestParam String path) {
        try {
            String temporaryLink = dropboxService.getTemporaryLink(path);
            return ResponseEntity.ok(temporaryLink);
        } catch (DbxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@RequestParam String path) {
        try {
            byte[] thumbnail = dropboxService.getThumbnail(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnail);
        } catch (DbxException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/thumbnail-id/{id}")
    public ResponseEntity<byte[]> getThumbnailId(@PathVariable Long id) {
        try {
            String path = "/" + id + ".jpg";
            byte[] thumbnail = dropboxService.getThumbnail(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnail);
        } catch (DbxException | IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/thumbnail-link")
    public ResponseEntity<String> thumbnailLink(@RequestParam String path) throws IOException {
        try {
            String temporaryLink = dropboxService.getThumbnailLink(path);
            return ResponseEntity.ok(temporaryLink);
        } catch (DbxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("file-request")
    public String fileRequest() {
        return dropboxService.getFileRequest("perfil.jpg", "/hola");
    }

    // Otros endpoints para interactuar con Dropbox
}
