package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Admin;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminRepositoryExtension {

    /**
     * Admin par agence (pour notifications)
     */
    @Query("SELECT a FROM Admin a WHERE a.agence.id = :agenceId")
    Optional<Admin> findByAgenceId(@Param("agenceId") Long agenceId);

    /**
     * Admins avec email activé
     */
    @Query("SELECT a FROM Admin a WHERE a.adresseMail IS NOT NULL AND a.adresseMail != ''")
    List<Admin> findWithEmail();

    /**
     * Email de l'admin
     */
    @Query("SELECT a.adresseMail FROM Admin a WHERE a.id = :adminId")
    String findEmailById(@Param("adminId") Long adminId);
}
