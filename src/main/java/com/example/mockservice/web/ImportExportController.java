package com.example.mockservice.web;

import com.example.mockservice.domain.MockConfiguration;
import com.example.mockservice.domain.MockRule;
import com.example.mockservice.repository.MockConfigurationRepository;
import com.example.mockservice.repository.MockRuleRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ImportExportController {

    private final MockConfigurationRepository mockConfigurationRepository;
    private final MockRuleRepository mockRuleRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/export")
    @ResponseBody
    public void exportConfigurations(HttpServletResponse response) throws IOException {
        List<MockConfiguration> configs = mockConfigurationRepository.findAll();
        List<MockRule> rules = mockRuleRepository.findAll();

        // Create a comprehensive export object
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("configurations", configs);
        exportData.put("rules", rules);
        exportData.put("exportDate", java.time.Instant.now().toString());
        exportData.put("version", "1.0");

        response.setContentType("application/json");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=flowable-mock-configurations.json");

        objectMapper.writeValue(response.getOutputStream(), exportData);
    }

    @PostMapping("/import")
    public String importConfigurations(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/services";
        }

        try {
            // Try to read as new format (with rules)
            Map<String, Object> importData = objectMapper.readValue(file.getInputStream(),
                    new TypeReference<Map<String, Object>>() {
                    });

            if (importData.containsKey("configurations")) {
                // New format
                List<MockConfiguration> configs = objectMapper.convertValue(
                        importData.get("configurations"),
                        new TypeReference<List<MockConfiguration>>() {
                        });

                mockConfigurationRepository.deleteAll();
                mockConfigurationRepository.saveAll(configs);

                if (importData.containsKey("rules")) {
                    List<MockRule> rules = objectMapper.convertValue(
                            importData.get("rules"),
                            new TypeReference<List<MockRule>>() {
                            });

                    mockRuleRepository.deleteAll();
                    mockRuleRepository.saveAll(rules);
                }

                redirectAttributes.addFlashAttribute("message",
                        "Configurations and rules imported successfully!");
            } else {
                // Old format - just configurations
                file.getInputStream().reset();
                List<MockConfiguration> configs = objectMapper.readValue(file.getInputStream(),
                        new TypeReference<List<MockConfiguration>>() {
                        });

                mockConfigurationRepository.deleteAll();
                mockConfigurationRepository.saveAll(configs);

                redirectAttributes.addFlashAttribute("message", "Configurations imported successfully!");
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Import failed: " + e.getMessage());
        }

        return "redirect:/services";
    }
}
