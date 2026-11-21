package com.bidmesh.catalog.repository;

import com.bidmesh.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
