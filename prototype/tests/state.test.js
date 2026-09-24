const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

function loadApp() {
  const dataPath = path.resolve(__dirname, '../assets/data.js');
  const appPath = path.resolve(__dirname, '../assets/app.js');
  delete require.cache[dataPath];
  delete require.cache[appPath];
  const memory = new Map();
  global.localStorage = {
    getItem: key => memory.has(key) ? memory.get(key) : null,
    setItem: (key, value) => memory.set(key, String(value)),
    removeItem: key => memory.delete(key)
  };
  global.DormRepairData = require(dataPath);
  return require(appPath);
}

test('初始状态包含三类账号和八种合法工单状态', () => {
  const app = loadApp();
  const state = app.getState();
  assert.deepEqual(state.users.map(user => user.username), ['student', 'worker', 'admin']);
  assert.deepEqual(global.DormRepairData.ORDER_STATUSES, ['待派单', '待接单', '维修中', '待确认', '返工中', '已中断', '已完成', '已取消']);
  assert.ok(state.orders.length >= 5);
});

test('损坏的持久化数据会恢复初始状态', () => {
  const app = loadApp();
  localStorage.setItem('dormRepairPrototype', '{broken');
  const state = app.getState();
  assert.equal(state.meta.version, 3);
  assert.ok(state.orders.length >= 5);
});

test('登录校验密码并阻止跨角色访问', () => {
  const app = loadApp();
  assert.equal(app.login('student', 'wrong').ok, false);
  assert.equal(app.login('student', '123456').ok, true);
  assert.equal(app.requireRole('student'), true);
  assert.equal(app.requireRole('admin'), false);
});

test('重复接单不会重复写入流转记录', () => {
  const app = loadApp();
  app.login('worker', '123456');
  const before = app.getOrder('WO202609200001').transitions.length;
  assert.equal(app.actions.acceptOrder('WO202609200001', '2026-09-21T16:00').ok, true);
  assert.equal(app.actions.acceptOrder('WO202609200001', '2026-09-21T16:00').ok, false);
  const transitions=app.getOrder('WO202609200001').transitions;
  assert.equal(transitions.length, before + 2);
  assert.deepEqual(transitions.slice(-2).map(item=>item.type),['维修人员接单','开始维修']);
});

test('重复评价不会生成第二条评价', () => {
  const app = loadApp();
  app.login('student', '123456');
  assert.equal(app.actions.reviewOrder('WO202609180006', {type: 'confirm'}).ok, true);
  assert.equal(app.actions.reviewOrder('WO202609180006', {type: 'rating', rating: 5, content: '处理及时'}).ok, true);
  assert.equal(app.actions.reviewOrder('WO202609180006', {type: 'rating', rating: 4, content: '重复'}).ok, false);
});

test('结构正确但字段残缺的存储会恢复初始状态', () => {
  const app = loadApp();
  localStorage.setItem('dormRepairPrototype', JSON.stringify({meta:{version:1},orders:[]}));
  assert.equal(app.login('student','123456').ok, true);
  assert.equal(app.getState().users.length, 3);
});

test('未登录用户不能执行维修动作', () => {
  const app = loadApp();
  assert.equal(app.actions.completeOrder('WO202609190018',{description:'越权完成'}).ok, false);
});
