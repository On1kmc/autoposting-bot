package com.ivanov.AutopostingBot.utils;

public enum TelegramType { // Типы поступающихъ сообщений боту.
    Command, Text, Photo, SuccessPayment, PreCheckoutQuery,
    ChannelPost, ChatJoinRequest, Unknown, CallBack, MyChatMember
}
