package com.ivanov.AutopostingBot.utils;

import com.ivanov.AutopostingBot.handler.Handler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class HandlerUtils { // Класс для поиска нужного хендлера
    private final List<Handler> handlerList;

    public HandlerUtils(List<Handler> handlerList) {
        this.handlerList = handlerList;
    }


    public Optional<Handler> getHandler(ClassifiedUpdate classifiedUpdate) { // Метод возвращает нужный хэндлер для сообщения
        return handlerList.stream().filter(s -> s.condition(classifiedUpdate)).findFirst();
    }
}
