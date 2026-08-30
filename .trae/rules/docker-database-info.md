# Docker 数据库环境信息汇总

> 记录日期：2026-08-30
> 用途：记录当前本机 Docker 数据库环境，便于快速连接、排查与重建。

## 一、运行中的容器

| 容器名 | 镜像 | 状态 | 端口映射 | 数据库 |
|--------|------|------|----------|--------|
| `mysql8` | `mysql:8.0` | Up | `13307→3306` | MySQL 8.0 |
| `postgres` | `postgres:15` | Up | `5432→5432` | PostgreSQL 15 |
| `datastream-dm8-test` | `qinchz/dm8-arm64:latest` | Up | `5236→5236` | 达梦 DM8 |

## 二、连接信息

### 1. MySQL 8.0

- 容器名：`mysql8`
- 端口：`13307`（宿主）→ `3306`（容器）
- 账号：`root` / `root123`
- 数据库：`dbtest1`
- JDBC：`jdbc:mysql://localhost:13307/dbtest1`
- 数据卷：匿名卷 → `/var/lib/mysql`

### 2. PostgreSQL 15

- 容器名：`postgres`
- 端口：`5432`（宿主）→ `5432`（容器）
- 账号：`postgres` / `postgres123`（未单独设用户，默认 superuser）
- 数据库：`testdb`
- JDBC：`jdbc:postgresql://localhost:5432/testdb`
- 数据卷：匿名卷 → `/var/lib/postgresql/data`

### 3. 达梦 DM8（ARM64 社区镜像）

- 容器名：`datastream-dm8-test`
- 镜像：`qinchz/dm8-arm64:latest`（社区镜像，基于官方 DM8 试用版·鲲鹏 ARM64 平台）
- 端口：`5236`（宿主）→ `5236`（容器）
- 账号：`SYSDBA` / `SYSDBA`
- JDBC：`jdbc:dm://localhost:5236`
- 数据卷：`dm8_data`（命名卷）→ `/home/dmdba/data`
- 版本：DM Database Server 64 V8，DB Version `0x7000c`（8.1.3.62，构建 20230927）
- 实例参数：`CHARSET=1`(UTF-8)、`LENGTH_IN_CHAR=1`、`CASE_SENSITIVE=0`、`PAGE_SIZE=16`
- 模拟表：`SYSDBA.DM_TEST_TABLE`（15 列，3 条数据，含中文/CLOB/二进制）
- ⚠️ 试用授权：`License will expire on 2026-09-13`，到期连接报 6001 时重启容器可续期约一个月

## 三、镜像

| 镜像 | 大小 |
|------|------|
| `mysql:8.0` | 1.08 GB |
| `postgres:15` | 654 MB |
| `qinchz/dm8-arm64:latest` | 4.09 GB |

> 说明：`mysql`/`postgres` 同时存在 `docker.m.daocloud.io/...` 别名标签；达梦同时存在 `docker.1ms.run/qinchz/dm8-arm64:latest` 拉取源标签。

## 四、数据卷

| 卷名 | 用途 |
|------|------|
| `dm8_data` | 达梦数据目录（`/home/dmdba/data`） |
| 4 个匿名哈希卷 | MySQL、PostgreSQL 等数据目录 |

## 五、测试环境 Compose

`doc/sql/test-data/docker-compose.yml` 定义的服务（与运行中容器不完全一致）：

| 服务 | 镜像 | 端口 | 备注 |
|------|------|------|------|
| `mysql-test` | `mysql:8.0` | `3307→3306` | 与运行中 `mysql8`（13307）不一致 |
| `postgres-test` | `postgres:14` | `5433→5432` | 与运行中 `postgres`（5432）不一致 |
| `oracle-test` | `gvenzl/oracle-xe:21-slim` | `1521→1521` | 尚未运行 |
| `dm8-test` | `qinchz/dm8-arm64:latest` | `5236→5236` | 与运行中一致 |
| `pgadmin` | `dpage/pgadmin4:latest` | `5050→80` | 可选 |
| `adminer` | `adminer:latest` | `8080→8080` | 可选 |

## 六、注意事项 / 踩坑记录

1. **镜像拉取**：Docker Hub 直连被墙，需走加速镜像。可用 `docker.1ms.run`（可达），`docker.m.daocloud.io` 现为白名单制（达梦镜像不在白名单）。
2. **达梦 disql 中文**：容器 locale 为 `POSIX`，执行含中文的 SQL 需先 `export LANG=en_US.UTF-8`，否则多行语句报 `-2007 Syntax error`。
3. **达梦类型精度**：`NUMBER(9)` 最多 9 位数字（`-2147483648` 会报 `-6149 Data lose`）；`NUMBER(18)` 最多 18 位。
4. **Navicat 连达梦**：需要达梦 ODBC 驱动（官方仅提供 Windows/Linux 版，无 macOS 版）；macOS 建议用 DBeaver + JDBC 驱动。
5. **达梦 JDBC 驱动**：已从容器导出至 `~/Downloads/DmJdbcDriver18.jar`（容器内路径 `/home/dmdba/dmdbms/drivers/jdbc/DmJdbcDriver18.jar`）。
6. **建表 SQL**：达梦模拟表脚本位于 `doc/sql/test-data/dameng-test-data.sql`。

## 七、常用命令

```bash
# 查看容器状态
docker ps

# 进入达梦并用 disql 连接（含中文需设 UTF-8 locale）
docker exec -it datastream-dm8-test sh -c 'export LANG=en_US.UTF-8; LD_LIBRARY_PATH=/home/dmdba/dmdbms/bin /home/dmdba/dmdbms/bin/disql SYSDBA/SYSDBA@localhost:5236'

# 执行达梦 SQL 脚本
docker exec -i datastream-dm8-test sh -c 'export LANG=en_US.UTF-8; LD_LIBRARY_PATH=/home/dmdba/dmdbms/bin /home/dmdba/dmdbms/bin/disql SYSDBA/SYSDBA@localhost:5236' < doc/sql/test-data/dameng-test-data.sql
```
