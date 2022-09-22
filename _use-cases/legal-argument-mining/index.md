---
title: Legal Argument Mining
subheadline: Annotating arguments in judgments of the European Court of Human Rights
permalink: /use-cases/legal-argument-mining/
screenshot: European_Court_of_Human_Rights_logo.svg.png
thumbnail: European_Court_of_Human_Rights_logo.svg.png
hidden: false
---

**Source**: <i>This use-case was kindly contributed by <a href="https://www.trusthlt.org">Dr. Ivan Habernal</a>,
 Trustworthy Human Technologies, Technical University of Darmstadt, Germany</i>

Identifying, classifying, and analyzing arguments in legal discourse has been a prominent area of research since the inception of the argument mining field.

We designed a new annotation scheme for legal arguments in proceedings of the European Court of Human Rights (ECHR) and used the INCEpTION tool to annotate a large corpus of 373 court decisions (2.3M tokens and 15k annotated argument spans).

In doing so, we made use of the following features of the INCEpTION platform:

* Annotation of spans involving multiple annotation layers
* Importing and exporting UIMA CAS annotations with [dkpro-cassis](https://github.com/dkpro/dkpro-cassis)
* Utilities for monitoring annotator progress and computing inter-annotator agreement
* Semi-automatic and manual curation of the final dataset

##### References

* Habernal, I., Faber, D., Recchia, N., Bretthauer, N., Gurevych, I., Spiecker genannt Döhmann, I., Burchard, C. (2022). Mining Legal Arguments in Court Decisions.
 [[arXiv preprint](https://arxiv.org/abs/2208.06178)]
  