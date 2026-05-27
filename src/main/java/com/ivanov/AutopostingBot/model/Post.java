package com.ivanov.AutopostingBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ColumnDefault("false")
    private boolean sent;
    @ColumnDefault("-1002306843314")
    private long channelId;

    private LocalDate date;
    private LocalTime time;

    @Column(columnDefinition = "varchar(2000)")
    private String title;
    @Column(columnDefinition = "varchar(1000)")
    private String videoTitle;
    private String tags;
    private String type;

    @Column(columnDefinition = "varchar(1000)")
    //caption for photo!!!
    private String callToAction;

    @Column(columnDefinition = "varchar(3500)")
    private String firstPrompt;

    @Column(columnDefinition = "varchar(3000)")
    private String secondPrompt;

    private String firstMedia;

    private String secondMedia;
}
