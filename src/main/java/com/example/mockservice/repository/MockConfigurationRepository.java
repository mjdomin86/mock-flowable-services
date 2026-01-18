package com.example.mockservice.repository;

import com.example.mockservice.domain.MockConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MockConfigurationRepository extends JpaRepository<MockConfiguration, String> {
    Optional<MockConfiguration> findByOperationId(String operationId);
}
