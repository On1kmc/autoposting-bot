package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.repo.PostRepo;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.MenuGenerator;
import com.ivanov.AutopostingBot.utils.PostUtils;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.List;

@Component
public class AgainPostHandler implements Handler{
    private final PostUtils postUtils;
    private final PostRepo postRepo;
    private final MenuGenerator menuGenerator;

    public AgainPostHandler(PostUtils postUtils, PostRepo postRepo, MenuGenerator menuGenerator) {
        this.postUtils = postUtils;
        this.postRepo = postRepo;
        this.menuGenerator = menuGenerator;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().startsWith("AGAINPOST");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        if (!update.isBad()) {
            Post post = postUtils.getPosts().get(update.getUserId());
            int hour = Integer.parseInt(update.getMessageText().substring(10));
            List<Post> allByDate = postRepo.findAllByDateAndChannelId(post.getDate(), post.getChannelId());
            EditMessageText editMessage = getEditMessage(update);
            editMessage.setReplyMarkup(menuGenerator.getAvailableTime(allByDate, hour, post.getDate()));
            editMessage.setText("Выберите время:");
            return List.of(editMessage);
        }
        return List.of();
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        Post post = postUtils.getPosts().get(update.getUserId());
        if (post != null) {
            Post newPost = new Post();
            newPost.setChannelId(post.getChannelId());
            newPost.setDate(post.getDate());
            postUtils.getPosts().put(update.getUserId(), newPost);
        } else {
            update.setBad(true);
        }
        return List.of();
    }
}
