package com.pm.model.vo;

import lombok.Data;
import java.util.List;

/**
 * Dashboard聚合数据VO
 */
@Data
public class DashboardVO {
    /** 风险/问题聚合区 */
    private List<RiskAggregationVO> riskAggregation;
    /** 未来关键节点 */
    private List<FutureNodeVO> futureNodes;
    /** 项目健康度总览 */
    private List<ProjectHealthVO> projectHealthList;
}
