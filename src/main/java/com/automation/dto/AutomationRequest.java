package com.automation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRequest {

    private Long userId;
    private List<String> platforms; // ["naukri", "linkedin"]
    private Map<String, Object> config; // automation configuration
}
