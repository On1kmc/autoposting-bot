package com.ivanov.AutopostingBot.utils;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PintoStyleWebAppEntity {

    private int categoryId = 4;
    private String name;
    private String previewUrl;
    private int sortOrder = 0;
    private String prompt;
    private String gender;
}
