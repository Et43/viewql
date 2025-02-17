package com.mwrcybersec.viewql.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

@Service
public class FileService {
    
    public Path extractZip(MultipartFile zipFile, String destinationDir) throws IOException {
        Path destPath = Path.of(destinationDir);
        Files.createDirectories(destPath);

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            var zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path newPath = destPath.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
        
        return destPath;
    }

    public void cleanup(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a))
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
}