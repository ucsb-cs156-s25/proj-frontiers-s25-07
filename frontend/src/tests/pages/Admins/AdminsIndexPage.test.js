import { render, screen, waitFor } from "@testing-library/react";
import AdminsIndexPage from "main/pages/Admins/AdminsIndexPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import mockConsole from "jest-mock-console";
import adminsFixtures from "fixtures/adminsFixtures";

import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

const queryClient = new QueryClient();

const mockToast = jest.fn();
jest.mock("react-toastify", () => {
  const originalModule = jest.requireActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: (x) => mockToast(x),
  };
});

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

  test("Renders for admin user", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/all").reply(200, []);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Admins/)).toBeInTheDocument();
    });
  });

  test("renders three admins correctly for admin user", async () => {
    setupAdminUser();
    axiosMock.onGet("/api/admin/all").reply(200, adminsFixtures.threeAdmins);

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
      ).toHaveTextContent("acdamstedt@ucsb.edu");
    });
    expect(
      screen.getByTestId(`${testId}-cell-row-1-col-email`),
    ).toHaveTextContent("acdamstedt@gmail.com");
    expect(
      screen.getByTestId(`${testId}-cell-row-2-col-email`),
    ).toHaveTextContent("testing@test.com");
  });

  test("renders empty table when backend unavailable, admin only", async () => {
    setupAdminUser();

    axiosMock.onGet("/api/admin/all").timeout();

    const restoreConsole = mockConsole();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await waitFor(() => {
      expect(axiosMock.history.get.length).toBeGreaterThanOrEqual(1);
    });

    const errorMessage = console.error.mock.calls[0][0];
    expect(errorMessage).toMatch(
      "Error communicating with backend via GET on /api/admin/all",
    );
    restoreConsole();
  });
});
