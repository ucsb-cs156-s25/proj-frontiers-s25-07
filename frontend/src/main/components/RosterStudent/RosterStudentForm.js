import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom";

function RosterStudentForm({
  initialContents,
  submitAction,
  buttonLabel = "Create",
}) {
  // Stryker disable all
  const {
    register,
    formState: { errors },
    handleSubmit,
  } = useForm({ defaultValues: initialContents || {} });
  // Stryker restore all

  const navigate = useNavigate();

  const testIdPrefix = "RosterStudentForm";

  return (
    <Form onSubmit={handleSubmit(submitAction)}>
      <Form.Group className="mb-3">
        <Form.Label htmlFor="studentId">Student Id</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-studentId"}
          id="studentId"
          type="text"
          isInvalid={Boolean(errors.studentId)}
          {...register("studentId", {
            required: "Student Id is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.studentId?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label htmlFor="firstName">First Name</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-firstName"}
          id="firstName"
          type="text"
          isInvalid={Boolean(errors.firstName)}
          {...register("firstName", {
            required: "First Name is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.firstName?.message}
        </Form.Control.Feedback>
      </Form.Group>

      <Form.Group className="mb-3">
        <Form.Label htmlFor="lastName">Last Name</Form.Label>
        <Form.Control
          data-testid={testIdPrefix + "-lastName"}
          id="lastName"
          type="text"
          isInvalid={Boolean(errors.lastName)}
          {...register("lastName", {
            required: "Last Name is required.",
          })}
        />
        <Form.Control.Feedback type="invalid">
          {errors.lastName?.message}
        </Form.Control.Feedback>
      </Form.Group>

      {!initialContents && (
        <Form.Group className="mb-3">
          <Form.Label htmlFor="email">Email</Form.Label>
          <Form.Control
            id="email"
            type="text"
            isInvalid={Boolean(errors.email)}
            {...register("email", {
              required: "Email is required.",
            })}
          />
          <Form.Control.Feedback type="invalid">
            {errors.email?.message}
          </Form.Control.Feedback>
        </Form.Group>
      )}

      {initialContents && (
        <Form.Group className="mb-3">
          <Form.Label htmlFor="email">Email</Form.Label>
          <Form.Control
            data-testid={testIdPrefix + "-email"}
            id="email"
            type="text"
            {...register("email")}
            value={initialContents.email}
            disabled
          />
        </Form.Group>
      )}

      <Button type="submit" data-testid={testIdPrefix + "-submit"}>
        {buttonLabel}
      </Button>
      <Button
        variant="Secondary"
        onClick={() => navigate(-1)}
        data-testid={testIdPrefix + "-cancel"}
      >
        Cancel
      </Button>
    </Form>
  );
}

export default RosterStudentForm;
