import * as api from './api.js';
import { getApiBaseUrl, setApiBaseUrl } from './config.js';
import { renderDashboardPipeline, renderReportsCharts } from './charts.js';

/**
 * Main UI controller for DispatchDesk Web Frontend.
 * Connects cleanly to the Java Backend REST API with zero client-side mock overrides
 * when connected, and provides an executive SaaS UI for professor demonstrations.
 */

const state = {
  session: null,
  currentPage: 'dashboard',
  orders: [],
  drivers: [],
  assignments: [],
  lastReport: null,
  orderStatusFilter: 'All',
  backendConnected: false
};

const PAGE_TITLES = {
  dashboard: 'Dashboard',
  orders: 'Orders Management',
  drivers: 'Drivers Directory',
  assignments: 'Delivery Assignments',
  reports: 'Delivery Reports & Analytics'
};

const ORDER_STATUSES = ['All', 'Pending', 'Assigned', 'In Progress', 'Delivered', 'Failed'];

// =========================================================
// UTILITIES
// =========================================================
function esc(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function getInitials(name) {
  if (!name || !name.trim()) return 'DD';
  const parts = name.trim().split(/\s+/);
  return parts.length >= 2
    ? (parts[0][0] + parts[1][0]).toUpperCase()
    : name.trim()[0].toUpperCase();
}

function statusBadgeHtml(status) {
  const s = (status || '').trim();
  const key = s.toLowerCase();
  let cls = 'badge-progress';

  if (key === 'delivered' || key === 'available') cls = 'badge-delivered';
  else if (key === 'in progress') cls = 'badge-progress';
  else if (key === 'assigned') cls = 'badge-assigned';
  else if (key === 'pending') cls = 'badge-pending';
  else if (key === 'failed') cls = 'badge-failed';
  else if (key === 'busy') cls = 'badge-busy';

  return `<span class="badge ${cls}">● ${esc(s)}</span>`;
}

function lifecycleStepperHtml(status) {
  const normalized = (status || 'Pending').toLowerCase();
  const isFailed = normalized === 'failed';
  const stageIndex = {
    'pending': 0,
    'assigned': 1,
    'in progress': 2,
    'delivered': 3,
    'failed': 3
  }[normalized] ?? 0;

  const steps = ['Pending', 'Assigned', 'In Progress', isFailed ? 'Failed' : 'Delivered'];
  return `
    <div class="lifecycle-stepper" aria-label="Order Lifecycle">
      ${steps.map((label, idx) => {
        let cls = 'step-node';
        let symbol = idx + 1;
        if (isFailed && idx === 3) {
          cls += ' failed';
          symbol = '✕';
        } else if (idx < stageIndex || (normalized === 'delivered' && idx === 3)) {
          cls += ' done';
          symbol = '✓';
        } else if (idx === stageIndex) {
          cls += ' current';
        }
        return `
          <div class="${cls}">
            <span class="step-dot">${symbol}</span>
            <span>${label}</span>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

function updateTopbarDate() {
  const el = document.getElementById('topbar-date');
  if (!el) return;
  el.textContent = new Date().toLocaleDateString(undefined, {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  });
}

// =========================================================
// MODAL SYSTEM
// =========================================================
function openModal(title, bodyHtml, onReady) {
  document.getElementById('modal-title').textContent = title;
  const body = document.getElementById('modal-body');
  body.innerHTML = bodyHtml;
  document.getElementById('modal-backdrop').classList.remove('hidden');
  if (typeof onReady === 'function') onReady(body);
}

function closeModal() {
  document.getElementById('modal-backdrop').classList.add('hidden');
  document.getElementById('modal-body').innerHTML = '';
}

// =========================================================
// BACKEND HEALTH & API URL CONFIG
// =========================================================
async function refreshBackendStatus() {
  const authPill = document.getElementById('auth-backend-status');
  const headerPill = document.getElementById('header-backend-pill');
  const headerText = document.getElementById('header-backend-text');
  const offlineBox = document.getElementById('offline-preview-box');
  const offlineUrl = document.getElementById('offline-api-url');

  if (api.isPreviewMode()) {
    state.backendConnected = false;
    headerPill.className = 'status-pill status-preview';
    headerText.textContent = 'Offline UI Preview';
    return;
  }

  try {
    const res = await api.checkHealth();
    state.backendConnected = res.connected;
    if (res.connected) {
      authPill.className = 'status-pill status-connected';
      authPill.querySelector('.status-text').textContent = 'Java Backend Connected';
      headerPill.className = 'status-pill status-connected';
      headerText.textContent = 'Java Backend Connected';
      offlineBox.classList.add('hidden');
    } else {
      throw new Error('Degraded');
    }
  } catch {
    state.backendConnected = false;
    authPill.className = 'status-pill status-offline';
    authPill.querySelector('.status-text').textContent = 'Java Backend Offline';
    headerPill.className = 'status-pill status-offline';
    headerText.textContent = 'Backend Offline';
    offlineUrl.textContent = getApiBaseUrl();
    offlineBox.classList.remove('hidden');
  }
}

function openApiConfigModal() {
  const current = getApiBaseUrl();
  openModal('Java Backend Connection', `
    <p class="card-subtitle" style="margin:0">
      Enter the base URL of your running DispatchDesk Java Backend (<code>BackendApiServer</code>).
    </p>
    <form id="api-config-form" class="auth-form">
      <div class="form-group">
        <label for="api-url-input">Backend Base URL</label>
        <input id="api-url-input" type="url" value="${esc(current)}" placeholder="http://localhost:8080" required />
      </div>
      <div class="modal-footer">
        <button type="button" id="reset-api-url-btn" class="btn btn-outline">Reset to localhost:8080</button>
        <button type="submit" class="btn btn-primary">Save &amp; Test Connection</button>
      </div>
    </form>
  `, (body) => {
    body.querySelector('#reset-api-url-btn').addEventListener('click', () => {
      body.querySelector('#api-url-input').value = 'http://localhost:8080';
    });
    body.querySelector('#api-config-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const val = body.querySelector('#api-url-input').value.trim();
      setApiBaseUrl(val);
      api.setPreviewMode(false);
      closeModal();
      await refreshBackendStatus();
      showToast(state.backendConnected ? 'Connected to Java Backend!' : 'Backend not reachable at that URL.', state.backendConnected ? 'success' : 'error');
    });
  });
}

// =========================================================
// AUTHENTICATION FLOW
// =========================================================
function setAuthMode(mode) {
  const isSignIn = mode === 'signin';
  document.getElementById('tab-signin').classList.toggle('active', isSignIn);
  document.getElementById('tab-register').classList.toggle('active', !isSignIn);
  document.getElementById('signin-form').classList.toggle('hidden', !isSignIn);
  document.getElementById('register-form').classList.toggle('hidden', isSignIn);
  document.getElementById('auth-subtitle').textContent = isSignIn
    ? 'Sign in to access your dispatch operations'
    : 'Register a new coordinator ID to access the console';
  hideAuthAlert();
}

function showAuthAlert(msg, isSuccess = false) {
  const el = document.getElementById('auth-alert');
  el.textContent = msg;
  el.classList.remove('hidden', 'success');
  if (isSuccess) el.classList.add('success');
}

function hideAuthAlert() {
  document.getElementById('auth-alert').classList.add('hidden');
}

async function enterApp(session) {
  state.session = session;
  state.orderStatusFilter = 'All';
  sessionStorage.setItem('dispatchdesk_session', JSON.stringify(session));

  document.getElementById('user-avatar').textContent = getInitials(session.fullName);
  document.getElementById('user-fullname').textContent = session.fullName;
  document.getElementById('user-role').textContent = `@${session.username}`;
  document.getElementById('welcome-greeting').textContent = `Welcome back, ${session.fullName}`;
  document.getElementById('dashboard-subtitle').textContent =
    `Private workspace for @${session.username} · Real-time order pipeline, driver workload & analytics`;

  document.getElementById('order-search-input').value = '';
  updateTopbarDate();

  document.getElementById('auth-view').classList.add('hidden');
  document.getElementById('app-view').classList.remove('hidden');

  navigateTo('dashboard');
  await refreshBackendStatus();
  await refreshAllData();
}

function logout() {
  openModal('Confirm Logout', `
    <p style="margin:0;color:var(--text-secondary)">Are you sure you want to log out of DispatchDesk?</p>
    <div class="modal-footer">
      <button type="button" id="cancel-logout-btn" class="btn btn-outline">Cancel</button>
      <button type="button" id="confirm-logout-btn" class="btn btn-danger">Logout</button>
    </div>
  `, (body) => {
    body.querySelector('#cancel-logout-btn').addEventListener('click', closeModal);
    body.querySelector('#confirm-logout-btn').addEventListener('click', () => {
      closeModal();
      state.session = null;
      state.orders = [];
      state.drivers = [];
      state.assignments = [];
      api.setPreviewMode(false);
      sessionStorage.removeItem('dispatchdesk_session');
      document.getElementById('app-view').classList.add('hidden');
      document.getElementById('auth-view').classList.remove('hidden');
      document.getElementById('signin-form').reset();
      document.getElementById('register-form').reset();
      setAuthMode('signin');
      refreshBackendStatus();
    });
  });
}

// =========================================================
// NAVIGATION & DATA REFRESH
// =========================================================
function navigateTo(page) {
  state.currentPage = page;
  document.getElementById('page-title').textContent = PAGE_TITLES[page] || 'Dashboard';

  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.page === page);
  });

  document.querySelectorAll('.page-section').forEach(sec => {
    sec.classList.toggle('hidden', sec.id !== `page-${page}`);
  });

  document.getElementById('sidebar').classList.remove('open');
  document.getElementById('sidebar-backdrop').classList.add('hidden');

  if (page === 'reports' && state.session) {
    loadReports();
  }
}

