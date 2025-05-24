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
    render(
      <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />,
    );

    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.getByText("admin@example.com")).toBeInTheDocument();
    expect(screen.getByText("user@example.org")).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    expect(deleteButtons.length).toBe(testData.length);
  });

  test("calls deleteCallback when Delete button clicked", () => {
    render(
      <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />,
    );

    const buttons = screen.getAllByRole("button");
    expect(buttons.length).toBe(testData.length);

    fireEvent.click(buttons[0]);

    expect(mockDeleteCallback).toHaveBeenCalledTimes(1);
  });

  test("renders Delete button with correct label", () => {
    render(
      <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />,
    );
    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    expect(deleteButtons.length).toBe(testData.length);
    deleteButtons.forEach((btn) => {
      expect(btn).toHaveTextContent(/delete/i);
    });
  });

  test("renders table with correct testid", () => {
    render(
      <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />,
    );
    expect(
      screen.getByTestId("RoleEmailTable-header-email"),
    ).toBeInTheDocument();
  });

  test("Delete button has correct Bootstrap class", () => {
    render(
      <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />,
    );
    const deleteButtons = screen.getAllByRole("button", { name: /delete/i });
    deleteButtons.forEach((btn) => {
      expect(btn).toHaveClass("btn-danger");
    });
  });

  test("Delete button has correct data-testid using table ID prefix", () => {
    render(
      <RoleEmailTable data={testData} deleteCallback={mockDeleteCallback} />,
    );
    const deleteButton = screen.getByTestId(
      "RoleEmailTable-cell-row-0-col-Delete-button",
    );
    expect(deleteButton).toBeInTheDocument();
  });
});
