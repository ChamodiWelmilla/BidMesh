package com.bidmesh.controller;

import com.bidmesh.model.Item;
import com.bidmesh.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") 

public class ItemController {

    private final ItemRepository itemRepository;

    @PostMapping
    public ResponseEntity<Item> createItem(@RequestBody Item item) {
        return ResponseEntity.ok(itemRepository.save(item));
    }
}