function updateSidebarBadges(stats) {
  const pendingBadge = document.getElementById('nav-badge-pending');
  const driversBadge = document.getElementById('nav-badge-drivers');
  const activeBadge = document.getElementById('nav-badge-active');

  const pending = stats.pendingOrders || 0;
  pendingBadge.textContent = pending;
  pendingBadge.classList.toggle('hidden', pending === 0);

  driversBadge.textContent = stats.totalDrivers || 0;
  activeBadge.textContent = (stats.assignedOrders || 0) + (stats.inProgressOrders || 0);
}

async function refreshAllData() {
  if (!state.session) return;
  try {
    await Promise.all([
      loadDashboard(),
      loadOrders(),
      loadDrivers(),
      loadAssignments(),
      loadReports()
    ]);
    renderDashboardActivity();
  } catch (err) {
    showToast(err.message || 'Failed to sync with backend.', 'error');
  }
}

// =========================================================
// DASHBOARD
// =========================================================
async function loadDashboard() {
  const stats = await api.getDashboardStats(state.session.userId);
  const total = stats.totalOrders || 0;
  const pending = stats.pendingOrders || 0;
  const transit = (stats.inProgressOrders || 0) + (stats.assignedOrders || 0);
  const delivered = stats.deliveredOrders || 0;

  document.getElementById('kpi-total-orders').textContent = total;
  document.getElementById('kpi-pending-orders').textContent = pending;
  document.getElementById('kpi-intransit-orders').textContent = transit;
  document.getElementById('kpi-delivered-orders').textContent = delivered;

  const pct = (val) => total > 0 ? `${Math.round((val * 100) / total)}%` : '0%';
  document.getElementById('kpi-total-share').textContent = total > 0 ? '100%' : '0%';
  document.getElementById('kpi-pending-share').textContent = pct(pending);
  document.getElementById('kpi-transit-share').textContent = pct(transit);
  document.getElementById('kpi-delivered-share').textContent = pct(delivered);

  renderDashboardPipeline(stats);
  updateSidebarBadges(stats);
}

