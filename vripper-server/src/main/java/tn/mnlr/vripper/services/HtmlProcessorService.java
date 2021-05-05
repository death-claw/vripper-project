package tn.mnlr.vripper.services;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import tn.mnlr.vripper.exception.HtmlProcessorException;

@Service
public class HtmlProcessorService {

  public Document clean(String htmlContent) throws HtmlProcessorException {
    try {
      TagNode clean = new HtmlCleaner().clean(htmlContent);
      return new DomSerializer(new CleanerProperties()).createDOM(clean);
    } catch (Exception e) {
      throw new HtmlProcessorException(e);
    }
  }
}
