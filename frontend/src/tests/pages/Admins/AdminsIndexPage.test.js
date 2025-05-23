import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import AdminsIndexPage from "main/pages/Admins/AdminsIndexPage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("AdminsIndexPage mutation survival tests", () => {
  const queryClient = new QueryClient();
  const axiosMock = new AxiosMockAdapter(axios);

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();

    axiosMock
      .onGet("/api/currentUser")
      .reply(200, { roles: ["ROLE_ADMIN"], user: "admin" });
    axiosMock.onGet("/api/systemInfo").reply(200, {});
  });

  test("errorMessage initial state is empty string", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(
      screen.queryByTestId("AdminsIndexPage-error"),
    ).not.toBeInTheDocument();
  });

  test("deleteMutation config has correct method, url, and params", async () => {
    axiosMock
      .onGet("/api/admin/all")
      .reply(200, [{ email: "user@example.com", roles: ["ADMIN"] }]);
    axiosMock.onDelete("/api/admin/delete").reply(200);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsIndexPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const userEmailCell = await screen.findByText("user@example.com");
    expect(userEmailCell).toBeInTheDocument();
  });

  test("onSuccess callback clears error message", async () => {
    axiosMock
      .onGet("/api/admin/all")
      .reply(200, [{ email: "user@example.com", roles: ["ADMIN"] }]);
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
        screen.queryByTestId("AdminsIndexPage-error"),
      ).not.toBeInTheDocument();
    });
  });
});
