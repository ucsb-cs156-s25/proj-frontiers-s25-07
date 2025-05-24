import React from "react";
import RoleEmailTable from "main/components/Users/RoleEmailTable";

export default {
  title: "components/Users/RoleEmailTable",
  component: RoleEmailTable,
};

const sampleData = [
  { email: "admin@example.com" },
  { email: "user@example.org" },
];

const mockDeleteCallback = (cell) => {
  console.log(`Delete clicked for: ${cell.row.values.email}`);
};

const Template = (args) => <RoleEmailTable {...args} />;

export const Default = Template.bind({});
Default.args = {
  data: sampleData,
  deleteCallback: mockDeleteCallback,
};
