const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const pages = [
  'login.html','student-home.html','student-repair-create.html','student-orders.html','student-order-detail.html','student-order-review.html',
  'worker-home.html','worker-orders.html','worker-order-detail.html','worker-process.html','worker-materials.html','worker-complete.html','worker-leave.html','worker-messages.html',
  'admin-dashboard.html','admin-orders.html','admin-order-detail.html','admin-dispatch.html','admin-exceptions.html','admin-transfer-approvals.html','admin-leave-approvals.html','admin-workers.html','admin-regions.html','admin-schedules.html','admin-fault-types.html','admin-settings.html'
];

test('26个主要页面与入口页全部存在', () => {
  for (const page of ['index.html', ...pages]) assert.ok(fs.existsSync(path.join(root, page)), `${page} 不存在`);
});

test('所有主要页面具备中文语义、视口和本地资源', () => {
  for (const page of pages) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /<html lang="zh-CN">/, page);
    assert.match(html, /name="viewport"/, page);
    assert.match(html, /<title>[^<]+<\/title>/, page);
    assert.doesNotMatch(html, /https?:\/\//, `${page} 引用了外部资源`);
    assert.match(html, /assets\/styles\.css/, page);
    assert.match(html, /assets\/data\.js/, page);
    assert.match(html, /assets\/app\.js/, page);
  }
});

test('登录表单有显式标签和密码字段', () => {
  const html = fs.readFileSync(path.join(root, 'login.html'), 'utf8');
  assert.match(html, /<label[^>]*for="username"/);
  assert.match(html, /<label[^>]*for="password"/);
  assert.match(html, /type="password"/);
  for (const field of ['register-username','register-password','register-confirm-password','register-real-name','register-phone']) assert.match(html,new RegExp(`id="${field}"`));
});

test('共享样式包含焦点、减少动画和移动断点', () => {
  const css = fs.readFileSync(path.join(root, 'assets/styles.css'), 'utf8');
  assert.match(css, /#E53935/i);
  assert.match(css, /:focus-visible/);
  assert.match(css, /prefers-reduced-motion/);
  assert.match(css, /max-width:\s*600px/);
});

test('HTML内部链接均指向存在的页面', () => {
  for (const page of pages) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    for (const [, href] of html.matchAll(/href="([^"#]+\.html)(?:\?[^"#]*)?"/g)) {
      assert.ok(fs.existsSync(path.join(root, href)), `${page} -> ${href} 断链`);
    }
  }
});

test('学生页面加载独立交互脚本以支持双栏导航和报修弹窗', () => {
  for (const page of ['student-home.html','student-orders.html','student-order-detail.html','student-repair-create.html']) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /assets\/student-ui\.js/, page);
  }
  assert.ok(fs.existsSync(path.join(root,'assets/student-ui.js')));
});

test('维修人员页面加载独立交互脚本并提供消息中心页面', () => {
  for (const page of ['worker-home.html','worker-orders.html','worker-order-detail.html','worker-process.html','worker-materials.html','worker-complete.html','worker-leave.html','worker-messages.html']) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /assets\/worker-ui\.js/, page);
  }
  assert.ok(fs.existsSync(path.join(root,'assets/worker-ui.js')));
});

test('管理员页面加载独立交互脚本并拆分基础维护页面', () => {
  for (const page of ['admin-dashboard.html','admin-orders.html','admin-order-detail.html','admin-dispatch.html','admin-exceptions.html','admin-transfer-approvals.html','admin-leave-approvals.html','admin-workers.html','admin-regions.html','admin-schedules.html','admin-fault-types.html']) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /assets\/admin-ui\.js/, page);
  }
  assert.ok(fs.existsSync(path.join(root,'assets/admin-ui.js')));
});
