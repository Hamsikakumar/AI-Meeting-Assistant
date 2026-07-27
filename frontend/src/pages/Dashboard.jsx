import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { uploadMeeting, getMeetings } from "../api/meetings";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [meetings, setMeetings] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    loadMeetings();
  }, []);

  async function loadMeetings() {
    try {
      const data = await getMeetings();
      setMeetings(data);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleUpload(e) {
    e.preventDefault();
    if (!selectedFile) return;

    setError("");
    setUploading(true);

    try {
      await uploadMeeting(selectedFile);
      setSelectedFile(null);
      await loadMeetings();
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
    }
  }

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div style={{ maxWidth: 700, margin: "60px auto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h2>Welcome, {user?.name}!</h2>
        <button onClick={handleLogout}>Log Out</button>
      </div>

      <hr style={{ margin: "24px 0" }} />

      <h3>Upload a Meeting Recording</h3>
      <form onSubmit={handleUpload}>
        <input
          type="file"
          accept="audio/*,video/*"
          onChange={(e) => setSelectedFile(e.target.files[0])}
        />
        <button type="submit" disabled={!selectedFile || uploading} style={{ marginLeft: 12 }}>
          {uploading ? "Uploading..." : "Upload"}
        </button>
      </form>

      {error && <p style={{ color: "red" }}>{error}</p>}

      <hr style={{ margin: "24px 0" }} />

      <h3>Your Meetings</h3>
      {meetings.length === 0 ? (
        <p>No meetings uploaded yet.</p>
      ) : (
        <ul>
          {meetings.map((m) => (
            <li key={m.id}>
              <strong>{m.originalFilename}</strong> — {m.status} —{" "}
              {new Date(m.createdAt).toLocaleString()}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}