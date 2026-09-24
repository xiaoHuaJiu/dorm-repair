# 项目协作入口

本项目的开发规则统一存放在 [`doc/rules/`](doc/rules/) 目录中。

开始分析、设计、编码或评审前，必须先阅读 [`doc/rules/project.md`](doc/rules/project.md)，并根据当前任务查阅 `doc/rules/addons/` 中适用的附加规则。

项目文档按以下目录维护：

- `doc/requirements/`：用户原始需求、范围变更与验收标准。
- `doc/architecture/`：系统边界、模块关系、数据流及关键技术决策。
- `doc/api/`：前后端接口契约。
- `doc/database/`：表结构、索引、字段及数据约束。
- `doc/security/`：身份认证、权限控制和安全边界。
- `doc/rules/`：项目通用开发规则及专项规则。
- doc/verify/：各阶段工作的验收记录。

若规则与已确认的用户需求冲突，以用户最新明确要求为准，并同步更新相关文档。
