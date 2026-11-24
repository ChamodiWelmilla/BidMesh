package com.bidmesh.bidding.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bidding_auction_projections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionProjection {
    @Id
    private Long id;

    private Long itemId;
    private String itemName;
    private String itemDescription;
    private String itemImageUrl;
    private Long creatorId;
    private String creatorUsername;

    @Column(nullable = false)
    private BigDecimal startPrice;

    @Column(nullable = false)
    private BigDecimal currentPrice;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @Column(nullable = false)
    @Builder.Default
    private long catalogVersion = -1;

    @Column(nullable = false)
    @Builder.Default
    private long bidCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Version
    private Long version;
}
