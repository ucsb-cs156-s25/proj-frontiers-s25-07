import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import RoleEmailTable from "main/components/Users/RoleEmailTable"; 

const testData = [
  { email: "admin@example.com" },
  { email: "user@example.org" },
];

const mockDeleteCallback = jest.fn();

describe("RoleEmailTable", () => {
  beforeEach(() => {
    mockDeleteCallback.mockClear();
  });

  test("renders table with email data", () => {
    render(<RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />);

    expect(screen.getByText("admin@example.com")).toBeInTheDocument();
    expect(screen.getByText("user@example.org")).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    expect(deleteButtons.length).toBe(testData.length);
  });

  test("calls deleteCallback when Delete button clicked", () => {
    render(<RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />);

    const firstDeleteButton = screen.getAllByRole("button", { name: /delete/i })[0];
    fireEvent.click(firstDeleteButton);

    expect(mockDeleteCallback).toHaveBeenCalledTimes(1);
  });
});