function renderDashboardActivity() {
  const container = document.getElementById('dashboard-activity-list');
  if (!state.orders.length) {
    container.innerHTML = `<div class="empty-state">No orders yet. Click <strong>+ New Order</strong> to create your first delivery.</div>`;
    return;
  }

  // Prioritize Pending orders at top, then recent orders
  const sorted = [...state.orders].sort((a, b) => {
    const aPend = a.status === 'Pending' ? 0 : 1;
    const bPend = b.status === 'Pending' ? 0 : 1;
    if (aPend !== bPend) return aPend - bPend;
    return b.orderId - a.orderId;
  }).slice(0, 5);

  container.innerHTML = sorted.map(o => `
    <div class="activity-item" data-order-id="${o.orderId}">
      <div class="activity-main">
        <span class="avatar-circle avatar-sm">${esc(getInitials(o.customer))}</span>
        <div class="activity-info">
          <div class="activity-title">#${o.orderId} · ${esc(o.customer)}</div>
          <div class="activity-meta">${esc(o.area)} · ${esc(o.deliveryDate)}</div>
        </div>
      </div>
      <div class="activity-right">
        ${o.status === 'Pending'
          ? `<button type="button" class="btn btn-primary btn-sm quick-assign-dash" data-order-id="${o.orderId}">⚡ Assign</button>`
          : statusBadgeHtml(o.status)
        }
      </div>
    </div>
  `).join('');

  container.querySelectorAll('.activity-item').forEach(item => {
    item.addEventListener('click', (e) => {
      if (e.target.closest('.quick-assign-dash')) return;
      const id = Number(item.dataset.orderId);
      const order = state.orders.find(x => x.orderId === id);
      if (order) openOrderDetailsModal(order);
    });
  });

  container.querySelectorAll('.quick-assign-dash').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const id = Number(btn.dataset.orderId);
      const order = state.orders.find(x => x.orderId === id);
      if (!order) return;
      await runSmartAssign(order);
    });
  });
}

// =========================================================
// ORDERS
// =========================================================
async function loadOrders() {
  state.orders = await api.getOrders(state.session.userId);
  renderOrderStatusPills();
  renderOrdersTable();
}

function renderOrderStatusPills() {
  const container = document.getElementById('order-status-pills');
  const counts = { All: state.orders.length };
  for (const s of ORDER_STATUSES.slice(1)) {
    counts[s] = state.orders.filter(o => o.status === s).length;
  }

  container.innerHTML = ORDER_STATUSES.map(status => `
    <button
      type="button"
      class="filter-pill-btn ${state.orderStatusFilter === status ? 'active' : ''}"
      data-status="${status}">
      <span>${status}</span>
      <span class="filter-pill-count">${counts[status] || 0}</span>
    </button>
  `).join('');

  container.querySelectorAll('.filter-pill-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      state.orderStatusFilter = btn.dataset.status;
      renderOrderStatusPills();
      renderOrdersTable();
    });
  });
}

