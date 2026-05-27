package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.utils.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Component
public class FirstPromptHandler implements Handler {
    private final PostUtils postUtils;
    private final MenuGenerator menuGenerator;

    public FirstPromptHandler(PostUtils postUtils, MenuGenerator menuGenerator) {
        this.postUtils = postUtils;
        this.menuGenerator = menuGenerator;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        States currentState = getCurrentState(update);
        return currentState != null && currentState.equals(States.INPUT_FIRSTPROMPT) && update.getTelegramType().equals(TelegramType.Text);
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        SendMessage message = getMessage(update);
        message.setText("Загрузите фото для публикации:");
        return List.of(message);
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        Post post = postUtils.getPosts().get(update.getUserId());
        post.setFirstPrompt(update.getMessageText());
        putState(update, States.FIRST_MEDIA);
        return List.of();
    }
}
