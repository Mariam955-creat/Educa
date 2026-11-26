package com.educa.backend.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    long countByRoles_NameAndDeletedAtIsNull(RoleName name);

    long countByEnabledFalseAndDeletedAtIsNull();

    long countByCreatedAtAfterAndDeletedAtIsNull(java.time.Instant since);

    long countByDeletedAtIsNull();

    List<User> findByDeletedAtIsNotNullOrderByDeletedAtDesc();

    boolean existsByEmailIgnoreCase(String email);

    long countByInstructorRequestedAtIsNotNullAndDeletedAtIsNull();

    /** Recherche admin ; les demandes de rôle formateur en attente passent en tête. */
    @Query("""
            select u from User u
            where u.deletedAt is null
              and (:requestsOnly = false or u.instructorRequestedAt is not null)
              and (cast(:q as string) is null
               or lower(u.fullName) like lower(concat('%', cast(:q as string), '%'))
               or lower(u.email) like lower(concat('%', cast(:q as string), '%')))
            order by case when u.instructorRequestedAt is null then 1 else 0 end, u.createdAt desc
            """)
    Page<User> search(@Param("q") String q, @Param("requestsOnly") boolean requestsOnly, Pageable pageable);
}