function renderOrdersTable() {
  const tbody = document.getElementById('orders-tbody');
  const query = document.getElementById('order-search-input').value.trim().toLowerCase();
  const statusFilter = state.orderStatusFilter;

  const filtered = state.orders.filter(o => {
    const matchesStatus = statusFilter === 'All' || o.status === statusFilter;
    const hay = `${o.orderId} ${o.customer} ${o.area} ${o.address} ${o.deliveryDate} ${o.timeSlot}`.toLowerCase();
    const matchesQuery = !query || hay.includes(query);
    return matchesStatus && matchesQuery;
  });

  if (!filtered.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No orders match the current filter. Click <strong>+ Add Order</strong> to create one.</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map(o => `
    <tr class="clickable-row" data-order-id="${o.orderId}">
      <td><strong>#${o.orderId}</strong></td>
      <td>
        <span class="cell-avatar">
          <span class="avatar-circle avatar-sm">${esc(getInitials(o.customer))}</span>
          <span>${esc(o.customer)}</span>
        </span>
      </td>
      <td><span class="area-tag">${esc(o.area)}</span></td>
      <td>${esc(o.deliveryDate)} · ${esc(o.timeSlot)}</td>
      <td>${statusBadgeHtml(o.status)}</td>
      <td class="text-right">
        <div class="row-actions-group">
          ${o.status === 'Pending'
            ? `<button type="button" class="btn btn-primary btn-sm row-smart-assign" data-order-id="${o.orderId}">⚡ Smart Assign</button>`
            : ''
          }
          <button type="button" class="btn btn-outline btn-sm view-order-btn" data-order-id="${o.orderId}">Details →</button>
        </div>
      </td>
    </tr>
  `).join('');

  tbody.querySelectorAll('tr.clickable-row').forEach(row => {
    row.addEventListener('click', (e) => {
      if (e.target.closest('.row-smart-assign')) return;
      const id = Number(row.dataset.orderId);
      const order = state.orders.find(x => x.orderId === id);
      if (order) openOrderDetailsModal(order);
    });
  });

  tbody.querySelectorAll('.row-smart-assign').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const id = Number(btn.dataset.orderId);
      const order = state.orders.find(x => x.orderId === id);
      if (order) await runSmartAssign(order);
    });
  });
}

