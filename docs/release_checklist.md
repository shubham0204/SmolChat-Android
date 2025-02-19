# Release Checklist for SmolChat

1. Edit `CHANGELOG.md` to describe new features included in the release in single-line bullet points.
2. Commit and push `CHANGELOG.md`
3. Create a new tag `v0.0.x` with `git tag v0.0.x`
4. Push the new tag with `git push origin --tags`
5. Verify that the CI has been initiated and a new release gets created.

## Removing tags

1. Remove the tag locally: `git tag -d v0.0.x`
2. Remove the tag from remote: `git push origin --delete v0.0.4`