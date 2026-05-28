#!/bin/bash

# GzDoc 通用部署脚本
# 支持灵活配置，可用于任何环境

set -e

# ============================================
# 配置区域 - 根据实际情况修改
# ============================================

# 默认配置（可通过环境变量覆盖）
SERVER="${DEPLOY_SERVER:-192.168.57.10}"
USER="${DEPLOY_USER:-root}"
REMOTE_DIR="${DEPLOY_DIR:-/opt/gzdoc}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.test.yml}"
ENV_NAME="${ENV_NAME:-test}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ============================================
# 函数定义
# ============================================

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 未安装"
        return 1
    fi
    return 0
}

# 显示使用说明
show_usage() {
    cat << EOF
使用方法:
  $0 [选项]

选项:
  -s, --server <IP>       目标服务器IP (默认: 192.168.57.10)
  -u, --user <USER>       SSH用户名 (默认: root)
  -d, --dir <DIR>         远程部署目录 (默认: /opt/gzdoc)
  -f, --file <FILE>       docker-compose文件 (默认: docker-compose.test.yml)
  -e, --env <ENV>         环境名称 (默认: test)
  --skip-check            跳过环境检查
  --skip-upload           跳过文件上传
  --only-restart          仅重启服务
  -h, --help              显示帮助信息

环境变量:
  DEPLOY_SERVER           目标服务器IP
  DEPLOY_USER             SSH用户名
  DEPLOY_DIR              远程部署目录
  COMPOSE_FILE            docker-compose文件名
  ENV_NAME                环境名称

示例:
  # 部署到测试环境
  $0

  # 部署到生产环境
  $0 -s 36.141.21.176 -f docker-compose.prod.yml -e prod

  # 使用环境变量
  DEPLOY_SERVER=192.168.57.10 $0

  # 仅重启服务
  $0 --only-restart
EOF
}

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -s|--server)
                SERVER="$2"
                shift 2
                ;;
            -u|--user)
                USER="$2"
                shift 2
                ;;
            -d|--dir)
                REMOTE_DIR="$2"
                shift 2
                ;;
            -f|--file)
                COMPOSE_FILE="$2"
                shift 2
                ;;
            -e|--env)
                ENV_NAME="$2"
                shift 2
                ;;
            --skip-check)
                SKIP_CHECK=true
                shift
                ;;
            --skip-upload)
                SKIP_UPLOAD=true
                shift
                ;;
            --only-restart)
                ONLY_RESTART=true
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            *)
                print_error "未知参数: $1"
                show_usage
                exit 1
                ;;
        esac
    done
}

# 检查本地环境
check_local_env() {
    print_info "检查本地环境..."

    if ! check_command ssh; then
        print_error "请安装 ssh"
        exit 1
    fi

    if ! check_command scp; then
        print_error "请安装 scp"
        exit 1
    fi

    print_info "✓ 本地环境检查通过"
}

# 检查SSH连接
check_ssh() {
    print_info "检查SSH连接到 $USER@$SERVER..."

    if ssh -o ConnectTimeout=5 -o BatchMode=yes $USER@$SERVER "echo 'SSH连接成功'" &> /dev/null; then
        print_info "✓ SSH连接正常"
        return 0
    else
        print_warn "SSH连接失败，可能需要输入密码"
        if ssh -o ConnectTimeout=5 $USER@$SERVER "echo 'SSH连接成功'"; then
            print_info "✓ SSH连接正常"
            return 0
        else
            print_error "SSH连接失败"
            return 1
        fi
    fi
}

# 检查远程Docker环境
check_remote_docker() {
    print_info "检查远程Docker环境..."

    if ssh $USER@$SERVER "docker --version && docker-compose --version" &> /dev/null; then
        print_info "✓ Docker环境正常"
        return 0
    else
        print_warn "Docker未安装或版本过低"
        print_info "是否自动安装Docker? (y/n)"
        read -r response
        if [[ "$response" == "y" ]]; then
            install_docker
        else
            print_error "请手动安装Docker后重试"
            return 1
        fi
    fi
}

# 安装Docker
install_docker() {
    print_info "正在安装Docker..."
    ssh $USER@$SERVER "curl -fsSL https://get.docker.com | sh && systemctl start docker && systemctl enable docker"
    ssh $USER@$SERVER "curl -L https://github.com/docker/compose/releases/latest/download/docker-compose-\$(uname -s)-\$(uname -m) -o /usr/local/bin/docker-compose && chmod +x /usr/local/bin/docker-compose"
    print_info "✓ Docker安装完成"
}

# 创建远程目录
create_remote_dir() {
    print_info "创建远程目录 $REMOTE_DIR..."
    ssh $USER@$SERVER "mkdir -p $REMOTE_DIR"
    print_info "✓ 目录创建完成"
}

# 上传文件
upload_files() {
    print_info "上传配置文件..."

    local script_dir="$(cd "$(dirname "$0")/.." && pwd)"

    # 上传docker-compose文件
    if [ -f "$script_dir/infrastructure/docker/$COMPOSE_FILE" ]; then
        scp "$script_dir/infrastructure/docker/$COMPOSE_FILE" $USER@$SERVER:$REMOTE_DIR/docker-compose.yml
        print_info "✓ 上传 $COMPOSE_FILE"
    else
        print_error "文件不存在: $COMPOSE_FILE"
        exit 1
    fi

    # 上传init-db.sql
    if [ -f "$script_dir/infrastructure/docker/init-db.sql" ]; then
        scp "$script_dir/infrastructure/docker/init-db.sql" $USER@$SERVER:$REMOTE_DIR/
        print_info "✓ 上传 init-db.sql"
    fi

    # 上传.env文件（如果存在）
    if [ -f "$script_dir/infrastructure/docker/.env.$ENV_NAME" ]; then
        scp "$script_dir/infrastructure/docker/.env.$ENV_NAME" $USER@$SERVER:$REMOTE_DIR/.env
        print_info "✓ 上传 .env.$ENV_NAME"
    fi

    print_info "✓ 文件上传完成"
}

