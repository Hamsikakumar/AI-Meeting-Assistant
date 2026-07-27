const API_BASE = "http://localhost:8080/api";

function getToken() {
  return localStorage.getItem("token");
}

export async function uploadMeeting(file) {
  const formData = new FormData();
  formData.append("file", file);

  const res = await fetch(`${API_BASE}/meetings/upload`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
    body: formData,
  });

  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(errorText || "Upload failed");
  }

  return res.json();
}

export async function getMeetings() {
  const res = await fetch(`${API_BASE}/meetings`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });

  if (!res.ok) {
    throw new Error("Failed to fetch meetings");
  }

  return res.json();
}