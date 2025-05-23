import React from "react";
import { useBackend } from "main/utils/useBackend";

import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailTable from "main/components/Users/RoleEmailTable";

import { Button } from "react-bootstrap";
import { Link } from "react-router-dom";

// Accept optional admins prop for Storybook or testing
export default function AdminsIndexPage({ admins: adminsFromProps }) {
  const {
    data: adminsFromBackend,
    error: _error,
    status: _status,
  } = useBackend(
    ["/api/admins/all"],
    { method: "GET", url: "/api/admins/all" },
    [],
  );

  // Use props admins if provided, else fallback to backend data
  const admins = adminsFromProps || adminsFromBackend;

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Admins</h1>
        <Button
          variant="primary"
          as={Link}
          to="/admin/admins/create"
          className="mb-3"
          data-testid="AdminsIndexPage-new-admin-button"
        >
          New Admin
        </Button>
        <RoleEmailTable data={admins} />
      </div>
    </BasicLayout>
  );
}
