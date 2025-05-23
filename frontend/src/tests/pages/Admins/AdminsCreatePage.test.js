import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminsCreatePage from "main/pages/Admins/AdminsCreatePage";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router-dom";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";
import { ToastContainer } from "react-toastify";
import * as toast from "react-toastify";

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
      </QueryClientProvider>
    );
  });

  test("submits form without error on success", async () => {
    axiosMock.onPost("/api/admin/post").reply(200, {});

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    const emailField = screen.getByLabelText("Email");
    const createButton = screen.getByText("Create");

    fireEvent.change(emailField, { target: { value: "test@example.com" } });
    fireEvent.click(createButton);

    await waitFor(() => {
      const postRequests = axiosMock.history.post;
      expect(postRequests.length).toBeGreaterThan(0);

      // Check that the email param was sent as a query param, not in data
      expect(postRequests[0].params).toMatchObject({ email: "test@example.com" });
    });
  });
  

  test("does not display error message in DOM when POST returns 403", async () => {
    axiosMock.onPost("/api/admin/post").reply(403);
  
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminsCreatePage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  
    const emailField = screen.getByLabelText("Email");
    const createButton = screen.getByText("Create");
  
    fireEvent.change(emailField, { target: { value: "forbidden@example.com" } });
    fireEvent.click(createButton);
  
    // Wait a short time for async to settle
    await waitFor(() => {
      // Check that no error element is shown in the page
      expect(screen.queryByTestId("AdminsCreatePage-error")).not.toBeInTheDocument();
    });
  
    // Optionally confirm the form is still there and enabled
    expect(screen.getByLabelText("Email")).toBeInTheDocument();
    expect(screen.getByText("Create")).toBeEnabled();
  });  
});
