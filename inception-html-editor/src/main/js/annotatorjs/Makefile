REPORTER=spec

default: all

.DEFAULT:
	cd pkg && $(MAKE) $@
	cp pkg/annotator-full.js ../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
#	cp pkg/annotator-full.map ../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
	cp pkg/annotator-full.min.js ../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
#	cp pkg/annotator-full.min.map ../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
	cp pkg/annotator.css ../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/
	cp pkg/annotator.min.css ../../java/de/tudarmstadt/ukp/inception/htmleditor/annotatorjs/resources/

test: develop
	./tools/test -R $(REPORTER)

develop:
	./tools/build

doc:
	cd doc && $(MAKE) html

.PHONY: test develop doc
