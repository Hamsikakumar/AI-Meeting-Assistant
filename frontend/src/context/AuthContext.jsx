import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const savedName = localStorage.getItem("userName");
    const savedEmail = localStorage.getItem("userEmail");
    const token = localStorage.getItem("token");
    return token ? { name: savedName, email: savedEmail } : null;
  });

  function login(authResponse) {
    localStorage.setItem("token", authResponse.token);
    localStorage.setItem("userName", authResponse.name);
    localStorage.setItem("userEmail", authResponse.email);
    setUser({ name: authResponse.name, email: authResponse.email });
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userName");
    localStorage.removeItem("userEmail");
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}