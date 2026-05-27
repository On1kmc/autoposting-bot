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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class PostDateHandler implements Handler {
    private final PostUtils postUtils;
    private final MenuGenerator menuGenerator;
    private final PostRepo postRepo;

    public PostDateHandler(PostUtils postUtils, MenuGenerator menuGenerator, PostRepo postRepo) {
        this.postUtils = postUtils;
        this.menuGenerator = menuGenerator;
        this.postRepo = postRepo;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().startsWith("POSTDATE");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        Post post = postUtils.getPosts().get(update.getUserId());
        List<Post> posts = postRepo.findAllByDateAndChannelId(post.getDate(), post.getChannelId());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM", new Locale("ru"));
        String text = "";
        if (!posts.isEmpty()) {
            text = getTextForDate(posts);
        }

        EditMessageText editMessage = getEditMessage(update);
        editMessage.setText("На " + dateTimeFormatter.format(post.getDate()) + " запланировано " + posts.size() + " постов" +
                "\n" + text +
                "\n\nВыберите час:");
        editMessage.setReplyMarkup(menuGenerator.getTimes(post.getDate()));
        return List.of(editMessage);
    }

    private String getTextForDate(List<Post> posts) {
        StringBuilder stringBuilder = new StringBuilder();
        posts.sort(Comparator.comparingInt(o -> o.getTime().getHour()));
        Post post1 = posts.get(0);
        int hour = post1.getTime().getHour();
        int count = 1;
        for (int i = 1; i < posts.size(); i++) {
            Post post = posts.get(i);
            if (post.getTime().getHour() == hour) {
                count++;
            } else {
                stringBuilder.append(hour).append(" - ").append(count).append("\n");
                hour = post.getTime().getHour();
                count = 1;
            }
        }
        stringBuilder.append(hour).append(" - ").append(count);
        return stringBuilder.toString();
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        TemporalAccessor parse = formatter.parse(update.getMessageText().substring(9));
        postUtils.getPosts().get(update.getUserId()).setDate(LocalDate.from(parse));
        return List.of();
    }
}
