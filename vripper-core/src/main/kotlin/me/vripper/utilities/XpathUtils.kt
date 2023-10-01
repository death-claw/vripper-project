package me.vripper.utilities

import me.vripper.exception.XpathException
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

object XpathUtils {
    private val xpath = XPathFactory.newInstance().newXPath()

    @Throws(XpathException::class)
    fun getAsNode(source: Node?, xpathExpression: String?): Node? {
        return try {
            val value = xpath.compile(xpathExpression).evaluate(source, XPathConstants.NODE)
            if(value != null) {
                value as Node
            } else {
                null
            }
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