# Security Policy

## Supported Versions

AutoCat is a fork of Lawnchair 15. The latest version of AutoCat is the only supported version.

| Version | Supported |
| ------- | ------------------ |
| dev-latest (100+) | :white_check_mark: |
| 15.0.b1-autocat.* | :white_check_mark: |
| Older builds | :x: |

**Note**: Lawnchair Legacy is **unsupported**. See [Lawnchair FAQ](https://lawnchair.app/faq/#what-happened-to-lawnchair-legacy) for details.

---

## Reporting Security Issues

We take security seriously and appreciate your efforts to responsibly disclose your findings.

### How to Report

**Option 1: GitHub Security Advisory** (Preferred)

File a [security advisory](https://github.com/thejaustin/AutoCat/security/advisories/new) on GitHub.

**Option 2: Contact Developer**

Contact a developer on [Telegram](https://t.me/lccommunity) or [Discord](https://discord.com/invite/3x8qNWxgGZ) with a message starting with **"SECURITY"**.

### What to Include

When reporting a security issue, please include:

1. **Description**: Clear description of the vulnerability
2. **Steps to reproduce**: Detailed steps to reproduce the issue
3. **Impact**: Potential impact and severity
4. **Affected versions**: Which AutoCat versions are affected
5. **Proposed fix** (optional): Suggestions for remediation

### Response Time

We will:
- Acknowledge your report within **72 hours**
- Provide an initial assessment within **1 week**
- Keep you updated throughout the process
- Credit you in the fix (unless you prefer anonymity)

---

## Security Practices

### AutoCat-Specific Security

#### LLM API Key Protection

**Problem**: API keys stored in app settings could be exposed in backups

**Mitigation**:
- Support for environment variables (more secure)
- API keys not logged in debug output
- Keys sanitized in crash reports
- Recommendation to use environment variables in documentation

#### Prompt Injection Protection

**Problem**: Malicious apps could inject commands via app metadata ([#11](https://github.com/thejaustin/AutoCat/issues/11))

**Mitigation** (Fixed in build ~80):
- Input sanitization for all app metadata
- Escaped special characters in prompts
- LLM response validation before database insertion
- Structured output format (JSON)

#### SQL Injection Prevention

**Protection**:
- Room database with parameterized queries
- No raw SQL with user input
- Input validation on all database operations

#### Network Security

**Protection**:
- HTTPS-only connections to LLM APIs
- Certificate pinning (future enhancement)
- Timeout configuration ([#10](https://github.com/thejaustin/AutoCat/issues/10))
- OkHttpClient with secure defaults

---

## Known Security Issues

### Fixed Vulnerabilities

#### CVE-AUTOCAT-2025-001: Prompt Injection (Fixed)

**Severity**: Medium

**Affected**: Builds < 80

**Description**: Malicious apps could inject commands into LLM prompts via crafted app names or descriptions.

**Fix**: [Commit 06e6cdb](https://github.com/thejaustin/AutoCat/commit/06e6cdcb29)
- Input sanitization
- Escaped special characters
- Response validation

**Status**: ✅ Fixed in build 80+

#### CVE-AUTOCAT-2025-002: Socket Timeout Missing (Fixed)

**Severity**: Medium

**Affected**: Builds < 80

**Description**: Missing socket timeout could cause hanging connections and DoS.

**Fix**: [Issue #10](https://github.com/thejaustin/AutoCat/issues/10)
- Configured connection timeouts
- Read/write timeouts
- Proper error handling

**Status**: ✅ Fixed in build 80+

---

## Security Best Practices for Users

### API Key Management

**Do**:
- ✅ Use environment variables instead of storing in settings
- ✅ Rotate API keys every 90 days
- ✅ Set spending limits on LLM provider accounts
- ✅ Monitor API usage regularly
- ✅ Use separate API keys for different apps

**Don't**:
- ❌ Share API keys with others
- ❌ Commit API keys to git repositories
- ❌ Include API keys in screenshots or bug reports
- ❌ Store API keys in plain text files

### App Permissions

AutoCat requires these permissions:

- **Internet**: Required for LLM API calls
- **Storage**: Required for database and preferences
- **Query All Packages**: Required to list installed apps (launcher requirement)

**Note**: All permissions are standard for Android launchers.

### Data Privacy

**What AutoCat stores locally**:
- App categorizations (package names + categories)
- User corrections and overrides
- LLM provider accuracy metrics
- Custom category definitions

**What AutoCat sends to LLM providers**:
- App package names (e.g., `com.android.chrome`)
- App display names (e.g., "Chrome")
- App descriptions (if available)

**What AutoCat does NOT send**:
- Personal information
- App usage data
- Contact information
- File contents
- Location data

---

## Upstream Security

### Lawnchair Security

AutoCat inherits security practices from upstream Lawnchair:

- Code review process
- Regular security updates
- Community auditing
- Responsible disclosure policy

See [Lawnchair Security Policy](https://github.com/LawnchairLauncher/lawnchair/security/policy) for upstream security details.

### Dependency Security

AutoCat uses:
- **Room Database**: Google's secure SQLite wrapper
- **OkHttpClient**: Secure HTTP client with modern TLS
- **Kotlin Coroutines**: Memory-safe concurrency
- **Jetpack Compose**: Modern UI framework

All dependencies are kept up-to-date with security patches.

---

## Vulnerability Disclosure Timeline

### Standard Process

1. **Report received**: Within 24 hours
2. **Initial assessment**: Within 1 week
3. **Fix development**: 1-4 weeks (depending on severity)
4. **Testing**: 1 week
5. **Release**: Immediate (critical), next release (medium/low)
6. **Public disclosure**: After fix released + 90 days

### Critical Vulnerabilities

Critical vulnerabilities are handled with urgency:

1. **Immediate response**: Within 24 hours
2. **Hotfix development**: 24-72 hours
3. **Emergency release**: Immediate
4. **User notification**: Via GitHub + documentation
5. **Public disclosure**: After fix released + 30 days

---

## Security Audits

### Self-Audits

Regular security reviews of:
- API key handling
- Database queries
- Network requests
- Input validation
- Permission usage

### Community Audits

We welcome security researchers to audit AutoCat:

- Full source code available on GitHub
- Open to responsible disclosure
- Credit given for valid findings
- Bounty program (future consideration)

---

## Compliance

### Data Protection

AutoCat complies with:

- **GDPR**: No personal data collected or transmitted
- **CCPA**: No sale of user data
- **Android Privacy Guidelines**: Minimal permissions, transparent data usage

### Open Source Licenses

AutoCat respects:
- **Lawnchair License**: Apache 2.0
- **Dependency Licenses**: All dependencies properly attributed
- **Contributor Agreements**: CLA not required (open contribution)

---

## Security Roadmap

### Planned Enhancements

- [ ] Certificate pinning for LLM API connections
- [ ] Encrypted local storage for API keys
- [ ] Rate limiting for API calls (prevent abuse)
- [ ] Audit logging for categorization operations
- [ ] Formal security audit by third party
- [ ] Bug bounty program

### Security Priorities

1. **API key protection** - Highest priority
2. **Input validation** - Ongoing effort
3. **Network security** - Regular updates
4. **Dependency updates** - Automated monitoring

---

## Contact

For security issues:
- **Email**: Create GitHub security advisory (no public email)
- **Telegram**: @lccommunity (start message with "SECURITY")
- **Discord**: Lawnchair server (start message with "SECURITY")

For non-security issues:
- **GitHub Issues**: https://github.com/thejaustin/AutoCat/issues

---

## Acknowledgments

We thank the security researchers and community members who have responsibly disclosed vulnerabilities:

- **Community Contributors**: For identifying prompt injection vulnerability
- **GitHub Security Lab**: For automated security scanning
- **Upstream Lawnchair Team**: For security best practices

---

*Last Updated: 2025-12-25*

*This policy is based on the [Lawnchair Security Policy](https://github.com/LawnchairLauncher/lawnchair/blob/15-dev/SECURITY.md).*
