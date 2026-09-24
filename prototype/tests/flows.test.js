const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

function fresh() {
  const memory = new Map();
  global.localStorage = {getItem:k=>memory.get(k)||null,setItem:(k,v)=>memory.set(k,String(v)),removeItem:k=>memory.delete(k)};
  const dp=path.resolve(__dirname,'../assets/data.js'), ap=path.resolve(__dirname,'../assets/app.js');
  delete require.cache[dp]; delete require.cache[ap]; global.DormRepairData=require(dp); return require(ap);
}
function as(app, username){app.logout(); assert.equal(app.login(username,'123456').ok,true);}

test('学生可在重复提示后选择继续提交',()=>{
  const app=fresh(); as(app,'student');
  const input={campus:'东校区',area:'学生生活区',building:'3号楼',room:'502室',faultType:'水暖',description:'洗手池漏水',contact:'林同学',phone:'13800138000'};
  const warned=app.actions.createOrder(input);
  assert.equal(warned.ok,false); assert.equal(warned.data.reason,'duplicate');
  const created=app.actions.createOrder({...input,force:true});
  assert.equal(created.ok,true); assert.ok(created.data.flags.includes('疑似重复'));
});

test('正常维修从接单流转至确认和评价',()=>{
  const app=fresh(); as(app,'worker');
  assert.equal(app.actions.acceptOrder('WO202609200001','2026-09-21T16:00').ok,true);
  assert.equal(app.actions.addProcess('WO202609200001',{content:'更换阀芯并测试'}).ok,true);
  assert.equal(app.actions.addMaterial('WO202609200001',{name:'阀芯',quantity:1,unit:'个'}).ok,true);
  assert.equal(app.actions.completeOrder('WO202609200001',{description:'漏水问题已解决'}).ok,true);
  as(app,'student'); assert.equal(app.actions.reviewOrder('WO202609200001',{type:'confirm'}).ok,true);
  assert.equal(app.actions.reviewOrder('WO202609200001',{type:'rating',rating:5,content:'处理及时'}).ok,true);
  const order=app.getOrder('WO202609200001');
  assert.equal(order.status,'已完成'); assert.equal(order.processes.length,1); assert.equal(order.materials.length,1); assert.equal(order.rating.score,5);
  assert.deepEqual(order.transitions.map(item=>item.type),[
    '学生提交工单','系统派单','维修人员接单','开始维修','维修处理记录','登记维修材料','提交维修完成','学生确认完成'
  ]);
});

test('转派审批前负责人不变，通过后排除原负责人',()=>{
  const app=fresh(); as(app,'worker');
  app.actions.acceptOrder('WO202609200001');
  const request=app.actions.requestTransfer('WO202609200001',{reason:'需要其他工种'});
  assert.equal(app.getOrder('WO202609200001').assigneeId,'W001');
  as(app,'admin'); const approved=app.actions.approveTransfer(request.data.id,true);
  assert.equal(approved.ok,true); assert.equal(app.getOrder('WO202609200001').assigneeId,'W002'); assert.equal(app.getOrder('WO202609200001').status,'待接单');
  assert.deepEqual(app.getOrder('WO202609200001').transitions.slice(-3).map(item=>item.type),['维修人员申请转派','管理员批准转派','系统执行转派']);
});

test('请假未来生效不立即停工，演示生效后逐单改派或进入待办',()=>{
  const app=fresh(); as(app,'admin');
  assert.equal(app.actions.approveLeave('LV20260920001',true,false).ok,true);
  assert.equal(app.getState().workers.find(w=>w.id==='W001').status,'正常');
  app.resetState(); as(app,'admin');
  assert.equal(app.actions.approveLeave('LV20260920001',true,true).ok,true);
  const state=app.getState(); assert.equal(state.workers.find(w=>w.id==='W001').status,'请假中');
  assert.equal(state.orders.filter(o=>o.assigneeId==='W001'&&['待接单','维修中','返工中','已中断'].includes(o.status)).length,0);
});

test('第三次返工触发异常标记且不创建新工单',()=>{
  const app=fresh(); as(app,'student'); const count=app.getState().orders.length;
  const state=app.getState(), order=state.orders.find(o=>o.id==='WO202609180008'); order.status='待确认'; app.saveState(state);
  assert.equal(app.actions.reviewOrder(order.id,{type:'rework',reason:'仍然卡顿'}).ok,true);
  const updated=app.getOrder(order.id); assert.equal(updated.reworkCount,3); assert.ok(updated.flags.includes('异常工单')); assert.equal(app.getState().orders.length,count);
});

