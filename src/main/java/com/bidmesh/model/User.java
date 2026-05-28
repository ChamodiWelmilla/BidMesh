package com.bidmesh.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Auction> createdAuctions;

    @OneToMany(mappedBy = "bidder", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Bid> bids;
}
