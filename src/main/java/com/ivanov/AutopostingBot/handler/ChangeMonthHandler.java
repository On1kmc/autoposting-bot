package com.ivanov.AutopostingBot.handler;


import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.utils.CalendarUtil;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.PostUtils;
import com.ivanov.AutopostingBot.utils.TelegramType;
import lombok.SneakyThrows;
import org.joda.time.LocalDate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ChangeMonthHandler implements Handler { // перехватчик кнопки смены месяца в календаре

    private final CalendarUtil calendarUtil;
    private final PostUtils postUtils;

    public ChangeMonthHandler(CalendarUtil calendarUtil, PostUtils postUtils) {
        this.calendarUtil = calendarUtil;
        this.postUtils = postUtils;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack)
                && (update.getUpdate().getCallbackQuery().getData().startsWith(">")
                || update.getUpdate().getCallbackQuery().getData().startsWith("<"));
    }

    @SneakyThrows
    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", new Locale("ru"));
        Post post = postUtils.getPosts().get(update.getUserId());

        String[] split = update.getMessageText().split(" ");
        Date date;
        String callback;
        try {
            date = formatter.parse(split[2]);
            callback = split[1];
        } catch (ParseException e) {
            date = formatter.parse(split[3]);
            callback = split[1] + " " + split[2];
        }

        LocalDate localDate = new LocalDate(date);
        if (update.getMessageText().startsWith(">")) {
            localDate = localDate.plusMonths(1);
        } else {
            localDate = localDate.minusMonths(1);
        }
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.enableHtml(true);
        editMessageText.setMessageId(update.getUpdate().getCallbackQuery().getMessage().getMessageId());
        editMessageText.setChatId(update.getUpdate().getCallbackQuery().getMessage().getChatId());
        editMessageText.setReplyMarkup(calendarUtil.generateKeyboard(localDate, callback, post.getChannelId()));
        editMessageText.setText("Выберите дату");
        return Collections.singletonList(editMessageText);
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        return List.of();
    }
}
