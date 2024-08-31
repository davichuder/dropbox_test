package com.demo.dropbox_test.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.demo.dropbox_test.services.DropboxService;

@RestController
@RequestMapping("/api/dropbox")
public class DropboxController {

    @Autowired
    private DropboxService dropboxService;

    @GetMapping("/run_test")
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

    // Otros endpoints para interactuar con Dropbox
}

