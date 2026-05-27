package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.repo.PostRepo;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.MenuGenerator;
import com.ivanov.AutopostingBot.utils.PostUtils;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

@Component
public class HourHandler implements Handler {
    private final PostRepo postRepo;
    private final MenuGenerator menuGenerator;
    private final PostUtils postUtils;

    public HourHandler(PostRepo postRepo, MenuGenerator menuGenerator, PostUtils postUtils) {
        this.postRepo = postRepo;
        this.menuGenerator = menuGenerator;
        this.postUtils = postUtils;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().startsWith("HOUR");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String[] s = update.getMessageText().split(" ");
        int hour = Integer.parseInt(s[2]);
        LocalDate date = LocalDate.from(formatter.parse(s[1]));
        List<Post> allByDate = postRepo.findAllByDateAndChannelId(date, postUtils.getPosts().get(update.getUserId()).getChannelId());
        SendMessage editMessage = getMessage(update);
        editMessage.setReplyMarkup(menuGenerator.getAvailableTime(allByDate, hour, date));
        editMessage.setText("Выберите время:");
        return List.of(getDeleteMessage(update), editMessage);
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        return List.of();
    }
}
