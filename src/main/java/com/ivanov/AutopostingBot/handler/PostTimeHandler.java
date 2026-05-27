package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.PostUtils;
import com.ivanov.AutopostingBot.utils.States;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Component
public class PostTimeHandler implements Handler {
    private final PostUtils postUtils;

    public PostTimeHandler(PostUtils postUtils) {
        this.postUtils = postUtils;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().startsWith("POSTTIME");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        EditMessageText editMessage = getEditMessage(update);
        editMessage.setText("Введите промпт для генерации:");
        return List.of(editMessage);
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        String substring = update.getMessageText().substring(9);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime time = LocalTime.parse(substring, formatter);
        postUtils.getPosts().get(update.getUserId()).setTime(time);
        putState(update, States.INPUT_FIRSTPROMPT);
        return List.of();
    }
}
