# Chayns-Dev-Tools

<!-- Plugin description -->
ChaynsDevTools provides essential utilities for developers working with chayns®.

### Features:

- **Sites Panel:**

  - Search sites by name, SiteID, LocationID, personId, or user name
  - Support for searching multiple personIds simultaneously using a variable separator
  - Select text in editor and search via right-click menu option or shortcut (ALT+SHIFT+W)
  - Results displayed in a table with click-to-copy functionality for any cell
  - Right-click menu on rows to generate and copy site tokens, open sites, or preserve results for later use

- **Persons Panel:**

  - Search users by name, personId, or multiple personIds using a separator
  - Select text in editor and search via right-click menu option or shortcut (ALT+SHIFT+E)
  - Results displayed in a table with click-to-copy functionality
  - Right-click menu on rows to preserve results for later use

- **Chayns Exceptions:**
  - Insert new exceptions via right-click menu option or ALT+SHIFT+X shortcut
  - Automatically detects namespace configuration from project's appsettings.json
  - Opens a configuration dialog to customize the exception parameters
  - Inserts the formatted exception code at the current cursor position

- **Remote Login:** Login to chayns® via chayns.de remote login.
- **Persistent Data Management:** Sites and Persons are stored across IDEs

### Roadmap:

- **UAC Panel:** Get groups for Sites and check members in Group (Only with user token)
- **Search Options:** Add option to show raw search amount, so duplicates are not filtered out
- **Sync Data via WebSocket:** Sync data via WebSocket to all IDEs, instead of sync button

This plugin streamlines your chayns® development workflow by providing quick access to common tools and information
directly within your IDE.
<!-- Plugin description end -->