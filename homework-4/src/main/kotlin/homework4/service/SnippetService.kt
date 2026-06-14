package homework4.service

import homework4.models.Snippet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

interface SnippetService {
    fun create(title: String, content: String): Snippet
    fun get(id: Int): Snippet?
    fun search(query: String): List<Snippet>
}

/**
 * In-memory snippet store.
 */
class InMemorySnippetService : SnippetService {
    private val store = ConcurrentHashMap<Int, Snippet>()
    private val sequence = AtomicInteger(0)

    override fun create(title: String, content: String): Snippet {
        val id = sequence.incrementAndGet()
        val snippet = Snippet(id = id, title = title, content = content)
        store[id] = snippet
        return snippet
    }

    override fun get(id: Int): Snippet? = store[id]

    override fun search(query: String): List<Snippet> {
        return store.values.filter { it.title.contains(query, ignoreCase = true) }
    }
}
