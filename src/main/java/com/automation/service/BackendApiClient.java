package com.automation.service;

import com.automation.dto.QuotaStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BackendApiClient {

    @Value("${backend.api.url}")
    private String backendApiUrl;

    @Value("${backend.api.key}")
    private String backendApiKey;

    private final RestTemplate restTemplate;

    public BackendApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Service-Key", backendApiKey);
        return headers;
    }

    public Map<String, Object> getUserProfile(Long userId) {
        String url = backendApiUrl + "/api/external/users/" + userId + "/profile";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getNaukriSettings(Long userId) {
        String url = backendApiUrl + "/api/external/users/" + userId + "/naukri-settings";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getLinkedInSettings(Long userId) {
        String url = backendApiUrl + "/api/external/users/" + userId + "/linkedin-settings";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getActiveResume(Long userId) {
        String url = backendApiUrl + "/api/external/users/" + userId + "/active-resume";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getApiKeysStatus(Long userId) {
        String url = backendApiUrl + "/api/external/users/" + userId + "/api-keys-status";
        HttpEntity<?> entity = new HttpEntity<>(createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public Long createApplication(Long userId, Map<String, Object> applicationData) {
        String url = backendApiUrl + "/api/external/users/" + userId + "/applications";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(applicationData, createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();
        return responseBody != null ? ((Number) responseBody.get("applicationId")).longValue() : null;
    }

    public Long createJobPosting(Long userId, Map<String, Object> jobPostingData) {
        String url = backendApiUrl + "/api/external/users/" + userId + "/job-postings";
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(jobPostingData, createHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();
        return responseBody != null ? ((Number) responseBody.get("jobPostingId")).longValue() : null;
    }
}
