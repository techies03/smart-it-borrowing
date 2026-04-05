/* ============================================================
   Smart IT Borrowing — Client-side Logic
   Theme toggle · Toast notifications · Modal · Mobile nav
   ============================================================ */

document.addEventListener('DOMContentLoaded', function () {
  initTheme();
  initMobileNav();
  initAlertToasts();
  initImagePreview();
  initAdminConfirmations();
  initDashboardExportModal();
});

/* ===========================
   1. Theme Toggle
   =========================== */
function initTheme() {
  const saved = localStorage.getItem('theme');
  if (saved) {
    document.documentElement.setAttribute('data-theme', saved);
  } else {
    // default to light
    document.documentElement.setAttribute('data-theme', 'light');
  }
  updateThemeIcon();
}

function toggleTheme() {
  const current = document.documentElement.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
  updateThemeIcon();
}

function updateThemeIcon() {
  const btn = document.getElementById('theme-toggle-btn');
  if (!btn) return;
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  btn.textContent = isDark ? '☼' : '☾';
  btn.title = isDark ? 'Switch to light mode' : 'Switch to dark mode';
  btn.setAttribute('aria-label', btn.title);
}

/* ===========================
   2. Toast Notifications
   =========================== */
function getToastContainer() {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  return container;
}

const TOAST_ICONS = {
  success: '✅',
  danger:  '❌',
  warning: '⚠️',
  info:    'ℹ️'
};

/**
 * Show a toast notification.
 * @param {string} message
 * @param {'success'|'danger'|'warning'|'info'} type
 * @param {number} duration – ms (default 4500)
 */
function showToast(message, type, duration) {
  type = type || 'info';
  duration = duration || 4500;

  const container = getToastContainer();
  const toast = document.createElement('div');
  toast.className = 'toast toast-' + type;
  toast.innerHTML =
    '<span class="toast-icon">' + (TOAST_ICONS[type] || '') + '</span>' +
    '<span class="toast-body">' + message + '</span>' +
    '<button class="toast-close" aria-label="Close">&times;</button>' +
    '<div class="toast-progress" style="animation-duration:' + duration + 'ms"></div>';

  container.appendChild(toast);

  // Close button
  toast.querySelector('.toast-close').addEventListener('click', function () {
    removeToast(toast);
  });

  // Auto-remove
  setTimeout(function () { removeToast(toast); }, duration);
}

function removeToast(toast) {
  if (toast.classList.contains('removing')) return;
  toast.classList.add('removing');
  toast.addEventListener('animationend', function () { toast.remove(); });
}

/**
 * Convert server-side .alert divs into toasts, then remove them.
 */
function initAlertToasts() {
  var alerts = document.querySelectorAll('.alert');
  alerts.forEach(function (el) {
    var type = 'info';
    if (el.classList.contains('alert-success')) type = 'success';
    else if (el.classList.contains('alert-danger'))  type = 'danger';
    else if (el.classList.contains('alert-warning')) type = 'warning';
    showToast(el.textContent.trim(), type);
    el.remove();
  });
}

/* ===========================
   3. Modal System
   =========================== */
function openModal(id) {
  var modal = document.getElementById(id);
  if (!modal) return;
  modal.style.display = 'flex';
  modal.classList.remove('hiding');
  // Trap focus (simple: focus first input)
  var firstInput = modal.querySelector('input, select, button');
  if (firstInput) setTimeout(function () { firstInput.focus(); }, 80);
}

function closeModal(id) {
  var modal = document.getElementById(id);
  if (!modal) return;
  if (id === 'confirm-modal') {
    pendingAdminConfirmForm = null;
  }
  modal.classList.add('hiding');
  modal.addEventListener('animationend', function handler() {
    modal.style.display = 'none';
    modal.classList.remove('hiding');
    modal.removeEventListener('animationend', handler);
  });
}

