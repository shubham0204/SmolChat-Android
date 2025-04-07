name: Build and Release Android APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build_and_release_apk:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          submodules: 'true'

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Setup Android NDK
        uses: nttld/setup-ndk@v1
        id: setup-ndk
        with:
          ndk-version: r27c
          add-to-path: true
        env:
          ANDROID_NDK_HOME: ${{ steps.setup-ndk.outputs.ndk-path }}

      - name: Decode keystore
        env:
          ENCODED_STRING: ${{ secrets.KEYSTORE_BASE_64 }}
          RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }}
          RELEASE_KEYSTORE_ALIAS: ${{ secrets.RELEASE_KEYSTORE_ALIAS }}
          RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
        run: |
          echo $ENCODED_STRING > keystore-b64.txt
          base64 -d keystore-b64.txt > keystore.jks

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build APK
        env:
          RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }}
          RELEASE_KEYSTORE_ALIAS: ${{ secrets.RELEASE_KEYSTORE_ALIAS }}
          RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
        run: ./gradlew assembleRelease --stacktrace

      - name: Create a release
        uses: actions/create-release@v1
        id: create_release
        with:
          tag_name: ${{ github.ref }}
          release_name: ${{ github.ref }}
          draft: false
          prerelease: false
          body_path: CHANGELOG.md
        env:
          GITHUB_TOKEN: ${{ github.token }}

      - name: Upload APK to release
        uses: actions/upload-release-asset@v1
        env:
          GITHUB_TOKEN: ${{ github.token }}
        with:
          upload_url: ${{ steps.create_release.outputs.upload_url }}
          asset_path: app/build/outputs/apk/release/app-release.apk
          asset_name: SmolChat_${{ github.ref_name }}.apk
          asset_content_type: application/vnd.android.package-archive