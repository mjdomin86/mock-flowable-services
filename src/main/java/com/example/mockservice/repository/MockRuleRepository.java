package com.example.mockservice.repository;

import com.example.mockservice.domain.MockRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MockRuleRepository extends JpaRepository<MockRule, String> {
    List<MockRule> findByServiceOperationIdOrderByPriorityAsc(String serviceOperationId);
}
