# My Bills v122

Build: **122 / 19.1-v122**

This release makes `app/src/main/assets/index.html` the single authoritative Android UI source. The older `app/src/index.html` is intentionally reduced to an unused marker page. MainActivity loads only the bundled asset and clears WebView cache before loading it.
