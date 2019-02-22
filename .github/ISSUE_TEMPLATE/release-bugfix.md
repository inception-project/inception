---
name: Feature release (for developers)
about: Release checklist for feature releases (first or second digit increase)

---

**GitHub issue tracker**
- [ ] Ensure all issues and PRs are resolved/merged

**Local**
- [ ] Run Maven release (usually increase second digit of version, not first)
- [ ] Sign the standalone JAR

**GitHub release page**
- [ ] Upload the JAR to the GitHub release
- [ ] Write release announcement and add to GitHub release

**Github pages**
- [ ] Update the `releases.yml` file
- [ ] Add release documentation to GitHub pages

**Docker release**
- [ ] Release to DockerHub

**CI server**
- [ ] Create a new build job for the new maintenance branch by copying the build job of the current 
      maintenance branch and updating the branch info.
- [ ] Remove auto-deploy configuration from the old maintenance branch

**Mailing list**
- [ ] Send release announcement to mailing list
