package com.ivanov.AutopostingBot.utils;


import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class CalendarUtil { // Метод для клавиатуры с выбором даты

    public static final String IGNORE = "ignore!@#$%^&";

    public static final String[] WD = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    private final long ideasChannelId;
    private final long businessChannelId;

    public CalendarUtil(@Value("${channel.ideas.id}") long ideasChannelId,
                        @Value("${channel.business.id}") long businessChannelId) {
        this.ideasChannelId = ideasChannelId;
        this.businessChannelId = businessChannelId;
    }

    public InlineKeyboardMarkup generateKeyboard(LocalDate date, String callback, long channelId) {


        if (date == null) {
            return null;
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // row - Month and Year
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        headerRow.add(createButton("< " + callback + " " + date, "<"));
        headerRow.add(createButton(IGNORE, new SimpleDateFormat("MMMM").format(date.toDate())));
        headerRow.add(createButton("> " + callback + " " + date, ">"));
        keyboard.add(headerRow);

        // row - Days of the week
        List<InlineKeyboardButton> daysOfWeekRow = new ArrayList<>();
        for (String day : WD) {
            daysOfWeekRow.add(createButton(IGNORE, day));
        }
        keyboard.add(daysOfWeekRow);

        LocalDate firstDay = date.dayOfMonth().withMinimumValue();

        int shift = firstDay.dayOfWeek().get() - 1;
        int daysInMonth = firstDay.dayOfMonth().getMaximumValue();
        int rows = ((daysInMonth + shift) % 7 > 0 ? 1 : 0) + (daysInMonth + shift) / 7;
        for (int i = 0; i < rows; i++) {
            keyboard.add(buildRow(firstDay, shift, callback));
            firstDay = firstDay.plusDays(7 - shift);
            shift = 0;
        }

        List<InlineKeyboardButton> line2 = new ArrayList<>();
        line2.add(createButton("CHANNEL " + ideasChannelId, channelId == ideasChannelId ? "✅ Идеи" : "Идеи"));
        line2.add(createButton("CHANNEL " + businessChannelId, channelId == businessChannelId ? "✅ Бизнес" : "Бизнес"));
        keyboard.add(line2);

        List<InlineKeyboardButton> lastRow = new ArrayList<>();
        lastRow.add(createButton("MAIN", "\uD83D\uDD19В главное меню"));
        keyboard.add(lastRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    private InlineKeyboardButton createButton(String callBack, String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setCallbackData(callBack);
        button.setText(text);
        return button;
    }

    private List<InlineKeyboardButton> buildRow(LocalDate date, int shift, String callback) {
        int actualMaximum = date.dayOfMonth().getMaximumValue();
        List<InlineKeyboardButton> row = new ArrayList<>();
        int day = date.getDayOfMonth();
        for (int j = 0; j < shift; j++) {
            row.add(createButton(IGNORE, " "));
        }
        for (int j = shift; j < 7; j++) {
            if (day <= actualMaximum)    {
                if (date.toDate().before(LocalDate.now().toDate())) {
                    row.add(createButton(IGNORE, "❌"));
                    day++;
                    date = date.plusDays(1);
                    continue;
                }
                row.add(createButton(callback + " " + date, Integer.toString(day++)));
                date = date.plusDays(1);
            } else {
                row.add(createButton(IGNORE, " "));
            }
        }
        return row;
    }
}