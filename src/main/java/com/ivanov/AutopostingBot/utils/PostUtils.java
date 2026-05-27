package com.ivanov.AutopostingBot.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivanov.AutopostingBot.MyBotForPosting;
import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.repo.PostRepo;
import lombok.Getter;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@Getter
public class PostUtils {

    private final Map<Long, Post> posts = new HashMap<>();
    private final PostRepo postRepo;
    private final MenuGenerator menuGenerator;
    private final FilesUtils filesUtils;

    public PostUtils(PostRepo postRepo, MenuGenerator menuGenerator, FilesUtils filesUtils) {
        this.postRepo = postRepo;
        this.menuGenerator = menuGenerator;
        this.filesUtils = filesUtils;
    }

    public List<PartialBotApiMethod<?>> getMessagesForPost(Post post, long chatId, boolean needButtons) {
        StringBuilder text = new StringBuilder();
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setParseMode(ParseMode.HTML);

        String savePath = "/opt/bot/photo/" + post.getId() + ".jpg"; // путь, куда сохранить файл
        filesUtils.downloadFile(post.getFirstMedia(), savePath);
        filesUtils.getFiles().add(Pair.of(new File(savePath), LocalDateTime.now().plusMinutes(10)));

        sendPhoto.setPhoto(new InputFile(new File(savePath)));
        sendPhoto.setCaption(post.getTitle());


        if (needButtons) {
            sendPhoto.setReplyMarkup(menuGenerator.getPostButtons(post));
        }
        text.append("""
                    <blockquote expandable><i><b>Скопируй</b></i> #промпт - подчини реальность своему совершенству! 👇
                    <code>""").append(post.getFirstPrompt()).append("</code></blockquote>");

        SendMessage sendMessage = new SendMessage();
        sendMessage.disableWebPagePreview();
        sendMessage.setChatId(chatId);
        sendMessage.setParseMode(ParseMode.HTML);
        sendMessage.setText(text.toString());
        sendMessage.setReplyMarkup(menuGenerator.getLinkForPost(post.getCallToAction()));

        return List.of(sendPhoto, sendMessage);
    }




    @Scheduled(cron = "0,30 0-59 6-23 * * *")
    public void sendPost() {
        List<Post> allByDate = postRepo.findAllByDateAndSentIsFalse(LocalDate.now());
        LocalTime time = LocalTime.now();
        allByDate.stream().filter(s -> s.getTime().isBefore(time)).forEach(s -> {

            MyBotForPosting.getMessageStack().addAll(getMessagesForPost(s, s.getChannelId(), false));
            s.setSent(true);
            postRepo.save(s);
        });
    }
}
