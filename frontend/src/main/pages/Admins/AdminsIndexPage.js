import React, { useState } from "react";  // <-- import useState here
import { useBackend } from "main/utils/useBackend";
import { useBackendMutation } from "main/utils/useBackend";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailTable from "main/components/Users/RoleEmailTable";

import { Button } from "react-bootstrap";
import { Link } from "react-router-dom";

// Accept optional admins prop for Storybook or testing
export default function AdminsIndexPage({ admins: adminsFromProps }) {
  // Declare errorMessage state
  const [errorMessage, setErrorMessage] = useState("");

  const {
    data: adminsFromBackend,
    error: _error,
    status: _status,
  } = useBackend(
    ["/api/admin/all"],
    { method: "GET", url: "/api/admin/all" },
    [],
  );

  const deleteMutation = useBackendMutation(
    (cell) => ({
      url: "/api/admin/delete",
      method: "DELETE",
      params: { email: cell.row.original.email },
    }),
    {
      onSuccess: () => {
        setErrorMessage(""); // clear error on success
      },
      onError: (error) => {
        if (error.response?.status === 403) {
          setErrorMessage("You do not have permission to delete this admin.");
        } else {
          setErrorMessage("An unexpected error occurred. Please try again.");
        }
      },
    },
    ["/api/admin/all"]
  );

  // Callback function passed to the table
  const deleteCallback = (cell) => {
    deleteMutation.mutate(cell);
  };

  // Use props admins if provided, else fallback to backend data
  const admins = adminsFromProps || adminsFromBackend;

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Admins</h1>

        {errorMessage && (
          <div className="alert alert-danger" role="alert" data-testid="AdminsIndexPage-error">
            {errorMessage}
          </div>
        )}

        <Button
          variant="primary"
          as={Link}
          to="/admin/admins/create"
          className="mb-3"
          data-testid="AdminsIndexPage-new-admin-button"
        >
          New Admin
        </Button>
        <RoleEmailTable data={admins} deleteCallback={deleteCallback} />
      </div>
    </BasicLayout>
  );
}
