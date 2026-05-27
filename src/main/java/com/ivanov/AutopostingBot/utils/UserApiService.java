package com.ivanov.AutopostingBot.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserApiService {

    private final String apiEndpoint;
    private final String apiToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, Boolean> rightsCache = new HashMap<>();

    public UserApiService(@Value("${pinto.api.endpoint}") String apiEndpoint,
                          @Value("${pinto.api.token}") String apiToken) {
        this.apiEndpoint = apiEndpoint;
        this.apiToken = apiToken;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void clearCache() {
        rightsCache.clear();
    }

    public boolean hasPostingRightCached(long userId) {
        return rightsCache.computeIfAbsent(userId, this::hasPostingRight);
    }

    @SneakyThrows
    private boolean hasPostingRight(long userId) {
        HttpGet httpGet = new HttpGet(apiEndpoint + "/users/user-rights?id=" + userId);
        httpGet.setHeader("Authorization", apiToken);

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(
                SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
                NoopHostnameVerifier.INSTANCE);

        try (CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(scsf).build();
             CloseableHttpResponse response = client.execute(httpGet)) {

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 204 || response.getEntity() == null) {
                return false;
            }

            try (InputStream in = response.getEntity().getContent()) {
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                JsonNode array = objectMapper.readTree(json);
                for (JsonNode node : array) {
                    if ("POSTING".equals(node.asText())) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
