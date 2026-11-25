/**
 * Backend API base URL configuration.
 * Defaults to http://localhost:8080, can be overridden via ?api=<url> query param
 * or configured from the UI and persisted in localStorage.
 */

const STORAGE_KEY = 'dispatchdesk_api_base';
const DEFAULT_API_BASE = 'https://dispatchdesk-backend.onrender.com';

export function getApiBaseUrl() {
  const params = new URLSearchParams(window.location.search);
  const queryApi = params.get('api');
  if (queryApi && queryApi.trim()) {
    const cleaned = queryApi.trim().replace(/\/+$/, '');
    localStorage.setItem(STORAGE_KEY, cleaned);
    return cleaned;
  }

  const saved = localStorage.getItem(STORAGE_KEY);
  return saved && saved.trim() ? saved.trim().replace(/\/+$/, '') : DEFAULT_API_BASE;
}

export function setApiBaseUrl(url) {
  const cleaned = (url || DEFAULT_API_BASE).trim().replace(/\/+$/, '');
  localStorage.setItem(STORAGE_KEY, cleaned || DEFAULT_API_BASE);
  return getApiBaseUrl();
}
