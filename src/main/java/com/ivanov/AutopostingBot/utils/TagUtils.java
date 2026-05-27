package com.ivanov.AutopostingBot.utils;

import com.ivanov.AutopostingBot.model.MainTag;
import com.ivanov.AutopostingBot.model.OtherTag;
import com.ivanov.AutopostingBot.repo.MainTagRepo;
import com.ivanov.AutopostingBot.repo.OtherTagRepo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
@Setter
public class TagUtils {

    private final OtherTagRepo otherTagRepo;
    private final MainTagRepo mainTagRepo;

    private List<MainTag> tags;
    private List<OtherTag> otherTags;

    public TagUtils(OtherTagRepo otherTagRepo, MainTagRepo mainTagRepo) {
        this.otherTagRepo = otherTagRepo;
        this.mainTagRepo = mainTagRepo;
        tags = mainTagRepo.findAll();
        otherTags = otherTagRepo.findAll();
    }
}
