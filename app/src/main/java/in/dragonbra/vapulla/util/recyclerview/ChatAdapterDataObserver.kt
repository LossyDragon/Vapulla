package `in`.dragonbra.vapulla.util.recyclerview

import `in`.dragonbra.vapulla.adapter.ChatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatAdapterDataObserver(
    private val adapter: ChatAdapter,
    private val layoutManager: LinearLayoutManager,
    private val recyclerView: RecyclerView
) : RecyclerView.AdapterDataObserver() {

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (adapter.itemCount == 0) {
            return
        }

        val fromLocal = adapter.peek(0)?.fromLocal ?: false
        val findVisiblePosition = layoutManager.findFirstVisibleItemPosition()

        if (fromLocal ||
            findVisiblePosition == -1 ||
            positionStart == 0 &&
            findVisiblePosition == 0
        ) {
            recyclerView.scrollToPosition(positionStart)
        }
    }
}
