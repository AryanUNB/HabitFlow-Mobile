package ca.unb.mobiledev.habitflow

import android.app.Activity
import android.os.Bundle
import android.widget.*
import java.util.UUID

class AddHabitActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)
        fitSystemBars()
        findViewById<Button>(R.id.back).setOnClickListener { finish() }
        val target = findViewById<Spinner>(R.id.target)
        target.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, (1..7).map { "$it days per week" })
        if (savedInstanceState == null) target.setSelection(4)
        findViewById<Button>(R.id.save).setOnClickListener {
            val input = findViewById<EditText>(R.id.name)
            val name = input.text.toString().trim()
            if (name.isEmpty()) { input.error = "Enter a habit name"; input.requestFocus(); return@setOnClickListener }
            val habit = Habit(UUID.randomUUID().toString(), name, target.selectedItemPosition + 1,
                findViewById<EditText>(R.id.category).text.toString().trim())
            val repository = HabitRepository(this)
            if (repository.save(repository.load() + habit)) finish()
            else Toast.makeText(this, "Could not save. Please try again.", Toast.LENGTH_LONG).show()
        }
    }
}
