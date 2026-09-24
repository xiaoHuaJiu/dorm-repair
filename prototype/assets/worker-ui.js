(function(root){
  'use strict';
  const esc=value=>String(value??'').replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  const App=()=>root.DormRepairApp;
  const state=()=>App().getState();
  const user=()=>App().currentUser(state());
  const mine=()=>state().orders.filter(order=>order.assigneeId===user().id);
  const empty=copy=>`<div class="empty compact"><div class="empty-mark">✓</div>${esc(copy)}</div>`;
  const tags=order=>`<span class="tag ${order.status==='已完成'?'green':['待接单','待确认'].includes(order.status)?'amber':'red'}">${esc(order.status)}</span>${order.flags.map(flag=>`<span class="tag red">${esc(flag)}</span>`).join('')}`;
  function card(order,action='查看详情',showOwner=false){
    return `<article class="worker-order-card clickable-order" data-status="${esc(order.status)}" tabindex="0" role="link" aria-label="查看工单 ${esc(order.id)} ${esc(order.title)}" data-order-detail="${esc(App().getOrderDetailUrl('worker',order.id))}"><div class="worker-card-main"><div class="order-id">${esc(order.id)}</div><h3>${esc(order.title)}</h3><div class="meta"><span>${esc(order.location.building)} ${esc(order.location.room)}</span><span>${esc(order.faultType)}</span>${showOwner?`<span>负责人：${esc(App().workerName(state(),order.assigneeId))}</span>`:''}<span>${esc(order.completeDeadline||order.acceptDeadline||'待确定期限')}</span></div><div class="worker-card-tags">${tags(order)}</div></div></article>`;
  }
  function installInbox(){
    const top=document.querySelector('.top-actions');if(!top||top.querySelector('.message-link'))return;
    const unread=state().notifications.filter(item=>item.role==='worker'&&(!item.userId||item.userId===user().id)&&!item.read).length;
    const link=document.createElement('a');link.className='message-link';link.href='worker-messages.html';link.setAttribute('aria-label',`消息中心${unread?`，${unread}条未读`:''}`);link.innerHTML=`<span aria-hidden="true">✉</span>${unread?'<i class="message-dot"></i>':''}`;top.prepend(link);
  }
  function mount(page,title,subtitle,body,headAction=''){
    document.getElementById('app').innerHTML=App().ui.shell('worker',page+'.html',title,subtitle,body);
    if(headAction)document.querySelector('.page-head')?.insertAdjacentHTML('beforeend',headAction);
    installInbox();
    App().ui.bindDetailRows();
    document.querySelector('[data-logout]')?.addEventListener('click',()=>{App().logout();location.href='login.html'});
    document.querySelector('[data-reset]')?.addEventListener('click',()=>App().ui.modal({title:'重置演示数据',content:'<p>将恢复所有预置工单和消息状态。</p>',actions:[{label:'取消',onClick:wrap=>wrap.remove()},{label:'确认重置',className:'primary',onClick:()=>{App().resetState();location.reload()}}]}));
  }
  function renderHome(){
    const board=App().getWorkerBoardData(),{active,pending,alerts}=board;
    mount('worker-home','今日工作台',`${user().name}，优先处理待接单与异常任务`,`<div class="worker-board"><div class="worker-main-lanes"><section class="worker-lane lane-active"><div class="lane-head"><div><span class="lane-kicker">正在处理</span><h2>我负责的维修工单</h2></div><strong>${active.length}</strong></div><div class="lane-list">${active.map(order=>card(order,'继续处理')).join('')||empty('当前没有维修中的工单')}</div></section><section class="worker-lane lane-all"><div class="lane-head"><div><span class="lane-kicker">全校动态</span><h2>全部工单</h2></div><strong>${board.all.length}</strong></div><p class="lane-intro">查看当前学校全部报修任务；非本人负责的工单仅供查看。</p><div class="lane-list lane-list-scroll">${board.all.map(order=>card(order,'查看详情',true)).join('')||empty('当前没有工单')}</div></section></div><div class="worker-side"><section class="worker-lane lane-pending"><div class="lane-head"><div><span class="lane-kicker">等待响应</span><h2>系统派单</h2></div><strong>${pending.length}</strong></div><div class="lane-list">${pending.map(order=>card(order,'立即接单')).join('')||empty('暂无待接单任务')}</div></section><section class="worker-lane lane-alert"><div class="lane-head"><div><span class="lane-kicker">需要关注</span><h2>异常工单</h2></div><strong>${alerts.length}</strong></div><div class="lane-list">${alerts.map(order=>card(order,'查看异常')).join('')||empty('暂无返工或中断工单')}</div></section></div></div>`);
  }
  function renderOrders(){
    const statuses=['维修中','待确认','返工中','已中断','已完成','已取消'];
    mount('worker-orders','我的工单','查看当前及历史负责工单',`<div class="filters" aria-label="工单状态筛选"><button class="filter active" data-filter="全部">全部</button>${statuses.map(status=>`<button class="filter" data-filter="${status}">${status}</button>`).join('')}</div><div class="worker-order-list">${mine().map(order=>card(order)).join('')||empty('暂无负责工单')}</div>`);
    const initial=new URLSearchParams(location.search).get('status')||'全部';
    function apply(status){document.querySelectorAll('.filter').forEach(button=>button.classList.toggle('active',button.dataset.filter===status));document.querySelectorAll('.worker-order-card').forEach(item=>item.hidden=status!=='全部'&&item.dataset.status!==status);}
    document.querySelectorAll('.filter').forEach(button=>button.onclick=()=>apply(button.dataset.filter));apply(statuses.includes(initial)?initial:'全部');
  }
  function leaveModal(){
    App().ui.modal({title:'发起请假申请',content:'<form class="form" id="worker-leave-form"><div class="field"><label class="required" for="leave-start">开始时间</label><input class="input" id="leave-start" type="datetime-local" required></div><div class="field"><label class="required" for="leave-end">结束时间</label><input class="input" id="leave-end" type="datetime-local" required></div><div class="field"><label class="required" for="leave-reason">请假原因</label><textarea class="textarea" id="leave-reason" required placeholder="请说明请假原因"></textarea></div><div class="error" id="leave-error" role="alert"></div></form>',actions:[{label:'取消',onClick:wrap=>wrap.remove()},{label:'提交申请',className:'primary',onClick:wrap=>{const result=App().actions.createLeave({start:document.getElementById('leave-start').value,end:document.getElementById('leave-end').value,reason:document.getElementById('leave-reason').value});if(!result.ok){document.getElementById('leave-error').textContent=result.message;return}wrap.remove();App().ui.toast(result.message);setTimeout(()=>location.reload(),350)}}]});
  }
  function renderLeave(){
    const requests=state().leaveRequests.filter(item=>item.workerId===user().id);
    mount('worker-leave','我的请假','审批通过后，到开始时间才会停止派单',`<div class="leave-list">${requests.map(item=>`<article class="card leave-card"><div><span class="tag ${item.status==='已通过'?'green':item.status==='已驳回'?'red':'amber'}">${esc(item.status)}</span><h2>${esc(item.start)} 至 ${esc(item.end)}</h2><p>${esc(item.reason)}</p></div><div class="leave-meta"><span>申请时间</span><strong>${esc(item.createdAt)}</strong>${item.activatedAt?'<span class="success">已生效</span>':''}</div></article>`).join('')||empty('暂无请假申请')}</div>`,`<button class="btn primary worker-leave-trigger">发起请假申请</button>`);
    document.querySelector('.worker-leave-trigger').onclick=leaveModal;
  }
  function renderMessages(){
    const messages=state().notifications.filter(item=>item.role==='worker'&&(!item.userId||item.userId===user().id));
    mount('worker-messages','消息中心','派单、审批和返工提醒集中在这里',`<div class="message-list">${messages.map(item=>`<details class="message-card ${item.read?'':'unread'}"><summary><span class="message-type">${esc(item.type||'系统通知')}</span><span class="message-copy"><strong>${esc(item.title)}</strong><small>${esc(item.time)}</small></span>${item.read?'':'<i class="message-unread" aria-label="未读"></i>'}</summary><div class="message-detail"><p>${esc(item.detail||item.title)}</p><button class="btn outline small" data-message="${esc(item.id)}">查看相关内容</button></div></details>`).join('')||empty('暂无消息')}</div>`);
    document.querySelectorAll('[data-message]').forEach(button=>button.onclick=()=>{const result=App().actions.readNotification(button.dataset.message);if(result.ok)location.href=result.data.href||'worker-home.html';else App().ui.toast(result.message,'error')});
  }
  function render(page){
    if(!['worker-home','worker-orders','worker-leave','worker-messages'].includes(page)){setTimeout(()=>{installInbox();if(page==='worker-order-detail'){const order=state().orders.find(item=>item.id===new URLSearchParams(location.search).get('id'));if(order&&order.assigneeId!==user().id){document.querySelectorAll('#accept,#resume,#interrupt,#transfer').forEach(element=>element.remove());const aside=document.querySelector('.split > aside');if(aside)aside.innerHTML='<h2>只读查看</h2><p class="muted">该工单当前由其他维修人员负责，你可以查看完整轨迹，但不能执行维修操作。</p>';}}});return false;}
    if(!App().requireRole('worker')){location.href='login.html';return true;}
    ({'worker-home':renderHome,'worker-orders':renderOrders,'worker-leave':renderLeave,'worker-messages':renderMessages}[page])();return true;
  }
  root.DormRepairWorkerUI={render};
})(typeof globalThis!=='undefined'?globalThis:this);