// Close on overlay click
document.addEventListener('click', function (e) {
  if (e.target.classList.contains('modal-overlay')) {
    closeModal(e.target.id);
  }
});

// Close on Escape
document.addEventListener('keydown', function (e) {
  if (e.key === 'Escape') {
    var modals = document.querySelectorAll('.modal-overlay');
    modals.forEach(function (m) {
      if (m.style.display !== 'none' && m.style.display !== '') closeModal(m.id);
    });
  }
});

/* ===========================
   4. Mobile Nav
   =========================== */
function initMobileNav() {
  var btn = document.getElementById('nav-hamburger');
  var links = document.getElementById('nav-links');
  if (!btn || !links) return;
  btn.addEventListener('click', function () {
    links.classList.toggle('open');
  });
  // Close when clicking a link
  links.querySelectorAll('a').forEach(function (a) {
    a.addEventListener('click', function () { links.classList.remove('open'); });
  });
}

/* ===========================
   5. Password Toggle
   =========================== */
function togglePassword(inputId) {
  var input = document.getElementById(inputId);
  if (!input) return;
  var isPassword = input.type === 'password';
  input.type = isPassword ? 'text' : 'password';
  var btn = input.parentElement.querySelector('.password-toggle');
  if (btn) btn.textContent = isPassword ? '🙈' : '👁️';
}

/* ===========================
   6. Return Modal (Booking)
   =========================== */
function showReturnModal(bookingId) {
  document.getElementById('returnBookingId').value = bookingId;
  openModal('return-modal');
}

function hideReturnModal() { closeModal('return-modal'); }

/* ===========================
   7. Admin Confirm Modal
   =========================== */
var pendingAdminConfirmForm = null;

function initAdminConfirmations() {
  var forms = document.querySelectorAll('form[data-confirm-message]');
  var confirmButton = document.getElementById('confirm-submit-btn');
  if (!forms.length || !confirmButton) return;

  forms.forEach(function (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      pendingAdminConfirmForm = form;
      showAdminConfirmModal(form);
    });
  });

  confirmButton.addEventListener('click', function () {
    if (!pendingAdminConfirmForm) return;
    var damageFeeInput = document.getElementById('confirm-damage-fee-input');
    var hiddenDamageFee = pendingAdminConfirmForm.querySelector('input[name="damageFee"]');
    if (hiddenDamageFee && damageFeeInput) {
      if (pendingAdminConfirmForm.dataset.damageFeeRequired === 'true') {
        var parsedFee = parseFloat(damageFeeInput.value || '0');
        if (!damageFeeInput.value || Number.isNaN(parsedFee) || parsedFee <= 0) {
          damageFeeInput.focus();
          return;
        }
      }
      hiddenDamageFee.value = damageFeeInput.value || '0.00';
    }
    var formToSubmit = pendingAdminConfirmForm;
    pendingAdminConfirmForm = null;
    closeModal('confirm-modal');
    formToSubmit.submit();
  });
}

function showAdminConfirmModal(form) {
  var title = form.dataset.confirmTitle || 'Confirm action';
  var message = form.dataset.confirmMessage || 'Proceed with this admin action?';
  var buttonLabel = form.dataset.confirmButton || 'Confirm';
  var buttonTone = form.dataset.confirmTone || 'primary';
  var damageFeeRequired = form.dataset.damageFeeRequired === 'true';

  var titleEl = document.getElementById('confirm-title');
  var messageEl = document.getElementById('confirm-message');
  var buttonEl = document.getElementById('confirm-submit-btn');
  var damageFeeGroup = document.getElementById('confirm-damage-fee-group');
  var damageFeeInput = document.getElementById('confirm-damage-fee-input');
  var damageFeeNote = document.getElementById('confirm-damage-fee-note');

  if (titleEl) titleEl.textContent = title;
  if (messageEl) messageEl.textContent = message;
  if (buttonEl) {
    buttonEl.textContent = buttonLabel;
    buttonEl.className = 'btn btn-' + buttonTone;
  }
  if (damageFeeGroup && damageFeeInput && damageFeeNote) {
    if (damageFeeRequired) {
      damageFeeGroup.style.display = '';
      damageFeeInput.required = true;
      damageFeeInput.min = '0.01';
      damageFeeInput.value = form.dataset.damageFeeDefault || '0.00';
      damageFeeNote.textContent = form.dataset.damageFeeNote || 'Add the additional damage charge to be combined with any late fee.';
    } else {
      damageFeeGroup.style.display = 'none';
      damageFeeInput.required = false;
      damageFeeInput.min = '0';
      damageFeeInput.value = '0.00';
      damageFeeNote.textContent = 'Add the additional damage charge to be combined with any late fee.';
    }
  }

  openModal('confirm-modal');
}

