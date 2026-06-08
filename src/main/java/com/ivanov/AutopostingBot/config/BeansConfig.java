package com.ivanov.AutopostingBot.config;

import com.ivanov.gptClient.MyGptClient.GPTModel;
import com.ivanov.gptClient.MyGptClient.assistant.ChatGPTClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
@Configuration
public class BeansConfig {

    private final String KEY_ID;
    private final String SECRET_KEY;
    private final String REGION;
    private final String S3_ENDPOINT;
    private final String OPEN_AI_KEY;


    public BeansConfig(@Value("${s3.key}") String keyId, @Value("${s3.secret}") String secretKey,
                       @Value("${s3.region}") String region, @Value("${s3.endpoint}") String s3Endpoint,@Value("${openai.key}")  String openAiKey) {
        KEY_ID = keyId;
        SECRET_KEY = secretKey;
        REGION = region;
        S3_ENDPOINT = s3Endpoint;
        OPEN_AI_KEY = openAiKey;
    }

    @Bean
    public S3Client awsCredentials() {
        AwsCredentials credentials = AwsBasicCredentials.create(KEY_ID, SECRET_KEY);
        return S3Client.builder()
                .httpClient(ApacheHttpClient.create())
                .region(Region.of(REGION))
                .endpointOverride(URI.create(S3_ENDPOINT))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    public ChatGPTClient promptSupplier() {
        String systemMessage = "ТВОЯ ЗАДАЧА:\n" +
                "- Анализировать входной текст.\n" +
                "- Возвращать только готовый JSON без пояснений.\n" +
                "\n" +
                "ОСНОВНЫЕ ПРАВИЛА\n" +
                "\n" +
                "1. ОБРАБОТКА ВХОДЯЩЕГО ПРОМПТА\n" +
                "не требуется. Выдели из входящего сообщения промпт и верни его в поле prompt" +
                "\n"  +
                "2. Поле \"name\":\n" +
                "- краткое название стиля;\n" +
                "- максимум 3 слова.\n" +
                "\n" +
                "Поле \"gender\":\n" +
                "- MAN,\n" +
                "- WOMAN,\n" +
                "- UNISEX.\n" +
                "\n" +
                "3. TITLEFOTO\n" +
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
                "4. ФОРМАТ ОТВЕТА\n" +
                "\n" +
                "Только RAW JSON:\n" +
                "\n" +
                "{\n" +
                "  \"titlefoto\": \"...\",\n" +
                "  \"name\": \"...\",\n" +
                "  \"gender\": \"WOMAN\",\n" +
                "  \"prompt\": \"...\"\n" +
                "}\n";

        return new ChatGPTClient.Builder()
                .apiToken(OPEN_AI_KEY)
                .model(GPTModel.GPT_5_4)
                .enableSystemMessage(systemMessage)
                .build();
    }

}
