package com.example.mockservice.repository;

import com.example.mockservice.domain.ServiceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServiceDefinitionRepository extends JpaRepository<ServiceDefinition, String> {
    Optional<ServiceDefinition> findByKey(String key);
}
