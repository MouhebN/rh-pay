package com.rhpaie.payslip.service;

import com.rhpaie.common.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties properties;

    public Path storePayslip(String fileName, byte[] content) {
        try {
            Path directory = Path.of(properties.getPayslipUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve(fileName).normalize();
            if (!target.startsWith(directory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
            }
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return target;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store payslip PDF");
        }
    }

    public Resource load(String filePath) {
        try {
            Resource resource = new UrlResource(Path.of(filePath).toAbsolutePath().normalize().toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip file not found");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payslip file not found");
        }
    }
}
