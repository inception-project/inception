---
name: Feature release (for developers)
about: Release checklist for feature releases (first or second digit increase)

---

**GitHub issue tracker**
- [ ] Ensure all issues and PRs are resolved/merged

**Local**
- [ ] Run Maven release (usually increase second digit of version, not first). Check that the JDK 
      used to run the release is not newer than the Java version specified in the minimum system
      requirements!
- [ ] Sign the standalone JAR

**GitHub release page**
- [ ] Upload the JAR to the GitHub release
- [ ] Write release announcement and add to GitHub release

**Github pages**
- [ ] Update the `releases.yml` file
- [ ] Add release documentation to GitHub pages

**CI server**
- [ ] Create a new build job for the new maintenance branch by copying the build job of the current 
      maintenance branch and updating the branch info.
- [ ] Remove auto-deploy configuration from the old maintenance branch

**Demo/test server**
- [ ] *stable instance*: Update to release version
- [ ] *community instance*: Update to release version
- [ ] *testing instance*: Update auto-deployment script to match new SNAPSHOT version
- [ ] *demo instance*: Update to release version

**Docker**
- [ ] Push the release to Docker

**Mailing list**
- [ ] Send release announcement to mailing list
