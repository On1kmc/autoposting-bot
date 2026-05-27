package com.ivanov.AutopostingBot.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
@Setter
@Component
public class FilesUtils {

    private volatile LinkedBlockingQueue<Pair<File, LocalDateTime>> files;

    public FilesUtils() {
        files = new LinkedBlockingQueue<>();
    }

    @SneakyThrows
    @Scheduled(fixedDelay = 5000)
    public void deleteFiles() {
        Pair<File, LocalDateTime> take = files.peek();
        if (take != null) {
            if (take.getSecond().isBefore(LocalDateTime.now())) {
                take.getFirst().delete();
                files.poll();
            }
        }
    }


    public void downloadFile(String url, String savePath) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet getFileRequest = new HttpGet(url);

            HttpResponse response = httpClient.execute(getFileRequest);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (InputStream inputStream = entity.getContent();
                         FileOutputStream outputStream = new FileOutputStream(savePath)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                } else {
                    throw new RuntimeException("FAILED");
                }
            } else {
                throw new RuntimeException("FAILED");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
