---
name: "📦 Feature release (for developers)"
about: Release checklist for feature releases (first digit increase)
---

**GitHub issue tracker**
- [ ] Ensure all issues and PRs are resolved/merged

**Local**
- [ ] Run Maven release (increase first digit of version).

**GitHub release page**
- [ ] Upload the JAR to the GitHub release
- [ ] Write release announcement and add to GitHub release

**Github pages**
- [ ] Update the `releases.yml` file
- [ ] Add release documentation to GitHub pages

**Demo/test server**
- [ ] *stable instance*: Update to release version
- [ ] *community instance*: Update to release version
- [ ] *demo instance*: Update to release version

**Docker**
- [ ] Push the release to Docker

**Mailing list**
- [ ] Send release announcement to mailing list
