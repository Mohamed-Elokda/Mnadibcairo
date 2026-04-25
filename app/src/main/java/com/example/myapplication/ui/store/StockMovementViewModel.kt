package com.example.myapplication.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domin.model.ItemMovement
import com.example.myapplication.domin.useCase.GetItemMovementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class StockMovementViewModel @Inject constructor(
    private val getItemMovementUseCase: GetItemMovementUseCase
) : ViewModel() {

    private val _movementState = MutableStateFlow<List<ItemMovement>>(emptyList())
    val movementState: StateFlow<List<ItemMovement>> = _movementState.asStateFlow()

    fun loadMovement(itemId: Int) {
        viewModelScope.launch {
            getItemMovementUseCase(itemId).collect { list ->
                _movementState.value = list
            }
        }
    }
}