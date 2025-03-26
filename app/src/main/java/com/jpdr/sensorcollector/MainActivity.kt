package com.jpdr.sensorcollector

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            SensorCollectorApp(
                state = state,
                handleIntent = {
                    viewModel.handleIntent(it)
                }
            )
        }
    }
}
