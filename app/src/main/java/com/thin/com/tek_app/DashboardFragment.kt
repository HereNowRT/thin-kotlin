package com.thin.com.tek_app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.EditText
import com.jakewharton.rxbinding3.widget.textChanges
import java.util.concurrent.TimeUnit


class DashboardFragment : Fragment() {
    lateinit var txtDestination: EditText
    lateinit var txtOrigin: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        txtDestination = view.findViewById(R.id.txt_destination)
        txtOrigin = view.findViewById(R.id.txt_origin)

        var destination_subscribtion = txtDestination
            .textChanges()
            .skip(1)
            .map { it.toString().toLowerCase() }
            .distinct()
            .filter { it.isNotBlank() }
            .debounce(300,TimeUnit.MILLISECONDS)
            .subscribe({
                Log.d("Text changes @ destination: ",it.toString())
            })

        var origin_subscription = txtOrigin
            .textChanges()
            .skip(1)
            .map { it.toString().toLowerCase() }
            .distinct()
            .filter { it.isNotBlank() }
            .debounce(300,TimeUnit.MILLISECONDS)
            .subscribe({
                Log.d("Text changes @ origin: ",it.toString())
            })


    }






}