# 检查现有服务
check_existing_services() {
    print_info "检查现有服务..."

    if ssh $USER@$SERVER "cd $REMOTE_DIR && docker-compose ps 2>/dev/null | grep -q Up"; then
        print_warn "发现运行中的服务"
        ssh $USER@$SERVER "cd $REMOTE_DIR && docker-compose ps"
        print_info "是否停止并重新部署? (y/n)"
        read -r response
        if [[ "$response" == "y" ]]; then
            stop_services
        else
            print_info "取消部署"
            exit 0
        fi
    fi
}

# 停止服务
stop_services() {
    print_info "停止现有服务..."
    ssh $USER@$SERVER "cd $REMOTE_DIR && docker-compose down"
    print_info "✓ 服务已停止"
}

# 启动服务
start_services() {
    print_info "启动Docker服务..."
    ssh $USER@$SERVER "cd $REMOTE_DIR && docker-compose up -d"
    print_info "✓ 服务启动完成"
}

# 重启服务
restart_services() {
    print_info "重启服务..."
    ssh $USER@$SERVER "cd $REMOTE_DIR && docker-compose restart"
    print_info "✓ 服务重启完成"
}

# 等待服务就绪
wait_for_services() {
    print_info "等待服务就绪..."
    sleep 10
}

# 检查服务状态
check_services_status() {
    print_info "检查服务状态..."
    ssh $USER@$SERVER "cd $REMOTE_DIR && docker-compose ps"
}

# 验证服务
verify_services() {
    print_info "验证服务..."

    # PostgreSQL
    if ssh $USER@$SERVER "docker ps --format '{{.Names}}' | grep -q postgres"; then
        local container=$(ssh $USER@$SERVER "docker ps --format '{{.Names}}' | grep postgres")
        if ssh $USER@$SERVER "docker exec $container pg_isready" &> /dev/null; then
            print_info "  ✓ PostgreSQL 正常"
        else
            print_warn "  ✗ PostgreSQL 异常"
        fi
    fi

    # Redis
    if ssh $USER@$SERVER "docker ps --format '{{.Names}}' | grep -q redis"; then
        local container=$(ssh $USER@$SERVER "docker ps --format '{{.Names}}' | grep redis")
        if ssh $USER@$SERVER "docker exec $container redis-cli ping" &> /dev/null; then
            print_info "  ✓ Redis 正常"
        else
            print_warn "  ✗ Redis 异常"
        fi
    fi

    # MinIO
    if ssh $USER@$SERVER "curl -f http://localhost:9000/minio/health/live" &> /dev/null; then
        print_info "  ✓ MinIO 正常"
    else
        print_warn "  ✗ MinIO 异常"
    fi
}

# 显示部署信息
show_deploy_info() {
    print_header "部署完成！"
    echo ""
    echo "环境: $ENV_NAME"
    echo "服务器: $SERVER"
    echo "部署目录: $REMOTE_DIR"
    echo ""
    echo "服务访问地址:"
    echo "  - PostgreSQL: $SERVER:5432"
    echo "  - Redis: $SERVER:6379"
    echo "  - MinIO: http://$SERVER:9000"
    echo "  - MinIO Console: http://$SERVER:9001"
    echo "  - Weaviate: http://$SERVER:8080"
    echo "  - ElasticSearch: http://$SERVER:9200"
    echo "  - Kafka: $SERVER:9092"
    echo ""
    echo "常用命令:"
    echo "  查看日志: ssh $USER@$SERVER 'cd $REMOTE_DIR && docker-compose logs -f'"
    echo "  停止服务: ssh $USER@$SERVER 'cd $REMOTE_DIR && docker-compose down'"
    echo "  重启服务: ssh $USER@$SERVER 'cd $REMOTE_DIR && docker-compose restart'"
    echo "  查看状态: ssh $USER@$SERVER 'cd $REMOTE_DIR && docker-compose ps'"
    echo ""
}

# ============================================
# 主流程
# ============================================

main() {
    # 解析参数
    parse_args "$@"

    print_header "GzDoc 部署脚本 - $ENV_NAME 环境"
    echo "服务器: $SERVER"
    echo "用户: $USER"
    echo "目录: $REMOTE_DIR"
    echo "配置文件: $COMPOSE_FILE"
    echo ""

    # 仅重启模式
    if [ "$ONLY_RESTART" = true ]; then
        check_ssh || exit 1
        restart_services
        show_deploy_info
        exit 0
    fi

    # 检查本地环境
    if [ "$SKIP_CHECK" != true ]; then
        check_local_env
        check_ssh || exit 1
        check_remote_docker || exit 1
    fi

    # 创建目录
    create_remote_dir

    # 上传文件
    if [ "$SKIP_UPLOAD" != true ]; then
        upload_files
    fi

    # 检查现有服务
    check_existing_services

    # 启动服务
    start_services

    # 等待服务就绪
    wait_for_services

    # 检查状态
    check_services_status

    # 验证服务
    verify_services

    # 显示部署信息
    show_deploy_info
}

# 执行主流程
main "$@"
