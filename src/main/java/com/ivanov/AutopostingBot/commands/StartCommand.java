package com.ivanov.AutopostingBot.commands;


import com.ivanov.AutopostingBot.utils.MenuGenerator;
import com.ivanov.AutopostingBot.utils.UserApiService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
public class StartCommand extends ServiceCommand {

    private final MenuGenerator menuGenerator;
    private final UserApiService userApiService;
    private final String ideasChannelLink;

    StartCommand(MenuGenerator menuGenerator, UserApiService userApiService,
                 @Value("${channel.ideas.link}") String ideasChannelLink) {
        super("start", "Начать работу");
        this.menuGenerator = menuGenerator;
        this.userApiService = userApiService;
        this.ideasChannelLink = ideasChannelLink;
    }

    @SneakyThrows
    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        if (userApiService.hasPostingRightCached(chat.getId())) {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText("Управление постингом Pinto Photo");
            sendMessage.setReplyMarkup(menuGenerator.getMainMenu());
            sendMessage.setChatId(chat.getId());
            absSender.execute(sendMessage);
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText("Канал с идеями тут: " + ideasChannelLink);
            sendMessage.setChatId(chat.getId());
            absSender.execute(sendMessage);
        }
    }

}
