package com.loneoaktech.tests.coloranimatorvm.ui.main

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    companion object {
        val DEFAULT_BACKGROUND_COLOR = Color.argb(255, 32, 32, 32)
        val DEFAULT_TEXT_COLOR = Color.argb( 255, 240, 240, 240)
        val LOOP_PERIOD = 50L
        val DEFAULT_SATURATION = 1.0f
        val DEFAULT_LIGHTNESS = 0.7f
    }

    private val _viewState = MutableStateFlow( ViewState(
        DEFAULT_BACKGROUND_COLOR,
        DEFAULT_TEXT_COLOR,
        "0x202020"
    ))

    private val _animationState = MutableStateFlow( false )

    private var currentHue: Int = 0


    private var colorJob: Job? = null

    val viewState: StateFlow<ViewState>
        get() = _viewState


    val animationState: StateFlow<Boolean>
        get() = _animationState

    suspend fun setAnimationState( newState: Boolean ) {
        _animationState.emit(newState)
    }

    fun toggleAnimationState() {
        viewModelScope.launch {
            _animationState.emit( _animationState.value.not() )
        }
    }

    init {
        run()
    }

    private fun run() {

        viewModelScope.launch {

            try {
                animationState.collect { active ->
                    if (active) {
                        if ( colorJob?.isActive != true ) {
                            startColorJob()
                        }
                    } else
                        stopColorJob()

                }
            } catch (ce: CancellationException) {
                stopColorJob()
            }
        }
    }

    private suspend fun startColorJob() {
        colorJob?.cancelAndJoin()
        colorJob = viewModelScope.launch {
            while(isActive) {
                currentHue = (currentHue + 1) % 360
                val newColor = currentHue.toFloat().colorForHue()
                _viewState.emit(
                    ViewState(
                        newColor,
                        newColor.complementaryColor(),
                        newColor.toColorHex()
                    )
                )
                delay(LOOP_PERIOD)
            }
        }
    }

    private suspend fun stopColorJob() {
        colorJob?.cancelAndJoin()
    }

    private fun Float.colorForHue() =
        Color.HSVToColor(
            255,
            listOf( this, DEFAULT_SATURATION, DEFAULT_LIGHTNESS).toFloatArray()
        )

    private fun Int.complementaryColor(): Int {
        val a = (this shr 24) and 0xFF
        val r = (this shr 16) and 0xFF
        val g = (this shr 8) and 0xFF
        val b = this and 0xFF

        return Color.argb(
            a,
            r xor 0xFF,
            g xor 0xFF,
            b xor 0xFF
        )
    }

    private fun Int.toColorHex() = String.format("#%06X", this and 0xFFFFFF)

    override fun onCleared() {
        super.onCleared()
        colorJob?.cancel()
    }
}