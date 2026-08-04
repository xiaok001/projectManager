package com.pm.controller;

import com.pm.common.exception.BusinessException;
import com.pm.common.response.R;
import com.pm.model.dto.ProjectDTO;
import com.pm.model.entity.Project;
import com.pm.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Value("${file.upload-root:./project-files}")
    private String uploadRoot;

    @PostMapping
    public R<Project> create(@Valid @RequestBody ProjectDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Project project = projectService.createProject(dto, userId);
        return R.ok(project);
    }

    @GetMapping
    public R<List<Project>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) Integer level) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(projectService.listProjects(userId, role, name, projectCode, level));
    }

    @GetMapping("/{id}")
    public R<Project> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return R.ok(projectService.getProjectDetail(id, userId, role));
    }

    @PutMapping("/{id}")
    public R<Project> update(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto,
                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(projectService.updateProject(id, dto, userId));
    }

    @PutMapping("/{id}/satisfaction")
    public R<Void> updateSatisfaction(@PathVariable Long id, @RequestBody Map<String, Integer> body,
                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        Integer score = body.get("score");
        if (score == null || score < 1 || score > 10) {
            throw new BusinessException("满意度分数必须在1-10之间");
        }
        projectService.updateSatisfaction(id, score, userId, role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        projectService.deleteProject(id, userId, role);
        return R.ok();
    }

    /**
     * 单独更新WBS在线链接（允许清空）
     */
    @PutMapping("/{id}/wbs-url")
    public R<Void> updateWbsUrl(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Project project = projectService.getById(id);
        if (project == null) throw new BusinessException("项目不存在");
        String url = body.get("wbsOnlineUrl");
        // 空字符串视为清空，存为null
        project.setWbsOnlineUrl((url != null && !url.trim().isEmpty()) ? url.trim() : null);
        projectService.updateById(project);
        return R.ok();
    }

    /**
     * 上传WBS离线附件（覆盖旧文件）
     */
    @PostMapping("/{id}/wbs-file")
    public R<Map<String, String>> uploadWbsFile(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file,
                                                 HttpServletRequest request) {
        Project project = projectService.getById(id);
        if (project == null) throw new BusinessException("项目不存在");

        if (file.isEmpty()) throw new BusinessException("请选择文件");

        try {
            // 构建路径: project-files/{projectCode}/{yyyyMMdd}/
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path dir = Paths.get(uploadRoot, project.getProjectCode(), dateDir);
            Files.createDirectories(dir);

            // 保留原始文件名
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "wbs_attachment";
            }
            Path target = dir.resolve(originalName);
            file.transferTo(target.toFile());

            // 更新数据库（相对路径）
            String relativePath = project.getProjectCode() + "/" + dateDir + "/" + originalName;
            project.setWbsOfflineFile(relativePath);
            projectService.updateById(project);

            log.info("WBS附件上传成功: projectId={}, path={}", id, relativePath);
            return R.ok(Map.of("path", relativePath, "fileName", originalName));
        } catch (IOException e) {
            log.error("WBS附件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载WBS离线附件
     */
    @GetMapping("/{id}/wbs-file")
    public void downloadWbsFile(@PathVariable Long id, HttpServletResponse response) {
        Project project = projectService.getById(id);
        if (project == null || project.getWbsOfflineFile() == null) {
            throw new BusinessException("附件不存在");
        }

        Path filePath = Paths.get(uploadRoot, project.getWbsOfflineFile());
        if (!Files.exists(filePath)) {
            throw new BusinessException("附件文件不存在");
        }

        try {
            String fileName = filePath.getFileName().toString();
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
            response.setContentLengthLong(Files.size(filePath));
            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("WBS附件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectController.class);
}
