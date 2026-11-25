import { getApiBaseUrl } from './config.js';

/**
 * REST API client connecting the Vercel frontend to the Java BackendApiServer.
 * Includes an explicit Offline UI Preview mode if the user chooses to inspect
 * the interface without running the local Java server.
 */

let previewMode = false;

const previewStore = {
  user: { userId: 1, username: 'admin', fullName: 'Dispatch Admin', role: 'Lead Coordinator' },
  drivers: [
    { driverId: 101, userId: 1, name: 'Marcus Vance', phone: '+1 555-0142', area: 'Downtown', available: true },
    { driverId: 102, userId: 1, name: 'Elena Rostova', phone: '+1 555-0188', area: 'North Side', available: true },
    { driverId: 103, userId: 1, name: 'Liam Chen', phone: '+1 555-0193', area: 'West End', available: false }
  ],
  orders: [
    { orderId: 201, userId: 1, customer: 'Sophia Patel', address: '420 Oak Avenue', area: 'Downtown', deliveryDate: '2026-11-25', timeSlot: '9 AM - 12 PM', status: 'Delivered', failureReason: null },
    { orderId: 202, userId: 1, customer: 'Daniel Brooks', address: '88 Pine Street', area: 'North Side', deliveryDate: '2026-11-25', timeSlot: '12 PM - 3 PM', status: 'In Progress', failureReason: null },
    { orderId: 203, userId: 1, customer: 'Maya Lin', address: '17 Cedar Lane', area: 'Downtown', deliveryDate: '2026-11-26', timeSlot: '3 PM - 6 PM', status: 'Pending', failureReason: null },
    { orderId: 204, userId: 1, customer: 'Noah Sterling', address: '55 Maple Blvd', area: 'West End', deliveryDate: '2026-11-24', timeSlot: '9 AM - 12 PM', status: 'Failed', failureReason: 'Recipient unavailable at gate' }
  ],
  assignments: [
    { assignmentId: 301, orderId: 201, userId: 1, customer: 'Sophia Patel', driver: 'Marcus Vance', area: 'Downtown', status: 'Delivered', assignedAt: '2026-11-25 08:45:00', failureReason: null },
    { assignmentId: 302, orderId: 202, userId: 1, customer: 'Daniel Brooks', driver: 'Elena Rostova', area: 'North Side', status: 'In Progress', assignedAt: '2026-11-25 10:15:00', failureReason: null },
    { assignmentId: 303, orderId: 204, userId: 1, customer: 'Noah Sterling', driver: 'Liam Chen', area: 'West End', status: 'Failed', assignedAt: '2026-11-24 08:30:00', failureReason: 'Recipient unavailable at gate' }
  ]
};

export function setPreviewMode(enabled) {
  previewMode = Boolean(enabled);
}

export function isPreviewMode() {
  return previewMode;
}

async function request(path, options = {}) {
  const baseUrl = getApiBaseUrl();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };
  if (options.userId) {
    headers['X-User-Id'] = String(options.userId);
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || `Request failed (${response.status})`);
  }
  return data;
}

export async function checkHealth() {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 3000);
  try {
    const res = await fetch(`${getApiBaseUrl()}/api/health`, { signal: controller.signal });
    const data = await res.json();
    return { connected: res.ok && data.status === 'ok', data };
  } finally {
    clearTimeout(timer);
  }
}

export async function login(username, password) {
  if (previewMode) return { ...previewStore.user };
  return request('/api/auth/login', {
    method: 'POST',
    body: { username, password }
  });
}

export async function register(username, password, fullName) {
  if (previewMode) {
    return { userId: 1, username, fullName, role: 'Coordinator' };
  }
  return request('/api/auth/register', {
    method: 'POST',
    body: { username, password, fullName, role: 'Coordinator' }
  });
}

export async function getDashboardStats(userId) {
  if (previewMode) {
    const orders = previewStore.orders;
    const drivers = previewStore.drivers;
    return {
      userId,
      totalOrders: orders.length,
      pendingOrders: orders.filter(o => o.status === 'Pending').length,
      assignedOrders: orders.filter(o => o.status === 'Assigned').length,
      inProgressOrders: orders.filter(o => o.status === 'In Progress').length,
      deliveredOrders: orders.filter(o => o.status === 'Delivered').length,
      failedOrders: orders.filter(o => o.status === 'Failed').length,
      totalDrivers: drivers.length,
      availableDrivers: drivers.filter(d => d.available).length
    };
  }
  return request('/api/dashboard', { userId });
}

