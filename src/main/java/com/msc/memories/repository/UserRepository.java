package com.msc.memories.repository;

import com.msc.memories.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByRegistrationNumber(String registrationNumber);
    Optional<User> findByRegistrationNumberAndEmail(String registrationNumber, String email);
    Optional<User> findByRegistrationNumberAndPhoneNumber(String registrationNumber, String phoneNumber);
    Optional<User>findByEmail(String email);
    
}