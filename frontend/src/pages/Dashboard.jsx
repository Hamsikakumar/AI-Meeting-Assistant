import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div style={{ maxWidth: 600, margin: "80px auto" }}>
      <h2>Welcome, {user?.name}!</h2>
      <p>Email: {user?.email}</p>
      <p>This is your meeting dashboard. (We'll build this out in later phases.)</p>
      <button onClick={handleLogout} style={{ padding: 10 }}>
        Log Out
      </button>
    </div>
  );
}