- The app now supports automatic recognition (speech-to-text) for providing queries to the model.
  The speech-to-text capabilities are powered
  by [Moonshine](https://github.com/moonshine-ai/moonshine) and the packaged models could be found
  on [HuggingFace](https://huggingface.co/shubhxm0204/moonshine-asr-models/tree/main).
  - The speech is transformed into text during an on-device ASR model provided by Moonshine. Users
    will only require an internet connection when downloading the ASR models from the abovementioned
    HuggingFace Models repository.
  - Currently supported languages are English (`en`) and Chinese (`zh`). Although Moonshine supports
    more languages at the moment, SmolChat will add support for them in the future.

- UI enhancements when downloading models from HuggingFace. Thanks to Gemini in Android Studio, the
  UI of the HuggingFace model browser screens has been improved and modernized.

- A bug causing the app to crash after modifying model settings has been resolved (#114). Thanks to
  @jkkj for the contribution.