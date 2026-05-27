package com.ivanov.AutopostingBot;


import com.ivanov.AutopostingBot.commands.StartCommand;
import com.ivanov.AutopostingBot.handler.Handler;
import com.ivanov.AutopostingBot.utils.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.*;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class MyBotForPosting extends TelegramLongPollingCommandBot { // Класс бота.
    private final HandlerUtils handlerUtils; // Экземлпяр класса, перехватывающего сообщения боту для их обработки и отправки ответа.
    @Getter
    @Setter
    private static Map<Long, States> statesMap;

    private final MenuGenerator menuGenerator;

    @Getter
    private volatile static LinkedBlockingQueue<PartialBotApiMethod<?>> messageStack = new LinkedBlockingQueue<>();

        // Конструктор класса
    private final UserApiService userApiService;

    public MyBotForPosting(@Value("${bot.token}") String token, HandlerUtils handlerUtils,
               StartCommand startCommand, MenuGenerator menuGenerator, UserApiService userApiService) {

        super(token); // Обращаемся к конструктору суперкласса даем на вход токен бота.
        this.handlerUtils = handlerUtils;
        this.menuGenerator = menuGenerator;
        this.userApiService = userApiService;
        statesMap = new HashMap<>();

        register(startCommand);



        // Устанавливаем команды в меню.
        SetMyCommands setMyCommands = new SetMyCommands();
        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("start", "Перезапуск бота"));
        setMyCommands.setCommands(commandList);
        try {
            execute(setMyCommands);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "Autoposting";
    }

    @SneakyThrows
    @Override
    public void processNonCommandUpdate(Update update) { // Сюда попадают все сообщения, которые пришли боту.
                                                         // Update - событие, произошедшее в чате с ботом.

        ClassifiedUpdate classifiedUpdate = new ClassifiedUpdate(update); // создаем экземпляр нашего вспомогательного класса

        if (userApiService.hasPostingRightCached(classifiedUpdate.getUserId())) {
            Optional<Handler> handler = handlerUtils.getHandler(classifiedUpdate);// Находим нужный перехватчик сообщения
            if (handler.isPresent()) {
                if (classifiedUpdate.getTelegramType().equals(TelegramType.Photo)) {
                    classifiedUpdate.setFile(downloadPhotoFromTelegram(classifiedUpdate));
                }

                List<PartialBotApiMethod<?>> proceed = handler.get().proceed(classifiedUpdate);// обрабатываем полученное сообщение, если нужно
                if (!proceed.isEmpty()) {
                    for (PartialBotApiMethod<?> method : proceed) {
                        try {
                            conversionAndExecute(method);
                            Thread.sleep(50);
                        } catch (TelegramApiException e) {
                            System.out.println("Executing throw exception!");
                            System.out.println(e.getMessage());
                            statesMap.remove(classifiedUpdate.getUserId());
                        }
                    }

                }


                List<PartialBotApiMethod<?>> answerList = handler.get().getAnswer(classifiedUpdate);// Формируем и отдаем ответ пользователю.
                answerList.forEach(s -> {
                    try {
                        conversionAndExecute(s);
                        Thread.sleep(50);
                    } catch (TelegramApiException | InterruptedException e) {
                        System.out.println("Executing throw exception!");
                        System.out.println(e.getMessage());
                        statesMap.remove(classifiedUpdate.getUserId());
                    }
                });

            } else {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(classifiedUpdate.getUserId());
                sendMessage.setText("Что-то пошло не так. Нажмите /start для перезапуска бота.");
                conversionAndExecute(sendMessage);
            }
        }

    }

    private File downloadPhotoFromTelegram(ClassifiedUpdate update) {
        GetFile getFile = new GetFile(update.getMessage().getPhoto().get(update.getMessage().getPhoto().size() - 1).getFileId());
        org.telegram.telegrambots.meta.api.objects.File fileToDownload = null;
        try {
            fileToDownload = execute(getFile);
            File photo = new File("/opt/bot/files/" + getFile.getFileId() + ".jpg");
            downloadFile(fileToDownload, photo);
            return photo;
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    @SneakyThrows
    @Scheduled(fixedDelay = 50)
    public void sendNextMessage() {
        PartialBotApiMethod<?> poll = messageStack.poll();
        try {
            conversionAndExecute(poll);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public void conversionAndExecute(PartialBotApiMethod<?> answer) throws TelegramApiException {
        if (answer instanceof SendMessage) {
            execute((SendMessage) answer);
        } else if (answer instanceof SendDocument) {
            execute((SendDocument) answer);
        } else if (answer instanceof EditMessageText) {
            execute((EditMessageText) answer);
        } else if (answer instanceof SendPhoto) {
            execute((SendPhoto) answer);
        } else if (answer instanceof EditMessageCaption) {
            execute((EditMessageCaption) answer);
        } else if (answer instanceof SendMediaGroup) {
            execute((SendMediaGroup) answer);
        } else if (answer instanceof DeleteMessage) {
            execute((DeleteMessage) answer);
        } else if (answer instanceof AnswerCallbackQuery) {
            execute((AnswerCallbackQuery) answer);
        } else if (answer instanceof ForwardMessage) {
            execute((ForwardMessage) answer);
        } else if (answer instanceof SendInvoice) {
            execute((SendInvoice) answer);
        } else if (answer instanceof AnswerPreCheckoutQuery) {
            execute((AnswerPreCheckoutQuery) answer);
        } else if (answer instanceof SendVideo) {
            execute((SendVideo) answer);
        }
    }
}
