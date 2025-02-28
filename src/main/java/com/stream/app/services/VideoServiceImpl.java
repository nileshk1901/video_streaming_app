package com.stream.app.services;

import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    private String DIR;

    @Value("${file.video.hls}")
    private String HLS_DIR;

    private VideoRepository videoRepository;
   // private static final Logger LOGGER = Logger.getLogger(VideoServiceImpl.class.getName());

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init() {

        File file = new File(DIR);


        try {
            Files.createDirectories(Paths.get(HLS_DIR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!file.exists()) {
            file.mkdir();
            System.out.println("Folder Created:");
        } else {
            System.out.println("Folder already created");
        }

    }


    @Override
    public Video save(Video video, MultipartFile file) {
        try {


            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();


            // file path
            String cleanFileName = StringUtils.cleanPath(filename);


            //folder path : create

            String cleanFolder = StringUtils.cleanPath(DIR);


            // folder path with  filename
            Path path = Paths.get(cleanFolder, cleanFileName);

            System.out.println(contentType);
            System.out.println(path);

            // copy file to the folder
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);


            // video meta data

            video.setContentType(contentType);
            video.setFilePath(path.toString());
            Video savedVideo = videoRepository.save(video);
            //processing video
            processVideo(savedVideo.getVideoId());

            //delete actual video file and database entry  if exception

            // metadata save
            return savedVideo;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error in processing video ");
        }

    }

    @Override
    public Video get(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {
        Video video = this.get(videoId);
        String filePath = video.getFilePath();

        //path where to store data:
        Path videoPath = Paths.get(filePath);


//        String output360p = HSL_DIR + videoId + "/360p/";
//        String output720p = HSL_DIR + videoId + "/720p/";
//        String output1080p = HSL_DIR + videoId + "/1080p/";

        try {
//            Files.createDirectories(Paths.get(output360p));
//            Files.createDirectories(Paths.get(output720p));
//            Files.createDirectories(Paths.get(output1080p));

            // ffmpeg command
            Path outputPath = Paths.get(HLS_DIR, videoId);

            Files.createDirectories(outputPath);


            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath, outputPath, outputPath
            );

//            StringBuilder ffmpegCmd = new StringBuilder();
//            ffmpegCmd.append("ffmpeg  -i ")
//                    .append(videoPath.toString())
//                    .append(" -c:v libx264 -c:a aac")
//                    .append(" ")
//                    .append("-map 0:v -map 0:a -s:v:0 640x360 -b:v:0 800k ")
//                    .append("-map 0:v -map 0:a -s:v:1 1280x720 -b:v:1 2800k ")
//                    .append("-map 0:v -map 0:a -s:v:2 1920x1080 -b:v:2 5000k ")
//                    .append("-var_stream_map \"v:0,a:0 v:1,a:0 v:2,a:0\" ")
//                    .append("-master_pl_name ").append(HSL_DIR).append(videoId).append("/master.m3u8 ")
//                    .append("-f hls -hls_time 10 -hls_list_size 0 ")
//                    .append("-hls_segment_filename \"").append(HSL_DIR).append(videoId).append("/v%v/fileSequence%d.ts\" ")
//                    .append("\"").append(HSL_DIR).append(videoId).append("/v%v/prog_index.m3u8\"");


            System.out.println(ffmpegCmd);
            //file this command
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("video processing failed!!");
            }

            return videoId;


        } catch (IOException ex) {
            throw new RuntimeException("Video processing fail!!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
    }

//package com.stream.app.services;
//
//import com.stream.app.entities.Video;
//import com.stream.app.repositories.VideoRepository;
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class VideoServiceImpl implements VideoService {
//
//    private static final Logger logger = LoggerFactory.getLogger(VideoServiceImpl.class);
//
//    @Value("${files.video}")
//    private String DIR;
//
//    @Value("${file.video.hls}")
//    private String HLS_DIR;
//
//    private final VideoRepository videoRepository;
//
//    public VideoServiceImpl(VideoRepository videoRepository) {
//        this.videoRepository = videoRepository;
//    }
//
//    @PostConstruct
//    public void init() {
//        try {
//            // Validate ffmpeg installation
//            ProcessBuilder checkFFmpeg = new ProcessBuilder("ffmpeg", "-version");
//            checkFFmpeg.redirectErrorStream(true);
//            Process process = checkFFmpeg.start();
//
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                String line;
//                StringBuilder output = new StringBuilder();
//                while ((line = reader.readLine()) != null) {
//                    output.append(line).append("\n");
//                }
//
//                if (process.waitFor() != 0) {
//                    logger.error("FFmpeg check output: {}", output);
//                    throw new RuntimeException("FFmpeg is not properly installed");
//                }
//                logger.info("FFmpeg check successful: {}", output);
//            }
//
//            // Create directories
//            Files.createDirectories(Paths.get(DIR));
//            Files.createDirectories(Paths.get(HLS_DIR));
//
//            // Validate write permissions
//            Path testFile = Paths.get(HLS_DIR, "test.txt");
//            Files.writeString(testFile, "test");
//            Files.delete(testFile);
//
//            logger.info("Video service initialized successfully");
//
//        } catch (IOException | InterruptedException e) {
//            logger.error("Failed to initialize video service", e);
//            throw new RuntimeException("Failed to initialize video service: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public Video save(Video video, MultipartFile file) {
//        try {
//            // Validate file
//            if (file.isEmpty()) {
//                throw new RuntimeException("File is empty");
//            }
//
//            String contentType = file.getContentType();
//            if (contentType == null || !contentType.startsWith("video/")) {
//                throw new RuntimeException("Invalid file type. Only video files are allowed");
//            }
//
//            Path videoDir = Paths.get(DIR);
//            if (!Files.exists(videoDir)) {
//                Files.createDirectories(videoDir);
//            }
//
//            String filename = StringUtils.cleanPath(file.getOriginalFilename());
//            Path targetPath = videoDir.resolve(filename);
//
//            // Copy file
//            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
//
//            // Save metadata
//            video.setContentType(contentType);
//            video.setFilePath(targetPath.toString());
//            Video savedVideo = videoRepository.save(video);
//
//            try {
//                // Process video
//                processVideo(savedVideo.getVideoId());
//                return savedVideo;
//            } catch (Exception e) {
//                // Cleanup on failure
//                logger.error("Failed to process video. Cleaning up...", e);
//                Files.deleteIfExists(targetPath);
//                videoRepository.delete(savedVideo);
//                throw e;
//            }
//
//        } catch (IOException e) {
//            logger.error("Failed to save video", e);
//            throw new RuntimeException("Failed to save video: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public Video get(String videoId) {
//        return videoRepository.findById(videoId)
//                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));
//    }
//
//    @Override
//    public Video getByTitle(String title) {
//        return null; // Implement if needed
//    }
//
//    @Override
//    public List<Video> getAll() {
//        return videoRepository.findAll();
//    }
//
//    @Override
//    public String processVideo(String videoId) {
//        Video video = this.get(videoId);
//        Path videoPath = Paths.get(video.getFilePath());
//        Path outputPath = Paths.get(HLS_DIR, videoId);
//
//        try {
//            Files.createDirectories(outputPath);
//
//            List<String> command = new ArrayList<>();
//
//            // Add appropriate command based on OS
//            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
//                command.add("cmd");
//                command.add("/c");
//            } else {
//                command.add("/bin/bash");
//                command.add("-c");
//            }
//
//            // Build FFmpeg command
//            String ffmpegCmd = String.format(
//                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls " +
//                            "-hls_time 10 -hls_list_size 0 " +
//                            "-hls_segment_filename \"%s\" \"%s\"",
//                    videoPath.toString(),
//                    outputPath.resolve("segment_%3d.ts").toString(),
//                    outputPath.resolve("master.m3u8").toString()
//            );
//
//            command.add(ffmpegCmd);
//            logger.info("Executing FFmpeg command: {}", ffmpegCmd);
//
//            ProcessBuilder processBuilder = new ProcessBuilder(command);
//            processBuilder.redirectErrorStream(true);
//
//            Process process = processBuilder.start();
//
//            // Capture and log output
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    logger.debug("FFmpeg output: {}", line);
//                }
//            }
//
//            int exit = process.waitFor();
//            if (exit != 0) {
//                throw new RuntimeException("FFmpeg process exited with code: " + exit);
//            }
//
//            logger.info("Video processing completed successfully for videoId: {}", videoId);
//            return videoId;
//
//        } catch (IOException | InterruptedException ex) {
//            logger.error("Failed to process video", ex);
//            throw new RuntimeException("Video processing failed: " + ex.getMessage());
//        }
//    }
//}