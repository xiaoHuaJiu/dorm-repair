# 宿舍报修与维修工单系统 HTML 原型 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `prototype/` 中交付一套可直接用浏览器打开、覆盖三类角色与五条核心业务路径的 22 页面交互原型。

**Architecture:** 使用多页面 HTML，共享一套 CSS 和无依赖 JavaScript。`data.js` 定义可重置的演示数据，`app.js` 提供登录、角色守卫、状态读写、业务动作和通用 UI，页面脚本通过 `data-page` 选择渲染器；状态保存在 `localStorage`。

**Tech Stack:** HTML5、CSS3、原生 JavaScript、SVG、Node.js 内置测试模块（仅用于离线验证）。

**Spec:** `docs/superpowers/specs/2026-09-20-dorm-repair-html-prototype-design.md`

## Global Constraints

- 所有产物放在 `prototype/`，不得修改无关文件。
- 页面不连接后端，不编造 HTTP 接口，不发送外部网络请求。
- 不使用外部 CDN、框架、图片或字体，双击 `prototype/index.html` 即可运行。
- 登录演示账号固定为 `student`、`worker`、`admin`，密码均为 `123456`。
- 工单状态仅使用：待派单、待接单、维修中、待确认、返工中、已中断、已完成、已取消。
- 超时、疑似重复、派单失败、转派失败、异常工单只能表现为标记。
- 主色为 `#E53935`，强调色为 `#B71C1C`，背景为 `#F6F7F9`。
- 学生端和维修端移动优先；管理端桌面优先并支持窄屏。
- 当前目录不是 Git 仓库，不执行 `git commit`；每个任务以测试结果作为检查点。

## Review Focus

- 直接打开受保护页面且没有登录状态时，应回到登录页，而不是显示角色数据。
- 已登录角色手动打开其他角色页面时，应阻止访问并进入自己的首页。
- `localStorage` 缺失、损坏或含旧版本数据时，应安全恢复初始演示数据。
- 重复点击接单、确认完成或评价时，不应重复写入业务记录。
- 390px 宽度下表格、弹窗、底部导航和表单不应横向溢出或遮挡主操作。

---

## 文件结构

```text
prototype/
├─ index.html                         # 原型入口，跳转登录页
├─ login.html                         # 登录页
├─ student-home.html                  # 学生首页
├─ student-repair-create.html         # 新建报修
├─ student-orders.html                # 学生工单列表
├─ student-order-detail.html          # 学生工单详情
├─ student-order-review.html          # 确认、返工和评价
├─ worker-home.html                   # 维修工作台
├─ worker-orders.html                 # 维修工单列表
├─ worker-order-detail.html           # 维修工单详情和动作抽屉
├─ worker-process.html                # 维修过程记录
├─ worker-materials.html              # 材料登记
├─ worker-complete.html               # 完成提交
├─ worker-leave.html                  # 请假申请和记录
├─ admin-dashboard.html               # 管理驾驶舱
├─ admin-orders.html                  # 全部工单
├─ admin-order-detail.html            # 管理工单详情与审计
├─ admin-dispatch.html                # 人工派单
├─ admin-exceptions.html              # 异常工单
├─ admin-transfer-approvals.html      # 转派审批
├─ admin-leave-approvals.html         # 请假审批
├─ admin-workers.html                 # 维修人员配置
├─ admin-settings.html                # 故障、区域与作息配置
├─ assets/
│  ├─ styles.css                      # 设计令牌、布局和响应式样式
│  ├─ data.js                         # 初始演示数据和版本号
│  ├─ app.js                          # 状态仓库、业务动作、页面渲染
│  └─ icons.svg                       # 本地 SVG 符号
└─ tests/
   ├─ state.test.js                   # 状态与业务规则测试
   ├─ pages.test.js                   # 页面、链接和静态可访问性检查
   └─ flows.test.js                   # 五条端到端状态流测试
```

### Task 1: 演示数据与状态仓库

**Files:**
- Create: `prototype/assets/data.js`
- Create: `prototype/assets/app.js`
- Create: `prototype/tests/state.test.js`

**Interfaces:**
- Produces: `DormRepairData.createInitialState(): AppState`
- Produces: `DormRepairApp.getState(): AppState`
- Produces: `DormRepairApp.saveState(state: AppState): void`
- Produces: `DormRepairApp.resetState(): AppState`
- Produces: `DormRepairApp.login(username: string, password: string): LoginResult`
- Produces: `DormRepairApp.requireRole(role: 'student'|'worker'|'admin'): boolean`
- Produces: `DormRepairApp.actions`，包含 `createOrder`、`acceptOrder`、`addProcess`、`addMaterial`、`interruptOrder`、`resumeOrder`、`completeOrder`、`reviewOrder`、`requestTransfer`、`approveTransfer`、`createLeave`、`approveLeave`、`manualDispatch`、`resetDemo`。

