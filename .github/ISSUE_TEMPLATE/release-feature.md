---
name: Bug-fix release (for developers)
about: Release checklist for bug-fix releases (third-digit increase)

---

**GitHub issue tracker**
- [ ] Ensure all issues and PRs are resolved/merged

**Local**
- [ ] Run Maven release (increase third digit of version)
- [ ] Sign the standalone JAR

**GitHub release page**
- [ ] Upload the JAR to the GitHub release
- [ ] Write release announcement and add to GitHub release

**Github pages**
- [ ] Update the `releases.yml` file
- [ ] Add release documentation to GitHub pages

**Docker release**
- [ ] Release to DockerHub

**Mailing list**
- [ ] Send release announcement to mailing list
