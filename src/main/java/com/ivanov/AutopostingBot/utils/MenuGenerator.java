package com.ivanov.AutopostingBot.utils;



import com.ivanov.AutopostingBot.model.MainTag;
import com.ivanov.AutopostingBot.model.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class MenuGenerator {

    private final long ideasChannelId;
    private final long businessChannelId;

    public MenuGenerator(@Value("${channel.ideas.id}") long ideasChannelId,
                         @Value("${channel.business.id}") long businessChannelId) {
        this.ideasChannelId = ideasChannelId;
        this.businessChannelId = businessChannelId;
    }

    private InlineKeyboardButton getButton(String name, String callback) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(name);
        button.setCallbackData(callback);
        return button;
    }


    public InlineKeyboardMarkup getMainMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("Добавить пост", "ADDPOST"));
        buttons.add(line2);

        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getNewPostMenu(int hour) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("Еще пост", "AGAINPOST " + hour));
        buttons.add(line2);

        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getAvailableTime(List<Post> allByDate, int hour, LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<LocalTime> list = allByDate.stream().map(Post::getTime).toList();
        List<LocalTime> allTimes = new ArrayList<>();
        allTimes.add(LocalTime.of(hour, 3));
        allTimes.add(LocalTime.of(hour, 18));
        allTimes.add(LocalTime.of(hour, 32));
        allTimes.add(LocalTime.of(hour, 47));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        for (int i = 0; i < allTimes.size(); i = i + 2) {
            List<InlineKeyboardButton> line1 = new ArrayList<>();
            LocalTime localTime = allTimes.get(i);
            if (list.contains(localTime)) {
                line1.add(getButton("✅" + formatter.format(localTime), "SHOWPOST "  + localTime));
            } else {
                line1.add(getButton(formatter.format(localTime), "POSTTIME " + localTime));
            }
            localTime = allTimes.get(i+1);
            if (list.contains(localTime)) {
                line1.add(getButton("✅" + formatter.format(localTime), "SHOWPOST "  + localTime));
            } else {
                line1.add(getButton(formatter.format(localTime), "POSTTIME " + localTime));
            }
            buttons.add(line1);
        }
        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("Назад", "POSTDATE " + date));
        buttons.add(line2);


        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public ReplyKeyboard getPostButtons(Post post) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("❌ Удалить пост", "DELETEPOST " + post.getId()));
        buttons.add(line2);

        List<InlineKeyboardButton> line3 = new ArrayList<>();
        line3.add(getButton("Назад", "HOUR " + post.getDate() + " " + post.getTime().getHour()));
        buttons.add(line3);

        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getTimes(LocalDate date) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        for (int i = 6; i <= 23; i = i + 4) {
            List<InlineKeyboardButton> line1 = new ArrayList<>();
            line1.add(getButton(String.valueOf(i), "HOUR " + date + " " + i));
            if (i < 23) {
                line1.add(getButton(String.valueOf(i + 1), "HOUR " + date + " " + (i + 1)));
            }
            if (i < 22) {
                line1.add(getButton(String.valueOf(i+2), "HOUR " + date + " " + (i+2)));
            }
            if (i < 21) {
                line1.add(getButton(String.valueOf(i+3), "HOUR " + date + " " + (i+3)));
            }
            buttons.add(line1);
        }
        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("Назад", "CHANNEL"));
        buttons.add(line2);


        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public ReplyKeyboard getTagMenu(List<MainTag> tags) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        for (int i = 0; i < tags.size(); i = i + 3) {
            MainTag mainTag = tags.get(i);
            List<InlineKeyboardButton> line1 = new ArrayList<>();
            line1.add(getButton(mainTag.getTitle(), "TAG "  + mainTag.getId()));

            if (i < tags.size() - 1) {
                mainTag = tags.get(i + 1);
                line1.add(getButton(mainTag.getTitle(), "TAG "  + mainTag.getId()));
            }
            if (i < tags.size() - 2) {
                mainTag = tags.get(i + 2);
                line1.add(getButton(mainTag.getTitle(), "TAG "  + mainTag.getId()));
            }

            buttons.add(line1);
        }
        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("В начало", "ADDPOST"));
        buttons.add(line2);
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public ReplyKeyboard getStopInputButton() {
        ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
        replyKeyboard.setIsPersistent(false);
        replyKeyboard.setResizeKeyboard(true);
        replyKeyboard.setOneTimeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        // Создаем строки клавиатуры
        KeyboardRow row1 = new KeyboardRow();

        // Создаем кнопки
        KeyboardButton button1 = new KeyboardButton("Закончить ввод");
        row1.add(button1);
        rows.add(row1);
        replyKeyboard.setKeyboard(rows);
        return replyKeyboard;
    }

    public InlineKeyboardMarkup getChannelsButtons() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("Идеи", "CHANNEL " + ideasChannelId));
        line2.add(getButton("Бизнес", "CHANNEL " + businessChannelId));
        buttons.add(line2);

        List<InlineKeyboardButton> line3 = new ArrayList<>();
        line3.add(getButton("Назад", "MAIN"));
        buttons.add(line3);

        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getModeMenu() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(getButton("Фото по тексту", "MODE GOD"));
        buttons.add(line2);

        List<InlineKeyboardButton> line3 = new ArrayList<>();
        line3.add(getButton("Образы и стили", "MODE STYLE")); //todo добавить везде
        line3.add(getButton("Режим Бога", "MODE DESIGN"));
        buttons.add(line3);

        List<InlineKeyboardButton> line4 = new ArrayList<>();
        line4.add(getButton("Режим Бога по реф. фото", "MODE DESIGN2"));
        buttons.add(line4);

        List<InlineKeyboardButton> line5 = new ArrayList<>();
        line5.add(getButton("Творчество", "MODE ART"));
        line5.add(getButton("Творчество Pro", "MODE PROART"));
        buttons.add(line5);


//        List<InlineKeyboardButton> line55 = new ArrayList<>();
//        line55.add(getButton("Создание музыки", "MODE MUSIC"));
//        buttons.add(line55);



        List<InlineKeyboardButton> line6 = new ArrayList<>();
        line6.add(getButton("Оживить фото", "MODE ANIMATION")); //todo добавить везде
        line6.add(getButton("Видео по тексту", "MODE VIDEO"));
        buttons.add(line6);

        List<InlineKeyboardButton> line7 = new ArrayList<>();
        line7.add(getButton("Парные фото", "MODE TOGPHOTO"));
        line7.add(getButton("Фото по Фото", "MODE P2P"));
        buttons.add(line7);

        List<InlineKeyboardButton> line8 = new ArrayList<>();
        line8.add(getButton("Назад", "MAIN"));
        line8.add(getButton("Пропустить", "MODE NULL"));
        buttons.add(line8);

        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }

    public ReplyKeyboard getLinkForPost(String link) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        List<InlineKeyboardButton> line6 = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setUrl(link);
        inlineKeyboardButton.setText("\uD83D\uDD25Сделать такое же фото\uD83D\uDD25");
        line6.add(inlineKeyboardButton);
        buttons.add(line6);

        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
    }
}
