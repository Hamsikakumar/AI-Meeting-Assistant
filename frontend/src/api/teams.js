const API_BASE = "http://localhost:8080/api";

function getToken() {
  return localStorage.getItem("token");
}

export async function createTeam(name) {
  const res = await fetch(`${API_BASE}/teams/create`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${getToken()}`,
    },
    body: JSON.stringify({ name }),
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to create team");
  return res.json();
}

export async function joinTeam(inviteCode) {
  const res = await fetch(`${API_BASE}/teams/join`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${getToken()}`,
    },
    body: JSON.stringify({ inviteCode }),
  });
  if (!res.ok) throw new Error(await res.text() || "Failed to join team");
  return res.json();
}

export async function getMyTeam() {
  const res = await fetch(`${API_BASE}/teams/me`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  if (!res.ok) throw new Error("Failed to fetch team");
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export async function leaveTeam() {
  const res = await fetch(`${API_BASE}/teams/leave`, {
    method: "POST",
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  if (!res.ok) throw new Error("Failed to leave team");
}