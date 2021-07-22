package io.bit3.jsass.importer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import io.bit3.jsass.context.ImportStack;

public class JsassCustomHeaderImporter implements Importer {

  private final ImportStack importStack;

  public JsassCustomHeaderImporter(ImportStack importStack) {
    this.importStack = importStack;
  }

  @Override
  public Collection<Import> apply(String url, Import previous) {
    List<Import> list = new LinkedList<>();

    list.add(createCustomHeaderImport(previous));

    return list;
  }

  private Import createCustomHeaderImport(Import previous) {
    int id = importStack.register(previous);

    StringBuilder source = new StringBuilder();

    // $jsass-void: jsass_import_stack_push(<id>) !global;
    source.append(
        String.format( //
            "$jsass-void: null;%n" + //
            "$jsass-void: jsass_import_stack_push(%d) !global;%n", //
            id
        )
    );

    try {
      return new Import(
          new URI(previous.getAbsoluteUri() + "/JSASS_CUSTOM.scss"),
          new URI(previous.getAbsoluteUri() + "/JSASS_CUSTOM.scss"),
          source.toString()
      );
    } catch (URISyntaxException e) {
      throw new ImportException(e);
    }
  }
}
