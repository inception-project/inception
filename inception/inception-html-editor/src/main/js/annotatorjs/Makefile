REPORTER=spec

default: all

.DEFAULT:
	cd pkg && $(MAKE) $@
#	cp annotator-full.js ../../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
#	cp annotator-full.map ../../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
#	cp annotator-full.min.js ../../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
#	cp annotator-full.min.map ../../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
#	cp annotator.css ../../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
#	cp annotator.min.css ../../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/

test: develop
	./tools/test -R $(REPORTER)

develop:
	./tools/build

doc:
	cd doc && $(MAKE) html

.PHONY: test develop doc
