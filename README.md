<div align="center">
  <a href="https://inception-project.github.io/">
    <img width="471" style="max-width:50%;" src="inception/inception-doc/src/main/resources/META-INF/asciidoc/inception-logo.png"  alt="INCEpTION Logo"/>
  </a>
  <p>
    A semantic annotation platform offering intelligent assistance and knowledge management.
  </p>
  <br/>
  <p>
    <a href="https://inception-project.github.io/"><strong>Homepage</strong></a> ·
    <a href="https://inception-project.github.io/documentation/latest/user-guide"><strong>Usage</strong></a> ·
    <a href="https://morbo.ukp.informatik.tu-darmstadt.de/demo"><strong>Demo</strong></a>  ·  
    <a href="https://inception-project.github.io/documentation/latest/user-guide#sect_faq"><strong>FAQ</strong></a>
  </p>
  <p>

[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/inception-project/inception)](https://github.com/inception-project/inception/releases/latest)
[![GitHub license](https://img.shields.io/github/license/inception-project/inception)](https://github.com/inception-project/inception/blob/master/LICENSE.txt)
[![Join the chat at https://gitter.im/inception-project/Lobby](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/inception-project/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

  </p>

</div>

---

# INCEpTION

INCEpTION provides a semantic annotation platform offering intelligent annotation assistance and knowledge management.
For more information, visit the [INCEpTION website](https://inception-project.github.io/). For a first impression on
what INCEpTION is, you may want to watch
our [introduction videos](https://www.youtube.com/watch?v=Ely8eBKqiSI&list=PL5Hz5pttaj96SlXHGRZf8KzlYvpVHIoL-).

<div align="center" style="margin: 10px">
    <img src="https://inception-project.github.io/images/screenshot-annotation.png" alt="INCEpTION Screenshot" />
</div>

INCEpTION is a text-annotation environment useful for various kinds of annotation tasks on written text. Annotations are
usually used for linguistic and/or machine learning concerns. INCEpTION is a web application in which several users can
work on the same annotation project, and it can contain several annotation projects at a time. It provides a *recommender system* 
that suggest potential annotations to help you create annotations faster and easier. Beyond annotating, you can also *create a corpus*
by searching an external document repository and adding documents. Moreover, you can use *knowledge bases*, e.g. for
tasks like entity linking.

## Getting started

The best way to get started is to
watch [our tutorial videos](https://www.youtube.com/watch?v=Ely8eBKqiSI&list=PL5Hz5pttaj96SlXHGRZf8KzlYvpVHIoL-),
working through
the [Getting Started Guide](https://inception-project.github.io/documentation/latest/user-guide#sect_core_funct) and
playing with INCEpTION on the [demo server](https://morbo.ukp.informatik.tu-darmstadt.de/demo).

## See our documentation for further reading

- **User Guide:** If you only use INCEpTION and do not develop it,
  the [User Guide](https://inception-project.github.io/documentation/latest/user-guide#sect_core_funct) beginning right
  after *Getting Started* is the guide of your choice. If it does not answer your questions, don’t hesitate to contact
  us (see *Do you have questions or feedback?*).
- **Admin Guide:** For information on how to set up INCEpTION for a group of users on a server and more installation
  details, see the [Admin Guide](https://inception-project.github.io/documentation/latest/admin-guide).
- **Developer Guide:** INCEpTION is open source. So if you would like to develop for it,
  the [Developer Guide](https://inception-project.github.io/documentation/latest/developer-guide) might be interesting
  for you.

Many more materials like [example projects](https://inception-project.github.io/example-projects/)
, [use case descriptions](https://inception-project.github.io/use-cases/)
and [helpful scripts](https://inception-project.github.io/example-projects/python/) are available via the INCEpTION
homepage.

We also offer several Jupyter Notebooks which describe how you can interact in Python with INCEpTION, prepare or 
post-process annotations:

- [![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/inception-project/inception/blob/master/notebooks/annotations_as_one_sentence_and_label_per_line.ipynb) Export annotations as one sentence per line
- [![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/inception-project/inception/blob/master/notebooks/using_pretokenized_and_preannotated_text.ipynb) Use pre-tokenized and pre-annotated documents in INCEpTION
- [![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/inception-project/inception/blob/master/notebooks/annotated_word_files_to_cas_xmi.ipynb) Convert Word files to CAS XMI for import into INCEpTION
- [![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/inception-project/inception/blob/master/notebooks/Working_with_INCEpTION_slot_features_in_DKPro_Cassis.ipynb) Working with INCEpTION slot features in DKPro Cassis
- ... [more Python examples](https://inception-project.github.io/example-projects/python/)


## Do you have questions or feedback?

INCEpTION is still in development, so you are welcome to give us feedback and tell us your wishes and requirements.

- For many questions, you find answers in the main documentation: [Core Functionalities](https://inception-project.github.io/documentation/latest/user-guide#sect_core_funct).
- Consider our Google group [inception-users](https://groups.google.com/forum/#!forum/inception-users)
- You can also open an issue on [Github](https://github.com/inception-project/inception/issues).

## How to cite

Please use the following citation:

    @inproceedings{klie-etal-2018-inception,
        title = "The {INCE}p{TION} Platform: Machine-Assisted and Knowledge-Oriented Interactive Annotation",
        author = "Klie, Jan-Christoph and Bugert, Michael  and Boullosa, Beto and Eckart de Castilho, Richard and Gurevych, Iryna",
        booktitle = "Proceedings of the 27th International Conference on Computational Linguistics: System Demonstrations",
        year = "2018",
        address = "Santa Fe, New Mexico",
        url = "https://www.aclweb.org/anthology/C18-2002",
        pages = "5--9"
    }

## Contributing

Do you miss a feature? We very much appreciate your contribution! Please open an issue before sending a pull request.
INCEpTION uses the [DKPro Contribution Guidelines](https://dkpro.github.io/contributing).

1. Create a fork
2. Create your feature branch: `git checkout -b my-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request 🚀

## License

INCEpTION is provided as open source under the Apache License v2.0.

---

<div align="center">
<img height="50" style="margin-right: 15px" src="https://inception-project.github.io/images/logos/tud_logo.gif">
<img height="50" style="margin-right: 15px" src="https://inception-project.github.io/images/logos/ukp-lab.png">
<img height="50" style="margin-right: 15px" src="https://inception-project.github.io/images/logos/dfg_logo_schriftzug_blau_foerderung.jpg">
</div>

