package com.ivanov.AutopostingBot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.repo.PostRepo;
import com.ivanov.gptClient.MyGptClient.assistant.ChatGPTClient;
import com.ivanov.gptClient.MyGptClient.entities.GPTResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptGenerator {

    private final String REPLICATE_KEY;
    private final Pattern JSON_BLOCK = Pattern.compile("```json*(.*?)```", Pattern.DOTALL);
    private final PostRepo postRepo;
    private final S3Utils s3Utils;
    private final ChatGPTClient chatGPTClient;
    private final String pintoAPIToken;
    private final long ideasChannelId;
    private final String pintoBotLink;
    private final String pintoPromptsUrl;
    private final String webappApiUrl;
    private final String webappUser;
    private final String webappPassword;

    public PromptGenerator(@Value("${replicate.key}") String replicateKey,
                           PostRepo postRepo, S3Utils s3Utils,
                           ChatGPTClient chatGPTClient,
                           @Value("${pinto.api.token}") String pintoAPIToken,
                           @Value("${channel.ideas.id}") long ideasChannelId,
                           @Value("${pinto.bot.link}") String pintoBotLink,
                           @Value("${pinto.api.prompts.url}") String pintoPromptsUrl,
                           @Value("${webapp.api.url}") String webappApiUrl,
                           @Value("${webapp.api.username}") String webappUser,
                           @Value("${webapp.api.password}") String webappPassword) {
        REPLICATE_KEY = replicateKey;
        this.postRepo = postRepo;
        this.s3Utils = s3Utils;
        this.chatGPTClient = chatGPTClient;
        this.pintoAPIToken = pintoAPIToken;
        this.ideasChannelId = ideasChannelId;
        this.pintoBotLink = pintoBotLink;
        this.pintoPromptsUrl = pintoPromptsUrl;
        this.webappApiUrl = webappApiUrl;
        this.webappUser = webappUser;
        this.webappPassword = webappPassword;
    }


    public void createPost(Post post) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String answer = getPromptByPost(post.getFirstPrompt());

        if (!answer.equals("NOPROMPT")) {
            Matcher matcher = JSON_BLOCK.matcher(answer);
            if (matcher.find()) {
                answer = matcher.group(1).trim();
            }
            JsonEntityFromGemini jsonEntityFromGemini = null;
            try {
                jsonEntityFromGemini = objectMapper.readValue(answer, JsonEntityFromGemini.class);
            } catch (Exception e) {
                answer = getPromptByPost(post.getFirstPrompt());
                matcher = JSON_BLOCK.matcher(answer);
                if (matcher.find()) {
                    answer = matcher.group(1).trim();
                }
                try {
                    jsonEntityFromGemini = objectMapper.readValue(answer, JsonEntityFromGemini.class);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed Request to Gemini");
                }
            }

            //todo получить id
            String promptId;
            try {
                promptId = getId(jsonEntityFromGemini.getPrompt());
            } catch (Exception ex) {
                throw new RuntimeException("failed to get prompt id");

            }
            String postLink = pintoBotLink + promptId;

            post.setFirstPrompt(jsonEntityFromGemini.getPrompt());
            post.setCallToAction(postLink);
            post.setChannelId(ideasChannelId);
            post.setTitle(jsonEntityFromGemini.getTitlefoto());

            sendToWebapp(jsonEntityFromGemini, post.getFirstMedia());
            postRepo.save(post);
        }
    }



    public String getPromptByPost(String postText) throws JsonProcessingException {
        GPTResponse gptResponse = chatGPTClient.sendTextMessage(21312L, postText);
        return gptResponse.getAnswer();


//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//        HttpPost request = new HttpPost("https://api.replicate.com/v1/models/google/gemini-3.1-pro/predictions");
//        request.addHeader("Authorization", "Bearer " + REPLICATE_KEY);
//        request.addHeader("Content-Type", "application/json");
//        request.addHeader("Prefer", "wait");
//        GeminiGenerateEntity entity = new GeminiGenerateEntity();
//        InputForGemini input = new InputForGemini();
//        input.setPrompt(postText);
//        input.setSystem_instruction("");
//
//        entity.setInput(input);
//
//        try {
//            request.setEntity(new StringEntity(objectMapper.writeValueAsString(entity), StandardCharsets.UTF_8));
//            String id = sendRequest(request);
//            return checkResult(id);
//        } catch (Exception e) {
//            throw new RuntimeException();
//        }
    }

    private String sendRequest(HttpPost request) {
        System.out.println("sending request to gemini...");
        String str = "";
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                HttpEntity responseEntity = response.getEntity();
                try (InputStream ins = responseEntity.getContent()) {
                    byte[] bytes1 = ins.readAllBytes();
                    str = new String(bytes1, StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode obj = mapper.readTree(str);
                    return obj.get("id").textValue();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private String checkResult(String id) {
        System.out.println("get result from gemini...");
        HttpGet request2 = new HttpGet("https://api.replicate.com/v1/predictions/" + id);
        request2.addHeader("Authorization", "Bearer " + REPLICATE_KEY);
        request2.addHeader("Content-Type", "application/json");
        StringBuilder result = new StringBuilder();
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            LocalDateTime now = LocalDateTime.now();
            label:
            while (true) {
                LocalDateTime now1 = LocalDateTime.now();
                long between = ChronoUnit.MINUTES.between(now1, now);
                if (Math.abs(between) > 3) {
                    throw new RuntimeException();
                }
                try (CloseableHttpResponse response = httpclient.execute(request2)) {
                    HttpEntity responseEntity = response.getEntity();

                    try (InputStream ins = responseEntity.getContent()) {
                        byte[] bytes1 = ins.readAllBytes();
                        String str = new String(bytes1, StandardCharsets.UTF_8);
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode obj = mapper.readTree(str);
                        JsonNode jsonNode = obj.get("status");
                        String status = jsonNode.textValue();
                        if (status.equals("succeeded")) {
                            jsonNode = obj.get("output");
                            for (JsonNode node : jsonNode) {
                                result.append(node.textValue());
                            }
                            break label;
                        } else if (status.equals("failed")) {
                            throw new RuntimeException();
                        }
                    }
                }
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        System.out.println("Success generation prompt");
        return result.toString();
    }

    private String getId(String prompt) throws IOException {
        String url = pintoPromptsUrl;

        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost request = new HttpPost(url);

        request.setHeader("Authorization", pintoAPIToken);
        request.setHeader("Content-Type", "text/plain");

        StringEntity entity = new StringEntity(prompt, StandardCharsets.UTF_8);
        request.setEntity(entity);

        CloseableHttpResponse response = client.execute(request);

        String json = EntityUtils.toString(response.getEntity());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        String id = node.get("id").asText();

        response.close();
        client.close();
        return id;
    }

    private void sendToWebapp(JsonEntityFromGemini jsonEntityFromGemini, String link) {
        String url = webappApiUrl;
        String basic = Base64.getEncoder().encodeToString(
                (webappUser + ":" + webappPassword).getBytes(StandardCharsets.UTF_8));

        ObjectMapper objectMapper = new ObjectMapper();
        PintoStyleWebAppEntity pintoStyleWebAppEntity = new PintoStyleWebAppEntity();
        pintoStyleWebAppEntity.setPrompt(jsonEntityFromGemini.getPrompt());
        pintoStyleWebAppEntity.setName(jsonEntityFromGemini.getName());
        pintoStyleWebAppEntity.setGender(jsonEntityFromGemini.getGender());
        pintoStyleWebAppEntity.setPreviewUrl(link);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("Authorization", "Basic " + basic);
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(pintoStyleWebAppEntity), ContentType.APPLICATION_JSON));

            client.execute(post);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
