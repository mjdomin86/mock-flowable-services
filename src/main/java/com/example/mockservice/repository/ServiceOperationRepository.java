package com.example.mockservice.repository;

import com.example.mockservice.domain.ServiceOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ServiceOperationRepository extends JpaRepository<ServiceOperation, String> {
    Optional<ServiceOperation> findByServiceDefinitionKeyAndKey(String serviceKey, String key);

    // Needed to find operation by method and URL pattern match?
    // We might need a custom query or do in-memory matching for complex paths.
    // For now, simple list by definition.
    List<ServiceOperation> findByServiceDefinitionId(String serviceDefinitionId);

    // Find operations by method and url (case insensitive for method usually, but
    // here we enforce strict or loose?)
    // Spring Data JPA query derivation
    List<ServiceOperation> findByMethodAndUrl(String method, String url);

    // Find all operations by HTTP method (for path pattern matching)
    List<ServiceOperation> findByMethod(String method);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "rules" })
    Optional<ServiceOperation> findWithRulesById(String id);
}
