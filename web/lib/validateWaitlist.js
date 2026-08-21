const NAME_MAX = 80;
const EMAIL_MAX = 254;

const EMAIL_PATTERN =
  /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;

function trimField(value) {
  return typeof value === "string" ? value.trim() : "";
}

function isValidName(value) {
  const name = trimField(value);
  return name.length >= 1 && name.length <= NAME_MAX;
}

function isValidEmail(value) {
  const email = trimField(value);
  if (email.length < 3 || email.length > EMAIL_MAX) return false;
  if (email.includes(" ")) return false;
  return EMAIL_PATTERN.test(email);
}

function validateWaitlist(input) {
  const firstName = trimField(input && input.firstName);
  const lastName = trimField(input && input.lastName);
  const email = trimField(input && input.email);
  const errors = {};

  if (!isValidName(firstName)) {
    errors.firstName = "Enter your first name.";
  }
  if (!isValidName(lastName)) {
    errors.lastName = "Enter your last name.";
  }
  if (!isValidEmail(email)) {
    errors.email = "Enter a valid email.";
  }

  if (Object.keys(errors).length > 0) {
    return { ok: false, errors };
  }

  return {
    ok: true,
    value: { firstName, lastName, email: email.toLowerCase() }
  };
}

module.exports = {
  NAME_MAX,
  EMAIL_MAX,
  isValidEmail,
  isValidName,
  validateWaitlist
};
