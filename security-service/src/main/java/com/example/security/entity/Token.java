package com.rapidalert.security.entity;

import com.rapidalert.security.model.TokenType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.rapidalert.security.model.TokenType.BEARER;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tokens")
public class Token implements BaseEntity<Long> {

    @Id
    @GeneratedValue
    public Long id;

    @OneToOne
    @JoinColumn(name = "client_id")
    public Client client;

    @Enumerated(EnumType.STRING)
    public final TokenType tokenType = BEARER;

    @Column(unique = true)
    public String jwt;

    public boolean revoked;
    public boolean expired;
}
