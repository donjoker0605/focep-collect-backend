package org.example.collectfocep.repositories;

import org.example.collectfocep.entities.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByAdresseMail(String email);

    @Query("SELECT a FROM Admin a JOIN FETCH a.agence WHERE a.adresseMail = :email")
    Optional<Admin> findByAdresseMailWithAgence(@Param("email") String email);

    @Query("SELECT a FROM Admin a WHERE a.agence.id = :agenceId")
    List<Admin> findByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT a FROM Admin a WHERE a.agence.id = :agenceId")
    Page<Admin> findByAgenceId(@Param("agenceId") Long agenceId, Pageable pageable);

    boolean existsByAdresseMail(String email);

    boolean existsByNumeroCni(String numeroCni);

    @Query("SELECT COUNT(a) FROM Admin a WHERE a.agence.id = :agenceId")
    long countByAgenceId(@Param("agenceId") Long agenceId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Admin a WHERE a.adresseMail = :email AND a.agence.id = :agenceId")
    boolean existsByAdresseMailAndAgenceId(@Param("email") String email, @Param("agenceId") Long agenceId);
}