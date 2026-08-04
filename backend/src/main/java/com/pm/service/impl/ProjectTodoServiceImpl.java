package com.pm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pm.common.constants.Constants;
import com.pm.common.exception.BusinessException;
import com.pm.mapper.ProjectMapper;
import com.pm.mapper.ProjectStageMapper;
import com.pm.mapper.ProjectTodoMapper;
import com.pm.mapper.SysUserMapper;
import com.pm.model.dto.TodoDTO;
import com.pm.model.entity.Project;
import com.pm.model.entity.ProjectRisk;
import com.pm.model.entity.ProjectStage;
import com.pm.model.entity.ProjectTodo;
import com.pm.model.entity.SysUser;
import com.pm.service.ProjectRiskService;
import com.pm.service.ProjectTodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTodoServiceImpl extends ServiceImpl<ProjectTodoMapper, ProjectTodo> implements ProjectTodoService {

    private final ProjectMapper projectMapper;
    private final SysUserMapper sysUserMapper;
    private final ProjectStageMapper stageMapper;
    private final ProjectRiskService riskService;

    @Override
    public List<ProjectTodo> listByProjectId(Long projectId) {
        List<ProjectTodo> list = lambdaQuery()
                .eq(ProjectTodo::getProjectId, projectId)
                .orderByDesc(ProjectTodo::getCreatedAt)
                .list();
        fillExtraNames(list);
        return list;
    }

    @Override
    public Page<ProjectTodo> pageList(Integer pageNum, Integer pageSize, Long projectId,
                                       String status, String priority, String keyword,
                                       Long userId, String role) {
        Page<ProjectTodo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProjectTodo> wrapper = new LambdaQueryWrapper<>();

        if (projectId != null) {
            wrapper.eq(ProjectTodo::getProjectId, projectId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ProjectTodo::getStatus, status);
        }
        if (priority != null && !priority.isEmpty()) {
            wrapper.eq(ProjectTodo::getPriority, priority);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ProjectTodo::getTitle, keyword)
                    .or().like(ProjectTodo::getTodoCode, keyword)
                    .or().like(ProjectTodo::getRemark, keyword));
        }

        wrapper.orderByDesc(ProjectTodo::getCreatedAt);
        Page<ProjectTodo> result = page(page, wrapper);
        fillExtraNames(result.getRecords());
        return result;
    }

    @Override
    @Transactional
    public ProjectTodo createTodo(Long projectId, TodoDTO dto, Long operatorId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException("项目不存在");

        ProjectTodo todo = new ProjectTodo();
        todo.setTodoCode(generateTodoCode(projectId));
        todo.setProjectId(projectId);
        todo.setStageId(dto.getStageId());
        todo.setTitle(dto.getTitle());
        todo.setSource(dto.getSource());
        todo.setPriority(dto.getPriority());
        todo.setUrgency(dto.getUrgency());
        todo.setOwnerId(dto.getOwnerId());
        todo.setOwnerName(dto.getOwnerName());
        todo.setPlanStart(dto.getPlanStart());
        todo.setPlanEnd(dto.getPlanEnd());
        todo.setActualEnd(dto.getActualEnd());
        todo.setStatus(dto.getStatus() != null ? dto.getStatus() : "待处理");
        todo.setProgress(dto.getProgress() != null ? dto.getProgress() : 0);
        todo.setBlockIssue(dto.getBlockIssue());
        todo.setRiskDesc(dto.getRiskDesc());
        todo.setOutputDesc(dto.getOutputDesc());
        todo.setRemark(dto.getRemark());
        todo.setCreatedBy(operatorId);
        save(todo);
        fillExtraNames(List.of(todo));
        return todo;
    }

    @Override
    @Transactional
    public ProjectTodo updateTodo(Long id, TodoDTO dto, Long operatorId) {
        ProjectTodo todo = getById(id);
        if (todo == null) throw new BusinessException("待办不存在");

        if (dto.getStageId() != null) todo.setStageId(dto.getStageId());
        if (dto.getTitle() != null) todo.setTitle(dto.getTitle());
        if (dto.getSource() != null) todo.setSource(dto.getSource());
        if (dto.getPriority() != null) todo.setPriority(dto.getPriority());
        if (dto.getUrgency() != null) todo.setUrgency(dto.getUrgency());
        if (dto.getOwnerId() != null) todo.setOwnerId(dto.getOwnerId());
        if (dto.getOwnerName() != null) todo.setOwnerName(dto.getOwnerName());
        if (dto.getPlanStart() != null) todo.setPlanStart(dto.getPlanStart());
        if (dto.getPlanEnd() != null) todo.setPlanEnd(dto.getPlanEnd());
        if (dto.getActualEnd() != null) todo.setActualEnd(dto.getActualEnd());
        if (dto.getStatus() != null) todo.setStatus(dto.getStatus());
        if (dto.getProgress() != null) todo.setProgress(dto.getProgress());
        if (dto.getBlockIssue() != null) todo.setBlockIssue(dto.getBlockIssue());
        if (dto.getRiskDesc() != null) todo.setRiskDesc(dto.getRiskDesc());
        if (dto.getOutputDesc() != null) todo.setOutputDesc(dto.getOutputDesc());
        if (dto.getRemark() != null) todo.setRemark(dto.getRemark());

        // 进度联动状态
        if (dto.getProgress() != null) {
            if (dto.getProgress() >= 100) {
                todo.setProgress(100);
                todo.setStatus("已完成");
                if (todo.getActualEnd() == null) todo.setActualEnd(LocalDate.now());
            } else if (dto.getProgress() > 0) {
                todo.setStatus("进行中");
            }
        }

        updateById(todo);
        fillExtraNames(List.of(todo));
        return todo;
    }

    @Override
    @Transactional
    public void deleteTodo(Long id) {
        removeById(id);
    }

    @Override
    public void refreshOverdueTodos() {
        LocalDate today = LocalDate.now();
        List<ProjectTodo> todos = lambdaQuery()
                .ne(ProjectTodo::getStatus, "已完成")
                .ne(ProjectTodo::getStatus, "已取消")
                .isNotNull(ProjectTodo::getPlanEnd)
                .le(ProjectTodo::getPlanEnd, today)
                .list();

        for (ProjectTodo todo : todos) {
            if (!"已逾期".equals(todo.getStatus())) {
                todo.setStatus("已逾期");
                updateById(todo);

                // 自动创建风险记录
                try {
                    ProjectRisk risk = new ProjectRisk();
                    risk.setRiskCode(riskService.generateRiskCode(todo.getProjectId()));
                    risk.setProjectId(todo.getProjectId());
                    risk.setDescription("待办事项逾期: " + todo.getTitle());
                    risk.setType("问题");
                    risk.setSeverity("高".equals(todo.getPriority()) ? "高" : "低".equals(todo.getPriority()) ? "低" : "中");
                    risk.setStatus(Constants.RISK_PENDING);
                    risk.setIsStale(false);
                    risk.setOwnerId(todo.getOwnerId());
                    riskService.save(risk);
                    log.info("待办逾期自动创建风险: todoCode={}", todo.getTodoCode());
                } catch (Exception e) {
                    log.error("待办逾期创建风险失败: {}", todo.getTodoCode(), e);
                }
            }
        }
        log.info("刷新待办逾期完成，更新{}条", todos.size());
    }

    @Override
    public String generateTodoCode(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        String base = project.getProjectCode() + "-TD-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = lambdaQuery().likeRight(ProjectTodo::getTodoCode, base).count();
        return count == 0 ? base : base + "-" + String.format("%02d", count + 1);
    }

    private void fillExtraNames(List<ProjectTodo> list) {
        // 填充责任人姓名
        List<Long> ownerIds = list.stream()
                .map(ProjectTodo::getOwnerId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!ownerIds.isEmpty()) {
            Map<Long, String> nameMap = sysUserMapper.selectBatchIds(ownerIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            list.forEach(t -> {
                if (t.getOwnerId() != null && t.getOwnerName() == null) {
                    t.setOwnerName(nameMap.get(t.getOwnerId()));
                }
            });
        }
        // 填充阶段名称
        List<Long> stageIds = list.stream()
                .map(ProjectTodo::getStageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!stageIds.isEmpty()) {
            Map<Long, String> stageMap = stageMapper.selectBatchIds(stageIds).stream()
                    .collect(Collectors.toMap(ProjectStage::getId, ProjectStage::getStageName));
            list.forEach(t -> {
                if (t.getStageId() != null) t.setStageName(stageMap.get(t.getStageId()));
            });
        }
        // 填充项目编号
        List<Long> projectIds = list.stream()
                .map(ProjectTodo::getProjectId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (!projectIds.isEmpty()) {
            Map<Long, String> codeMap = projectMapper.selectBatchIds(projectIds).stream()
                    .collect(Collectors.toMap(Project::getId, Project::getProjectCode));
            list.forEach(t -> {
                if (t.getProjectId() != null) t.setProjectCode(codeMap.get(t.getProjectId()));
            });
        }
    }
}