- [ ] **Step 1: 编写状态仓库失败测试**

使用 Node `node:test`、`assert` 和内存版 `localStorage`，断言初始状态含三个账号、八种合法状态和可用于五条流程的种子工单；断言损坏 JSON 会恢复初始状态；断言登录成功、错误密码失败、角色守卫拒绝跨角色访问。

```js
test('损坏的持久化数据会恢复初始状态', () => {
  storage.setItem('dormRepairPrototype', '{broken');
  const state = app.getState();
  assert.equal(state.meta.version, 1);
  assert.ok(state.orders.length >= 5);
});
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `node --test prototype/tests/state.test.js`

Expected: FAIL，原因是 `data.js` 和 `app.js` 尚不存在。

- [ ] **Step 3: 实现数据模型与状态仓库**

`AppState` 必须包含 `meta`、`session`、`users`、`workers`、`orders`、`transferRequests`、`leaveRequests`、`notifications`、`settings`。每张工单至少包含编号、学生、结构化位置、故障类型、描述、状态、负责人、创建时间、接单截止时间、完成截止时间、标记、过程、材料、流转记录、返工次数、评价。

所有业务动作返回 `{ ok: boolean, message: string, data?: unknown }`；动作成功后统一保存状态；重复动作返回 `ok: false` 且不新增记录。

- [ ] **Step 4: 运行状态测试**

Run: `node --test prototype/tests/state.test.js`

Expected: PASS，包括角色越权、损坏存储、重复接单、重复评价和非法状态动作测试。

### Task 2: 共享视觉系统、页面壳与登录

**Files:**
- Create: `prototype/assets/styles.css`
- Create: `prototype/assets/icons.svg`
- Create: `prototype/index.html`
- Create: `prototype/login.html`
- Create: `prototype/tests/pages.test.js`
- Modify: `prototype/assets/app.js`

**Interfaces:**
- Consumes: `DormRepairApp.login`、`requireRole`、`resetDemo`
- Produces: `DormRepairApp.ui.toast(message, tone)`、`modal(options)`、`renderShell(options)`、`logout()`

- [ ] **Step 1: 编写页面壳失败测试**

测试必须检查：`index.html` 和 `login.html` 存在；所有页面声明 `lang="zh-CN"`、viewport、页面标题；登录页具有用户名、密码的显式 `<label>`；所有 HTML 仅引用相对本地资源；CSS 定义主色、自适应断点、`:focus-visible` 和 `prefers-reduced-motion`。

- [ ] **Step 2: 运行页面测试并确认失败**

Run: `node --test prototype/tests/pages.test.js`

Expected: FAIL，报告入口页或共享样式缺失。

- [ ] **Step 3: 实现设计令牌和响应式组件**

CSS 定义颜色、字号、间距、圆角、阴影、按钮、输入框、标签、列表、数据表、时间线、移动底栏、管理侧栏、弹窗、抽屉、Toast 和空状态。红色轨迹线只用于当前进度及时间线；大面积背景保持中性。

- [ ] **Step 4: 实现入口和登录行为**

`index.html` 使用脚本跳转 `login.html`。登录页展示三个演示账号卡片，点击可填充账号；提交后调用 `login`，成功分别跳转 `student-home.html`、`worker-home.html`、`admin-dashboard.html`，失败在表单附近显示具体错误。

- [ ] **Step 5: 运行页面与状态测试**

Run: `node --test prototype/tests/state.test.js prototype/tests/pages.test.js`

Expected: PASS。

### Task 3: 学生端五个页面

**Files:**
- Create: `prototype/student-home.html`
- Create: `prototype/student-repair-create.html`
- Create: `prototype/student-orders.html`
- Create: `prototype/student-order-detail.html`
- Create: `prototype/student-order-review.html`
- Modify: `prototype/assets/app.js`
- Modify: `prototype/tests/pages.test.js`
- Create: `prototype/tests/flows.test.js`

**Interfaces:**
- Consumes: `requireRole('student')`、`createOrder`、`reviewOrder`
- Produces: 页面渲染器 `renderStudentHome`、`renderStudentCreate`、`renderStudentOrders`、`renderStudentDetail`、`renderStudentReview`

- [ ] **Step 1: 编写学生路径失败测试**

断言学生创建合法报修后生成唯一工单号并出现在本人列表；相同房间、相同故障类型且 24 小时内存在未关闭工单时返回 `duplicate` 信息但允许 `force: true` 继续创建；确认、返工和评价只能在允许状态执行。

```js
test('疑似重复只提示不阻止强制提交', () => {
  const first = app.actions.createOrder(input);
  const warned = app.actions.createOrder(input);
  assert.equal(warned.ok, false);
  assert.equal(warned.data.reason, 'duplicate');
  assert.equal(app.actions.createOrder({...input, force: true}).ok, true);
});
```

- [ ] **Step 2: 运行学生路径测试并确认失败**

Run: `node --test prototype/tests/flows.test.js`

Expected: FAIL，原因是学生动作或页面尚未完成。

- [ ] **Step 3: 实现学生页面和导航**

首页突出“我要报修”和当前工单；创建页包含校区、区域、楼栋、房间、具体位置、故障类型、描述、图片预览、联系人和联系方式；列表支持状态筛选；详情展示工单概要与维修时间线；验收页根据状态展示确认、返工或评价表单。

- [ ] **Step 4: 实现重复报修与表单反馈**

提交前校验必填项和中国大陆手机号格式。疑似重复时弹窗显示原工单号，提供“查看已有工单”“返回修改”“仍然提交”三个明确动作。

- [ ] **Step 5: 运行学生路径和页面检查**

Run: `node --test prototype/tests/state.test.js prototype/tests/pages.test.js prototype/tests/flows.test.js`

Expected: PASS，且五个学生页面都可从底部导航或业务按钮抵达。

### Task 4: 维修人员端七个页面

**Files:**
- Create: `prototype/worker-home.html`
- Create: `prototype/worker-orders.html`
- Create: `prototype/worker-order-detail.html`
- Create: `prototype/worker-process.html`
- Create: `prototype/worker-materials.html`
- Create: `prototype/worker-complete.html`
- Create: `prototype/worker-leave.html`
- Modify: `prototype/assets/app.js`
- Modify: `prototype/tests/pages.test.js`
- Modify: `prototype/tests/flows.test.js`

**Interfaces:**
- Consumes: `acceptOrder`、`addProcess`、`addMaterial`、`interruptOrder`、`resumeOrder`、`completeOrder`、`requestTransfer`、`createLeave`
- Produces: 七个 `renderWorker*` 页面渲染器和工单操作抽屉。

- [ ] **Step 1: 编写维修路径失败测试**

断言只有当前负责人能接单或处理；接单记录预计完成时间或默认加 24 小时；中断必须有原因；恢复只允许已中断工单；完成后进入待确认；转派申请不立即改变负责人；请假开始时间必须早于结束时间。

- [ ] **Step 2: 运行维修路径测试并确认失败**

Run: `node --test prototype/tests/flows.test.js --test-name-pattern="维修|接单|转派|请假"`

Expected: FAIL，指出对应动作或页面缺失。

- [ ] **Step 3: 实现工作台、列表和详情动作**

工作台展示待接单倒计时、即将超时和当前维修；列表可按状态和时限筛选；详情显示位置、联系人、截止时间、过程时间线，并提供接单、中断/恢复、转派、记录过程、材料和提交完成入口。

- [ ] **Step 4: 实现过程、材料、完成和请假页面**

过程页支持说明和图片预览；材料页支持名称、规格、数量、单位、使用时间、备注；完成页支持完成说明和图片；请假页支持起止时间、原因和历史审批结果。

- [ ] **Step 5: 运行全量 Node 测试**

Run: `node --test prototype/tests/*.test.js`

Expected: PASS。

### Task 5: 管理端九个页面

**Files:**
- Create: `prototype/admin-dashboard.html`
- Create: `prototype/admin-orders.html`
- Create: `prototype/admin-order-detail.html`
- Create: `prototype/admin-dispatch.html`
- Create: `prototype/admin-exceptions.html`
- Create: `prototype/admin-transfer-approvals.html`
- Create: `prototype/admin-leave-approvals.html`
- Create: `prototype/admin-workers.html`
- Create: `prototype/admin-settings.html`
- Modify: `prototype/assets/app.js`
- Modify: `prototype/tests/pages.test.js`
- Modify: `prototype/tests/flows.test.js`

**Interfaces:**
- Consumes: `manualDispatch`、`approveTransfer`、`approveLeave` 和共享查询方法。
- Produces: 九个 `renderAdmin*` 页面渲染器、管理侧栏、桌面表格与窄屏卡片视图。

- [ ] **Step 1: 编写管理动作失败测试**

断言人工派单要求候选人技能、区域和可用状态匹配；转派驳回不改变负责人，通过后排除原负责人并回到待接单；请假审批通过但未到开始时间时人员仍正常；演示生效后逐单转派，失败工单进入管理员待办；第三次返工出现在异常列表。

- [ ] **Step 2: 运行管理动作测试并确认失败**

Run: `node --test prototype/tests/flows.test.js --test-name-pattern="管理员|派单|审批|异常"`

Expected: FAIL，指出管理动作或查询缺失。

- [ ] **Step 3: 实现驾驶舱、列表和详情**

驾驶舱展示待人工派单、待审批、超时和异常数量；工单列表支持状态、故障、区域、负责人和标记筛选；详情明确区分工单流转记录与系统操作日志，并展示材料明细。

- [ ] **Step 4: 实现派单、异常和审批页面**

人工派单页显示候选人的技能、区域、状态和未完成工单量；异常页按异常来源分组；转派与请假审批显示申请原因、影响范围和审批后的预期结果。

- [ ] **Step 5: 实现人员和基础配置页面**

人员页展示技能、负责区域、人员状态和负载；设置页以可编辑演示表格展示故障类型、校区区域层级、夏冬工作时间方案。保存仅更新本地演示数据并提示成功。

- [ ] **Step 6: 运行全量 Node 测试**

Run: `node --test prototype/tests/*.test.js`

Expected: PASS，22 个页面全部存在且内部链接无缺失目标。

### Task 6: 五条端到端状态流与幂等性

**Files:**
- Modify: `prototype/tests/flows.test.js`
- Modify: `prototype/assets/app.js`

**Interfaces:**
- Consumes: 全部 `DormRepairApp.actions`
- Produces: 稳定且可重置的完整演示流程。

- [ ] **Step 1: 补齐五条完整路径测试**

分别从重置状态开始执行：正常维修、接单超时改派、主动转派、请假生效批量转派、多次返工。每条路径断言最终状态、当前负责人、关键时间、标记和流转记录均符合规格。

- [ ] **Step 2: 添加幂等和非法顺序测试**

覆盖重复接单、重复完成、重复评价、已完成后继续维修、非负责人操作、跨角色动作和损坏持久化数据。

- [ ] **Step 3: 运行测试并修复最小差异**

Run: `node --test prototype/tests/*.test.js`

Expected: PASS，零失败。

### Task 7: 浏览器视觉与交互验收

**Files:**
- Modify: `prototype/assets/styles.css`
- Modify: `prototype/assets/app.js`
- Modify: 存在视觉或交互问题的对应 `prototype/*.html`

**Interfaces:**
- Consumes: 完整原型。
- Produces: 可交付的浏览器原型。

- [ ] **Step 1: 启动本地静态服务器**

Run: `python -m http.server 4173 --directory prototype`

Expected: `http://localhost:4173/` 可访问并进入登录页。若 Python 不可用，使用 `npx --yes serve prototype -l 4173` 前必须获得下载依赖许可；优先不下载依赖。

- [ ] **Step 2: 验证三类登录和角色守卫**

分别使用三个演示账号登录，确认进入正确首页；直接访问其他角色页面应回到当前角色首页；退出后访问受保护页应回到登录页。

- [ ] **Step 3: 在浏览器逐条执行五个核心流程**

每次先重置演示数据。确认每个动作有即时反馈、状态跨页面同步、刷新后保留，并且流程最终状态与规格一致。

- [ ] **Step 4: 检查 390px 与 1440px 布局**

在两种视口逐页抽查登录页、三端首页、三端详情页、创建报修、人工派单和审批页。修复横向溢出、内容遮挡、不可见焦点、过小点击区域和弹窗无法关闭问题。

- [ ] **Step 5: 执行最终自动验证**

Run: `node --test prototype/tests/*.test.js`

Expected: PASS，零失败。

- [ ] **Step 6: 检查交付目录**

Run: `rg --files prototype | Sort-Object`

Expected: 22 个主要 HTML 页面、`index.html`、4 个共享资源文件和 3 个测试文件全部存在；没有临时截图、下载依赖、密钥或真实个人信息。

## 完成报告要求

交付时必须说明：

- 实际创建和修改了什么。
- 自动测试命令与结果。
- 浏览器验证过的账号、视口和五条流程。
- 尚未实现或无法验证的内容。
- 原型入口的绝对路径。
