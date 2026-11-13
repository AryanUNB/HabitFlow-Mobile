
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

    companion object {
        const val EXTRA_EDIT_HABIT_ID = "edit_habit_id"
    }

    private val green = Color.rgb(21, 62, 53)
    private val pageBackgroundColor = Color.rgb(243, 247, 244)

    private lateinit var repository: HabitRepository
    private var editingHabit: Habit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = HabitRepository(this)

        val editId = intent.getStringExtra(EXTRA_EDIT_HABIT_ID)

        if (editId != null) {
            editingHabit = repository.load().firstOrNull {
                it.id == editId
            }

            if (editingHabit == null) {
                Toast.makeText(
                    this,
                    "Habit not found.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return
            }

            showHabitForm(editingHabit!!.category)
        } else {
            showCategoryScreen()
        }
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

        container.addView(Button(this).apply {
            text = "BACK"
            setOnClickListener { finish() }
        })

        container.addView(TextView(this).apply {
            text = "Choose your focus"
            textSize = 29f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(24), 0, dp(8))
        })

        container.addView(TextView(this).apply {
            text = "What would you like to work on?"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, dp(20))
        })

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

            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
            )

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()

                setStroke(
                    dp(1),
                    Color.rgb(215, 229, 222)
                )
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

        card.addView(
            TextView(this).apply {
                text = emoji
                textSize = 30f
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(dp(55), dp(55))
        )

        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        textContainer.addView(TextView(this).apply {
            text = title
            textSize = 19f
            setTextColor(green)
            setTypeface(null, Typeface.BOLD)
        })

        textContainer.addView(TextView(this).apply {
            text = description
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, dp(4), 0, 0)
        })

        card.addView(
            textContainer,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        container.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        )
    }

    private fun showHabitForm(selectedCategory: String) {
        setContentView(R.layout.activity_add_habit)
        fitSystemBars()

        val currentHabit = editingHabit

        findViewById<Button>(R.id.back).setOnClickListener {
            if (currentHabit != null) {
                finish()
            } else {
                showCategoryScreen()
            }
        }

        val nameInput = findViewById<EditText>(R.id.name)
        val categoryInput = findViewById<EditText>(R.id.category)
        val target = findViewById<Spinner>(R.id.target)
        val saveButton = findViewById<Button>(R.id.save)

        target.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (1..7).map { "$it days per week" }
        )

        if (currentHabit != null) {
            // Prepopulate the form with existing values.
            nameInput.setText(currentHabit.name)
            categoryInput.setText(currentHabit.category)

            target.setSelection(
                (currentHabit.target - 1).coerceIn(0, 6)
            )

            saveButton.text = "SAVE CHANGES"
        } else {
            categoryInput.setText(selectedCategory)
            target.setSelection(4)

            saveButton.text = "SAVE HABIT"
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val category = categoryInput.text.toString().trim()
            val weeklyTarget = target.selectedItemPosition + 1

            if (name.isEmpty()) {
                nameInput.error = "Enter a habit name"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            val habits = repository.load()

            val saved = if (currentHabit != null) {
                // Preserve the existing ID and completion history.
                if (habits.none { it.id == currentHabit.id }) {
                    false
                } else {
                    val updatedHabits = habits.map { habit ->
                        if (habit.id == currentHabit.id) {
                            habit.copy(
                                name = name,
                                category = category,
                                target = weeklyTarget
                            )
                        } else {
                            habit
                        }
                    }

                    repository.save(updatedHabits)
                }
            } else {
                val newHabit = Habit(
                    UUID.randomUUID().toString(),
                    name,
                    weeklyTarget,
                    category
                )

                repository.save(habits + newHabit)
            }

            if (saved) {
                Toast.makeText(
                    this,
                    if (currentHabit != null) {
                        "Habit updated successfully"
                    } else {
                        "Habit created successfully"
                    },
                    Toast.LENGTH_SHORT
                ).show()

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
