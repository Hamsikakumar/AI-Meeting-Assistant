import { useState, useEffect, useRef } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { uploadMeeting, getMeetings, summarizeMeeting } from "../api/meetings";
import { createTeam, joinTeam, getMyTeam, leaveTeam } from "../api/teams";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [team, setTeam] = useState(null);
  const [teamNameInput, setTeamNameInput] = useState("");
  const [inviteCodeInput, setInviteCodeInput] = useState("");
  const [teamError, setTeamError] = useState("");

  const [meetings, setMeetings] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [expandedId, setExpandedId] = useState(null);
  const [summarizingId, setSummarizingId] = useState(null);

  const pollingRef = useRef(null);
  const MAX_FILE_SIZE = 25 * 1024 * 1024; // 25MB, matches backend/Groq limit

  useEffect(() => {
    loadMeetings();

    pollingRef.current = setInterval(() => {
      loadMeetings();
    }, 5000);

    return () => clearInterval(pollingRef.current);
  }, []);

  async function loadMeetings() {
    try {
      const data = await getMeetings();
      setMeetings(data);
    } catch (err) {
      setError(err.message);
    }
  }
  useEffect(() => {
    loadTeam();
  }, []);

  async function loadTeam() {
    try {
      const data = await getMyTeam();
      setTeam(data);
    } catch (err) {
      console.error(err);
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

  async function handleCreateTeam(e) {
    e.preventDefault();
    setTeamError("");
    try {
      const newTeam = await createTeam(teamNameInput);
      setTeam(newTeam);
      setTeamNameInput("");
      await loadMeetings();
    } catch (err) {
      setTeamError(err.message);
    }
  }

  async function handleJoinTeam(e) {
    e.preventDefault();
    setTeamError("");
    try {
      const joinedTeam = await joinTeam(inviteCodeInput);
      setTeam(joinedTeam);
      setInviteCodeInput("");
      await loadMeetings();
    } catch (err) {
      setTeamError(err.message);
    }
  }

  async function handleLeaveTeam() {
    try {
      await leaveTeam();
      setTeam(null);
      await loadMeetings();
    } catch (err) {
      setTeamError(err.message);
    }
  }

  async function handleSummarize(id) {
    setSummarizingId(id);
    setError("");
    try {
      await summarizeMeeting(id);
      await loadMeetings();
    } catch (err) {
      setError(err.message);
    } finally {
      setSummarizingId(null);
    }
  }

  function handleLogout() {
    clearInterval(pollingRef.current);
    logout();
    navigate("/login");
  }

  function toggleExpand(id) {
    setExpandedId(expandedId === id ? null : id);
  }

  function statusColor(status) {
    switch (status) {
      case "COMPLETED": return "green";
      case "PROCESSING": return "orange";
      case "FAILED": return "red";
      default: return "gray";
    }
  }

  return (
    <div style={{ maxWidth: 700, margin: "60px auto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h2>Welcome, {user?.name}!</h2>
        <button onClick={handleLogout}>Log Out</button>
      </div>

      <hr style={{ margin: "24px 0" }} />
      <hr style={{ margin: "24px 0" }} />

      <h3>Team</h3>
      {team ? (
        <div style={{ background: "#f0f7ff", padding: 12, borderRadius: 6 }}>
          <p><strong>{team.name}</strong></p>
          <p>Invite code: <code>{team.inviteCode}</code> (share this with teammates)</p>
          <button onClick={handleLeaveTeam}>Leave Team</button>
        </div>
      ) : (
      <div>
        <form onSubmit={handleCreateTeam} style={{ marginBottom: 10 }}>
          <input
            type="text"
            placeholder="Team name"
            value={teamNameInput}
            onChange={(e) => setTeamNameInput(e.target.value)}
          />
          <button type="submit" style={{ marginLeft: 8 }}>Create Team</button>
        </form>
        <form onSubmit={handleJoinTeam}>
          <input
            type="text"
            placeholder="Invite code"
            value={inviteCodeInput}
            onChange={(e) => setInviteCodeInput(e.target.value)}
          />
          <button type="submit" style={{ marginLeft: 8 }}>Join Team</button>
        </form>
      </div>
    )}
    {teamError && <p style={{ color: "red" }}>{teamError}</p>}

      <h3>Upload a Meeting Recording</h3>
      <form onSubmit={handleUpload}>
        <input
         type="file"
        accept="audio/*,video/*"
        onChange={(e) => {
          const file = e.target.files[0];
          if (!file) return;

          if (file.size > MAX_FILE_SIZE) {
            setError(
              `File too large (${(file.size / 1024 / 1024).toFixed(1)}MB). Maximum size is 25MB.`
            );
            setSelectedFile(null);
            e.target.value = ""; // reset the input so the same oversized file can be re-picked after fixing
            return;
          }

          setError("");
          setSelectedFile(file);
        }}
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
        <ul style={{ listStyle: "none", padding: 0 }}>
          {meetings.map((m) => (
            <li
              key={m.id}
              style={{
                border: "1px solid #ddd",
                borderRadius: 6,
                padding: 12,
                marginBottom: 10,
              }}
            >
              <div
                style={{ cursor: m.status === "COMPLETED" ? "pointer" : "default" }}
                onClick={() => m.status === "COMPLETED" && toggleExpand(m.id)}
              >
                <strong>{m.originalFilename}</strong> —{" "}
                <span style={{ color: statusColor(m.status), fontWeight: "bold" }}>
                  {m.status}
                </span>{" "}
                — {new Date(m.createdAt).toLocaleString()}
                {m.status === "COMPLETED" && (
                  <span style={{ marginLeft: 8, fontSize: 12, color: "#666" }}>
                    {expandedId === m.id ? "▲ hide transcript" : "▼ show transcript"}
                  </span>
                )}
              </div>

              {m.status === "PROCESSING" && (
                <p style={{ fontSize: 13, color: "#888", marginTop: 6 }}>
                  Transcribing... this page will update automatically.
                </p>
              )}

              {m.status === "FAILED" && (
                <p style={{ fontSize: 13, color: "red", marginTop: 6 }}>
                  Transcription failed for this file.
                </p>
              )}

              {expandedId === m.id && m.transcript && (
                <p style={{ marginTop: 10, fontSize: 14, lineHeight: 1.5, whiteSpace: "pre-wrap" }}>
                  {m.transcript}
                </p>
              )}

              {m.status === "COMPLETED" && (
                <div style={{ marginTop: 10 }}>
                  {!m.summary ? (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleSummarize(m.id);
                      }}
                      disabled={summarizingId === m.id}
                    >
                      {summarizingId === m.id ? "Generating summary..." : "Generate Summary"}
                    </button>
                  ) : (
                    <div style={{ background: "#f7f7f7", padding: 10, borderRadius: 6 }}>
                      <p><strong>Summary:</strong> {m.summary}</p>
                      <p><strong>Action Items:</strong> {m.actionItems}</p>
                      <p><strong>Deadlines:</strong> {m.deadlines}</p>
                    </div>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}