package com.bidmesh.controller;

import com.bidmesh.model.Item;
import com.bidmesh.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class FileUploadController {

    private final ItemRepository itemRepository;
    private final Cloudinary cloudinary;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        Item item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found");
        }

        try {
            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String secureUrl = uploadResult.get("secure_url").toString();

            item.setImageUrl(secureUrl);
            itemRepository.save(item);

            java.util.Set<String> keys = redisTemplate.keys("auction:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            return ResponseEntity.ok("Image uploaded successfully: " + secureUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image to Cloudinary");
        }
    }
}
