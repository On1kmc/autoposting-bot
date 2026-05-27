package com.ivanov.AutopostingBot.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InputForGemini {

    private String system_instruction;
    private String prompt;
    private String thinking_level = "low";
    private String[] images;
}
