#!/bin/bash
# ============================================================
# 一键部署脚本 - 多项目管理系统
# 部署目标: 192.168.4.161:/opt/nyk/pm
# ============================================================

set -e

# 配置
SERVER="root@192.168.4.161"
REMOTE_DIR="/opt/nyk/pm"
SSH_PASS="root2022123"
SSH_OPTS="-o StrictHostKeyChecking=no -o ConnectTimeout=10"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# SSH/SCP 封装
ssh_cmd() { SSHPASS="$SSH_PASS" sshpass -e ssh $SSH_OPTS "$SERVER" "$@"; }
scp_cmd() { SSHPASS="$SSH_PASS" sshpass -e scp $SSH_OPTS "$@"; }

# 进度打印
info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC}   $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail()  { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

# ============================================================
# 构建后端
# ============================================================
build_backend() {
    info "构建后端 JAR..."
    cd "$BACKEND_DIR"
    mvn clean package -DskipTests -q 2>&1 | tail -3
    local jar="$BACKEND_DIR/target/project-manager-1.0.0-SNAPSHOT.jar"
    if [ ! -f "$jar" ]; then
        fail "后端构建失败，JAR 文件不存在"
    fi
    local size=$(ls -lh "$jar" | awk '{print $5}')
    ok "后端构建完成 ($size)"
    cd "$PROJECT_DIR"
}

# ============================================================
# 构建前端
# ============================================================
build_frontend() {
    info "构建前端 dist..."
    cd "$FRONTEND_DIR"
    npm run build 2>&1 | tail -3
    if [ ! -f "$FRONTEND_DIR/dist/index.html" ]; then
        fail "前端构建失败，dist 目录不存在"
    fi
    ok "前端构建完成"
    cd "$PROJECT_DIR"
}

# ============================================================
# 部署后端
# ============================================================
deploy_backend() {
    info "上传后端 JAR..."
    scp_cmd "$BACKEND_DIR/target/project-manager-1.0.0-SNAPSHOT.jar" "$SERVER:$REMOTE_DIR/backend/app.jar"
    ok "JAR 上传完成"

    info "上传配置文件..."
    scp_cmd "$BACKEND_DIR/src/main/resources/application.yml" "$SERVER:$REMOTE_DIR/backend/application.yml"
    ok "配置文件上传完成"

    info "重启后端服务..."
    ssh_cmd "$REMOTE_DIR/start.sh"
    ok "后端启动指令已发送"
}

# ============================================================
# 部署前端
# ============================================================
deploy_frontend() {
    info "清理旧前端文件..."
    ssh_cmd "rm -rf $REMOTE_DIR/frontend/assets/*"

    info "上传前端文件..."
    scp_cmd -r "$FRONTEND_DIR/dist/"* "$SERVER:$REMOTE_DIR/frontend/"
    ok "前端上传完成"
}

# ============================================================
# 等待后端启动
# ============================================================
wait_backend() {
    info "等待后端启动..."
    for i in $(seq 1 20); do
        sleep 3
        local status
        status=$(ssh_cmd "grep -q 'Started ProjectManagerApplication' $REMOTE_DIR/backend.log 2>/dev/null && echo ok || echo wait" 2>/dev/null)
        if [ "$status" = "ok" ]; then
            ok "后端启动成功"
            return 0
        fi
        echo -ne "  等待中 ($((i*3))s)...\r"
    done
    fail "后端启动超时，请检查日志: ssh $SERVER 'tail -50 $REMOTE_DIR/backend.log'"
}

# ============================================================
# 验证服务
# ============================================================
verify() {
    echo ""
    info "验证服务..."

    local api_code
    api_code=$(curl -s -o /dev/null -w "%{http_code}" http://192.168.4.161:8080/api/v1/auth/login \
        -X POST -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin123"}' 2>/dev/null || echo "000")

    local web_code
    web_code=$(curl -s -o /dev/null -w "%{http_code}" http://192.168.4.161:8090/ 2>/dev/null || echo "000")

    echo -e "  后端 API (8080): $([ "$api_code" = "200" ] && echo -e "${GREEN}✅ 正常${NC}" || echo -e "${RED}❌ 异常 ($api_code)${NC}")"
    echo -e "  前端页面 (8090): $([ "$web_code" = "200" ] && echo -e "${GREEN}✅ 正常${NC}" || echo -e "${RED}❌ 异常 ($web_code)${NC}")"
    echo ""
    echo -e "  访问地址: ${BLUE}http://192.168.4.161:8090${NC}"
}

# ============================================================
# 主流程
# ============================================================
echo ""
echo "=========================================="
echo "  多项目管理系统 - 一键部署"
echo "  目标: 192.168.4.161:$REMOTE_DIR"
echo "=========================================="
echo ""
echo "请选择部署内容:"
echo "  1) 仅部署前端"
echo "  2) 仅部署后端"
echo "  3) 全部部署 (前端 + 后端)"
echo ""
read -p "请输入选项 [1/2/3]: " choice
echo ""

case "$choice" in
    1)
        info "===== 部署前端 ====="
        build_frontend
        deploy_frontend
        echo ""
        ok "前端部署完成！"
        verify
        ;;
    2)
        info "===== 部署后端 ====="
        build_backend
        deploy_backend
        wait_backend
        echo ""
        ok "后端部署完成！"
        verify
        ;;
    3)
        info "===== 部署前端 + 后端 ====="
        echo ""
        info "--- 第 1 步: 构建 ---"
        build_backend
        build_frontend
        echo ""
        info "--- 第 2 步: 部署前端 ---"
        deploy_frontend
        echo ""
        info "--- 第 3 步: 部署后端 ---"
        deploy_backend
        wait_backend
        echo ""
        ok "全部部署完成！"
        verify
        ;;
    *)
        fail "无效选项，请输入 1、2 或 3"
        ;;
esac
