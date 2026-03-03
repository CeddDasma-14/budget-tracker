package com.cedd.budgettracker.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.budgettracker.data.local.entity.GoalContributionEntity
import com.cedd.budgettracker.data.local.entity.GoalEntity
import com.cedd.budgettracker.data.local.relation.GoalWithContributions
import com.cedd.budgettracker.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val showAddDialog: Boolean = false,
    val contributeTarget: GoalWithContributions? = null,
    val deleteTarget: GoalWithContributions? = null,
    val successMessage: String? = null
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    val goals: StateFlow<List<GoalWithContributions>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState

    // ── Dialogs ───────────────────────────────────────────────────────────────

    fun showAddDialog()  { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideAddDialog()  { _uiState.update { it.copy(showAddDialog = false) } }

    fun showContribute(goal: GoalWithContributions) {
        _uiState.update { it.copy(contributeTarget = goal) }
    }
    fun hideContribute() { _uiState.update { it.copy(contributeTarget = null) } }

    fun showDeleteConfirm(goal: GoalWithContributions) {
        _uiState.update { it.copy(deleteTarget = goal) }
    }
    fun hideDeleteConfirm() { _uiState.update { it.copy(deleteTarget = null) } }

    fun clearMessage() { _uiState.update { it.copy(successMessage = null) } }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun createGoal(
        name: String,
        emoji: String,
        targetAmount: Double,
        deadline: Long?,
        colorHex: String
    ) {
        if (name.isBlank() || targetAmount <= 0) return
        viewModelScope.launch {
            repository.createGoal(
                GoalEntity(
                    name         = name.trim(),
                    emoji        = emoji,
                    targetAmount = targetAmount,
                    deadline     = deadline,
                    colorHex     = colorHex
                )
            )
            _uiState.update { it.copy(showAddDialog = false, successMessage = "Goal \"${name.trim()}\" created!") }
        }
    }

    fun addContribution(goalId: Long, amount: Double, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            repository.addContribution(
                GoalContributionEntity(
                    goalId    = goalId,
                    amount    = amount,
                    note      = note.trim(),
                    date      = System.currentTimeMillis()
                )
            )
            // Auto-complete goal if reached
            val goal = goals.value.firstOrNull { it.goal.id == goalId }
            goal?.let {
                val newTotal = it.totalSaved + amount
                if (!it.goal.isCompleted && newTotal >= it.goal.targetAmount) {
                    repository.updateGoal(it.goal.copy(isCompleted = true))
                    _uiState.update { s -> s.copy(contributeTarget = null, successMessage = "🎉 Goal \"${it.goal.name}\" completed!") }
                } else {
                    _uiState.update { s -> s.copy(contributeTarget = null, successMessage = "Saved ₱${"%,.2f".format(amount)} toward \"${it.goal.name}\"") }
                }
            }
        }
    }

    fun deleteContribution(contribution: GoalContributionEntity) {
        viewModelScope.launch { repository.deleteContribution(contribution) }
    }

    fun confirmDelete() {
        val goal = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            repository.deleteGoal(goal.goal)
            _uiState.update { it.copy(deleteTarget = null) }
        }
    }

    fun markComplete(goal: GoalWithContributions) {
        viewModelScope.launch {
            repository.updateGoal(goal.goal.copy(isCompleted = !goal.goal.isCompleted))
        }
    }
}
