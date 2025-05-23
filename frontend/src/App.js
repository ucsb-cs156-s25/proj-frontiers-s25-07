import { BrowserRouter, Routes, Route } from "react-router-dom";
import HomePage from "main/pages/HomePage";
import ProfilePage from "main/pages/ProfilePage";
import AdminUsersPage from "main/pages/AdminUsersPage";

import { hasRole, useCurrentUser } from "main/utils/currentUser";

import "bootstrap/dist/css/bootstrap.css";
import "react-toastify/dist/ReactToastify.css";
import CoursesIndexPage from "main/pages/Courses/CoursesIndexPage";
import AdminsIndexPage from "main/pages/Admins/AdminsIndexPage";

function App() {
  const { data: currentUser } = useCurrentUser();

  return (
    <BrowserRouter>
      <Routes>
        <Route exact path="/" element={<HomePage />} />
        <Route exact path="/profile" element={<ProfilePage />} />
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <Route exact path="/admin/users" element={<AdminUsersPage />} />
        )}
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <Route exact path="/admin/courses" element={<CoursesIndexPage />} />
        )}
        {hasRole(currentUser, "ROLE_ADMIN") && (
          <Route exact path="/admin/admins" element={<AdminsIndexPage />} />
        )}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
