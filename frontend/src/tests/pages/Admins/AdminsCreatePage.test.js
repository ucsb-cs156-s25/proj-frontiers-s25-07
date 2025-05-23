import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "react-query";
import { describe, it, expect, vi } from "vitest";
import { toast } from "react-toastify";

// Mock toast
vi.mock("react-toastify", () => ({
  toast: vi.fn(),
}));

// Mock useBackendMutation
vi.mock("main/utils/useBackend", async () => {
  const actual = await vi.importActual("main/utils/useBackend");
  return {
    ...actual,
    useBackendMutation: () => ({
      mutate: vi.fn(),
      isSuccess: false,
    }),
  };
});

describe("AdminsCreatePage", () => {
  const queryClient = new QueryClient();

  it("renders without crashing", () => {
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

  it("shows validation error when email is empty", async () => {
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
      expect(
        screen.getByText("A valid email is required."),
      ).toBeInTheDocument();
    });
  });

  it("allows form submission when email is valid", async () => {
    const mockMutate = vi.fn();

    vi.mocked(require("main/utils/useBackend")).useBackendMutation = () => ({
      mutate: mockMutate,
      isSuccess: false,
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage storybook={true} />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    const emailInput = screen.getByTestId("RoleEmailForm-email");
    fireEvent.change(emailInput, { target: { value: "admin@example.com" } });

    const submitButton = screen.getByTestId("RoleEmailForm-submit");
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledWith({ email: "admin@example.com" });
    });
  });
});
