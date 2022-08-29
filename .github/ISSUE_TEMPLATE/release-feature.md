---
name: Bug-fix release (for developers)
about: Release checklist for bug-fix releases (third-digit increase)

---

**GitHub issue tracker**
- [ ] Ensure all issues and PRs are resolved/merged

**Local**
- [ ] Run Maven release (increase third digit of version). Check that the JDK 
      used to run the release is not newer than the Java version specified in the minimum system
      requirements!
- [ ] Sign the standalone JAR

**GitHub release page**
- [ ] Upload the JAR to the GitHub release
- [ ] Write release announcement and add to GitHub release

**Github pages**
- [ ] Update the `releases.yml` file
- [ ] Add release documentation to GitHub pages

**Demo/test server**
- [ ] *stable instance*: Update to release version
- [ ] *community instance*: Update to release version
- [ ] *testing instance*: Update auto-deployment script to match new SNAPSHOT version
- [ ] *demo instance*: Update to release version

**Docker**
- [ ] Push the release to Docker

**Mailing list**
- [ ] Send release announcement to mailing list
