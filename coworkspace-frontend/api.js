// ---- CoWorkSpace shared API helper ----
// Talks to the Spring Boot backend at localhost:8080

const API_BASE = String(window.COWORKSPACE_API_BASE || "http://localhost:8080/api").replace(/\/+$/, "");

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[char]));
}

function formatINR(amount) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(Number(amount));
}

const Auth = {
  getToken() {
    return localStorage.getItem("cw_token");
  },
  getUser() {
    const raw = localStorage.getItem("cw_user");
    if (!raw) return null;
    try { return JSON.parse(raw); } catch { return null; }
  },
  setSession(authResponse) {
    localStorage.setItem("cw_token", authResponse.token);
    localStorage.setItem(
      "cw_user",
      JSON.stringify({
        name: authResponse.name,
        email: authResponse.email,
        role: authResponse.role,
      })
    );
  },
  updateStoredName(name) {
    const user = this.getUser();
    if (user) {
      user.name = name;
      localStorage.setItem("cw_user", JSON.stringify(user));
    }
  },
  clearSession() {
    localStorage.removeItem("cw_token");
    localStorage.removeItem("cw_user");
  },
  isLoggedIn() {
    return !!this.getToken();
  },
  isAdmin() {
    const u = this.getUser();
    return u && u.role === "ADMIN";
  },
  // Redirect to login if not authenticated. Call at top of protected pages.
  requireLogin() {
    if (!this.isLoggedIn()) {
      window.location.href = "login.html";
    }
  },
  requireAdmin() {
    if (!this.isLoggedIn() || !this.isAdmin()) {
      window.location.href = "login.html";
    }
  },
  logout() {
    this.clearSession();
    window.location.href = "login.html";
  },
};

// Generic fetch wrapper that attaches JWT and handles JSON/errors uniformly.
async function apiFetch(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  const token = Auth.getToken();
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    });
  } catch (error) {
    if (error instanceof TypeError) {
      throw new Error(`Cannot connect to the CoWorkSpace backend at ${API_BASE}. Start the backend and try again.`);
    }
    throw error;
  }

  let body = null;
  const text = await res.text();
  if (text) {
    try {
      body = JSON.parse(text);
    } catch (e) {
      body = text;
    }
  }

  if (!res.ok) {
    const validationMessages =
      body && body.fields && Object.values(body.fields).filter(Boolean);
    const message =
      (validationMessages && validationMessages.length
        ? validationMessages.join(" ")
        : body && (body.error || body.message)) ||
      `Request failed with status ${res.status}`;

    if (res.status === 401 && !path.startsWith("/auth/")) {
      Auth.clearSession();
      window.location.href = "login.html";
    }

    throw new Error(message);
  }

  return body;
}

const Api = {
  // ---- Authentication ----
  register(name, email, password) {
    return apiFetch("/auth/register", { method: "POST", body: JSON.stringify({ name, email, password }) });
  },
  login(email, password) {
    return apiFetch("/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
  },

  // ---- Workspaces ----
  getSpaces() { return apiFetch("/spaces"); },
  getSpace(id) { return apiFetch(`/spaces/${id}`); },
  getAvailability(spaceId, start, end) {
    return apiFetch(`/spaces/${spaceId}/availability?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`);
  },
  createSpace(data) { return apiFetch("/spaces", { method: "POST", body: JSON.stringify(data) }); },
  updateSpace(id, data) { return apiFetch(`/spaces/${id}`, { method: "PUT", body: JSON.stringify(data) }); },
  deleteSpace(id) { return apiFetch(`/spaces/${id}`, { method: "DELETE" }); },
  addSeatUnit(spaceId, data) { return apiFetch(`/spaces/${spaceId}/seat-units`, { method: "POST", body: JSON.stringify(data) }); },

  // ---- Reservations (no OTP step) ----
  createBooking(data) { return apiFetch("/bookings", { method: "POST", body: JSON.stringify(data) }); },
  getMyBookings() { return apiFetch("/bookings/my"); },
  cancelBooking(id) { return apiFetch(`/bookings/${id}/cancel`, { method: "PUT" }); },

  // ---- Admin ----
  getAllBookings() { return apiFetch("/admin/bookings"); },

  // ---- Profile ----
  getProfile() { return apiFetch("/profile"); },
  updateProfile(name, phone) { return apiFetch("/profile", { method: "PUT", body: JSON.stringify({ name, phone }) }); },
  changePassword(currentPassword, newPassword) {
    return apiFetch("/profile/password", { method: "PUT", body: JSON.stringify({ currentPassword, newPassword }) });
  },
};
