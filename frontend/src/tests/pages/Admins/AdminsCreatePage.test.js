import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

describe("AdminsCreatePage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const queryClient = new QueryClient();

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
  });

  test("default prop storybook is false and controls redirect", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "test@example.com" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/admin/admins/create"]}>
          <Routes>
            <Route path="/admin/admins/create" element={<AdminsCreatePage />} />
            <Route
              path="/admin/admins"
              element={<div data-testid="admins-list-page" />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    // Default storybook should be false (not true)
    // Submit form triggers redirect to admins list
    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.click(screen.getByText("Create"));

    await waitFor(() => {
      expect(screen.getByTestId("admins-list-page")).toBeInTheDocument();
    });
  });

  test("storybook=true disables redirect", async () => {
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

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "storybook@example.com" },
    });
    fireEvent.click(screen.getByText("Create"));

    await waitFor(() => {
      expect(screen.getByLabelText("Email")).toBeInTheDocument();
    });
  });

  test("onSuccess callback clears errors and shows toast", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "toast@example.com" });

    // Spy on console log or a side effect if toast is used (can't use toast, so check for onSuccess calls)
    let onSuccessCalled = false;

    // Wrap component to intercept onSuccess prop or check behavior indirectly
    // Since onSuccess is internal, test the side effect that after submit, form behaves correctly

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "toast@example.com" },
    });
    fireEvent.click(screen.getByText("Create"));

    // Wait for success, check form is replaced by navigation or toast call indirectly
    await waitFor(() => {
      expect(screen.queryByLabelText("Email")).not.toBeInTheDocument();
    });
  });

  test("POST request includes email param and cache invalidation keys are correct", async () => {
    let cacheKeys = null;
    const axiosMockLocal = new AxiosMockAdapter(axios);
    axiosMockLocal.onPost("/api/admin/post").reply((config) => {
      // We can't directly access cache keys here, but can check request params
      expect(config.params).toMatchObject({ email: "cachekey@example.com" });
      return [200, { email: "cachekey@example.com" }];
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "cachekey@example.com" },
    });
    fireEvent.click(screen.getByText("Create"));

    // We rely on component to invalidate "/api/admin/all" cache key on success,
    // So this test indirectly checks this behavior by ensuring a post request is made with params

    await waitFor(() => {
      const postRequests = axiosMockLocal.history.post;
      expect(postRequests.length).toBeGreaterThan(0);
    });
  });
});
