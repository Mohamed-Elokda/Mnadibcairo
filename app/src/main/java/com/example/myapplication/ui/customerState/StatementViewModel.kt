package com.example.myapplication.ui.customerState

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domin.model.StatementTransaction
import com.example.myapplication.domin.useCase.GetCustomerStatementUseCase
import com.example.myapplication.domin.useCase.ReconcileAllCustomersDebt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatementViewModel @Inject constructor(
    private val getCustomerStatementUseCase: GetCustomerStatementUseCase,
    private val reconcileAllCustomersDebt: ReconcileAllCustomersDebt
) : ViewModel() {

    // حالة البيانات (القائمة التي ستعرض في الـ RecyclerView)
    private val _statementState = MutableStateFlow<List<StatementTransaction>>(emptyList())
    val statementState: StateFlow<List<StatementTransaction>> = _statementState.asStateFlow()


    fun refreshCustomersDept(){
        viewModelScope.launch {
            reconcileAllCustomersDebt.invoke()
        }
    }

    // وظيفة لجلب كشف الحساب
    fun loadStatement(customerId: Int) {
        viewModelScope.launch {
            getCustomerStatementUseCase(customerId).collect { list ->
                _statementState.value = list
            }
        }
    }
}