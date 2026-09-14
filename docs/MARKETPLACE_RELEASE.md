# JetBrains Marketplace first release

The first Spring Config Guard publication is a manual Marketplace upload. The repository workflow
only creates a signed, signature-verified ZIP; it does not publish anything to JetBrains.

## 1. Create signing material locally

Use a trusted local machine. Never commit the generated files.

```bash
openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096
openssl rsa -in private_encrypted.pem -out private.pem
openssl req -key private.pem -new -x509 -days 365 -out chain.crt
```

Choose and retain a strong private-key password. Store the key and certificate in a controlled secret
store. Certificate renewal and key rotation are operator responsibilities.

## 2. Configure GitHub Actions secrets

Add these repository Actions secrets under **Settings → Secrets and variables → Actions**:

| Secret | Exact content |
|---|---|
| `CERTIFICATE_CHAIN` | Complete PEM text from `chain.crt` |
| `PRIVATE_KEY` | Complete PEM text from `private.pem` |
| `PRIVATE_KEY_PASSWORD` | Private-key password |

Do not add a Marketplace token for the first upload. The workflow does not accept or use one.

## 3. Build the signed ZIP

1. Confirm the exact commit intended for release has a successful normal CI run.
2. Open **Actions → Signed release artifact → Run workflow**.
3. Enter the full 40-character commit SHA in `git_ref`.
4. Wait for every workflow step, including `verifyPluginSignature`, to succeed.
5. Download `spring-config-guard-signed-<workflow SHA>` and extract the outer GitHub artifact archive.
   Keep the inner `*-signed.zip` intact.

The workflow fails before checkout when any signing secret is absent or invalid. It materializes the
certificate and private key as permission-restricted files under the GitHub runner's temporary directory
because signature verification consumes file paths. Raw multiline secrets are scoped only to that setup
step. A final step removes the temporary files on success or failure. The workflow prints only a missing
secret name or validation error, never the secret content. The produced artifact is retained for 14 days.

## 4. Perform the first Marketplace upload

Sign in to JetBrains Marketplace, choose **Add new plugin**, complete the listing, and upload the inner
signed ZIP manually. Submission and review status must be checked in Marketplace; a successful GitHub
workflow does not establish that the plugin is public.

Only after JetBrains accepts the initial plugin entry should a separate change add token-based
`publishPlugin` automation for later versions.

## 5. After submission: review checklist

Once version `0.1.0` has been submitted, treat the repository and Marketplace as separate sources of
truth until JetBrains finishes review.

- Keep `README.md` release status as **pending review** until the Marketplace listing itself confirms
  approval/public availability. Do not infer approval from a successful build, signed artifact, plugin ID,
  or existence of a Marketplace edit page.
- Do not upload a replacement `0.1.0` artifact or publish a new plugin version while review is pending
  unless JetBrains explicitly requests a corrected artifact or the release owner approves a new version.
- If JetBrains requests changes, capture the request in a repository issue, make the smallest required
  change through the normal PR → CI → squash-merge workflow, then rebuild a signed artifact from the exact
  approved commit.
- If JetBrains approves the plugin, verify the public listing, displayed version, compatibility range,
  license, vendor, and installation path before changing repository documentation from pending to released.
- If JetBrains rejects the plugin, preserve the rejection reason verbatim in the internal work log or issue
  description, but do not commit credentials, private correspondence, signing material, or account details.
- Keep publishing manual for the initial release. Adding a Marketplace token or enabling automated
  `publishPlugin` remains a separate, explicitly approved change.

## Official references

- [JetBrains: Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
- [JetBrains: Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html)
- [JetBrains: IntelliJ Platform Gradle tasks](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html)
