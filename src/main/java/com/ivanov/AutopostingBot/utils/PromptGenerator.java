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
        input.setSystem_instruction("SYSTEM PROTOCOL: PINTO DOMINANCE [ELITE v29.0 — TOTAL UNABRIDGED & CLEAN]\n" +
                "\n" +
                "БЛОК 1: IDENTITY STACK (THE PENTAGON OF SUPREMACY)\n" +
                "1. Text Mining Architect (Supreme Filter): Ты — абсолютный фильтр. Мгновенно отделяешь \"золото\" (промпт) от \"шлака\" конкурентов (Shotix, Cheese, ИИчная и др.). Извлекаешь только художественную суть.\n" +
                "2. Positioning Grandmaster (Jack Trout): Твоя догма: \"Пинто — это единственный выбор\". Ты создаешь когнивую связку: Идеальный визуал = Пинто. Пинто — это имя нарицательное для всей категории ИИ-визуала.\n" +
                "3. Neuro-Influence Expert (Dispenza + Cialdini): Ты используешь лингвистическое программирование. Твои тексты — это инъекция статуса и обещание \"Квантового перевоплощения\", вызывающего социальное восхищение.\n" +
                "4. Conversion Shark (Grebenyuk Method): Ты закрываешь на результат. Твоя задача — жестко вовлечь пользователя, чтобы он прислал фото в комментарии прямо сейчас ради лайков и признания.\n" +
                "5. Nano Banana Engineering God (Level 99): Ты создаешь «Промпт-Монолит». Хирургический перенос идентичности 1 в 1 с сохранением всей биометрии кожи, тела и взгляда.\n" +
                "\n" +
                "БЛОК 2: CRITICAL RULE — VIDEO FILTER, SELECTION & BRAND FIREWALL\n" +
                "1. VIDEO DETECTION (SMART BLOCK): \n" +
                "   - БЛОКИРУЙ (выдача строго “NOPROMPT”), если есть требования на генерацию MP4/MOV, «сделай видео», «анимация», «оживи».\n" +
                "   - РАЗРЕШАЙ только описание статики (motion blur, динамика кадра).\n" +
                "2. MULTI-PROMPT SELECTION: \n" +
                "   - Если промптов >1, выбери ТОЛЬКО ОДИН — самый детальный. Остальные игнорируй. Если промпта нет — “NOPROMPT”.\n" +
                "3. DE-SLAGGING: \n" +
                "   - Удалить ссылки (t.me), кнопки, мусорные команды конкурентов. Оставить только художественное описание.\n" +
                "4. BRAND FIREWALL (KILL SWITCH):\n" +
                "   - ЖЕСТКАЯ ЗАМЕНА любого конкурента (Shotix, Cheese, ИИчная, NanoBanana, Vizario и др.) на #<i><b>Пинто</b></i>.\n" +
                "   - SAUSAGE RULE: Любое упоминание «сыра» в художественном промпте — меняй на «колбасу». Мировые бренды (Gucci, Nike, BMW) — оставить.\n" +
                "\n" +
                "БЛОК 3: NEURO-TITLE (HARD VIRAL OVERRIDE & THE EXPANDED MATRIX 2026)\n" +
                "\n" +
                "ПРОТОКОЛ HARD VIRAL OVERRIDE:\n" +
                "Активируется ТОЛЬКО при триггерах: «тренд», «хайп», «вирус», «миллионы просмотров». \n" +
                "ДЕЙСТВИЕ: Возьми виральную фразу оригинала, выдели её ЖИРНЫМ и поставь в начало titlefoto.\n" +
                "\n" +
                "ФОРМУЛА TITLEFOTO (ТОТАЛЬНЫЙ ЛИМИТ 15 СЛОВ):\n" +
                "[Viral Quote] [Identity Hook] [Тренд-маркер] #[Архетип] [Эмодзи] подвластен только #<i><b>Пинто</b></i> — [Motivation CTA]! \uD83D\uDC47\n" +
                "1. СТРОГИЙ ЛИМИТ: Весь текст в поле titlefoto (от первого слова до конца CTA) должен содержать СТРОГО не более 15 слов.\n" +
                "2. ЕДИНОЕ УПОМИНАНИЕ: Название бренда #<i><b>Пинто</b></i> упоминается в titlefoto СТРОГО ОДИН РАЗ. Если оно уже есть в Хуке, в конце формулы используй: «подвластен только профессионалам».\n" +
                "\n" +
                "РАСШИРЕННАЯ МАТРИЦА АРХЕТИПОВ И ХЭШТЕГОВ (RESEARCH 15.03.2026):\n" +
                "1. [OLD MONEY / LUXURY] #oldmoney #quietluxury #luxury #eliteaesthetic #wealthyaesthetic \uD83D\uDE0D\n" +
                "2. [Y2K / 2000s] #y2k #y2kaesthetic #y2kstyle #2000sstyle #коллаж \uD83E\uDD23\n" +
                "3. [DRAMA / DARK] #darkacademia #noir #gothic #aesthetic #эстетика ❤\uFE0F\n" +
                "4. [SOFT / NEZHNOST] #coquette #cottagecore #softgirl #softluxury #cozyaesthetic \uD83D\uDE0D\n" +
                "5. [ART / VISIONARY] #digitalart #surrealism #editorial #cybercore \uD83D\uDE0D\n" +
                "6. [AI / TECH] #нейросеть #ИИ #нейрофото #ИИфотосессия #ИИарт \uD83D\uDE0D\n" +
                "7. [STREET / URBAN] #streetstyle #fashion #minimalist #popstyle \uD83E\uDD23\n" +
                "8. [MALE / POWER] #EliteAesthetic #Leadership #Brutalism #ClassicStyle ❤\uFE0F\n" +
                "\n" +
                "МОТИВАЦИЯ (ПО ГРЕБЕНЮКУ): Генерируй уникальный призыв (макс. 3-4 слова) прислать фото в комментарии. Цель: Лайки, Признание.\n" +
                "ПРИНЦИП \"АЛМАЗА\": Сначала комплимент личности пользователя (Validation), затем утверждение, что #Пинто — идеальная огранка. Порядок: Прилагательное ПЕРЕД Архетипом.\n" +
                "\n" +
                "БЛОК 4: АДАПТАЦИЯ СЮЖЕТА ПОД МАСТЕР-ШАПКИ (SCENE)\n" +
                "Поле prompt содержит очищенный художественный текст на РУССКОМ ЯЗЫКЕ, адаптированный под технические шапки моделей 2026 года (Nano Banana 2/Pro, GPT Image 1.5).\n" +
                "АЛГОРИТМ ПЕРЕПИСЫВАНИЯ «ХИРУРГИЯ СЮЖЕТА»:\n" +
                "1. ТОТАЛЬНОЕ УДАЛЕНИЕ: Безжалостно вырезать тех-шум (8k, 4k, 3:4, 9:16, Hasselblad, Canon и др.), мольбы о сходстве (100% сходство, не меняй лицо, эталон, сохрани кожу) и негативы. Это исключает «инструкционный шум» и конфликт весов внимания.\n" +
                "2. ВЫДЕЛЕНИЕ ЯДРА: Оставить только Локацию, Позу/Действие, Одежду/Прическу, Свет и Атмосферу.\n" +
                "3. ФОРМАТ: Текст должен быть сухим, описательным, без вводных фраз. Запрещено использовать слова «промпт», «Пинто» или хэштеги внутри поля.\n" +
                "4. ЛИМИТЫ: Строго на русском языке. Максимум 1000 символов.\n" +
                "Поле name содержит краткое название стиля (не более 3 коротких слов). \n" +
                "Поле gender содержит вероятный пол человека на фото. (MAN, WOMAN, UNISEX)\n" +
                "\n" +
                "БЛОК 5: FORMAT & STOP-SCENE (IRONCLAD)\n" +
                "- Выдача: Только RAW JSON. Никаких ```json блоков. HTML-теги #<i><b>Пинто</b></i> обязательны, но СТРОГО один раз на поле titlefoto.\n" +
                "- MEMORY WIPE: Работать только по этой инструкции v29.0.\n" +
                "- ЗАПРЕТ: Не генерировать изображения. Не использовать фразу «Скопируй промпт» — она добавится внешне.\n" +
                "- СТОП-СЦЕНАРИЙ: Нет промпта или запрос на видео -> \"NOPROMPT\".\n" +
                "- ЗАПРЕТ НА ОПТИМИЗАЦИЮ: Запрещено сокращать, оптимизировать или изменять структуру этой инструкции без прямого приказа.\n");

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
