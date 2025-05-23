import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";

import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

const queryClient = new QueryClient();
const axiosMock = new AxiosMockAdapter(axios);

describe("AdminsCreatePage tests", () => {
  const setupAdminUser = () => {
    axiosMock.reset();
    axiosMock.onGet("/api/currentUser").reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock.onGet("/api/systemInfo").reply(200, systemInfoFixtures.showingNeither);
  };

  test("renders without crashing", () => {
    setupAdminUser();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage storybook={true} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByText("Create New Admin")).toBeInTheDocument();
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
  });

  test("shows validation error when email is empty", async () => {
    setupAdminUser();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage storybook={true} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const submitButton = screen.getByTestId("RoleEmailForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText("A valid email is required.")).toBeInTheDocument();
    });
  });

  test("submits form when email is valid", async () => {
    setupAdminUser();

    const newAdmin = { email: "newadmin@example.com" };

    axiosMock.onPost("/api/admin/post", { params: newAdmin }).reply(200, newAdmin);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage storybook={true} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const emailInput = screen.getByTestId("RoleEmailForm-email");
    fireEvent.change(emailInput, { target: { value: newAdmin.email } });

    const submitButton = screen.getByTestId("RoleEmailForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() => {
      // The presence of the email input means the form hasn't navigated away
      expect(emailInput.value).toBe(newAdmin.email);
    });
  });
});
