package me.mnlr.vripper.services

import org.springframework.stereotype.Service
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import me.mnlr.vripper.exception.XpathException
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@Service
class XpathService {
    private val xpath = XPathFactory.newInstance().newXPath()

    @Throws(XpathException::class)
    fun getAsNode(source: Node?, xpathExpression: String?): Node? {
        return try {
            xpath.compile(xpathExpression).evaluate(source, XPathConstants.NODE) as Node
        } catch (e: Exception) {
            throw XpathException(e)
        }
    }

    @Throws(XpathException::class)
    fun getAsNodeList(source: Node?, xpathExpression: String?): NodeList {
        return try {
            xpath.compile(xpathExpression).evaluate(source, XPathConstants.NODESET) as NodeList
        } catch (e: Exception) {
            throw XpathException(e)
        }
    }
}