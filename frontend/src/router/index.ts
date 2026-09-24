import { createRouter, createWebHistory, type RouteRecordRaw, type RouterHistory } from 'vue-router'
import { ROLE, roleHomeOf } from '@/constants/role'
import { useAppStore } from '@/stores/app'

declare module 'vue-router' {
  interface RouteMeta {
    /** 匿名可访问，未登录不跳转登录页。 */
    public?: boolean
    /** 允许访问的角色，与后端 UserRoleEnum 数值一致。 */
    role?: number
  }
}

/**
 * 路由基线对应 `doc/architecture/前端页面与路由映射.md`。
 * 各端业务页面按阶段 F6/F7/F8 逐步接入，当前只有登录落点首页。
 */
export const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/system/ForbiddenView.vue'),
    meta: { public: true },
  },
  {
    path: '/student',
    redirect: '/student/home',
    children: [
      {
        path: 'home',
        name: 'student-home',
        component: () => import('@/views/student/StudentHomeView.vue'),
        meta: { role: ROLE.STUDENT },
      },
      {
        path: 'orders',
        name: 'student-orders',
        component: () => import('@/views/student/StudentOrdersView.vue'),
        meta: { role: ROLE.STUDENT },
      },
      {
        path: 'orders/:id(\\d+)',
        name: 'student-order-detail',
        component: () => import('@/views/student/StudentOrderDetailView.vue'),
        meta: { role: ROLE.STUDENT },
      },
      {
        path: 'orders/:id(\\d+)/review',
        name: 'student-order-review',
        component: () => import('@/views/student/StudentOrderReviewView.vue'),
        meta: { role: ROLE.STUDENT },
      },
    ],
  },
  {
    path: '/worker',
    redirect: '/worker/home',
    children: [
      {
        path: 'home',
        name: 'worker-home',
        component: () => import('@/views/worker/WorkerHomeView.vue'),
        meta: { role: ROLE.WORKER },
      },
      {
        path: 'orders',
        name: 'worker-orders',
        component: () => import('@/views/worker/WorkerOrdersView.vue'),
        meta: { role: ROLE.WORKER },
      },
      {
        path: 'orders/:id(\\d+)',
        name: 'worker-order-detail',
        component: () => import('@/views/worker/WorkerOrderDetailView.vue'),
        meta: { role: ROLE.WORKER },
      },
      {
        path: 'orders/:id(\\d+)/process',
        name: 'worker-order-process',
        component: () => import('@/views/worker/WorkerProcessView.vue'),
        meta: { role: ROLE.WORKER },
      },
      {
        path: 'orders/:id(\\d+)/materials',
        name: 'worker-order-materials',
        component: () => import('@/views/worker/WorkerMaterialsView.vue'),
        meta: { role: ROLE.WORKER },
      },
      {
        path: 'orders/:id(\\d+)/complete',
        name: 'worker-order-complete',
        component: () => import('@/views/worker/WorkerCompleteView.vue'),
        meta: { role: ROLE.WORKER },
      },
      {
        path: 'leave',
        name: 'worker-leave',
        component: () => import('@/views/worker/WorkerLeaveView.vue'),
        meta: { role: ROLE.WORKER },
      },
      {
        path: 'messages',
        name: 'worker-messages',
        component: () => import('@/views/worker/WorkerMessagesView.vue'),
        meta: { role: ROLE.WORKER },
      },
    ],
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'orders',
        name: 'admin-orders',
        component: () => import('@/views/admin/AdminOrdersView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'orders/exceptions',
        name: 'admin-exceptions',
        component: () => import('@/views/admin/AdminExceptionsView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'orders/manual-dispatch',
        name: 'admin-manual-dispatch',
        component: () => import('@/views/admin/AdminManualDispatchView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'orders/:id(\\d+)',
        name: 'admin-order-detail',
        component: () => import('@/views/admin/AdminOrderDetailView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'approvals/transfers',
        name: 'admin-transfer-approvals',
        component: () => import('@/views/admin/AdminTransferApprovalsView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'approvals/leaves',
        name: 'admin-leave-approvals',
        component: () => import('@/views/admin/AdminLeaveApprovalsView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'config/workers',
        name: 'admin-config-workers',
        component: () => import('@/views/admin/config/WorkerManagementView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'config/areas',
        name: 'admin-config-areas',
        component: () => import('@/views/admin/config/AreaManagementView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'config/schedules',
        name: 'admin-config-schedules',
        component: () => import('@/views/admin/config/WorkScheduleView.vue'),
        meta: { role: ROLE.ADMIN },
      },
      {
        path: 'config/fault-types',
        name: 'admin-config-fault-types',
        component: () => import('@/views/admin/config/FaultTypeView.vue'),
        meta: { role: ROLE.ADMIN },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/system/NotFoundView.vue'),
    meta: { public: true },
  },
]

export function createAppRouter(history: RouterHistory) {
  const router = createRouter({ history, routes })

  router.beforeEach((to) => {
    const app = useAppStore()
    const user = app.currentUser

    if (!user) {
      if (to.meta.public) return true
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    // 已登录访问登录页，回到角色首页。
    if (to.name === 'login') {
      const home = roleHomeOf(user.roleType)
      return home ? { path: home } : { name: 'forbidden' }
    }

    // 角色域校验：学生、维修人员、管理员只能进入各自端。
    const role = to.meta.role
    if (role !== undefined && role !== user.roleType) return { name: 'forbidden' }

    return true
  })

  return router
}

const router = createAppRouter(createWebHistory(import.meta.env.BASE_URL))

export default router
