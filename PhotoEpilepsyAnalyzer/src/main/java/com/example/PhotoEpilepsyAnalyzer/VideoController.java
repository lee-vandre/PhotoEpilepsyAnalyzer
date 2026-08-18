package com.example.PhotoEpilepsyAnalyzer;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoProcessingService videoProcessingService;

    // Where uploaded and corrected files live for the lifetime of the app.
    // Fine for local dev / a single-user tool; swap for a real store if this
    // ever needs to survive a restart or serve multiple concurrent users.
    private final Path workDir;

    // Maps a job id -> the corrected video's path, so /download/{id} can find it.
    private final Map<String, Path> processedFiles = new ConcurrentHashMap<>();

    public VideoController(VideoProcessingService videoProcessingService) throws IOException {
        this.videoProcessingService = videoProcessingService;
        this.workDir = Files.createTempDirectory("epilepsy-scanner");
    }

    /**
     * Accepts a video upload, runs the full analyze -> (reassemble if unsafe)
     * pipeline, and returns an AnalysisResponse. If the video was unsafe, the
     * response's downloadUrl points at the corrected file.
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new AnalysisResponse(false, null, "No file was uploaded."));
        }

        String jobId = UUID.randomUUID().toString();
        String safeName = sanitize(file.getOriginalFilename());
        Path inputPath = workDir.resolve(jobId + "_input_" + safeName);
        Path outputPath = workDir.resolve(jobId + "_safe_" + safeName);

        try {
            file.transferTo(inputPath);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(new AnalysisResponse(false, null, "Could not save the uploaded file: " + e.getMessage()));
        }

        AnalysisReport report;
        try {
            report = videoProcessingService.processVideoPipeline(
                    inputPath.toString(), outputPath.toString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new AnalysisResponse(false, null, "Video processing failed: " + e.getMessage()));
        }

        String downloadUrl = null;
        if (!report.isSafe() && Files.exists(outputPath)) {
            processedFiles.put(jobId, outputPath);
            downloadUrl = "/api/videos/download/" + jobId;
        }

        return ResponseEntity.ok(new AnalysisResponse(true, report, null, downloadUrl));
    }

    /**
     * Streams back the corrected video for a previously analyzed job.
     */
    @GetMapping("/download/{jobId}")
    public ResponseEntity<Resource> download(@PathVariable String jobId) throws MalformedURLException {
        Path path = processedFiles.get(jobId);
        if (path == null || !Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"safe_video.mp4\"")
                .body(resource);
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "video.mp4";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}