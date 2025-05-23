import React from "react";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";
import { QueryClient, QueryClientProvider } from "react-query";

const queryClient = new QueryClient();

export default {
  title: "pages/Admins/AdminsCreatePage",
  component: AdminsCreatePage,
};

// ✅ DO NOT wrap in <MemoryRouter> if the component already assumes routing context
export const Default = () => (
  <QueryClientProvider client={queryClient}>
    <AdminsCreatePage storybook={true} />
  </QueryClientProvider>
);
