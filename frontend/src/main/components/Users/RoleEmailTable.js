import React from "react";
import OurTable from "main/components/OurTable";
import { ButtonColumn } from "main/components/OurTable";

export default function RoleEmailTable({ data, deleteCallback }) {
  const columns = [
    {
      Header: "Email",
      accessor: "email",
    },
    // The delete button column calls deleteCallback with the cell info when clicked
    ButtonColumn("Delete", "danger", deleteCallback, "RoleEmailTable"),
  ];

  return <OurTable data={data} columns={columns} testid={"RoleEmailTable"} />;
}
