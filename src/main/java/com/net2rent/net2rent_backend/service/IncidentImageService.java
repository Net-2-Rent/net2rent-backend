package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentImage;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

@Service
public class IncidentImageService {

    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    public List<IncidentImage> buildImages(List<String> dataUris, Incident incident, LocalDateTime now) {
        List<String> safeList = dataUris != null ? dataUris : List.of();
        List<IncidentImage> images = new ArrayList<>();
        for (int i = 0; i < safeList.size(); i++) {
            images.add(toIncidentImage(safeList.get(i), incident, i, now));
        }
        return images;
    }

    private IncidentImage toIncidentImage(String dataUri, Incident incident, int index, LocalDateTime now) {
        String base64Payload = dataUri.contains(",")
                ? dataUri.substring(dataUri.indexOf(',') + 1)
                : dataUri;

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException e) {
            throw new ConflictException("La imagen " + (index + 1) + " no es un archivo válido");
        }

        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new ConflictException("La imagen " + (index + 1) + " supera el tamaño máximo de 5 MB");
        }

        String format = detectImageFormat(bytes);
        if (!"jpeg".equalsIgnoreCase(format) && !"png".equalsIgnoreCase(format)) {
            throw new ConflictException("La imagen " + (index + 1) + " debe ser JPG o PNG");
        }

        return IncidentImage.builder()
                .incident(incident)
                .data(bytes)
                .contentType("image/" + format.toLowerCase())
                .uploadedAt(now)
                .build();
    }

    private String detectImageFormat(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            return readers.hasNext() ? readers.next().getFormatName() : null;
        } catch (IOException e) {
            return null;
        }
    }
}