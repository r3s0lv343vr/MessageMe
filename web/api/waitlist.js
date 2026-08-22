const { validateWaitlist } = require("../lib/validateWaitlist");

const DEFAULT_NOTIFY_EMAIL = "ferguson.craig.j@gmail.com";

function readJsonBody(req) {
  if (req.body && typeof req.body === "object") return req.body;
  if (typeof req.body === "string" && req.body.length > 0) {
    return JSON.parse(req.body);
  }
  return {};
}

module.exports = async function handler(req, res) {
  res.setHeader("Content-Type", "application/json");

  if (req.method !== "POST") {
    res.status(405).json({ ok: false, error: "Method not allowed." });
    return;
  }

  let payload;
  try {
    payload = readJsonBody(req);
  } catch {
    res.status(400).json({ ok: false, error: "Invalid request." });
    return;
  }

  // Honeypot: bots fill hidden "website"; humans never see it.
  if (trimHoneypot(payload.website)) {
    res.status(200).json({ ok: true });
    return;
  }

  const result = validateWaitlist(payload);
  if (!result.ok) {
    res.status(400).json(result);
    return;
  }

  const notifyEmail = process.env.WAITLIST_NOTIFY_EMAIL || DEFAULT_NOTIFY_EMAIL;
  const forwarded = await forwardSignup(notifyEmail, result.value);
  if (!forwarded.ok) {
    res.status(502).json({
      ok: false,
      error: forwarded.error || "Could not save your details. Please try again."
    });
    return;
  }

  res.status(200).json({ ok: true });
};

function trimHoneypot(value) {
  return typeof value === "string" && value.trim().length > 0;
}

async function forwardSignup(notifyEmail, value) {
  const response = await fetch(`https://formsubmit.co/ajax/${encodeURIComponent(notifyEmail)}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json"
    },
    body: JSON.stringify({
      _subject: "MessageMe waitlist",
      _template: "table",
      _captcha: "false",
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email
    })
  });

  let data = {};
  try {
    data = await response.json();
  } catch {
    data = {};
  }

  const success =
    data.success === undefined || data.success === true || data.success === "true";
  if (!response.ok || !success) {
    return {
      ok: false,
      error: data.message || "Could not save your details. Please try again."
    };
  }
  return { ok: true };
}
