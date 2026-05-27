package com.ivanov.AutopostingBot.repo;

import com.ivanov.AutopostingBot.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PostRepo extends JpaRepository<Post, Integer> {


    List<Post> findAllByDateAndChannelId(LocalDate date, long channelId);

    List<Post> findAllByDateAndSentIsFalse(LocalDate date);
}
