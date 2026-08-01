// ---- CoWorkSpace shared API helper ----
// Talks to the Spring Boot backend at localhost:8080

const API_BASE = "http://localhost:8080/api";

const Auth = {
  getToken() {
    return localStorage.getItem("cw_token");
  },
  getUser() {
    const raw = localStorage.getItem("cw_user");
    return raw ? JSON.parse(raw) : null;
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

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  // 401/403 -> session invalid/expired, kick back to login
  if (res.status === 401 || res.status === 403) {
    Auth.clearSession();
    window.location.href = "login.html";
    throw new Error("Not authenticated");
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
    const message =
      (body && (body.error || body.message)) ||
      `Request failed with status ${res.status}`;
    throw new Error(message);
  }

  return body;
}

const Api = {
  // ---- Auth ----
  register(name, email, password) {
    return apiFetch("/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password }),
    });
  },
  login(email, password) {
    return apiFetch("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
  },

  // ---- Spaces ----
  getSpaces() {
    return apiFetch("/spaces");
  },
  getSpace(id) {
    return apiFetch(`/spaces/${id}`);
  },
  getAvailability(spaceId, start, end) {
    return apiFetch(
      `/spaces/${spaceId}/availability?start=${start}&end=${end}`
    );
  },
  createSpace(data) {
    return apiFetch("/spaces", { method: "POST", body: JSON.stringify(data) });
  },
  updateSpace(id, data) {
    return apiFetch(`/spaces/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },
  deleteSpace(id) {
    return apiFetch(`/spaces/${id}`, { method: "DELETE" });
  },
  addSeatUnit(spaceId, data) {
    return apiFetch(`/spaces/${spaceId}/seat-units`, {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  // ---- Bookings ----
  createBooking(data) {
    return apiFetch("/bookings", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },
  getMyBookings() {
    return apiFetch("/bookings/my");
  },
  cancelBooking(id) {
    return apiFetch(`/bookings/${id}/cancel`, { method: "PUT" });
  },

  // ---- Admin ----
  getAllBookings() {
    return apiFetch("/admin/bookings");
  },
};