export async function getOrders(userId) {
  if (previewMode) return [...previewStore.orders];
  return request('/api/orders', { userId });
}

export async function createOrder(userId, payload) {
  if (previewMode) {
    const nextId = Math.max(200, ...previewStore.orders.map(o => o.orderId)) + 1;
    previewStore.orders.unshift({ orderId: nextId, userId, ...payload, status: 'Pending', failureReason: null });
    return { orderId: nextId, userId };
  }
  return request('/api/orders', { method: 'POST', userId, body: payload });
}

export async function updatePendingOrder(userId, payload) {
  if (previewMode) {
    const item = previewStore.orders.find(o => o.orderId === payload.orderId && o.status === 'Pending');
    if (!item) throw new Error('Only pending orders can be updated.');
    Object.assign(item, payload);
    return { updated: true };
  }
  return request('/api/orders', { method: 'PUT', userId, body: payload });
}

export async function smartAssignOrder(userId, orderId, area) {
  if (previewMode) {
    const order = previewStore.orders.find(o => o.orderId === orderId);
    if (!order || order.status !== 'Pending') throw new Error('Only pending orders can be assigned.');
    const driver = previewStore.drivers.find(d => d.available && d.area.toLowerCase() === (area || order.area).toLowerCase());
    if (!driver) throw new Error(`No available driver found in ${area || order.area}.`);
    order.status = 'Assigned';
    previewStore.assignments.unshift({
      assignmentId: Date.now(),
      orderId,
      userId,
      customer: order.customer,
      driver: driver.name,
      area: order.area,
      status: 'Assigned',
      assignedAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
      failureReason: null
    });
    return { success: true, driverId: driver.driverId, driverName: driver.name, message: 'Driver assigned successfully!' };
  }
  return request('/api/orders/assign', { method: 'POST', userId, body: { orderId, area } });
}

export async function getDrivers(userId) {
  if (previewMode) return [...previewStore.drivers];
  return request('/api/drivers', { userId });
}

export async function addDriver(userId, payload) {
  if (previewMode) {
    const nextId = Math.max(100, ...previewStore.drivers.map(d => d.driverId)) + 1;
    previewStore.drivers.unshift({ driverId: nextId, userId, ...payload, available: true });
    return { driverId: nextId, userId };
  }
  return request('/api/drivers', { method: 'POST', userId, body: payload });
}

export async function updateDriverAvailability(userId, driverId, available) {
  if (previewMode) {
    const drv = previewStore.drivers.find(d => d.driverId === driverId);
    if (drv) drv.available = available;
    return { updated: true, driverId, available };
  }
  return request('/api/drivers/availability', {
    method: 'PUT',
    userId,
    body: { driverId, available }
  });
}

export async function getAssignments(userId) {
  if (previewMode) return [...previewStore.assignments];
  return request('/api/assignments', { userId });
}

export async function updateDeliveryStatus(userId, orderId, status, failureReason) {
  if (previewMode) {
    const order = previewStore.orders.find(o => o.orderId === orderId);
    const asg = previewStore.assignments.find(a => a.orderId === orderId);
    if (order) {
      order.status = status;
      order.failureReason = failureReason || null;
    }
    if (asg) {
      asg.status = status;
      asg.failureReason = failureReason || null;
    }
    return { updated: true, orderId, status };
  }
  return request('/api/assignments/status', {
    method: 'PUT',
    userId,
    body: { orderId, status, failureReason: failureReason || null }
  });
}

export async function getReportSummary(userId) {
  if (previewMode) {
    const orders = previewStore.orders;
    const totalOrders = orders.length;
    const deliveredOrders = orders.filter(o => o.status === 'Delivered').length;
    const failedOrders = orders.filter(o => o.status === 'Failed').length;
    const areaMap = new Map();
    for (const o of orders) {
      if (!areaMap.has(o.area)) {
        areaMap.set(o.area, { area: o.area, total: 0, delivered: 0, failed: 0 });
      }
      const stat = areaMap.get(o.area);
      stat.total++;
      if (o.status === 'Delivered') stat.delivered++;
      if (o.status === 'Failed') stat.failed++;
    }
    return {
      userId,
      totalOrders,
      deliveredOrders,
      failedOrders,
      successRate: totalOrders ? Number(((deliveredOrders * 100) / totalOrders).toFixed(1)) : 0,
      areas: Array.from(areaMap.values())
    };
  }
  return request('/api/reports', { userId });
}
