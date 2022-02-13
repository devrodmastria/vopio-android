package info.vopio.android.Utilities

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import java.util.*
import kotlin.math.max
import kotlin.math.abs

abstract class RecyclerSwipeHelper(private val recyclerView: RecyclerView, private val inactiveSessionList: List<DataSnapshot>, private val databaseRef : DatabaseReference) :
    ItemTouchHelper.SimpleCallback(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.LEFT){

    private var swipeBack = false
    private var swipedPosition = -1
    private val optionsBuffer: MutableMap<Int, List<MenuButton>> = mutableMapOf()
    private val recoverQueue = object : LinkedList<Int>(){
        override fun add(element: Int): Boolean {
            if (contains(element)) return false
            return super.add(element)
        }
    }

    private val touchListener = View.OnTouchListener { view, motionEvent ->
        if (swipedPosition < 0) return@OnTouchListener false

        optionsBuffer[swipedPosition]?.forEach {
            it.handleClick(motionEvent)
            view.performClick()
        }
        recoverQueue.add(swipedPosition)
        swipedPosition = -1
        recoverSwipedItem()

        swipeBack = (motionEvent.action == MotionEvent.ACTION_CANCEL || motionEvent.action == MotionEvent.ACTION_UP)

        true
    }

    private fun recoverSwipedItem(){

        while (!recoverQueue.isEmpty()){
            val position = recoverQueue.poll() ?: return
            recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    init {
        recyclerView.setOnTouchListener(touchListener)
    }

    private fun drawButtons(
        canvas: Canvas,
        buttons: List<MenuButton>,
        itemView: View,
        dX: Float
    ) {
        var right = itemView.right
        buttons.forEach { button ->
            val width = button.intrinsicWidth / buttons.intrinsicWidth() * abs(dX)
            val left = right - width
            button.draw(
                canvas,
                RectF(left, itemView.top.toFloat(), right.toFloat(), itemView.bottom.toFloat())
            )

            right = left.toInt()
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val position = viewHolder.bindingAdapterPosition
        var maxDX = dX
        val itemView = viewHolder.itemView

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                if (!optionsBuffer.containsKey(position)) {
                    optionsBuffer[position] = instantiateMenuButton(position)
                }

                val buttons = optionsBuffer[position] ?: return
                if (buttons.isEmpty()) return
                maxDX = max(-buttons.intrinsicWidth(), dX)
                drawButtons(c, buttons, itemView, maxDX)
            }
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            maxDX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

//        val itemPosition = viewHolder.bindingAdapterPosition
//
//        if (swipedPosition != itemPosition) recoverQueue.add(swipedPosition)
//        swipedPosition = itemPosition
//        recoverSwipedItem()
    }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {

        if(swipeBack){
            swipeBack = false
            return 0
        }

        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    abstract fun instantiateMenuButton(position: Int): List<MenuButton>

    interface MenuButtonClickListener {
        fun onClick()
    }

    class MenuButton(private val context: Context, private val title: String, textSize: Float,
                     @ColorRes private val colorRes: Int, private val clickListener: MenuButtonClickListener){

        private var clickableArea: RectF? =  null
        private val textSizeInPixel: Float = textSize * context.resources.displayMetrics.density
        private val horizontalPadding = 50.0f
        private val titleBounds : Rect
        val intrinsicWidth: Float

        init {

            val paint = Paint()
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.CENTER

            titleBounds = Rect()
            paint.getTextBounds(title, 0, title.length, titleBounds)
            intrinsicWidth = titleBounds.width() + (2 * horizontalPadding)
        }

        fun draw(canvas: Canvas, rect: RectF){

            val paint = Paint()

            // background
            paint.color = ContextCompat.getColor(context, colorRes)
            canvas.drawRect(rect, paint)

            // title
            paint.color = ContextCompat.getColor(context, android.R.color.white)
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.LEFT

            paint.getTextBounds(title, 0, title.length, titleBounds)

            val y = rect.height() / 2 + titleBounds.height() / 2 - titleBounds.bottom
            canvas.drawText(title, (rect.left + horizontalPadding), rect.top + y, paint)

            clickableArea = rect

        }

        fun handleClick(event: MotionEvent){
            clickableArea?.let {
                if (it.contains(event.x, event.y)){
                    clickListener.onClick()
                }
            }
        }
    }
}

private fun List<RecyclerSwipeHelper.MenuButton>.intrinsicWidth(): Float {
    if(isEmpty()) return 0.0f
    return map { it.intrinsicWidth }.reduce {acc, fl -> acc + fl }
}

