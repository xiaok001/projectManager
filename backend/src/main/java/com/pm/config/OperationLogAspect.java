package com.pm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.model.entity.OperationLog;
import com.pm.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志AOP切面
 * 自动记录所有Controller层接口的操作日志
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /** URL路径模块映射 */
    private static final Map<String, String> MODULE_MAP = new HashMap<>();

    static {
        MODULE_MAP.put("projects", "项目");
        MODULE_MAP.put("risks", "风险");
        MODULE_MAP.put("stages", "阶段");
        MODULE_MAP.put("users", "用户");
        MODULE_MAP.put("config", "配置");
        MODULE_MAP.put("auth", "认证");
        MODULE_MAP.put("dashboard", "仪表盘");
        MODULE_MAP.put("ai-suggestions", "AI建议");
        MODULE_MAP.put("change-logs", "变更日志");
        MODULE_MAP.put("email-digests", "邮件摘要");
        MODULE_MAP.put("operation-logs", "操作日志");
    }

    /** HTTP方法操作映射 */
    private static final Map<String, String> OPERATION_MAP = new HashMap<>();

    static {
        OPERATION_MAP.put("GET", "查询");
        OPERATION_MAP.put("POST", "新增");
        OPERATION_MAP.put("PUT", "修改");
        OPERATION_MAP.put("DELETE", "删除");
    }

    @Around("execution(* com.pm.controller..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前请求
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        // 跳过AuthController.login，登录日志单独处理
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        if ("AuthController".equals(className) && "login".equals(methodName)) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        Object result;
        int responseCode = 200;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            responseCode = 500;
            throw ex;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            // 异步保存日志，不阻塞请求
            try {
                saveLog(request, joinPoint, responseCode, executionTime);
            } catch (Exception e) {
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 异步保存操作日志
     */
    @org.springframework.scheduling.annotation.Async
    public void saveLog(HttpServletRequest request, ProceedingJoinPoint joinPoint,
                        int responseCode, long executionTime) {
        try {
            OperationLog operationLog = new OperationLog();

            // 解析模块
            String requestUri = request.getRequestURI();
            operationLog.setModule(resolveModule(requestUri));

            // 解析操作类型
            String httpMethod = request.getMethod();
            operationLog.setOperation(OPERATION_MAP.getOrDefault(httpMethod, httpMethod));

            // 构建操作描述
            String methodName = joinPoint.getSignature().getName();
            operationLog.setDescription(operationLog.getOperation() + " - " + methodName);

            // 操作人信息
            Long userId = (Long) request.getAttribute("userId");
            String username = (String) request.getAttribute("username");
            operationLog.setOperatorId(userId);
            operationLog.setOperatorName(username);

            // 请求信息
            operationLog.setRequestMethod(httpMethod);
            operationLog.setRequestUrl(requestUri);
            operationLog.setResponseCode(responseCode);
            operationLog.setExecutionTime(executionTime);

            // IP地址
            operationLog.setIp(getClientIp(request));

            // 请求参数(截断至500字符)
            String params = buildRequestParams(joinPoint);
            if (params != null && params.length() > 500) {
                params = params.substring(0, 500);
            }
            operationLog.setRequestParams(params);

            operationLogService.save(operationLog);
        } catch (Exception e) {
            log.error("保存操作日志异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 从URL路径提取模块名称
     * /api/v1/projects -> 项目
     */
    private String resolveModule(String uri) {
        if (uri == null) {
            return "未知";
        }
        // 提取 /api/v1/{module} 中的 module
        String[] parts = uri.split("/");
        if (parts.length >= 4) {
            String moduleKey = parts[3];
            return MODULE_MAP.getOrDefault(moduleKey, moduleKey);
        }
        return "未知";
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 构建请求参数字符串
     */
    private String buildRequestParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            // 过滤掉HttpServletRequest/Response等不可序列化的参数
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                String typeName = arg.getClass().getName();
                if (typeName.startsWith("jakarta.servlet") || typeName.startsWith("org.springframework")) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(objectMapper.writeValueAsString(arg));
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            log.debug("序列化请求参数失败: {}", e.getMessage());
            return null;
        }
    }
}
