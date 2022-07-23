package me.mnlr.vripper.services

import org.htmlcleaner.CleanerProperties
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.HtmlCleaner
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import me.mnlr.vripper.exception.HtmlProcessorException
import java.io.InputStream

@Service
class HtmlProcessorService {
    @Throws(HtmlProcessorException::class)
    fun clean(htmlContent: InputStream): Document {
        return try {
            htmlContent.use {
                val clean = HtmlCleaner().clean(htmlContent)
                DomSerializer(CleanerProperties()).createDOM(clean)
            }
        } catch (e: Exception) {
            throw HtmlProcessorException(e)
        }
    }
}