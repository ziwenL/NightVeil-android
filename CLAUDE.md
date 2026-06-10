# CLAUDE.md

本项目使用 `ai-engineering-skills` 作为 AI Coding 工程化能力仓库。

Claude Code 使用本文件作为项目级入口。

核心 Skills 位于：

```text
.ai/ai-engineering-skills/skills/
```

## 启动规则

开始任何任务前，必须先阅读：

1. `CLAUDE.md`
2. `.ai/ai-engineering-skills/skills/common/`
3. 与当前任务相关的 Skill
4. 项目中相关代码和相似实现

如果上下文不足，不要猜测，先列出需要补充的信息。

## Skill 执行补充规则

1. 如果某个 Skill 目录下还有 `checklist.md`、`architecture.md`、`log-analysis.md` 等辅助文件，读取 `SKILL.md` 后还必须继续读取并使用这些辅助文件。
2. 开发类 Skill 输出时，至少要明确：
   - 验证方式
   - 验证结果
   - 未验证项
3. 审查类 Skill 输出时，至少要明确：
   - 证据
   - 风险等级
   - 是否阻塞合并或发版
4. 如果某项结论缺少证据支撑，不要伪装成确定结论。

## Skill 路由

| 任务类型 | 必须读取 |
|---|---|
| 新项目 / 新会话上下文 | `.ai/ai-engineering-skills/skills/context-bootstrap/SKILL.md` |
| 需求分析 | `.ai/ai-engineering-skills/skills/requirement-analysis/SKILL.md` |
| 技术方案 | `.ai/ai-engineering-skills/skills/technical-design/SKILL.md` |
| 接口设计 | `.ai/ai-engineering-skills/skills/api-design/SKILL.md` |
| 数据模型设计 | `.ai/ai-engineering-skills/skills/data-model-design/SKILL.md` |
| Android 开发 | `.ai/ai-engineering-skills/skills/android-development/SKILL.md` |
| iOS 开发 | `.ai/ai-engineering-skills/skills/ios-development/SKILL.md` |
| Android 复刻 iOS | `.ai/ai-engineering-skills/skills/android-to-ios-porting/SKILL.md` |
| 服务端开发 | `.ai/ai-engineering-skills/skills/backend-development/SKILL.md` |
| 前端开发 | `.ai/ai-engineering-skills/skills/frontend-development/SKILL.md` |
| 测试用例 | `.ai/ai-engineering-skills/skills/test-case-generation/SKILL.md` |
| Code Review | `.ai/ai-engineering-skills/skills/code-review/SKILL.md` |
| 安全审查 | `.ai/ai-engineering-skills/skills/security-review/SKILL.md` |
| 性能审查 | `.ai/ai-engineering-skills/skills/performance-review/SKILL.md` |
| 构建 / CI 修复 | `.ai/ai-engineering-skills/skills/build-ci-fix/SKILL.md` |
| 线上 Bug 定位 | `.ai/ai-engineering-skills/skills/bug-triage/SKILL.md` |
| 发版检查 | `.ai/ai-engineering-skills/skills/release-check/SKILL.md` |
| 监控分析 | `.ai/ai-engineering-skills/skills/monitoring-analysis/SKILL.md` |
| 事故应急 | `.ai/ai-engineering-skills/skills/incident-response/SKILL.md` |
| 事故复盘 | `.ai/ai-engineering-skills/skills/postmortem/SKILL.md` |
| 会话沉淀 | `.ai/ai-engineering-skills/skills/session-retrospective/SKILL.md` |
| Skill 生成 / 迭代 | `.ai/ai-engineering-skills/skills/skill-authoring/SKILL.md` |

## 默认工作流

```text
context-bootstrap
→ requirement-analysis
→ technical-design
→ test-case-generation
→ development
→ code-review
→ session-retrospective
```

## 开发约束

1. 不修改与任务无关的文件。
2. 不做无关重构。
3. 不引入新依赖，除非用户确认。
4. 不提交密钥、Token、证书、隐私数据。
5. 高风险模块必须先出方案，不允许直接改代码。
6. 修改完成后必须说明修改、验证、未验证项和风险。
7. 每次任务结束后必须判断是否需要沉淀经验。