function hideConfirmModal() {
  pendingAdminConfirmForm = null;
  closeModal('confirm-modal');
}

/* ===========================
   8. Dashboard Export Modal
   =========================== */
function initDashboardExportModal() {
  var form = document.getElementById('dashboard-export-form');
  if (!form) return;

  var reportSelect = document.getElementById('dashboard-export-report');
  var formatSelect = document.getElementById('dashboard-export-format');
  var fromDateInput = document.getElementById('dashboard-export-from-date');
  var toDateInput = document.getElementById('dashboard-export-to-date');
  var note = document.getElementById('dashboard-export-note');
  var submitButton = document.getElementById('dashboard-export-submit');

  function syncDateBounds() {
    if (!fromDateInput || !toDateInput) return;
    toDateInput.min = fromDateInput.value || '';
    fromDateInput.max = toDateInput.value || '';
  }

  function updateExportNote() {
    if (!reportSelect || !formatSelect || !note) return;
    var report = reportSelect.value;
    var format = formatSelect.value;
    var formatText = format === 'pdf'
      ? 'PDF is best for sharing or printing.'
      : 'Excel is better for sorting, filtering, and further analysis.';

    if (report === 'data') {
      note.textContent = 'Booking Data exports detailed booking rows whose borrow dates fall inside the selected range. ' + formatText;
    } else {
      note.textContent = 'Dashboard Summary includes the current equipment snapshot plus booking metrics and trends for borrow dates inside the selected range. ' + formatText;
    }
  }

  if (reportSelect) reportSelect.addEventListener('change', updateExportNote);
  if (formatSelect) formatSelect.addEventListener('change', updateExportNote);
  if (fromDateInput) fromDateInput.addEventListener('change', syncDateBounds);
  if (toDateInput) toDateInput.addEventListener('change', syncDateBounds);

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    if (!form.reportValidity()) return;

    var originalLabel = submitButton ? submitButton.textContent : '';
    if (submitButton) {
      submitButton.disabled = true;
      submitButton.textContent = 'Preparing...';
    }

    try {
      var query = new URLSearchParams(new FormData(form)).toString();
      var response = await fetch(form.action + '?' + query, {
        method: 'GET',
        credentials: 'same-origin'
      });

      if (!response.ok) {
        var message = await response.text();
        throw new Error(message || 'Failed to export report.');
      }

      var blob = await response.blob();
      var fileName = extractDownloadFilename(response.headers.get('Content-Disposition'));
      var url = URL.createObjectURL(blob);
      var link = document.createElement('a');
      link.href = url;
      link.download = fileName || buildFallbackExportFilename(reportSelect, formatSelect);
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(function () { URL.revokeObjectURL(url); }, 1000);

      hideDashboardExportModal();
      showToast('Export downloaded successfully.', 'success', 3200);
    } catch (error) {
      showToast(error && error.message ? error.message : 'Failed to export report.', 'danger', 5000);
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent = originalLabel || 'Download Export';
      }
    }
  });

  syncDateBounds();
  updateExportNote();
}

