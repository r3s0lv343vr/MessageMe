const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const { isValidEmail, validateWaitlist } = require("./validateWaitlist");

describe("isValidEmail", () => {
  it("accepts ordinary addresses", () => {
    assert.equal(isValidEmail("craig@example.com"), true);
    assert.equal(isValidEmail("  a.b+tag@sub.mail.co.uk  "), true);
  });

  it("rejects missing or incomplete addresses", () => {
    assert.equal(isValidEmail(""), false);
    assert.equal(isValidEmail("not-an-email"), false);
    assert.equal(isValidEmail("foo@"), false);
    assert.equal(isValidEmail("@bar.com"), false);
    assert.equal(isValidEmail("foo@bar"), false);
    assert.equal(isValidEmail("foo @bar.com"), false);
  });
});

describe("validateWaitlist", () => {
  it("requires first name, last name, and a valid email", () => {
    const result = validateWaitlist({
      firstName: " ",
      lastName: "",
      email: "hello"
    });
    assert.equal(result.ok, false);
    assert.equal(result.errors.firstName, "Enter your first name.");
    assert.equal(result.errors.lastName, "Enter your last name.");
    assert.equal(result.errors.email, "Enter a valid email.");
  });

  it("trims names and lowercases email on success", () => {
    const result = validateWaitlist({
      firstName: "  Craig ",
      lastName: "Ferguson",
      email: "Craig.J@Example.COM"
    });
    assert.equal(result.ok, true);
    assert.deepEqual(result.value, {
      firstName: "Craig",
      lastName: "Ferguson",
      email: "craig.j@example.com"
    });
  });
});
