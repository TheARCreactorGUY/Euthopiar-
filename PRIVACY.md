# Privacy Policy for Euthopiar

**Last updated:** November 6, 2026

**Euthopiar** is an open-source, client-side Android application designed for media discovery, personal watchlist management, and viewing history tracking. We value your privacy and are committed to transparency.

---

## 1. Information Collection & Storage
* **Zero Personal Data Collection:** Euthopiar does not collect, log, track, or transmit your personal information, IP addresses, or browsing history to any centralized or private servers.
* **On-Device Storage:** All preferences, cached metadata, and local watch progress remain stored strictly on your local device storage.
* **Open Source Transparency:** The entire source code of Euthopiar is publicly available on [GitHub](https://github.com/TheARCreactorGUY/Euthopiar).

---

## 2. Third-Party Integrations & Authentication
Euthopiar allows users to optionally connect third-party tracking services to synchronize their media watchlists and scrobbles:

### A. Trakt.tv Integration
* **Authentication:** When you connect your Trakt.tv account, Euthopiar authenticates via OAuth 2.0 with PKCE (Proof Key for Code Exchange).
* **Credentials Security:** Your Trakt OAuth tokens are stored locally on your device in secure app preferences. We never have access to your Trakt account password or credentials.
* **Data Transmission:** Playback scrobbles and watchlist sync requests are made directly from your device to Trakt's official API endpoints (`https://api.trakt.tv`).
* **Disclaimer:** *Euthopiar uses Trakt but is not endorsed or certified by Trakt.*

### B. Simkl Integration
* **Authentication:** Connects directly via Simkl's official OAuth 2.0 PKCE flow.
* **Data Transmission:** Synchronizes watch status directly between your device and Simkl's servers.

---

## 3. Revoking Access
You can disconnect third-party integrations at any time:
1. **Inside the App:** Navigate to **Settings > Integrations** and tap **Disconnect**. This instantly deletes your stored access tokens from the device.
2. **From the Service:** You can revoke Euthopiar's access directly from your [Trakt Connected Apps](https://trakt.tv/oauth/authorized_applications) page.

---

## 4. Contact & Inquiries
If you have questions or feedback regarding this privacy policy, please open an issue on our [GitHub Issues](https://github.com/TheARCreactorGUY/Euthopiar/issues) page.
