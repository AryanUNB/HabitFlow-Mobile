
package ca.unb.mobiledev.habitflow

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.util.UUID

class AddHabitActivity : Activity() {

    private val green = Color.rgb(21, 62, 53)
    private val pageBackgroundColor = Color.rgb(243, 247, 244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCategoryScreen()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showCategoryScreen() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(pageBackgroundColor)
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        val backButton = Button(this).apply {
            text = "BACK"
            setOnClickListener { finish() }
        }

        container.addView(backButton)

        val heading = TextView(this).apply {
            text = "Choose your focus"
            textSize = 29f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(24), 0, dp(8))
        }

        container.addView(heading)

        val subtitle = TextView(this).apply {
            text = "What would you like to work on?"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, dp(20))
        }

        container.addView(subtitle)

        addCategory(
            container,
            "📚",
            "Study",
            "Build better learning habits"
        )

        addCategory(
            container,
            "🌿",
            "Recovery",
            "Make time for rest and self-care"
        )

        addCategory(
            container,
            "❤️",
            "Relationships",
            "Stay connected with people"
        )

        addCategory(
            container,
            "💪",
            "Health",
            "Improve your physical well-being"
        )

        addCategory(
            container,
            "✨",
            "Custom Habit",
            "Create a habit of your own"
        )

        scroll.addView(container)
        setContentView(scroll)
        fitSystemBars()
    }

    private fun addCategory(
        container: LinearLayout,
        emoji: String,
        title: String,
        description: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(215, 229, 222))
            }

            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "$title. $description"

            setOnClickListener {
                showHabitForm(
                    if (title == "Custom Habit") "" else title
                )
            }
        }

        val icon = TextView(this).apply {
            text = emoji
            textSize = 30f
            gravity = Gravity.CENTER
        }

        card.addView(
            icon,
            LinearLayout.LayoutParams(dp(55), dp(55))
        )

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        val name = TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
        }

        val detail = TextView(this).apply {
            text = description
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, dp(4), 0, 0)
        }

        textContainer.addView(name)
        textContainer.addView(detail)

        card.addView(
            textContainer,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(14)
        }

        container.addView(card, params)
    }

    private fun showHabitForm(selectedCategory: String) {
        setContentView(R.layout.activity_add_habit)
        fitSystemBars()

        findViewById<Button>(R.id.back).setOnClickListener {
            showCategoryScreen()
        }

        val target = findViewById<Spinner>(R.id.target)

        target.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (1..7).map { "$it days per week" }
        )

        target.setSelection(4)

        val category = findViewById<EditText>(R.id.category)
        category.setText(selectedCategory)

        findViewById<Button>(R.id.save).setOnClickListener {
            val input = findViewById<EditText>(R.id.name)
            val name = input.text.toString().trim()

            if (name.isEmpty()) {
                input.error = "Enter a habit name"
                input.requestFocus()
                return@setOnClickListener
            }

            val habit = Habit(
                UUID.randomUUID().toString(),
                name,
                target.selectedItemPosition + 1,
                category.text.toString().trim()
            )

            val repository = HabitRepository(this)

            if (repository.save(repository.load() + habit)) {
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Could not save. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
