package com.boot3.myrestapi.lectures;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Integer> {
    List<Lecture> findByNameContaining(String name);
    List<Lecture> findByBeginLectureDateTimeBetween (LocalDateTime time1,
                                                     LocalDateTime tim2);
}