test('非法操作与重复业务动作被拒绝',()=>{
  const app=fresh(); as(app,'student');
  assert.equal(app.actions.acceptOrder('WO202609200001').ok,false);
  assert.equal(app.actions.reviewOrder('WO202609190018',{type:'confirm'}).ok,false);
  as(app,'worker'); assert.equal(app.actions.interruptOrder('WO202609200001','等待材料').ok,false);
});

test('接单超时会排除原负责人并给新负责人完整接单时限',()=>{
  const app=fresh(); as(app,'admin');
  const result=app.actions.simulateAcceptTimeout('WO202609200001');
  assert.equal(result.ok,true);
  const order=app.getOrder('WO202609200001');
  assert.equal(order.assigneeId,'W002');
  assert.equal(order.status,'待接单');
  assert.match(order.acceptDeadline,/30分钟/);
  assert.equal(order.transitions.at(-1).type,'接单超时自动转派');
  assert.equal(order.transitions.at(-1).assigneeFrom,'W001');
});

test('学生新建工单可由演示维修账号接单',()=>{
  const app=fresh(); as(app,'student');
  const made=app.actions.createOrder({campus:'西校区',area:'教学区',building:'1号楼',room:'101室',faultType:'门窗',description:'窗户无法关闭',contact:'林同学',phone:'13800138000'});
  assert.equal(made.ok,true); assert.equal(made.data.assigneeId,'W001');
  as(app,'worker'); assert.equal(app.actions.acceptOrder(made.data.id).ok,true);
});

test('维修人员不能修改不属于自己的工单且转派申请幂等',()=>{
  const app=fresh(); as(app,'worker');
  assert.equal(app.actions.completeOrder('WO202609190018',{description:'越权完成'}).ok,false);
  app.actions.acceptOrder('WO202609200001');
  assert.equal(app.actions.requestTransfer('WO202609200001',{reason:'需要其他工种'}).ok,true);
  assert.equal(app.actions.requestTransfer('WO202609200001',{reason:'需要其他工种'}).ok,false);
});

test('转派匹配失败仍持久化审批结果和管理员待办',()=>{
  const app=fresh(); const state=app.getState();
  state.workers.filter(w=>w.id!=='W002').forEach(w=>w.status='停用'); app.saveState(state); as(app,'admin');
  const result=app.actions.approveTransfer('TR20260920001',true);
  assert.equal(result.ok,false);
  assert.equal(app.getState().transferRequests.find(r=>r.id==='TR20260920001').status,'已通过');
  assert.ok(app.getOrder('WO202609190018').flags.includes('管理员待办'));
});

test('已批准请假可以在后续演示节点生效',()=>{
  const app=fresh(); as(app,'admin');
  assert.equal(app.actions.approveLeave('LV20260920001',true,false).ok,true);
  assert.equal(app.actions.activateLeave('LV20260920001').ok,true);
  assert.equal(app.getState().workers.find(w=>w.id==='W001').status,'请假中');
});

test('人工派单不能重开已完成工单',()=>{
  const app=fresh(); as(app,'admin');
  const state=app.getState(); const order=state.orders.find(o=>o.id==='WO202609180006'); order.status='已完成'; app.saveState(state);
  assert.equal(app.actions.manualDispatch(order.id,'W001').ok,false);
});

for (const status of ['待派单','待接单','维修中','已中断','返工中']) {
  test(`学生可以取消${status}工单并保留原负责人审计`,()=>{
    const app=fresh(); as(app,'student');
    const state=app.getState(); const order=state.orders.find(o=>o.studentId==='S001');
    order.status=status; order.assigneeId='W001'; app.saveState(state);
    const result=app.actions.cancelOrder(order.id,`${status}时填写错误`);
    assert.equal(result.ok,true);
    const cancelled=app.getOrder(order.id);
    assert.equal(cancelled.status,'已取消');
    assert.equal(cancelled.assigneeId,null);
    assert.equal(cancelled.transitions.at(-1).type,'学生取消');
    assert.equal(cancelled.transitions.at(-1).assigneeFrom,'W001');
    assert.equal(cancelled.transitions.at(-1).reason,`${status}时填写错误`);
  });
}

