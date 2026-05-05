package homework2.utils.parsers

import homework2.models.CreateMetadataRequest
import homework2.models.CreateTicketRequest
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses an XML string into a list of [ParsedRow] results using JDK-only XML APIs.
 *
 * XXE and DTD attacks are mitigated by disabling external entities and doctypes
 * on the [DocumentBuilderFactory] before any parsing occurs.
 *
 * Expected structures:
 *   <tickets><ticket>...</ticket></tickets>   — multiple tickets
 *   <ticket>...</ticket>                      — single ticket (root element)
 *
 * Each <ticket> element maps named child text nodes to [CreateTicketRequest] fields.
 * <tags> contains zero or more <tag> children.
 * <metadata> contains optional <source>, <browser>, <device_type> children.
 *
 * Each element mapping is wrapped in try/catch so one bad element does not
 * abort the rest of the import.
 *
 * Row numbers are 1-based index of the <ticket> element in document order.
 */
object XmlTicketParser {

    fun parse(content: String): List<ParsedRow> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isExpandEntityReferences = false
            isNamespaceAware = false
        }

        val document = try {
            factory.newDocumentBuilder()
                .parse(InputSource(StringReader(content)))
        } catch (e: SAXParseException) {
            return listOf(ParsedRow.Failure(0, "XML parse error at line ${e.lineNumber}: ${e.message}", null))
        } catch (e: Exception) {
            return listOf(ParsedRow.Failure(0, "Failed to parse XML: ${e.message}", null))
        }

        val root = document.documentElement
        root.normalize()

        val ticketNodes: NodeList = when (root.tagName) {
            "tickets" -> root.getElementsByTagName("ticket")
            "ticket"  -> document.getElementsByTagName("ticket")
            else      -> return listOf(
                ParsedRow.Failure(0, "Expected root element <tickets> or <ticket>, got <${root.tagName}>", null)
            )
        }

        if (ticketNodes.length == 0) return emptyList()

        return (0 until ticketNodes.length).map { index ->
            val row = index + 1
            val element = ticketNodes.item(index) as Element
            try {
                ParsedRow.Success(row, elementToRequest(element))
            } catch (e: Exception) {
                ParsedRow.Failure(row, "Failed to map <ticket> element: ${e.message}", element.textContent)
            }
        }
    }

    private fun elementToRequest(el: Element): CreateTicketRequest {
        val tags = el.getElementsByTagName("tags")
            .takeIf { it.length > 0 }
            ?.item(0)
            ?.let { tagsNode ->
                (tagsNode as Element).getElementsByTagName("tag")
                    .let { tagNodes -> (0 until tagNodes.length).map { tagNodes.item(it).textContent.trim() } }
                    .filter { it.isNotEmpty() }
            } ?: emptyList()

        val metaEl = el.getElementsByTagName("metadata")
            .takeIf { it.length > 0 }
            ?.item(0) as? Element

        val metadataSource = metaEl?.childText("source") ?: el.childText("metadata_source")

        val metadata = metadataSource?.takeIf { it.isNotBlank() }?.let {
            CreateMetadataRequest(
                source     = it,
                browser    = metaEl?.childText("browser") ?: el.childText("metadata_browser"),
                deviceType = metaEl?.childText("device_type") ?: el.childText("metadata_device_type")
            )
        }

        return CreateTicketRequest(
            customerId    = el.requireText("customer_id"),
            customerEmail = el.requireText("customer_email"),
            customerName  = el.requireText("customer_name"),
            subject       = el.requireText("subject"),
            description   = el.requireText("description"),
            category      = el.childText("category"),
            priority      = el.childText("priority"),
            status        = el.childText("status"),
            assignedTo    = el.childText("assigned_to"),
            tags          = tags,
            metadata      = metadata
        )
    }

    /** Returns the trimmed text content of the first matching child element, or null if absent. */
    private fun Element.childText(tag: String): String? =
        getElementsByTagName(tag)
            .takeIf { it.length > 0 }
            ?.item(0)
            ?.textContent
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** Like [childText] but throws if the element is absent or blank. */
    private fun Element.requireText(tag: String): String =
        childText(tag) ?: throw IllegalArgumentException("Missing required element <$tag>")
}
