package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.repo.PostRepo;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.MenuGenerator;
import com.ivanov.AutopostingBot.utils.PostUtils;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ShowPostHandler implements Handler {
    private final PostUtils postUtils;
    private final PostRepo postRepo;
    private final MenuGenerator menuGenerator;

    public ShowPostHandler(PostUtils postUtils, PostRepo postRepo, MenuGenerator menuGenerator) {
        this.postUtils = postUtils;
        this.postRepo = postRepo;
        this.menuGenerator = menuGenerator;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().startsWith("SHOWPOST");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        Post post = postUtils.getPosts().get(update.getUserId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime time = LocalTime.parse(update.getMessageText().substring(9), formatter);
        List<Post> allByDate = postRepo.findAllByDateAndChannelId(post.getDate(), post.getChannelId());
        Optional<Post> any = allByDate.stream().filter(s -> s.getTime().equals(time)).findAny();
        if (any.isPresent()) {
            return postUtils.getMessagesForPost(any.get(), update.getUserId(), true);
        }
        return List.of();
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        return List.of();
    }
}
