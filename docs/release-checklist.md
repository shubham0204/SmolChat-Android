# Release Checklist

## Steps

1. Sync with upstream llama.cpp and make the commit,

```
chore: sync with llama.cpp upstream   
```

2. Update `CHANGELOG.md` with details on new features and bug fixes
3. Increment `versionCode` and `versionName` in `build.gradle.kts` for `app` module
4. Make a commit with the message,

```
release: prepare for release <new-version-number>
```

5. Create a new tag and push it to GitHub to initiate the CI pipeline for releasing,

```bash
git tag <new-version-number>
git push origin --tags
```

## Deleting a Release/Tag from GitHub

1. Delete the release from GitHub
2. Delete the corresponding tag from GitHub (thus deleting it from the remote `origin`)
3. Delete the tag from your local machine

```bash
git tag -d <new-version-number>
```