test('学生不能取消待确认、已完成或已取消工单',()=>{
  const app=fresh(); as(app,'student');
  for (const status of ['待确认','已完成','已取消']) {
    const state=app.getState(); const order=state.orders.find(o=>o.studentId==='S001');
    order.status=status; app.saveState(state);
    assert.equal(app.actions.cancelOrder(order.id,'填写错误').ok,false,status);
  }
});

test('学生不能取消其他学生的工单',()=>{
  const app=fresh(); as(app,'student');
  const state=app.getState(); const order=state.orders.find(o=>o.id==='WO202609190009');
  order.studentId='S002'; order.status='待派单'; app.saveState(state);
  assert.equal(app.actions.cancelOrder(order.id,'不是本人的工单').ok,false);
});

test('维修人员读取本人消息后消息变为已读并返回跳转目标',()=>{
  const app=fresh(); as(app,'worker');
  const result=app.actions.readNotification('N2');
  assert.equal(result.ok,true);
  assert.equal(result.data.href,'worker-order-detail.html?id=WO202609200001');
  assert.equal(app.getState().notifications.find(item=>item.id==='N2').read,true);
});

test('维修人员不能读取其他角色的消息',()=>{
  const app=fresh(); as(app,'worker');
  assert.equal(app.actions.readNotification('N1').ok,false);
  assert.equal(app.getState().notifications.find(item=>item.id==='N1').read,false);
});

test('管理员可以新增维修人员并停用无未完成工单的账号',()=>{
  const app=fresh(); as(app,'admin');
  const created=app.actions.saveWorker({name:'孙师傅',skills:['水暖'],areas:['西校区']});
  assert.equal(created.ok,true);
  assert.match(created.data.id,/^W/);
  const disabled=app.actions.setWorkerStatus(created.data.id,'停用');
  assert.equal(disabled.ok,true);
  assert.equal(app.getState().workers.find(item=>item.id===created.data.id).status,'停用');
});

test('管理员不能停用仍有未完成工单的维修人员',()=>{
  const app=fresh(); as(app,'admin');
  const result=app.actions.setWorkerStatus('W001','停用');
  assert.equal(result.ok,false);
  assert.match(result.message,/未完成工单/);
});

test('管理员新增区域节点时由父级严格推导学校、区域、楼栋和房间类型',()=>{
  const app=fresh(); as(app,'admin');
  const school=app.actions.addRegionNode(null,{name:'南校区'});
  assert.equal(school.ok,true);
  assert.equal(school.data.type,'campus');
  const area=app.actions.addRegionNode(school.data.id,{name:'教师生活区',type:'building'});
  assert.equal(area.ok,true);
  assert.equal(area.data.type,'area');
  const building=app.actions.addRegionNode(area.data.id,{name:'6号楼',type:'campus'});
  assert.equal(building.ok,true);
  assert.equal(building.data.type,'building');
  const room=app.actions.addRegionNode(building.data.id,{name:'601室',type:'area'});
  assert.equal(room.ok,true);
  assert.equal(room.data.type,'room');
});

test('管理员不能在房间下继续新增节点',()=>{
  const app=fresh(); as(app,'admin');
  const result=app.actions.addRegionNode('east-b1-r106',{name:'床位1'});
  assert.equal(result.ok,false);
  assert.match(result.message,/房间/);
});

test('管理员不能删除仍有下级节点的学校或区域',()=>{
  const app=fresh(); as(app,'admin');
  for (const id of ['east-campus','east-life']) {
    const result=app.actions.deleteRegionNode(id);
    assert.equal(result.ok,false);
    assert.equal(result.message,'请先删除下级节点');
  }
});

test('管理员可以删除房间和没有下级节点的区域',()=>{
  const app=fresh(); as(app,'admin');
  assert.equal(app.actions.deleteRegionNode('east-b1-r106').ok,true);
  assert.equal(app.actions.deleteRegionNode('east-teaching').ok,true);
  const east=app.getState().settings.regionTree.find(node=>node.id==='east-campus');
  assert.equal(east.children.some(node=>node.id==='east-teaching'),false);
  assert.equal(east.children.find(node=>node.id==='east-life').children.find(node=>node.id==='east-b1').children.length,0);
});

test('启用新的工作时间方案会停用原方案',()=>{
  const app=fresh(); as(app,'admin');
  const created=app.actions.saveSchedule({name:'国庆值班',startDate:'2026-10-01',endDate:'2026-10-07',startTime:'09:00',endTime:'17:00'});
  assert.equal(created.ok,true);
  assert.equal(app.actions.activateSchedule(created.data.id).ok,true);
  const enabled=app.getState().settings.schedules.filter(item=>item.enabled);
  assert.deepEqual(enabled.map(item=>item.id),[created.data.id]);
});

