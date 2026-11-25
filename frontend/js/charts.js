/**
 * SVG and HTML data visualization renderers for Dashboard & Reports.
 */

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

export function renderDashboardPipeline(stats) {
  const barEl = document.getElementById('pipeline-bar');
  const stagesEl = document.getElementById('pipeline-stages');
  const total = stats.totalOrders || 0;

  const stages = [
    { label: 'Delivered', count: stats.deliveredOrders || 0, color: '#424874' },
    { label: 'In Progress', count: stats.inProgressOrders || 0, color: '#7179A8' },
    { label: 'Assigned', count: stats.assignedOrders || 0, color: '#A6B1E1' },
    { label: 'Pending', count: stats.pendingOrders || 0, color: '#DCD6F7' },
    { label: 'Failed', count: stats.failedOrders || 0, color: '#C7B5CF' }
  ];

  barEl.innerHTML = total === 0
    ? ''
    : stages
        .filter(s => s.count > 0)
        .map(s => {
          const pct = (s.count / total) * 100;
          return `<div class="pipeline-seg" style="width:${pct.toFixed(2)}%;background:${s.color}" title="${s.label}: ${s.count}"></div>`;
        })
        .join('');

  stagesEl.innerHTML = stages
    .map(s => {
      const pct = total > 0 ? Math.round((s.count * 100) / total) : 0;
      return `
        <div class="stage-tile">
          <div class="stage-tile-top">
            <span class="stage-dot" style="background:${s.color}"></span>
            <span>${escapeHtml(s.label)}</span>
          </div>
          <div class="stage-tile-bottom">
            <span class="stage-count">${s.count}</span>
            <span class="stage-pct">${pct}%</span>
          </div>
        </div>
      `;
    })
    .join('');

  const totalDrivers = stats.totalDrivers || 0;
  const availableDrivers = stats.availableDrivers || 0;
  const busyDrivers = Math.max(0, totalDrivers - availableDrivers);
  const availPct = totalDrivers > 0 ? Math.round((availableDrivers * 100) / totalDrivers) : 0;

  document.getElementById('fleet-capacity-label').textContent =
    `${availPct}% Available (${availableDrivers} of ${totalDrivers} on standby)`;
  document.getElementById('fleet-bar-fill').style.width = `${availPct}%`;
  document.getElementById('fleet-standby-text').textContent = `Standby: ${availableDrivers} drivers`;
  document.getElementById('fleet-busy-text').textContent = `On Route / Busy: ${busyDrivers} drivers`;
  document.getElementById('fleet-total-text').textContent = `Total Fleet: ${totalDrivers} registered`;
}

export function renderReportsCharts(report) {
  renderAreaBarChart(report.areas || []);
  renderSuccessGauge(report);
}

function renderAreaBarChart(areas) {
  const container = document.getElementById('area-bar-chart');
  if (!areas.length) {
    container.innerHTML = `<div class="empty-state">No area records available yet.</div>`;
    return;
  }

  const maxVal = Math.max(1, ...areas.map(a => a.total));
  const width = Math.max(480, areas.length * 112);
  const height = 220;
  const padTop = 24;
  const padBottom = 36;
  const plotH = height - padTop - padBottom;
  const slotW = width / areas.length;
  const barW = Math.min(22, Math.max(12, Math.floor((slotW - 26) / 3)));

  const barsSvg = areas.map((a, i) => {
    const center = i * slotW + slotW / 2;
    const startX = center - ((barW * 3 + 6) / 2);

    const drawRect = (val, x, color, label) => {
      if (!val) return '';
      const h = Math.max(6, Math.round((val / maxVal) * (plotH - 14)));
      const y = padTop + plotH - h;
      return `
        <g>
          <title>${escapeHtml(a.area)} — ${label}: ${val}</title>
          <rect x="${x}" y="${y}" width="${barW}" height="${h}" rx="5" fill="${color}"></rect>
          <text x="${x + barW / 2}" y="${y - 6}" text-anchor="middle" font-size="10.5" font-weight="800" fill="#424874">${val}</text>
        </g>
      `;
    };

    const shortLabel = a.area.length > 13 ? `${a.area.slice(0, 11)}..` : a.area;
    return `
      <g>
        ${drawRect(a.total, startX, '#424874', 'Total')}
        ${drawRect(a.delivered, startX + barW + 3, '#A6B1E1', 'Delivered')}
        ${drawRect(a.failed, startX + (barW + 3) * 2, '#DCD6F7', 'Failed')}
        <text x="${center}" y="${height - 10}" text-anchor="middle" font-size="11.5" font-weight="700" fill="#686E95">
          ${escapeHtml(shortLabel)}
        </text>
      </g>
    `;
  }).join('');

  container.innerHTML = `
    <svg viewBox="0 0 ${width} ${height}" width="100%" height="${height}" role="img" aria-label="Area Fulfillment Comparison">
      <line x1="12" y1="${padTop}" x2="${width - 12}" y2="${padTop}" stroke="#F3EFFB" stroke-dasharray="4 4" />
      <line x1="12" y1="${padTop + plotH / 2}" x2="${width - 12}" y2="${padTop + plotH / 2}" stroke="#EFEAFB" stroke-dasharray="4 4" />
      <line x1="12" y1="${padTop + plotH}" x2="${width - 12}" y2="${padTop + plotH}" stroke="#DCD6F7" />
      ${barsSvg}
    </svg>
  `;
}

function renderSuccessGauge(report) {
  const container = document.getElementById('success-ring-gauge');
  const rate = Number(report.successRate || 0);
  const radius = 58;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (Math.min(100, Math.max(0, rate)) / 100) * circumference;

  container.innerHTML = `
    <svg viewBox="0 0 154 154" width="154" height="154" role="img" aria-label="Success Rate Gauge">
      <circle cx="77" cy="77" r="${radius}" fill="none" stroke="#EDE9FA" stroke-width="14"></circle>
      <circle
        cx="77" cy="77" r="${radius}" fill="none"
        stroke="#424874" stroke-width="14" stroke-linecap="round"
        stroke-dasharray="${circumference.toFixed(2)}"
        stroke-dashoffset="${offset.toFixed(2)}"
        transform="rotate(-90 77 77)">
      </circle>
      <text x="77" y="75" text-anchor="middle" font-size="22" font-weight="800" fill="#424874">
        ${rate.toFixed(1)}%
      </text>
      <text x="77" y="95" text-anchor="middle" font-size="10.5" font-weight="700" fill="#9398B8">
        FULFILLMENT
      </text>
    </svg>
    <p class="card-subtitle" style="margin-top:8px">
      ${report.deliveredOrders || 0} Delivered · ${report.failedOrders || 0} Failed · ${report.totalOrders || 0} Total
    </p>
  `;
}
