package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.model.Post;
import com.ivanov.AutopostingBot.repo.PostRepo;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.MenuGenerator;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DeletePostHandler implements Handler{
    private final PostRepo postRepo;
    private final MenuGenerator menuGenerator;

    public DeletePostHandler(PostRepo postRepo, MenuGenerator menuGenerator) {
        this.postRepo = postRepo;
        this.menuGenerator = menuGenerator;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().startsWith("DELETEPOST");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        int id = Integer.parseInt(update.getMessageText().substring(11));
        Post post = postRepo.findById(id).get();
        postRepo.delete(post);
        int hour = post.getTime().getHour();
        LocalDate date = post.getDate();
        List<Post> allByDate = postRepo.findAllByDateAndChannelId(date, post.getChannelId());
        SendMessage message = getMessage(update);
        message.setReplyMarkup(menuGenerator.getAvailableTime(allByDate, hour, date));
        message.setText("Выберите время:");
        return List.of(message);
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {

        return List.of(getAnswerCallbackQuery(update, "Пост удален."), getDeleteMessage(update));
    }
}