async function runSmartAssign(order) {
  try {
    const res = await api.smartAssignOrder(state.session.userId, order.orderId, order.area);
    showToast(`Order #${order.orderId} assigned to ${res.driverName} (${order.area})!`, 'success');
    await refreshAllData();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function openAddOrderModal() {
  const today = new Date().toISOString().slice(0, 10);
  openModal('Create New Delivery Order', `
    <form id="add-order-form" class="auth-form">
      <div class="form-group">
        <label for="ord-customer">Customer Name *</label>
        <input id="ord-customer" type="text" placeholder="e.g. Jane Doe" required />
      </div>
      <div class="form-group">
        <label for="ord-address">Delivery Address *</label>
        <input id="ord-address" type="text" placeholder="e.g. 123 Elm Street" required />
      </div>
      <div class="form-group">
        <label for="ord-area">Delivery Area *</label>
        <input id="ord-area" type="text" placeholder="e.g. Downtown" required />
      </div>
      <div class="form-group">
        <label for="ord-date">Delivery Date *</label>
        <input id="ord-date" type="date" value="${today}" required />
      </div>
      <div class="form-group">
        <label for="ord-slot">Time Slot</label>
        <select id="ord-slot">
          <option value="9 AM - 12 PM">9 AM - 12 PM</option>
          <option value="12 PM - 3 PM">12 PM - 3 PM</option>
          <option value="3 PM - 6 PM">3 PM - 6 PM</option>
        </select>
      </div>
      <div class="modal-footer">
        <button type="button" id="cancel-add-order" class="btn btn-outline">Cancel</button>
        <button type="submit" class="btn btn-primary">Create Order</button>
      </div>
    </form>
  `, (body) => {
    body.querySelector('#cancel-add-order').addEventListener('click', closeModal);
    body.querySelector('#add-order-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const customer = body.querySelector('#ord-customer').value.trim();
      const address = body.querySelector('#ord-address').value.trim();
      const area = body.querySelector('#ord-area').value.trim();
      const deliveryDate = body.querySelector('#ord-date').value.trim();
      const timeSlot = body.querySelector('#ord-slot').value;

      if (!customer || !address || !area || !deliveryDate) {
        showToast('Please fill all required fields.', 'error');
        return;
      }

      try {
        await api.createOrder(state.session.userId, { customer, address, area, deliveryDate, timeSlot });
        closeModal();
        showToast('Order created successfully!', 'success');
        await refreshAllData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

function openOrderDetailsModal(order) {
  const isPending = order.status.toLowerCase() === 'pending';
  openModal(`Order Details — #${order.orderId}`, `
    <div class="detail-header-card">
      <div class="detail-person">
        <span class="avatar-circle">${esc(getInitials(order.customer))}</span>
        <div>
          <div style="font-weight:800;font-size:16px">Order #${order.orderId}</div>
          <div style="font-size:13px;color:var(--text-secondary)">${esc(order.customer)}</div>
        </div>
      </div>
      ${statusBadgeHtml(order.status)}
    </div>

    ${lifecycleStepperHtml(order.status)}

    <div class="detail-field">
      <div class="detail-field-label">Delivery Area</div>
      <div class="detail-field-value">${esc(order.area)}</div>
    </div>
    <div class="detail-field">
      <div class="detail-field-label">Delivery Address</div>
      <div class="detail-field-value">${esc(order.address)}</div>
    </div>
    <div class="detail-field">
      <div class="detail-field-label">Scheduled Delivery</div>
      <div class="detail-field-value">${esc(order.deliveryDate)} (${esc(order.timeSlot)})</div>
    </div>
    ${order.failureReason ? `
      <div class="detail-field danger">
        <div class="detail-field-label">Failure Reason</div>
        <div class="detail-field-value">${esc(order.failureReason)}</div>
      </div>
    ` : ''}

    <div class="modal-footer">
      ${isPending ? `
        <button type="button" id="modal-smart-assign-btn" class="btn btn-primary">⚡ Smart Assign</button>
        <button type="button" id="modal-edit-order-btn" class="btn btn-secondary">✎ Edit Order</button>
      ` : ''}
      <button type="button" id="modal-close-order-btn" class="btn btn-outline">Close</button>
    </div>
  `, (body) => {
    body.querySelector('#modal-close-order-btn').addEventListener('click', closeModal);
    if (isPending) {
      body.querySelector('#modal-smart-assign-btn').addEventListener('click', async () => {
        closeModal();
        await runSmartAssign(order);
      });
      body.querySelector('#modal-edit-order-btn').addEventListener('click', () => {
        openEditOrderModal(order);
      });
    }
  });
}

function openEditOrderModal(order) {
  openModal(`Edit Order #${order.orderId}`, `
    <form id="edit-order-form" class="auth-form">
      <div class="form-group">
        <label for="edit-customer">Customer Name</label>
        <input id="edit-customer" type="text" value="${esc(order.customer)}" required />
      </div>
      <div class="form-group">
        <label for="edit-address">Address</label>
        <input id="edit-address" type="text" value="${esc(order.address)}" required />
      </div>
      <div class="form-group">
        <label for="edit-area">Delivery Area</label>
        <input id="edit-area" type="text" value="${esc(order.area)}" required />
      </div>
      <div class="form-group">
        <label for="edit-date">Delivery Date</label>
        <input id="edit-date" type="date" value="${esc(order.deliveryDate)}" required />
      </div>
      <div class="form-group">
        <label for="edit-slot">Time Slot</label>
        <select id="edit-slot">
          ${['9 AM - 12 PM', '12 PM - 3 PM', '3 PM - 6 PM'].map(slot =>
            `<option value="${slot}" ${order.timeSlot === slot ? 'selected' : ''}>${slot}</option>`
          ).join('')}
        </select>
      </div>
      <div class="modal-footer">
        <button type="button" id="cancel-edit-order" class="btn btn-outline">Cancel</button>
        <button type="submit" class="btn btn-primary">Save Changes</button>
      </div>
    </form>
  `, (body) => {
    body.querySelector('#cancel-edit-order').addEventListener('click', closeModal);
    body.querySelector('#edit-order-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        orderId: order.orderId,
        customer: body.querySelector('#edit-customer').value.trim(),
        address: body.querySelector('#edit-address').value.trim(),
        area: body.querySelector('#edit-area').value.trim(),
        deliveryDate: body.querySelector('#edit-date').value.trim(),
        timeSlot: body.querySelector('#edit-slot').value.trim()
      };
      if (!payload.customer || !payload.address || !payload.area || !payload.deliveryDate) {
        showToast('Please fill in all fields.', 'error');
        return;
      }
      try {
        await api.updatePendingOrder(state.session.userId, payload);
        closeModal();
        showToast('Order updated successfully!', 'success');
        await refreshAllData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

// =========================================================
// DRIVERS
// =========================================================
async function loadDrivers() {
  state.drivers = await api.getDrivers(state.session.userId);
  renderDriversTable();
}

function getDriverActiveWorkload(driverName) {
  return state.assignments.filter(
    a => a.driver === driverName && (a.status === 'Assigned' || a.status === 'In Progress')
  ).length;
}

function renderDriversTable() {
  const total = state.drivers.length;
  const avail = state.drivers.filter(d => d.available).length;
  document.getElementById('drv-stat-total').textContent = total;
  document.getElementById('drv-stat-available').textContent = avail;
  document.getElementById('drv-stat-busy').textContent = Math.max(0, total - avail);

  const tbody = document.getElementById('drivers-tbody');
  if (!total) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No drivers registered yet. Click <strong>+ Add Driver</strong> to add your fleet.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.drivers.map(d => {
    const activeLoad = getDriverActiveWorkload(d.name);
    return `
      <tr>
        <td><strong>#${d.driverId}</strong></td>
        <td>
          <span class="cell-avatar">
            <span class="avatar-circle avatar-sm">${esc(getInitials(d.name))}</span>
            <span>${esc(d.name)}</span>
          </span>
        </td>
        <td>${esc(d.phone)}</td>
        <td><span class="area-tag">${esc(d.area)}</span></td>
        <td><strong>${activeLoad}</strong> active</td>
        <td>${statusBadgeHtml(d.available ? 'Available' : 'Busy')}</td>
        <td class="text-right">
          <label class="switch" title="Toggle availability for ${esc(d.name)}">
            <input type="checkbox" class="driver-switch-input" data-driver-id="${d.driverId}" ${d.available ? 'checked' : ''} />
            <span class="switch-slider"></span>
          </label>
        </td>
      </tr>
    `;
  }).join('');

  tbody.querySelectorAll('.driver-switch-input').forEach(input => {
    input.addEventListener('change', async () => {
      const driverId = Number(input.dataset.driverId);
      const nextAvailable = input.checked;
      try {
        await api.updateDriverAvailability(state.session.userId, driverId, nextAvailable);
        showToast(nextAvailable ? 'Driver marked Available.' : 'Driver marked Busy.', 'success');
        await refreshAllData();
      } catch (err) {
        input.checked = !nextAvailable;
        showToast(err.message, 'error');
      }
    });
  });
}

function openAddDriverModal() {
  openModal('Add New Fleet Driver', `
    <form id="add-driver-form" class="auth-form">
      <div class="form-group">
        <label for="drv-name">Driver Name *</label>
        <input id="drv-name" type="text" placeholder="e.g. John Doe" required />
      </div>
      <div class="form-group">
        <label for="drv-phone">Phone Number *</label>
        <input id="drv-phone" type="text" placeholder="e.g. +1 555-0199" required />
      </div>
      <div class="form-group">
        <label for="drv-area">Delivery Area *</label>
        <input id="drv-area" type="text" placeholder="e.g. Downtown" required />
      </div>
      <div class="modal-footer">
        <button type="button" id="cancel-add-driver" class="btn btn-outline">Cancel</button>
        <button type="submit" class="btn btn-primary">Add Driver</button>
      </div>
    </form>
  `, (body) => {
    body.querySelector('#cancel-add-driver').addEventListener('click', closeModal);
    body.querySelector('#add-driver-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = body.querySelector('#drv-name').value.trim();
      const phone = body.querySelector('#drv-phone').value.trim();
      const area = body.querySelector('#drv-area').value.trim();
      if (!name || !phone || !area) {
        showToast('Please fill all driver fields.', 'error');
        return;
      }
      try {
        await api.addDriver(state.session.userId, { name, phone, area });
        closeModal();
        showToast('Driver added successfully!', 'success');
        await refreshAllData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

// =========================================================
// ASSIGNMENTS
// =========================================================
async function loadAssignments() {
  state.assignments = await api.getAssignments(state.session.userId);
  renderAssignmentsTable();
  renderDriversTable();
}

function renderAssignmentsTable() {
  const tbody = document.getElementById('assignments-tbody');
  if (!state.assignments.length) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No assignments yet. Use <strong>⚡ Smart Assign</strong> on a Pending order to dispatch a driver.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.assignments.map(a => {
    const isFinal = a.status === 'Delivered' || a.status === 'Failed';
    return `
      <tr class="clickable-row" data-order-id="${a.orderId}">
        <td><strong>#${a.orderId}</strong></td>
        <td>
          <span class="cell-avatar">
            <span class="avatar-circle avatar-sm">${esc(getInitials(a.customer))}</span>
            <span>${esc(a.customer)}</span>
          </span>
        </td>
        <td>
          <span class="cell-avatar">
            <span class="avatar-circle avatar-sm">${esc(getInitials(a.driver))}</span>
            <span>${esc(a.driver)}</span>
          </span>
        </td>
        <td><span class="area-tag">${esc(a.area)}</span></td>
        <td>${statusBadgeHtml(a.status)}</td>
        <td class="text-right">
          <div class="row-actions-group">
            ${!isFinal
              ? `<button type="button" class="btn btn-secondary btn-sm row-update-status" data-order-id="${a.orderId}">Update Status</button>`
              : ''
            }
            <button type="button" class="btn btn-outline btn-sm">Details →</button>
          </div>
        </td>
      </tr>
    `;
  }).join('');

  tbody.querySelectorAll('tr.clickable-row').forEach(row => {
    row.addEventListener('click', (e) => {
      if (e.target.closest('.row-update-status')) return;
      const orderId = Number(row.dataset.orderId);
      const rec = state.assignments.find(x => x.orderId === orderId);
      if (rec) openAssignmentDetailsModal(rec);
    });
  });

  tbody.querySelectorAll('.row-update-status').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const orderId = Number(btn.dataset.orderId);
      const rec = state.assignments.find(x => x.orderId === orderId);
      if (rec) openUpdateStatusModal(rec);
    });
  });
}

function openAssignmentDetailsModal(rec) {
  const isFinal = rec.status === 'Delivered' || rec.status === 'Failed';
  openModal(`Assignment Details — Order #${rec.orderId}`, `
    <div class="detail-header-card">
      <div>
        <div style="font-weight:800;font-size:17px">Assignment #${rec.orderId}</div>
        <div style="font-size:13px;color:var(--text-secondary)">Customer: ${esc(rec.customer)}</div>
      </div>
      ${statusBadgeHtml(rec.status)}
    </div>

    ${lifecycleStepperHtml(rec.status)}

    <div class="detail-field">
      <div class="detail-field-label">Assigned Driver</div>
      <div class="detail-field-value">${esc(rec.driver)}</div>
    </div>
    <div class="detail-field">
      <div class="detail-field-label">Delivery Coverage Area</div>
      <div class="detail-field-value">${esc(rec.area)}</div>
    </div>
    <div class="detail-field">
      <div class="detail-field-label">Dispatched Timestamp</div>
      <div class="detail-field-value">${esc(rec.assignedAt || 'N/A')}</div>
    </div>
    ${rec.failureReason ? `
      <div class="detail-field danger">
        <div class="detail-field-label">Failure Reason</div>
        <div class="detail-field-value">${esc(rec.failureReason)}</div>
      </div>
    ` : ''}

    <div class="modal-footer">
      ${!isFinal ? `<button type="button" id="modal-update-status-btn" class="btn btn-primary">Update Status</button>` : ''}
      <button type="button" id="modal-close-asg-btn" class="btn btn-outline">Close</button>
    </div>
  `, (body) => {
    body.querySelector('#modal-close-asg-btn').addEventListener('click', closeModal);
    if (!isFinal) {
      body.querySelector('#modal-update-status-btn').addEventListener('click', () => {
        openUpdateStatusModal(rec);
      });
    }
  });
}

function openUpdateStatusModal(rec) {
  openModal(`Update Status — Order #${rec.orderId}`, `
    <form id="update-status-form" class="auth-form">
      <div class="form-group">
        <label for="status-select">Updated Delivery Status</label>
        <select id="status-select">
          <option value="In Progress" ${rec.status === 'In Progress' ? 'selected' : ''}>In Progress</option>
          <option value="Delivered">Delivered</option>
          <option value="Failed">Failed</option>
        </select>
      </div>
      <div id="failure-reason-group" class="form-group hidden">
        <label for="failure-reason-input">Reason for Delivery Failure *</label>
        <input id="failure-reason-input" type="text" placeholder="e.g. Customer unreachable / Wrong address" />
      </div>
      <div class="modal-footer">
        <button type="button" id="cancel-status-btn" class="btn btn-outline">Cancel</button>
        <button type="submit" class="btn btn-primary">Save Status</button>
      </div>
    </form>
  `, (body) => {
    const select = body.querySelector('#status-select');
    const reasonGroup = body.querySelector('#failure-reason-group');
    const reasonInput = body.querySelector('#failure-reason-input');

    const syncReasonVisibility = () => {
      const isFailed = select.value === 'Failed';
      reasonGroup.classList.toggle('hidden', !isFailed);
      reasonInput.required = isFailed;
    };
    select.addEventListener('change', syncReasonVisibility);
    syncReasonVisibility();

    body.querySelector('#cancel-status-btn').addEventListener('click', closeModal);
    body.querySelector('#update-status-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const newStatus = select.value;
      const reason = newStatus === 'Failed' ? reasonInput.value.trim() : null;
      if (newStatus === 'Failed' && !reason) {
        showToast('Failure reason cannot be empty.', 'error');
        return;
      }
      try {
        await api.updateDeliveryStatus(state.session.userId, rec.orderId, newStatus, reason);
        closeModal();
        showToast(`Delivery status updated to ${newStatus}.`, 'success');
        await refreshAllData();
      } catch (err) {
        showToast(err.message, 'error');
      }
    });
  });
}

// =========================================================
// REPORTS & CSV EXPORT
// =========================================================
async function loadReports() {
  const report = await api.getReportSummary(state.session.userId);
  state.lastReport = report;
  document.getElementById('report-total-orders').textContent = report.totalOrders ?? 0;
  document.getElementById('report-delivered-orders').textContent = report.deliveredOrders ?? 0;
  document.getElementById('report-failed-orders').textContent = report.failedOrders ?? 0;
  document.getElementById('report-success-rate').textContent = `${Number(report.successRate || 0).toFixed(1)}%`;

  renderReportsCharts(report);

  const tbody = document.getElementById('reports-area-tbody');
  const areas = report.areas || [];
  if (!areas.length) {
    tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No order data available for area reporting.</td></tr>`;
    return;
  }

  tbody.innerHTML = areas.map(a => {
    const rate = a.total > 0 ? ((a.delivered * 100) / a.total).toFixed(1) : '0.0';
    return `
      <tr>
        <td><span class="area-tag">${esc(a.area)}</span></td>
        <td>${a.total}</td>
        <td>${a.delivered}</td>
        <td>${a.failed}</td>
        <td><strong>${rate}%</strong></td>
      </tr>
    `;
  }).join('');
}

function exportReportCsv() {
  const areas = state.lastReport?.areas || [];
  if (!areas.length) {
    showToast('No area metrics available to export.', 'error');
    return;
  }
  const rows = [['Delivery Area', 'Total Orders', 'Delivered', 'Failed', 'Success Rate (%)']];
  for (const a of areas) {
    const rate = a.total > 0 ? ((a.delivered * 100) / a.total).toFixed(1) : '0.0';
    rows.push([`"${a.area.replaceAll('"', '""')}"`, a.total, a.delivered, a.failed, rate]);
  }
  const blob = new Blob([rows.map(r => r.join(',')).join('\n')], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `dispatchdesk-area-report-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
  showToast('Area report CSV downloaded.', 'success');
}

// =========================================================
// BOOTSTRAP & EVENT WIRING
// =========================================================
function initEventListeners() {
  document.getElementById('tab-signin').addEventListener('click', () => setAuthMode('signin'));
  document.getElementById('tab-register').addEventListener('click', () => setAuthMode('register'));
  document.getElementById('go-register-link').addEventListener('click', () => setAuthMode('register'));
  document.getElementById('go-signin-link').addEventListener('click', () => setAuthMode('signin'));

  document.querySelectorAll('.btn-toggle-pass').forEach(btn => {
    btn.addEventListener('click', () => {
      const target = document.getElementById(btn.dataset.target);
      const isPass = target.type === 'password';
      target.type = isPass ? 'text' : 'password';
      btn.textContent = isPass ? 'Hide' : 'Show';
    });
  });

  document.getElementById('demo-fill-btn').addEventListener('click', () => {
    document.getElementById('signin-username').value = 'admin';
    document.getElementById('signin-password').value = 'admin123';
    hideAuthAlert();
  });

  document.getElementById('signin-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('signin-username').value.trim();
    const password = document.getElementById('signin-password').value.trim();
    if (!username || !password) {
      showAuthAlert('Please enter your username and password.');
      return;
    }
    try {
      const session = await api.login(username, password);
      hideAuthAlert();
      await enterApp(session);
    } catch (err) {
      showAuthAlert(err.message || 'Invalid username or password.');
    }
  });

  document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fullName = document.getElementById('reg-fullname').value.trim();
    const username = document.getElementById('reg-username').value.trim();
    const password = document.getElementById('reg-password').value.trim();
    const confirm = document.getElementById('reg-confirm').value.trim();

    if (!fullName || !username || !password || !confirm) {
      showAuthAlert('Please fill in all registration fields.');
      return;
    }
    if (username.length < 3) {
      showAuthAlert('Username/ID must be at least 3 characters.');
      return;
    }
    if (password.length < 4) {
      showAuthAlert('Password must be at least 4 characters.');
      return;
    }
    if (password !== confirm) {
      showAuthAlert('Passwords do not match. Please verify.');
      return;
    }

    try {
      const session = await api.register(username, password, fullName);
      hideAuthAlert();
      await enterApp(session);
    } catch (err) {
      showAuthAlert(err.message || 'Could not register user.');
    }
  });

  document.getElementById('open-api-config-auth').addEventListener('click', openApiConfigModal);
  document.getElementById('header-backend-pill').addEventListener('click', openApiConfigModal);
  document.getElementById('enter-demo-preview-btn').addEventListener('click', async () => {
    api.setPreviewMode(true);
    await enterApp({ userId: 1, username: 'admin', fullName: 'Dispatch Admin', role: 'Lead Coordinator' });
    showToast('Entered Offline UI Preview with sample data.', 'info');
  });

  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', () => navigateTo(btn.dataset.page));
  });
  document.getElementById('mobile-menu-btn').addEventListener('click', () => {
    document.getElementById('sidebar').classList.add('open');
    document.getElementById('sidebar-backdrop').classList.remove('hidden');
  });
  document.getElementById('sidebar-backdrop').addEventListener('click', () => {
    document.getElementById('sidebar').classList.remove('open');
    document.getElementById('sidebar-backdrop').classList.add('hidden');
  });
  document.getElementById('logout-btn').addEventListener('click', logout);

  document.getElementById('topbar-new-order-btn').addEventListener('click', openAddOrderModal);
  document.getElementById('hero-add-driver-btn').addEventListener('click', openAddDriverModal);
  document.getElementById('view-all-orders-link').addEventListener('click', () => navigateTo('orders'));
  document.getElementById('refresh-dashboard-btn').addEventListener('click', refreshAllData);
  document.getElementById('add-order-btn').addEventListener('click', openAddOrderModal);
  document.getElementById('refresh-orders-btn').addEventListener('click', refreshAllData);
  document.getElementById('order-search-input').addEventListener('input', renderOrdersTable);
  document.getElementById('add-driver-btn').addEventListener('click', openAddDriverModal);
  document.getElementById('refresh-drivers-btn').addEventListener('click', refreshAllData);
  document.getElementById('refresh-assignments-btn').addEventListener('click', refreshAllData);
  document.getElementById('refresh-reports-btn').addEventListener('click', loadReports);
  document.getElementById('export-csv-btn').addEventListener('click', exportReportCsv);

  document.getElementById('modal-close-btn').addEventListener('click', closeModal);
  document.getElementById('modal-backdrop').addEventListener('click', (e) => {
    if (e.target.id === 'modal-backdrop') closeModal();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeModal();
  });
}

async function boot() {
  initEventListeners();
  await refreshBackendStatus();

  const savedSession = sessionStorage.getItem('dispatchdesk_session');
  if (savedSession && state.backendConnected) {
    try {
      await enterApp(JSON.parse(savedSession));
    } catch {
      sessionStorage.removeItem('dispatchdesk_session');
    }
  }
}

boot();
