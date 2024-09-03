package com.demo.dropbox_test.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.demo.dropbox_test.services.DropboxService;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.Metadata;

@RestController
@RequestMapping("/api/dropbox")
public class DropboxController {

    @Autowired
    private DropboxService dropboxService;

    @GetMapping("/run-test")
    public String run_test(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();  
        return "online API " + dtf.format(now);
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("path") String path, @RequestParam("file") MultipartFile file) {
        try {
            dropboxService.uploadFile(path + "/" + file.getOriginalFilename(), file.getInputStream());
            return "Archivo subido exitosamente.";
        } catch (Exception e) {
            return "Error al subir el archivo: " + e.getMessage();
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

    @GetMapping("/thumbnail-link")
    public ResponseEntity<String> thumbnailLink(@RequestParam String path) throws IOException {
        try {
            String temporaryLink = dropboxService.getThumbnailLink(path);
            return ResponseEntity.ok(temporaryLink);
        } catch (DbxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Otros endpoints para interactuar con Dropbox
}

