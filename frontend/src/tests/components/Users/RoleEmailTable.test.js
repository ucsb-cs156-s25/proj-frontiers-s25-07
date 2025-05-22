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
  
    const buttons = screen.getAllByRole("button");
    expect(buttons.length).toBe(testData.length);  // one button per row?
  
    fireEvent.click(buttons[0]);
  
    expect(mockDeleteCallback).toHaveBeenCalledTimes(1);
  });
  
  test("renders Delete button with correct label", () => {
    render(<RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />);
    const deleteButton = screen.getAllByRole("button", { name: /delete/i })[0];
    expect(deleteButton).toBeInTheDocument();
  });

  test("OurTable has correct testid attribute", () => {
    const { container } = render(<RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />);
    expect(container.querySelector('[data-testid="RoleEmailTable"]')).toBeInTheDocument();
  });
});
