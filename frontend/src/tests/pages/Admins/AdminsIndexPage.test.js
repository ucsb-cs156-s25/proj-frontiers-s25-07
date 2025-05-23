import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import AdminsIndexPage from "main/pages/Admins/AdminsIndexPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import adminsFixtures from "fixtures/adminsFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const queryClient = new QueryClient();

describe("AdminsIndexPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const testId = "RoleEmailTable";

  const setupAdminUser = () => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  };

  // Existing tests ...

  test("initially does NOT show error message alert", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/all").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => expect(screen.getByText(/Admins/)).toBeInTheDocument());

    expect(
      screen.queryByTestId("AdminsIndexPage-error"),
    ).not.toBeInTheDocument();
  });

  test("calls delete mutation and clears error on success", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/all").reply(200, adminsFixtures.threeAdmins);
    axiosMock.onDelete("/api/admin/delete").reply(200);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toBeInTheDocument();
    });

    // Find the first delete button (assuming accessible name includes 'delete')
    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    fireEvent.click(deleteButtons[0]);

    // Wait for delete axios call to finish
    await waitFor(() => {
      expect(axiosMock.history.delete.length).toBeGreaterThan(0);
    });

    // Confirm no error alert is shown
    expect(
      screen.queryByTestId("AdminsIndexPage-error"),
    ).not.toBeInTheDocument();
  });

  test("shows permission error message on 403 delete error", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/all").reply(200, adminsFixtures.threeAdmins);
    axiosMock.onDelete("/api/admin/delete").reply(403);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toBeInTheDocument();
    });

    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(screen.getByTestId("AdminsIndexPage-error")).toHaveTextContent(
        "You do not have permission to delete this admin.",
      );
    });
  });

  test("shows generic error message on other delete errors", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/all").reply(200, adminsFixtures.threeAdmins);
    axiosMock.onDelete("/api/admin/delete").reply(500);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId(`${testId}-cell-row-0-col-email`),
      ).toBeInTheDocument();
    });

    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(screen.getByTestId("AdminsIndexPage-error")).toHaveTextContent(
        "An unexpected error occurred. Please try again.",
      );
    });
  });
});
