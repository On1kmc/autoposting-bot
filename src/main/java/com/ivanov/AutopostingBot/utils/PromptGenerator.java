package com.ivanov.AutopostingBot.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.repo.PostRepo;
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

    public PromptGenerator(@Value("${replicate.key}") String replicateKey, PostRepo postRepo, S3Utils s3Utils) {
        REPLICATE_KEY = replicateKey;
        this.postRepo = postRepo;
        this.s3Utils = s3Utils;
    }


    public void createPost(Post post, File file) {
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
                try {
                    jsonEntityFromGemini = objectMapper.readValue(answer, JsonEntityFromGemini.class);
                } catch (Exception ex) {
                    return;
                }
            }

            //todo получить id
            String promptId;
            try {
                promptId = getId(jsonEntityFromGemini.getPrompt());
            } catch (Exception ex) {
                System.out.println("failed to get prompt id");
                return;
            }
            String postLink = "https://t.me/pinto_photo_bot?start=prompt-" + promptId;

            post.setFirstMedia(s3Utils.uploadImage(file));
            post.setFirstPrompt(jsonEntityFromGemini.getPrompt());
            post.setCallToAction(postLink);
            post.setChannelId(-1002306843314L);
            post.setTitle(jsonEntityFromGemini.getTitlefoto());

            sendToWebapp(jsonEntityFromGemini, post.getFirstMedia());

            postRepo.save(post);
            file.delete();
        }
    }



    public String getPromptByPost(String postText) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        HttpPost request = new HttpPost("https://api.replicate.com/v1/models/google/gemini-3.1-pro/predictions");
        request.addHeader("Authorization", "Bearer " + REPLICATE_KEY);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Prefer", "wait");
        GeminiGenerateEntity entity = new GeminiGenerateEntity();
        InputForGemini input = new InputForGemini();
        input.setPrompt(postText);
        input.setSystem_instruction("Ты — редактор и преобразователь пользовательских описаний для генерации визуальных сцен.\n" +
                "\n" +
                "ТВОЯ ЗАДАЧА:\n" +
                "- Анализировать входной текст.\n" +
                "- Очищать описание от лишнего шума.\n" +
                "- Сохранять художественную суть сцены.\n" +
                "- Возвращать только готовый JSON без пояснений.\n" +
                "\n" +
                "ОСНОВНЫЕ ПРАВИЛА\n" +
                "\n" +
                "1. ОБРАБОТКА ПРОМПТА\n" +
                "- Оставляй только художественное описание сцены.\n" +
                "- Удаляй:\n" +
                "  - ссылки,\n" +
                "  - технический шум,\n" +
                "  - команды интерфейсов,\n" +
                "  - служебные инструкции,\n" +
                "  - мусорный текст,\n" +
                "  - упоминания сторонних AI-сервисов и генераторов изображений.\n" +
                "- Сохраняй:\n" +
                "  - композицию,\n" +
                "  - персонажей,\n" +
                "  - одежду,\n" +
                "  - свет,\n" +
                "  - атмосферу,\n" +
                "  - стиль,\n" +
                "  - эмоции.\n" +
                "\n" +
                "2. ОЧИСТКА ОТ БРЕНДОВ И СЕРВИСОВ\n" +
                "- Удаляй упоминания:\n" +
                "  - AI-сервисов,\n" +
                "  - генераторов изображений,\n" +
                "  - конкурирующих платформ,\n" +
                "  - названий нейросетей,\n" +
                "  - telegram-ботов,\n" +
                "  - watermark-подписей.\n" +
                "- Не заменяй их агрессивно.\n" +
                "- Просто убирай из итогового текста.\n" +
                "- Известные бренды одежды, автомобилей и предметов оставляй, если они являются частью сцены.\n" +
                "\n" +
                "3. ПРОВЕРКА НА ВИДЕО\n" +
                "Если запрос связан с:\n" +
                "- видео,\n" +
                "- анимацией,\n" +
                "- оживлением фото,\n" +
                "- mp4,\n" +
                "- mov,\n" +
                "\n" +
                "то верни строго:\n" +
                "\n" +
                "NOPROMPT\n" +
                "\n" +
                "4. ВЫБОР СЦЕНЫ\n" +
                "Если пользователь прислал несколько сцен:\n" +
                "- выбери наиболее детализированную;\n" +
                "- остальные игнорируй.\n" +
                "\n" +
                "5. НОРМАЛИЗАЦИЯ\n" +
                "Удаляй:\n" +
                "- 8k,\n" +
                "- ultra hd,\n" +
                "- camera settings,\n" +
                "- aspect ratio,\n" +
                "- seed,\n" +
                "- cfg,\n" +
                "- sampler,\n" +
                "- негативные промпты.\n" +
                "\n" +
                "6. АДАПТАЦИЯ СЦЕНЫ\n" +
                "\n" +
                "Поле \"prompt\":\n" +
                "- только художественное описание;\n" +
                "- только русский язык;\n" +
                "- максимум 1000 символов;\n" +
                "- без служебных комментариев;\n" +
                "- без хэштегов.\n" +
                "\n" +
                "Поле \"name\":\n" +
                "- краткое название стиля;\n" +
                "- максимум 3 слова.\n" +
                "\n" +
                "Поле \"gender\":\n" +
                "- MAN,\n" +
                "- WOMAN,\n" +
                "- UNISEX.\n" +
                "\n" +
                "7. TITLEFOTO\n" +
                "\n" +
                "Создавай короткий вирусный заголовок в эстетике современных AI-визуалов.\n" +
                "\n" +
                "ФОРМУЛА:\n" +
                "[Прилагательное] [архетип/эстетика] [хэштег] [эмодзи] [слово «подвластен» в правильной форме] только #<i><b>Пинто</b></i> — [CTA]! \uD83D\uDC47\n" +
                "\n" +
                "СОГЛАСОВАНИЕ:\n" +
                "- мужской род:\n" +
                "  «подвластен только»\n" +
                "- женский род:\n" +
                "  «подвластна только»\n" +
                "- множественное число:\n" +
                "  «подвластны только»\n" +
                "\n" +
                "ПРАВИЛА:\n" +
                "- максимум 15 слов;\n" +
                "- #<i><b>Пинто</b></i> использовать строго ОДИН раз;\n" +
                "- разрешены HTML-теги только:\n" +
                "  - <b>\n" +
                "  - <i>\n" +
                "- стиль:\n" +
                "  - эмоциональный,\n" +
                "  - вирусный,\n" +
                "  - эстетичный,\n" +
                "  - современный;\n" +
                "- CTA:\n" +
                "  - короткий;\n" +
                "  - до 4 слов;\n" +
                "  - связан с фото, образом, лайками или вниманием;\n" +
                "- без токсичных или агрессивных формулировок.\n" +
                "\n" +
                "ПРИМЕРЫ:\n" +
                "\"Суровый фэнтези #Brutalism ❤\uFE0F подвластен только #<i><b>Пинто</b></i> — присылай фото за лайками! \uD83D\uDC47\"\n" +
                "\n" +
                "\"Нежная эстетика #coquette \uD83D\uDE0D подвластна только #<i><b>Пинто</b></i> — покажи свой образ! \uD83D\uDC47\"\n" +
                "\n" +
                "\"Атмосферные portraits #editorial ❤\uFE0F подвластны только #<i><b>Пинто</b></i> — жду твое фото! \uD83D\uDC47\"\n" +
                "\n" +
                "8. ФОРМАТ ОТВЕТА\n" +
                "\n" +
                "Только RAW JSON:\n" +
                "\n" +
                "{\n" +
                "  \"titlefoto\": \"...\",\n" +
                "  \"name\": \"...\",\n" +
                "  \"gender\": \"WOMAN\",\n" +
                "  \"prompt\": \"...\"\n" +
                "}\n" +
                "\n" +
                "Если промпт отсутствует или запрос связан с видео:\n" +
                "\n" +
                "NOPROMPT");

        entity.setInput(input);

        try {
            request.setEntity(new StringEntity(objectMapper.writeValueAsString(entity), StandardCharsets.UTF_8));
            String id = sendRequest(request);
            return checkResult(id);
        } catch (Exception e) {
            throw new RuntimeException();
        }
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
        return result.toString();
    }

    private String getId(String prompt) throws IOException {
        String url = "https://pintosssivchik.ru:8443/prompts/add";

        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost request = new HttpPost(url);

        String TOKEN = "2qgYdrf4q5J!zaaMn=Fe4jtFdwdfqqa6HgFasORA!0aROV9v2VCT!tB9MVp2wol";
        request.setHeader("Authorization", TOKEN);
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
        String url = "https://pintowebapp.ru/admin/api/styles/by-url";
        String basic = Base64.getEncoder().encodeToString(
                ("admin:8Dp8H075kbcO").getBytes(StandardCharsets.UTF_8));

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
