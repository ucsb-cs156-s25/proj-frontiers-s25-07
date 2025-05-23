import React from "react";
import AdminsIndexPage from "main/pages/Admins/AdminsIndexPage";
import { QueryClient, QueryClientProvider } from "react-query";
import adminsFixtures from "fixtures/adminsFixtures";

const queryClient = new QueryClient();

export default {
  title: "pages/Admins/AdminsIndexPage",
  component: AdminsIndexPage,
  decorators: [
    (Story) => (
      <QueryClientProvider client={queryClient}>
        {/* Removed MemoryRouter here */}
        <Story />
      </QueryClientProvider>
    ),
  ],
};

export const EmptyAdmins = () => <AdminsIndexPage admins={[]} />;

export const ThreeAdmins = () => (
  <AdminsIndexPage admins={adminsFixtures.threeAdmins} />
);
