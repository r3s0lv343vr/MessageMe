(() => {
  const EMAIL_PATTERN =
    /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;

  const form = document.getElementById("waitlist-form");
  if (!form) return;

  const fields = {
    firstName: form.querySelector("#first-name"),
    lastName: form.querySelector("#last-name"),
    email: form.querySelector("#email")
  };
  const submit = form.querySelector("button[type='submit']");
  const status = form.querySelector("[data-form-status]");

  function trim(value) {
    return (value || "").trim();
  }

  function validate() {
    const firstName = trim(fields.firstName.value);
    const lastName = trim(fields.lastName.value);
    const email = trim(fields.email.value);
    const errors = {};

    if (!firstName) errors.firstName = "Enter your first name.";
    if (!lastName) errors.lastName = "Enter your last name.";
    if (!email || email.length > 254 || email.includes(" ") || !EMAIL_PATTERN.test(email)) {
      errors.email = "Enter a valid email.";
    }
    return { errors, value: { firstName, lastName, email } };
  }

  function showErrors(errors) {
    form.querySelectorAll("[data-error-for]").forEach((el) => {
      const key = el.getAttribute("data-error-for");
      const message = errors[key] || "";
      el.textContent = message;
      const input = fields[key];
      if (input) {
        input.setAttribute("aria-invalid", message ? "true" : "false");
      }
    });
  }

  function setStatus(message, kind) {
    status.textContent = message;
    status.dataset.kind = kind || "";
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const { errors, value } = validate();
    showErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatus("Please fix the highlighted fields.", "error");
      const firstError = errors.firstName
        ? fields.firstName
        : errors.lastName
          ? fields.lastName
          : fields.email;
      firstError.focus();
      return;
    }

    submit.disabled = true;
    setStatus("Sending…", "");

    try {
      const response = await fetch("/api/waitlist", {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json" },
        body: JSON.stringify({
          firstName: value.firstName,
          lastName: value.lastName,
          email: value.email,
          website: form.querySelector("#website").value
        })
      });
      const data = await response.json().catch(() => ({}));
      if (response.status === 404) {
        throw new Error("Waitlist signup is live after this page is deployed.");
      }
      if (!response.ok || !data.ok) {
        if (data.errors) showErrors(data.errors);
        throw new Error(data.error || "Could not save your details.");
      }
      form.reset();
      showErrors({});
      setStatus("You're in. Open the Android app, enter the same details if asked, and write a letter. No password.", "success");
    } catch (error) {
      setStatus(error.message || "Could not save your details. Please try again.", "error");
    } finally {
      submit.disabled = false;
    }
  });

  document.documentElement.classList.add("ready");
})();
