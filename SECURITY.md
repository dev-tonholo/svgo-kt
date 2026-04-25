# Security Policy

## Supported versions

Only the most recent published release of `dev.tonholo:svgo-kt` receives
security fixes. Older versions track historical svgo upstream releases
and are not patched.

| Version (svgo-kt) | Status              |
| ----------------- | ------------------- |
| Latest tag        | :white_check_mark:  |
| Anything older    | :x:                 |

## Reporting a vulnerability

Please **do not open a public GitHub issue** for security reports.

Instead, use one of the following private channels:

- **Email:** rafael@tonholo.dev
- **GitHub Security Advisories:**
  https://github.com/rafaeltonholo/svgo-kt/security/advisories/new

Include in your report:

- A clear description of the issue and its impact.
- Steps to reproduce, ideally a minimal SVG / config that triggers the
  problem, plus the affected platform (JVM / JS / native).
- The svgo-kt version (or commit SHA) you observed it on.
- Any suggested fix or mitigation, if you have one.

You can expect:

- An acknowledgement within **3 business days**.
- An initial assessment (severity, scope, planned response) within
  **7 business days**.
- A coordinated disclosure timeline once a fix is in flight, usually no
  longer than 30 days from the acknowledgement for high-severity issues.

## Upstream svgo issues

svgo-kt is a behavior-faithful port of [svg/svgo](https://github.com/svg/svgo).
If a vulnerability exists in upstream svgo's optimization logic, we will
mirror upstream's response (timing, severity, fix scope) and attribute the
upstream advisory in our own. Please report behavioral / DoS issues that
reproduce in upstream svgo there first; we will track and import the fix.

## Scope

In scope:

- Code paths in `dev.tonholo:svgo-kt` (parser, plugin pipeline,
  stringifier, public DSL).
- Build configuration and CI pipelines under `.github/`.

Out of scope:

- Vulnerabilities in third-party dependencies that are already disclosed
  upstream -- please report those to the dependency itself.
- Issues that require physical access or compromised local environments.
- Denial-of-service attacks that require unrealistic input sizes (millions
  of nodes) without a specific complexity-amplification angle.
