(function (root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  root.DormRepairData = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  const ORDER_STATUSES = ['待派单', '待接单', '维修中', '待确认', '返工中', '已中断', '已完成', '已取消'];
  const now = '2026-09-20 10:20';

  function transition(type, from, to, actor, reason, assigneeFrom, assigneeTo, time = now) {
    return { type, from, to, actor, reason, assigneeFrom, assigneeTo, time };
  }

  function lifecycle(student, createdAt, worker, workerId, events = []) {
    const items = [transition('学生提交工单', null, '待派单', student, '填写并提交报修信息', null, null, createdAt)];
    if (workerId) items.push(transition('系统派单', '待派单', '待接单', '系统', `自动派单给${worker}`, null, workerId, createdAt));
    return items.concat(events);
  }

  function createInitialState() {
    return {
      meta: { version: 3, updatedAt: now },
      session: null,
      users: [
        { id: 'S001', username: 'student', password: '123456', role: 'student', name: '林同学' },
        { id: 'W001', username: 'worker', password: '123456', role: 'worker', name: '张师傅' },
        { id: 'A001', username: 'admin', password: '123456', role: 'admin', name: '王老师' }
      ],
      workers: [
        { id: 'W001', name: '张师傅', skills: ['水暖', '门窗'], areas: ['东校区'], status: '正常', workload: 3 },
        { id: 'W002', name: '李师傅', skills: ['水暖', '电气'], areas: ['东校区', '西校区'], status: '正常', workload: 1 },
        { id: 'W003', name: '赵师傅', skills: ['电气'], areas: ['西校区'], status: '请假中', workload: 0 },
        { id: 'W004', name: '陈师傅', skills: ['家具', '门窗'], areas: ['东校区'], status: '正常', workload: 2 }
      ],
      orders: [
        { id:'WO202609190020', studentId:'S002', location:{campus:'东校区',area:'学生生活区',building:'4号楼',room:'216室',detail:'公共洗衣房'}, faultType:'水暖', title:'洗衣机进水管渗水', description:'接口处持续渗水，需要检查密封圈。', contact:'周同学', phone:'13900139000', status:'维修中', assigneeId:'W001', createdAt:'2026-09-19 16:20', acceptDeadline:'2026-09-19 16:50', completeDeadline:'2026-09-21 12:00', flags:[], images:[], processes:[{time:'2026-09-20 08:40',worker:'张师傅',content:'已检查接口，准备更换密封圈。'}], materials:[], transitions:lifecycle('周同学','2026-09-19 16:20','张师傅','W001',[transition('维修人员接单','待接单','维修中','张师傅','确认接单','W001','W001','2026-09-19 16:28'),transition('开始维修','维修中','维修中','张师傅','开始现场检修','W001','W001','2026-09-19 16:29')]), reworkCount:0, rating:null },
        { id: 'WO202609200001', studentId: 'S001', location: {campus:'东校区', area:'学生生活区', building:'3号楼', room:'502室', detail:'卫生间洗手池下方'}, faultType:'水暖', title:'洗手池持续漏水', description:'阀门关闭后仍然滴水，地面积水。', contact:'林同学', phone:'13800138000', status:'待接单', assigneeId:'W001', createdAt:'2026-09-20 09:40', acceptDeadline:'2026-09-20 10:30', completeDeadline:null, flags:['即将超时'], images:[], processes:[], materials:[], transitions:lifecycle('林同学','2026-09-20 09:40','张师傅','W001'), reworkCount:0, rating:null },
        { id: 'WO202609190018', studentId: 'S001', location:{campus:'东校区',area:'学生生活区',building:'3号楼',room:'502室',detail:'阳台顶灯'}, faultType:'电气', title:'阳台灯不亮', description:'更换灯泡后仍然不亮。', contact:'林同学', phone:'13800138000', status:'维修中', assigneeId:'W002', createdAt:'2026-09-19 15:10', acceptDeadline:'2026-09-19 15:40', completeDeadline:'2026-09-20 18:00', flags:[], images:[], processes:[{time:'2026-09-20 09:05',worker:'李师傅',content:'检查开关与灯座，发现线路接触不良。'}], materials:[], transitions:lifecycle('林同学','2026-09-19 15:10','李师傅','W002',[transition('维修人员接单','待接单','维修中','李师傅','确认接单','W002','W002','2026-09-19 15:20'),transition('开始维修','维修中','维修中','李师傅','开始现场检修','W002','W002','2026-09-19 15:21')]), reworkCount:0, rating:null },
        { id: 'WO202609190012', studentId:'S001', location:{campus:'东校区',area:'学生生活区',building:'3号楼',room:'502室',detail:'宿舍门锁'}, faultType:'门窗', title:'宿舍门锁卡顿', description:'钥匙转动困难。', contact:'林同学', phone:'13800138000', status:'已中断', assigneeId:'W001', createdAt:'2026-09-19 10:00', acceptDeadline:'2026-09-19 10:30', completeDeadline:'2026-09-20 12:00', flags:['等待材料'], images:[], processes:[{time:'2026-09-19 11:30',worker:'张师傅',content:'确认锁芯磨损，等待同型号锁芯。'}], materials:[], transitions:lifecycle('林同学','2026-09-19 10:00','张师傅','W001',[transition('维修人员接单','待接单','维修中','张师傅','确认接单','W001','W001','2026-09-19 10:15'),transition('开始维修','维修中','维修中','张师傅','开始检查门锁','W001','W001','2026-09-19 10:16'),transition('维修中断','维修中','已中断','张师傅','等待材料','W001','W001','2026-09-19 11:32')]), reworkCount:0, rating:null },
        { id: 'WO202609190009', studentId:'S002', location:{campus:'东校区',area:'学生生活区',building:'5号楼',room:'214室',detail:'空调插座'}, faultType:'电气', title:'空调插座无电', description:'插座没有供电。', contact:'周同学', phone:'13900139000', status:'待派单', assigneeId:null, createdAt:'2026-09-19 09:20', acceptDeadline:null, completeDeadline:null, flags:['派单失败','管理员待办'], images:[], processes:[], materials:[], transitions:lifecycle('周同学','2026-09-19 09:20',null,null,[transition('自动派单失败','待派单','待派单','系统','没有同时满足技能和区域条件的可用人员',null,null,'2026-09-19 09:21'),transition('进入管理员待办','待派单','待派单','系统','等待管理员人工派单',null,null,'2026-09-19 09:21')]), reworkCount:0, rating:null },
        { id: 'WO202609180008', studentId:'S001', location:{campus:'东校区',area:'学生生活区',building:'2号楼',room:'308室',detail:'书桌'}, faultType:'家具', title:'书桌抽屉脱轨', description:'抽屉无法正常关闭。', contact:'林同学', phone:'13800138000', status:'返工中', assigneeId:'W001', createdAt:'2026-09-18 08:30', acceptDeadline:null, completeDeadline:'2026-09-20 16:00', flags:['二次返工','管理员预警'], images:[], processes:[], materials:[], transitions:lifecycle('林同学','2026-09-18 08:30','张师傅','W001',[transition('维修人员接单','待接单','维修中','张师傅','确认接单','W001','W001','2026-09-18 08:42'),transition('开始维修','维修中','维修中','张师傅','开始现场检修','W001','W001','2026-09-18 08:43'),transition('提交维修完成','维修中','待确认','张师傅','首次维修完成，等待学生确认','W001','W001','2026-09-18 10:10'),transition('学生申请返工','待确认','返工中','林同学','抽屉仍然卡住','W001','W001','2026-09-19 08:10'),transition('提交维修完成','返工中','待确认','张师傅','返工处理完成，等待学生确认','W001','W001','2026-09-19 17:20'),transition('学生申请二次返工','待确认','返工中','林同学','抽屉仍然卡住','W001','W001','2026-09-20 08:10'),transition('管理员预警','返工中','返工中','系统','工单已发生二次返工','W001','W001','2026-09-20 08:10')]), reworkCount:2, rating:null },
        { id: 'WO202609180006', studentId:'S001', location:{campus:'东校区',area:'学生生活区',building:'1号楼',room:'106室',detail:'窗户'}, faultType:'门窗', title:'窗户把手松动', description:'把手无法锁紧。', contact:'林同学', phone:'13800138000', status:'待确认', assigneeId:'W001', createdAt:'2026-09-18 07:50', acceptDeadline:null, completeDeadline:'2026-09-19 12:00', flags:[], images:[], processes:[{time:'2026-09-19 11:20',worker:'张师傅',content:'更换把手并完成开合测试。'}], materials:[{name:'窗户把手',spec:'通用型',quantity:1,unit:'个',time:'2026-09-19 11:10'}], result:{description:'已更换把手，窗户可正常锁闭。',time:'2026-09-19 11:30'}, transitions:lifecycle('林同学','2026-09-18 07:50','张师傅','W001',[transition('维修人员接单','待接单','维修中','张师傅','确认接单','W001','W001','2026-09-18 08:05'),transition('开始维修','维修中','维修中','张师傅','开始现场检修','W001','W001','2026-09-18 08:06'),transition('提交维修完成','维修中','待确认','张师傅','维修完成，等待学生确认','W001','W001','2026-09-19 11:30')]), reworkCount:0, rating:null }
      ],
      transferRequests: [{id:'TR20260920001',orderId:'WO202609190018',workerId:'W002',reason:'需要其他工种',detail:'线路可能涉及墙内布线，需要电工协同。',status:'待审批',createdAt:'2026-09-20 09:40'}],
      leaveRequests: [{id:'LV20260920001',workerId:'W001',start:'2026-09-22 08:00',end:'2026-09-24 18:00',reason:'个人事务',status:'待审批',createdAt:'2026-09-20 09:00'}],
      notifications: [
        {id:'N1',role:'admin',title:'1 张工单等待人工派单',time:'10分钟前',read:false},
        {id:'N2',role:'worker',userId:'W001',type:'自动派单',title:'收到新的系统派单',detail:'WO202609200001 距接单截止还有10分钟，请及时确认。',time:'刚刚',read:false,href:'worker-order-detail.html?id=WO202609200001'},
        {id:'N3',role:'worker',userId:'W001',type:'请假审批',title:'请假申请正在等待审批',detail:'2026-09-22 至 2026-09-24 的请假申请已提交。',time:'1小时前',read:true,href:'worker-leave.html'},
        {id:'N4',role:'worker',userId:'W001',type:'转派工单',title:'收到一张转派工单',detail:'宿舍门锁卡顿已转派给你，请查看维修记录。',time:'昨天',read:false,href:'worker-order-detail.html?id=WO202609190012'},
        {id:'N5',role:'worker',userId:'W001',type:'返工提醒',title:'工单需要二次返工',detail:'学生反馈问题仍未解决，请重新检查处理。',time:'2天前',read:true,href:'worker-orders.html?status=返工中'}
      ],
      settings: {faultTypes:['水暖','电气','门窗','家具'], campuses:['东校区','西校区'], regionTree:[{id:'east-campus',name:'东校区',type:'campus',children:[{id:'east-life',name:'学生生活区',type:'area',children:[{id:'east-b1',name:'1号楼',type:'building',children:[{id:'east-b1-r106',name:'106室',type:'room',children:[]}]},{id:'east-b3',name:'3号楼',type:'building',children:[{id:'east-b3-r502',name:'502室',type:'room',children:[]}]}]},{id:'east-teaching',name:'教学区',type:'area',children:[]}]},{id:'west-campus',name:'西校区',type:'campus',children:[{id:'west-life',name:'学生生活区',type:'area',children:[]}]}], schedules:[{id:'SCH001',name:'夏季作息',startDate:'2026-05-01',endDate:'2026-09-30',startTime:'08:00',endTime:'18:00',enabled:true},{id:'SCH002',name:'冬季作息',startDate:'2026-10-01',endDate:'2027-04-30',startTime:'08:30',endTime:'17:30',enabled:false}]}
    };
  }
  return { ORDER_STATUSES, createInitialState };
});
