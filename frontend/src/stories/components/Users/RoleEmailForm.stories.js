import React from "react";
import RoleEmailForm from "main/components/Users/RoleEmailForm";

export default {
  title: "components/Users/RoleEmailForm",
  component: RoleEmailForm,
};

const Template = (args) => {
  return <RoleEmailForm {...args} />;
};

export const Create = Template.bind({});

Create.args = {
  buttonLabel: "Create",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
