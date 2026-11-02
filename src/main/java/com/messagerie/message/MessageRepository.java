package com.messagerie.message;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("""
            select m from Message m
            where (m.expediteur.id = :a and m.destinataire.id = :b)
               or (m.expediteur.id = :b and m.destinataire.id = :a)
            order by m.dateEnvoi, m.id
            """)
    List<Message> findConversation(@Param("a") Long utilisateurId, @Param("b") Long correspondantId);
}
