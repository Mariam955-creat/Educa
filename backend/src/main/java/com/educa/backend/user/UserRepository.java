package com.educa.backend.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where cast(:q as string) is null
               or lower(u.fullName) like lower(concat('%', cast(:q as string), '%'))
               or lower(u.email) like lower(concat('%', cast(:q as string), '%'))
            order by u.createdAt desc
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}