test('故障类型可以新增和改名但被工单引用时不能删除',()=>{
  const app=fresh(); as(app,'admin');
  assert.equal(app.actions.saveFaultType({name:'网络设备'}).ok,true);
  assert.equal(app.actions.saveFaultType({oldName:'网络设备',name:'弱电网络'}).ok,true);
  assert.equal(app.actions.deleteFaultType('弱电网络').ok,true);
  const blocked=app.actions.deleteFaultType('水暖');
  assert.equal(blocked.ok,false);
  assert.match(blocked.message,/工单使用/);
});

test('学生注册创建固定STUDENT角色账号且不会自动登录',()=>{
  const app=fresh();
  const result=app.actions.registerStudent({username:'newstudent',password:'secret123',confirmPassword:'secret123',realName:'陈同学',phone:'13700137000'});
  assert.equal(result.ok,true);
  const state=app.getState(),created=state.users.find(item=>item.username==='newstudent');
  assert.equal(created.role,'student');
  assert.equal(created.role_type,'STUDENT');
  assert.equal(created.name,'陈同学');
  assert.equal(created.phone,'13700137000');
  assert.equal(state.session,null);
  assert.equal(app.login('newstudent','secret123').ok,true);
});

test('学生注册拒绝重复用户名、密码不一致和错误手机号',()=>{
  const app=fresh();
  assert.equal(app.actions.registerStudent({username:'student',password:'123456',confirmPassword:'123456',realName:'重复账号',phone:'13700137000'}).ok,false);
  assert.equal(app.actions.registerStudent({username:'newstudent',password:'123456',confirmPassword:'654321',realName:'陈同学',phone:'13700137000'}).ok,false);
  assert.equal(app.actions.registerStudent({username:'newstudent',password:'123456',confirmPassword:'123456',realName:'陈同学',phone:'123'}).ok,false);
});

test('位置选项按校区、区域和楼栋从架构树逐级返回房间',()=>{
  const app=fresh();
  assert.deepEqual(app.getLocationOptions(),{campuses:['东校区','西校区'],areas:[],buildings:[],rooms:[]});
  assert.deepEqual(app.getLocationOptions('东校区'),{campuses:['东校区','西校区'],areas:['学生生活区','教学区'],buildings:[],rooms:[]});
  assert.deepEqual(app.getLocationOptions('东校区','学生生活区'),{campuses:['东校区','西校区'],areas:['学生生活区','教学区'],buildings:['1号楼','3号楼'],rooms:[]});
  assert.deepEqual(app.getLocationOptions('东校区','学生生活区','3号楼'),{campuses:['东校区','西校区'],areas:['学生生活区','教学区'],buildings:['1号楼','3号楼'],rooms:['502室']});
});

test('维修工作台同时返回本人负责工单和学校全部工单',()=>{
  const app=fresh(); as(app,'worker');
  const board=app.getWorkerBoardData();
  assert.equal(board.all.length,app.getState().orders.length);
  assert.ok(board.active.every(order=>order.assigneeId==='W001'&&order.status==='维修中'));
  assert.ok(board.all.some(order=>order.assigneeId==='W002'));
});

test('工单轨迹完整记录从学生提交到维修完成待确认的生命周期',()=>{
  const app=fresh();
  const timeline=app.getOrderTimeline('WO202609180006');
  assert.deepEqual(timeline.map(item=>item.type),[
    '学生提交工单','系统派单','维修人员接单','开始维修','提交维修完成'
  ]);
  assert.equal(timeline[0].actor,'林同学');
  assert.equal(timeline.at(-1).to,'待确认');
});

test('三端工单行生成各自角色的详情地址',()=>{
  const app=fresh();
  assert.equal(app.getOrderDetailUrl('student','WO 001'),'student-order-detail.html?id=WO%20001');
  assert.equal(app.getOrderDetailUrl('worker','WO 001'),'worker-order-detail.html?id=WO%20001');
  assert.equal(app.getOrderDetailUrl('admin','WO 001'),'admin-order-detail.html?id=WO%20001');
});

test('整行工单导航只响应回车和空格键',()=>{
  const app=fresh();
  assert.equal(app.isDetailNavigationKey('Enter'),true);
  assert.equal(app.isDetailNavigationKey(' '),true);
  assert.equal(app.isDetailNavigationKey('Escape'),false);
});
