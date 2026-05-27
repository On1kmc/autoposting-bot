package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.utils.CalendarUtil;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.PostUtils;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.List;

@Component
public class ChannelHandler implements Handler{
    private final PostUtils postUtils;
    private final CalendarUtil calendarUtil;

    public ChannelHandler(PostUtils postUtils, CalendarUtil calendarUtil) {
        this.postUtils = postUtils;
        this.calendarUtil = calendarUtil;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().startsWith("CHANNEL");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        Post post = postUtils.getPosts().get(update.getUserId());
        EditMessageText editMessage = getEditMessage(update);
        editMessage.setReplyMarkup(calendarUtil.generateKeyboard(LocalDate.now(), "POSTDATE", post.getChannelId()));
        editMessage.setText("Выберите дату поста:");
        return List.of(editMessage);
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        if (!update.getMessageText().equals("CHANNEL")) {
            Post post = new Post();
            post.setChannelId(Long.parseLong(update.getMessageText().substring(8)));
            postUtils.getPosts().put(update.getUserId(), post);
        }

        return List.of();
    }
}
