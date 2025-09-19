- Fixed a bug where the app's memory usage kept increasing after switching models i.e. the memory acquired by the previous model was not 'released' when selecting a different model
- Sync with upstream llama.cpp
- Align default inference parameters with those found in `llama` executable

### UI Improvements

- Chat message actions like share/copy/edit are now available in a dialog which appears when the message is long-pressed
- Fix misleading/overflowing icons to enhance UX
- Preserve query text in the search box when a model is opened while browsing HuggingFace