function extractDownloadFilename(contentDisposition) {
  if (!contentDisposition) return '';
  var utfMatch = contentDisposition.match(/filename\*\s*=\s*UTF-8''([^;]+)/i);
  if (utfMatch && utfMatch[1]) {
    try {
      return decodeURIComponent(utfMatch[1].trim());
    } catch (e) {
      return utfMatch[1].trim();
    }
  }

  var basicMatch = contentDisposition.match(/filename\s*=\s*"?(.*?)"?(?:;|$)/i);
  return basicMatch && basicMatch[1] ? basicMatch[1].trim() : '';
}

function buildFallbackExportFilename(reportSelect, formatSelect) {
  var report = reportSelect ? reportSelect.value : 'summary';
  var format = formatSelect ? formatSelect.value : 'xlsx';
  return (report === 'data' ? 'booking-data' : 'dashboard-summary') + '.' + format;
}

function showDashboardExportModal() {
  openModal('dashboard-export-modal');
}

function hideDashboardExportModal() {
  closeModal('dashboard-export-modal');
}

/* ===========================
   9. Item Search Filter
   =========================== */
function initItemSearch() {
  var searchInput = document.getElementById('item-search');
  if (!searchInput) return;
  searchInput.addEventListener('input', function () {
    var query = this.value.toLowerCase();
    var cards = document.querySelectorAll('.item-card');
    cards.forEach(function (card) {
      var text = card.textContent.toLowerCase();
      card.style.display = text.includes(query) ? '' : 'none';
    });
  });
}

/* ===========================
   10. Item Image Preview
   =========================== */
function initImagePreview() {
  var input = document.getElementById('image');
  var preview = document.getElementById('item-image-preview');
  var placeholder = document.getElementById('item-image-placeholder');
  var panel = document.querySelector('.image-preview-panel');
  var fileUpload = document.querySelector('.file-upload');
  var fileName = document.getElementById('image-file-name');
  if (!input || !preview || !placeholder || !panel) return;

  var activeObjectUrl = null;

  function setFileState(text, hasFile) {
    if (fileName) {
      fileName.textContent = text;
    }
    if (fileUpload) {
      fileUpload.classList.toggle('has-file', Boolean(hasFile));
    }
  }

  function formatFileSize(bytes) {
    if (!bytes || bytes < 1024) return (bytes || 0) + ' B';
    var units = ['KB', 'MB', 'GB'];
    var size = bytes / 1024;
    var unitIndex = 0;
    while (size >= 1024 && unitIndex < units.length - 1) {
      size = size / 1024;
      unitIndex++;
    }
    return size.toFixed(size >= 10 || unitIndex > 0 ? 1 : 2).replace(/\.0$/, '') + ' ' + units[unitIndex];
  }

  if (preview.dataset.currentSrc) {
    setFileState('Current image is still active. Choose a new file to replace it.', false);
  } else {
    setFileState('No image selected yet.', false);
  }

  input.addEventListener('change', function () {
    var file = input.files && input.files[0];
    if (!file) {
      if (activeObjectUrl) {
        URL.revokeObjectURL(activeObjectUrl);
        activeObjectUrl = null;
      }

      if (preview.dataset.currentSrc) {
        preview.src = preview.dataset.currentSrc;
        preview.style.display = 'block';
        placeholder.style.display = 'none';
        panel.classList.remove('is-empty');
        setFileState('Current image is still active. Choose a new file to replace it.', false);
      } else {
        preview.removeAttribute('src');
        preview.style.display = 'none';
        placeholder.style.display = '';
        panel.classList.add('is-empty');
        setFileState('No image selected yet.', false);
      }
      return;
    }

    if (activeObjectUrl) {
      URL.revokeObjectURL(activeObjectUrl);
    }

    activeObjectUrl = URL.createObjectURL(file);
    preview.src = activeObjectUrl;
    preview.style.display = 'block';
    placeholder.style.display = 'none';
    panel.classList.remove('is-empty');
    setFileState(file.name + ' • ' + formatFileSize(file.size), true);
  });
}
