package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.MenuGenerator;
import com.ivanov.AutopostingBot.utils.TelegramType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.List;

@Component
public class MainHandler implements Handler {
    private final MenuGenerator menuGenerator;

    public MainHandler(MenuGenerator menuGenerator) {
        this.menuGenerator = menuGenerator;
    }

    @Override
    public boolean condition(ClassifiedUpdate update) {
        return update.getTelegramType().equals(TelegramType.CallBack) && update.getMessageText().equals("MAIN");
    }

    @Override
    public List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update) {
        EditMessageText editMessage = getEditMessage(update);
        editMessage.setText("Управление постингом Pinto Photo");
        editMessage.setReplyMarkup(menuGenerator.getMainMenu());
        return List.of(editMessage);
    }

    @Override
    public List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update) {
        return List.of();
    }
}
