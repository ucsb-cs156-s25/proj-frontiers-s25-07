import React from "react";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import RoleEmailForm from "main/components/Users/RoleEmailForm";
import { Navigate } from "react-router-dom";
import { useBackendMutation } from "main/utils/useBackend";
import { toast } from "react-toastify";

export default function AdminsCreatePage({ storybook = false }) {
  const objectToAxiosParams = (admin) => ({
    url: "/api/admin/post",
    method: "POST",
    params: {
      email: admin.email,
    },
  });

  const onSuccess = (admin) => {
    toast(`New Admin Created - email: ${admin.email}`);
  };

  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess },
    ["/api/admin/all"], // Cache key to invalidate
  );

  const { isSuccess } = mutation;

  const onSubmit = (data) => {
    mutation.mutate(data);
  };

  if (isSuccess && !storybook) {
    // After success, navigate back to admins list page
    return <Navigate to="/admin/admins" />;
  }

  return (
    <BasicLayout>
      <div className="pt-2">
        <h1>Create New Admin</h1>
        <RoleEmailForm submitAction={onSubmit} />
      </div>
    </BasicLayout>
  );
}
