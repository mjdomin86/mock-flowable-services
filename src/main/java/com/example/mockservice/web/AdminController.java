package com.example.mockservice.web;

import com.example.mockservice.domain.MockConfiguration;
import com.example.mockservice.domain.ServiceDefinition;
import com.example.mockservice.domain.ServiceOperation;
import com.example.mockservice.repository.MockConfigurationRepository;
import com.example.mockservice.repository.RequestLogRepository;
import com.example.mockservice.repository.ServiceDefinitionRepository;
import com.example.mockservice.repository.ServiceOperationRepository;
import com.example.mockservice.service.FlowableClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

import com.example.mockservice.domain.MockRule;
import com.example.mockservice.repository.MockRuleRepository;

@Controller
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AdminController {

    private final FlowableClientService flowableClientService;
    private final com.example.mockservice.service.MockExecutionService mockExecutionService;
    private final ServiceDefinitionRepository serviceDefinitionRepository;
    private final ServiceOperationRepository serviceOperationRepository;
    private final MockConfigurationRepository mockConfigurationRepository;
    private final RequestLogRepository requestLogRepository;

    @GetMapping("/")
    public String index() {
        return "redirect:/services";
    }

    @GetMapping("/services")
    public String viewServices(Model model) {
        model.addAttribute("services", serviceDefinitionRepository.findAll());
        return "services";
    }

    @PostMapping("/sync")
    public String syncServices(RedirectAttributes redirectAttributes) {
        try {
            flowableClientService.syncDefinitions();
            redirectAttributes.addFlashAttribute("message", "Services synced successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Sync failed: " + e.getMessage());
        }
        return "redirect:/services";
    }

    @GetMapping("/services/{serviceId}")
    public String viewServiceOperations(@PathVariable String serviceId, Model model) {
        Optional<ServiceDefinition> service = serviceDefinitionRepository.findById(serviceId);
        if (service.isPresent()) {
            model.addAttribute("service", service.get());
            model.addAttribute("operations", service.get().getOperations());
            return "operations";
        }
        return "redirect:/services";
    }

    @GetMapping("/operation/{operationId}/config")
    public String configureOperation(@PathVariable String operationId, Model model) {
        Optional<ServiceOperation> op = serviceOperationRepository.findById(operationId);
        if (op.isPresent()) {
            model.addAttribute("operation", op.get());
            MockConfiguration config = mockConfigurationRepository.findByOperationId(operationId)
                    .orElse(new MockConfiguration());
            // If new, set operationId
            if (config.getOperationId() == null) {
                config.setOperationId(operationId);
            }
            model.addAttribute("config", config);
            return "config";
        }
        return "redirect:/services";
    }

    @PostMapping("/operation/{operationId}/config")
    public String saveConfiguration(@PathVariable String operationId, @ModelAttribute MockConfiguration config,
            RedirectAttributes redirectAttributes) {

        log.info("Saving config for operationId: {}. received status: {}", operationId, config.getHttpStatus());

        MockConfiguration existing = mockConfigurationRepository.findByOperationId(operationId)
                .orElse(new MockConfiguration());

        existing.setOperationId(operationId);
        existing.setHttpStatus(config.getHttpStatus());
        existing.setCustomResponseBody(config.getCustomResponseBody());

        MockConfiguration saved = mockConfigurationRepository.save(existing);
        log.info("Saved config: id={}, opId={}, status={}", saved.getId(), saved.getOperationId(),
                saved.getHttpStatus());

        redirectAttributes.addFlashAttribute("message", "Configuration saved!");

        // Find service ID to redirect back
        ServiceOperation op = serviceOperationRepository.findById(operationId).orElseThrow();
        String serviceId = op.getServiceDefinition().getId();

        return "redirect:/services/" + serviceId;
    }

    @PostMapping("/operations/{id}/rules")
    public String addRule(@PathVariable String id, @ModelAttribute MockRule rule) {
        mockExecutionService.addRule(id, rule);
        // Need serviceId for redirect. Could be returned by addRule or fetched.
        ServiceOperation op = serviceOperationRepository.findById(id).orElseThrow();
        return "redirect:/services/" + op.getServiceDefinition().getId();
    }

    @GetMapping("/rules/{id}/delete")
    public String deleteRule(@PathVariable String id) {
        String serviceId = mockExecutionService.deleteRule(id);
        return "redirect:/services/" + serviceId;
    }

    @GetMapping("/logs")
    public String viewLogs(Model model) {
        model.addAttribute("logs", requestLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp")));
        return "logs";
    }
}
