package com.bidmesh.catalog.controller;

import com.bidmesh.catalog.repository.ItemRepository;
import com.bidmesh.catalog.model.Item;
import com.bidmesh.catalog.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class FileUploadController {

    private final ItemRepository itemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final S3Service s3Service;

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (!s3Service.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Image uploads are unavailable because AWS S3 is not configured");
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        Item item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                    : ".jpg";
            String uniqueFileName = UUID.randomUUID().toString() + extension;

            String uploadKey = "raw/" + uniqueFileName;
            String secureUrl = s3Service.uploadImage(
                    uploadKey, file.getContentType(), file.getInputStream(), file.getSize());

            item.setImageUrl(secureUrl);
            itemRepository.save(item);

            Set<String> keys = redisTemplate.keys("auction:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            return ResponseEntity.ok("Image uploaded successfully: " + secureUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image to AWS S3");
        }
    }
}
