package com.stream.app.services;

import com.stream.app.entities.Video;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Service
public interface VideoService {

    //save video
    Video save(Video video, MultipartFile file);

    //get video by id

    Video get(String videoId);

    //get video by title
    Video getByTitle(String title);

    List<Video> getAll();
    String processVideo(String videoId);
}
