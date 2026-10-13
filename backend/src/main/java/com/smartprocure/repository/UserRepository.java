package com.smartprocure.repository;

import com.smartprocure.entity.Role;
import com.smartprocure.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.departmentEntity.id = :departmentId")
    List<User> findByDepartmentId(@org.springframework.data.repository.query.Param("departmentId") Long departmentId);

    Page<User> findByActive(boolean active, Pageable pageable);
}
