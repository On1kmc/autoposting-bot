package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.utils.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class MediaHandler implements Handler {
    private final PostUtils postUtils;
    private final MenuGenerator menuGenerator;
    private final PromptGenerator promptGenerator;
    private final S3Utils s3Utils;

    public MediaHandler(PostUtils postUtils, MenuGenerator menuGenerator, PromptGenerator promptGenerator, S3Utils s3Utils) {
        this.postUtils = postUtils;
        this.menuGenerator = menuGenerator;
        this.promptGenerator = promptGenerator;
        this.s3Utils = s3Utils;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return getCurrentState(update) == States.FIRST_MEDIA && update.getTelegramType().equals(TelegramType.Photo);
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
     return List.of();
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        removeState(update);
        Post post = postUtils.getPosts().get(update.getUserId());
        update.setBad(true);
        SendMessage message = getMessage(update);
        message.setText("Пост генерируется... скоро он появится в расписании.");
        message.setReplyMarkup(menuGenerator.getNewPostMenu(post.getTime().getHour()));

        CompletableFuture.runAsync(() -> promptGenerator.createPost(post, update.getFile()));
        return List.of(message);
    }
}
