import type { RouteRecordRaw } from 'vue-router'

import {
  FORBIDDEN_PATH,
  HOME_PATH,
  LOGIN_PATH,
  NOT_FOUND_PATH,
  ROOT_ROUTE_NAME
} from '@/utils/constants'

import { resolveLayoutComponent, resolveViewComponent } from './componentLoader'

/**
 * 静态路由（任务书 18.1）：登录页、403、404、布局路由 + 工作台。
 *
 * 说明：路由组件一律通过 componentLoader 的 glob 映射表定位，**不手写零散 import 语句**。
 * 工作台**不进菜单表**，它是布局路由的静态子路由，侧边栏固定展示第一项。
 */
export const staticRoutes: RouteRecordRaw[] = [
  {
    path: LOGIN_PATH,
    name: 'Login',
    component: resolveViewComponent('login/index', LOGIN_PATH, 'Login'),
    meta: { title: 'login.title', keepAlive: false, visible: false }
  },
  {
    path: FORBIDDEN_PATH,
    name: 'Forbidden',
    component: resolveViewComponent('error/403', FORBIDDEN_PATH, 'Forbidden'),
    meta: { title: 'common.forbiddenTitle', keepAlive: false, visible: false }
  },
  {
    path: NOT_FOUND_PATH,
    name: 'NotFound',
    component: resolveViewComponent('error/404', NOT_FOUND_PATH, 'NotFound'),
    meta: { title: 'common.notFoundTitle', keepAlive: false, visible: false }
  },
  {
    path: '/',
    name: ROOT_ROUTE_NAME,
    component: resolveLayoutComponent(),
    redirect: HOME_PATH,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: resolveViewComponent('dashboard/index', HOME_PATH, 'Dashboard'),
        meta: {
          title: 'dashboard.title',
          icon: 'HomeFilled',
          keepAlive: true,
          visible: true
        }
      }
    ]
  }
]

export default staticRoutes
