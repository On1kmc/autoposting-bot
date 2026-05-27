package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.utils.CalendarUtil;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.MenuGenerator;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.List;

@Component
public class AddPostHandler implements Handler {
    private final CalendarUtil calendarUtil;
    private final MenuGenerator menuGenerator;

    public AddPostHandler(CalendarUtil calendarUtil, MenuGenerator menuGenerator) {
        this.calendarUtil = calendarUtil;
        this.menuGenerator = menuGenerator;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().equals("ADDPOST");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        if (!update.isBad()) {
            EditMessageText editMessage = getEditMessage(update);
            editMessage.setText("В какой канал будет пост?");
            editMessage.setReplyMarkup(menuGenerator.getChannelsButtons());
            return List.of(editMessage);
        }
      return List.of();
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        return List.of();
    }
}
