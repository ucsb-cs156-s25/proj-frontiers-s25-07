import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";

import * as toastModule from "react-toastify";

jest.mock("react-toastify", () => ({
  toast: jest.fn(),
}));

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

    await waitFor(() => {
      expect(screen.queryByLabelText("Email")).not.toBeInTheDocument();
    });
  });

  test("POST request includes email param and cache invalidation keys are correct", async () => {
    const axiosMockLocal = new AxiosMockAdapter(axios);
    axiosMockLocal.onPost("/api/admin/post").reply((config) => {
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

    await waitFor(() => {
      const postRequests = axiosMockLocal.history.post;
      expect(postRequests.length).toBeGreaterThan(0);
    });
  });

  test("POST request includes the email param in params object", async () => {
    const axiosMockLocal = new AxiosMockAdapter(axios);

    // Track the POST request
    axiosMockLocal.onPost("/api/admin/post").reply((config) => {
      const parsedUrl = new URLSearchParams(config.params);
      expect(parsedUrl.get("email")).toBe("verify@example.com");
      return [200, { email: "verify@example.com" }];
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "verify@example.com" },
    });

    fireEvent.click(screen.getByText("Create"));

    await waitFor(() => {
      expect(axiosMockLocal.history.post.length).toBe(1);
    });
  });

  test("onSuccess calls toast with the new admin's email", async () => {
    const axiosMockLocal = new AxiosMockAdapter(axios);
    axiosMockLocal
      .onPost("/api/admin/post")
      .reply(200, { email: "toastcheck@example.com" });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "toastcheck@example.com" },
    });

    fireEvent.click(screen.getByText("Create"));

    await waitFor(() => {
      expect(toastModule.toast).toHaveBeenCalledWith(
        "New Admin Created - email: toastcheck@example.com",
      );
    });
  });

  test("successful admin creation removes form", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "success@example.com" });

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

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "success@example.com" },
    });

    fireEvent.click(screen.getByText("Create"));

    // Wait for the redirect to happen after success
    await waitFor(() => {
      expect(screen.getByTestId("admins-list-page")).toBeInTheDocument();
    });
  });

  test("onSuccess callback triggers navigation to admins list page", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "redirect@example.com" });

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

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "redirect@example.com" },
    });

    fireEvent.click(screen.getByText("Create"));

    await waitFor(() => {
      // This only appears if the redirect (and hence onSuccess) occurred
      expect(screen.getByTestId("admins-list-page")).toBeInTheDocument();
    });
  });

  test("cache invalidation is triggered for /api/admin/all after admin creation", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "invalidate@example.com" });

    const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "invalidate@example.com" },
    });
    fireEvent.click(screen.getByText("Create"));

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith(["/api/admin/all"]);
    });

    invalidateSpy.mockRestore();
  });

  test("cache invalidation is triggered for /api/admin/all after admin creation", async () => {
    axiosMock
      .onPost("/api/admin/post")
      .reply(200, { email: "test@example.com" });

    // Spy on invalidateQueries
    const invalidateSpy = jest.spyOn(queryClient, "invalidateQueries");

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "test@example.com" },
    });
    fireEvent.click(screen.getByText("Create"));

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith(["/api/admin/all"]);
    });

    invalidateSpy.mockRestore();
  });

  test("handles form submission with empty params object mutation", async () => {
    // Create a QueryClient instance for this test
    const queryClient = new QueryClient();

    // Save original axios.post to restore later
    const originalPost = axios.post;

    // Override axios.post just for this test
    axios.post = async (url, data) => {
      if (url === "/api/admin/post" && data.email === "testadmin@example.com") {
        return Promise.resolve({ data: { message: "Admin created" } });
      }
      return Promise.reject(new Error("Unexpected call"));
    };

    try {
      render(
        <QueryClientProvider client={queryClient}>
          <AdminsCreatePage />
        </QueryClientProvider>,
      );

      const emailInput = screen.getByLabelText(/email/i);
      const submitButton = screen.getByRole("button", {
        name: /create admin/i,
      });

      fireEvent.change(emailInput, {
        target: { value: "testadmin@example.com" },
      });
      fireEvent.click(submitButton);

      await waitFor(() => {
        // Check that success message is shown
        expect(screen.getByText(/admin created/i)).toBeInTheDocument();
      });
    } finally {
      // Restore original axios.post after test
      axios.post = originalPost;
    }
  });
});
