package com.messagerie.message;

import com.messagerie.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private User expediteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private User destinataire;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "date_envoi", nullable = false, updatable = false)
    private Instant dateEnvoi;

    protected Message() {
    }

    public Message(User expediteur, User destinataire, String contenu) {
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.contenu = contenu;
    }

    @PrePersist
    private void horodater() {
        dateEnvoi = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getExpediteur() {
        return expediteur;
    }

    public User getDestinataire() {
        return destinataire;
    }

    public String getContenu() {
        return contenu;
    }

    public Instant getDateEnvoi() {
        return dateEnvoi;
    }
}
