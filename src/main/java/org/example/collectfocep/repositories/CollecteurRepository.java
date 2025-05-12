package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Collecteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollecteurRepository extends JpaRepository<Collecteur, Long> {
    List<Collecteur> findByAgenceId(Long agenceId);

    // Ajout de méthode paginée
    Page<Collecteur> findByAgenceId(Long agenceId, Pageable pageable);

    Optional<Collecteur> findByAdresseMail(String email);

    // Méthode optimisée pour la sécurité
    @Query("SELECT c FROM Collecteur c JOIN FETCH c.agence WHERE c.adresseMail = :email")
    Optional<Collecteur> findByAdresseMailWithAgence(@Param("email") String email);

    boolean existsByAdresseMail(String adresseMail);
    boolean existsByNumeroCni(String numeroCni);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Collecteur c JOIN c.clients cl WHERE c.adresseMail = :email AND cl.id = :clientId")
    boolean existsByAdresseMailAndClientId(@Param("email") String email, @Param("clientId") Long clientId);

    @Query("SELECT c.agence.id FROM Collecteur c WHERE c.id = :collecteurId")
    Long findAgenceIdByCollecteurId(@Param("collecteurId") Long collecteurId);
}