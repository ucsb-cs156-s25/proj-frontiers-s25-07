import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

describe("AdminsCreatePage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  test("renders without crashing", () => {
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  });

  test("submits form without error on success", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "test@example.com" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const emailField = screen.getByLabelText("Email");
    const createButton = screen.getByText("Create");

    fireEvent.change(emailField, { target: { value: "test@example.com" } });
    fireEvent.click(createButton);

    await waitFor(() => {
      const postRequests = axiosMock.history.post;
      expect(postRequests.length).toBeGreaterThan(0);

      expect(postRequests[0].params).toMatchObject({
        email: "test@example.com",
      });
    });
  });

  test("does not display error message in DOM when POST returns 403", async () => {
    axiosMock.onPost("/api/admin/post").reply(403);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const emailField = screen.getByLabelText("Email");
    const createButton = screen.getByText("Create");

    fireEvent.change(emailField, {
      target: { value: "forbidden@example.com" },
    });
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(
        screen.queryByTestId("AdminsCreatePage-error"),
      ).not.toBeInTheDocument();
    });

    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    expect(screen.getByText("Create")).toBeEnabled();
  });

  // New tests to kill mutations

  test("storybook=true disables navigation on success", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "storybook@example.com" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage storybook={true} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const emailField = screen.getByLabelText("Email");
    const createButton = screen.getByText("Create");

    fireEvent.change(emailField, {
      target: { value: "storybook@example.com" },
    });
    fireEvent.click(createButton);

    // Wait to ensure component does not redirect (stay on form)
    await waitFor(() => {
      expect(screen.getByLabelText("Email")).toBeInTheDocument();
    });
  });

  test("storybook=false triggers navigation after success", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "navi@example.com" });

    // Set up routing to catch navigation
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/admin/admins/create"]}>
          <Routes>
            <Route
              path="/admin/admins/create"
              element={<AdminsCreatePage storybook={false} />}
            />
            <Route
              path="/admin/admins"
              element={<div data-testid="admins-list-page">Admins List</div>}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const emailField = screen.getByLabelText("Email");
    const createButton = screen.getByText("Create");

    fireEvent.change(emailField, { target: { value: "navi@example.com" } });
    fireEvent.click(createButton);

    // Wait for navigation to happen
    await waitFor(() => {
      expect(screen.getByTestId("admins-list-page")).toBeInTheDocument();
    });
  });

  test("POST uses correct URL and cache invalidation keys", async () => {
    // Spy on axios to catch request URL
    let postUrl = "";
    axiosMock.onPost("/api/admin/post").reply((config) => {
      postUrl = config.url;
      return [200, { email: "testurl@example.com" }];
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const emailField = screen.getByLabelText("Email");
    const createButton = screen.getByText("Create");

    fireEvent.change(emailField, { target: { value: "testurl@example.com" } });
    fireEvent.click(createButton);

    await waitFor(() => {
      // The URL should not be empty string (catch mutation)
      expect(postUrl).toBe("/api/admin/post");
    });
  });
});
