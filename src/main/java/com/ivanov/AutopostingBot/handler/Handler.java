package com.ivanov.AutopostingBot.handler;

import com.ivanov.AutopostingBot.MyBotForPosting;
import com.ivanov.AutopostingBot.utils.ClassifiedUpdate;
import com.ivanov.AutopostingBot.utils.States;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import java.util.List;

public interface Handler {

    // Условия, при которых мы воспользуемся этим обработчиком
    boolean condition(ClassifiedUpdate update);
    // В этом методе, с помощью апдейта мы будем получать answer
    List<PartialBotApiMethod<?>> getAnswer(ClassifiedUpdate update);

    List<PartialBotApiMethod<?>> proceed(ClassifiedUpdate update);

    default SendMessage getMessage(ClassifiedUpdate update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getUserId());
        sendMessage.setParseMode(ParseMode.HTML);
        return sendMessage;
    }

    default EditMessageText getEditMessage(ClassifiedUpdate update) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(update.getUserId());
        editMessageText.setMessageId(update.getMessage().getMessageId());
        editMessageText.setParseMode(ParseMode.HTML);
        return editMessageText;
    }

    default DeleteMessage getDeleteMessage(ClassifiedUpdate update) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setMessageId(update.getMessage().getMessageId());
        deleteMessage.setChatId(update.getUserId());
        return deleteMessage;
    }

    default SendPhoto getPhotoMessage(ClassifiedUpdate update) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(update.getUserId());
        sendPhoto.setParseMode(ParseMode.HTML);
        return sendPhoto;
    }

    default EditMessageCaption getEditCaption(ClassifiedUpdate update) {
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setChatId(update.getUserId());
        editMessageCaption.setMessageId(update.getMessage().getMessageId());
        editMessageCaption.setParseMode(ParseMode.HTML);
        return editMessageCaption;
    }

    default AnswerCallbackQuery getAnswerCallbackQuery(ClassifiedUpdate update, String text) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(update.getUpdate().getCallbackQuery().getId());
        answerCallbackQuery.setShowAlert(true);
        answerCallbackQuery.setText(text);
        return answerCallbackQuery;
    }

    default States getCurrentState(ClassifiedUpdate classifiedUpdate) {
        return MyBotForPosting.getStatesMap().get(classifiedUpdate.getUserId());
    }

    default void putState(ClassifiedUpdate classifiedUpdate, States states) {
        MyBotForPosting.getStatesMap().put(classifiedUpdate.getUserId(), states);
    }

    default void removeState(ClassifiedUpdate classifiedUpdate) {
        MyBotForPosting.getStatesMap().remove(classifiedUpdate.getUserId());
    }

}