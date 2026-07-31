package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.ProjectRiskMapper;
import com.pm.model.dto.RiskDTO;
import com.pm.model.entity.Project;
import com.pm.model.entity.ProjectRisk;
import com.pm.model.entity.SysUser;
import com.pm.model.vo.RiskAggregationVO;
import com.pm.mapper.SysUserMapper;
import com.pm.service.ProjectRiskService;
import com.pm.service.ProjectService;
import com.pm.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectRiskServiceImpl extends ServiceImpl<ProjectRiskMapper, ProjectRisk>
        implements ProjectRiskService {

    private final ProjectService projectService;
    private final SysUserMapper sysUserMapper;
    private final SystemConfigService configService;

    @Override
    public ProjectRisk createRisk(Long projectId, RiskDTO dto, Long operatorId) {
        Project project = projectService.getById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        ProjectRisk risk = new ProjectRisk();
        risk.setRiskCode(generateRiskCode(projectId));
        risk.setProjectId(projectId);
        risk.setDescription(dto.getDescription());
        risk.setType(dto.getType());
        risk.setSeverity(dto.getSeverity());
        risk.setOwnerId(dto.getOwnerId());
        risk.setActionPlan(dto.getActionPlan());
        risk.setStatus(Constants.RISK_PENDING);
        risk.setIsStale(false);
        risk.setLastUpdatedAt(LocalDateTime.now());
        risk.setUpdatedBy(operatorId);

        save(risk);
        return risk;
    }

    @Override
    public ProjectRisk updateRisk(Long id, RiskDTO dto, Long operatorId) {
        ProjectRisk risk = getById(id);
        if (risk == null) {
            throw new BusinessException("风险不存在");
        }

        if (dto.getDescription() != null) risk.setDescription(dto.getDescription());
        if (dto.getType() != null) risk.setType(dto.getType());
        if (dto.getSeverity() != null) risk.setSeverity(dto.getSeverity());
        if (dto.getOwnerId() != null) risk.setOwnerId(dto.getOwnerId());
        if (dto.getActionPlan() != null) risk.setActionPlan(dto.getActionPlan());
        risk.setLastUpdatedAt(LocalDateTime.now());
        risk.setUpdatedBy(operatorId);

        updateById(risk);
        return risk;
    }

    @Override
    public List<ProjectRisk> listByProjectId(Long projectId) {
        return lambdaQuery()
                .eq(ProjectRisk::getProjectId, projectId)
                .orderByDesc(ProjectRisk::getCreatedAt)
                .list();
    }

    @Override
    public List<RiskAggregationVO> listAggregated(Long userId, String role) {
        // 获取用户可见的项目
        List<Project> projects = projectService.listProjects(userId, role);
        if (projects.isEmpty()) return new ArrayList<>();

        List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        // 查询未关闭的风险
        List<ProjectRisk> risks = lambdaQuery()
                .in(ProjectRisk::getProjectId, projectIds)
                .ne(ProjectRisk::getStatus, Constants.RISK_CLOSED)
                .ne(ProjectRisk::getStatus, Constants.RISK_RESOLVED)
                .list();

        int staleThreshold = configService.getIntValue("stale_threshold_days", 7);

        List<RiskAggregationVO> voList = new ArrayList<>();
        for (ProjectRisk risk : risks) {
            Project project = projectMap.get(risk.getProjectId());
            if (project == null) continue;

            RiskAggregationVO vo = new RiskAggregationVO();
            vo.setRiskId(risk.getId());
            vo.setRiskCode(risk.getRiskCode());
            vo.setDescription(risk.getDescription());
            vo.setRiskType(risk.getType());
            vo.setSeverity(risk.getSeverity());
            vo.setStatus(risk.getStatus());
            vo.setIsStale(risk.getIsStale());
            vo.setProjectId(risk.getProjectId());
            vo.setProjectCode(project.getProjectCode());
            vo.setProjectName(project.getName());
            vo.setProjectLevel(project.getLevel());
            vo.setLastUpdatedAt(risk.getLastUpdatedAt());

            // 计算停滞天数
            if (risk.getLastUpdatedAt() != null) {
                long days = java.time.Duration.between(risk.getLastUpdatedAt(), LocalDateTime.now()).toDays();
                vo.setStaleDays((int) days);
            }

            // 查询责任人姓名
            if (risk.getOwnerId() != null) {
                SysUser owner = sysUserMapper.selectById(risk.getOwnerId());
                if (owner != null) vo.setOwnerName(owner.getRealName());
            }

            voList.add(vo);
        }

        // 排序: 项目等级 → 严重程度 → 停滞天数(降序)
        voList.sort((a, b) -> {
            int cmp = Integer.compare(
                    a.getProjectLevel() != null ? a.getProjectLevel() : 99,
                    b.getProjectLevel() != null ? b.getProjectLevel() : 99);
            if (cmp != 0) return cmp;

            cmp = severityOrder(a.getSeverity()) - severityOrder(b.getSeverity());
            if (cmp != 0) return cmp;

            return Integer.compare(
                    b.getStaleDays() != null ? b.getStaleDays() : 0,
                    a.getStaleDays() != null ? a.getStaleDays() : 0);
        });

        return voList;
    }

    @Override
    public void updateStaleStatus(Long id, Boolean staleOverride, Long operatorId) {
        ProjectRisk risk = getById(id);
        if (risk == null) {
            throw new BusinessException("风险不存在");
        }
        risk.setStaleOverride(staleOverride);
        // 如果手动设置了覆盖值，则以覆盖值为准
        if (staleOverride != null) {
            risk.setIsStale(staleOverride);
        }
        risk.setLastUpdatedAt(LocalDateTime.now());
        risk.setUpdatedBy(operatorId);
        updateById(risk);
    }

    @Override
    public void refreshStaleStatus() {
        int staleThreshold = configService.getIntValue("stale_threshold_days", 7);
        LocalDateTime threshold = LocalDateTime.now().minusDays(staleThreshold);

        // 查找未关闭且非手动覆盖的风险
        List<ProjectRisk> risks = lambdaQuery()
                .ne(ProjectRisk::getStatus, Constants.RISK_CLOSED)
                .ne(ProjectRisk::getStatus, Constants.RISK_RESOLVED)
                .isNull(ProjectRisk::getStaleOverride)
                .list();

        for (ProjectRisk risk : risks) {
            boolean shouldBeStale = risk.getLastUpdatedAt() != null
                    && risk.getLastUpdatedAt().isBefore(threshold);

            if (shouldBeStale != risk.getIsStale()) {
                risk.setIsStale(shouldBeStale);
                updateById(risk);
            }
        }
        log.info("刷新停滞状态完成，检查{}条风险", risks.size());
    }

    @Override
    public String generateRiskCode(Long projectId) {
        Project project = projectService.getById(projectId);
        String projectCode = project.getProjectCode();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseCode = projectCode + "-" + dateStr;

        // 查找当日同项目的最大序号
        long count = lambdaQuery()
                .likeRight(ProjectRisk::getRiskCode, baseCode)
                .count();

        if (count == 0) {
            return baseCode;
        } else {
            return baseCode + "-" + String.format("%02d", count + 1);
        }
    }

    private int severityOrder(String severity) {
        if ("高".equals(severity)) return 0;
        if ("中".equals(severity)) return 1;
        if ("低".equals(severity)) return 2;
        return 3;
    }
}
