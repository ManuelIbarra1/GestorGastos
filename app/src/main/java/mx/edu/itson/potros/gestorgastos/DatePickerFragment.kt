package mx.edu.itson.potros.gestorgastos

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.Calendar

class DatePickerFragment(val listener: (dia: Int, mes: Int, año: Int) -> Unit): DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        listener(dayOfMonth, month, year)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c: Calendar = Calendar.getInstance()

        val dia: Int = c.get(Calendar.DAY_OF_MONTH)
        val mes: Int = c.get(Calendar.MONTH)
        val año: Int = c.get(Calendar.YEAR)

        val picker = DatePickerDialog(activity as Context, this, año, mes, dia)
        return picker
    }
}